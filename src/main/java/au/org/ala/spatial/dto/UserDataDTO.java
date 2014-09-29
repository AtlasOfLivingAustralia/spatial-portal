package au.org.ala.spatial.dto;

import au.org.ala.spatial.util.Query;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Object to hold user uploaded data information
 *
 * @author ajay
 */
public class UserDataDTO implements Serializable {

    private String name;
    private String description;
    private String type;
    private String filename;
    private int featureCount;
    private long uploadedTimeInMs;
    private String metadata;
    private int subType;
    private String lsid;
    private Query query;

    public UserDataDTO(String name) {
        this.name = name;
    }

    public UserDataDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UserDataDTO(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

    public String getDisplayTime() {
        return covertMillisecondsToDate(uploadedTimeInMs);
    }

    private String covertMillisecondsToDate(long ms) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ms);

        return formatter.format(calendar.getTime());

    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public int getSubType() {
        return subType;
    }

    public void setSubType(int subType) {
        this.subType = subType;
    }

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }
}
