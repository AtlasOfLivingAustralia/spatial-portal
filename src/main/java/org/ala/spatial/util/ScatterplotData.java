/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.awt.geom.Rectangle2D;

/**
 *
 * @author Adam
 */
public class ScatterplotData {

    String layer1;
    String layer1name;
    String layer2;
    String layer2name;
    String pid;
    Rectangle2D.Double selection;
    boolean enabled;
    String lsid;
    String name;
    String backgroundLsid;

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

    public ScatterplotData() {
        enabled = false;
    }

    public ScatterplotData(String lsid, String name, String layer1, String layer1name, String layer2, String layer2name, String pid, Rectangle2D.Double selection, boolean enabled
            ,String backgroundLsid, String filterWkt, String highlightWkt, boolean envGri) {
        this.lsid = lsid;
        this.name = name;
        this.layer1 = layer1;
        this.layer1name = layer1name;
        this.layer2 = layer2;
        this.layer2name = layer2name;
        this.pid = pid;
        this.selection = selection;
        this.enabled = enabled;
        this.backgroundLsid = backgroundLsid;
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

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public void setSpeciesName(String name) {
        this.name = name;
    }

    public String getLsid() {
        return lsid;
    }

    public String getSpeciesName() {
        return name;
    }

    public String getBackgroundLsid() {
        return backgroundLsid;
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

    public void setBackgroundLsid(String backgroundLsid) {
        this.backgroundLsid = backgroundLsid;
    }
}
