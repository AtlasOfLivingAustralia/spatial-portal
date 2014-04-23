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
package org.ala.layers.dao;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import javax.annotation.Resource;

import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.grid.GridCacheReader;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.intersect.SamplingThread;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Implementation of the sampling.
 *
 * @author adam
 */
@Service("layerIntersectDao")
public class LayerIntersectDAOImpl implements LayerIntersectDAO {

    /**
     * log4j logger
     */
    private static final Logger logger = Logger.getLogger(LayerIntersectDAOImpl.class);
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;
    @Resource(name = "layerDao")
    private LayerDAO layerDao;

    @Autowired
    private ApplicationContext appcontext;

    IntersectConfig intersectConfig;
    LinkedBlockingQueue<GridCacheReader> gridReaders = null;
    int gridGroupCount = 0;
    Object initLock = new Object();

    @Override
    public String reload() {
        String error = null;
        try {
            if (intersectConfig == null) {
                init();
            }
            synchronized (initLock) {
                int oldGridCacheReaderCount = intersectConfig.getGridCacheReaderCount();

                intersectConfig = new IntersectConfig(fieldDao, layerDao);

                ArrayList<GridCacheReader> newGridReaders = new ArrayList<GridCacheReader>();
                for (int i = 0; i < intersectConfig.getGridCacheReaderCount(); i++) {
                    GridCacheReader gcr = fixGridCacheReaderNames(new GridCacheReader(intersectConfig.getGridCachePath()));
                    newGridReaders.add(gcr);
                    gridGroupCount = gcr.getGroupCount();
                }
                if (newGridReaders.isEmpty()) {
                    newGridReaders = null;
                }

                //remove old grid readers
                for (int i = 0; i < oldGridCacheReaderCount; i++) {
                    gridReaders.take();
                }

                //add new gridReaders
                gridReaders.addAll(newGridReaders);

                return null;
            }
        } catch (Exception e) {
            logger.error("error reloading properties and table images", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            error = "error reloading properties and table images\n" + sw.getBuffer().toString();
        }
        return error;
    }

    void init() {
        if (intersectConfig == null) {
            synchronized (initLock) {
                if (intersectConfig != null) {
                    return;
                }
                intersectConfig = new IntersectConfig(fieldDao, layerDao);
                gridReaders = new LinkedBlockingQueue<GridCacheReader>();
                for (int i = 0; i < intersectConfig.getGridCacheReaderCount(); i++) {
                    GridCacheReader gcr = fixGridCacheReaderNames(new GridCacheReader(intersectConfig.getGridCachePath()));
                    try {
                        gridReaders.put(gcr);
                    } catch (InterruptedException ex) {
                        logger.error("failed to add a GridCacheReader");
                    }
                    gridGroupCount = gcr.getGroupCount();
                }
                if (gridReaders.size() == 0) {
                    gridReaders = null;
                }
            }
        }
    }

    @Override
    public Vector samplingFull(String fieldIds, double longitude, double latitude) {
        init();

        Vector out = new Vector();

        for (String id : fieldIds.split(",")) {
            Layer layer = null;
            int newid = cleanObjectId(id);

            IntersectionFile f = intersectConfig.getIntersectionFile(id);
            if (f != null) {
                layer = layerDao.getLayerByName(f.getLayerName());
            } else {
                if (newid != -1) {
                    layer = layerDao.getLayerById(newid);
                }
                if (layer == null) {
                    layer = layerDao.getLayerByName(id);
                }
            }

            double[][] p = {{longitude, latitude}};

            if (layer != null) {
                if (layer.isShape() && (f != null && f.getClasses() == null)) {
                    ObjectDAO objectDao = (ObjectDAO) appcontext.getBean("objectDao");
                    Objects o = objectDao.getObjectByIdAndLocation("cl" + layer.getId(), longitude, latitude);
                    if (o != null) {
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("value", o.getName());
                        m.put("layername", o.getFieldname());   //close enough
                        m.put("pid", o.getPid());
                        m.put("description", o.getDescription());
                        //m.put("fid", o.getFid());

                        out.add(m);
                    } else {
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("value", "n/a");
                        m.put("layername", layer.getDisplayname());   //close enough

                        out.add(m);
                    }
                } else if (layer.isGrid() || (f != null && f.getClasses() != null)) {
                    Grid g = new Grid(getConfig().getLayerFilesPath() + layer.getPath_orig());
                    if (g != null) {
                        float[] v = g.getValues(p);
                        //s = "{\"value\":" + v[0] + ",\"layername\":\"" + layer.getDisplayname() + "\"}";
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("layername", layer.getDisplayname());   //close enough

                        if (f != null && f.getClasses() != null) {
                            GridClass gc = f.getClasses().get((int) v[0]);
                            m.put("value", (gc == null ? "n/a" : gc.getName()));
                            if (gc != null) {
                                //TODO: re-enable intersection for type 'a' after correct implementation
                                //TODO: of 'defaultField' fields table column
                                g = new Grid(f.getFilePath() + File.separator + "polygons");
                                if (g != null) {
                                    int v0 = (int) v[0];
                                    v = g.getValues(p);
                                    m.put("pid", f.getLayerPid() + ":" + v0 + ":" + ((int) v[0]));
                                }
                            }
                        }
                        if (!m.containsKey("value")) {
                            m.put("value", (Float.isNaN(v[0]) ? "n/a" : v[0]));
                            m.put("units", layer.getEnvironmentalvalueunits());
                        }
                        out.add(m);
                    } else {
                        logger.error("Cannot find grid file: " + getConfig().getLayerFilesPath() + layer.getPath_orig());
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("value", "n/a");
                        m.put("layername", layer.getDisplayname());   //close enough

                        out.add(m);
                    }
                }
            } else {
                String[] info = getConfig().getAnalysisLayerInfo(id);

                if (info != null) {
                    String gid = info[0];
                    String filename = info[1];
                    String name = info[2];
                    Grid grid = new Grid(filename);

                    if (grid != null && (new File(filename + ".grd").exists())) {
                        float[] v = grid.getValues(p);
                        if (v != null) {
                            Map m = new HashMap();
                            m.put("field", id);
                            m.put("layername", name + "(" + gid + ")");
                            if (Float.isNaN(v[0])) {
                                m.put("value", "n/a");
                            } else {
                                m.put("value", (Float.isNaN(v[0]) ? "n/a" : v[0]));
                            }

                            out.add(m);
                        }
                    }
                }
            }
        }

        return out;
    }

    /**
     * Single coordinate sampling.
     *
     * @param fieldIds  comma separated field ids.
     * @param longitude
     * @param latitude
     * @return the intersection value for each input field id as a \n separated
     * String.
     */
    @Override
    public String sampling(String fieldIds, double longitude, double latitude) {
        init();

        double[][] p = {{longitude, latitude}};
        String[] fields = fieldIds.split(",");

        //count el fields
        int elCount = 0;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].length() > 0 && fields[i].charAt(0) == 'e') {
                elCount++;
            }
        }

        StringBuilder sb = new StringBuilder();
        HashMap<String, Float> gridValues = null;
        for (String fid : fields) {
            IntersectionFile f = getConfig().getIntersectionFile(fid);

            if (sb.length() > 0) {
                sb.append("\n");
            }

            if (f != null) {
                if (f.getShapeFields() != null && getConfig().getShapeFileCache() != null) {
                    SimpleShapeFile ssf = getConfig().getShapeFileCache().get(f.getFilePath());
                    if (ssf != null) {
                        String s = ssf.intersect(longitude, latitude);
                        if (s != null) {
                            sb.append(s);
                        }
                    } else {
                        ObjectDAO objectDao = (ObjectDAO) appcontext.getBean("objectDao");
                        Objects o = objectDao.getObjectByIdAndLocation(f.getFieldId(), longitude, latitude);
                        if (o != null) {
                            sb.append(o.getName());
                        }
                    }
                } else {
                    if (gridValues == null && gridReaders != null && elCount > gridGroupCount) {
                        try {
                            GridCacheReader gcr = gridReaders.take();
                            gridValues = gcr.sample(longitude, latitude);
                            gridReaders.put(gcr);
                        } catch (Exception e) {
                            logger.error("GridCacheReader failed.", e);
                        }
                    }

                    if (gridValues != null) {
                        Float v = gridValues.get(fid);
                        if (v == null && !gridValues.containsKey(fid)) {
                            Grid g = new Grid(f.getFilePath());
                            if (g != null) {
                                float fv = g.getValues(p)[0];
                                if (f.getClasses() != null) {
                                    GridClass gc = f.getClasses().get((int) fv);
                                    if (gc != null) {
                                        sb.append(gc.getName());
                                    }
                                } else {
                                    if (!Float.isNaN(fv)) {
                                        sb.append(String.valueOf(fv));
                                    }
                                }
                            }
                        } else {
                            if (f.getClasses() != null) {
                                GridClass gc = f.getClasses().get(v.intValue());
                                if (gc != null) {
                                    sb.append(gc.getName());
                                }
                            } else {
                                if (v != null && !v.isNaN()) {
                                    sb.append(String.valueOf(v));
                                }
                            }
                        }
                    } else {
                        Grid g = new Grid(f.getFilePath());
                        if (g != null) {
                            float fv = g.getValues(p)[0];
                            if (f.getClasses() != null) {
                                GridClass gc = f.getClasses().get((int) fv);
                                if (gc != null) {
                                    sb.append(gc.getName());
                                }
                            } else {
                                if (!Float.isNaN(fv)) {
                                    sb.append(String.valueOf(fv));
                                }
                            }
                        }
                    }
                }
            } else {
                String[] info = getConfig().getAnalysisLayerInfo(fid);

                if (info != null) {
                    String filename = info[1];
                    Grid grid = new Grid(filename);

                    if (grid != null && (new File(filename + ".grd").exists())) {
                        sb.append(String.valueOf(grid.getValues(p)[0]));
                    }
                }
            }
        }

        return sb.toString();
    }

    @Override
    public HashMap<String, String> sampling(double longitude, double latitude) {
        init();

        HashMap<String, String> output = new HashMap<String, String>();

        if (getConfig().getShapeFileCache() != null) {
            HashMap<String, SimpleShapeFile> ssfs = getConfig().getShapeFileCache().getAll();
            for (Entry<String, SimpleShapeFile> entry : ssfs.entrySet()) {
                String s = entry.getValue().intersect(longitude, latitude);
                if (s == null) {
                    s = "n/a";
                }
                output.put(entry.getKey(), s);
            }
        }

        if (gridReaders != null) {
            GridCacheReader gcr = null;
            HashMap<String, Float> gridValues = null;
            try {
                gcr = gridReaders.take();
                gridValues = gcr.sample(longitude, latitude);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (gcr != null) {
                    try {
                        gridReaders.put(gcr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (gridValues != null) {
                for (Entry<String, Float> entry : gridValues.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isNaN()) {
                        output.put(entry.getKey(), "n/a");
                    } else {
                        output.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
        }

        return output;
    }

    @Override
    public ArrayList<String> sampling(String fieldIds, String pointsString) {
        init();

        //parse points
        String[] pointsArray = pointsString.split(",");
        double[][] points = new double[pointsArray.length / 2][2];
        for (int i = 0; i < pointsArray.length; i += 2) {
            try {
                points[i / 2][1] = Double.parseDouble(pointsArray[i]);
                points[i / 2][0] = Double.parseDouble(pointsArray[i + 1]);
            } catch (Exception e) {
                points[i / 2][1] = Double.NaN;
                points[i / 2][0] = Double.NaN;
            }
        }

        //parse fids
        String[] fidsArray = fieldIds.split(",");

        return sampling(fidsArray, points);
    }

    @Override
    public ArrayList<String> sampling(String[] fieldIds, double[][] points, IntersectCallback callback) {
        init();
        IntersectionFile[] intersectionFiles = new IntersectionFile[fieldIds.length];
        for (int i = 0; i < fieldIds.length; i++) {
            intersectionFiles[i] = intersectConfig.getIntersectionFile(fieldIds[i]);
            if (intersectionFiles[i] == null) {
                logger.warn("failed to find layer for id '" + fieldIds[i] + "'");
            }
        }
        if (callback == null)
            callback = new DummyCallback();
        return sampling(intersectionFiles, points, callback);
    }

    @Override
    public ArrayList<String> sampling(String[] fieldIds, double[][] points) {
        return sampling(fieldIds, points, new DummyCallback());
    }

    @Override
    public ArrayList<String> sampling(IntersectionFile[] intersectionFiles, double[][] points) {
        return sampling(intersectionFiles, points, new DummyCallback());
    }

    ArrayList<String> sampling(IntersectionFile[] intersectionFiles, double[][] points, IntersectCallback callback) {
        init();
        if (callback == null)
            callback = new DummyCallback();
        if (intersectConfig.isLocalSampling()) {
            return localSampling(intersectionFiles, points, callback);
        } else {
            return remoteSampling(intersectionFiles, points, callback);
        }
    }

    @Override
    public IntersectConfig getConfig() {
        init();
        return intersectConfig;
    }

    ArrayList<String> localSampling(IntersectionFile[] intersectionFiles, double[][] points, IntersectCallback callback) {
        logger.info("begin LOCAL sampling, number of threads " + intersectConfig.getThreadCount()
                + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);
        long start = System.currentTimeMillis();
        int threadCount = intersectConfig.getThreadCount();
        SamplingThread[] threads = new SamplingThread[threadCount];
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue();
        CountDownLatch cdl = new CountDownLatch(intersectionFiles.length);
        ArrayList<String> output = new ArrayList<String>();
        for (int i = 0; i < intersectionFiles.length; i++) {
            output.add("n/a");
            lbq.add(i);
        }

        callback.setLayersToSample(intersectionFiles);
        logger.info("Initialising sampling threads: " + threadCount);
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new SamplingThread(lbq,
                    cdl,
                    intersectionFiles,
                    points,
                    output,
                    intersectConfig.getThreadCount(),
                    intersectConfig.getShapeFileCache(),
                    intersectConfig.getGridBufferSize(),
                    callback
            );
            threads[i].start();
        }

        try {
            cdl.await();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].interrupt();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        logger.info("End sampling, threads=" + threadCount
                + " layers=" + intersectionFiles.length
                + " in " + (System.currentTimeMillis() - start) + "ms");
        return output;
    }

    ArrayList<String> remoteSampling(IntersectionFile[] intersectionFiles, double[][] points, IntersectCallback callback) {
        logger.info("begin REMOTE sampling, number of threads " + intersectConfig.getThreadCount()
                + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);

        ArrayList<String> output = null;

        try {
            long start = System.currentTimeMillis();
            URL url = new URL(intersectConfig.getLayerIndexUrl() + "/intersect/batch");
            URLConnection c = url.openConnection();
            c.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(c.getOutputStream());
            out.write("fids=");
            for (int i = 0; i < intersectionFiles.length; i++) {
                if (i > 0) {
                    out.write(",");
                }
                out.write(intersectionFiles[i].getFieldId());
            }
            out.write("&points=");
            for (int i = 0; i < points.length; i++) {
                if (i > 0) {
                    out.write(",");
                }
                out.write(String.valueOf(points[i][0]));
                out.write(",");
                out.write(String.valueOf(points[i][1]));
            }
            out.close();

            CSVReader csv = new CSVReader(new InputStreamReader(new GZIPInputStream(c.getInputStream())));

            long mid = System.currentTimeMillis();

            ArrayList<StringBuilder> tmpOutput = new ArrayList<StringBuilder>();
            for (int i = 0; i < intersectionFiles.length; i++) {
                tmpOutput.add(new StringBuilder());
            }
            String[] line;
            int row = 0;
            csv.readNext(); //discard header
            while ((line = csv.readNext()) != null) {
                //order is consistent with request
                for (int i = 2; i < line.length && i - 2 < tmpOutput.size(); i++) {
                    if (row > 0) {
                        tmpOutput.get(i - 2).append("\n");
                    }
                    tmpOutput.get(i - 2).append(line[i]);
                }
                row++;
            }
            csv.close();

            output = new ArrayList<String>();
            for (int i = 0; i < tmpOutput.size(); i++) {
                output.add(tmpOutput.get(i).toString());
                tmpOutput.set(i, null);
            }

            long end = System.currentTimeMillis();

            logger.info("sample time for " + 5 + " layers and " + 3 + " coordinates: get response="
                    + (mid - start) + "ms, write response=" + (end - mid) + "ms");

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return output;
    }

    /**
     * Clean up and just return an int for LAYER object
     *
     * @param id
     * @return
     */
    private int cleanObjectId(String id) {
        //test field id value
        int len = Math.min(6, id.length());
        id = id.substring(0, len);
        char prefix = id.toUpperCase().charAt(0);
        String number = id.substring(2, len);
        try {
            int i = Integer.parseInt(number);
            return i;
        } catch (Exception e) {
        }

        return -1;
    }

    /**
     * update a grid cache reader with fieldIds
     */
    GridCacheReader fixGridCacheReaderNames(GridCacheReader gcr) {
        ArrayList<String> fileNames = gcr.getFileNames();
        for (int i = 0; i < fileNames.size(); i++) {
            gcr.updateNames(fileNames.get(i), intersectConfig.getFieldIdFromFile(fileNames.get(i)));
        }

        return gcr;
    }

    /**
     * A dummy callback for convenience.
     */
    class DummyCallback implements IntersectCallback {
        public void setLayersToSample(IntersectionFile[] layersToSample) {
        }

        public void setCurrentLayer(IntersectionFile layer) {
        }

        public void setCurrentLayerIdx(Integer layer) {
        }

        public void progressMessage(String message) {
        }
    }
}

