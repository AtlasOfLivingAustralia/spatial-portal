/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.dto;

import au.org.ala.legend.LegendObject;
import au.org.ala.spatial.util.Query;
import au.org.emii.portal.menu.SelectedArea;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;

/**
 * @author Adam
 */
public class ScatterplotDataDTO implements Serializable {

    //appearance
    private String colourMode = "-1";
    private int red = 0;
    private int green = 0;
    private int blue = 255;
    private int opacity = 100;
    private int size = 4;
    private int selectionCount;
    private String imagePath;
    private double[] prevSelection = null;
    private Boolean missingDataChecked = false;
    private String layer1;
    private String layer1name;
    private String layer2;
    private String layer2name;
    private String pid;
    private Rectangle2D.Double selection;
    private boolean enabled;
    private Query query;
    private String name;
    private SelectedArea highlightSa;
    private SelectedArea filterSa;  //keep for backwards compatability for saved sessions
    //data
    private double[] points;
    private double[][] data;
    private int missingCount;
    private LegendObject legend;
    private String id;

    public ScatterplotDataDTO() {
        enabled = false;
        missingDataChecked = false;
    }

    public ScatterplotDataDTO(Query query, String name, String layer1, String layer1name, String layer2
            , String layer2name, String pid, Rectangle2D.Double selection, boolean enabled, SelectedArea highlightSa) {
        this.query = query;
        this.name = name;
        this.layer1 = layer1;
        this.layer1name = layer1name;
        this.layer2 = layer2;
        this.layer2name = layer2name;
        this.pid = pid;
        this.selection = selection;
        this.enabled = enabled;
        this.highlightSa = highlightSa;
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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Rectangle2D.Double getSelection() {
        return selection;
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

    public String getSpeciesName() {
        return name;
    }

    public SelectedArea getHighlightSa() {
        return highlightSa;
    }

    public void setHighlightSa(SelectedArea sa) {
        highlightSa = sa;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public double[] getPoints() {
        return points;
    }

    public void setPoints(double[] points) {
        this.points = points == null ? null : points.clone();
    }

    public double[][] getData() {
        return data;
    }

    public void setData(double[][] data) {
        this.data = data == null ? null : data.clone();
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public LegendObject getLegend() {
        return legend;
    }

    public void setLegend(LegendObject legend) {
        this.legend = legend;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double[] getPrevSelection() {
        return prevSelection;
    }

    public void setPrevSelection(double[] prevSelection) {
        this.prevSelection = prevSelection == null ? null : prevSelection.clone();
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getSelectionCount() {
        return selectionCount;
    }

    public void setSelectionCount(int selectionCount) {
        this.selectionCount = selectionCount;
    }

    public Boolean getMissingDataChecked() {
        return missingDataChecked == null ? false : missingDataChecked;
    }

    public void setMissingDataChecked(boolean missingDataChecked) {
        this.missingDataChecked = missingDataChecked;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public String getColourMode() {
        return colourMode;
    }

    public void setColourMode(String colourMode) {
        this.colourMode = colourMode;
    }
}
