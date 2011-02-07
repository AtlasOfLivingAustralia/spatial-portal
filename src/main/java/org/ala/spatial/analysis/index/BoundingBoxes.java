/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.HashMap;
import org.ala.spatial.analysis.service.LoadedPointsService;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class BoundingBoxes {
//TODO: bounding box hashmap cleanup

    static HashMap<String, double[]> lsidBoundingBox = new HashMap<String, double[]>();

    static public double[] getLsidBoundingBoxDouble(String lsid) {
        double[] bb = lsidBoundingBox.get(lsid);
        if (bb == null) {
            bb = LoadedPointsService.getBoundingBox(lsid);
            if (bb != null) {
                return bb;
            }

            double[] p = OccurrencesCollection.getPoints(new OccurrencesFilter(lsid, TabulationSettings.MAX_RECORD_COUNT_CLUSTER));

            if (p != null) {
                double minx = p[0];
                double miny = p[1];
                double maxx = minx;
                double maxy = miny;
                for (int i = 0; i < p.length; i += 2) {
                    if (minx > p[i]) {
                        minx = p[i];
                    }
                    if (maxx < p[i]) {
                        maxx = p[i];
                    }
                    if (miny > p[i + 1]) {
                        miny = p[i + 1];
                    }
                    if (maxy < p[i + 1]) {
                        maxy = p[i + 1];
                    }
                }
                bb = new double[4];
                bb[0] = minx;
                bb[1] = miny;
                bb[2] = maxx;
                bb[3] = maxy;
                lsidBoundingBox.put(lsid, bb);
            }
        }
        return bb;
    }

    static public void putLSIDBoundingBox(String lsid, double[] bb) {
        lsidBoundingBox.put(lsid, bb);
    }

    static public String getLsidBoundingBox(String lsid) {
        double[] bb = getLsidBoundingBoxDouble(lsid);

        StringBuffer sb = new StringBuffer();
        sb.append(bb[0]).append(",").append(bb[1]).append(",").append(bb[2]).append(",").append(bb[3]);

        return sb.toString();
    }
}
