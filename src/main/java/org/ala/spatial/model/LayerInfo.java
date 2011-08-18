package org.ala.spatial.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.List;
import java.util.Vector;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class serves as a model object for a list of layers
 * served by the ALA Spatial Portal
 *
 * @author ajay
 */

@Entity
@Table(name = "layers")
@XmlRootElement(name="layer")
@XStreamAlias("layer")
public class LayerInfo {
    @Id
    @GeneratedValue ( strategy = GenerationType.SEQUENCE, generator="layers_id_seq")
    @SequenceGenerator(name = "layers_id_seq", sequenceName = "layers_id_seq")
    @Column(name = "id", insertable = false, updatable = false)
    private long id;

    @Column(name="uid")
    private String uid;

    @Column(name="name")
    private String name;

    @Column(name="displayname")
    private String displayname;

    @Column(name="description")
    private String description;

    @Column(name="type")
    private String type;

    @Column(name="source")
    private String source;

    @Column(name="path")
    private String path;

    @Column(name="displaypath")
    private String displaypath;

    @Column(name="scale")
    private String scale;

    @Column(name="extents")
    private String extent;

    @Column(name="minlatitude")
    private double minlatitude;

    @Column(name="minlongitude")
    private double minlongitude;

    @Column(name="maxlatitude")
    private double maxlatitude;

    @Column(name="maxlongitude")
    private double maxlongitude;

    @Column(name="notes")
    private String notes;

    @Column(name="enabled")
    private boolean enabled;

    @Column(name="environmentalvaluemin")
    private String environmentalvaluemin;

    @Column(name="environmentalvaluemax")
    private String environmentalvaluemax;

    @Column(name="environmentalvalueunits")
    private String environmentalvalueunits;

    @Column(name="lookuptablepath")
    private String lookuptablepath;

    @Column(name="metadatapath")
    private String metadatapath;

    @Column(name="classification1")
    private String classification1;

    @Column(name="classification2")
    private String classification2;

    @Column(name="mddatest")
    private String mddatest;

    @Column(name="citation_date")
    private String citationdate;

    @Column(name="datalang")
    private String datalang;

    @Column(name="mdhrlv")
    private String mdhrlv;

    @Column(name="respparty_role")
    private String resppartyrole;

    @Column(name="licence_level")
    private String licencelevel;

    @Column(name="licence_link")
    private String licence_link;

    @Column(name="licence_notes")
    private String licence_notes;

    @Column(name="source_link")
    private String sourcelink;

    @Column(name="keywords")
    private String keywords;

//    @GeneratedValue
//    private String capabilities;
//
//    @GeneratedValue
//    private String preview;
//
    public LayerInfo() {
    }

    public LayerInfo(String name, String type) {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getMaxlatitude() {
        return maxlatitude;
    }

    public void setMaxlatitude(double maxlatitude) {
        this.maxlatitude = maxlatitude;
    }

    public double getMaxlongitude() {
        return maxlongitude;
    }

    public void setMaxlongitude(double maxlongitude) {
        this.maxlongitude = maxlongitude;
    }

    public double getMinlatitude() {
        return minlatitude;
    }

    public void setMinlatitude(double minlatitude) {
        this.minlatitude = minlatitude;
    }

    public double getMinlongitude() {
        return minlongitude;
    }

    public void setMinlongitude(double minlongitude) {
        this.minlongitude = minlongitude;
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

    public String getCitationdate() {
        return citationdate;
    }

    public void setCitationdate(String citationdate) {
        this.citationdate = citationdate;
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

    public String getLicencelevel() {
        return licencelevel;
    }

    public void setLicencelevel(String licencelevel) {
        this.licencelevel = licencelevel;
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

    public String getResppartyrole() {
        return resppartyrole;
    }

    public void setResppartyrole(String resppartyrole) {
        this.resppartyrole = resppartyrole;
    }

    public String getSourcelink() {
        return sourcelink;
    }

    public void setSourcelink(String sourcelink) {
        this.sourcelink = sourcelink;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
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
        v.add(displayname);
        v.add(description.replaceAll("\n", " "));
        v.add(source);
        v.add(sourcelink);
        v.add(resppartyrole);
        v.add(mddatest);
        v.add(citationdate);
        v.add(licencelevel);
        v.add(licence_link);
        v.add(licence_notes.replaceAll("\n", " "));
        v.add(type);
        v.add(classification1);
        v.add(classification2);
        v.add(environmentalvalueunits);
        v.add(datalang);
        v.add(mdhrlv);
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
        lyr += "\"" + sourcelink + "\", ";
        lyr += "\"" + resppartyrole + "\", ";
        lyr += "\"" + mddatest + "\", ";
        lyr += "\"" + citationdate + "\", ";
        lyr += "\"" + licencelevel + "\", ";
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

}
