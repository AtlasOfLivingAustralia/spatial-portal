/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.wms;

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
import org.ala.spatial.util.QueryField;
import org.ala.spatial.util.SolrQuery;

/**
 *
 * @author Adam
 */
public class RecordsLookup {
    static final int MAX_SIZE = 500;
    final static String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir");

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> selections = new HashMap<String, Object[]>();

    static public void addData(String id, Object data) {
        Object[] o = selections.get(id);
        if (o == null) {
            freeMem();
            o = new Object[2];
        }
        o[0] = Long.valueOf(System.currentTimeMillis());
        o[1] = data;

        store(id, o);

        selections.put(id, o);
    }

    public static Object getData(String id) {
        Object[] o = selections.get(id);
        if (o == null) {
            o = retrieve(id);
        }
        if (o != null) {
            o[0] = Long.valueOf(System.currentTimeMillis());
            Object v = o[1];
            return v;
        }

        return null;
    }

    private static void freeMem() {
        if (selections.size() > MAX_SIZE) {
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

    static void store(String key, Object[] o) {
        try {
            File file = new File(TEMP_FILE_PATH + File.separator + "selection" + key + ".dat");
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            Object v = (o[1]);
            oos.writeObject(v);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Object[] retrieve(String key) {
        try {
            File file = new File(TEMP_FILE_PATH + File.separator + "selection" + key + ".dat");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object v = ois.readObject();
                ois.close();

                addData(key, v);
                return selections.get(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object v = queryData(key);
        addData(key, v);

        return selections.get(key);
    }

    static Object queryData(String key) {
        long start = System.currentTimeMillis();

        ArrayList<QueryField> fields = getAllFields();

        double [] points = new SolrQuery(key, null, null).getPoints(fields);

        Object [] o = new Object[2];
        o[0] = points;
        o[1] = fields;

        long end = System.currentTimeMillis();
        System.out.println("query:" + key + " time:" + (System.currentTimeMillis() - start) + "ms");

        return o;
    }

    static ArrayList<QueryField> getAllFields() {
        ArrayList<QueryField> fields = new ArrayList<QueryField> ();
        fields.add(new QueryField("id", "uuid", true));
        fields.add(new QueryField("basis_of_record", "basisOfRecord", true));
        //fields.add(new QueryField("type_status"));
        fields.add(new QueryField("institution_uid", "institutionUid", true));
        fields.add(new QueryField("collection_uid", "collectionUid", true));
        fields.add(new QueryField("data_resource", "dataResourceName", true));
        //fields.add(new QueryField("country"));
        fields.add(new QueryField("state", "stateProvince", true));
        //fields.add(new QueryField("biogeographic_region"));
        //fields.add(new QueryField("rank"));
        //fields.add(new QueryField("species_group"));
        fields.add(new QueryField("kingdom", "kingdom", true));
        fields.add(new QueryField("family","family", true));
        //fields.add(new QueryField("species_guid"));
        //fields.add(new QueryField("uncertainty"));
        //fields.add(new QueryField("state_conservation"));
        //fields.add(new QueryField("raw_state_conservation"));
        //fields.add(new QueryField("sensitive"));
        //fields.add(new QueryField("assertions"));
        //fields.add(new QueryField("month"));
        //fields.add(new QueryField("year"));
        //fields.add(new QueryField("multimedia"));

        return fields;
    }
}
