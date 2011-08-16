/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam
 */
public class Layer {

    static final long minRefreshTime = 120000; //minimum refresh time in ms
    static HashMap<String, Layer> layers;
    static long lastRefresh;
    
    private final String id;
    private final String name;
    private final String description;
    private final String type;
    private final String source;
    private final String path;
    private final String extents;
    private final String minlatitude;
    private final String minlongitude;
    private final String maxlatitude;
    private final String maxlongitude;
    private final String notes;
    private final String displayname;
    private final String displaypath;
    private final String scale;
    private final String environmentalvaluemin;
    private final String environmentalvaluemax;
    private final String environmentalvalueunits;
    private final String lookuptablepath;
    private final String metadatapath;
    private final String classification1;
    private final String classification2;
    private final String uid;
    private final String mddatest;
    private final String citation_date;
    private final String datalang;
    private final String mdhrlv;
    private final String respparty_role;
    private final String licence_level;
    private final String licence_link;
    private final String licence_notes;
    private final String source_link;
    private final String pid;
    private final String path_orig;
    private final String path_1km;
    private final String path_250m;

    private static HashMap<String, Layer> getDBLayers() {
        HashMap<String, Layer> fs = new HashMap<String, Layer>();

        ResultSet rs = DBConnection.query("SELECT * FROM layers WHERE enabled=TRUE;");
        try {
            while (rs != null && rs.next()) {
                fs.put(rs.getString("id"), new Layer(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("source"),
                        rs.getString("path"),
                        rs.getString("extents"),
                        rs.getString("minlatitude"),
                        rs.getString("minlongitude"),
                        rs.getString("maxlatitude"),
                        rs.getString("maxlongitude"),
                        rs.getString("notes"),
                        rs.getString("displayname"),
                        rs.getString("displaypath"),
                        rs.getString("scale"),
                        rs.getString("environmentalvaluemin"),
                        rs.getString("environmentalvaluemax"),
                        rs.getString("environmentalvalueunits"),
                        rs.getString("lookuptablepath"),
                        rs.getString("metadatapath"),
                        rs.getString("classification1"),
                        rs.getString("classification2"),
                        rs.getString("uid"),
                        rs.getString("mddatest"),
                        rs.getString("citation_date"),
                        rs.getString("datalang"),
                        rs.getString("mdhrlv"),
                        rs.getString("respparty_role"),
                        rs.getString("licence_level"),
                        rs.getString("licence_link"),
                        rs.getString("licence_notes"),
                        rs.getString("source_link"),
                        rs.getString("pid"),
                        rs.getString("path_orig"),
                        rs.getString("path_1km"),
                        rs.getString("path_250m")));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Layer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return fs;
    }

    static public Layer getLayer(String id) {
        Layer l = null;
        if (layers == null
                || ((l = layers.get(id)) == null
                && minRefreshTime + System.currentTimeMillis() > lastRefresh)) {
            refreshLayers();

            l = layers.get(id);

            //Temporary
            for(Layer layer : layers.values()) {
                if (layer.name != null && layer.name.equals(id)) {
                    return layer;
                }
            }
        }

        return l;
    }

    static void refreshLayers() {
        //load/reload fields table
        HashMap<String, Layer> ls = getDBLayers();

        if (ls != null && ls.size() > 0) {
            lastRefresh = System.currentTimeMillis();
            layers = ls;
        }
    }

    private Layer(String id, String name, String description, String type, String source, String path, String extents, String minlatitude, String minlongitude, String maxlatitude, String maxlongitude, String notes, String displayname, String displaypath, String scale, String evironmentalvaluemin, String environmentalvaluemax, String environmentalvalueunits, String lookuptablepath, String metadatapath, String classification1, String classification2, String uid, String mddatest, String citation_date, String datalang, String mdhrlv, String respparty_role, String licence_level, String licence_link, String licence_notes, String source_link, String pid, String path_orig, String path_1km, String path_250m) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.source = source;
        this.path = path;
        this.extents = extents;
        this.minlatitude = minlatitude;
        this.minlongitude = minlongitude;
        this.maxlatitude = maxlatitude;
        this.maxlongitude = maxlongitude;
        this.notes = notes;
        this.displayname = displayname;
        this.displaypath = displaypath;
        this.scale = scale;
        this.environmentalvaluemin = evironmentalvaluemin;
        this.environmentalvaluemax = environmentalvaluemax;
        this.environmentalvalueunits = environmentalvalueunits;
        this.lookuptablepath = lookuptablepath;
        this.metadatapath = metadatapath;
        this.classification1 = classification1;
        this.classification2 = classification2;
        this.uid = uid;
        this.mddatest = mddatest;
        this.citation_date = citation_date;
        this.datalang = datalang;
        this.mdhrlv = mdhrlv;
        this.respparty_role = respparty_role;
        this.licence_level = licence_level;
        this.licence_link = licence_link;
        this.licence_notes = licence_notes;
        this.source_link = source_link;
        this.pid = pid;
        this.path_orig = path_orig;
        this.path_1km = path_1km;
        this.path_250m = path_250m;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getPath() {
        return path;
    }

    public String getExtents() {
        return extents;
    }

    public String getMinLatitude() {
        return minlatitude;
    }

    public String getMinLongitude() {
        return minlongitude;
    }

    public String getMaxLatitude() {
        return maxlatitude;
    }

    public String getMaxLongtitude() {
        return maxlongitude;
    }

    public String getNotes() {
        return notes;
    }

    public String getDisplayName() {
        return displayname;
    }

    public String getDisplayPath() {
        return displaypath;
    }

    public String getScale() {
        return scale;
    }

    public String getEnvironmentalValueMin() {
        return environmentalvaluemin;
    }

    public String getEnvironmentalValueMax() {
        return environmentalvaluemax;
    }

    public String getEnvironmentalValueUnits() {
        return environmentalvalueunits;
    }

    public String getLookupTablePath() {
        return lookuptablepath;
    }

    public String getMetadataPath() {
        return metadatapath;
    }

    public String getClassification1() {
        return classification1;
    }

    public String getClassification2() {
        return classification2;
    }

    public String getUid() {
        return uid;
    }

    public String getMddatest() {
        return mddatest;
    }

    public String getCitationDate() {
        return citation_date;
    }

    public String getDatalang() {
        return datalang;
    }

    public String getMdhrlv() {
        return mdhrlv;
    }

    public String getResppartyRole() {
        return respparty_role;
    }

    public String getLicenceLevel() {
        return licence_level;
    }

    public String getLicenceLink() {
        return licence_link;
    }

    public String getLicenceNotes() {
        return licence_notes;
    }

    public String getSourceLink() {
        return source_link;
    }

    public String getPid() {
        return pid;
    }

    public String getPathOrig() {
        return path_orig;
    }

    public String getPath1km() {
        return path_1km;
    }

    public String getPath250m() {
        return path_250m;
    }

    public boolean isShape() {
        return type != null && "contextual".equals(type.toLowerCase());
    }

    public boolean isGrid() {
        return type != null && "environmental".equals(type.toLowerCase());
    }
}
