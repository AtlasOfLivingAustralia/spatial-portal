/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.userdata;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author brendon
 */
public class UserSearch implements Serializable {

    private long id;
    private long userId;
    private String searchName;
    private String keyword;
    private Date startDate;
    private Date endDate;
    private double north;
    private double south;
    private double east;
    private double west;
    private boolean mappable;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isMappable() {
        return mappable;
    }

    public void setMappable(boolean mappable) {
        this.mappable = mappable;
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }   

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public double getSouth() {
        return south;
    }

    public void setSouth(double south) {
        this.south = south;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId2) {
        this.userId = userId2;
    }

    public double getWest() {
        return west;
    }

    public void setWest(double west) {
        this.west = west;
    }

}
