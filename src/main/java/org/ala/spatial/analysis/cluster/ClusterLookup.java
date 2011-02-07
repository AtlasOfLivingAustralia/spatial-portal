/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.cluster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class ClusterLookup {

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> clusters = new HashMap<String, Object[]>();

    static public void addCluster(String id, Vector cluster) {
        Object[] o = clusters.get(id);
        if (o == null) {
            freeCluster();
            o = new Object[2];
        }
        o[0] = Long.valueOf(System.currentTimeMillis());
        o[1] = cluster;

        clusters.put(id, o);
    }

    private static void freeCluster() {
        if (clusters.size() > TabulationSettings.cluster_lookup_size) {
            Long time_min = Long.valueOf(0);
            Long time_max = Long.valueOf(0);

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

    public static long getClusterId(String id, int cluster, int idx) {
        Object[] o = clusters.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            o[0] = Long.valueOf(System.currentTimeMillis());
            Vector<Vector> v = (Vector<Vector>) o[1];
            if (v.size() > cluster && cluster >= 0) {
                Vector c = v.get(cluster);
                if (c.size() > idx && idx >= 0) {
                    Record r = (Record) c.get(idx);
                    return r.getId();
                }
            }
        }

        return Long.MIN_VALUE;
    }

    static void store(String key, Object[] o) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "cluster" + key + ".dat");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            Vector v = (Vector) (o[1]);
            oos.writeObject(v);
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
                Vector v = (Vector) ois.readObject();
                ois.close();

                addCluster(key, v);
                return clusters.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
