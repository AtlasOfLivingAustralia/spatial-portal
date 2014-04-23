package org.ala.spatial.analysis.scatterplot;

import com.vividsolutions.jts.geom.CoordinateArrays;

/**
 * Created by a on 10/03/2014.
 */
public class ScatterplotStyleDTO {

    //appearance
    public String colourMode = "-1";
    public int red = 0;
    public int green = 0;
    public int blue = 255;
    public int opacity = 100;
    public int size = 4;

    String highlightWkt = null;

    double[] selection = null;
    double[] prevSelection = null;

    int width = 320;
    int height = 320;
    String prevHighlightWkt = null;

    public ScatterplotStyleDTO() {
    }

    public String getColourMode() {
        return colourMode;
    }

    public void setColourMode(String colourMode) {
        this.colourMode = colourMode;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getHighlightWkt() {
        return highlightWkt;
    }

    public void setHighlightWkt(String highlightWkt) {
        if (highlightWkt != null && highlightWkt.length() == 0) {
            this.highlightWkt = null;
        } else {
            this.highlightWkt = highlightWkt;
        }
    }

    public double[] getSelection() {
        return selection;
    }

    public void setSelection(double[] selection) {
        this.selection = selection;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double[] getPrevSelection() {
        return prevSelection;
    }

    public void setPrevSelection(double[] prevSelection) {
        this.prevSelection = prevSelection;
    }

    public void setPrevHighlightWKT(String highlightWkt) {
        this.prevHighlightWkt = highlightWkt;
    }


    public String getPrevHighlightWkt() {
        return prevHighlightWkt;
    }
}
