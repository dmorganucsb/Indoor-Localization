package com.ece596.ucsb.localization;

import java.util.ArrayList;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity  implements SensorEventListener, OnClickListener {
	
	//sensor Manager
	private SensorManager sensorManager;
	
	// Step Detection Classes
	//TODO Combine each axis step detection into a single class for all axes?
	StepDetector xAxisStepDetector = new StepDetector(X_AXIS);
	StepDetector yAxisStepDetector = new StepDetector(Y_AXIS);
	StepDetector zAxisStepDetector = new StepDetector(Z_AXIS);
	
	// preferences Data
	public static boolean useFilter = false;
	
	public enum Axes {
		X_AXIS(0),
		Y_AXIS(1),
		Z_AXIS(2);
		
		private final int value;
		
		private Axes(final int newValue){
			value = newValue;
		}
		
		public int getValue() {return value; }
	}

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
	private static double[] accelFFTfreq = {0,0,0};
	private static double[] prevAccelFFTfreq = {0,0,0};
	//private static double[] gyroFFTfreq = {0,0,0};
	//private static double[] prevGyroFFTfreq = {0,0,0};
	
	// Array size limitation for sensor data
	private final static int ARRAY_SIZE = 512;
	public final static int X_AXIS     = 0;
	public final static int Y_AXIS     = 1;
	public final static int Z_AXIS     = 2;
	
	// data arrays
	private ArrayList<AccelData> accData = new ArrayList<AccelData>();  // Accelerometer data array
	private ArrayList<AccelData> trainaccData = new ArrayList<AccelData>();  // Accelerometer training data array 
	//private ArrayList<AccelData> gyroData; // Gyroscope data array 
	//private ArrayList<AccelData> filteredgyroData; // Gyroscope data array 
	//private ArrayList<AccelData> gravData; // Gravity data array 

	// Sensor Energy Data
	private double[] accelEnergy = new double[3];
	//private double[] gyroEnergy = new double[3];
	private final static int NOACTIVITY = 4;
	private double EHIGH = 2.35;
	private double ELOW = 0.15;
	
	//value display refresh rate
	private final int REFRESH_RATE 		= 2;                // rate of value/screen updates per second
	private final static double SENSOR_DELAY 	= .01;              // sensor delay in seconds 
	public final static double Fs		   		= 1/SENSOR_DELAY;   //sampling rate of sensor data in Hz
	private int counter 		   		= 0;                //counter for refresh rate
	
	//display variable
	private TextView x_stepFreq;    		//display for x frequency
	private TextView y_stepFreq;    		//display for y frequency
	private TextView z_stepFreq;    		//display for z frequency
	//private TextView x_stepEnergy;  		//display for X axis Energy Detection
	//private TextView y_stepEnergy;  		//display for Y axis Energy Detection
	//private TextView z_stepEnergy;  		//display for Z axis Energy Detection
	private TextView step_num_display;
	private TextView step_length_display;   // display for step length
	private TextView thetaWRTN;     		//display for angle
	private TextView avgthetaWRTN;     		//display for angle
	private TextView distance_display;
	
	//display checkboxes
	private CheckBox cb_x_freq;
	private CheckBox cb_y_freq;
	private CheckBox cb_z_freq;
	//private CheckBox cb_x_energy;
	//private CheckBox cb_y_energy;
	//private CheckBox cb_z_energy;
	private CheckBox cb_step_num;
	private CheckBox cb_step_length;
	private CheckBox cb_thetaWRTN;
	private CheckBox cb_avg_thetaWRTN;
	private CheckBox cb_distance_display;
	
	
	// values to compute orientation
    public static float[] mGrav = new float[3];
    public static float[] mGeom = new float[3];;
	
    // variables for pedometer
    private static int step_value = 0;
    private double step_length = 0;
    private double distance = 0;
	public final static double FREQTHRESH = 0.3;
	
	//two control buttons
	private Button reset_btn, train_btn;
	
	public static FragmentManager fm;
    public TrainDataDialog enterHeightDialog = new TrainDataDialog();
    public static CalibrationDialog calibrateSteps = new CalibrationDialog();
    public static boolean calibration_inProgress = false;
    private double inputHeight = 0;
    private static boolean timeout = false;
    private int timeoutCount = 0;
    
    public static float[] mValues = new float[3];
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		//******************initialization***********************
		fm = getSupportFragmentManager();
		setupDisplays();
		StepLength.trainModel();
		
		this.myOnClick(null, R.id.train_btn);
	}
		
	public void startPedometer() {
		// Inflate the menu; this adds items to the action bar if it is present.
		xAxisStepDetector.setThreshVariables(CalibrationData.peakAvg[X_AXIS], CalibrationData.peakThresh[X_AXIS]/*+wiggleRoom*/, CalibrationData.troughAvg[X_AXIS], CalibrationData.troughThresh[X_AXIS]/*+wiggleRoom*/, CalibrationData.p2pThresh[X_AXIS]/*-wiggleRoom*/); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		yAxisStepDetector.setThreshVariables(CalibrationData.peakAvg[Y_AXIS], CalibrationData.peakThresh[Y_AXIS]/*+wiggleRoom*/, CalibrationData.troughAvg[Y_AXIS], CalibrationData.troughThresh[Y_AXIS]/*+wiggleRoom*/, CalibrationData.p2pThresh[Y_AXIS]/*-wiggleRoom*/); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh
		zAxisStepDetector.setThreshVariables(CalibrationData.peakAvg[Z_AXIS], CalibrationData.peakThresh[Z_AXIS]/*+wiggleRoom*/, CalibrationData.troughAvg[Z_AXIS], CalibrationData.troughThresh[Z_AXIS]/*+wiggleRoom*/, CalibrationData.p2pThresh[Z_AXIS]/*-wiggleRoom*/); // initPeakAvg peakThresh inittroughAvg troughThresh, diffThresh

		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch (item.getItemId()) 
	    {
	        case R.id.settings_title:
	            startActivity(new Intent(this, Prefs.class));
	            return true;
	        case R.id.exit_title:
	            finish();
	            return true;
	    }
	  return false;
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
        Toast.makeText(this, "Calibration started", Toast.LENGTH_SHORT).show();
        calibration_inProgress = CalibrationData.initCalibration(trainaccData);
        calibrateSteps.show(fm, "fragment_calibrate_steps");

	}
	
	public void finishCalibration() {
		calibration_inProgress = CalibrationData.finalizeCalibration(trainaccData);
        Toast.makeText(this, "Calibration finished", Toast.LENGTH_SHORT).show();      
        startPedometer();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), (int) (SENSOR_DELAY*1000000)); //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), (int) (SENSOR_DELAY*10000000)); 	 		 //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), (int) (SENSOR_DELAY*10000000));     //convert from seconds to micro seconds
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);     //convert from seconds to micro seconds
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
				long timestampAcc = System.currentTimeMillis();
				accData.add(new AccelData(timestampAcc, event.values[X_AXIS], event.values[Y_AXIS], event.values[Z_AXIS]));
				if (accData.size() > ARRAY_SIZE)
					accData.remove(1);
				if (useFilter) // TODO verify filter functionality 
					accData = PedometerUtilities.filter(accData);  //Xth order butterworth filter
				//***************************************************************************
				
				//****************************Store Calibration Data*****************************
				//TODO change this so that new data goes straight to trainaccData not to accData -> trainaccData
				if (calibration_inProgress){
					trainaccData.add(new AccelData(timestampAcc, accData.get(accData.size()-1).getValue(X_AXIS), 
							accData.get(accData.size()-1).getValue(Y_AXIS), 
							accData.get(accData.size()-1).getValue(Z_AXIS)));
					CalibrationData.updateEcalib(trainaccData, type);
					break;
				}
				//*******************************************************************************
				
				//*****************************Energy Section********************************
				// get energy
				int axisMaxE = 0;
				accelEnergy = PedometerUtilities.findMaxEnergy(accData, type);
				// find max energy axis
				if (accelEnergy[X_AXIS] < accelEnergy[Y_AXIS] || accelEnergy[X_AXIS] < accelEnergy[Z_AXIS])
					axisMaxE = (accelEnergy[Y_AXIS] > accelEnergy[Z_AXIS] ? Y_AXIS:Z_AXIS);
				else
					axisMaxE = X_AXIS;
				
				if (accelEnergy[axisMaxE] < CalibrationData.energyThreshLow[axisMaxE])
					axisMaxE = NOACTIVITY;
				//****************************************************************************
				
				//*****************************FFT SECTION************************************
				// hardcoded to ignore frequencies higher than 3.2 hertz (should be a static variable?)
				//TODO add variable for max frequency allowed
				// get X FFT
				accelFFTfreq[X_AXIS] = (PedometerUtilities.calculateFFT(type, X_AXIS, accData));
				accelFFTfreq[X_AXIS] = (accelFFTfreq[X_AXIS] > 3.2 ? prevAccelFFTfreq[X_AXIS]:accelFFTfreq[X_AXIS]);
				prevAccelFFTfreq[X_AXIS] = accelFFTfreq[X_AXIS];
				// get Y FFT
				accelFFTfreq[Y_AXIS] = (PedometerUtilities.calculateFFT(type, Y_AXIS, accData));
				accelFFTfreq[Y_AXIS] = (accelFFTfreq[Y_AXIS] > 3.2 ? prevAccelFFTfreq[Y_AXIS]:accelFFTfreq[Y_AXIS]);
				prevAccelFFTfreq[Y_AXIS] = accelFFTfreq[Y_AXIS];
				// get Z FFT
				accelFFTfreq[Z_AXIS] = (PedometerUtilities.calculateFFT(type, Z_AXIS, accData));
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
							//***********Step Size Estimation and distance calculation***********************
							 //calculateStepSize(); //using current step frequency and hieght parameter
							 updateDistance();
							//*******************************************************************************
						}
					}
				}
				//*******************************************************************************
				
				//****************************Step detection timeout period**********************
				// this should be hardcoded to limit multiple steps as well?
				//TODO change this to some variable (related to preiod of FFT somehow?)
				if (timeoutCount != 0)
					timeoutCount--;
				else
					timeout = false;
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
				//****************************Data accumulation******************************
				//long timestampGyro = System.currentTimeMillis();
				//gyroData.add(new AccelData(timestampGyro, event.values[X_AXIS], event.values[Y_AXIS], event.values[Z_AXIS]));
				//if (gyroData.size() > ARRAY_SIZE)
				//	gyroData.remove(1);
				//filter();  //10th order butterworth filter
				//***************************************************************************
				
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				// TODO can be used to help step detection?
	            System.arraycopy(event.values, 0, mGeom, 0, 3);
	            PedometerUtilities.getTheta(mGrav, mGeom);
	    		if (cb_avg_thetaWRTN.isChecked())
	    			PedometerUtilities.getAvgTheta();
	    		else{
	    			PedometerUtilities.count = 0;
	    			PedometerUtilities.first = true;
	    		}
	    		
				break;
			case Sensor.TYPE_ORIENTATION:
	            mValues = event.values;
	            break;
			default:
				break;
		}
		
	}

	private void updateDistance() {
		distance += StepLength.calculateStepSize(inputHeight, accelFFTfreq);
		
		return;
	}

	private void setupDisplays(){
		//display data
		x_stepFreq = (TextView)findViewById( R.id.x_freq_display);
		y_stepFreq = (TextView)findViewById( R.id.y_freq_display);
		z_stepFreq = (TextView)findViewById( R.id.z_freq_display);
		//x_stepEnergy = (TextView)findViewById( R.id.x_energy_display);
		//y_stepEnergy = (TextView)findViewById( R.id.y_energy_display);
		//z_stepEnergy = (TextView)findViewById( R.id.z_energy_display);
		thetaWRTN = (TextView)findViewById(R.id.theta_display);
		avgthetaWRTN = (TextView)findViewById(R.id.avg_theta_display);
		step_num_display = (TextView)findViewById(R.id.step_num_display);
		step_length_display = (TextView)findViewById(R.id.step_length_display);
		distance_display = (TextView)findViewById(R.id.distance_display);
		
		//checkboxes for display data
		cb_x_freq = (CheckBox) findViewById(R.id.cb_x_freq_display);
		//cb_x_energy = (CheckBox) findViewById(R.id.cb_x_energy_display);
		cb_step_num = (CheckBox) findViewById(R.id.cb_step_num_display);
		cb_step_length = (CheckBox) findViewById(R.id.cb_step_length_display);
		cb_y_freq = (CheckBox) findViewById(R.id.cb_y_freq_display);
		//cb_y_energy = (CheckBox) findViewById(R.id.cb_y_energy_display);
		cb_thetaWRTN = (CheckBox) findViewById(R.id.cb_theta_display);
		cb_avg_thetaWRTN = (CheckBox) findViewById(R.id.cb_avg_theta_display);
		cb_z_freq = (CheckBox) findViewById(R.id.cb_z_freq_display);
		//cb_z_energy = (CheckBox) findViewById(R.id.cb_z_energy_display);
		cb_distance_display = (CheckBox) findViewById(R.id.cb_distance_display);
		
		reset_btn = (Button) findViewById(R.id.reset_btn);
		reset_btn.setOnClickListener(this);
		train_btn = (Button) findViewById(R.id.train_btn);
		train_btn.setOnClickListener(this);
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
		//x_stepEnergy.setText((cb_x_energy.isChecked() ? String.format("%.4f", accelEnergy[X_AXIS]):"N.A."));
		//y_stepEnergy.setText((cb_y_energy.isChecked() ? String.format("%.4f", accelEnergy[Y_AXIS]):"N.A."));
		//z_stepEnergy.setText((cb_z_energy.isChecked() ? String.format("%.4f", accelEnergy[Z_AXIS]):"N.A."));
		
		resetColors();
		
		switch(primary_axis){
			case X_AXIS:
				x_stepFreq.setTextColor(Color.GREEN);
				//x_stepEnergy.setTextColor(Color.GREEN);
				break;
			case Y_AXIS:
				y_stepFreq.setTextColor(Color.GREEN);
				//y_stepEnergy.setTextColor(Color.GREEN);
				break;
			case Z_AXIS:
				z_stepFreq.setTextColor(Color.GREEN);
				//z_stepEnergy.setTextColor(Color.GREEN);
				break;
			default:
				resetColors();
				break;
		}
		
		step_length_display.setText((cb_step_length.isChecked() ? Double.toString(step_length):"N.A."));
		thetaWRTN.setText((cb_thetaWRTN.isChecked() ? Float.toString(PedometerUtilities.rotValueGyro/*rotValues[0]*/):"N.A."));
		avgthetaWRTN.setText(Float.toString(PedometerUtilities.avgrotValue));
		step_num_display.setText((cb_step_num.isChecked() ? Integer.toString(step_value):"N.A."));
		distance_display.setText(cb_distance_display.isChecked() ? String.format("%.4f", distance):"N.A.");
		
		return;
	}
	
	private void resetColors(){
		
		x_stepFreq.setTextColor(Color.WHITE);
		//x_stepEnergy.setTextColor(Color.WHITE);
		y_stepFreq.setTextColor(Color.WHITE);
		//y_stepEnergy.setTextColor(Color.WHITE);
		z_stepFreq.setTextColor(Color.WHITE);
		//z_stepEnergy.setTextColor(Color.WHITE);
		
		return;
	}
	
}
