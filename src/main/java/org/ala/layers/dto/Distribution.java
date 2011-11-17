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
 * This class serves as a model object for the "distributions" table
 *
 * @author ajay
 */

//@XmlRootElement(name="distribution")
//@XStreamAlias("distribution")
@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
public class Distribution {
    public static final String EXPERT_DISTRIBUTION = "e";
    public static final String SPECIES_CHECKLIST = "c";

    Long gid;
    Long spcode;
    String scientific;
    String authority_;
    String common_nam;
    String family;
    String genus_name;
    String specific_n;
    Double min_depth;
    Double max_depth;
    Double pelagic_fl;
    String metadata_u;
    String geometry;
    String wmsurl;
    String lsid;
    String type;
    String area_name;
    String pid;

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public Long getGid() {
        return gid;
    }

    public void setSpcode(Long spcode) {
        this.spcode = spcode;
    }
    
    public void setSpcode(Double spcode) {
        if(spcode != null) {
            this.spcode = (long)(double)spcode;
        }
    }

    public Long getSpcode() {
        return spcode;
    }

    public void setScientific(String scientific) {
        this.scientific = scientific;
    }

    public String getScientific() {
        return scientific;
    }


    public void setAuthority_(String authority) {
        this.authority_ = authority;
    }

    public String getAuthority_() {
        return authority_;
    }

    public void setCommon_nam(String common_nam) {
        this.common_nam = common_nam;
    }

    public String getCommon_nam() {
        return common_nam;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }

    public void setGenus_name(String genus_name) {
        this.genus_name = genus_name;
    }

    public String getGenus_name() {
        return genus_name;
    }

    public void setSpecific_n(String specific_n) {
        this.specific_n = specific_n;
    }

    public String getSpecific_n() {
        return specific_n;
    }

    public void setMin_depth(Double min_depth) {
        this.min_depth = min_depth;
    }

    public Double getMin_depth() {
        return min_depth;
    }

    public void setMax_depth(Double max_depth) {
        this.max_depth = max_depth;
    }

    public Double getMax_depth() {
        return max_depth;
    }

    public void setPelagic_fl(Double pelagic_fl) {
        this.pelagic_fl = pelagic_fl;
    }

    public Double getPelagic_fl() {
        return pelagic_fl;
    }

    public void setMetadata_u(String metadata_u) {
        this.metadata_u = metadata_u;
    }

    public String getMetadata_u() {
        return metadata_u;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setWmsurl(String wmsurl) {
        this.wmsurl = wmsurl;
    }

    public String getWmsurl() {
        return wmsurl;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String getLsid() {
        return lsid;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setArea_name(String area_name) {
        this.area_name = area_name;
    }

    public String getArea_name() {
        return area_name;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }
}
