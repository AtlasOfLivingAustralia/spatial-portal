package org.ala.spatial.analysis.cluster;

import java.util.Vector;

/**
 *
 * @author ajay
 */
public class ClusteredRecord {

    private String name;
    private double latitude;
    private double longitude;
    private int count;
    private Vector<Record> records;

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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Vector<Record> getRecords() {
        return records;
    }

    public void setRecords(Vector<Record> records) {
        this.records = records;
    }

    public void addRecord(Record r) {
        if (this.records == null) {
            this.records = new Vector<Record>();
        }
        this.records.add(r);
    }


}
