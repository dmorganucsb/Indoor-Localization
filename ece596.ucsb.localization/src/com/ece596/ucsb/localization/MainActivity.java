package com.ece596.ucsb.localization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends FragmentActivity  implements SensorEventListener, OnClickListener {
	
	//sensor Manager
	private SensorManager sensorManager;
	
	// Step Detection Classes
	StepDetector xAxisStepDetector;
	StepDetector yAxisStepDetector;
	StepDetector zAxisStepDetector;
	
	//FFT library variable
	private final int FFT_SIZE = 512;
	private DoubleFFT_1D fftlib = new DoubleFFT_1D(FFT_SIZE);
	private static double[] accelFFTfreq = {0,0,0};
	private static double[] prevAccelFFTfreq = {0,0,0};
	private static double[] gyroFFTfreq = {0,0,0};
	private static double[] prevGyroFFTfreq = {0,0,0};
	
	// Array size limitation for sensor data
	private final int ARRAY_SIZE = 512;
	public final static int X_AXIS     = 0;
	public final static int Y_AXIS     = 1;
	public final static int Z_AXIS     = 2;
	
	// data arrays
	private ArrayList<AccelData> accData; // Accelerometer data array 
	private ArrayList<AccelData> gyroData; // Accelerometer data array 
	private ArrayList<AccelData> gravData; // Accelerometer data array 
	
	// Sensor Energy Data
	private double[] accelEnergy = new double[3];
	private double[] gyroEnergy = new double[3];
	private final static double ENERGYTHRESHLOW = 0.15;
	private final static double ENERGYTHRESHHIGH = 2.35;
	private final static int NOACTIVITY = 4;
	
	// filter data
	private static int ORDER = 10;
	private static double[] A_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private static double[] B_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};
	
	//value display refresh rate
	private final int REFRESH_RATE = 2;              // rate of value/screen updates per second
	private final double SENSOR_DELAY = .01;          // sensor delay in seconds 
	private final double Fs		   = 1/SENSOR_DELAY;  //sampling rate of sensor data in Hz
	private int counter 		   = 0;               //counter for refresh rate
	
	//display variable
	private TextView x_stepFreq;   //display for x frequency
	private TextView y_stepFreq;   //display for y frequency
	private TextView z_stepFreq;   //display for z frequency
	private TextView thetaWRTN;   //display for angle
	private TextView x_stepEnergy;  //display for X axis Energy Detection
	private TextView y_stepEnergy;  //display for Y axis Energy Detection
	private TextView z_stepEnergy;  //display for Z axis Energy Detection
	private TextView step_length;  // display for step length
	
	// values to compute orientation
    public static float[] mInclin;
    public static float[] mRot = new float[16];
    public static float[] mGrav = new float[3];
    public static float[] mGeom = new float[3];
    public static float[] rotValues = new float[3];
	
    // variables for pedometer
    private static int step_value = 0;
    private TextView step_num_display;
    public static long zAccDataCurPeakTS = 0;
    public static double zAccDataCurPeak = 0;
    public static boolean zAccDataPeakVer = false;
    public static long zAccDataCurTroughTS = 0;
    public static double zAccDataCurTrough = 0;
    public static boolean zAccDataTroughVer = false;
	public final static int NO_PEAK     = 0;
	public final static int PEAK     = 1;
	public final static int TROUGH     = 2;
	public final static int LOOKBACKLENGTH = 20;
	public final static long LOOKFORWARDTIME = 100; // in ms
	public final static double NOISEFLOOR = 1.0;
	public final static int MAXMIN = 1;
	public final static double FREQTHRESH = 0.3;
	public final static double DIFFTHRESHMIN = 1;
	public final static double DIFFTHRESHMAX = 6;
	public static double highestPeak = 0;
	public static double testPeakValue = 0;
	public static double testTroughValue = 0;
	
	private Button reset_btn, train_btn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		//create arraylist for accelerometer data
		accData = new ArrayList<AccelData>();
		x_stepFreq = (TextView)findViewById( R.id.x_freq_display);
		y_stepFreq = (TextView)findViewById( R.id.y_freq_display);
		z_stepFreq = (TextView)findViewById( R.id.z_freq_display);
		x_stepEnergy = (TextView)findViewById( R.id.x_energy_display);
		y_stepEnergy = (TextView)findViewById( R.id.y_energy_display);
		z_stepEnergy = (TextView)findViewById( R.id.z_energy_display);
		thetaWRTN = (TextView)findViewById(R.id.theta_display);
		//axisEnergy = (TextView)findViewById(R.id.energy_display);
		step_num_display = (TextView)findViewById(R.id.step_num_display);
		step_length = (TextView)findViewById(R.id.step_length_display);
		
		reset_btn = (Button) findViewById(R.id.reset_btn);
		reset_btn.setOnClickListener(this);
		train_btn = (Button) findViewById(R.id.train_btn);
		train_btn.setOnClickListener(this);
		
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), (int) (SENSOR_DELAY*1000000)); //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), (int) (SENSOR_DELAY*10000000)); 	 		 //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), (int) (SENSOR_DELAY*10000000));     //convert from seconds to micro seconds

		// Steup classes for x y and z step detection. these variables need to come from an initial calibration phase
		xAxisStepDetector = new StepDetector(X_AXIS, 1, 100, 1, 0.1, 3);
		yAxisStepDetector = new StepDetector(Y_AXIS, 1, 100, 1, 0.1, 3);
		zAxisStepDetector = new StepDetector(Z_AXIS, 1, 100, 1, 0.1, 3);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.reset_btn:
			step_value = 0;
			break;
		
		case R.id.train_btn:
			showEditDialog();
			break;
			
		default:
			break;
		
		}
	}
	
    private void showEditDialog() {
        FragmentManager fm = getSupportFragmentManager();
        TrainDataDialog enterHeightDialog = new TrainDataDialog();
        enterHeightDialog.show(fm, "fragment_edit_name");
    }

    public void onFinishEditDialog(String inputText) {
        Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();
    }
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		int type = sensor.getType();
		switch(type){
			case Sensor.TYPE_LINEAR_ACCELERATION:
				//****************************building up
				
				//****************************Data accumulation******************************
				long timestamp = System.currentTimeMillis();
				accData.add(new AccelData(timestamp, event.values[X_AXIS], event.values[Y_AXIS], event.values[Z_AXIS]));
				filter();  //10th order butterworth filter
				resizeData(type);
				//***************************************************************************
				
				//*****************************Energy Section********************************
				// get energy
				int axisMaxE = 0;
				accelEnergy = findMaxEnergy(type);
				// find max energy axis
				if (accelEnergy[X_AXIS] < accelEnergy[Y_AXIS] || accelEnergy[X_AXIS] < accelEnergy[Z_AXIS])
					axisMaxE = (accelEnergy[Y_AXIS] > accelEnergy[Z_AXIS] ? Y_AXIS:Z_AXIS);
				else
					axisMaxE = X_AXIS;
				
				if (accelEnergy[axisMaxE] < ENERGYTHRESHLOW)
					axisMaxE = NOACTIVITY;
				//****************************************************************************
				
				//*****************************FFT SECTION************************************
				// get X FFT
				accelFFTfreq[X_AXIS] = (calculateFFT(type, X_AXIS));
				accelFFTfreq[X_AXIS] = (accelFFTfreq[X_AXIS] > 3.2 ? prevAccelFFTfreq[X_AXIS]:accelFFTfreq[X_AXIS]);
				prevAccelFFTfreq[X_AXIS] = accelFFTfreq[X_AXIS];
				// get Y FFT
				accelFFTfreq[Y_AXIS] = (calculateFFT(type, Y_AXIS));
				accelFFTfreq[Y_AXIS] = (accelFFTfreq[Y_AXIS] > 3.2 ? prevAccelFFTfreq[Y_AXIS]:accelFFTfreq[Y_AXIS]);
				prevAccelFFTfreq[Y_AXIS] = accelFFTfreq[Y_AXIS];
				// get Z FFT
				accelFFTfreq[Z_AXIS] = (calculateFFT(type, Z_AXIS));
				accelFFTfreq[Z_AXIS] = (accelFFTfreq[Z_AXIS] > 3.2 ? prevAccelFFTfreq[Z_AXIS]:accelFFTfreq[Z_AXIS]);
				prevAccelFFTfreq[Z_AXIS] = accelFFTfreq[Z_AXIS];
				//******************************************************************************
				
				//****************************STEP DETECTION SECTION****************************
				if (accData.size() == ARRAY_SIZE){
					zAxisStepDetector.updateArray(accData, accelFFTfreq);
					xAxisStepDetector.updateArray(accData, accelFFTfreq);
					yAxisStepDetector.updateArray(accData, accelFFTfreq);
					boolean xStep = xAxisStepDetector.FindStep();
					//boolean yStep = yAxisStepDetector.FindStep();
					boolean zStep = zAxisStepDetector.FindStep();
					if ( xStep || /*yStep ||*/ zStep){
						if (accelFFTfreq[X_AXIS] != 0 && accelFFTfreq[Y_AXIS] != 0 && accelFFTfreq[Z_AXIS] != 0
								&& (accelFFTfreq[X_AXIS] - 2*accelFFTfreq[Y_AXIS]) < FREQTHRESH
								&& accelFFTfreq[X_AXIS] - accelFFTfreq[Z_AXIS] < FREQTHRESH
								&& ENERGYTHRESHHIGH > accelEnergy[X_AXIS] && accelEnergy[X_AXIS] > ENERGYTHRESHLOW
								&& ENERGYTHRESHHIGH > accelEnergy[Y_AXIS] && accelEnergy[Y_AXIS] > ENERGYTHRESHLOW 
								&& ENERGYTHRESHHIGH > accelEnergy[Z_AXIS] && accelEnergy[Z_AXIS] > ENERGYTHRESHLOW){                                   // && yStep_detect == MAXMIN);
							step_value++;
						}
					}
				}
				//*******************************************************************************
				
				//*****************************DISPLAY UPDATE SECTION****************************
				counter++;			
				if (counter > Fs/REFRESH_RATE){
					updateDisplays(axisMaxE);
					counter = 0;
				}
				//********************************************************************************
				
				break;
			case Sensor.TYPE_GRAVITY:
				// TODO can be used to help step detection?
	            System.arraycopy(event.values, 0, mGrav, 0, 3);
	            
				break;
			case Sensor.TYPE_GYROSCOPE:
				// TODO can be used to help step detection?
				
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				// TODO can be used to help step detection?
	            System.arraycopy(event.values, 0, mGeom, 0, 3);
	            getTheta();

				break;
			default:
				break;
		}
		
	}
		
	/**
	 * Function to maintain the size of the array lists
	 * 
	 * This function will take in a type (which corresponds to the array list
	 * you want to resize) and ensures that it is the correct size determined
	 * by the variable SIZE. The function assumes that there is only one array
	 * for each type of arraylist data and modifies this array appropriately
	 * 
	 * @param context
	 * @param attrs
	 */
	private void resizeData(int type){
		switch(type){
		case Sensor.TYPE_LINEAR_ACCELERATION:
			if (accData.size() > ARRAY_SIZE)
				accData.remove(1);
			break;
		default:
			break;
		}
		
		return;
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
	private double calculateFFT(int type, int axis){
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
    		freqRange[i] = Fs / 2*linspace[i];  //

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
	private double[] findMaxEnergy(int type){
		
		double xEnergy = 0;
		double yEnergy = 0;
		double zEnergy = 0;
		int size = 0;
		double result[] = {0,0,0};
		
		switch(type){
			case Sensor.TYPE_LINEAR_ACCELERATION:
				for (int i=0;i<accData.size();i++){
					xEnergy += Math.abs(accData.get(i).getValue(X_AXIS));
					yEnergy += Math.abs(accData.get(i).getValue(Y_AXIS));
					zEnergy += Math.abs(accData.get(i).getValue(Z_AXIS));
				}
				size = accData.size();
				break;
			default:
				break;
		}
		
		result[X_AXIS] = xEnergy / size;
		result[Y_AXIS] = yEnergy / size;
		result[Z_AXIS] = zEnergy / size;
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
	private void getTheta(){
	    if (mGrav != null && mGeom != null) {
	    	boolean success = SensorManager.getRotationMatrix(mRot, mInclin, mGrav, mGeom);
			if (success){
				SensorManager.getOrientation(mRot, rotValues);
				rotValues[0] = (float) (((rotValues[0] + Math.PI/2) *180/Math.PI));
				if (rotValues[0] < 0)
					rotValues[0] = rotValues[0] + 360;
			}
			return;
	    }
	}
	/*
	private int findCycle(int Axis, double currPeak, long currPeakTS, double currTrough, long currTroughTS double avgValue){
		double AccData[] = new double[accData.size()];
	    //double AccDataAvg = 0;
	    double AccDataCurValue = 0;
	    long AccDataCurValueTS = 0;
	    double AccDataPrevValue = 0;

		for (int i=0;i<accData.size();i++){
			AccData[i] = accData.get(i).getValue(Z_AXIS);
			AccDataAvg += AccData[i];
		}
		AccDataAvg = AccDataAvg / AccData.length;
		AccDataCurValue = AccData[AccData.length-1];
		AccDataCurValueTS = accData.get(AccData.length-1).getTimestamp();
		
		//if not currently holding a peak look for a new peak
		if (currPeak == 0){
			int j = 1;
			while (j < LOOKBACKLENGTH && (AccData.length - j) > 0 ){
				AccDataPrevValue = AccData[AccData.length-1-j];
				if (AccDataPrevValue > AccDataCurValue)
					break; //not a true peak
				j++;
			}
			if (j == LOOKBACKLENGTH){
				//found a peak (I think) need to verify by looking at future data
				zAccDataCurPeak = AccDataCurValue; // store the supposed peak
				zAccDataCurPeakTS = AccDataCurValueTS; // get peak time stamp
				return NO_PEAK;
			}
			return NO_PEAK; //keep searching
		}
		else if (zAccDataCurPeak != 0 && zAccDataCurTrough == 0){
			if (zAccDataPeakVer = false){
				//still need to verify the current "supposed" peak
				if (zAccDataCurPeakTS + LOOKFORWARDTIME > AccDataCurValueTS){
					// try to verify peak
					if (zAccDataCurPeak > AccDataCurValue && zAccDataCurPeak > AccDataAvg && zAccDataCurPeak > NOISEFLOOR){
						// still a peak (but may need more verification)
						return NO_PEAK;
					}
					else {  //not a true peak after all
						zAccDataCurPeak = AccDataCurValue; // since the new value is larger, test it as new peak
						zAccDataCurPeakTS = AccDataCurValueTS;
						return NO_PEAK;
					}
				}
				else //peak verified! time to start looking for a trough
				{
					zAccDataPeakVer = true; //peak verified
					// add to running average of last N peaks to help with false detection later
					return PEAK;
				}
				
			}
			else{
				//we have a peak, lets look for a trough here
				int j = 1;
				while (j < LOOKBACKLENGTH && (AccData.length - j) > 0 ){
					AccDataPrevValue = AccData[AccData.length-1-j];
					if (AccDataPrevValue < AccDataCurValue)
						break; //not a true trough
					j++;
				}
				if (j == LOOKBACKLENGTH){
					//found a trough (I think) need to verify by looking at future data
					zAccDataCurTrough = AccDataCurValue; // store the supposed trough
					zAccDataCurTroughTS = AccDataCurValueTS; // get trough time stamp
					return NO_PEAK;
				}
				return NO_PEAK; //keep searching
				
			}
			
		}
		else if (zAccDataCurPeak != 0 && zAccDataCurTrough != 0){
			//found a peak and a supposed trough
			//if (yAccDataTroughVer = false){
				// still need to verify the current "supposed" trough
				if (zAccDataCurTroughTS + LOOKFORWARDTIME > zAccDataCurValueTS){
					// try to verify trough
					if (zAccDataCurTrough < zAccDataCurValue && zAccDataCurTrough < zAccDataAvg && zAccDataCurTrough < -NOISEFLOOR){
						// still a peak (but may need more verification)
						return NO_PEAK;
					}
					else {  //not a true trough after all
						zAccDataCurTrough = zAccDataCurValue; // since the new value is smaller, test it as new trough
						zAccDataCurTroughTS = zAccDataCurValueTS;
						return NO_PEAK;
					}
				}
				else { //trough verified time to count the step, and start looking all over again
					testPeakValue = zAccDataCurPeak;
					zAccDataCurPeak = 0;
					testTroughValue = zAccDataCurTrough;
					zAccDataCurTrough = 0;
					zAccDataPeakVer = false;
					return TROUGH;
					
					// made need an additional wait time here to further verify the waveform, and if so, we would need
					// a second flag for trough detected to know we need to enter the further verification routine
				}
			//}
			
		}
		else {//(shouldn't get here)
	        Toast toast = Toast.makeText(getApplicationContext(), "You shouldnt be here", Toast.LENGTH_SHORT);
	        toast.show();
		}
		return NO_PEAK;
	}
	*/
	
	private int findYCycle(){
		/*
		double yAccData[] = new double[accData.size()];
	    double yAccDataAvg = 0;
	    double yAccDataCurValue = 0;
	    long yAccDataCurValueTS = 0;
	    double yAccDataPrevValue = 0;

		for (int i=0;i<accData.size();i++){
			yAccData[i] = accData.get(i).getValue(Y_AXIS);
			yAccDataAvg += yAccData[i];
		}
		yAccDataAvg = yAccDataAvg / yAccData.length;
		yAccDataCurValue = yAccData[yAccData.length-1];
		yAccDataCurValueTS = accData.get(yAccData.length-1).getTimestamp();
		
		//if not currently holding a peak look for a new peak
		if (zAccDataCurPeak == 0){
			int j = 1;
			while (j < LOOKBACKLENGTH && (yAccData.length - j) > 0 ){
				yAccDataPrevValue = yAccData[yAccData.length-1-j];
				if (yAccDataPrevValue > yAccDataCurValue)
					break; //not a true peak
				j++;
			}
			if (j == LOOKBACKLENGTH){
				//found a peak (I think) need to verify by looking at future data
				yAccDataCurPeak = yAccDataCurValue; // store the supposed peak
				yAccDataCurPeakTS = yAccDataCurValueTS; // get peak time stamp
				return NONE;
			}
			return NONE; //keep searching
		}
		else if (zAccDataCurPeak != 0 && zAccDataCurTrough == 0){
			if (zAccDataPeakVer = false){
				//still need to verify the current "supposed" peak
				if (zAccDataCurPeakTS + LOOKFORWARDTIME > zAccDataCurValueTS){
					// try to verify peak
					if (zAccDataCurPeak > zAccDataCurValue && zAccDataCurPeak > zAccDataAvg && zAccDataCurPeak > NOISEFLOOR){
						// still a peak (but may need more verification)
						return NONE;
					}
					else {  //not a true peak after all
						zAccDataCurPeak = zAccDataCurValue; // since the new value is larger, test it as new peak
						zAccDataCurPeakTS = zAccDataCurValueTS;
						return NONE;
					}
				}
				else //peak verified! time to start looking for a trough
				{
					zAccDataPeakVer = true; //peak verified
					// add to running average of last N peaks to help with false detection later
					return PEAK;
				}
				
			}
			else{
				//we have a peak, lets look for a trough here
				int j = 1;
				while (j < LOOKBACKLENGTH && (zAccData.length - j) > 0 ){
					zAccDataPrevValue = zAccData[zAccData.length-1-j];
					if (zAccDataPrevValue < zAccDataCurValue)
						break; //not a true trough
					j++;
				}
				if (j == LOOKBACKLENGTH){
					//found a trough (I think) need to verify by looking at future data
					zAccDataCurTrough = zAccDataCurValue; // store the supposed trough
					zAccDataCurTroughTS = zAccDataCurValueTS; // get trough time stamp
					return NONE;
				}
				return NONE; //keep searching
				
			}
			
		}
		else if (zAccDataCurPeak != 0 && zAccDataCurTrough != 0){
			//found a peak and a supposed trough
			//if (yAccDataTroughVer = false){
				// still need to verify the current "supposed" trough
				if (zAccDataCurTroughTS + LOOKFORWARDTIME > zAccDataCurValueTS){
					// try to verify trough
					if (zAccDataCurTrough < zAccDataCurValue && zAccDataCurTrough < zAccDataAvg && zAccDataCurTrough < -NOISEFLOOR){
						// still a peak (but may need more verification)
						return NONE;
					}
					else {  //not a true trough after all
						zAccDataCurTrough = zAccDataCurValue; // since the new value is smaller, test it as new trough
						zAccDataCurTroughTS = zAccDataCurValueTS;
						return NONE;
					}
				}
				else { //trough verified time to count the step, and start looking all over again
					zAccDataCurPeak = 0;
					zAccDataCurTrough = 0;
					zAccDataPeakVer = false;
					return TROUGH;
					
					// made need an additional wait time here to further verify the waveform, and if so, we would need
					// a second flag for trough detected to know we need to enter the further verification routine
				}
			//}
			
		}
		else {//(shouldn't get here)
			Log.d("MyApp", "you shouldn't be in here");
		}
		*/
		return NO_PEAK;
		
	}
	
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
	public void filter(){ // implements a 10th order butterworth filter (coefficients created in MATLAB)
		double[] x_orig = new double[accData.size()];
		double[] y_filt = new double[accData.size()];
		
		for (int i=0;i<accData.size();i++){
			x_orig[i] = accData.get(i).getValue(Z_AXIS);
		}
		
		int nSize = x_orig.length;
		for (int n=0;n<nSize;n++){
			for (int m=0;m<ORDER;m++){
				if (n-m >= 0)
					y_filt[n] += x_orig[n-m]*B_coeffs[m];
				
				if (n-m >=1 && m >=1)
					y_filt[n] -= y_filt[n-m]*A_coeffs[m];
			}
		}
		
		return;
	}

	/**
	 * Function to display the results of sensor algorithms
	 * 
	 * This function will take check the status of various checkboxes
	 * for each of the displayed results, and will display them only
	 * if the appropriate box is checked. This function should only
	 * be called as frequenctly as you want updates
	 * 
	 * @param context
	 * @param attrs
	 */
	private void updateDisplays(int primary_axis){
		x_stepFreq.setText(Double.toString(accelFFTfreq[X_AXIS]));
		y_stepFreq.setText(Double.toString(accelFFTfreq[Y_AXIS]));
		z_stepFreq.setText(Double.toString(accelFFTfreq[Z_AXIS]));
		x_stepEnergy.setText(String.format("%.4f", accelEnergy[X_AXIS]));
		y_stepEnergy.setText(String.format("%.4f", accelEnergy[Y_AXIS]));
		z_stepEnergy.setText(String.format("%.4f", accelEnergy[Z_AXIS]));
		
		resetColors();
		
		switch(primary_axis){
			case X_AXIS:
				x_stepFreq.setTextColor(Color.GREEN);
				x_stepEnergy.setTextColor(Color.GREEN);
				break;
			case Y_AXIS:
				y_stepFreq.setTextColor(Color.GREEN);
				y_stepEnergy.setTextColor(Color.GREEN);
				break;
			case Z_AXIS:
				z_stepFreq.setTextColor(Color.GREEN);
				z_stepEnergy.setTextColor(Color.GREEN);
				break;
			default:
				resetColors();
				break;
		}
		
		//step_length.setText(Double.toString(highestPeak));
		thetaWRTN.setText(Float.toString(rotValues[0]));
		step_num_display.setText(Integer.toString(step_value));
		
		return;
	}
	
	private void resetColors(){
		
		x_stepFreq.setTextColor(Color.WHITE);
		x_stepEnergy.setTextColor(Color.WHITE);
		y_stepFreq.setTextColor(Color.WHITE);
		y_stepEnergy.setTextColor(Color.WHITE);
		z_stepFreq.setTextColor(Color.WHITE);
		z_stepEnergy.setTextColor(Color.WHITE);
		
		return;
	}
	
}





