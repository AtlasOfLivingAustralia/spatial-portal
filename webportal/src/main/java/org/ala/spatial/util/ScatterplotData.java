/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.List;
import org.ala.spatial.data.LegendObject;
import org.ala.spatial.data.Query;

/**
 *
 * @author Adam
 */
public class ScatterplotData implements Serializable {

    String layer1;
    String layer1name;
    String layer2;
    String layer2name;
    String pid;
    Rectangle2D.Double selection;
    boolean enabled;
    Query query;
    String name;
    Query backgroundQuery;

    //area info
    String filterWkt;
    String highlightWkt;

    //grid
    boolean envGrid;

    //appearance
    public String colourMode = "-1";
    public int red = 0;
    public int green = 0;
    public int blue = 255;
    public int opacity = 100;
    public int size = 4;

    //data
    List<String[]> csv;
    double [] points;
    String [] series;
    String [] ids;
    double [][] data;
    int missingCount;
    LegendObject legend;

    public ScatterplotData() {
        enabled = false;
    }

    public ScatterplotData(Query query, String name, String layer1, String layer1name, String layer2, String layer2name, String pid, Rectangle2D.Double selection, boolean enabled
            ,Query backgroundQuery, String filterWkt, String highlightWkt, boolean envGrid) {
        this.query = query;
        this.name = name;
        this.layer1 = layer1;
        this.layer1name = layer1name;
        this.layer2 = layer2;
        this.layer2name = layer2name;
        this.pid = pid;
        this.selection = selection;
        this.enabled = enabled;
        this.backgroundQuery = backgroundQuery;
        this.filterWkt = filterWkt;
        this.highlightWkt = highlightWkt;
        this.envGrid = envGrid;
    }

    public String getLayer1() {
        return layer1;
    }

    public String getLayer2() {
        return layer2;
    }

    public String getLayer1Name() {
        return layer1name;
    }

    public String getLayer2Name() {
        return layer2name;
    }

    public void setLayer1Name(String name) {
        layer1name = name;
    }

    public void setLayer2Name(String name) {
        layer2name = name;
    }

    public String getPid() {
        return pid;
    }

    public Rectangle2D.Double getSelection() {
        return selection;
    }

    public void setLayer1(String layer) {
        layer1 = layer;
    }

    public void setLayer2(String layer) {
        layer2 = layer;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setSelection(Rectangle2D.Double rect) {
        selection = rect;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean state) {
        enabled = state;
    }

    public void setSpeciesName(String name) {
        this.name = name;
    }

    public String getSpeciesName() {
        return name;
    }

    public Query getBackgroundQuery() {
        return backgroundQuery;
    }

    public String getFilterWkt() {
        return filterWkt;
    }

    public String getHighlightWkt() {
        return highlightWkt;
    }

    public boolean isEnvGrid() {
        return envGrid;
    }

    public void setBackgroundQuery(Query backgroundQuery) {
        this.backgroundQuery = backgroundQuery;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setCSV(List<String[]> csv) {
        this.csv = csv;
    }

    public List<String[]> getCSV() {
        return csv;
    }

    public void setPoints(double [] points) {
        this.points = points;
    }

    public double [] getPoints() {
        return points;
    }

    public void setSeries(String [] series) {
        this.series = series;
    }

    public String [] getSeries() {
        return series;
    }

    public void setIds(String [] ids) {
        this.ids = ids;
    }

    public String [] getIds() {
        return ids;
    }

    public void setData(double [][] data) {
        this.data = data;
    }

    public double [][] getData() {
        return data;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setLegend(LegendObject legend) {
        this.legend = legend;
    }

    public LegendObject getLegend() {
        return legend;
    }
}
