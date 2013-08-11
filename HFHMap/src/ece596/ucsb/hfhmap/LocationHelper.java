package ece596.ucsb.hfhmap;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.util.Log;
import android.widget.Toast;

public class LocationHelper {
	
	TreeMap<String, Integer> FinderList;
	public final int AVG   = 0;
	public final int VAR   = 1;
	public final int COUNT = 2;
	
	public LocationHelper(){
		FinderList = new TreeMap<String, Integer>();

	}
	
	public void LocationSearch(TreeMap<String, LocationClass> LocationMap, String BSSID, String level){
		
		   for (Entry<String, LocationClass> LocationEntry : LocationMap.entrySet()) {
			   //Location
		        //Log.d("test", "For " + LocationEntry.getKey());
		        double[] FoundEntry = LocationEntry.getValue().LocationInfo.get(BSSID);
		        if (FoundEntry != null && (FoundEntry[AVG] - Double.parseDouble(level)) < FoundEntry[VAR]){
		        	
		        	Integer LocationCount = FinderList.get(LocationEntry.getKey());
		        	if (LocationCount != null){
		        		FinderList.put(LocationEntry.getKey(), (LocationCount+1));
		        	}
		        	else{
		        		FinderList.put(LocationEntry.getKey(), (1));
		        	}
		        }
		        
		        
		   }
		   //FinderList.put(LocationEntry.getKey(),MatchCount);
		   //Log.d("test", "most likely location is " + entriesSortedByValues(FinderList).first().getKey());
	
	}
	
	public String getLocation(){
		Log.d("test", "most likely location is " + entriesSortedByValues(FinderList).first().getKey());
		Log.d("test", "with count " + entriesSortedByValues(FinderList).first().getValue());
		StringBuilder sb = new StringBuilder();
		int HighestCount = entriesSortedByValues(FinderList).first().getValue();
		
		//sb.append("most likely location is ");
		
		for (Entry<String, Integer> entry : FinderList.entrySet()) {
			//if (entry.getValue() - HighestCount < 2){
			//	sb.append(entry.getKey() + " ");
			//}
			sb.append(entry.getKey() + " " + entry.getValue() + " ");
		}
		
		
		
		return (sb.toString());
		
	}
	
	static <K,V extends Comparable<? super V>>
	SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                return e1.getValue().compareTo(e2.getValue());
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}
}
