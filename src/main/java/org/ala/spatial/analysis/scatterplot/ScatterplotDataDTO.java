package org.ala.spatial.analysis.scatterplot;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Created by a on 10/03/2014.
 */
public class ScatterplotDataDTO {

    //data
    double[] points;
    String[] series;
    String[] ids;
    double[][] data;
    double[] seriesValues;
    double[] backgroundPoints;
    String[] backgroundSeries;
    String[] backgroundIds;
    double[][] backgroundData;

    int missingCount;

    double [][] gridData;
    float [][] gridCutoffs;

    //fields that can change and then require data to be refreshed
    String colourMode;
    String layer1;
    String layer2;
    private double[] extents;

    public ScatterplotDataDTO() {
    }

    public double[] getPoints() {
        return points;
    }

    public void setPoints(double[] points) {
        this.points = points;
    }

    public String[] getSeries() {
        return series;
    }

    public void setSeries(String[] series) {
        this.series = series;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public double[][] getData() {
        return data;
    }

    public void setData(double[][] data) {
        this.data = data;
    }

    public double[] getSeriesValues() {
        return seriesValues;
    }

    public void setSeriesValues(double[] seriesValues) {
        this.seriesValues = seriesValues;
    }

    public double[] getBackgroundPoints() {
        return backgroundPoints;
    }

    public void setBackgroundPoints(double[] backgroundPoints) {
        this.backgroundPoints = backgroundPoints;
    }

    public String[] getBackgroundSeries() {
        return backgroundSeries;
    }

    public void setBackgroundSeries(String[] backgroundSeries) {
        this.backgroundSeries = backgroundSeries;
    }

    public String[] getBackgroundIds() {
        return backgroundIds;
    }

    public void setBackgroundIds(String[] backgroundIds) {
        this.backgroundIds = backgroundIds;
    }

    public double[][] getBackgroundData() {
        return backgroundData;
    }

    public void setBackgroundData(double[][] backgroundData) {
        this.backgroundData = backgroundData;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public double[][] getGridData() {
        return gridData;
    }

    public void setGridData(double[][] gridData) {
        this.gridData = gridData;
    }

    public float[][] getGridCutoffs() {
        return gridCutoffs;
    }

    public void setGridCutoffs(float[][] gridCutoffs) {
        this.gridCutoffs = gridCutoffs;
    }

    public String getColourMode() {
        return colourMode;
    }

    public void setColourMode(String colourMode) {
        this.colourMode = colourMode;
    }

    public String getLayer1() {
        return layer1;
    }

    public void setLayer1(String layer1) {
        this.layer1 = layer1;
    }

    public String getLayer2() {
        return layer2;
    }

    public void setLayer2(String layer2) {
        this.layer2 = layer2;
    }

    public double[] getExtents() {
        return extents;
    }

    public void setExtents(double [] extents) {
        this.extents = extents;
    }
}
