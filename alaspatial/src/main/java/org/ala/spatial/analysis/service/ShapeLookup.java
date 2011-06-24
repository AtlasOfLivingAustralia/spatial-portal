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
import org.ala.spatial.analysis.index.FilteringIndex;
import org.ala.spatial.analysis.index.LayerFilter;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.util.ComplexRegion;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.SimpleShapeFile;
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

        store(id, o);

        shapes.put(id, o);
    }

    private static void freeShapes() {
        if (shapes.size() > 80){//TabulationSettings.cluster_lookup_size) {
            Long time_min = new Long(0);
            Long time_max = new Long(0);
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
            File file = new File(TabulationSettings.file_store
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
            File file = new File(TabulationSettings.file_store
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

    public static boolean addShape(String id, String table, String value) {
        LayerFilter lf = FilteringIndex.getLayerFilter(table);
        if(lf == null) {
            return false;
        }
        int pos = java.util.Arrays.binarySearch(lf.catagory_names, value);

        //does it exist?
        if(pos >= 0 && pos < lf.catagory_names.length){
            ComplexRegion cr = SimpleShapeFile.loadShapeInRegion(
                    TabulationSettings.index_path + table, pos);

            //attach species points to this region
            lf.catagories = new int[1];
            lf.catagories[0] = pos;
            if(cr.getAttribute("species_records") == null){
                cr.setAttribute("species_records",OccurrencesCollection.getCatagorySampleSet(lf));
            }
            /*if(cr.getAttribute("cells") == null){
                cr.setAttribute("cells",cr.getOverlapGridCells(
                        TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                        TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                        TabulationSettings.grd_ncols, TabulationSettings.grd_nrows,
                        null));
            }*/

            addShape(id,cr);

            return true;
        }

        return false;
    }
}
