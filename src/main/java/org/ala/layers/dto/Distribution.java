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

// @XmlRootElement(name="distribution")
// @XStreamAlias("distribution")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
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
    // additional fields
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
    String bounding_box;

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
        this.estuarine_fl = estuarine_fl != null && estuarine_fl > 0 ? true : false;
    }

    public Boolean getCoastal_fl() {
        return coastal_fl;
    }

    public void setCoastal_fl(Integer coastal_fl) {
        this.coastal_fl = coastal_fl != null && coastal_fl > 0 ? true : false;
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
        this.desmersal_fl = desmersal_fl != null && desmersal_fl > 0 ? true : false;
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
        if (spcode != null) {
            this.spcode = (long) (double) spcode;
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


    public String getBounding_box() {
        return bounding_box;
    }

    public void setBounding_box(String bounding_box) {
        this.bounding_box = bounding_box;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((area_km == null) ? 0 : area_km.hashCode());
        result = prime * result + ((area_name == null) ? 0 : area_name.hashCode());
        result = prime * result + ((authority_ == null) ? 0 : authority_.hashCode());
        result = prime * result + ((caab_family_number == null) ? 0 : caab_family_number.hashCode());
        result = prime * result + ((caab_species_number == null) ? 0 : caab_species_number.hashCode());
        result = prime * result + ((checklist_name == null) ? 0 : checklist_name.hashCode());
        result = prime * result + ((coastal_fl == null) ? 0 : coastal_fl.hashCode());
        result = prime * result + ((common_nam == null) ? 0 : common_nam.hashCode());
        result = prime * result + ((data_resource_uid == null) ? 0 : data_resource_uid.hashCode());
        result = prime * result + ((desmersal_fl == null) ? 0 : desmersal_fl.hashCode());
        result = prime * result + ((estuarine_fl == null) ? 0 : estuarine_fl.hashCode());
        result = prime * result + ((family == null) ? 0 : family.hashCode());
        result = prime * result + ((family_lsid == null) ? 0 : family_lsid.hashCode());
        result = prime * result + ((genus_lsid == null) ? 0 : genus_lsid.hashCode());
        result = prime * result + ((genus_name == null) ? 0 : genus_name.hashCode());
        result = prime * result + ((geom_idx == null) ? 0 : geom_idx.hashCode());
        result = prime * result + ((geometry == null) ? 0 : geometry.hashCode());
        result = prime * result + ((gid == null) ? 0 : gid.hashCode());
        result = prime * result + ((group_name == null) ? 0 : group_name.hashCode());
        result = prime * result + ((image_quality == null) ? 0 : image_quality.hashCode());
        result = prime * result + ((lsid == null) ? 0 : lsid.hashCode());
        result = prime * result + ((max_depth == null) ? 0 : max_depth.hashCode());
        result = prime * result + ((metadata_u == null) ? 0 : metadata_u.hashCode());
        result = prime * result + ((min_depth == null) ? 0 : min_depth.hashCode());
        result = prime * result + ((notes == null) ? 0 : notes.hashCode());
        result = prime * result + ((pelagic_fl == null) ? 0 : pelagic_fl.hashCode());
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((scientific == null) ? 0 : scientific.hashCode());
        result = prime * result + ((spcode == null) ? 0 : spcode.hashCode());
        result = prime * result + ((specific_n == null) ? 0 : specific_n.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Distribution other = (Distribution) obj;
        if (area_km == null) {
            if (other.area_km != null)
                return false;
        } else if (!area_km.equals(other.area_km))
            return false;
        if (area_name == null) {
            if (other.area_name != null)
                return false;
        } else if (!area_name.equals(other.area_name))
            return false;
        if (authority_ == null) {
            if (other.authority_ != null)
                return false;
        } else if (!authority_.equals(other.authority_))
            return false;
        if (caab_family_number == null) {
            if (other.caab_family_number != null)
                return false;
        } else if (!caab_family_number.equals(other.caab_family_number))
            return false;
        if (caab_species_number == null) {
            if (other.caab_species_number != null)
                return false;
        } else if (!caab_species_number.equals(other.caab_species_number))
            return false;
        if (checklist_name == null) {
            if (other.checklist_name != null)
                return false;
        } else if (!checklist_name.equals(other.checklist_name))
            return false;
        if (coastal_fl == null) {
            if (other.coastal_fl != null)
                return false;
        } else if (!coastal_fl.equals(other.coastal_fl))
            return false;
        if (common_nam == null) {
            if (other.common_nam != null)
                return false;
        } else if (!common_nam.equals(other.common_nam))
            return false;
        if (data_resource_uid == null) {
            if (other.data_resource_uid != null)
                return false;
        } else if (!data_resource_uid.equals(other.data_resource_uid))
            return false;
        if (desmersal_fl == null) {
            if (other.desmersal_fl != null)
                return false;
        } else if (!desmersal_fl.equals(other.desmersal_fl))
            return false;
        if (estuarine_fl == null) {
            if (other.estuarine_fl != null)
                return false;
        } else if (!estuarine_fl.equals(other.estuarine_fl))
            return false;
        if (family == null) {
            if (other.family != null)
                return false;
        } else if (!family.equals(other.family))
            return false;
        if (family_lsid == null) {
            if (other.family_lsid != null)
                return false;
        } else if (!family_lsid.equals(other.family_lsid))
            return false;
        if (genus_lsid == null) {
            if (other.genus_lsid != null)
                return false;
        } else if (!genus_lsid.equals(other.genus_lsid))
            return false;
        if (genus_name == null) {
            if (other.genus_name != null)
                return false;
        } else if (!genus_name.equals(other.genus_name))
            return false;
        if (geom_idx == null) {
            if (other.geom_idx != null)
                return false;
        } else if (!geom_idx.equals(other.geom_idx))
            return false;
        if (geometry == null) {
            if (other.geometry != null)
                return false;
        } else if (!geometry.equals(other.geometry))
            return false;
        if (gid == null) {
            if (other.gid != null)
                return false;
        } else if (!gid.equals(other.gid))
            return false;
        if (group_name == null) {
            if (other.group_name != null)
                return false;
        } else if (!group_name.equals(other.group_name))
            return false;
        if (image_quality == null) {
            if (other.image_quality != null)
                return false;
        } else if (!image_quality.equals(other.image_quality))
            return false;
        if (lsid == null) {
            if (other.lsid != null)
                return false;
        } else if (!lsid.equals(other.lsid))
            return false;
        if (max_depth == null) {
            if (other.max_depth != null)
                return false;
        } else if (!max_depth.equals(other.max_depth))
            return false;
        if (metadata_u == null) {
            if (other.metadata_u != null)
                return false;
        } else if (!metadata_u.equals(other.metadata_u))
            return false;
        if (min_depth == null) {
            if (other.min_depth != null)
                return false;
        } else if (!min_depth.equals(other.min_depth))
            return false;
        if (notes == null) {
            if (other.notes != null)
                return false;
        } else if (!notes.equals(other.notes))
            return false;
        if (pelagic_fl == null) {
            if (other.pelagic_fl != null)
                return false;
        } else if (!pelagic_fl.equals(other.pelagic_fl))
            return false;
        if (pid == null) {
            if (other.pid != null)
                return false;
        } else if (!pid.equals(other.pid))
            return false;
        if (scientific == null) {
            if (other.scientific != null)
                return false;
        } else if (!scientific.equals(other.scientific))
            return false;
        if (spcode == null) {
            if (other.spcode != null)
                return false;
        } else if (!spcode.equals(other.spcode))
            return false;
        if (specific_n == null) {
            if (other.specific_n != null)
                return false;
        } else if (!specific_n.equals(other.specific_n))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (wmsurl == null) {
            if (other.wmsurl != null)
                return false;
        } else if (!wmsurl.equals(other.wmsurl))
            return false;
        return true;
    }
}