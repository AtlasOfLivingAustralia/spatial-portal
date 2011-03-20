/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.analysis.index.OccurrenceRecordNumbers;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;

/**
 *
 * @author Adam
 */
public class LoadedPoints {

    double[][] points;
    HashMap<String, Object> attributes;
    String name;

    public LoadedPoints(double[][] points_, String name_, String[] ids_) {
        points = points_;
        name = name_;
        attributes = new HashMap<String, Object>();

        //-180 < longitude <= 180
        for (int i = 0; i < points_.length; i++) {
            while (points_[i][0] <= -180) {
                points_[i][0] += 360;
            }
            while (points_[i][0] > 180) {
                points_[i][0] -= 360;
            }
        }

        //default to line numbers as ids
        if (ids_ == null) {
            String[] ids = new String[points_.length];
            for (int i = 0; i < points_.length; i++) {
                ids[i] = String.valueOf(i + 1);
            }

            ids_ = ids;
        }

        attributes.put("id", ids_);
    }

    public String getName() {
        return name;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public double[][] getPoints(SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records) {
        if (region == null) {
            return points;
        } else {
            int[] valid_points = new int[points.length];
            int p = 0;
            for (int i = 0; i < points.length; i++) {
                if (region.isWithin(points[i][0], points[i][1])) {
                    valid_points[p] = i;
                    p++;
                }
            }
            double[][] output = new double[p][2];
            for (int i = 0; i < p; i++) {
                output[i][0] = points[valid_points[i]][0];
                output[i][1] = points[valid_points[i]][1];
            }

            return output;
        }
    }

    public double[] getPointsFlat(SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records) {
        if (region == null) {
            double[] output = new double[points.length * 2];
            for (int i = 0; i < points.length; i++) {
                output[i * 2] = points[i][0];
                output[i * 2 + 1] = points[i][1];
            }
            return output;
        } else {
            int[] valid_points = new int[points.length];
            int p = 0;
            for (int i = 0; i < points.length; i++) {
                if (region.isWithin(points[i][0], points[i][1])) {
                    valid_points[p] = i;
                    p++;
                }
            }
            double[] output = new double[p * 2];
            for (int i = 0; i < p; i++) {
                output[i * 2] = points[valid_points[i]][0];
                output[i * 2 + 1] = points[valid_points[i]][1];
            }

            return output;
        }
    }

    public void buildSampling(Layer[] layers) {
        //any previous sampling?
        ConcurrentHashMap<String, String[]> sampling = (ConcurrentHashMap<String, String[]>) getAttribute("sampling");
        if (sampling == null) {
            sampling = new ConcurrentHashMap<String, String[]>();
        }
        if (layers == null) {
            return;
        }

        LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue<Integer>();

        for (int i = 0; i < layers.length; i++) {
            //only add it it is not already sampled
            if (!sampling.containsKey(layers[i].name)) {
                lbq.add(i);
            }
        }

        if (lbq.size() > 0) {
            CountDownLatch cdl = new CountDownLatch(lbq.size());

            LoadedPointsSamplingThread[] lpst = new LoadedPointsSamplingThread[layers.length];

            for (int i = 0; i < lpst.length; i++) {
                lpst[i] = new LoadedPointsSamplingThread(lbq, cdl, layers, points, sampling);
                lpst[i].start();
            }

            try {
                cdl.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

            setAttribute("sampling", sampling);
        }
    }

    public String getSampling(Layer[] layers, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, int max_rows) {
        buildSampling(layers);
        ConcurrentHashMap<String, String[]> sampling = (ConcurrentHashMap<String, String[]>) getAttribute("sampling");
        String[] ids = (String[]) getAttribute("id");

        ArrayList<String[]> columns = null;
        if (layers != null) {
            columns = new ArrayList<String[]>(layers.length);
            for (int i = 0; i < layers.length; i++) {
                columns.add(sampling.get(layers[i].name));
            }
        }

        StringBuffer sb = new StringBuffer();

        //header: longitude, latitude, layernames
        sb.append("id,longitude,latitude");

        if (layers != null) {
            for (int i = 0; i < layers.length; i++) {
                sb.append(",").append(layers[i].display_name);
            }
        }
        sb.append("\r\n");

        //rows:
        int rowsAdded = 0;
        for (int i = 0; i < points.length && rowsAdded < max_rows; i++) {
            //is it a valid point?
            if ((region == null || region.isWithin(points[i][0], points[i][1]))) {
                sb.append(ids[i]).append(",");
                sb.append(points[i][0]).append(",").append(points[i][1]);
                if (layers != null) {
                    for (int j = 0; j < layers.length; j++) {
                        sb.append(",").append(columns.get(j)[i]);
                    }
                }
                sb.append("\r\n");
                rowsAdded++;
            }
        }

        return sb.toString();
    }

    String[] getIds() {
        String[] ids = (String[]) attributes.get("id");
        if (ids == null) {
            ids = new String[points.length];
        }
        return ids;
    }
}
