package org.ala.spatial.analysis.cluster;

/**
 *
 * @author ajay
 */
public class Record {

    private double latitude;
    private double longitude;
    private int uncertainity;
    private long id;
    private String name;

    public Record(long id, String name, double longitude, double latitude, int uncertainity) {
        this.id = id;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.uncertainity = uncertainity;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUncertainity() {
        return uncertainity;
    }

    public void setUncertainity(int uncertainity) {
        this.uncertainity = uncertainity;
    }

    @Override
    public String toString() {
        return id + "_" + name + " at "
                + longitude + ", " + latitude
                + "=> precision: " + uncertainity;
    }
}
