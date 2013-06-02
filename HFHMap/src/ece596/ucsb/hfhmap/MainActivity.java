package ece596.ucsb.hfhmap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;


public class MainActivity extends Activity {
	private static double step_length = 0.6;
	private static double theta = 90;
	private static int step_num = 5;
	private static double total_distance = 3.0;
	
	private TextView step_length_view;
	private TextView angleWRTN_view;
	private TextView distance_view;
	
	private static LatLng HFH = new LatLng(34.413812, -119.84137);
	private GoogleMap mMap;	
	private double ArrowLatLongSize = 0.000050;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		step_length_view = (TextView)findViewById( R.id.textview1);
		angleWRTN_view = (TextView)findViewById( R.id.textview2);
		distance_view = (TextView)findViewById( R.id.textview3);
		
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		setUpMapIfNeeded();
		
		mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH, 16));
		
		//var opt = { minZoom: 6, maxZoom: 9 };
		//mMap.setOptions();
		
		Bitmap hfh = BitmapFactory.decodeResource(getResources(), R.drawable.hfh_map);
		Bitmap arrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
		
		//      [ Bot  (decrease to move south), Left  (decrease to move east) ]	
		double arrowBot = 34.414590;
		double arrowLeft = -119.845305;
		
		    //      [ N/S  (decrease to move south), E/W  (decrease to move west) ]
		LatLng hfh_sw = new LatLng(34.413000,-119.846000);
		LatLng hfh_ne = new LatLng(34.415370,-119.836700);
		LatLngBounds hfh_bounds = new LatLngBounds(hfh_sw, hfh_ne);    // get a bounds
		
		LatLng arrow_sw = new LatLng(arrowBot,arrowLeft);
		LatLng arrow_ne = new LatLng(arrowBot + ArrowLatLongSize,arrowLeft + ArrowLatLongSize);
		LatLngBounds arrow_bounds = new LatLngBounds(arrow_sw, arrow_ne);    // get a bounds
		
		// Adds a ground overlay with 50% transparency.
		mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the map
		     .image(BitmapDescriptorFactory.fromBitmap(hfh))
		     .positionFromBounds(hfh_bounds)
		     .transparency(0.5f));
		
		mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the map
	     .image(BitmapDescriptorFactory.fromBitmap(arrow))
	     .positionFromBounds(arrow_bounds)
	     .transparency(0.5f));
		
		updateDisplay();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//setUpMapIfNeeded();
	}
	private void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap != null) {
	        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH, 16));
	        	/*
	        	mMap.
	    		  		  map:uiCompass="false"
	    		  		  map:uiRotateGestures="true"
	    		  		  map:uiScrollGestures="true"
	    		  		  map:uiTiltGestures="false"
	    		  		  map:uiZoomControls="false"
	    		  		  map:uiZoomGestures="true"
	    		 */

	        }
	    }
	}
	
	public void updateDisplay(){
		step_length_view.setText("Step Length is: " + Double.toString(step_length));
		angleWRTN_view.setText("Angle WRTN is: " + Double.toString(theta) + " degrees");
		distance_view.setText(Integer.toString(step_num) + " Steps and " + Double.toString(total_distance) + " Meters");
		
		return;
	}
	

}
