package com.ece596.ucsb.localization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class StepLength {
	
	//WEKA Library variables
	//Classifier myLeastSquares;
	static LinearRegression myLR;
	static Evaluation eval;
	static DataSource modelSource;
	static Instances modelData;
	static DataSource testSource;
	static Instances test;
	static final String header = "@relation Step-Size\n\n@attribute Hieght real\n@attribute freq_times_height real\n@attribute Step_size real\n\n@data\n";
	
	public static void initModel(){
		//Weka Libraries
		try {
			myLR = new LinearRegression();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void trainModel(){
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
	
	public static double calculateStepSize(double inputHeight, double[] accelFFTfreq){
		double step_length = 0;
		try {
			
			// build file
			
			StringBuilder Builder = new StringBuilder();

			Builder.append(header);
			Builder.append(Double.toString(inputHeight));
			Builder.append(",");
			Builder.append(Double.toString(accelFFTfreq[MainActivity.Z_AXIS]));
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
		return step_length;
	}
	
}