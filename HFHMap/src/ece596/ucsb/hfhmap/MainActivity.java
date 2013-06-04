package ece596.ucsb.hfhmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MainActivity extends FragmentActivity implements OnClickListener {
	private static final double RADIUSEARTH = 6367000;
	
	GroundOverlay arrowOverlay;
	
	private static double step_length = 0.6;
	private static double theta = 90;
	private static int step_num = 0;
	private static double total_distance = 0.0;
	
    public static StepLenDialog stepLenDialog;
    public static AngleWRTNDialog angleWRTNDialog;
    public static FragmentManager fm;
	
	private TextView step_length_view;
	private TextView angleWRTN_view;
	private TextView distance_view;
	
	private Bitmap HFHFloorPlan;
	private static LatLng HFH = new LatLng(34.413812, -119.84137);
	private GoogleMap mMap;	
	
	private Bitmap arrow;
	private double ArrowLatLongSize = 0.000050;
	//      [ Bot  (decrease to move south), Left  (decrease to move east) ]	
	double initArrowBot = 34.414590;
	double initArrowLeft = -119.845395;
	private LatLng arrow_sw;
	private LatLng arrow_ne;
	
	private double mapScale = 11.4;
	
	//control buttons
	private Button step_len_btn, angle_btn, step_btn, reset;
	
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
		
		step_length_view = (TextView)findViewById( R.id.textview1);
		angleWRTN_view = (TextView)findViewById( R.id.textview2);
		distance_view = (TextView)findViewById( R.id.textview3);
		
		mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		setUpMapIfNeeded();
		
		mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HFH, 16));
		
		HFHFloorPlan = BitmapFactory.decodeResource(getResources(), R.drawable.hfh_map_new);
		arrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
		
		stepLenDialog = new StepLenDialog();
		angleWRTNDialog = new AngleWRTNDialog();
		
		setMap();
		resetArrow();
		updateDisplay();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void resetArrow(){
		if (arrowOverlay != null)
			arrowOverlay.remove();
		
		arrow_sw = new LatLng(initArrowBot,initArrowLeft);
		arrow_ne = new LatLng(initArrowBot + ArrowLatLongSize,initArrowLeft + ArrowLatLongSize);
		LatLngBounds arrow_bounds = new LatLngBounds(arrow_sw, arrow_ne);    // get a bounds
		
		arrowOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the arrow
	     .image(BitmapDescriptorFactory.fromBitmap(arrow))
	     .positionFromBounds(arrow_bounds)
	     .transparency(0.7f));
	}
	
	public void setMap(){
	    //      [ N/S  (decrease to move south), E/W  (decrease to move west) ]
		LatLng hfh_sw = new LatLng(34.413000,-119.846000);
		LatLng hfh_ne = new LatLng(34.415370,-119.836700);
		LatLngBounds hfh_bounds = new LatLngBounds(hfh_sw, hfh_ne);    // get a bounds
	
		// Adds a ground overlay with 70% transparency.
		mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the map
	     .image(BitmapDescriptorFactory.fromBitmap(HFHFloorPlan))
	     .positionFromBounds(hfh_bounds)
	     .transparency(0.7f));
	}
	
	@Override
	public void onClick(View arg0) {
		
		int switchValue = arg0.getId();
		switch (switchValue) {
	
		case R.id.reset:
			step_num = 0;
			total_distance = 0;
			resetArrow();
			updateDisplay();
			break;
		
		case R.id.step_len_btn:
			stepLenDialog.show(fm, "fragment_step_length");
			break;
		
		case R.id.angle_btn:
			angleWRTNDialog.show(fm, "fragment_input_angle");
			break;
			
		case R.id.step_btn:
			updateCursor(step_length, theta);
			step_num++;
			total_distance = total_distance + step_length;
			updateDisplay();
			break;
			
		default:
			break;
		
		}
	}
	
	public void inputStepLength(double step_len) {
		// TODO Auto-generated method stub
		step_length = step_len;
		updateDisplay();
	}

	public void inputAngleWRTN(double angle) {
		// TODO Auto-generated method stub
		theta = angle;
		updateDisplay();
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
	
	
	/// <summary>
	/// Calculates the end-point from a given source at a given range (meters) and bearing (degrees).
	/// This methods uses simple geometry equations to calculate the end-point.
	/// </summary>
	/// <param name="source">Point of origin</param>
	/// <param name="range">Range in meters</param>
	/// <param name="bearing">Bearing in degrees</param>
	/// <returns>End-point from the source given the desired range and bearing.</returns>
	public LatLng CalculateDerivedPosition(LatLng source, double range, double angleWRTN)
	{
	    double latA = Math.toRadians(source.latitude);
	    double lonA = Math.toRadians(source.longitude);
	    double angularDistance = range * mapScale / RADIUSEARTH;
	    double trueCourse = Math.toRadians(angleWRTN);

	    double lat = Math.asin(
	        Math.sin(latA) * Math.cos(angularDistance) + 
	        Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));

	    double dlon = Math.atan2(
	        Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA), 
	        Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

	    double lon = ((lonA + dlon + Math.PI) % (Math.PI*2)) - Math.PI;

	    return new LatLng(Math.toDegrees(lat), Math.toDegrees(lon));
	}
	
	
	public double LatLontoMeter(double startLat, double startLon, double endLat, double endLon){
		double dlon = (endLon - startLon);
		double dlat = (endLat - startLat);
		dlon = (dlon * Math.PI/180);
		dlat = (dlat * Math.PI/180);
		double a = Math.pow(Math.sin(dlat/2),2) + Math.cos(startLat*Math.PI/180)*Math.cos(endLat*Math.PI/180)*Math.pow(Math.sin(dlon/2),2);
		double c = 2 * Math.atan2(Math.sqrt(a),  Math.sqrt(1-a));
		double d = RADIUSEARTH * c;
		
		return d;
	}
	
	public void updateCursor(double distance, double angleWRTN){
		arrowOverlay.remove();
		
		arrow_sw = CalculateDerivedPosition(arrow_sw, distance, angleWRTN);
		arrow_ne = CalculateDerivedPosition(arrow_ne, distance, angleWRTN);
		LatLngBounds arrow_bounds = new LatLngBounds(arrow_sw, arrow_ne);    // get a bounds
		
		arrowOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()   // overlay the arrow
	     .image(BitmapDescriptorFactory.fromBitmap(arrow))
	     .positionFromBounds(arrow_bounds)
	     .transparency(0.7f));
		
		return;
	}
}
