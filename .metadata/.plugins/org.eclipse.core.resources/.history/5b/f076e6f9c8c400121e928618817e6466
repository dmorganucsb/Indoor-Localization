package ucsb.ece596.indoortrack;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * This app demonstrates the Mobiletuts+ tutorial
 * - Android SDK: Creating Custom Views
 * 
 * The app creates a custom view class, updating its appearance from
 * this main class.
 * The app also uses a layout file and the attrs XML file.
 * 
 * Sue Smith
 * January 2013
 */

public class MainActivity extends Activity implements SensorEventListener{

    private final static String TAG = "StepDetector";
    private float   mLimit = 4;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;
    
//    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();
	
	private SensorManager mSensorManager;
	private Sensor mSensor;
	public static float[] mValues;
	
    public static float[] mInclin;
    
    public static float[] mRot = new float[16];
    public static float[] mGrav = new float[3];
    public static float[] mGeom = new float[3];
    public static float[] zValues = new float[3];
	
	private int mStepValue;
	private float mDistValue;
	private Sensor mAccelerometer;
	private Sensor mMagnetic;
	private Sensor mGravity;

	//the custom view
	private SampleView myView;
/*
	private final SensorEventListener mListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            mValues = event.values;
            if (myView != null) myView.invalidate();
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
*/
	
    @Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        
		setContentView(R.layout.activity_main);

		//get reference to the custom view
		myView = (SampleView)findViewById(R.id.custView);
		
		mStepValue = 0;
		
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
		
		final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	TextView numberSteps = (TextView)findViewById( R.id.textView1);
            	numberSteps.setText(Integer.toString(0));
            	TextView Stepsdistance = (TextView)findViewById( R.id.textView2);
            	Stepsdistance.setText(Float.toString((float) 0.00));
            	myView.reset();
            }
        });
    }
    
	public void btnPressed(View view){

	}   
    
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  TextView numberSteps = (TextView)findViewById( R.id.textView1);
  	  numberSteps.setText(numberSteps.getText()); 
  	  System.out.println("CHAANGED");
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		TextView numberSteps = (TextView)findViewById( R.id.textView1);
		TextView Stepsdistance = (TextView)findViewById( R.id.textView2);
        Sensor sensor = event.sensor; 
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
                mValues = event.values;
                if (myView != null) myView.invalidate();
            }
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            	System.arraycopy(event.values, 0, mGeom, 0, 3);
            }
            if (sensor.getType() == Sensor.TYPE_GRAVITY) {
            	System.arraycopy(event.values, 0, mGrav, 0, 3);
            }
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            	//Log.d("MyActivity","this " + zValues[0]);
                float vSum = 0;
                for (int i=0 ; i<3 ; i++) {
                    final float v = mYOffset + event.values[i] * mScale[1];
                    vSum += v;
                }
                int k = 0;
                float v = vSum / 3;
                
                float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                if (direction == - mLastDirections[k]) {
                    // Direction changed
                    int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                    mLastExtremes[extType][k] = mLastValues[k];
                    float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);
                    
                    if (diff > mLimit) {
                        
                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                        boolean isNotContra = (mLastMatch != 1 - extType);
                        
                        mStepValue = Integer.parseInt((String) numberSteps.getText());
                        mDistValue = Float.parseFloat((String) Stepsdistance.getText());
                        
                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                            Log.i(TAG, "step");
                            //for (StepListener stepListener : mStepListeners) {
                            //    stepListener.onStep();
                            //}
                            numberSteps.setText(Integer.toString(++mStepValue));
                            Stepsdistance.setText(Float.toString((float) (mDistValue + .33)));
                            if (mGrav != null && mGeom != null) {
                            	boolean success = SensorManager.getRotationMatrix(mRot, mInclin, mGrav, mGeom);
	                    		if (success){
	                    			SensorManager.getOrientation(mRot, zValues);
	                    		}
                            }
                            myView.step(getApplicationContext(), zValues);
                            mLastMatch = extType;
                        }
                        else {
                            mLastMatch = -1;
                        }
                    }
                    mLastDiff[k] = diff;
                }
                mLastDirections[k] = direction;
                mLastValues[k] = v;
            }
        }
    }
    

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
}
