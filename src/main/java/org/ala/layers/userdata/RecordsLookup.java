/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.userdata;

import org.ala.layers.dao.UserDataDAO;
import org.ala.layers.legend.QueryField;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Adam
 */
public class RecordsLookup {

    private static Logger logger = Logger.getLogger(RecordsLookup.class);


    static final int MAX_SIZE = 500;
    final static String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir");

    @Resource(name = "userDataDao")
    private static UserDataDAO userDataDao;

    /**
     * store for ID and {last access time (Long) , cluster (Vector) }
     */
    static HashMap<String, Object[]> selections = new HashMap<String, Object[]>();

    static void addData(String id, Object data) {
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

    static void store(String key, Object[] obj) {
        //stored in postgres not a local file, so do nothing here
    }

    static Object[] retrieve(String key) {
        //is it a facet?
        String header_id = key;
        String facet_id = "";
        if (key.contains(":")) {
            header_id = key.split(":")[0];
            facet_id = key.split(":")[1];
        }

        //stored in postgres not a local file
        Object[] output = new Object[6];

        output[0] = String.valueOf(key);

        double[] points = userDataDao.getDoubleArray(key, "points");

        output[1] = points;

        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        for (String ref : userDataDao.listData(key, "QueryField")) {
            fields.add(userDataDao.getQueryField(header_id, ref));
        }
        output[2] = fields;

        output[3] = userDataDao.getMetadata(Long.valueOf(header_id));

        //bounding box
        double minX = points[0];
        double maxX = points[0];
        double minY = points[1];
        double maxY = points[1];
        for (int i = 0; i < points.length; i += 2) {
            if (minX > points[i]) minX = points[i];
            if (maxX < points[i]) maxX = points[i];
            if (minY > points[i + 1]) minY = points[i + 1];
            if (maxY < points[i + 1]) maxY = points[i + 1];
        }
        output[4] = new double[]{minX, minY, maxX, maxY};

        addData(key, output);

        return selections.get(key);

    }

    public static void setUserDataDao(UserDataDAO udDao) {
        userDataDao = udDao;
    }

    public static void addQf(String id, QueryField qf) {
        Object[] o = selections.get(id);

        if (o != null) {
            o[0] = Long.valueOf(System.currentTimeMillis());
            Object v = ((ArrayList<QueryField>) ((Object[]) o[1])[2]).add(qf);
        }
    }
}
