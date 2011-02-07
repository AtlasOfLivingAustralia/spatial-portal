/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class MakeOccurrenceParts extends Thread {

    /**
     * all occurrences data
     */
    HashMap<Integer, Object> occurrences;
    /**
     * index to actual csv field positions to named
     * fields
     */
    int[] column_positions;
    int[] sensitive_column_positions;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    String prev_key = null;
    /**
     * for column_keys write buffer to take some load off sorting
     */
    StringBuffer prev_value = null;
    TreeMap<String, Integer>[] columnKeys;
    int[] columnKeysToOccurrencesOrder;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;
    String occurrences_csv;
    String index_path;

    public MakeOccurrenceParts(LinkedBlockingQueue<String[]> lbq_, CountDownLatch cdl_, int[] column_positions_, int[] sensitive_column_positions_, String occurrences_csv_, String index_path_) {
        setPriority(Thread.MIN_PRIORITY);
        lbq = lbq_;
        cdl = cdl_;
        column_positions = column_positions_;
        sensitive_column_positions = sensitive_column_positions_;
        occurrences_csv = occurrences_csv_;
        index_path = index_path_;
    }

    public void run() {
        try {
            while (true) {
                String[] next = lbq.take();
                makePart(Long.parseLong(next[0]), next[1], next[2]);
                cdl.countDown();
            }
        } catch (Exception e) {
        }
    }

    void makePart(long part_number, String firstLine, String terminatingLine) {
        String[] columns = TabulationSettings.occurrences_csv_fields;
        String[] columnsSettings =
                TabulationSettings.occurrences_csv_field_settings;
        int i;
        columnKeys = new TreeMap[columns.length];
        for (i = 0; i < columnKeys.length; i++) {
            columnKeys[i] = new TreeMap<String, Integer>();
        }

        occurrences = new HashMap<Integer, Object>();

        int id_column_idx = sensitive_column_positions[0];
        int slong_column_idx = sensitive_column_positions[1];
        int slat_column_idx = sensitive_column_positions[2];

        /* read occurrences_csv */
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(occurrences_csv));

            FileWriter fwExcluded = new FileWriter(index_path
                    + OccurrencesIndex.EXCLUDED_FILENAME + part_number);

            FileWriter fws = new FileWriter(
                    index_path
                    + OccurrencesIndex.SENSITIVE_COORDINATES + part_number);

            String s;
            String[] sa;
            int[] il;
            Integer iv;

            /* lines read */
            int progress = 0;

            /* helps with ',' in text qualifier records */
            int max_columns = column_positions.length;

            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

            columnKeysToOccurrencesOrder = new int[columnsSettings.length];
            int p = 0;
            for (i = 0; i < ofu.onetwoCount; i++) {
                columnKeysToOccurrencesOrder[p++] = ofu.onestwos[i];
            }
            for (i = 0; i < ofu.zeroCount; i++) {
                columnKeysToOccurrencesOrder[p++] = ofu.zeros[i];
            }
            columnKeysToOccurrencesOrder[p++] = ofu.longitudeColumn;
            columnKeysToOccurrencesOrder[p] = ofu.latitudeColumn;

            br.skip(part_number * OccurrencesIndex.PART_SIZE_MAX);

            //if part number == 0, skip header as well, otherwise skip to first
            //valid line (2nd to read after br.skip()
            s = br.readLine();
            //check for continuation line
            while (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '\\') {
                String spart = br.readLine();
                if (spart == null) {  //same as whole line is null
                    break;
                } else {
                    s.replace('\\', ' ');   //new line is same as 'space'
                    s += spart;
                }
            }//repeat as necessary

            while ((s = br.readLine()) != null) {
                //check for continuation line
                while (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '\\') {
                    String spart = br.readLine();
                    if (spart == null) {  //same as whole line is null
                        break;
                    } else {
                        s.replace('\\', ' ');   //new line is same as 'space'
                        s += spart;
                    }
                }//repeat as necessary

                if (s.equals(terminatingLine)) {
                    break;
                }
                if (s.length() != s.getBytes().length) {
                    i = 2;
                }

                //apply limit for dev
                if (progress > TabulationSettings.occurrences_csv_max_records && TabulationSettings.occurrences_csv_max_records > 0) {
                    break;
                }

                sa = s.split(",");

                /* handlers for the text qualifiers and ',' in the middle */
                if (sa != null && sa.length > max_columns) {
                    sa = OccurrencesIndex.split(s);
                }

                /* remove quotes and commas form terms */
                for (i = 0; i < sa.length; i++) {
                    if (sa[i].length() > 0) {
                        sa[i] = sa[i].replace("\"", "");
                        sa[i] = sa[i].replace(",", " ");
                    }
                }

                progress++;

                //export for sensitive coordinates
                if (slat_column_idx >= sa.length
                        || slong_column_idx >= sa.length
                        || id_column_idx >= sa.length) {
                    System.out.println(s);
                } else if (sa[slong_column_idx] != null && sa[slong_column_idx].length() > 0
                        && sa[slat_column_idx] != null && sa[slat_column_idx].length() > 0) {
                    fws.append(sa[id_column_idx]).append(",").append(sa[slong_column_idx]).append(",").append(sa[slat_column_idx]).append("\n");
                }

                {
                    /*ignore records with no species or longitude or
                     * latitude */

                    if (sa.length >= columnsSettings.length
                            //&& sa[column_positions[ofu.speciesColumn]].length() > 0
                            && sa[column_positions[ofu.longitudeColumn]].length() > 0
                            && sa[column_positions[ofu.latitudeColumn]].length() > 0) {
                        try {
                            //parse long & lat, failure makes record skipped
                            Double.parseDouble(sa[column_positions[ofu.longitudeColumn]]);
                            Double.parseDouble(sa[column_positions[ofu.latitudeColumn]]);

                            /* get int vs unique key for every column */
                            il = new int[columnsSettings.length];
                            for (i = 0; i < columnsSettings.length; i++) {
                                iv = columnKeys[i].get(sa[column_positions[i]]);
                                if (iv == null) {
                                    il[i] = columnKeys[i].size();
                                    columnKeys[i].put(sa[column_positions[i]], columnKeys[i].size());
                                } else {
                                    il[i] = iv.intValue();
                                }
                            }

                            /* put into tree */
                            HashMap<Integer, Object> obj = occurrences;
                            HashMap<Integer, Object> objtmp;
                            for (i = 0; i < ofu.onetwoCount; i++) {
                                objtmp = (HashMap<Integer, Object>) obj.get(Integer.valueOf(il[ofu.onestwos[i]]));
                                if (objtmp == null) {
                                    objtmp = new HashMap<Integer, Object>();
                                    obj.put(Integer.valueOf(il[ofu.onestwos[i]]), objtmp);
                                }
                                obj = objtmp;
                            }

                            /* create int[] to add, longitude + latitude + zeros */
                            int[] it = new int[ofu.zeroCount + 2];
                            for (i = 0; i < ofu.zeroCount; i++) {
                                it[i] = il[ofu.zeros[i]];
                            }
                            it[i++] = il[ofu.longitudeColumn];
                            it[i] = il[ofu.latitudeColumn];

                            ArrayList<int[]> al;

                            //add
                            if (obj.size() == 0) {
                                al = new ArrayList<int[]>();
                                al.add(it);
                                obj.put(Integer.valueOf(0), al);
                            } else {
                                al = (ArrayList<int[]>) obj.get(Integer.valueOf(0));
                                al.add(it);
                            }
                        } catch (Exception e) {
                            //error
                            fwExcluded.append(s).append("\n");
                        }
                    } else {
                        fwExcluded.append(s).append("\n");
                    }
                }
            }

            //export remaining sorted part
            exportSortedPart((int) part_number);
            //reset
            occurrences = new HashMap<Integer, Object>();
            for (i = 0; i < columnKeys.length; i++) {
                columnKeys[i] = new TreeMap<String, Integer>();
            }

            fwExcluded.close();
            br.close();

            fws.close();

            SpatialLogger.log("loadoccurrences done: " + part_number);
        } catch (Exception e) {
            SpatialLogger.log("loadoccurrences", e.toString());
            e.printStackTrace();
        }
    }

    void exportSortedPart(int partNumber) {
        System.out.println("exporting part: " + partNumber);

        int i;

        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

        try {
            /* sorting & exporting of keys */
            ArrayList<int[]> columnKeysOrder = new ArrayList<int[]>(columnKeys.length);
            ArrayList<String[]> columnKeysReverseOrderStrings = new ArrayList<String[]>(columnKeys.length);
            int j;
            for (i = 0; i < columnKeys.length; i++) {
                int[] il = new int[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                String[] ilStringsReverseOrder = new String[columnKeys[columnKeysToOccurrencesOrder[i]].size()];
                Set<Map.Entry<String, Integer>> mes = columnKeys[columnKeysToOccurrencesOrder[i]].entrySet();
                Iterator<Map.Entry<String, Integer>> mei = mes.iterator();
                Map.Entry<String, Integer> me;

                /* make key to order mapping */
                j = 0;
                while (mei.hasNext()) {
                    me = mei.next();
                    il[me.getValue().intValue()] = j;
                    ilStringsReverseOrder[j] = me.getKey();
                    j++;
                }

                columnKeysOrder.add(il);
                columnKeysReverseOrderStrings.add(ilStringsReverseOrder);
            }

            /* open output file */
            FileWriter sorted = new FileWriter(
                    index_path + OccurrencesIndex.SORTED_FILENAME + "_" + partNumber);

            /* store points (long/lat) here until exported */
            ArrayList<Double> aPoints = new ArrayList<Double>(500000);


            //write to file
            StringBuffer s = new StringBuffer();
            writeMap(sorted, aPoints, occurrences, columnKeysOrder, columnKeysReverseOrderStrings, 0, ofu.onetwoCount, s);
            sorted.close();

            /* export points */
            RandomAccessFile points = new RandomAccessFile(
                    index_path + OccurrencesIndex.POINTS_FILENAME + "_" + partNumber,
                    "rw");
            byte[] b = new byte[aPoints.size() * 8];
            ByteBuffer bb = ByteBuffer.wrap(b);
            for (Double d : aPoints) {
                bb.putDouble(d.doubleValue());
            }
            points.write(b);
            points.close();

            SpatialLogger.log("exportSortedPoints done");
        } catch (Exception e) {
            SpatialLogger.log("exportSortedPoints", e.toString());
            e.printStackTrace();
        }
    }

    void writeMap(FileWriter sorted, ArrayList<Double> aPoints, HashMap<Integer, Object> map,
            ArrayList<int[]> columnKeysOrder,
            ArrayList<String[]> columnKeysReverseOrderStrings,
            int depth, int maxDepth, StringBuffer line) {
        /* two cases; last record (one object, is arraylist of String []),
         * not last record (object is another hash map)
         */
        if (depth == maxDepth) {
            ArrayList<int[]> ai =
                    (ArrayList<int[]>) map.get(Integer.valueOf(0));
            int i, j;
            try {
                String linec = new String(line.toString().getBytes("US-ASCII"));
                for (i = 0; i < ai.size(); i++) {
                    sorted.append(linec);
                    int[] sl = ai.get(i);
                    for (j = 0; j < sl.length; j++) {
                        int[] keysOrder = columnKeysOrder.get(depth + j);
                        String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j);
                        String s = new String(keysReverseOrderStrings[keysOrder[sl[j]]].getBytes("US-ASCII"));
                        sorted.append(s);

                        if (j < sl.length - 1) {
                            sorted.append(",");
                        }
                    }

                    //parse longlat
                    double longitude = 0;
                    double latitude = 0;
                    try {
                        int[] keysOrder = columnKeysOrder.get(depth + j - 2);
                        String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 2);
                        longitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j - 2]]]);

                        keysOrder = columnKeysOrder.get(depth + j - 1);
                        keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth + j - 1);

                        latitude = Double.parseDouble(keysReverseOrderStrings[keysOrder[sl[j - 1]]]);

                        //translate to -180 to +180
                        while (longitude < -180) {
                            longitude += 360;
                        }
                        while (longitude >= 180) {
                            longitude -= 360;
                        }
                        while (latitude < -180) {
                            longitude += 360;
                        }
                        while (latitude >= 180) {
                            longitude -= 360;
                        }

                    } catch (Exception e) {
                    }
                    aPoints.add(longitude);
                    aPoints.add(latitude);

                    sorted.append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //sort this hash map as in a tree map
            TreeMap<Integer, Object> sortedMap = new TreeMap<Integer, Object>();
            int[] keysOrder = columnKeysOrder.get(depth);
            Iterator<Map.Entry<Integer, Object>> it = map.entrySet().iterator();
            Map.Entry<Integer, Object> me;
            while (it.hasNext()) {
                me = it.next();
                //translate Integer value
                sortedMap.put(keysOrder[me.getKey().intValue()], me.getValue());
            }

            //iterate over & write
            String[] keysReverseOrderStrings = columnKeysReverseOrderStrings.get(depth);
            it = sortedMap.entrySet().iterator();
            while (it.hasNext()) {
                me = it.next();

                StringBuffer sb = new StringBuffer(line.toString());
                sb.append(keysReverseOrderStrings[me.getKey().intValue()]);
                sb.append(",");

                //drill down
                writeMap(sorted, aPoints, (HashMap<Integer, Object>) me.getValue(), columnKeysOrder,
                        columnKeysReverseOrderStrings, depth + 1, maxDepth, sb);
            }
        }
    }
}
