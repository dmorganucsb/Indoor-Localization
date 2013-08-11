package ece596.ucsb.hfhmap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.Toast;

public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "WiFiScanReceiver";
  MainActivity wifiDemo;
  boolean newLocation;
  public final int AVG   = 0;
  public final int VAR   = 1;
  public final int COUNT = 2;
  public int ScanNum = 5;
  TreeMap<String, LocationClass> WiFiMap = new TreeMap<String, LocationClass>();
  //public int roomNum;
  
  public WiFiScanReceiver(MainActivity wifiDemo) {
    super();
    this.wifiDemo = wifiDemo;
  }

  public List<String> results_string;
  
  @Override
  public void onReceive(Context c, Intent intent) {
    List<ScanResult> results = wifiDemo.wifi.getScanResults();
    
    if (MainActivity.Train == true){
	    String Location = "Location: " + (MainActivity.CurLocation+1);
	    
	    LocationClass curLocation = WiFiMap.get(Location);
	    
	    if (curLocation == null){
	    	WiFiMap.put(Location, new LocationClass());
	    	appendLog("added new location: " + Location);
	    	curLocation = WiFiMap.get(Location);
	    }
	    else
	    	appendLog("updating location: " + Location);
	
	    
	    for (ScanResult result : results) {
	    	
	    	//Log.d("test", result.toString());
	    	
	    	String[] parts = result.toString().split(",");
	    	String[] SSID = parts[0].split(": ");
	    	String[] BSSID = parts[1].split(": ");
	    	String[] level = parts[3].split(": -");
	    	
	    	//Log.d("test", SSID[0] + BSSID[0] + level[0]);
	    	if (SSID.length == 2){
	    		curLocation.UpdateLocationInfo(SSID[1], BSSID[1], Double.parseDouble(level[1]));
	    		appendLog(SSID[1] + BSSID[1] + level[1] + " avg = " + curLocation.LocationInfo.get(BSSID[1])[AVG] + " var = " + curLocation.LocationInfo.get(BSSID[1])[VAR] + " count = " + curLocation.LocationInfo.get(BSSID[1])[COUNT]);
	    	}
	    }
	    
	    if (ScanNum != 1){
	    	MainActivity.wifi.startScan();
	    	ScanNum--;
	    }
	    else{
	    	ScanNum = 5;
	    
		    Log.d(TAG, "scan done");

		    Toast.makeText(wifiDemo, "Training Scan Complete", Toast.LENGTH_SHORT).show();
	    }
    }
    else{
    	LocationHelper Search = new LocationHelper();
    	
    	for (ScanResult result : results) {
	    	String[] parts = result.toString().split(",");
	    	String[] SSID = parts[0].split(": ");
	    	String[] BSSID = parts[1].split(": ");
	    	String[] level = parts[3].split(": -");
	    	
	        //curLocation.UpdateLocationInfo(SSID[1], BSSID[1], Double.parseDouble(level[1]));
	        //appendLog(SSID[1] + BSSID[1] + level[1] + " avg = " + curLocation.LocationInfo.get(BSSID[1])[AVG] + " var = " + curLocation.LocationInfo.get(BSSID[1])[VAR] + " count = " + curLocation.LocationInfo.get(BSSID[1])[COUNT]);
	    	Search.LocationSearch(WiFiMap, BSSID[1], level[1]);
    	}
    	
    	Toast.makeText(wifiDemo, Search.getLocation(), Toast.LENGTH_LONG).show();
    }
  }
  
  public void appendLog(String text)
  {       
     File logFile = new File("/mnt/sdcard/log.file");
     if (!logFile.exists())
     {
        try
        {
           logFile.createNewFile();
        }
        catch (IOException e)
        {
           // TODO Auto-generated catch block
           e.printStackTrace();
        }
     }
     try
     {
        //BufferedWriter for performance, true to set append to file flag
        BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
        buf.append(text);
        buf.newLine();
        buf.close();
     }
     catch (IOException e)
     {
        // TODO Auto-generated catch block
        e.printStackTrace();
     }
  }


  
}
