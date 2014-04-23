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

    double[][] gridData;
    float[][] gridCutoffs;

    //fields that can change and then require data to be refreshed
    String colourMode;
    String layer1;
    String layer2;

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

    //only returns extents for the first two layers
    public double[][] layerExtents() {
        double minx = Double.NaN;
        double maxx = Double.NaN;
        double miny = Double.NaN;
        double maxy = Double.NaN;

        for (int i = 0; i < data.length; i++) {
            if (Double.isNaN(minx) || minx > data[i][0]) {
                minx = data[i][0];
            }
            if (Double.isNaN(maxx) || maxx < data[i][0]) {
                maxx = data[i][0];
            }
            if (Double.isNaN(miny) || miny > data[i][1]) {
                miny = data[i][1];
            }
            if (Double.isNaN(maxy) || maxy < data[i][1]) {
                maxy = data[i][1];
            }
        }

        return new double[][]{{minx, maxx}, {miny, maxy}};
    }
}
