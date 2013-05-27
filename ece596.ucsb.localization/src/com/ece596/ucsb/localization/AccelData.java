package com.ece596.ucsb.localization;


public class AccelData{
	private long timestamp;
	private double x;
	private double y;
	private double z;
	
	
	public AccelData(long timestamp, double x, double y, double z) {
		this.timestamp = timestamp;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public void setValue(int axis, double value){
		switch(axis){
		case MainActivity.X_AXIS:
			this.x = value;
			break;
		case MainActivity.Y_AXIS:
			this.y = value;
			break;
		case MainActivity.Z_AXIS:
			this.z = value;
			break;
		default:
			break;
		}
		return;
	}
	public double getValue(int axis){
		double returnValue = 0;
		switch(axis){
		case MainActivity.X_AXIS:
			returnValue = x;
			break;
		case MainActivity.Y_AXIS:
			returnValue = y;
			break;
		case MainActivity.Z_AXIS:
			returnValue = z;
			break;
		default:
			break;
		}
		return returnValue;
	}
	public String toString()
	{
		return "t="+timestamp+", x="+x+", y="+y+", z="+z;
	}
	

}
