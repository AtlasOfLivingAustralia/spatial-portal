/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class RecordsLookup {

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> selections = new HashMap<String, Object[]>();

    static public void addRecords(String id, int[] selection) {
        Object[] o = selections.get(id);
        if (o == null) {
            freeRecords();
            o = new Object[2];
        }
        o[0] = Long.valueOf(System.currentTimeMillis());
        o[1] = selection;

        selections.put(id, o);
    }

    private static void freeRecords() {
        if (selections.size() > TabulationSettings.cluster_lookup_size) {
            Long time_min = Long.valueOf(0);
            Long time_max = Long.valueOf(0);

            for (Entry<String, Object[]> e : selections.entrySet()) {
                if (time_min == 0 || (Long) e.getValue()[0] < time_min) {
                    time_min = (Long) e.getValue()[0];
                }
                if (time_max == 0 || (Long) e.getValue()[0] < time_max) {
                    time_max = (Long) e.getValue()[0];
                }
            }
            Long time_mid = (time_max - time_min) / 2 + time_min;
            for (Entry<String, Object[]> e : selections.entrySet()) {
                if ((Long) e.getValue()[0] < time_mid) {
                    selections.remove(e.getKey());
                }
            }
        }
    }

    public static int[] getRecords(String id) {
        Object[] o = selections.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            o[0] = Long.valueOf(System.currentTimeMillis());
            int[] v = (int[]) o[1];
            return v;
        }

        return null;
    }

    static void store(String key, Object[] o) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "selection" + key + ".dat");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            int[] v = (int[]) (o[1]);
            oos.writeObject(v);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Object[] retrieve(String key) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir")
                    + "selection" + key + ".dat");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                int[] v = (int[]) ois.readObject();
                ois.close();

                addRecords(key, v);
                return selections.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
