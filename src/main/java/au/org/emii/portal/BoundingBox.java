package au.org.emii.portal;

import java.io.Serializable;

public class BoundingBox implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private float minLatitude = 0.0f;
	private float maxLatitude = 0.0f;
	private float minLongitude = 0.0f;
	private float maxLongitude = 0.0f;
	
	public float getMinLatitude() {
		return minLatitude;
	}
	public void setMinLatitude(float minLatitude) {
		this.minLatitude = minLatitude;
	}
	public float getMaxLatitude() {
		return maxLatitude;
	}
	public void setMaxLatitude(float maxLatitude) {
		this.maxLatitude = maxLatitude;
	}
	public float getMinLongitude() {
		return minLongitude;
	}
	public void setMinLongitude(float minLongitude) {
		this.minLongitude = minLongitude;
	}
	public float getMaxLongitude() {
		return maxLongitude;
	}
	public void setMaxLongitude(float maxLongitude) {
		this.maxLongitude = maxLongitude;
	}
	
	public void copyFrom(au.org.emii.portal.config.xmlbeans.BoundingBox boundingBox) {
		setMinLatitude(boundingBox.getMinLatitude());
		setMaxLatitude(boundingBox.getMaxLatitude());
		setMinLongitude(boundingBox.getMinLongitude());
		setMaxLongitude(boundingBox.getMaxLongitude());
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
