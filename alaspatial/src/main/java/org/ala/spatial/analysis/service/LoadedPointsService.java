/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import org.ala.spatial.analysis.index.BoundingBoxes;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 * Allow alaspatial to operate on loaded points (lat long)
 *
 * Used in
 * - Sampling
 * - Maxent
 * - Clustering
 *
 * @author Adam
 */
public class LoadedPointsService {

    /**
     * store for ID and {last access time (Long) , points (double[][2]) }
     */
    static HashMap<String, Object[]> clusters = new HashMap<String, Object[]>();

    static public void addCluster(String id, LoadedPoints points) {
        Object[] o = clusters.get(id);
        if (o == null) {
            freeCluster();
            o = new Object[2];
        }
        o[0] = new Long(System.currentTimeMillis());
        o[1] = points;

        clusters.put(id, o);
    }

    private static void freeCluster() {
        if (clusters.size() > TabulationSettings.cluster_lookup_size) { //TODO: unique setting
            Long time_min = new Long(0);
            Long time_max = new Long(0);
            for (Entry<String, Object[]> e : clusters.entrySet()) {
                if (time_min == 0 || (Long) e.getValue()[0] < time_min) {
                    time_min = (Long) e.getValue()[0];
                }
                if (time_max == 0 || (Long) e.getValue()[0] < time_max) {
                    time_max = (Long) e.getValue()[0];
                }
            }
            Long time_mid = (time_max - time_min) / 2 + time_min;
            for (Entry<String, Object[]> e : clusters.entrySet()) {
                if ((Long) e.getValue()[0] < time_mid) {
                    clusters.remove(e.getKey());
                }
            }
        }
    }

    static void store(String key, Object[] o) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "points" + key + ".dat");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            LoadedPoints lp = (LoadedPoints) (o[1]);
            oos.writeObject(lp);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Object[] retrieve(String key) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "cluster" + key + ".dat");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                LoadedPoints lp = (LoadedPoints) ois.readObject();
                ois.close();

                addCluster(key, lp);
                return clusters.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static LoadedPoints getLoadedPoints(String id) {
        Object[] o = clusters.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            return (LoadedPoints) o[1];
        }
        return null;
    }

    static public double[][] getPoints(String id, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records) {
        LoadedPoints lp = getLoadedPoints(id);
        if (lp != null) {
            return lp.getPoints(region, records);
        }
        return null;
    }

    static public double[] getPointsFlat(String id, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records) {
        LoadedPoints lp = getLoadedPoints(id);
        if (lp != null) {
            return lp.getPointsFlat(region, records);
        }
        return null;
    }

    static public String getSampling(String id, Layer[] layers, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, int max_rows) {
        LoadedPoints lp = getLoadedPoints(id);
        if (lp != null) {
            return lp.getSampling(layers, region, records, max_rows);
        }
        return null;
    }

    public static String[] getIds(String id) {
        LoadedPoints lp = getLoadedPoints(id);
        if (lp != null) {
            return lp.getIds();
        }
        return null;
    }

    public static double [] getBoundingBox(String id) {
        LoadedPoints lp = getLoadedPoints(id);
        if (lp != null) {
            double[][] points = getPoints(id, null, null);

            double minx = points[0][0];
            double miny = points[0][1];
            double maxx = points[0][0];
            double maxy = points[0][1];
            for (int i = 0; i < points.length; i++) {
                if (minx > points[i][0]) {
                    minx = points[i][0];
                }
                if (maxx < points[i][0]) {
                    maxx = points[i][0];
                }
                if (miny > points[i][1]) {
                    miny = points[i][1];
                }
                if (maxy < points[i][1]) {
                    maxy = points[i][1];
                }
            }
            double [] bb = new double[4];
            bb[0] = minx;
            bb[1] = miny;
            bb[2] = maxx;
            bb[3] = maxy;
            BoundingBoxes.putLSIDBoundingBox(id, bb);
            return bb;
        }
        return null;
    }
}
