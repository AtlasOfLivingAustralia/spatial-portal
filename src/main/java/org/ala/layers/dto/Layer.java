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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This class serves as a model object for a list of layers
 * served by the ALA Spatial Portal
 *
 * @author ajay
 */

@Entity
@Table(name = "layers")
//@XmlRootElement(name="layer")
//@XStreamAlias("layer")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Layer {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "layers_id_seq")
    @SequenceGenerator(name = "layers_id_seq", sequenceName = "layers_id_seq")
    @Column(name = "id", insertable = false, updatable = false)
    private Long id;

    @Column(name = "uid")
    private String uid;

    @Column(name = "name")
    private String name;

    @Column(name = "displayname")
    private String displayname;

    @Column(name = "description")
    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "source")
    private String source;

    @Column(name = "path")
    private String path;

    @Column(name = "displaypath")
    private String displaypath;

    @Column(name = "scale")
    private String scale;

    @Column(name = "extents")
    private String extent;

    @Column(name = "minlatitude")
    private Double minlatitude;

    @Column(name = "minlongitude")
    private Double minlongitude;

    @Column(name = "maxlatitude")
    private Double maxlatitude;

    @Column(name = "maxlongitude")
    private Double maxlongitude;

    @Column(name = "notes")
    private String notes;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "environmentalvaluemin")
    private String environmentalvaluemin;

    @Column(name = "environmentalvaluemax")
    private String environmentalvaluemax;

    @Column(name = "environmentalvalueunits")
    private String environmentalvalueunits;

    @Column(name = "lookuptablepath")
    private String lookuptablepath;

    @Column(name = "metadatapath")
    private String metadatapath;

    @Column(name = "classification1")
    private String classification1;

    @Column(name = "classification2")
    private String classification2;

    @Column(name = "mddatest")
    private String mddatest;

    @Column(name = "citation_date")
    private String citation_date;

    @Column(name = "datalang")
    private String datalang;

    @Column(name = "mdhrlv")
    private String mdhrlv;

    @Column(name = "respparty_role")
    private String respparty_role;

    @Column(name = "licence_level")
    private String licence_level;

    @Column(name = "licence_link")
    private String licence_link;

    @Column(name = "licence_notes")
    private String licence_notes;

    @Column(name = "source_link")
    private String source_link;

    @Column(name = "keywords")
    private String keywords;

    @Column(name = "path_orig")
    private String path_orig;

    @Column(name = "path_1km")
    private String path_1km;

    @Column(name = "path_250m")
    private String path_250m;

    @Column(name = "domain")
    private String domain;

    @Column(name = "pid")
    private String pid;


    //    @GeneratedValue
//    private String capabilities;
//
//    @GeneratedValue
//    private String preview;
//
    public Layer() {
    }

    public Layer(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

//    public Boolean isEnabled() {
//        return enabled;
//    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getMaxlatitude() {
        return maxlatitude;
    }

    public void setMaxlatitude(Double maxlatitude) {
        if (maxlatitude != null) {
            this.maxlatitude = maxlatitude.doubleValue();
        }
    }

    public Double getMaxlongitude() {
        return maxlongitude;
    }

    public void setMaxlongitude(Double maxlongitude) {
        if (maxlongitude != null) {
            this.maxlongitude = maxlongitude.doubleValue();
        }
    }

    public Double getMinlatitude() {
        return minlatitude;
    }

    public void setMinlatitude(Double minlatitude) {
        if (minlatitude != null) {
            this.minlatitude = minlatitude.doubleValue();
        }
    }

    public Double getMinlongitude() {
        return minlongitude;
    }

    public void setMinlongitude(Double minlongitude) {
        if (minlongitude != null) {
            this.minlongitude = minlongitude.doubleValue();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        //setCapabilities(name);
        //setPreview(preview);
        //this.capabilities = "http://spatial.ala.org.au/geoserver/wms?request=getCapabilities";
        //this.preview = "http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:"+this.name+"&width=300";
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDisplaypath() {
        return displaypath;
    }

    public void setDisplaypath(String displaypath) {
        this.displaypath = displaypath;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public String getClassification1() {
        return classification1;
    }

    public void setClassification1(String classification1) {
        this.classification1 = classification1;
    }

    public String getClassification2() {
        return classification2;
    }

    public void setClassification2(String classification2) {
        this.classification2 = classification2;
    }

    public String getEnvironmentalvaluemax() {
        return environmentalvaluemax;
    }

    public void setEnvironmentalvaluemax(String environmentalvaluemax) {
        this.environmentalvaluemax = environmentalvaluemax;
    }

    public String getEnvironmentalvaluemin() {
        return environmentalvaluemin;
    }

    public void setEnvironmentalvaluemin(String environmentalvaluemin) {
        this.environmentalvaluemin = environmentalvaluemin;
    }

    public String getEnvironmentalvalueunits() {
        return environmentalvalueunits;
    }

    public void setEnvironmentalvalueunits(String environmentalvalueunits) {
        this.environmentalvalueunits = environmentalvalueunits;
    }

    public String getLookuptablepath() {
        return lookuptablepath;
    }

    public void setLookuptablepath(String lookuptablepath) {
        this.lookuptablepath = lookuptablepath;
    }

    public String getMetadatapath() {
        return metadatapath;
    }

    public void setMetadatapath(String metadatapath) {
        this.metadatapath = metadatapath;
    }

    public String getCitation_date() {
        return citation_date;
    }

    public void setCitation_date(String citationdate) {
        this.citation_date = citationdate;
    }

    public String getDatalang() {
        return datalang;
    }

    public void setDatalang(String datalang) {
        this.datalang = datalang;
    }

    public String getLicence_link() {
        return licence_link;
    }

    public void setLicence_link(String licence_link) {
        this.licence_link = licence_link;
    }

    public String getLicence_notes() {
        return licence_notes;
    }

    public void setLicence_notes(String licence_notes) {
        this.licence_notes = licence_notes;
    }

    public String getLicence_level() {
        return licence_level;
    }

    public void setLicence_level(String licencelevel) {
        this.licence_level = licencelevel;
    }

    public String getMddatest() {
        return mddatest;
    }

    public void setMddatest(String mddatest) {
        this.mddatest = mddatest;
    }

    public String getMdhrlv() {
        return mdhrlv;
    }

    public void setMdhrlv(String mdhrlv) {
        this.mdhrlv = mdhrlv;
    }

    public String getRespparty_role() {
        return respparty_role;
    }

    public void setRespparty_role(String resppartyrole) {
        this.respparty_role = resppartyrole;
    }

    public String getSource_link() {
        return source_link;
    }

    public void setSource_link(String sourcelink) {
        this.source_link = sourcelink;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getPath_1km() {
        return path_1km;
    }

    public void setPath_1km(String path1km) {
        this.path_1km = path1km;
    }

    public String getPath_250m() {
        return path_250m;
    }

    public void setPath250m(String path250m) {
        this.path_250m = path250m;
    }

    public String getPath_orig() {
        return path_orig;
    }

    public void setPath_orig(String pathorig) {
        this.path_orig = pathorig;
    }

    public String getdomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

//    public String getCapabilities() {
//        return capabilities;
//    }
//
//    private void setCapabilities(String capabilities) {
//        //this.capabilities = capabilities;
//        this.capabilities = "http://spatial.ala.org.au/geoserver/wms?request=getCapabilities";
//    }
//
//    public String getPreview() {
//        return preview;
//    }
//
//    private void setPreview(String preview) {
//        //this.preview = preview;
//        //this.preview = "http://spatial.ala.org.au/geoserver/wms/reflect?layers=ALA:"+this.name+"&width=300";
//    }

    public String[] toArray() {
        if (description == null) {
            description = "";
        }
        if (licence_notes == null) {
            licence_notes = "";
        }
        if (notes == null) {
            notes = "";
        }
        if (keywords == null) {
            keywords = "";
        }

        List v = new Vector();
        v.add(uid);
        v.add(name);
        v.add(displayname);
        v.add(description.replaceAll("\n", " "));
        v.add(source);
        v.add(source_link);
        v.add(respparty_role);
        v.add(mddatest);
        v.add(citation_date);
        v.add(licence_level);
        v.add(licence_link);
        v.add(licence_notes.replaceAll("\n", " "));
        v.add(type);
        v.add(classification1);
        v.add(classification2);
        v.add(environmentalvalueunits);
        v.add(notes.replaceAll("\n", " "));
        v.add(metadatapath);
        v.add(keywords);

        return (String[]) v.toArray(new String[v.size()]);

    }

    @Override
    public String toString() {

        if (description == null) {
            description = "";
        }
        if (licence_notes == null) {
            licence_notes = "";
        }
        if (notes == null) {
            notes = "";
        }
        if (keywords == null) {
            keywords = "";
        }

        //return uid + "," + displayname + "description=" + description + "type=" + type + "source=" + source + "path=" + path + "displaypath=" + displaypath + "scale=" + scale + "extent=" + extent + "minlatitude=" + minlatitude + "minlongitude=" + minlongitude + "maxlatitude=" + maxlatitude + "maxlongitude=" + maxlongitude + "notes=" + notes + "enabled=" + enabled + "environmentalvaluemin=" + environmentalvaluemin + "environmentalvaluemax=" + environmentalvaluemax + "environmentalvalueunits=" + environmentalvalueunits + "lookuptablepath=" + lookuptablepath + "metadatapath=" + metadatapath + "classification1=" + classification1 + "classification2=" + classification2 + "mddatest=" + mddatest + "citationdate=" + citationdate + "datalang=" + datalang + "mdhrlv=" + mdhrlv + "resppartyrole=" + resppartyrole + "licencelevel=" + licencelevel + "licence_link=" + licence_link + "licence_notes=" + licence_notes + "sourcelink=" + sourcelink + '}';
        String lyr = "";
        lyr += "\"" + uid + "\", ";
        lyr += "\"" + displayname + "\", ";
        lyr += "\"" + description.replaceAll("\n", " ").replaceAll("\"", "\\\"") + "\", "; //
        lyr += "\"" + source + "\", ";
        lyr += "\"" + source_link + "\", ";
        lyr += "\"" + respparty_role + "\", ";
        lyr += "\"" + mddatest + "\", ";
        lyr += "\"" + citation_date + "\", ";
        lyr += "\"" + licence_level + "\", ";
        lyr += "\"" + licence_link + "\", ";
        lyr += "\"" + licence_notes.replaceAll("\n", " ").replaceAll("\"", "\\\"") + "\", "; //
        lyr += "\"" + type + "\", ";
        lyr += "\"" + classification1 + "\", ";
        lyr += "\"" + classification2 + "\", ";
        lyr += "\"" + environmentalvalueunits + "\", ";
        lyr += "\"" + datalang + "\", ";
        lyr += "\"" + mdhrlv + "\", ";
        lyr += "\"" + notes.replaceAll("\n", " ").replaceAll("\"", "\\\"") + "\", "; //
        lyr += "\"" + metadatapath + "\", ";
        lyr += "\"" + keywords + "\"";

        return lyr;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<String, Object>();

        m.put("citation_date", citation_date);
        m.put("classification1", classification1);
        m.put("classification2", classification2);
        m.put("datalang", datalang);
        m.put("description", description);
        m.put("displayname", displayname);
        m.put("displaypath", displaypath);
        m.put("enabled", enabled);
        m.put("domain", domain);
        m.put("environmentalvaluemax", environmentalvaluemax);
        m.put("environmentalvaluemin", environmentalvaluemin);
        m.put("environmentalvalueunits", environmentalvalueunits);
        m.put("extents", extent);
        m.put("keywords", keywords);
        m.put("licence_link", licence_link);
        m.put("licence_notes", licence_notes);
        m.put("licence_level", licence_level);
        m.put("lookuptablepath", lookuptablepath);
        m.put("maxlatitude", maxlatitude);
        m.put("maxlongitude", maxlongitude);
        m.put("mddatest", mddatest);
        m.put("mdhrlv", mdhrlv);
        m.put("metadatapath", metadatapath);
        m.put("minlatitude", minlatitude);
        m.put("minlongitude", minlongitude);
        m.put("name", name);
        m.put("notes", notes);
        m.put("path", path);
        m.put("path_1km", path_1km);
        m.put("path_250m", path_250m);
        m.put("path_orig", path_orig);
        m.put("pid", pid);
        m.put("respparty_role", respparty_role);
        m.put("scale", scale);
        m.put("source", source);
        m.put("source_link", source_link);
        m.put("type", type);
        m.put("uid", uid);
        m.put("id", id);

        return m;
    }

    public boolean isShape() {
        return type != null && "contextual".equals(type.toLowerCase());
    }

    public boolean isGrid() {
        return type != null && "environmental".equals(type.toLowerCase());
    }
}
