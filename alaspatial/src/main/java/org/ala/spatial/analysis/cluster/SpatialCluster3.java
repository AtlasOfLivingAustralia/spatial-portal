/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.cluster;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * @author ajay
 */
public class SpatialCluster3 {

    private int map_zoom = 21;
    private int map_offset = 268435456; // half the Earth's circumference at zoom level 21
    private double map_radius = map_offset / Math.PI;
    double meters_per_pixel = 78271.5170; //at zoom level 1
    int current_zoom = 0;
    Vector global_clusters;
    private int current_min_distance;

    public int convertLngToPixel(double lng) {
        return (int) Math.round(map_offset + map_radius * lng * Math.PI / 180);
    }

    public double convertPixelToLng(int px) {
        return (px - map_offset) / map_radius * 180 / Math.PI;
    }

    public int convertLatToPixel(double lat) {
        return (int) Math.round(map_offset - map_radius
                * Math.log((1 + Math.sin(lat * Math.PI / 180))
                / (1 - Math.sin(lat * Math.PI / 180))) / 2);
    }

    public double convertPixelToLat(int px) {
        return Math.asin((Math.pow(Math.E, ((map_offset - px) / map_radius * 2)) - 1) / (1 + Math.pow(Math.E, ((map_offset - px) / map_radius * 2)))) * 180 / Math.PI;
    }

    public double convertMetersToPixels(double meters, double latitude, int zoom) {
        return meters / ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom)));
    }

    public double convertPixelsToMeters(int pixels, double latitude, int zoom) {
        return ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom))) * pixels;
    }

    public double convertMetersToLng(double meters) {
        return meters / 20037508.342789244 * 180;
    }

    public double convertMetersToLat(double meters) {
        return 180.0 / Math.PI * (2 * Math.atan(Math.exp(meters / 20037508.342789244 * Math.PI)) - Math.PI / 2.0);
    }

    public int planeDistance(double lat1, double lng1, double lat2, double lng2, int zoom) {
        // Given a pair of lat/long coordinates and a map zoom level, returns
        // the distance between the two points in pixels

        int x1 = convertLngToPixel(lng1);
        int y1 = convertLatToPixel(lat1);

        int x2 = convertLngToPixel(lng2);
        int y2 = convertLatToPixel(lat2);

        int distance = (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

        return distance >> (map_zoom - zoom);

    }

    public Vector cluster(Vector<Record> datapoints, int cluster_distance, int zoom, int min_radius) {
        if (datapoints.size() == 0) {
            return null;
        }
        current_zoom = zoom;
        current_min_distance = min_radius;

        // Groups points that are less than cluster_distance pixels apart at
        // a given zoom level into a cluster.

        Vector<Vector> clusters = new Vector();
        boolean[] removed = new boolean[datapoints.size()];

        for (int i = datapoints.size() - 1; i > -1; i--) {
            if (removed[i]) {
                continue;
            }
            Record r = (Record) datapoints.get(i);

            Vector<Record> cluster = new Vector();
            for (int j = 0; j < i; j++) {
                if (removed[j]) {
                    continue;
                }

                Record r2 = (Record) datapoints.get(j);

                int pixel_distance = planeDistance(r.getLatitude(), r.getLongitude(), r2.getLatitude(), r2.getLongitude(), zoom);

                /* If two markers are closer than given distance remove */
                /* target marker from array and add it to cluster.          */
                if (pixel_distance < cluster_distance) {
                    cluster.add(r2);
                    removed[j] = true;
                }
            }

            // add the first point to the cluster
            /* If a marker has been added to cluster, add also the one  */
            /* we were comparing to and remove the original from array. */
            cluster.add(r);
            clusters.add(cluster);
        }

        setupAttributes(clusters);

        return clusters;
    }

    void setupAttributes(Vector clusters) {
        global_clusters = clusters;

        makeCentroids();
        makeRadius();
        makeDensity();
    }
    double[] global_radius;
    double[] global_density;
    double[][] global_centroids;
    double[] global_uncertainty;

    public double getRadius(int i) {
        return global_radius[i];
    }

    public double getUncertainty(int i) {
        return global_uncertainty[i];
    }

    public double getDensity(int i) {
        return global_density[i];
    }

    /**
     * return centroid
     * [0] longitude
     * [1] latitude
     * 
     * @param i
     * @return
     */
    public double[] getCentroid(int i) {
        double[] d = {global_centroids[i][0], global_centroids[i][1]};
        return d;
    }

    void makeCentroids() {
        global_centroids = new double[global_clusters.size()][2];
        for (int i = 0; i < global_clusters.size(); i++) {
            Vector cluster = (Vector) global_clusters.get(i);

            if (cluster.size() == 1) {
                global_centroids[i][0] = ((Record) cluster.get(0)).getLongitude();
                global_centroids[i][1] = ((Record) cluster.get(0)).getLatitude();
            } else {
                double[] extents = getExtents(cluster);

                if (convertLngToPixel(extents[0]) == convertLngToPixel(extents[2])
                        && convertLatToPixel(extents[1]) == convertLatToPixel(extents[3])) {
                    global_centroids[i][0] = ((Record) cluster.get(0)).getLongitude();
                    global_centroids[i][1] = ((Record) cluster.get(0)).getLatitude();
                }

                //get furthest point for each quarter of the extents box
                double longitude = convertPixelToLng((int) ((convertLngToPixel(extents[2]) + convertLngToPixel(extents[0])) / 2.0));
                double latitude = convertPixelToLat((int) ((convertLatToPixel(extents[3]) + convertLatToPixel(extents[1])) / 2.0));
                double[] qlong = new double[4];
                double[] qlat = new double[4];
                double[] qradius = new double[4];
                for (int k = 0; k < 4; k++) {
                    qlong[k] = longitude;
                    qlat[k] = latitude;
                }
                for (int k = 0; k < cluster.size(); k++) {
                    Record o2 = (Record) cluster.get(k);
                    double dist = planeDistance(latitude, longitude, o2.getLatitude(), o2.getLongitude(), current_zoom);
                    int q = ((latitude < o2.getLatitude()) ? 1 : 0)
                            + ((longitude < o2.getLongitude()) ? 2 : 0);
                    if (dist > qradius[q]) {
                        qlong[q] = o2.getLongitude();
                        qlat[q] = o2.getLatitude();
                        qradius[q] = dist;
                    }
                }

                //get centre of circle for these 4 points

                //find largest distance between the quarter points found
                int maxk = 2, maxj = 3;
                double maxdist = planeDistance(qlat[2], qlong[2], qlat[3], qlong[3], current_zoom);
                for (int k = 0; k < 2; k++) {
                    for (int j = k + 1; j < 4; j++) {
                        double dist = planeDistance(qlat[k], qlong[k], qlat[j], qlong[j], current_zoom);
                        if (dist > maxdist) {
                            maxk = k;
                            maxj = j;
                            maxdist = dist;
                        }
                    }
                }

                int[] large = new int[2];
                large[0] = maxk;
                large[1] = maxj;

                int[] small = new int[2];
                int p = 0;
                for (int k = 0; k < 4; k++) {
                    if (large[0] != k && large[1] != k) {
                        small[p] = k;
                        p++;
                    }
                }
                double largedist = maxdist;

                //furthest point from middle
                longitude = convertPixelToLng((int) ((convertLngToPixel(qlong[large[0]]) + convertLngToPixel(qlong[large[1]])) / 2.0));
                latitude = convertPixelToLat((int) ((convertLatToPixel(qlat[large[0]]) + convertLatToPixel(qlat[large[1]])) / 2.0));
                int r0 = planeDistance(qlat[small[0]], qlong[small[0]], latitude, longitude, current_zoom);
                int r1 = planeDistance(qlat[small[1]], qlong[small[1]], latitude, longitude, current_zoom);
                int rlarge;

                //assign a centre
                global_centroids[i][0] = longitude;
                global_centroids[i][1] = latitude;
                if (r0 < largedist / 2 && r1 < largedist / 2) {
                    //is this one ok?
                    continue;
                } else if (r0 > r1) {
                    rlarge = small[0];
                } else {
                    rlarge = small[1];
                }

                //centre from large[0], large[1] and rlarge
                long[] ilong = new long[4];
                long[] ilat = new long[4];
                for (int k = 0; k < 4; k++) {
                    ilong[k] = convertLngToPixel(qlong[k]);
                    ilat[k] = convertLatToPixel(qlat[k]);
                }
                int a = large[0], b = large[1], c = rlarge;

                try {
                    //line ab and tangent at midpoint
                    double m1 = (ilat[a] - ilat[b]) / (double) (ilong[a] - ilong[b]);
                    //double b1 = ilat[a] - m1 * ilong[a];
                    double t1x = (ilong[a] + ilong[b]) / 2.0;
                    double t1y = (ilat[a] + ilat[b]) / 2.0;
                    double m2 = -1 / m1;
                    double b2 = t1y - m2 * t1x;

                    //line ac and tangent at midpoint
                    double m3 = (ilat[a] - ilat[c]) / (double) (ilong[a] - ilong[c]);
                    //double b3 = ilat[a] - m3 * ilong[a];
                    double t2x = (ilong[a] + ilong[c]) / 2.0;
                    double t2y = (ilat[a] + ilat[c]) / 2.0;
                    double m4 = -1 / m3;
                    double b4 = t2y - m4 * t2x;

                    //intercept
                    double cx = (b4 - b2) / (m2 - m4);
                    double cy = m2 * cx + b2;

                    //test if within extents
                    longitude = convertPixelToLng((int) cx);
                    latitude = convertPixelToLat((int) cy);
                    if (longitude <= extents[2] && longitude >= extents[0]
                            && latitude <= extents[3] && latitude >= extents[1]) {
                        global_centroids[i][0] = longitude;
                        global_centroids[i][1] = latitude;
                    }//else already assigned
                } catch (Exception e) {
                    //centre already assigned
                }
            }
        }
    }

    void makeRadius() {
        global_radius = new double[global_clusters.size()];
        global_uncertainty = new double[global_clusters.size()];
        for (int i = 0; i < global_clusters.size(); i++) {
            Vector cluster = (Vector) global_clusters.get(i);

            //get radius
            double radius = 0;
            double uncertainty = -1;
            double u = 0;
            for (Object o : cluster) {
                double dist = planeDistance(global_centroids[i][1], global_centroids[i][0], ((Record) o).getLatitude(), ((Record) o).getLongitude(), current_zoom);
                if (dist > radius) {
                    radius = dist;
                }
                try {
                    u = (((Record) o).getUncertainity());
                    if (u == Integer.MIN_VALUE) {
                        u = 10000;
                    }
                } catch (Exception e) {
                    u = 10000;
                }
                if (u <= 0) {
                    u = 10000;
                }
                u = dist + convertMetersToPixels(u, ((Record) o).getLatitude(), current_zoom) + 1; //adjustment trunc in planeDistance
                if (u > uncertainty) {
                    uncertainty = u;
                }
            }
            if (radius < current_min_distance) {
                radius = current_min_distance;
            }
            if (uncertainty < radius) {
                uncertainty = radius;
            }

            global_radius[i] = radius;
            global_uncertainty[i] = convertPixelsToMeters((int) Math.ceil(uncertainty + 1), global_centroids[i][1], current_zoom);//Math.max(m1,m2);;
        }
    }

    void makeDensity() {
        double max_density = Double.MAX_VALUE * -1;
        double min_density = Double.MAX_VALUE;
        global_density = new double[global_clusters.size()];
        for (int i = 0; i < global_clusters.size(); i++) {
            Vector v = (Vector) global_clusters.get(i);
            double radius = global_radius[i];
            double density = (v.size() / (Math.PI * radius * radius));
            if (max_density < density) {
                max_density = density;
            }
            if (min_density > density) {
                min_density = density;
            }
            global_density[i] = density;
        }
        double range_density = max_density - min_density;

        if (range_density == 0) {
            //single value, 1
            for (int i = 0; i < global_density.length; i++) {
                global_density[i] = 1;
            }
        } else {
            //scale between 0 and 1, linear
            for (int i = 0; i < global_density.length; i++) {
                global_density[i] = (global_density[i] - min_density) / range_density;
            }
        }
    }

    public static void main(String[] args) {
        try {
            Vector<Record> dataPoints = new Vector();
//            dataPoints.add(new Record("1", "1", 24.729494, 59.441193));
//            dataPoints.add(new Record("2", "2", 24.742992, 59.432365));
//            dataPoints.add(new Record("3", "3", 24.757563, 59.431602));
//            dataPoints.add(new Record("4", "4", 24.765759, 59.437843));
//            dataPoints.add(new Record("5", "5", 24.779041, 59.439644));
//            dataPoints.add(new Record("6", "6", 24.756681, 59.434776));
            //dataPoints.add(new Record("", "", ));

            dataPoints.add(new Record(192091618, "Acacia abrupta", 129.251, -24.8819, 10));
            dataPoints.add(new Record(192091619, "Acacia abrupta", 129.235, -24.8819, 10));
            dataPoints.add(new Record(192091620, "Acacia abrupta", 129.318, -22.5819, 10));
            dataPoints.add(new Record(192091621, "Acacia abrupta", 131.641, -24.2806, 10));
            dataPoints.add(new Record(192091622, "Acacia abrupta", 129.798, -22.9586, 10));
            dataPoints.add(new Record(192091623, "Acacia abrupta", 132.317, -24.5333, 10));
            dataPoints.add(new Record(192091624, "Acacia abrupta", 131.585, -24.2819, 10));
            dataPoints.add(new Record(192091625, "Acacia abrupta", 129.051, -24.8486, 10));
            dataPoints.add(new Record(192091626, "Acacia abrupta", 129.285, -24.7153, 10));
            dataPoints.add(new Record(192091627, "Acacia abrupta", 131.353, -25.2163, 10));
            dataPoints.add(new Record(192091628, "Acacia abrupta", 130.151, -23.7908, 10));
            dataPoints.add(new Record(192091629, "Acacia abrupta", 129.018, -24.6486, 10));
            dataPoints.add(new Record(192091630, "Acacia abrupta", 129.585, -24.6986, 10));
            dataPoints.add(new Record(192091631, "Acacia abrupta", 127.918, -25.4986, 10));
            dataPoints.add(new Record(192091632, "Acacia abrupta", 130.951, -23.7319, 10));
            dataPoints.add(new Record(192091633, "Acacia abrupta", 129.268, -24.9153, 10));
            dataPoints.add(new Record(192091634, "Acacia abrupta", 131.185, -25.3486, 10));
            dataPoints.add(new Record(192091635, "Acacia abrupta", 131.635, -24.2819, 10));
            dataPoints.add(new Record(192091636, "Acacia abrupta", 129.249, -24.8844, 10));
            dataPoints.add(new Record(192091637, "Acacia abrupta", 119.353, -26.6, 10));
            dataPoints.add(new Record(192091638, "Acacia abrupta", 130.551, -25.0986, 10));
            dataPoints.add(new Record(192091639, "Acacia abrupta", 130.135, -25.0819, 10));
            dataPoints.add(new Record(192091640, "Acacia abrupta", 131.318, -23.8319, 10));
            dataPoints.add(new Record(192091641, "Acacia abrupta", 129.268, -24.8819, 10));
            dataPoints.add(new Record(192091642, "Acacia abrupta", 130.101, -25.0819, 10));
            dataPoints.add(new Record(192091643, "Acacia abrupta", 130.168, -24.5986, 10));
            dataPoints.add(new Record(192091644, "Acacia abrupta", 130.101, -25.0653, 10));
            dataPoints.add(new Record(192091645, "Acacia abrupta", 125.635, -26.4487, 10));
            dataPoints.add(new Record(192091646, "Acacia abrupta", 129.251, -24.8986, 10));
            dataPoints.add(new Record(192091647, "Acacia abrupta", 128.201, -24.9319, 10));
            dataPoints.add(new Record(192091648, "Acacia abrupta", 130.803, -24.1192, 10));
            dataPoints.add(new Record(192091649, "Acacia abrupta", 128.911, -23.8942, 10));
            dataPoints.add(new Record(192091650, "Acacia abrupta", 131.626, -24.3015, 10));
            dataPoints.add(new Record(192091651, "Acacia abrupta", 129.289, -24.4514, 10));
            dataPoints.add(new Record(192091652, "Acacia abrupta", 127.583, -24.7667, 10));




            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, 20, 4, 4);
            Vector allFeatures = new Vector();
            for (int i = 0; i < clustered.size(); i++) {
                System.out.println(i + "> " + clustered.get(i).toString());
                Vector<Record> cluster = clustered.get(i);
                Record r = cluster.get(0);
                Hashtable geometry = new Hashtable();
                geometry.put("type", "Point");
                double[] coords = {r.getLongitude(), r.getLatitude()};
                geometry.put("coordinates", coords);
                Map cFeature = new HashMap();
                cFeature.put("type", "Feature"); // feature.getType().getName().toString()
                cFeature.put("id", "occurrences." + i + 1);
                cFeature.put("properties", cluster);
                cFeature.put("geometry_name", "the_geom");
                cFeature.put("geometry", geometry);
                allFeatures.add(cFeature);
            }
            System.out.println("returning allFeatures:" + allFeatures.toArray());
            System.out.println("===========================");
            ObjectMapper mapper = new ObjectMapper();
            Hashtable data = new Hashtable();
            data.put("type", "FeatureCollection");
            data.put("features", allFeatures);
            mapper.writeValue(System.out, data);
            System.out.println("\n===========================");
            //System.out.println(clustered.toString());
        } catch (Exception ex) {
            Logger.getLogger(SpatialCluster3.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private double[] getExtents(Vector<Record> datapoints) {
        double[] extents = new double[4]; //long1, lat1, long2, lat2
        extents[0] = datapoints.get(0).getLongitude();
        extents[2] = extents[0];
        extents[1] = datapoints.get(0).getLatitude();
        extents[3] = extents[1];

        for (Record r : datapoints) {
            if (extents[0] > r.getLongitude()) {
                extents[0] = r.getLongitude();
            }
            if (extents[2] < r.getLongitude()) {
                extents[2] = r.getLongitude();
            }
            if (extents[1] > r.getLatitude()) {
                extents[1] = r.getLatitude();
            }
            if (extents[3] < r.getLatitude()) {
                extents[3] = r.getLatitude();
            }
        }

        return extents;
    }
}
