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

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This class serves as a model object for a list of objects
 * served by the ALA Spatial Portal
 *
 * @author ajay
 */
//@XmlRootElement(name="objects")
//@XStreamAlias("objects")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Objects {

    private String id;
    private String pid;
    private String description;
    private String name;
    private String fid;
    private String fieldname;
    private String geometry;
    private int name_id;
    private String bbox;
    private Double area_km;
    private Double degrees;
    private Double distance;
    private String wmsurl;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getFieldname() {
        return fieldname;
    }

    public void setFieldname(String fieldname) {
        this.fieldname = fieldname;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getName_id() {
        return name_id;
    }

    public void setName_id(int nameId) {
        this.name_id = nameId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    public Double getArea_km() {
        return area_km;
    }

    public void setArea_km(Double area_km) {
        this.area_km = area_km;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getDegrees() {
        return degrees;
    }

    public void setDegrees(Double degrees) {
        this.degrees = degrees;
    }

    public void setWmsurl(String wmsurl) {
        this.wmsurl = wmsurl;
    }

    public String getWmsurl() {
        return wmsurl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((area_km == null) ? 0 : area_km.hashCode());
        result = prime * result + ((bbox == null) ? 0 : bbox.hashCode());
        result = prime * result + ((degrees == null) ? 0 : degrees.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((distance == null) ? 0 : distance.hashCode());
        result = prime * result + ((fid == null) ? 0 : fid.hashCode());
        result = prime * result + ((fieldname == null) ? 0 : fieldname.hashCode());
        result = prime * result + ((geometry == null) ? 0 : geometry.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + name_id;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((wmsurl == null) ? 0 : wmsurl.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Objects other = (Objects) obj;
        if (area_km == null) {
            if (other.area_km != null)
                return false;
        } else if (!area_km.equals(other.area_km))
            return false;
        if (bbox == null) {
            if (other.bbox != null)
                return false;
        } else if (!bbox.equals(other.bbox))
            return false;
        if (degrees == null) {
            if (other.degrees != null)
                return false;
        } else if (!degrees.equals(other.degrees))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (distance == null) {
            if (other.distance != null)
                return false;
        } else if (!distance.equals(other.distance))
            return false;
        if (fid == null) {
            if (other.fid != null)
                return false;
        } else if (!fid.equals(other.fid))
            return false;
        if (fieldname == null) {
            if (other.fieldname != null)
                return false;
        } else if (!fieldname.equals(other.fieldname))
            return false;
        if (geometry == null) {
            if (other.geometry != null)
                return false;
        } else if (!geometry.equals(other.geometry))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (name_id != other.name_id)
            return false;
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        if (wmsurl == null) {
            if (other.wmsurl != null)
                return false;
        } else if (!wmsurl.equals(other.wmsurl))
            return false;
        return true;
    }


}
