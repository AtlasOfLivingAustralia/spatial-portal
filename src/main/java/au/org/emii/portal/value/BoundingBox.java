package au.org.emii.portal.value;

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

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return String.valueOf(minLongitude) + "," + String.valueOf(minLatitude) + "," + String.valueOf(maxLongitude) + "," + String.valueOf(maxLatitude);
    }
}
