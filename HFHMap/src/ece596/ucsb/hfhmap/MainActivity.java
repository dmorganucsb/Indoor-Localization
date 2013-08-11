package ece596.ucsb.hfhmap;

import java.io.FileWriter;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnClickListener {
	private static final double RADIUSEARTH = 6367000;
	
	GroundOverlay arrowOverlay;
	
	BroadcastReceiver receiver;
	public static boolean Train = false;
	
    public static FragmentManager fm;
	
	public String TAG = "HFHMAPS";
	
	private Bitmap HFHFloorPlan;
	private static LatLng HFH = new LatLng(34.413812, -119.84137);
	private GoogleMap mMap;	
	
	private double mapScale = 10.0;
	
	//control buttons
	private Button step_len_btn, angle_btn, step_btn, reset;
	
	static WifiManager wifi;
	
	/////////////////////////////////////////////////////////////////
	final int LATITUDE = 0;
	final int LONGITUDE = 1;
	
	double[][] Locations = {
			{34.416439987202175, -119.84506241977215},
			{34.41645519945808, -119.84358552843331},
			{34.41652213335112, -119.84209388494492},
			{34.416470688297586, -119.84066896140575},
			{34.41644496575897, -119.83924705535173},
			{34.4164698585384, -119.8377701640129},
			{34.4152708479116, -119.84501011669634},
			{34.41526365656187, -119.8435754701495},
			{34.415235444337696, -119.84212338924407},
			{34.415215806411325, -119.84058380126952},
			{34.41528882628324, -119.83921151608229},
			{34.41527416699587, -119.83773227781057},
			{34.41401788418296, -119.84497223049402},
			{34.41403669260728, -119.84357111155987},
			{34.41400101191636, -119.84203420579433},
			{34.41407845835794, -119.84063141047955},
			{34.41399050132233, -119.83918201178311},
			{34.413989671538545, -119.83769036829472},
			{34.412835434308924, -119.84502822160722},
			{34.41282713635601, -119.8434839397669},
			{34.41285313660575, -119.84206639230251},
			{34.41283737049782, -119.84063073992729},
			{34.41279754031722, -119.83915921300648},
			{34.41282049799309, -119.83768533915281},
			{34.41161286037048, -119.8449843004346},
			{34.41166347860537, -119.84354898333548},
			{34.41161673280484, -119.84208583831789},
			{34.41163249914283, -119.84060090035202},
			{34.41166458501452, -119.83922056853771},
			{34.4116828407636, -119.83773831278084}};
	Marker marker;
	public static int CurLocation = 0;
	////////////////////////////////////////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		fm = getSupportFragmentManager();
		
		step_len_btn = (Button) findViewById(R.id.step_len_btn);
		step_len_btn.setOnClickListener(this);
		angle_btn = (Button) findViewById(R.id.angle_btn);
		angle_btn.setOnClickListener(this);
		step_btn = (Button) findViewById(R.id.step_btn);
		step_btn.setOnClickListener(this);
		reset = (Button) findViewById(R.id.reset);
		reset.setOnClickListener(this);
		
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		try {
			FileWriter fw = new FileWriter("/mnt/sdcard/log.file", false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		setUpMapIfNeeded();
		
		mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH, 16));
		
		HFHFloorPlan = BitmapFactory.decodeResource(getResources(), R.drawable.wifigrid);
		
		setMap();
		//resetArrow();
		//updateDisplay();
		
		marker = mMap.addMarker(new MarkerOptions()
		.position(new LatLng(Locations[CurLocation][LATITUDE],Locations[CurLocation][LONGITUDE]))
	     .title("title")
	     .snippet("info"));
		
		mMap.setOnMapClickListener(new OnMapClickListener() {
			
			    @Override
			    public void onMapClick(LatLng latln) {
			        // TODO Auto-generated method stub
			    	
			    	//marker.setPosition(latln);   
			    }
			});
		
		// Register Broadcast Receiver
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);

		registerReceiver(receiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.d(TAG, "onCreate()");
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void setMap(){
	    //      [ N/S  (decrease to move south), E/W  (decrease to move west) ]
		LatLng hfh_sw = new LatLng(34.410800,-119.846000);
		LatLng hfh_ne = new LatLng(34.417170,-119.836700);
		LatLngBounds hfh_bounds = new LatLngBounds(hfh_sw, hfh_ne);    // get a bounds
	
		// Adds a ground overlay with 70% transparency.
		mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the map
	     .image(BitmapDescriptorFactory.fromBitmap(HFHFloorPlan))
	     .positionFromBounds(hfh_bounds)
	     .transparency(0.7f));
	}
	
	@Override
	public void onStop() {
		unregisterReceiver(receiver);
		super.onStop();
	}
	
	@Override
	public void onClick(View arg0) {
		
		int switchValue = arg0.getId();
		switch (switchValue) {
	
		case R.id.reset:
			//step_num = 0;
			//total_distance = 0;
			//resetArrow();
			//updateDisplay();
			// reset the WiFi map
			break;
		
		case R.id.step_len_btn:
			//stepLenDialog.show(fm, "fragment_step_length");
			//This will be for Toggling locations
			CurLocation++;
			if (CurLocation >= 30)
				CurLocation = 0;
			marker.setPosition(new LatLng(Locations[CurLocation][LATITUDE], Locations[CurLocation][LONGITUDE]));  
			break;
		
		case R.id.angle_btn:
			//angleWRTNDialog.show(fm, "fragment_input_angle");
			//this will be for training locations
			Log.d(TAG, "train button");
			Train = true;
			wifi.startScan();
			break;
			
		case R.id.step_btn:
			//updateCursor(step_length, theta);
			//step_num++;
			//total_distance = total_distance + step_length;
			//updateDisplay();
			//this will be for finding current location
			Log.d(TAG, "find button");
			Train = false;
			wifi.startScan();
			break;
			
		default:
			break;
		
		}
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
}
