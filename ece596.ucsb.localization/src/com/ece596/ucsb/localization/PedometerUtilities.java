package com.ece596.ucsb.localization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class PedometerUtilities {
		
	// FFT Variables
	private final static int FFT_SIZE = 512;
	private final static DoubleFFT_1D fftlib = new DoubleFFT_1D(FFT_SIZE);
	
	// Energy Variables
	private final static int ENERGYWINDOWSIZE = 50;
	
	// Filter Variables
	public static int ORDER = 5;
	private final static double[] A_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private final static double[] B_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};

	// Angle Theta Variables
    private static float[] mInclin;
    private static float[] mRot = new float[16];
    public static float[] rotValues = new float[3];
    public static float avgrotValue = 0;
    public static float rotValueGyro = 0;
    public static int count = 0;
    public static boolean first = true;
    
    static Matrix matrix = new Matrix();
	
	/**
	 * 10th Order Butterworth Filter
	 * 
	 * parameters for this filter were created in MATLAB and the coefficients
	 * were hard-coded in. The filter is a 10th order butter worth filter with
	 * a cutoff frequency of 10/3 (repeating) Hz.
	 * 
	 * @param context
	 * @param attrs
	 */
	public static ArrayList<AccelData> filter(ArrayList<AccelData> accData){ // implements a 10th order butterworth filter (coefficients created in MATLAB)
		ArrayList<AccelData> filtData = new ArrayList<AccelData>();
		double[] x_orig = new double[accData.size()];
		double[] y_filt = new double[accData.size()];
		
		// create return AccelData with same time stamps!

		
		//for (MainActivity.Axes AXIS : MainActivity.Axes.values()){
			for (int i=0;i<accData.size();i++)
				x_orig[i] = accData.get(i).getValue(MainActivity.Z_AXIS);
		
			int nSize = accData.size();
			for (int n=0;n<nSize;n++){
				for (int m=0;m<ORDER;m++){
					if (n-m >= 0)
						y_filt[n] += x_orig[n-m]*B_coeffs[m];
					
					if (n-m >=1 && m >=1)
						y_filt[n] -= y_filt[n-m]*A_coeffs[m];
				}
			}			
			for (int i=0;i<y_filt.length;i++)
				filtData.add(new AccelData(accData.get(i).getTimestamp(), 0, 0, y_filt[i]));
		//}
		return filtData;
	}
	
	/**
	 * Function to calculate the FFT of sensor data
	 * 
	 * This function will take in a sensor type, (which corresponds
	 * to the variable used for input to the FFT) and the axis (x, y, z)
	 * of the data which you want to take the FFT of. The result is
	 * analyzed and the maximum frequency component (excluding DC) is
	 * stored in the variable FFTfreq.
	 * 
	 * @param context
	 * @param attrs
	 */
	public static double calculateFFT(int type, int axis, ArrayList<AccelData> accData){
		double myFFT[] = new double[FFT_SIZE];
		double mySpectrum[] = new double[FFT_SIZE / 2];
		double freqRange[] = new double[FFT_SIZE / 2 + 1];
		double linspace[] = new double[FFT_SIZE / 2 + 1];
		double curFreq = 0;
		
		switch(type){
		case Sensor.TYPE_LINEAR_ACCELERATION:
			if (accData.size() == FFT_SIZE)
			{
				for(int i=0;i<accData.size();i++)
					myFFT[i] = accData.get(i).getValue(axis);
			}
			break;
		default:
			break;
		}
		
		fftlib.realForward(myFFT);
        for (int i=0;i<FFT_SIZE;i++)
        	myFFT[i] = Math.abs(myFFT[i]);
        
        for (int k = 0; k < FFT_SIZE/2 - 1;k++){
            mySpectrum[k] = Math.sqrt(myFFT[2*k]*myFFT[2*k] + myFFT[2*k+1]*myFFT[2*k+1]);
            linspace[k+1] = (double) 2* (k+1) / FFT_SIZE;
        }
        linspace[0]=0;
        linspace[FFT_SIZE / 2]=1;
        mySpectrum[0] = 0; mySpectrum[1] = 0; mySpectrum[2]=0; //remove DC

    	for (int i = 0;i<linspace.length;i++)
    		freqRange[i] = MainActivity.Fs / 2*linspace[i];  //

    	List<Double> b = Arrays.asList(ArrayUtils.toObject(mySpectrum));
    	if (Collections.max(b) < 20){  // must be noise
    		curFreq = 0;
    		return curFreq;
    	}
    	int max = b.indexOf(Collections.max(b));
    	curFreq = freqRange[max];
    	
    	return curFreq;
	}
	
	/**
	 * Function to Determine the axis (x,y or z) which has the highest energy (activity)
	 * 
	 * This function will take the array of accelerometer data and compute the average 
	 * (absolute value)
	 * format of result is array{x_value,y_value,z_value}
	 * 
	 * @param context
	 * @param attrs
	 */
	public static double[] findMaxEnergy(ArrayList<AccelData> data, int type){
		double result[] = {0,0,0};
		double xEnergy = 0;
		double yEnergy = 0;
		double zEnergy = 0;
		int size = 0;
		
		if (data.size() < ENERGYWINDOWSIZE)
			return result;
		
		switch(type){
			case Sensor.TYPE_LINEAR_ACCELERATION:
				for (int i=data.size();i>data.size()-ENERGYWINDOWSIZE;i--){
					xEnergy += Math.abs(data.get(i-1).getValue(MainActivity.X_AXIS));
					yEnergy += Math.abs(data.get(i-1).getValue(MainActivity.Y_AXIS));
					zEnergy += Math.abs(data.get(i-1).getValue(MainActivity.Z_AXIS));
				}
				size = ENERGYWINDOWSIZE;
				break;
			default:
				break;
		}
		
		result[MainActivity.X_AXIS] = xEnergy / size;
		result[MainActivity.Y_AXIS] = yEnergy / size;
		result[MainActivity.Z_AXIS] = zEnergy / size;
		return result;
	}
	
	/**
	 * Get the angle Theta
	 * 
	 * This function will find the value of theta, which is the angle
	 * about the Z axis (earth reference has z pointing into the earth).
	 * The angle offset is with respect to north (magnetic north rather
	 * than true north).
	 * 
	 * @param context
	 * @param attrs
	 */
	public static void getTheta(float[] mGrav, float[] mGeom){
	    if (mGrav != null && mGeom != null) {
	    	if (!first && (Math.abs(MainActivity.mValues[0]) < 0.5)){
	    		//rotValueGyro = (float) Math.toDegrees(MainActivity.mValues[0]);
	    		rotValueGyro += Math.toDegrees(MainActivity.mValues[0]);
	    		return;
	    	}
	    	
	    	boolean success = SensorManager.getRotationMatrix(mRot, mInclin, mGrav, mGeom);
			if (success){
				SensorManager.getOrientation(mRot, rotValues);
				rotValues[0] = (float) (((rotValues[0] + Math.PI/2) *180/Math.PI));
				if (rotValues[0] < 0)
					rotValues[0] = rotValues[0] + 360;
				rotValueGyro = rotValues[0];
				first = false;
			}
			return;
	    }
	}
	
	public static void getAvgTheta(){
		avgrotValue = (rotValueGyro/*rotValues[0]*/+count*avgrotValue)/(count+1);
		count++;
	}
}