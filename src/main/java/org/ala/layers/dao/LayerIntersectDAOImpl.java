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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import javax.annotation.Resource;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Layer;
import org.ala.layers.dto.Objects;
import org.ala.layers.grid.GridCacheReader;
import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.intersect.SamplingThread;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 *
 * @author adam
 */
@Service("layerIntersectDao")
public class LayerIntersectDAOImpl implements LayerIntersectDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(LayerIntersectDAOImpl.class);
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;
    @Resource(name = "layerDao")
    private LayerDAO layerDao;
    @Resource(name = "objectDao")
    private ObjectDAO objectDao;
    IntersectConfig intersectConfig;
    LinkedBlockingQueue<GridCacheReader> gridReaders = null;
    int gridGroupCount = 0;
    Object initLock = new Object();

    void init() {
        if (intersectConfig == null) {
            synchronized(initLock) {
                if(intersectConfig != null) {
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
                if(gridReaders.size() == 0) {
                    gridReaders = null;
                }
            }
        } else {
            intersectConfig.load();
        }
    }

    @Override
    public Vector samplingFull(String fieldIds, double longitude, double latitude) {
        init();

        Vector out = new Vector();

        for (String id : fieldIds.split(",")) {

            Layer layer = null;
            int newid = cleanObjectId(id);

            if (newid != -1) {
                layer = layerDao.getLayerById(newid);
            }
            if (layer == null) {
                layer = layerDao.getLayerByName(id);
            }

            double[][] p = {{longitude, latitude}};

            if (layer != null) {
                if (layer.isShape()) {
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
                        m.put("value", "");
                        m.put("layername", layer.getDisplayname());   //close enough

                        out.add(m);
                    }
                } else if (layer.isGrid()) {
                    Grid g = new Grid(getConfig().getLayerFilesPath() + layer.getPath_orig());
                    if (g != null) {
                        float[] v = g.getValues(p);
                        //s = "{\"value\":" + v[0] + ",\"layername\":\"" + layer.getDisplayname() + "\"}";
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("value", (Float.isNaN(v[0]) ? "" : v[0]));
                        m.put("layername", layer.getDisplayname());

                        out.add(m);
                    } else {
                        logger.error("Cannot find grid file: " + getConfig().getLayerFilesPath() + layer.getPath_orig());
                        Map m = new HashMap();
                        m.put("field", id);
                        m.put("value", "");
                        m.put("layername", layer.getDisplayname());   //close enough

                        out.add(m);
                    }
                }
            } else {
                String gid = null;
                String filename = null;
                String name = null;

                if (id.startsWith("species_")) {
                    //maxent layer
                    gid = id.substring(8);
                    filename = getConfig().getAlaspatialOutputPath() + File.separator + "maxent" + File.separator + gid + File.separator + gid;
                    name = "Prediction";
                } else if (id.startsWith("aloc_")) {
                    //aloc layer
                    gid = id.substring(8);
                    filename = getConfig().getAlaspatialOutputPath() + File.separator + "aloc" + File.separator + gid + File.separator + gid;
                    name = "Classification";
                }

                if (filename != null) {
                    Grid grid = new Grid(filename);

                    if (grid != null && (new File(filename + ".grd").exists())) {
                        float[] v = grid.getValues(p);
                        if (v != null) {
                            Map m = new HashMap();
                            if (Float.isNaN(v[0])) {
                                //s = "{\"value\":\"no data\",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("field", id);
                                m.put("value", "");
                                m.put("layername", name + "(" + gid + ")");
                            } else {
                                //s = "{\"value\":" + v[0] + ",\"layername\":\"" + name + " (" + gid + ")\"}";
                                m.put("value", (Float.isNaN(v[0]) ? "" : v[0]));
                                m.put("layername", name + "(" + gid + ")");
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
     *
     * @param fieldIds comma separated field ids.
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
                if (f.getFieldId().startsWith("cl")) {
                    SimpleShapeFile ssf = getConfig().getShapeFileCache().get(f.getFilePath());
                    if (ssf != null) {
                        String s = ssf.intersect(longitude, latitude);
                        if(s != null) {
                            sb.append(s);
                        }
                    } else {
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
                                if(!Float.isNaN(fv)) {
                                    sb.append(String.valueOf(fv));
                                }
                            }
                        } else {
                            if (v != null && !v.isNaN()) {
                                sb.append(String.valueOf(v));
                            }
                        }
                    } else {
                        Grid g = new Grid(f.getFilePath());
                        if (g != null) {
                            float fv = g.getValues(p)[0];
                            if(!Float.isNaN(fv)) {
                                sb.append(String.valueOf(fv));
                            }
                        }
                    }
                }
            } else {
                String gid = null;
                String filename = null;

                if (fid.startsWith("species_")) {
                    //maxent layer
                    gid = fid.substring(8);
                    filename = getConfig().getAlaspatialOutputPath() + File.separator + "maxent" + File.separator + gid + File.separator + gid;
                } else if (fid.startsWith("aloc_")) {
                    //aloc layer
                    gid = fid.substring(8);
                    filename = getConfig().getAlaspatialOutputPath() + File.separator + "aloc" + File.separator + gid + File.separator + gid;
                }

                if (filename != null) {
                    Grid grid = new Grid(filename);

                    if (grid != null && (new File(filename + ".grd").exists())) {
                        sb.append(String.valueOf(grid.getValues(p)[0]));
                    }
                }
            }
        }

        return sb.toString();
    }

    public HashMap<String, String> sampling(double longitude, double latitude) {
        init();
        
        HashMap<String, String> output = new HashMap<String, String>();

        HashMap<String, SimpleShapeFile> ssfs = getConfig().getShapeFileCache().getAll();
        for(Entry<String, SimpleShapeFile> entry : ssfs.entrySet()) {
            String s = entry.getValue().intersect(longitude, latitude);
            if(s == null) s = "";
            output.put(entry.getKey(), s);
        }

        if(gridReaders != null) {
            GridCacheReader gcr = null;
            HashMap<String, Float> gridValues = null;
            try {
                gcr = gridReaders.take();
                gridValues = gcr.sample(longitude, latitude);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(gcr != null) {
                    try {
                        gridReaders.put(gcr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if(gridValues != null) {
                for(Entry<String, Float> entry : gridValues.entrySet()) {
                    if(entry.getValue() == null || entry.getValue().isNaN()) {
                        output.put(entry.getKey(), "");
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
    public ArrayList<String> sampling(String[] fieldIds, double[][] points) {
        init();

        IntersectionFile[] intersectionFiles = new IntersectionFile[fieldIds.length];
        for (int i = 0; i < fieldIds.length; i++) {
            Field f = fieldDao.getFieldById(fieldIds[i]);

            if (f != null && f.getId() != null) {
                Layer l = layerDao.getLayerById(Integer.parseInt(f.getSpid()));
                if (l != null) {
                    logger.info(l.getName() + "," + l.getId() + "," + l.getPath_orig() + "," + l.getLicence_link() + "," + l.getLicence_level());
                    intersectionFiles[i] = new IntersectionFile(fieldIds[i], intersectConfig.getLayerFilesPath() + l.getPath_orig(), f.getSname(), l.getName(), f.getName());
                } else {
                    logger.warn("failed to find layer for id: " + f.getSpid());
                }
            } else {
                logger.warn("failed to find field for id: " + fieldIds[i]);
                intersectionFiles[i] = null;
            }
        }

        return sampling(intersectionFiles, points);
    }

    @Override
    public ArrayList<String> sampling(IntersectionFile[] intersectionFiles, double[][] points) {
        init();

        if (intersectConfig.getLayerIndexUrl() != null) {
            return remoteSampling(intersectionFiles, points);
        } else {
            return localSampling(intersectionFiles, points);
        }
    }

    @Override
    public IntersectConfig getConfig() {
        init();

        return intersectConfig;
    }

    ArrayList<String> localSampling(IntersectionFile[] intersectionFiles, double[][] points) {
        logger.info("begin LOCAL sampling, number of threads " + intersectConfig.getThreadCount() + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);
        long start = System.currentTimeMillis();
        int threadCount = intersectConfig.getThreadCount();
        SamplingThread[] threads = new SamplingThread[threadCount];
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue();
        CountDownLatch cdl = new CountDownLatch(intersectionFiles.length);
        ArrayList<String> output = new ArrayList<String>();
        for (int i = 0; i < intersectionFiles.length; i++) {
            output.add("");
            lbq.add(i);
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new SamplingThread(lbq, cdl, intersectionFiles, points, output, intersectConfig.getThreadCount(), intersectConfig.getShapeFileCache(), intersectConfig.getGridBufferSize());
            threads[i].start();
        }

        try {
            cdl.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();

            logger.error(null, ex);
        } finally {
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(null, e);
                }
            }
        }

        logger.info("End sampling, threads=" + threadCount
                + " layers=" + intersectionFiles.length
                + " in " + (System.currentTimeMillis() - start) + "ms");
        return output;
    }

    ArrayList<String> remoteSampling(IntersectionFile[] intersectionFiles, double[][] points) {
        logger.info("begin REMOTE sampling, number of threads " + intersectConfig.getThreadCount() + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);

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

            logger.info("sample time for " + 5 + " layers and " + 3 + " coordinates: get response=" + (mid - start) + "ms, write response=" + (end - mid) + "ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /**
     * Clean up and just return an int for LAYER object
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
}
