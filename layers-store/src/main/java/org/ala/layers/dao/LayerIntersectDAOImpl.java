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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Resource;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Layer;
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.intersect.SamplingThread;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 *
 * @author adam
 */
@Service("layerIntersectDao")
public class LayerIntersectDAOImpl implements LayerIntersectDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(LayerPidDAOImpl.class);
    @Resource(name = "fieldDao")
    private FieldDAO fieldDao;
    @Resource(name = "layerDao")
    private LayerDAO layerDao;
    IntersectConfig intersectConfig;

    void init() {
        if (intersectConfig == null) {
            intersectConfig = new IntersectConfig(fieldDao, layerDao);
        } else {
            intersectConfig.load();
        }
    }

    @Override
    public ArrayList<String[]> sampling(String fieldIds, String pointsString) {
        init();

        //parse points
        String[] pointsArray = pointsString.split(",");
        double[][] points = new double[pointsArray.length / 2][2];
        for (int i = 0; i < pointsArray.length; i += 2) {
            points[i / 2][0] = Double.parseDouble(pointsArray[i]);
            points[i / 2][1] = Double.parseDouble(pointsArray[i + 1]);
        }

        //parse fids
        String[] fidsArray = fieldIds.split(",");

        return sampling(fidsArray, points);
    }

    @Override
    public ArrayList<String[]> sampling(String[] fieldIds, double[][] points) {
        init();

        IntersectionFile[] intersectionFiles = new IntersectionFile[fieldIds.length];
        for (int i = 0; i < fieldIds.length; i++) {
            Field f = fieldDao.getFieldById(fieldIds[i]);

            if (f != null && f.getId() != null) {
                Layer l = layerDao.getLayerById(Integer.parseInt(f.getSpid()));
                if (l != null) {
                    logger.info(l.getName() + "," + l.getId() + "," + l.getPath_orig() + "," + l.getLicence_link() + "," + l.getLicence_level());
                    intersectionFiles[i] = new IntersectionFile(fieldIds[i], intersectConfig.getLayerFilesPath() + l.getPath_orig(), f.getSname());
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
    public ArrayList<String[]> sampling(IntersectionFile[] intersectionFiles, double[][] points) {
        init();

        logger.info("begin samplingFull, number of threads " + intersectConfig.getThreadCount() + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);
        System.out.println("begin samplingFull, number of threads " + intersectConfig.getThreadCount() + ", number of layers=" + intersectionFiles.length + ", number of coordinates=" + points.length);
        long start = System.currentTimeMillis();
        int threadCount = intersectConfig.getThreadCount();
        SamplingThread[] threads = new SamplingThread[threadCount];
        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue();
        CountDownLatch cdl = new CountDownLatch(intersectionFiles.length);
        ArrayList<String[]> output = new ArrayList<String[]>();
        for (int i = 0; i < intersectionFiles.length; i++) {
            output.add(new String[points.length]);
            lbq.add(i);
        }

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new SamplingThread(lbq, cdl, intersectionFiles, points, output, intersectConfig.getThreadCount(), intersectConfig.getShapeFileCache());
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
        System.out.println("End sampling, threads=" + threadCount
                + " layers=" + intersectionFiles.length
                + " in " + (System.currentTimeMillis() - start) + "ms");
        return output;
    }

    @Override
    public IntersectConfig getConfig() {
        init();

        return intersectConfig;
    }
}
