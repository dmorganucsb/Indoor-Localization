package com.smartapps.accel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.apache.commons.lang3.ArrayUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends Activity implements SensorEventListener,
		OnClickListener {
	private SensorManager sensorManager;
	private Button btnStart, btnStop, btnToggle, btnUpload, btnReset;
	private TextView StepFreq;
	private boolean started = false;

	private static int ORDER = 10;
	private static double[] A_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private static double[] B_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};

	public final static int X_AXIS     = 0;
	public final static int Y_AXIS     = 1;
	public final static int Z_AXIS     = 2;
	
	private ArrayList<AccelData> sensorData;
	private ArrayList<AccelData> linData;
	private ArrayList<AccelData> gravData;
	private ArrayList<AccelData> gyroData;
	private ArrayList<AccelData> plotData;
	private float[] linVals, gravVals, gyroVals;
	DoubleFFT_1D fftlib = new DoubleFFT_1D(512);

	private LinearLayout layout;
	private View mChart;

	private int toggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		layout = (LinearLayout) findViewById(R.id.chart_container);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorData = new ArrayList<AccelData>();
		linData = new ArrayList<AccelData>();
		gravData = new ArrayList<AccelData>();
		gyroData = new ArrayList<AccelData>();
		plotData = new ArrayList<AccelData>();
		toggle = 0;

		Log.d("MyApp","Upload pressed");

		btnStart = (Button) findViewById(R.id.btnStart);
		btnStop = (Button) findViewById(R.id.btnStop);
		btnToggle = (Button) findViewById(R.id.btnToggle);
		btnUpload = (Button) findViewById(R.id.btnUpload);
		btnReset = (Button) findViewById(R.id.btnReset);
		StepFreq = (TextView)findViewById( R.id.textView1);
		btnStart.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnUpload.setOnClickListener(this);
		btnReset.setOnClickListener(this);
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
		btnToggle.setEnabled(true);
		if (sensorData == null || sensorData.size() == 0) {
			btnUpload.setEnabled(false);
			btnReset.setEnabled(false);
		}

		btnToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	switch(toggle){
            	case 0: //accel chart
            		openChart(Sensor.TYPE_LINEAR_ACCELERATION, linData);
            		break;
            	case 1:
            		openChart(Sensor.TYPE_GRAVITY, gravData);
            		break;
            	case 2:
            		openChart(Sensor.TYPE_GYROSCOPE, gyroData);
            		break;
            	default:
            		break;
            	}
            	
            	toggle = toggle + 1;
            	if (toggle == 3)
            		toggle = 0;
            }
        });

	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (started == true) {
			sensorManager.unregisterListener(this);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		int type = sensor.getType();
		if (started) {
			plotData.clear();
			long timestamp = System.currentTimeMillis();
			switch(type){
				case Sensor.TYPE_LINEAR_ACCELERATION:
					linVals = event.values.clone();
					linData.add(new AccelData(timestamp, linVals[0], linVals[1], linVals[2]));
		  			if (linData.size() > 50){
			  	        int size = linData.size();
			  	        for(int i=(size-51);i<size;i++)
			  	        {
			  	            plotData.add(linData.get(i));
			  	        }
						if (toggle == 0)
							openChart(type, plotData);
						break;
		  			}
					if (toggle == 0)
						openChart(type, linData);
					break;
				case Sensor.TYPE_GRAVITY:
					gravVals = lowPass( event.values.clone(), gravVals );
					gravData.add(new AccelData(timestamp, gravVals[0], gravVals[1], gravVals[2]));
		  			if (gravData.size() > 50){
			  	        int size = gravData.size();
			  	        for(int i=(size-51);i<size;i++)
			  	        {
			  	            plotData.add(gravData.get(i));
			  	        }
						if (toggle == 1)
							openChart(type, plotData);
						break;
		  			}
					if (toggle == 1)
						openChart(type, gravData);
					break;
				case Sensor.TYPE_GYROSCOPE:
					gyroVals = lowPass( event.values.clone(), gyroVals );
					gyroData.add(new AccelData(timestamp, gyroVals[0], gyroVals[1], gyroVals[2]));
		  			if (gyroData.size() > 50){
			  	        int size = gyroData.size();
			  	        for(int i=(size-51);i<size;i++)
			  	        {
			  	            plotData.add(gyroData.get(i));
			  	        }
						if (toggle == 2)
							openChart(type, plotData);
						break;
		  			}
					if (toggle == 2)
						openChart(type, gyroData);
					break;
				default:
					break;
			}
		}

	}

	@SuppressLint("WorldWriteableFiles")
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnStart:
			btnStart.setEnabled(false);
			btnStop.setEnabled(true);
			btnUpload.setEnabled(false);
			btnReset.setEnabled(false);
			sensorData = new ArrayList<AccelData>();
			// save prev data if available
			started = true;

			Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			sensorManager.registerListener(this, accel, 10000);
			Sensor grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
			sensorManager.registerListener(this, grav, 10000);
			Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			sensorManager.registerListener(this, gyro, 10000);

			break;
		case R.id.btnStop:
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
			btnUpload.setEnabled(true);
			btnReset.setEnabled(true);
			started = false;
			sensorManager.unregisterListener(this);

			// show data in chart
			break;
		case R.id.btnReset:
			plotData.clear();
			sensorData.clear();
			linData.clear();
			gravData.clear();
			gyroData.clear();
			plotData.clear();
			layout.removeAllViews();
			StepFreq.setText("0");
			break;
		case R.id.btnUpload:
			Log.d("MyApp","Upload pressed");
			//Create a file to save the final result
		    File logFile = new File("/mnt/sdcard/log.file");
	        try
	        {
	           logFile.delete();
	           logFile.createNewFile();
	        } 
	        catch (IOException e)
	        {
	           // TODO Auto-generated catch block
	           e.printStackTrace();
	        }
	        StringBuilder Builder = new StringBuilder();
		    double[] xValues;
		    double[] yValues;
		    double[] zValues;

			//********************************Acceleromter Section***********************************************
			//convert Arraylist<Double> to double[]
		    xValues = new double[linData.size()];
		    yValues = new double[linData.size()];
		    zValues = new double[linData.size()];
		    for (int i=0; i < xValues.length; i++)
		    {
		        xValues[i] = linData.get(i).getValue(MainActivity.X_AXIS);
		        yValues[i] = linData.get(i).getValue(MainActivity.Y_AXIS);
		        zValues[i] = linData.get(i).getValue(MainActivity.Z_AXIS);
		    }

		    //convert double[] toString with comma seperators
			Builder.append("Accelerometer X-Data = ");
			for (double i : xValues) {
			  Builder.append(i);
			  Builder.append(",");
			}
			Builder.append("\n");
			Builder.append("Accelerometer Y-Data = ");
			for (double i : yValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
			Builder.append("\n");
			Builder.append("Accelerometer Z-Data = ");
			for (double i : zValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
			Builder.append("\n");
			Builder.append("\n");
			//*********************************Gyroscope Section*********************************************************
			//convert Arraylist<Double> to double[]
		    xValues = new double[gyroData.size()];
		    yValues = new double[gyroData.size()];
		    zValues = new double[gyroData.size()];
		    for (int i=0; i < xValues.length; i++)
		    {
		        xValues[i] = gyroData.get(i).getValue(MainActivity.X_AXIS);
		        yValues[i] = gyroData.get(i).getValue(MainActivity.Y_AXIS);
		        zValues[i] = gyroData.get(i).getValue(MainActivity.Z_AXIS);
		    }

		    //convert double[] toString with comma seperators
			Builder.append("GyroScope X-Data = ");
			for (double i : xValues) {
			  Builder.append(i);
			  Builder.append(",");
			}
			Builder.append("\n");
			Builder.append("GyroScope Y-Data = ");
			for (double i : yValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
			Builder.append("\n");
			Builder.append("GyroScope Z-Data = ");
			for (double i : zValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
			Builder.append("\n");
			Builder.append("\n");
			//*********************************Gravity Section*********************************************************
			//convert Arraylist<Double> to double[]
		    xValues = new double[gravData.size()];
		    yValues = new double[gravData.size()];
		    zValues = new double[gravData.size()];
		    for (int i=0; i < xValues.length; i++)
		    {
		        xValues[i] = gravData.get(i).getValue(MainActivity.X_AXIS);
		        yValues[i] = gravData.get(i).getValue(MainActivity.Y_AXIS);
		        zValues[i] = gravData.get(i).getValue(MainActivity.Z_AXIS);
		    }

		    //convert double[] toString with comma seperators
			Builder.append("Gravity X-Data = ");
			for (double i : xValues) {
			  Builder.append(i);
			  Builder.append(",");
			}
			Builder.append("\n");
			Builder.append("Gravity Y-Data = ");
			for (double i : yValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
			Builder.append("\n");
			Builder.append("Gravity Z-Data = ");
			for (double i : zValues) {
				  Builder.append(i);
				  Builder.append(",");
				}
//**********************************************************End String Builder************************************
			String text = Builder.toString();

		     try
		     {
		        //BufferedWriter for performance, true to set append to file flag
		        BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		        buf.append(text);
		        buf.newLine();
		        buf.close();
		        Toast toast = Toast.makeText(getApplicationContext(), "file saved", Toast.LENGTH_SHORT);
		        toast.show();
		     }
		     catch (IOException e)
		     {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		     }
			break;
		default:
			break;
		}

	}

	private void openChart(int type, ArrayList<AccelData> _data) {
		sensorData = _data;
		if (sensorData != null || sensorData.size() > 0) {

			layout.removeAllViews();

			long t = sensorData.get(0).getTimestamp();
			XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

			XYSeries xSeries = new XYSeries("X");
			XYSeries ySeries = new XYSeries("Y");
			XYSeries zSeries = new XYSeries("Z");

			for (AccelData data : sensorData) {
				xSeries.add(data.getTimestamp() - t, data.getValue(MainActivity.X_AXIS));
				ySeries.add(data.getTimestamp() - t, data.getValue(MainActivity.Y_AXIS));
				zSeries.add(data.getTimestamp() - t, data.getValue(MainActivity.Z_AXIS));
			}

			dataset.addSeries(xSeries);
			dataset.addSeries(ySeries);
			dataset.addSeries(zSeries);

			XYSeriesRenderer xRenderer = new XYSeriesRenderer();
			xRenderer.setColor(Color.RED);
			xRenderer.setPointStyle(PointStyle.CIRCLE);
			xRenderer.setFillPoints(true);
			xRenderer.setLineWidth(1);
			xRenderer.setDisplayChartValues(false);

			XYSeriesRenderer yRenderer = new XYSeriesRenderer();
			yRenderer.setColor(Color.GREEN);
			yRenderer.setPointStyle(PointStyle.CIRCLE);
			yRenderer.setFillPoints(true);
			yRenderer.setLineWidth(1);
			yRenderer.setDisplayChartValues(false);

			XYSeriesRenderer zRenderer = new XYSeriesRenderer();
			zRenderer.setColor(Color.BLUE);
			zRenderer.setPointStyle(PointStyle.CIRCLE);
			zRenderer.setFillPoints(true);
			zRenderer.setLineWidth(1);
			zRenderer.setDisplayChartValues(false);

			XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
			multiRenderer.setXLabels(0);
			multiRenderer.setLabelsColor(Color.RED);
			switch(type){
			case Sensor.TYPE_LINEAR_ACCELERATION:
				multiRenderer.setChartTitle("t vs Linear Accel(x,y,z)");
				break;
			case Sensor.TYPE_GRAVITY:
				multiRenderer.setChartTitle("t vs Gravity(x,y,z)");
				break;
			case Sensor.TYPE_GYROSCOPE:
				multiRenderer.setChartTitle("t vs Gyroscope(x,y,z)");
				break;
			default:
				multiRenderer.setChartTitle("Error");
			}
			multiRenderer.setXTitle("Sensor Data");
			multiRenderer.setYTitle("Values");
			multiRenderer.setZoomButtonsVisible(true);
			for (int i = 0; i < sensorData.size(); i++) {

				multiRenderer.addXTextLabel(i + 1, ""
						+ (sensorData.get(i).getTimestamp() - t));
			}
			for (int i = 0; i < 12; i++) {
				multiRenderer.addYTextLabel(i + 1, ""+i);
			}

			multiRenderer.addSeriesRenderer(xRenderer);
			multiRenderer.addSeriesRenderer(yRenderer);
			multiRenderer.addSeriesRenderer(zRenderer);

			// Creating a Line Chart
			mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
					multiRenderer);

			// Adding the Line Chart to the LinearLayout
			layout.addView(mChart);

			double myFFT[];
			myFFT = new double[512];

  			if (linData.size() > 512){
	  	        int size = linData.size();
	  	        for(int i=0;i<512;i++)
	  	        {
	  	            myFFT[i] = linData.get(i+size-512).getValue(MainActivity.Z_AXIS);
	  	        }
	  	        fftlib.realForward(myFFT);
	  	        for (int i=0;i<512;i++){
	  	        	myFFT[i] = Math.abs(myFFT[i]);
	  	        }
	  	        myFFT[0] = 0; myFFT[1] = 0; myFFT[2] = 0;//remove dc?
	        	List<Double> b = Arrays.asList(ArrayUtils.toObject(myFFT));
	            double freq = (double) b.indexOf(Collections.max(b)) * 50 / (2 * myFFT.length);
	            StepFreq.setText(Double.toString(freq));
  			}

		}
	}

	static final float ALPHA = 0.15f;

	protected float[] lowPass( float[] input, float[] output ) {
	    if ( output == null ) return input;

	    for ( int i=0; i<input.length; i++ ) {
	        output[i] = output[i] + ALPHA * (input[i] - output[i]);
	    }
	    return output;
	}

	public static ArrayList<AccelData> filter(ArrayList<AccelData> accData, ArrayList<AccelData> filtData){ // implements a 10th order butterworth filter (coefficients created in MATLAB)
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
				y_filt[n] = y_filt[n]/A_coeffs[0];
			}
			for (int i=0;i<y_filt.length;i++)
				filtData.add(new AccelData(accData.get(i).getTimestamp(), 0, 0, y_filt[i]));
		//}
		return filtData;
	}

}