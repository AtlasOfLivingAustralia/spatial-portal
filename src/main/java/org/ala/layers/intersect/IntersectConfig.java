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
 *
 * @author Adam
 */
public class IntersectConfig {

    /** log4j logger */
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
    static final String GDAL_PATH = "GDAL_PATH";
    static final String ANALYSIS_RESOLUTIONS = "ANALYSIS_RESOLUTIONS";
    static final String OCCURRENCE_SPECIES_RECORDS_FILENAME = "OCCURRENCE_SPECIES_RECORDS_FILENAME";
    static final String LAYER_PROPERTIES = "layer.properties";
    static ObjectMapper mapper = new ObjectMapper();
    private FieldDAO fieldDao;
    private LayerDAO layerDao;
    String layerFilesPath;
    String analysisLayerFilesPath;
    String alaspatialOutputPath;
    String layerIndexUrl;
    int batchThreadCount;
    long configReloadWait;
    long lastReload;
    String preloadedShapeFiles;
    int gridBufferSize;
    SimpleShapeFileCache shapeFileCache;
    HashMap<String, IntersectionFile> intersectionFiles;
    String gridCachePath;
    int gridCacheReaderCount;
    HashMap<String, HashMap<Integer, GridClass>> classGrids;
    boolean localSampling;
    String geoserverUrl;
    String gdalPath;
    List<Double> analysisResolutions;
    String occurrenceSpeciesRecordsFilename;

    public IntersectConfig(FieldDAO fieldDao, LayerDAO layerDao) {
        this.fieldDao = fieldDao;
        this.layerDao = layerDao;

        load();
    }

    public void load() {
        lastReload = System.currentTimeMillis();

        Properties properties = new Properties();
        try {
            InputStream is = IntersectConfig.class.getResourceAsStream("/" + LAYER_PROPERTIES);
            if (is != null) {
                properties.load(is);
            } else {
                String msg = "cannot get properties file: " + IntersectConfig.class.getResource(LAYER_PROPERTIES).getFile();
                logger.warn(msg);
            }
        } catch (IOException ex) {
            logger.error(null, ex);
        }

        layerFilesPath = getProperty(LAYER_FILES_PATH, properties, null);
        analysisLayerFilesPath = getProperty(ANALYSIS_LAYER_FILES_PATH, properties, null);
        alaspatialOutputPath = getProperty(ALASPATIAL_OUTPUT_PATH, properties, null);
        layerIndexUrl = getProperty(LAYER_INDEX_URL, properties, null);
        batchThreadCount = (int) getPositiveLongProperty(BATCH_THREAD_COUNT, properties, 1);
        configReloadWait = getPositiveLongProperty(CONFIG_RELOAD_WAIT, properties, 3600000);
        preloadedShapeFiles = getProperty(PRELOADED_SHAPE_FILES, properties, null);
        gridBufferSize = (int) getPositiveLongProperty(GRID_BUFFER_SIZE, properties, 4096);
        gridCachePath = getProperty(GRID_CACHE_PATH, properties, null);
        gridCacheReaderCount = (int) getPositiveLongProperty(GRID_CACHE_READER_COUNT, properties, 10);
        localSampling = getProperty(LOCAL_SAMPLING, properties, "true").toLowerCase().equals("true");
        geoserverUrl = getProperty(GEOSERVER_URL, properties, null);
        gdalPath = getProperty(GDAL_PATH, properties, null);
        analysisResolutions = getDoublesFrom(getProperty(ANALYSIS_RESOLUTIONS, properties, "0.5"));
        occurrenceSpeciesRecordsFilename = getProperty(OCCURRENCE_SPECIES_RECORDS_FILENAME, properties, null);

        try {
            updateIntersectionFiles();
            updateShapeFileCache();
        } catch (Exception e) {
            //if it fails, set reload wait low
            logger.error("load failed, retry in 30s", e);
            configReloadWait = 30000;
        }
    }

    String getProperty(String property, Properties properties, String defaultValue) {
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

    long getPositiveLongProperty(String property, Properties properties, long defaultValue) {
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

    public String getAlaspatialOutputPath() {
        return alaspatialOutputPath;
    }

    public String getLayerFilesPath() {
        return layerFilesPath;
    }

    public String getLayerIndexUrl() {
        return layerIndexUrl;
    }

    public int getThreadCount() {
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
                layerType.put(layers.getJSONObject(i).getString("id"),
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

    void updateShapeFileCache() {
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

    public int getGridBufferSize() {
        return gridBufferSize;
    }

    public String getGridCachePath() {
        return gridCachePath;
    }

    public int getGridCacheReaderCount() {
        return gridCacheReaderCount;
    }

    static private HashMap<Integer, GridClass> getGridClasses(String filePath, String type) throws IOException {
        HashMap<Integer, GridClass> classes = null;
        if (type.equals("Contextual")
                && new File(filePath + ".gri").exists()
                && new File(filePath + ".grd").exists()
                && new File(filePath + ".txt").exists()) {
            File gridClassesFile = new File(filePath + ".classes.json");
            if (gridClassesFile.exists()) {
                classes = mapper.readValue(gridClassesFile, new TypeReference<Map<Integer, GridClass>>() {
                });
                logger.info("found grid classes for " + gridClassesFile.getPath());
            } else {
                logger.info("building " + gridClassesFile.getPath());
                long start = System.currentTimeMillis();
                classes = GridClassBuilder.buildFromGrid(filePath);
                logger.info("finished building " + gridClassesFile.getPath() + " in " + (System.currentTimeMillis() - start) + " ms");
            }
        } else {
            logger.info("no grid classes for " + filePath);
        }
        return classes;
    }

    public long getConfigReloadWait() {
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

    public String getGeoserverUrl() {
        return geoserverUrl;
    }

    public String getAnalysisLayerFilesPath() {
        return analysisLayerFilesPath;
    }

    public String getGdalPath() {
        return gdalPath;
    }

    public List<Double> getAnalysisResolutions() {
        return analysisResolutions;
    }

    private List<Double> getDoublesFrom(String property) {
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

    public String getOccurrenceSpeciesRecordsFilename() {
        return occurrenceSpeciesRecordsFilename;
    }
}
