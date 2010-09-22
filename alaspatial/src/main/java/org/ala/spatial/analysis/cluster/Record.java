package org.ala.spatial.analysis.cluster;

/**
 *
 * @author ajay
 */
public class Record {

    private double latitude;
    private double longitude;
    private String uncertainity;
    private String id;
    private String name;

    public Record(String id, String name, double longitude, double latitude, String uncertainity) {
        this.id = id;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.uncertainity = uncertainity; 
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

//    public String getName() {
//        return name;
//    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUncertainity() {
        return uncertainity;
    }

    public void setUncertainity(String uncertainity) {
        this.uncertainity = uncertainity;
    }

    @Override
    public String toString() {
        return id + "_" + name + " at " 
                + longitude + ", " + latitude
                + "=> precision: " + uncertainity;
    }

}
