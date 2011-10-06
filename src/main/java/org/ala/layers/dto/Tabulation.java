/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.dto;

/**
 *
 * @author Adam
 */
public class Tabulation {
    String fid1;
    String pid1;
    String name1;
    String fid2;
    String pid2;
    String name2;
    Double area;
    String geometry;

    public void setFid1(String fid1) {
        this.fid1 = fid1;
    }

    public String getFid1() {
        return fid1;
    }

    public void setPid1(String pid1) {
        this.pid1 = pid1;
    }

    public String getPid1() {
        return pid1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName1() {
        return name1;
    }

    public void setFid2(String fid2) {
        this.fid2 = fid2;
    }

    public String getFid2() {
        return fid2;
    }

    public void setPid2(String pid2) {
        this.pid2 = pid2;
    }

    public String getPid2() {
        return pid2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getName2() {
        return name2;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getArea() {
        return area;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }
}
