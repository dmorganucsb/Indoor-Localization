package ucsb.ece596.indoortrack;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * LovelyView demonstrates a custom view
 * for Mobiletuts+ tutorial - Android SDK: Creating Custom Views
 * 
 * The view displays a circle with a text string displayed in the middle.
 * Circle color, text and text color can all be set in layout XML or Java
 * - see the main app Activity
 * 
 * The view also refers to attributes specified in the app res/values/attrs XML
 */

public class SampleView extends View {
    private Bitmap arrowImage;
    private Bitmap outlineImage;
    private Bitmap rotatedArrow;
    private int xPos = 290;
    private int yPos = 980;
    
    ArrayList<Integer> path = new ArrayList<Integer>();
//    private int[] path = {150, 1000};
    
    Paint paint = new Paint();
    Matrix matrix = new Matrix();

	/**
	 * Constructor method for custom view
	 * - calls superclass method and adds custom processing
	 * 
	 * @param context
	 * @param attrs
	 */
	public SampleView(Context context, AttributeSet attrs){
		super(context, attrs);

        arrowImage = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
        outlineImage = BitmapFactory.decodeResource(getResources(), R.drawable.outline_hfh);
        
        path.add(290);
        path.add(980);
        
        paint.setColor(Color.WHITE);
	}	

	/**
	 * Override the onDraw method to specify custom view appearance using canvas
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(outlineImage, 100, 50, null);

        if (MainActivity.mValues != null) {
        	matrix.setRotate(MainActivity.mValues[0]+180); // anti-clockwise by 90 degrees    
        	rotatedArrow = Bitmap.createBitmap(arrowImage , 0, 0, arrowImage.getWidth(), arrowImage.getHeight(), matrix, true);
        }
       canvas.drawBitmap(rotatedArrow, xPos, yPos, null);
       
	   for (int i=4; i < path.size(); i=i+2){
		   canvas.drawLine(path.get(i-4), path.get(i-3), path.get(i-2), path.get(i-1), paint);
		   //oldxPos = xPos;
		   //oldyPos = yPos;
	   }
	}
	
	void step(Context context, float[] zValues){
		xPos = (int) (xPos - 9*Math.cos(zValues[0] - Math.PI/2));
		yPos = (int) (yPos - 9*Math.sin(zValues[0] - Math.PI/2));
		path.add(xPos + arrowImage.getWidth()/2);
		path.add(yPos + arrowImage.getHeight()/2);
		
		//SensorManager.getRotationMatrix(mRot, mInclin, mGrav, mGeom);
		//SensorManager.getOrientation(mRot, zValues);
		//Toast.makeText(context, "orientation " + zValues[0] + " " + zValues[1] + " " + zValues[2], Toast.LENGTH_SHORT).show();
	}
	void reset(){
		yPos = 980;
		xPos = 290;
		path.clear();
	}
}