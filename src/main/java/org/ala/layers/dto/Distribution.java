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
    String checklist_name;
    Double area_km;
    String notes;
    Long geom_idx;
    //additional fields
    String group_name;
    String family_lsid;
    String genus_lsid;
    Boolean estuarine_fl;
    Boolean coastal_fl;
    Boolean desmersal_fl;
    String caab_species_number;
    String caab_family_number;
    String data_resource_uid;
    String image_quality;

    public String getData_resource_uid() {
        return data_resource_uid;
    }

    public void setData_resource_uid(String data_resource_uid) {
        this.data_resource_uid = data_resource_uid;
    }

    public String getImage_quality() {
        return image_quality;
    }

    public void setImage_quality(String image_quality) {
        this.image_quality = image_quality;
    }

    public String getFamily_lsid() {
        return family_lsid;
    }

    public void setFamily_lsid(String family_lsid) {
        this.family_lsid = family_lsid;
    }

    public String getGenus_lsid() {
        return genus_lsid;
    }

    public void setGenus_lsid(String genus_lsid) {
        this.genus_lsid = genus_lsid;
    }

    public Boolean getEstuarine_fl() {
        return estuarine_fl;
    }

    public void setEstuarine_fl(Integer estuarine_fl) {
        this.estuarine_fl = estuarine_fl !=null &&  estuarine_fl >0 ? true : false ;
    }

    public Boolean getCoastal_fl() {
        return coastal_fl;
    }

    public void setCoastal_fl(Integer coastal_fl) {
        this.coastal_fl = coastal_fl !=null &&  coastal_fl >0 ? true : false ;
    }

    public Boolean getDesmersal_fl() {
        return desmersal_fl;
    }

    public void setEstuarine_fl(Boolean estuarine_fl) {
        this.estuarine_fl = estuarine_fl;
    }

    public void setCoastal_fl(Boolean coastal_fl) {
        this.coastal_fl = coastal_fl;
    }

    public void setDesmersal_fl(Boolean desmersal_fl) {
        this.desmersal_fl = desmersal_fl;
    }

    public void setDesmersal_fl(Integer desmersal_fl) {
        this.desmersal_fl = desmersal_fl !=null &&  desmersal_fl >0 ? true : false ;
    }

    public String getCaab_species_number() {
        return caab_species_number;
    }

    public void setCaab_species_number(String caab_species_number) {
        this.caab_species_number = caab_species_number;
    }

    public String getCaab_family_number() {
        return caab_family_number;
    }

    public void setCaab_family_number(String caab_family_number) {
        this.caab_family_number = caab_family_number;
    }

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
    
    public void setChecklist_name(String checklist_name) {
        this.checklist_name = checklist_name;
    }

    public String getChecklist_name() {
        return checklist_name;
    }

    public void setArea_km(Double area_km) {
        this.area_km = area_km;
    }

    public Double getArea_km() {
        return area_km;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setGeom_idx(Long geom_idx) {
        this.geom_idx = geom_idx;
    }

    public Long getGeom_idx() {
        return geom_idx;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }
}