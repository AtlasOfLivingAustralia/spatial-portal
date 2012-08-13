/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Adam
 */
public class LayerSelection {

    String displayString;
    String layerName;
    String analysisType;
    long created;
    long lastUse;
    String layers;
    ArrayList<Long> analysisIds;

    public LayerSelection(String displayString, String layersString) {
        this.displayString = displayString;
        this.layers = layersString;
    }

    public LayerSelection(String analysisType, String layerName, long created, String layersString) {
        this.layerName = layerName;
        this.analysisType = analysisType;
        this.created = created;
        this.lastUse = created;

        this.layers = layersString;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public long getCreated() {
        return created;
    }

    public void setLastUse(long lastUse) {
        this.lastUse = lastUse;
    }

    public long getLastUse() {
        return lastUse;
    }

    public boolean contains(String layerId) {
        String lookFor = "(" + layerId + ")";
        return layers.contains(lookFor);
    }

    public String getLayers() {
        return layers;
    }
    
    @Override
    public String toString() {
        if(displayString != null) {
            return displayString;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
        int len = layers.split(",").length;
        if (len != 1) {
            return layerName + " | " + sdf.format(new Date(created)) + " | " + len + " layers";
        } else {
            return layerName + " | " + sdf.format(new Date(created)) + " | " + len + " layer";
        }
    }

    public boolean equalsList(LayerSelection ls) {
        String [] thisList = layers.split(",");
        String [] thatList = ls.layers.split(",");

        for(int i=0;i<thisList.length;i++) {
            boolean found = false;
            for(int j=0;j<thatList.length;j++) {
                if(thisList[i].equals(thatList[j])) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        for(int j=0;j<thatList.length;j++) {
            boolean found = false;
            for(int i=0;i<thisList.length;i++) {
                if(thisList[i].equals(thatList[j])) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        return true;
    }
}
