/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.gazetteer;

/**
 *
 * @author brendon
 */
public class GazetteerSearchResult {
    private String name;
    private int serial;
    private String featureType;
    private String state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public String getFeatureType() {
	return featureType;
    }
	
    public void setFeatureType(String featureType) {
    	this.featureType = featureType;
    }

    public String getState() {
	return state;
    }

    public void setState(String state) {
	this.state = state;
    } 
}
