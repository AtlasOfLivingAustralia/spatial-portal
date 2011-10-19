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
    String layers;
    ArrayList<Long> analysisIds;

    public LayerSelection(String displayString, String layersString) {
        this.displayString = displayString;
        this.layers = layersString;
    }

    public LayerSelection(String layerName, String analysisType, long created, String layersString) {
        this.layerName = layerName;
        this.analysisType = analysisType;
        this.created = created;

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
            return layerName + " / " + analysisType + " / " + sdf.format(new Date(created)) + " / " + len + " layers";
        } else {
            return layerName + " / " + analysisType + " / " + sdf.format(new Date(created)) + " / " + len + " layer";
        }
    }
}
