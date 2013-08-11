package ece596.ucsb.hfhmap;

import java.util.TreeMap;

import android.util.Log;

public class LocationClass {

	private static final String TAG = "WiFiScanReceiver";
	public TreeMap<String, double[]> LocationInfo;
	//public String SSID;
	public final int AVG   = 0;
	public final int VAR   = 1;
	public final int COUNT = 2;
	
	public LocationClass() {

		LocationInfo = new TreeMap<String, double[]>();
	}
	
	public void UpdateLocationInfo(String SSID, String BSSID, double level){
		double[] info = LocationInfo.get(BSSID);
		if (info != null){
			double newMean;
			//found it, now lets update the values
			newMean = updateAverage(info[AVG],info[COUNT],level);
			info[VAR] = updateVariance(info[COUNT],level,info[VAR],info[AVG],newMean);
			info[AVG] = newMean;
			info[COUNT] = info[COUNT]+1;
			LocationInfo.put(BSSID,info);
			//Log.d(TAG, SSID + " count is now " + info[COUNT]);
		}
		else{
			//new info, add it to the map
			double[] newInfo = {level,0,1};
			LocationInfo.put(BSSID,newInfo);
		}
			
	}

	private double updateVariance(double count, double level, double oldVar, double oldAvg, double newAvg) {
		// TODO Auto-generated method stub
		//  new variance = [(#values-1)(orig var)+(#values)*(orig mean)^2+(new RSSI)^2-(#values+1)(new mean)^2]/#values
		
		return ((count-1)*(oldVar)+(count*Math.pow(oldAvg,2))+Math.pow(level,2)-(count+1)*(Math.pow(newAvg,2)))/count;
	}

	private double updateAverage(double avg, double count, double level) {
		// TODO Auto-generated method stub
		// new mean = [(#values)*(orig mean)+(new rssi)]/(#values + 1)
		
		return ((count*avg)+level)/(count+1);
	}
	
}
