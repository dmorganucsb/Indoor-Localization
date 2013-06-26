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
	
	private static int ORDER = 2;
	private static double[] B_coeffs = {1, -8.66133120135126, 33.8379111594403, -78.5155814397813, 119.815727607323, -125.635905892562, 91.6674737604170, -45.9506609307247, 15.1439586368786, -2.96290103992494, 0.261309426400923};
	private static double[] A_coeffs = {8.40968636959052e-11, 8.40968636959052e-10, 3.78435886631574e-09, 1.00916236435086e-08, 1.76603413761401e-08, 2.11924096513681e-08, 1.76603413761401e-08, 1.00916236435086e-08, 3.78435886631574e-09, 8.40968636959052e-10, 8.40968636959052e-11};
	
	public final static int X_AXIS     = 0;
	public final static int Y_AXIS     = 1;
	public final static int Z_AXIS     = 2;
	
	private ArrayList<AccelData> sensorData;
	private ArrayList<AccelData> linData;
	private ArrayList<AccelData> filtlinData;
	//private ArrayList<AccelData> gravData;
	//private ArrayList<AccelData> gyroData;
	private ArrayList<AccelData> plotData;
	private float[] linVals;//, gravVals, gyroVals;
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
		filtlinData = new ArrayList<AccelData>();
		//gravData = new ArrayList<AccelData>();
		//gyroData = new ArrayList<AccelData>();
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
            		openChart(Sensor.TYPE_LINEAR_ACCELERATION, linData, toggle);
            		break;
            	case 1:
            		//openChart(Sensor.TYPE_GRAVITY, gravData);
            		filtlinData.clear();
            		filter(linData, filtlinData);
            		openChart(Sensor.TYPE_LINEAR_ACCELERATION, filtlinData, toggle);
            		break;
            	case 2:
            		//openChart(Sensor.TYPE_GYROSCOPE, gyroData);
            		break;
            	default:
            		break;
            	}
            	
            	toggle = toggle + 1;
            	if (toggle == 2)
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
							openChart(type, plotData, toggle);
						if (toggle == 1){
							filtlinData.clear();
							 filter(plotData, filtlinData);
							openChart(type, filtlinData, toggle);
						}
						break;
		  			}
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
			sensorData = new ArrayList<AccelData>();
			// save prev data if available
			started = true;
			
			Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			sensorManager.registerListener(this, accel, 500000);
			Sensor grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
			sensorManager.registerListener(this, grav, 500000);
			Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			sensorManager.registerListener(this, gyro, 500000);
			
			break;
		case R.id.btnStop:
			started = false;
			sensorManager.unregisterListener(this);

			// show data in chart
			break;
		default:
			break;
		}

	}

	private void openChart(int type, ArrayList<AccelData> _data, int toggle) {
		sensorData = _data;
		if (sensorData != null || sensorData.size() > 0) {
			
			layout.removeAllViews();
			
			long t = sensorData.get(0).getTimestamp();
			XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

			XYSeries zSeries = new XYSeries("Z");

			for (AccelData data : sensorData) {
				zSeries.add(data.getTimestamp() - t, data.getValue(Z_AXIS));
			}

			dataset.addSeries(zSeries);

			XYSeriesRenderer zRenderer = new XYSeriesRenderer();
			zRenderer.setColor(Color.BLUE);
			zRenderer.setPointStyle(PointStyle.CIRCLE);
			zRenderer.setFillPoints(true);
			zRenderer.setLineWidth(1);
			zRenderer.setDisplayChartValues(false);

			XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
			multiRenderer.setXLabels(0);
			multiRenderer.setLabelsColor(Color.RED);
			switch(toggle){
			case 0:
				multiRenderer.setChartTitle("unfiltered");
				break;
			case 1:
				multiRenderer.setChartTitle("filtered");
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
			
			multiRenderer.addSeriesRenderer(zRenderer);
			
			// Creating a Line Chart
			mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
					multiRenderer);

			// Adding the Line Chart to the LinearLayout
			layout.addView(mChart);
		}
		else
			Toast.makeText(this, "null data", Toast.LENGTH_LONG).show();
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
