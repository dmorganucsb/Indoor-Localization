package com.ece596.ucsb.localization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
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
	
	//WEKA Library variables
	//Classifier myLeastSquares;
	LinearRegression myLR;
	Evaluation eval;
	DataSource modelSource;
	Instances modelData;
	DataSource testSource;
	Instances test;
	String header = "@relation Step-Size\n\n@attribute Hieght real\n@attribute freq_times_height real\n@attribute Step_size real\n\n@data\n";

	//FFT library variables
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
	private ArrayList<AccelData> accData;  // Accelerometer data array 
	private ArrayList<AccelData> gyroData; // Gyroscope data array 
	private ArrayList<AccelData> gravData; // Gravity data array 
	private ArrayList<AccelData> trainData;
	private double[][] trainPeakData; // array for calibration Data
	private double[][] trainTroughData; // array for calibration Data
	private double[] peakAvg = new double[3];
	private double[] troughAvg = new double[3];
	private double[] peakThresh = new double[3];
	private double[] troughThresh = new double[3];
	private double[] p2pThresh = new double[3];
	
	// Sensor Energy Data
	private double[] accelEnergy = new double[3];
	private double[] gyroEnergy = new double[3];
	private static double[] energyThreshLow;
	private static double[] energyThreshHigh;
	private final static int NOACTIVITY = 4;
	private final int ENERGYWINDOWSIZE = 50;
	private double[] calibE = new double[3];
	private double EHIGH = 2.35;
	private double ELOW = 0.15;
	
	// filter data
	private static int ORDER = 10;
	private static double[] A_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private static double[] B_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};
	
	//value display refresh rate
	private final int REFRESH_RATE 		= 2;                // rate of value/screen updates per second
	private final double SENSOR_DELAY 	= .01;              // sensor delay in seconds 
	private final double Fs		   		= 1/SENSOR_DELAY;   //sampling rate of sensor data in Hz
	private int counter 		   		= 0;                //counter for refresh rate
	
	//display variable
	private TextView x_stepFreq;    //display for x frequency
	private TextView y_stepFreq;    //display for y frequency
	private TextView z_stepFreq;    //display for z frequency
	private TextView x_stepEnergy;  //display for X axis Energy Detection
	private TextView y_stepEnergy;  //display for Y axis Energy Detection
	private TextView z_stepEnergy;  //display for Z axis Energy Detection
	private TextView step_length_display;   // display for step length
	private TextView thetaWRTN;     //display for angle
	private TextView distance_display;
	
	//display checkboxes
	private CheckBox cb_x_freq;
	private CheckBox cb_x_energy;
	private CheckBox cb_step_num;
	private CheckBox cb_step_length;
	private CheckBox cb_y_freq;
	private CheckBox cb_y_energy;
	private CheckBox cb_thetaWRTN;
	private CheckBox cb_z_freq;
	private CheckBox cb_z_energy;
	private CheckBox cb_distance_display;
	
	
	// values to compute orientation
    public static float[] mInclin;
    public static float[] mRot = new float[16];
    public static float[] mGrav = new float[3];
    public static float[] mGeom = new float[3];
    public static float[] rotValues = new float[3];
	
    // variables for pedometer
    private TextView step_num_display;
    private static int step_value = 0;
    private static int fake_step_value = 0;
	public final static double FREQTHRESH = 0.3;
	
	//two control buttons
	private Button reset_btn, train_btn;
	
	public static FragmentManager fm;
    public TrainDataDialog enterHeightDialog;
    public static CalibrationDialog calibrateSteps;
    public static boolean calibration_inProgress;
    private double inputHeight;
    private static boolean timeout;
    private int timeoutCount;
    
    private final double wiggleRoom = .2;
    
    private double step_length;
    private double distance;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		fm = getSupportFragmentManager();
        enterHeightDialog = new TrainDataDialog();
        calibrateSteps = new CalibrationDialog();
        calibration_inProgress = false;
        timeout = false;
        timeoutCount = 0;
        distance = 0;
		
		//create arraylist for accelerometer data
		accData = new ArrayList<AccelData>();
		trainData = new ArrayList<AccelData>();
		trainPeakData = new double[3][20];
		trainTroughData = new double[3][20];
		energyThreshHigh = new double[3];
		energyThreshLow = new double[3];
		
		//Weka Libraries
		try {
			myLR = new LinearRegression();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//display data
		x_stepFreq = (TextView)findViewById( R.id.x_freq_display);
		y_stepFreq = (TextView)findViewById( R.id.y_freq_display);
		z_stepFreq = (TextView)findViewById( R.id.z_freq_display);
		x_stepEnergy = (TextView)findViewById( R.id.x_energy_display);
		y_stepEnergy = (TextView)findViewById( R.id.y_energy_display);
		z_stepEnergy = (TextView)findViewById( R.id.z_energy_display);
		thetaWRTN = (TextView)findViewById(R.id.theta_display);
		step_num_display = (TextView)findViewById(R.id.step_num_display);
		step_length_display = (TextView)findViewById(R.id.step_length_display);
		distance_display = (TextView)findViewById(R.id.distance_display);
		
		//checkboxes for display data
		cb_x_freq = (CheckBox) findViewById(R.id.cb_x_freq_display);
		cb_x_energy = (CheckBox) findViewById(R.id.cb_x_energy_display);
		cb_step_num = (CheckBox) findViewById(R.id.cb_step_num_display);
		cb_step_length = (CheckBox) findViewById(R.id.cb_step_length_display);
		cb_y_freq = (CheckBox) findViewById(R.id.cb_y_freq_display);
		cb_y_energy = (CheckBox) findViewById(R.id.cb_y_energy_display);
		cb_thetaWRTN = (CheckBox) findViewById(R.id.cb_theta_display);
		cb_z_freq = (CheckBox) findViewById(R.id.cb_z_freq_display);
		cb_z_energy = (CheckBox) findViewById(R.id.cb_z_energy_display);
		cb_distance_display = (CheckBox) findViewById(R.id.cb_distance_display);
		
		reset_btn = (Button) findViewById(R.id.reset_btn);
		reset_btn.setOnClickListener(this);
		train_btn = (Button) findViewById(R.id.train_btn);
		train_btn.setOnClickListener(this);
		
		trainModel();
		
		xAxisStepDetector = new StepDetector(X_AXIS); // AXIS
		yAxisStepDetector = new StepDetector(Y_AXIS);
		zAxisStepDetector = new StepDetector(Z_AXIS);
		
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), (int) (SENSOR_DELAY*1000000)); //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), (int) (SENSOR_DELAY*10000000)); 	 		 //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), (int) (SENSOR_DELAY*10000000));     //convert from seconds to micro seconds

		
		this.myOnClick(null, R.id.train_btn);

	}
		
	public void startPedometer() {
		// Inflate the menu; this adds items to the action bar if it is present.
		xAxisStepDetector.setThreshVariables(peakAvg[X_AXIS], peakThresh[X_AXIS]+wiggleRoom, troughAvg[X_AXIS], troughThresh[X_AXIS]+wiggleRoom, p2pThresh[X_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		yAxisStepDetector.setThreshVariables(peakAvg[Y_AXIS], peakThresh[Y_AXIS]+wiggleRoom, troughAvg[Y_AXIS], troughThresh[Y_AXIS]+wiggleRoom, p2pThresh[Y_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		zAxisStepDetector.setThreshVariables(peakAvg[Z_AXIS], peakThresh[Z_AXIS]+wiggleRoom, troughAvg[Z_AXIS], troughThresh[Z_AXIS]+wiggleRoom, p2pThresh[Z_AXIS]-wiggleRoom); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		
		Log.d("MyApp", "X data = " + peakAvg[X_AXIS]+ " " +
				peakThresh[X_AXIS] + " " +  troughAvg[X_AXIS] + " " + 
				troughThresh[X_AXIS] + " " + p2pThresh[X_AXIS]);
		Log.d("MyApp", "y data = " + peakAvg[Y_AXIS]+ " " + 
				peakThresh[Y_AXIS] + " " +  troughAvg[Y_AXIS] + " " + 
				troughThresh[Y_AXIS] + " " + p2pThresh[Y_AXIS]);
		Log.d("MyApp", "z data = " + peakAvg[Z_AXIS]+ " " + 
				peakThresh[Z_AXIS] + " " +  troughAvg[Z_AXIS] + " " + 
				troughThresh[Z_AXIS] + " " + p2pThresh[Z_AXIS]);
		// Steup classes for x y and z step detection. these variables need to come from an initial calibration phase
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onClick(View arg0) {
		myOnClick(arg0, 0);
		
	}
	
	public void myOnClick(View v, int Id) {
		int switchValue;
		
		if (v == null)
			switchValue = Id;
		else
			switchValue = v.getId();
		
		switch (switchValue) {
		
		case R.id.reset_btn:
			step_value = 0;
			distance = 0;
			break;
		
		case R.id.train_btn:
	        enterHeightDialog.show(fm, "fragment_enter_height");
			break;
			
		default:
			break;
		
		}
	}

		public void startCalibration(double inputHeight) {
		
			this.inputHeight = inputHeight;
	        Toast.makeText(this, "Calibration started, " + inputHeight, Toast.LENGTH_SHORT).show();
	        trainData.clear();        
	        calibrateSteps.show(fm, "fragment_calibrate_steps");
	        calibration_inProgress = true;
	        energyThreshLow[X_AXIS] = 10;
	        energyThreshLow[Y_AXIS] = 10;
	        energyThreshLow[Z_AXIS] = 10;
	        energyThreshHigh[X_AXIS] = 0;
	        energyThreshHigh[Y_AXIS] = 0;
	        energyThreshHigh[Z_AXIS] = 0;

		}
		
		public void finishCalibration() {
			calibration_inProgress = false;
	        Toast.makeText(this, "Calibration finished", Toast.LENGTH_SHORT).show();
	        for (int i=0;i<3;i++)
	        	extractCalibratedData(i);
	        
	        startPedometer();
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
				//****************************Data accumulation******************************
				long timestamp = System.currentTimeMillis();
				accData.add(new AccelData(timestamp, event.values[X_AXIS], event.values[Y_AXIS], event.values[Z_AXIS]));
				filter();  //10th order butterworth filter
				resizeData(type);
				//***************************************************************************
				
				//****************************Store Calibration Data*****************************
				if (calibration_inProgress){
					trainData.add(new AccelData(timestamp, accData.get(accData.size()-1).getValue(X_AXIS), 
							accData.get(accData.size()-1).getValue(Y_AXIS), 
							accData.get(accData.size()-1).getValue(Z_AXIS)));
					calibE = findMaxEnergy(trainData, type);
					for (int i = 0;i<3;i++){
						if (calibE[i] + wiggleRoom/2 > energyThreshHigh[i] + wiggleRoom/2)
							energyThreshHigh[i] = calibE[i] + wiggleRoom/2;
						if (calibE[i] != 0 && calibE[i] < energyThreshLow[i])
							energyThreshLow[i] = calibE[i];
					}
					return;
				}
				//*******************************************************************************
				
				//*****************************Energy Section********************************
				// get energy
				int axisMaxE = 0;
				accelEnergy = findMaxEnergy(accData, type);
				// find max energy axis
				if (accelEnergy[X_AXIS] < accelEnergy[Y_AXIS] || accelEnergy[X_AXIS] < accelEnergy[Z_AXIS])
					axisMaxE = (accelEnergy[Y_AXIS] > accelEnergy[Z_AXIS] ? Y_AXIS:Z_AXIS);
				else
					axisMaxE = X_AXIS;
				
				if (accelEnergy[axisMaxE] < energyThreshLow[axisMaxE])
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
					boolean yStep = yAxisStepDetector.FindStep();
					boolean zStep = zAxisStepDetector.FindStep();
					if ( xStep || yStep || zStep){
						fake_step_value++;
						if (accelFFTfreq[X_AXIS] != 0 && accelFFTfreq[Y_AXIS] != 0 && accelFFTfreq[Z_AXIS] != 0
								&& (accelFFTfreq[X_AXIS] - 2*accelFFTfreq[Y_AXIS]) < FREQTHRESH
								&& accelFFTfreq[X_AXIS] - accelFFTfreq[Z_AXIS] < FREQTHRESH
								&& /*energyThreshHigh[X_AXIS]*/ EHIGH > accelEnergy[X_AXIS] && accelEnergy[X_AXIS] > ELOW/*energyThreshLow[X_AXIS]*/
								&& /*energyThreshHigh[Y_AXIS]*/ EHIGH > accelEnergy[Y_AXIS] && accelEnergy[Y_AXIS] > ELOW/*energyThreshLow[Y_AXIS]*/
								&& /*energyThreshHigh[Z_AXIS]*/ EHIGH > accelEnergy[Z_AXIS] && accelEnergy[Z_AXIS] > ELOW/*energyThreshLow[Z_AXIS]*/
								&& calibration_inProgress == false && timeout == false){                                   // && yStep_detect == MAXMIN);
							step_value++;
							timeout = true;
							timeoutCount = 33;
							//****************************Step Size Estimation*******************************
							 calculateStepSize(); //using current step frequency and hieght parameter
							 calculateDistance();
							//*******************************************************************************
						}
					}
				}
				//*******************************************************************************
				
				//****************************Step detection timeout period**********************
				if (timeoutCount != 0)
					timeoutCount--;
				else
					timeout = false;
				//*******************************************************************************
				
				//****************************Distance Calculation*******************************
				
				//TODO Distance = Distance + stepsize if step detected
				
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
	private double[] findMaxEnergy(ArrayList<AccelData> data, int type){
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
					xEnergy += Math.abs(data.get(i-1).getValue(X_AXIS));
					yEnergy += Math.abs(data.get(i-1).getValue(Y_AXIS));
					zEnergy += Math.abs(data.get(i-1).getValue(Z_AXIS));
				}
				size = ENERGYWINDOWSIZE;
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
	
	private void extractCalibratedData(int axis){
		//TODO
		/**
		 * find top 10 peaks and bottom 10 troughs (x and z directions)
		 * calculate 
		 * 1) peak average value
		 * 2) largest diff between one of the ten peaks and the computed average
		 * 3) trough average value
		 * 4) largest diff between one of the ten troughs and the computed average
		 * 5) largest difference between a peak and a trough
		 */
		trainPeakData[axis] = findPeaks(trainData, axis);
		trainTroughData[axis] = findTroughs(trainData, axis);
		peakAvg[axis] = calculatePeakAverage(trainPeakData[axis]);
		troughAvg[axis] = calculateTroughAverage(trainTroughData[axis]);
		peakThresh[axis] = findPeakThresh(peakAvg[axis], trainPeakData[axis]);
		troughThresh[axis] = findTroughThresh(troughAvg[axis], trainTroughData[axis]);
		p2pThresh[axis] = findP2PThresh(trainPeakData[axis], trainTroughData[axis]);
		
		Log.d("MyApp","value is " + energyThreshLow[axis]);
		return;
	}

	public double[] findPeaks(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < xAxisStepDetector.LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev > data.get(i).getValue(stepAxis) || Next > data.get(i).getValue(stepAxis))
					break; // not a true peak
				j++;
			}
			if (j == xAxisStepDetector.LOOKLENGTH && k < 60) {
				// found a peak
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	
	public double[] findTroughs(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < xAxisStepDetector.LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev < data.get(i).getValue(stepAxis) || Next < data.get(i).getValue(stepAxis))
					break; // not a true trough
				j++;
			}
			if (j == xAxisStepDetector.LOOKLENGTH && k < 60) {
				// found a trough
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	public double calculatePeakAverage(double[] data) {
		int i = 0;
		int index = 0;
		double avg = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(data));
		while (i<10 && i < data.length){
			avg += Collections.max(b).doubleValue();
			index = b.indexOf(Collections.max(b));
			b.set(index, (double) 0);
			i++;
		}
		return avg/i;
	}
	
	public double calculateTroughAverage(double[] data) {
		int i = 0;
		int index = 0;
		double avg = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(data));
		while (i<10 && i < data.length){
			avg += Collections.min(b).doubleValue();
			index = b.indexOf(Collections.min(b));
			b.set(index, (double) 0);
			i++;
		}
		return avg/i;
	}
	
	public double findPeakThresh(double avg, double[] peaks){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(peaks));
		result = (Collections.max(b).doubleValue() - avg);
		return result;
	}
	
	public double findTroughThresh(double avg, double[] troughs){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(troughs));
		result = (Math.abs(Collections.min(b).doubleValue()) - Math.abs(avg));
		return result;
	}
	
	public double findP2PThresh(double[] peaks, double[] troughs){
		double result = 0;
		int i = 0;
		int index = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(peaks));
		List<Double> c = Arrays.asList(ArrayUtils.toObject(troughs));
		while (i<9 && i < (peaks.length - 1) && i < troughs.length - 1){
			index = b.indexOf(Collections.max(b));
			b.set(index, (double) 0);
			index = c.indexOf(Collections.min(c));
			c.set(index, (double) 0);
			i++;
		}
		result = (Collections.max(b).doubleValue() - Collections.min(c).doubleValue());
				
		return result;
	}
	
	private void trainModel(){
		//TODO apply least squares model using WEKA and inputHight Variable
		
		try {
			modelSource = new DataSource("/mnt/sdcard/WEKA/ARFF_JAVA.arff");
			modelData = modelSource.getDataSet();
			modelData.setClassIndex(modelData.numAttributes() - 1);
			
			myLR.buildClassifier(modelData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void calculateStepSize(){
		try {
			
			// build file
			
			StringBuilder Builder = new StringBuilder();

			Builder.append(header);
			Builder.append(Double.toString(inputHeight));
			Builder.append(",");
			Builder.append(Double.toString(accelFFTfreq[Z_AXIS]));
			Builder.append(",");
			Builder.append("?");
			String text = Builder.toString();
		    File predFile = new File("/mnt/sdcard/WEKA/pred.arff");
	        try
	        {
	        	predFile.delete();
	        	predFile.createNewFile();
		        BufferedWriter buf = new BufferedWriter(new FileWriter(predFile, true)); 
		        buf.append(text);
		        buf.newLine();
		        buf.close();
	        } 
	        catch (IOException e)
	        {
	           // TODO Auto-generated catch block
	           e.printStackTrace();
	        }
			
			testSource = new DataSource("/mnt/sdcard/WEKA/pred.arff");
			test = testSource.getDataSet();
			test.setClassIndex(test.numAttributes() - 1);
			eval = new Evaluation(modelData);
			eval.evaluateModel(myLR, test);
			//step_length = (Double) eval.predictions().get(1);
			String step_length_str = eval.predictions().toString().substring(10, 16);
			step_length = Double.parseDouble(step_length_str);
			Log.d("MyApp","length is " + step_length_str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void calculateDistance() {
		distance += step_length;
		
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
		
		x_stepFreq.setText((cb_x_freq.isChecked() ? Double.toString(accelFFTfreq[X_AXIS]):"N.A."));
		y_stepFreq.setText((cb_y_freq.isChecked() ? Double.toString(accelFFTfreq[Y_AXIS]):"N.A."));
		z_stepFreq.setText((cb_z_freq.isChecked() ? Double.toString(accelFFTfreq[Z_AXIS]):"N.A."));
		x_stepEnergy.setText((cb_x_energy.isChecked() ? String.format("%.4f", accelEnergy[X_AXIS]):"N.A."));
		y_stepEnergy.setText((cb_y_energy.isChecked() ? String.format("%.4f", accelEnergy[Y_AXIS]):"N.A."));
		z_stepEnergy.setText((cb_z_energy.isChecked() ? String.format("%.4f", accelEnergy[Z_AXIS]):"N.A."));
		
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
		
		step_length_display.setText((cb_step_length.isChecked() ? Double.toString(step_length):"N.A."));
		thetaWRTN.setText((cb_thetaWRTN.isChecked() ? Float.toString(rotValues[0]):"N.A."));
		step_num_display.setText((cb_step_num.isChecked() ? Integer.toString(step_value):"N.A."));
		distance_display.setText(cb_distance_display.isChecked() ? String.format("%.4f", distance):"N.A.");
		
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





