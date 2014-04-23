/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.intersect;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.ala.layers.client.Client;
import org.ala.layers.dao.LayerIntersectDAO;
import org.ala.layers.util.IntersectUtil;

/**
 * @author Adam
 */
public class Test {

    public static void main(String[] args) {
        System.out.println("args[0] = fieldIds e.g. cl22,el805,cl1918");
        System.out.println("args[1] = points string (lat,long) e.g. -22,132,-24,133,21,133,-29.911,132.769,-20.911,122.769");

        try {
            LayerIntersectDAO layerIntersect = Client.getLayerIntersectDao();

            String fieldList = args[0];
            String pointsList = args[1];

            String[] fields = fieldList.split(",");

            //sampling type 1
            System.out.println("SAMPLING TYPE 1");
            ArrayList<String> sample = layerIntersect.sampling(fieldList, pointsList);

            //output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String[] ps = pointsList.split(",");
            IntersectUtil.writeSampleToStream(fieldList.split(","), ps, sample, baos);
            baos.close();
            System.out.println(new String(baos.toByteArray()));

            //sampling type 2
            System.out.println("SAMPLING TYPE 2...");
            for (int i = 0; i < ps.length; i += 2) {
                Vector v = layerIntersect.samplingFull(fieldList, Double.parseDouble(ps[i + 1]), Double.parseDouble(ps[i]));
                System.out.println("vector=" + v);
                if (v != null && v.size() > 0 && v.get(0) != null) {
                    Map m = (Map) v.get(0);
                    for (Object key : m.keySet()) {
//                        for(int j=0;j<fields.length;j++) {
//                            if(key.equals(fields[j])) {
                        System.out.print(key + "=" + m.get(key) + ", ");
//                            }
//                        }
                    }
                    System.out.print("\n");
                } else {
                    System.out.println("failed for: " + ps[i + 1] + " " + ps[i]);
                }
            }

            System.out.println("\n\n Types 3 & 4 operate on the grid cache.  Shape files require shape cache to be initialized.");

            //sampling type 3
            System.out.println("SAMPLING TYPE 3");
            for (int i = 0; i < ps.length; i += 2) {
                HashMap<String, String> hm = layerIntersect.sampling(Double.parseDouble(ps[i + 1]), Double.parseDouble(ps[i]));
                if (hm != null && hm.size() > 0) {
                    Map m = hm;
                    for (Object key : m.keySet()) {
                        System.out.print(key + "=" + m.get(key) + ", ");
                    }
                    System.out.print("\n");
                } else {
                    System.out.println("failed for: " + ps[i + 1] + " " + ps[i]);
                }
            }

            //sampling type 4
            System.out.println("SAMPLING TYPE 4");
            for (int i = 0; i < ps.length; i += 2) {
                for (int j = 0; j < fields.length; j++) {
                    String v = layerIntersect.sampling(fields[j], Double.parseDouble(ps[i + 1]), Double.parseDouble(ps[i]));
                    if (v != null) {
                        System.out.print(fields[j] + "=" + v + ", ");
                    } else {
                        System.out.print("failed for " + fields[j] + " " + ps[i + 1] + " " + ps[i]);
                    }
                }
                System.out.print("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    static String loadPoints() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader("d:\\coordinates.txt"));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s[1]).append(",").append(s[0]);
            }
            br.close();
        } catch (Exception e) {
        }
        return sb.toString();
    }
}
