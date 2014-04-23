/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.dto;

/**
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
    int occurrences;
    int species;

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

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setSpecies(int species) {
        this.species = species;
    }

    public int getSpecies() {
        return species;
    }

}
