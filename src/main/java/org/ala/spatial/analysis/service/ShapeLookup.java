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
import java.util.HashMap;
import java.util.Map.Entry;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class ShapeLookup {

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> shapes = new HashMap<String, Object[]>();

    static public void addShape(String id, SimpleRegion region) {
        Object[] o = shapes.get(id);
        if (o == null) {
            freeShapes();
            o = new Object[2];
        }
        o[0] = new Long(System.currentTimeMillis());
        o[1] = region;

        shapes.put(id, o);
    }

    private static void freeShapes() {
        if (shapes.size() > 200){//TabulationSettings.cluster_lookup_size) {
            Long time_min = new Long(0);
            Long time_max = new Long(0);
            String key = null;
            Object o = null;
            for (Entry<String, Object[]> e : shapes.entrySet()) {
                if (time_min == 0 || (Long) e.getValue()[0] < time_min) {
                    time_min = (Long) e.getValue()[0];
                }
                if (time_max == 0 || (Long) e.getValue()[0] < time_max) {
                    time_max = (Long) e.getValue()[0];
                }
            }
            Long time_mid = (time_max - time_min) / 2 + time_min;
            for (Entry<String, Object[]> e : shapes.entrySet()) {
                if ((Long) e.getValue()[0] < time_mid) {
                    shapes.remove(e.getKey());
                }
            }
        }
    }

    public static SimpleRegion getShape(String id) {
        String s = id.substring(id.lastIndexOf(',')+1);
        if(s.indexOf(")") > 0){
            s = s.substring(0,s.length()-1);
        }

        Object[] o = shapes.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            o[0] = new Long(System.currentTimeMillis());
            SimpleRegion sr = (SimpleRegion) o[1];
            return sr;
        }

        return null;
    }

    static void store(String key, Object [] o) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "shape" + key + ".dat");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            SimpleRegion v = (SimpleRegion) (o[1]);
            oos.writeObject(v);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Object[] retrieve(String key) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "shape" + key + ".dat");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                SimpleRegion v = (SimpleRegion) ois.readObject();
                ois.close();

                addShape(key, v);
                return shapes.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
