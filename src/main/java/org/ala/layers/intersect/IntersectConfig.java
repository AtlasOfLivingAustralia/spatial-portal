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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Properties;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.IntersectionFile;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 *
 * @author Adam
 */
public class IntersectConfig {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(IntersectConfig.class);
    static final String ALASPATIAL_OUTPUT_PATH = "ALASPATIAL_OUTPUT_PATH";
    static final String LAYER_FILES_PATH = "LAYER_FILES_PATH";
    static final String LAYER_INDEX_URL = "LAYER_INDEX_URL";
    static final String BATCH_THREAD_COUNT = "BATCH_THREAD_COUNT";
    static final String CONFIG_RELOAD_WAIT = "CONFIG_RELOAD_WAIT";
    static final String PRELOADED_SHAPE_FILES = "PRELOADED_SHAPE_FILES";
    static final String LAYER_PROPERTIES = "layer.properties";
    private FieldDAO fieldDao;
    private LayerDAO layerDao;
    String layerFilesPath;
    String alaspatialOutputPath;
    String layerIndexUrl;
    int batchThreadCount;
    long configReloadWait;
    long lastReload;
    String preloadedShapeFiles;
    SimpleShapeFileCache shapeFileCache;
    HashMap<String, IntersectionFile> intersectionFiles;

    public IntersectConfig(FieldDAO fieldDao, LayerDAO layerDao) {
        this.fieldDao = fieldDao;
        this.layerDao = layerDao;

        load();
    }

    public void load() {
        if (lastReload + configReloadWait >= System.currentTimeMillis()) {
            return;
        }
        lastReload = System.currentTimeMillis();

        String lfp = System.getProperty(LAYER_FILES_PATH);
        String aop = System.getProperty(ALASPATIAL_OUTPUT_PATH);
        String liu = System.getProperty(LAYER_INDEX_URL);
        String btc = System.getProperty(BATCH_THREAD_COUNT);
        String crw = System.getProperty(CONFIG_RELOAD_WAIT);
        String psf = System.getProperty(PRELOADED_SHAPE_FILES);

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

        if (lfp == null) {
            lfp = properties.getProperty(LAYER_FILES_PATH);
        }
        if (aop == null) {
            aop = properties.getProperty(ALASPATIAL_OUTPUT_PATH);
        }
        if (liu == null) {
            liu = properties.getProperty(LAYER_INDEX_URL);
        }
        if (btc == null) {
            btc = properties.getProperty(BATCH_THREAD_COUNT);
        }
        if (crw == null) {
            crw = properties.getProperty(CONFIG_RELOAD_WAIT);
        }
        if (psf == null) {
            psf = properties.getProperty(PRELOADED_SHAPE_FILES);
        }

        layerFilesPath = lfp;
        alaspatialOutputPath = aop;
        layerIndexUrl = liu;
        int ibtc = 1;
        try {
            ibtc = Integer.parseInt(btc);
        } catch (NumberFormatException ex) {
            logger.error("parsing BATCH_THREAD_COUNT: " + btc, ex);
        }
        if (ibtc <= 0) {
            ibtc = 1;
        }
        batchThreadCount = ibtc;
        long lcrw = 3600000;
        try {
            lcrw = Long.parseLong(crw);
        } catch (NumberFormatException ex) {
            logger.error("parsing CONFIG_RELOAD_WAIT: " + crw, ex);
        }
        if (lcrw <= 0) {
            lcrw = 3600000;
        }
        configReloadWait = lcrw;
        preloadedShapeFiles = psf;

        try {
            updateIntersectionFiles();
            updateShapeFileCache();
        } catch (Exception e) {
            //if it fails, set reload wait low
            logger.error("load failed, retry in 30s", e);
            configReloadWait = 30000;
        }

        logger.info(LAYER_FILES_PATH + " > " + layerFilesPath);
        logger.info(ALASPATIAL_OUTPUT_PATH + " > " + alaspatialOutputPath);
        logger.info(LAYER_INDEX_URL + " > " + layerIndexUrl);
        logger.info(BATCH_THREAD_COUNT + " > " + batchThreadCount);
        logger.info(CONFIG_RELOAD_WAIT + " > " + configReloadWait);
        logger.info(PRELOADED_SHAPE_FILES + " > " + preloadedShapeFiles);
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

    private void updateIntersectionFiles() throws MalformedURLException, IOException {
        if (intersectionFiles == null) {
            intersectionFiles = new HashMap<String, IntersectionFile>();
        }

        if (layerIndexUrl != null) {
            //request from url
            JSONArray layers = JSONArray.fromObject(getUrl(layerIndexUrl + "/layers"));
            HashMap<String, String> layerPathOrig = new HashMap<String, String>();
            for (int i = 0; i < layers.size(); i++) {
                layerPathOrig.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("path_orig"));
            }

            JSONArray fields = JSONArray.fromObject(getUrl(layerIndexUrl + "/fieldsdb"));
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jo = fields.getJSONObject(i);
                intersectionFiles.put(jo.getString("id"),
                        new IntersectionFile(jo.getString("id"),
                        layerFilesPath + layerPathOrig.get(jo.getString("spid")),
                        (jo.containsKey("sname") ? jo.getString("sname") : null)));
            }
        } else {
            for (Field f : fieldDao.getFields()) {
                if(f.isIndb()) {
                    intersectionFiles.put(f.getId(),
                            new IntersectionFile(f.getId(),
                            getLayerFilesPath() + layerDao.getLayerById(Integer.parseInt(f.getSpid())).getPath_orig(),
                            f.getSname()));
                }
            }
        }
    }

    String getUrl(String url) {
        try {
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
        if (fields.length == 1 && fields[0].equalsIgnoreCase("all")) {
            int countCL = 0;
            for (String s : intersectionFiles.keySet()) {
                if (s.startsWith("cl")) {
                    countCL++;
                }
            }
            layers = new String[countCL];
            columns = new String[countCL];
            int i = 0;
            for (IntersectionFile f : intersectionFiles.values()) {
                if (f.getName().startsWith("cl")) {
                    layers[i] = f.getFilePath();
                    columns[i] = f.getShapeFields();
                    i++;
                }
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                layers[i] = getIntersectionFile(fields[i].trim()).getFilePath();
                columns[i] = getIntersectionFile(fields[i].trim()).getShapeFields();
            }
        }

        if (shapeFileCache == null) {
            shapeFileCache = new SimpleShapeFileCache(layers, columns);
        } else {
            shapeFileCache.update(layers, columns);
        }
    }

    public SimpleShapeFileCache getShapeFileCache() {
        return shapeFileCache;
    }
}
