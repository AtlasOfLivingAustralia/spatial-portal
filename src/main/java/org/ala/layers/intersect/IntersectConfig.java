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
package org.ala.layers.intersect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Layer;
import org.ala.layers.grid.GridClassBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * @author Adam
 */
public class IntersectConfig {

    public static final String GEOSERVER_URL_PLACEHOLDER = "<COMMON_GEOSERVER_URL>";
    public static final String GEONETWORK_URL_PLACEHOLDER = "<COMMON_GEONETWORK_URL>";

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(IntersectConfig.class);
    static final String ALASPATIAL_OUTPUT_PATH = "ALASPATIAL_OUTPUT_PATH";
    static final String LAYER_FILES_PATH = "LAYER_FILES_PATH";
    static final String ANALYSIS_LAYER_FILES_PATH = "ANALYSIS_LAYER_FILES_PATH";
    static final String LAYER_INDEX_URL = "LAYER_INDEX_URL";
    static final String BATCH_THREAD_COUNT = "BATCH_THREAD_COUNT";
    static final String CONFIG_RELOAD_WAIT = "CONFIG_RELOAD_WAIT";
    static final String PRELOADED_SHAPE_FILES = "PRELOADED_SHAPE_FILES";
    static final String GRID_BUFFER_SIZE = "GRID_BUFFER_SIZE";
    static final String GRID_CACHE_PATH = "GRID_CACHE_PATH";
    static final String GRID_CACHE_READER_COUNT = "GRID_CACHE_READER_COUNT";
    static final String LOCAL_SAMPLING = "LOCAL_SAMPLING";
    static final String GEOSERVER_URL = "GEOSERVER_URL";
    static final String GEONETWORK_URL = "GEONETWORK_URL";
    static final String GDAL_PATH = "GDAL_PATH";
    static final String ANALYSIS_RESOLUTIONS = "ANALYSIS_RESOLUTIONS";
    static final String OCCURRENCE_SPECIES_RECORDS_FILENAME = "OCCURRENCE_SPECIES_RECORDS_FILENAME";
    static final String UPLOADED_SHAPES_FIELD_ID = "UPLOADED_SHAPES_FIELD_ID";
    static final String API_KEY_CHECK_URL_TEMPLATE = "API_CHECK_CHECK_URL_TEMPLATE";
    static final String SPATIAL_PORTAL_APP_NAME = "SPATIAL_PORTAL_APP_NAME";
    static final String LAYER_PROPERTIES = "layer.properties";
    static ObjectMapper mapper = new ObjectMapper();
    private FieldDAO fieldDao;
    private LayerDAO layerDao;
    static String layerFilesPath;
    static String analysisLayerFilesPath;
    static String alaspatialOutputPath;
    static String layerIndexUrl;
    static int batchThreadCount;
    static long configReloadWait;
    long lastReload;
    static String preloadedShapeFiles;
    static int gridBufferSize;
    SimpleShapeFileCache shapeFileCache;
    HashMap<String, IntersectionFile> intersectionFiles;
    static String gridCachePath;
    static int gridCacheReaderCount;
    HashMap<String, HashMap<Integer, GridClass>> classGrids;
    static boolean localSampling;
    static String geoserverUrl;
    static String geonetworkUrl;
    static String gdalPath;
    static List<Double> analysisResolutions;
    static String occurrenceSpeciesRecordsFilename;
    static String uploadedShapesFieldId;
    static String apiKeyCheckUrlTemplate;
    static String spatialPortalAppName;

    static {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            String pth = "/data/layers-store/config/layers-store-config.properties";
            logger.debug("config path: " + pth);
            is = new FileInputStream(pth);
            if (is != null) {
                properties.load(is);
            } else {
                String msg = "cannot get properties file: " + IntersectConfig.class.getResource(LAYER_PROPERTIES).getFile();
                logger.warn(msg);
            }
        } catch (IOException ex) {
            logger.error(null, ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("failed to close layers-store-config.properties", e);
                }
            }
        }

        layerFilesPath = getProperty(LAYER_FILES_PATH, properties, null);
        isValidPath(layerFilesPath, LAYER_FILES_PATH);
        analysisLayerFilesPath = getProperty(ANALYSIS_LAYER_FILES_PATH, properties, null);
        isValidPath(analysisLayerFilesPath, ANALYSIS_LAYER_FILES_PATH);
        alaspatialOutputPath = getProperty(ALASPATIAL_OUTPUT_PATH, properties, null);
        isValidPath(alaspatialOutputPath, ALASPATIAL_OUTPUT_PATH);
        layerIndexUrl = getProperty(LAYER_INDEX_URL, properties, null);
        isValidUrl(layerIndexUrl, LAYER_INDEX_URL);
        batchThreadCount = (int) getPositiveLongProperty(BATCH_THREAD_COUNT, properties, 1);
        configReloadWait = getPositiveLongProperty(CONFIG_RELOAD_WAIT, properties, 3600000);
        preloadedShapeFiles = getProperty(PRELOADED_SHAPE_FILES, properties, null);
        gridBufferSize = (int) getPositiveLongProperty(GRID_BUFFER_SIZE, properties, 4096);
        gridCachePath = getProperty(GRID_CACHE_PATH, properties, null);
        gridCacheReaderCount = (int) getPositiveLongProperty(GRID_CACHE_READER_COUNT, properties, 10);
        localSampling = getProperty(LOCAL_SAMPLING, properties, "true").toLowerCase().equals("true");
        geoserverUrl = getProperty(GEOSERVER_URL, properties, null);
        isValidUrl(geoserverUrl, GEOSERVER_URL);
        geonetworkUrl = getProperty(GEONETWORK_URL, properties, null);
        isValidUrl(geonetworkUrl, GEONETWORK_URL);
        gdalPath = getProperty(GDAL_PATH, properties, null);
        isValidPathGDAL(gdalPath, GDAL_PATH);
        analysisResolutions = getDoublesFrom(getProperty(ANALYSIS_RESOLUTIONS, properties, "0.5"));
        occurrenceSpeciesRecordsFilename = getProperty(OCCURRENCE_SPECIES_RECORDS_FILENAME, properties, null);
        uploadedShapesFieldId = getProperty(UPLOADED_SHAPES_FIELD_ID, properties, null);
        apiKeyCheckUrlTemplate = getProperty(API_KEY_CHECK_URL_TEMPLATE, properties, null);
        isValidUrl(apiKeyCheckUrlTemplate, API_KEY_CHECK_URL_TEMPLATE);
        spatialPortalAppName = getProperty(SPATIAL_PORTAL_APP_NAME, properties, null);
    }

    private static void isValidPath(String path, String desc) {
        File f = new File(path);

        if(!f.exists()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  It does not exist.");
        } else if(!f.isDirectory()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  It is not a directory.");
        } else if (!f.canRead()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  Not permitted to READ.");
        } else if (!f.canWrite()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  Not permitted to WRITE.");
        }

    }

    private static void isValidPathGDAL(String path, String desc) {
        File f = new File(path);

        if(!f.exists()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\" is not a valid local file path.  It does not exist.");
        } else if(!f.isDirectory()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  It is not a directory.");
        } else if (!f.canRead()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  Not permitted to READ.");
        }

        //look for GDAL file "gdalwarp"
        File g = new File(path + File.separator + "gdalwarp");
        if(!f.exists()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  gdalwarp does not exist.");
        } else if(!g.canExecute()) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + path + "\"  is not a valid local file path.  gdalwarp not permitted to EXECUTE.");
        }
    }

    private static void isValidUrl(String url, String desc) {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);

            if(result != 200) {
                logger.error("Config error. Property \"" + desc + "\" with value \"" + url + "\"  is not a valid URL.  Error executing GET request, response=" + result);
            }
        } catch (Exception e) {
            logger.error("Config error. Property \"" + desc + "\" with value \"" + url + "\"  is not a valid URL.  Error executing GET request.");
        }

    }

    public IntersectConfig(FieldDAO fieldDao, LayerDAO layerDao) {
        this.fieldDao = fieldDao;
        this.layerDao = layerDao;

        load();
    }

    public static void setPreloadedShapeFiles(String preloadedShapeFiles) {
        IntersectConfig.preloadedShapeFiles = preloadedShapeFiles;
    }

    public void load() {
        lastReload = System.currentTimeMillis();

        try {
            updateIntersectionFiles();
            updateShapeFileCache();
        } catch (Exception e) {
            //if it fails, set reload wait low
            logger.error("load failed, retry in 30s", e);
            configReloadWait = 30000;
        }
    }

    static String getProperty(String property, Properties properties, String defaultValue) {
        String p = System.getProperty(property);
        if (p == null) {
            p = properties.getProperty(property);
        }
        if (p == null) {
            p = defaultValue;
        }
        logger.info(property + " > " + p);
        return p;
    }

    static long getPositiveLongProperty(String property, Properties properties, long defaultValue) {
        String p = getProperty(property, properties, null);
        long l = defaultValue;
        try {
            l = Long.parseLong(p);
            if (l < 0) {
                l = defaultValue;
            }
        } catch (NumberFormatException ex) {
            logger.error("parsing " + property + ": " + p + ", using default: " + defaultValue, ex);
        }
        return l;
    }

    static public String getAlaspatialOutputPath() {
        return alaspatialOutputPath;
    }

    static public String getLayerFilesPath() {
        return layerFilesPath;
    }

    static public String getLayerIndexUrl() {
        return layerIndexUrl;
    }

    static public int getThreadCount() {
        return batchThreadCount;
    }

    public IntersectionFile getIntersectionFile(String fieldId) {
        return intersectionFiles.get(fieldId);
    }

    public String getFieldIdFromFile(String file) {
        String off, on;
        if (File.separator.equals("/")) {
            off = "\\";
            on = "/";
        } else {
            on = "\\";
            off = "/";
        }
        file = file.replace(off, on);
        for (Entry<String, IntersectionFile> entry : intersectionFiles.entrySet()) {
            if (entry.getValue().getFilePath().replace(off, on).equalsIgnoreCase(file)) {
                return entry.getKey();
            }
        }
        return file;
    }

    private void updateIntersectionFiles() throws MalformedURLException, IOException {
        if (intersectionFiles == null) {
            intersectionFiles = new HashMap<String, IntersectionFile>();
            classGrids = new HashMap<String, HashMap<Integer, GridClass>>();
        }

        if (layerIndexUrl != null) {
            //request from url
            JSONArray layers = JSONArray.fromObject(getUrl(layerIndexUrl + "/layers"));
            HashMap<String, String> layerPathOrig = new HashMap<String, String>();
            HashMap<String, String> layerName = new HashMap<String, String>();
            HashMap<String, String> layerType = new HashMap<String, String>();
            HashMap<String, String> layerPid = new HashMap<String, String>();
            for (int i = 0; i < layers.size(); i++) {
                layerPathOrig.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("path_orig"));
                layerName.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("name"));
                layerType.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("type"));
                layerPid.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("id"));
            }

            JSONArray fields = JSONArray.fromObject(getUrl(layerIndexUrl + "/fieldsdb"));
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jo = fields.getJSONObject(i);
                String spid = jo.getString("spid");
                if (layerPathOrig.get(spid) == null) {
                    logger.error("cannot find layer with id '" + spid + "'");
                    continue;
                }
                HashMap<Integer, GridClass> gridClasses =
                        getGridClasses(layerFilesPath + layerPathOrig.get(spid), layerType.get(spid));

                intersectionFiles.put(jo.getString("id"),
                        new IntersectionFile(jo.getString("name"),
                                layerFilesPath + layerPathOrig.get(spid),
                                (jo.containsKey("sname") ? jo.getString("sname") : null),
                                layerName.get(spid),
                                jo.getString("id"),
                                jo.getString("name"),
                                layerPid.get(spid),
                                jo.getString("type"),
                                gridClasses));
                //also register it under the layer name
                intersectionFiles.put(layerName.get(spid),
                        new IntersectionFile(jo.getString("name"),
                                layerFilesPath + layerPathOrig.get(spid),
                                (jo.containsKey("sname") ? jo.getString("sname") : null),
                                layerName.get(jo.getString("spid")),
                                jo.getString("id"),
                                jo.getString("name"),
                                layerPid.get(spid),
                                jo.getString("type"),
                                gridClasses));
                //also register it under the layer pid
                intersectionFiles.put(layerPid.get(spid),
                        new IntersectionFile(jo.getString("name"),
                                layerFilesPath + layerPathOrig.get(spid),
                                (jo.containsKey("sname") ? jo.getString("sname") : null),
                                layerName.get(jo.getString("spid")),
                                jo.getString("id"),
                                jo.getString("name"),
                                layerPid.get(spid),
                                jo.getString("type"),
                                gridClasses));
                classGrids.put(jo.getString("id"), gridClasses);
            }
        } else {
            for (Field f : fieldDao.getFields()) {
                if (f.isEnabled()) {
                    Layer layer = layerDao.getLayerById(Integer.parseInt(f.getSpid()));
                    if (layer == null) {
                        logger.error("cannot find layer with id '" + f.getSpid() + "'");
                        continue;
                    }
                    HashMap<Integer, GridClass> gridClasses = getGridClasses(getLayerFilesPath() + layer.getPath_orig(), layer.getType());
                    intersectionFiles.put(f.getId(),
                            new IntersectionFile(f.getName(),
                                    getLayerFilesPath() + layer.getPath_orig(),
                                    f.getSname(),
                                    layer.getName(),
                                    f.getId(),
                                    f.getName(),
                                    String.valueOf(layer.getId()),
                                    f.getType(),
                                    gridClasses));
                    //also register it under the layer name
                    intersectionFiles.put(layer.getName(),
                            new IntersectionFile(f.getName(),
                                    getLayerFilesPath() + layer.getPath_orig(),
                                    f.getSname(),
                                    layer.getName(),
                                    f.getId(),
                                    f.getName(),
                                    String.valueOf(layer.getId()),
                                    f.getType(),
                                    gridClasses));
                    //also register it under the layer pid
                    intersectionFiles.put(String.valueOf(layer.getId()),
                            new IntersectionFile(f.getName(),
                                    getLayerFilesPath() + layer.getPath_orig(),
                                    f.getSname(),
                                    layer.getName(),
                                    f.getId(),
                                    f.getName(),
                                    String.valueOf(layer.getId()),
                                    f.getType(),
                                    gridClasses));
                    classGrids.put(f.getId(), gridClasses);
                }
            }
        }
    }

    String getUrl(String url) {
        try {
            logger.info("opening url: " + url);

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void updateShapeFileCache() {
        if (preloadedShapeFiles == null) {
            return;
        }

        String[] fields = preloadedShapeFiles.split(",");

        //requres readLayerInfo() first
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        String[] fid = new String[fields.length];
        if (fields.length == 1 && fields[0].equalsIgnoreCase("all")) {
            int countCL = 0;
            for (String s : intersectionFiles.keySet()) {
                if (s.startsWith("cl")) {
                    countCL++;
                }
            }
            layers = new String[countCL];
            columns = new String[countCL];
            fid = new String[countCL];
            int i = 0;
            for (IntersectionFile f : intersectionFiles.values()) {
                if (f.getFieldId().startsWith("cl")) {
                    layers[i] = f.getFilePath();
                    columns[i] = f.getShapeFields();
                    fid[i] = f.getFieldId();
                    i++;
                }
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                layers[i] = getIntersectionFile(fields[i].trim()).getFilePath();
                columns[i] = getIntersectionFile(fields[i].trim()).getShapeFields();
                fid[i] = fields[i];
            }
        }

        if (shapeFileCache == null) {
            shapeFileCache = new SimpleShapeFileCache(layers, columns, fid);
        } else {
            shapeFileCache.update(layers, columns, fid);
        }
    }

    /**
     * Add shape files to the shape file cache.
     *
     * @param fieldIds comma separated fieldIds.  Must be cl fields.
     */
    public void addToShapeFileCache(String fieldIds) {
        if (preloadedShapeFiles != null) {
            fieldIds += "," + preloadedShapeFiles;
        }
        String[] fields = fieldIds.split(",");

        //requres readLayerInfo() first
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        String[] fid = new String[fields.length];

        int pos = 0;
        for (int i = 0; i < fields.length; i++) {
            try {
                layers[pos] = getIntersectionFile(fields[i].trim()).getFilePath();
                columns[pos] = getIntersectionFile(fields[i].trim()).getShapeFields();
                fid[pos] = fields[i];
                pos++;
            } catch (Exception e) {
                logger.error("problem adding shape file to cache for field: " + fields[i], e);
            }
        }
        if (pos < layers.length) {
            layers = java.util.Arrays.copyOf(layers, pos);
            columns = java.util.Arrays.copyOf(columns, pos);
            fid = java.util.Arrays.copyOf(fid, pos);
        }

        if (shapeFileCache == null) {
            shapeFileCache = new SimpleShapeFileCache(layers, columns, fid);
        } else {
            shapeFileCache.update(layers, columns, fid);
        }
    }

    public SimpleShapeFileCache getShapeFileCache() {
        return shapeFileCache;
    }

    static public int getGridBufferSize() {
        return gridBufferSize;
    }

    static public String getGridCachePath() {
        return gridCachePath;
    }

    static public int getGridCacheReaderCount() {
        return gridCacheReaderCount;
    }

    static private HashMap<Integer, GridClass> getGridClasses(String filePath, String type) throws IOException {
        HashMap<Integer, GridClass> classes = null;
        if (type.equals("Contextual")) {
            if(new File(filePath + ".gri").exists()
                && new File(filePath + ".grd").exists()
                && new File(filePath + ".txt").exists()) {
                File gridClassesFile = new File(filePath + ".classes.json");
                if (gridClassesFile.exists()) {
                    classes = mapper.readValue(gridClassesFile, new TypeReference<Map<Integer, GridClass>>() {
                    });
                    logger.info("found grid classes for " + gridClassesFile.getPath());
                } else {
                    logger.error("classes unavailable for " + gridClassesFile.getPath() + ", build classes offline");
    //                logger.info("building " + gridClassesFile.getPath());
    //                long start = System.currentTimeMillis();
    //                classes = GridClassBuilder.buildFromGrid(filePath);
    //                logger.info("finished building " + gridClassesFile.getPath() + " in " + (System.currentTimeMillis() - start) + " ms");
                }
            } else if(new File(filePath + ".gri").exists()
                    && new File(filePath + ".grd").exists()) {
                logger.error("missing grid classes for " + filePath);
            }
        } else {

        }
        return classes;
    }

    static public long getConfigReloadWait() {
        return configReloadWait;
    }

    public boolean requiresReload() {
        return lastReload + configReloadWait >= System.currentTimeMillis();
    }

    public boolean isLocalSampling() {
        return localSampling;
    }

    public List<Field> getFieldsByDB() {
        List<Field> fields = new ArrayList<Field>();
        if (layerIndexUrl != null) {
            try {
                //request from url
                fields = mapper.readValue(getUrl(layerIndexUrl + "/fieldsdb"), new TypeReference<List<Field>>() {
                });
            } catch (Exception ex) {
                logger.error("failed to read: " + layerIndexUrl + "/fieldsdb", ex);
            }
        }
        return fields;
    }

    static public String getGeoserverUrl() {
        return geoserverUrl;
    }

    static public String getGeonetworkUrl() {
        return geonetworkUrl;
    }

    static public String getAnalysisLayerFilesPath() {
        return analysisLayerFilesPath;
    }

    static public String getGdalPath() {
        return gdalPath;
    }

    static public List<Double> getAnalysisResolutions() {
        return analysisResolutions;
    }

    static private List<Double> getDoublesFrom(String property) {
        List<Double> l = new ArrayList<Double>();
        if (property != null) {
            for (String s : property.split(",")) {
                try {
                    Double d = Double.parseDouble(s.trim());
                    if (d != null && !d.isNaN()) {
                        l.add(d);
                    } else {
                        logger.warn("Cannot parse '" + s + "' to Double");
                    }
                } catch (Exception e) {
                    logger.warn("Cannot parse '" + s + "' to Double", e);
                }
            }
        }
        java.util.Collections.sort(l);
        return l;
    }

    static public String getOccurrenceSpeciesRecordsFilename() {
        return occurrenceSpeciesRecordsFilename;
    }

    static public String getUploadedShapesFieldId() {
        return uploadedShapesFieldId;
    }

    static public String getApiKeyCheckUrlTemplate() {
        return apiKeyCheckUrlTemplate;
    }

    static public String getSpatialPortalAppName() {
        return spatialPortalAppName;
    }

    public Map<String, IntersectionFile> getIntersectionFiles() {
        return intersectionFiles;
    }

    /**
     * get info on an analysis layer
     *
     * @param id layer id as String
     * @return String [] with [0] = analysis id, [1] = path to grid file, [2] = analysis type
     */
    public String[] getAnalysisLayerInfo(String id) {
        String gid, filename, name;
        gid = filename = name = null;
        if (id.startsWith("species_")) {
            //maxent layer
            gid = id.substring("species_".length());
            filename = getAlaspatialOutputPath() + File.separator + "maxent" + File.separator + gid + File.separator + gid;
            name = "Prediction";
        } else if (id.startsWith("aloc_")) {
            //aloc layer
            gid = id.substring("aloc_".length());
            filename = getAlaspatialOutputPath() + File.separator + "aloc" + File.separator + gid + File.separator + "aloc";
            name = "Classification";
        } else if (id.startsWith("odensity_")) {
            //occurrence density layer
            gid = id.substring("odensity_".length());
            filename = getAlaspatialOutputPath() + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "occurrence_density";
            name = "Occurrence Density";
        } else if (id.startsWith("srichness_")) {
            //species richness layer
            gid = id.substring("srichness_".length());
            filename = getAlaspatialOutputPath() + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "species_richness";
            name = "Species Richness";
        } else if (id.endsWith("_odensity")) {
            //occurrence density layer
            gid = id.substring(0, id.length() - "_odensity".length());
            filename = getAlaspatialOutputPath() + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "occurrence_density";
            name = "Occurrence Density";
        } else if (id.endsWith("_srichness")) {
            //species richness layer
            gid = id.substring(0, id.length() - "_srichness".length());
            filename = getAlaspatialOutputPath() + File.separator + "sitesbyspecies" + File.separator + gid + File.separator + "species_richness";
            name = "Species Richness";
        } else if (id.startsWith("envelope_")) {
            //envelope layer
            gid = id.substring("envelope_".length());
            filename = getAlaspatialOutputPath() + File.separator + "envelope" + File.separator + gid + File.separator + "envelope";
            name = "Environmental Envelope";
        } else if (id.startsWith("gdm_")) {
            //gdm layer
            int pos1 = id.indexOf("_");
            int pos2 = id.lastIndexOf("_");
            String[] gdmparts = new String[]{id.substring(0, pos1), id.substring(pos1 + 1, pos2), id.substring(pos2 + 1)};
            gid = gdmparts[2];
            filename = getAlaspatialOutputPath() + File.separator + "gdm" + File.separator + gid + File.separator + gdmparts[1];
            //Layer tmpLayer = layerDao.getLayerByName(gdmparts[1].replaceAll("Tran", ""));
            //name = "Transformed " + tmpLayer.getDisplayname();
            name = "Transformed " + getIntersectionFile(gdmparts[1].replaceAll("Tran", "")).getFieldName();
        } else if (id.contains("_")) {
            //2nd form of gdm layer name, why?
            int pos = id.indexOf("_");
            String[] gdmparts = new String[]{id.substring(0, pos), id.substring(pos + 1)};
            gid = gdmparts[0];
            filename = getAlaspatialOutputPath() + File.separator + "gdm" + File.separator + gid + File.separator + gdmparts[1] + "Tran";
            logger.debug("id: " + id);
            logger.debug("parts: " + gdmparts[0] + ", " + gdmparts[1]);
            logger.debug("filename: " + filename);
            //Layer tmpLayer = layerDao.getLayerByName(gdmparts[1].replaceAll("Tran", ""));
            //name = "Transformed " + tmpLayer.getDisplayname();
            name = "Transformed " + getIntersectionFile(gdmparts[1]).getFieldName();
        }

        if (gid != null) {
            return new String[]{gid, filename, name};
        } else {
            return null;
        }
    }
}
