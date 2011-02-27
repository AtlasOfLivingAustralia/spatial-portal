package au.org.emii.portal.value;

import au.org.emii.portal.aspect.CheckNotNull;
import java.io.Serializable;

public class BoundingBox implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private float minLatitude = 0.0f;
	private float maxLatitude = 0.0f;
	private float minLongitude = 0.0f;
	private float maxLongitude = 0.0f;
	private long id;	
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

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

	@CheckNotNull
	public void copyFrom(au.org.emii.portal.config.xmlbeans.BoundingBox boundingBox) {
		setMinLatitude(boundingBox.getMinLatitude());
		setMaxLatitude(boundingBox.getMaxLatitude());
		setMinLongitude(boundingBox.getMinLongitude());
		setMaxLongitude(boundingBox.getMaxLongitude());
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

        @Override
        public String toString() {
            return new StringBuffer(String.valueOf(minLongitude))
                    .append(String.valueOf(minLatitude))
                    .append(String.valueOf(maxLongitude))
                    .append(String.valueOf(maxLatitude))
                    .toString();
        }
}
