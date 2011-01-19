package org.ala.spatial.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Object to hold user uploaded data information
 *
 * @author ajay
 */
public class UserData {

    private String name;
    private String description;
    private String type;
    private int featureCount;
    private long uploadedTimeInMs;

    public UserData(String name) {
        this.name = name;
    }

    public UserData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UserData(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getUploadedTimeInMs() {
        return uploadedTimeInMs;
    }

    public void setUploadedTimeInMs(long uploadedTimeInMs) {
        this.uploadedTimeInMs = uploadedTimeInMs;
    }

    public String getDisplayName() {
        return "User " + covertMillisecondsToDate(uploadedTimeInMs); 
    }

    private String covertMillisecondsToDate(long ms) {
        //DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS"); 
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Calendar calendar = Calendar.getInstance(); 
        calendar.setTimeInMillis(ms); 

        return formatter.format(calendar.getTime());

    }
}
