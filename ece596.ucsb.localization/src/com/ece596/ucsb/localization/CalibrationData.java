package com.ece596.ucsb.localization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import android.util.Log;

public class CalibrationData {
	
	// Sensor Energy Data
	public static double[] energyThreshLow = new double[3];
	public static double[] energyThreshHigh = new double[3];
	private static final int LOOKLENGTH = 20;
	private static double[][] trainPeakData = new double[3][20]; // array for calibration Data
	private static double[][] trainTroughData = new double[3][20]; // array for calibration Data
	public static double[] peakAvg = new double[3];
	public static double[] troughAvg = new double[3];
	public static double[] peakThresh = new double[3];
	public static double[] troughThresh = new double[3];
	public static double[] p2pThresh = new double[3];
	
	private static double[] calibE = new double[3];
	private final static double wiggleRoom = .2;
	
	public static boolean initCalibration(ArrayList<AccelData> trainData) {	
        trainData.clear();
        for (MainActivity.Axes AXIS : MainActivity.Axes.values()){
        	energyThreshLow[AXIS.getValue()] = 10;
        	energyThreshHigh[AXIS.getValue()] = 0;
        }
        return true;
	}
	
	public static boolean finalizeCalibration(ArrayList<AccelData> trainData) {
		for (MainActivity.Axes AXIS : MainActivity.Axes.values())
        	extractCalibratedData(AXIS.getValue(), trainData);
        return false;
	}
	
	public static void updateEcalib(ArrayList<AccelData> trainData, int type){
		calibE = PedometerUtilities.findMaxEnergy(trainData, type);
		for (int i = 0;i<3;i++){
			if (calibE[i] + wiggleRoom/2 > CalibrationData.energyThreshHigh[i] + wiggleRoom/2)
				CalibrationData.energyThreshHigh[i] = calibE[i] + wiggleRoom/2;
			if (calibE[i] != 0 && calibE[i] < CalibrationData.energyThreshLow[i])
				CalibrationData.energyThreshLow[i] = calibE[i];
		}
	}
	
	private static void extractCalibratedData(int axis, ArrayList<AccelData> trainData){
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

	public static double[] findPeaks(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev > data.get(i).getValue(stepAxis) || Next > data.get(i).getValue(stepAxis))
					break; // not a true peak
				j++;
			}
			if (j == LOOKLENGTH && k < 60) {
				// found a peak
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	public static double[] findTroughs(ArrayList<AccelData> data, int stepAxis) {
		double Prev = 0;
		double Next = 0;
		double[] result = new double[60];
		int k = 0;
		for (int i=0;i<data.size();i++){
			int j = 1;
			while (j < LOOKLENGTH && (i - j) >= 0 && (i + j) < data.size()) {
				Prev = data.get(i - j).getValue(stepAxis);
				Next = data.get(i + j).getValue(stepAxis);
				if (Prev < data.get(i).getValue(stepAxis) || Next < data.get(i).getValue(stepAxis))
					break; // not a true trough
				j++;
			}
			if (j == LOOKLENGTH && k < 60) {
				// found a trough
				result[k] = data.get(i).getValue(stepAxis); // store the supposed peak
				k++;
			}
		}
		return result;
	}
	
	public static double calculatePeakAverage(double[] data) {
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
	
	public static double calculateTroughAverage(double[] data) {
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
	
	public static double findPeakThresh(double avg, double[] peaks){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(peaks));
		result = (Collections.max(b).doubleValue() - avg);
		return result;
	}
	
	public static double findTroughThresh(double avg, double[] troughs){
		double result = 0;
		List<Double> b = Arrays.asList(ArrayUtils.toObject(troughs));
		result = (Math.abs(Collections.min(b).doubleValue()) - Math.abs(avg));
		return result;
	}
	
	public static double findP2PThresh(double[] peaks, double[] troughs){
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
}