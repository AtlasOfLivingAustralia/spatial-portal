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
public class GridClass {

    Integer id;
    String name;
    Double area_km;
    String bbox;
    Integer minShapeIdx;
    Integer maxShapeIdx;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setArea_km(Double area_km) {
        this.area_km = area_km;
    }

    public Double getArea_km() {
        return area_km;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    public String getBbox() {
        return bbox;
    }

    public void setMinShapeIdx(Integer minShapeIdx) {
        this.minShapeIdx = minShapeIdx;
    }

    public Integer getMinShapeIdx() {
        return minShapeIdx;
    }

    public void setMaxShapeIdx(Integer maxShapeIdx) {
        this.maxShapeIdx = maxShapeIdx;
    }

    public Integer getMaxShapeIdx() {
        return maxShapeIdx;
    }
}
