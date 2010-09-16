/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * @author ajay
 */
public class SpatialCluster3 {

    private int map_zoom = 21;
    private int map_offset = 268435456; // half the Earth's circumference at zoom level 21
    private double map_radius = map_offset / Math.PI;

    public int convertLngToPixel(double lng) {
        return (int) Math.round(map_offset + map_radius * lng * Math.PI / 180);
    }

    public int convertLatToPixel(double lat) {
        return (int) Math.round(map_offset - map_radius
                * Math.log((1 + Math.sin(lat * Math.PI / 180))
                / (1 - Math.sin(lat * Math.PI / 180))) / 2);
    }

    private int planeDistance(double lat1, double lng1, double lat2, double lng2, int zoom) {
        // Given a pair of lat/long coordinates and a map zoom level, returns
        // the distance between the two points in pixels

        int x1 = convertLngToPixel(lng1);
        int y1 = convertLatToPixel(lat1);

        int x2 = convertLngToPixel(lng2);
        int y2 = convertLatToPixel(lat2);

        //int distance = (int) (Math.sqrt(Math.pow((x1 - x2), 2)) + Math.sqrt(Math.pow((y1 - y2), 2)));
        int distance = (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

        //System.out.println(x1 + ", " + y1 + " -> " + x2 + ", " + y2);
        //System.out.println("distance: " + distance + " >> " + (distance >> (map_zoom - zoom)));

        return distance >> (map_zoom - zoom);

    }

    public Vector cluster(Vector<Record> datapoints, int cluster_distance, int zoom) {

        // Groups points that are less than cluster_distance pixels apart at
        // a given zoom level into a cluster.

        Vector<Vector> clusters = new Vector();
        Vector<Record> removed = new Vector();

        //System.out.println("Clustering " + datapoints.size() + " records. ");

        for (int i = datapoints.size() - 1; i > -1; i--) {
            Record r = (Record) datapoints.get(i);

            //System.out.println("Checking r: " + r.getId());

            if (removed.contains(r)) {
                //System.out.println("   skipping: " + r.getId());
                continue;
            }

            Vector<Record> cluster = new Vector();
            for (int j = 0; j < datapoints.size(); j++) {
                Record r2 = (Record) datapoints.get(j);
                //System.out.println("Checking with r2: " + r2.getId());
                if (removed.contains(r2) || r == r2) {
                    //System.out.println("   skipping: " + r2.getId());
                    continue;
                }

                int pixel_distance = planeDistance(r.getLatitude(), r.getLongitude(), r2.getLatitude(), r2.getLongitude(), zoom);

                /* If two markers are closer than given distance remove */
                /* target marker from array and add it to cluster.          */
                if (pixel_distance < cluster_distance) {
                    //System.out.print("++++ Distance between " + r.getLongitude() + ", " + r.getLatitude());
                    //System.out.print("  and " + r2.getLongitude() + ", " + r2.getLatitude());
                    //System.out.println("  is " + pixel_distance);
                    cluster.add(r2);
                    //datapoints.remove(r2);
                    removed.add(r2);
                } else {
                    //System.out.print("---- Distance between " + r.getLongitude() + ", " + r.getLatitude());
                    //System.out.print("  and " + r2.getLongitude() + ", " + r2.getLatitude());
                    //System.out.println("  is " + pixel_distance);
                }
            }

            // add the first point to the cluster
            /* If a marker has been added to cluster, add also the one  */
            /* we were comparing to and remove the original from array. */
            cluster.add(r);
            clusters.add(cluster);
        }

        return clusters;
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

            dataPoints.add(new Record("192091618", "Acacia abrupta", 129.251, -24.8819));
            dataPoints.add(new Record("192091619", "Acacia abrupta", 129.235, -24.8819));
            dataPoints.add(new Record("192091620", "Acacia abrupta", 129.318, -22.5819));
            dataPoints.add(new Record("192091621", "Acacia abrupta", 131.641, -24.2806));
            dataPoints.add(new Record("192091622", "Acacia abrupta", 129.798, -22.9586));
            dataPoints.add(new Record("192091623", "Acacia abrupta", 132.317, -24.5333));
            dataPoints.add(new Record("192091624", "Acacia abrupta", 131.585, -24.2819));
            dataPoints.add(new Record("192091625", "Acacia abrupta", 129.051, -24.8486));
            dataPoints.add(new Record("192091626", "Acacia abrupta", 129.285, -24.7153));
            dataPoints.add(new Record("192091627", "Acacia abrupta", 131.353, -25.2163));
            dataPoints.add(new Record("192091628", "Acacia abrupta", 130.151, -23.7908));
            dataPoints.add(new Record("192091629", "Acacia abrupta", 129.018, -24.6486));
            dataPoints.add(new Record("192091630", "Acacia abrupta", 129.585, -24.6986));
            dataPoints.add(new Record("192091631", "Acacia abrupta", 127.918, -25.4986));
            dataPoints.add(new Record("192091632", "Acacia abrupta", 130.951, -23.7319));
            dataPoints.add(new Record("192091633", "Acacia abrupta", 129.268, -24.9153));
            dataPoints.add(new Record("192091634", "Acacia abrupta", 131.185, -25.3486));
            dataPoints.add(new Record("192091635", "Acacia abrupta", 131.635, -24.2819));
            dataPoints.add(new Record("192091636", "Acacia abrupta", 129.249, -24.8844));
            dataPoints.add(new Record("192091637", "Acacia abrupta", 119.353, -26.6));
            dataPoints.add(new Record("192091638", "Acacia abrupta", 130.551, -25.0986));
            dataPoints.add(new Record("192091639", "Acacia abrupta", 130.135, -25.0819));
            dataPoints.add(new Record("192091640", "Acacia abrupta", 131.318, -23.8319));
            dataPoints.add(new Record("192091641", "Acacia abrupta", 129.268, -24.8819));
            dataPoints.add(new Record("192091642", "Acacia abrupta", 130.101, -25.0819));
            dataPoints.add(new Record("192091643", "Acacia abrupta", 130.168, -24.5986));
            dataPoints.add(new Record("192091644", "Acacia abrupta", 130.101, -25.0653));
            dataPoints.add(new Record("192091645", "Acacia abrupta", 125.635, -26.4487));
            dataPoints.add(new Record("192091646", "Acacia abrupta", 129.251, -24.8986));
            dataPoints.add(new Record("192091647", "Acacia abrupta", 128.201, -24.9319));
            dataPoints.add(new Record("192091648", "Acacia abrupta", 130.803, -24.1192));
            dataPoints.add(new Record("192091649", "Acacia abrupta", 128.911, -23.8942));
            dataPoints.add(new Record("192091650", "Acacia abrupta", 131.626, -24.3015));
            dataPoints.add(new Record("192091651", "Acacia abrupta", 129.289, -24.4514));
            dataPoints.add(new Record("192091652", "Acacia abrupta", 127.583, -24.7667));




            SpatialCluster3 scluster = new SpatialCluster3();
            Vector<Vector<Record>> clustered = scluster.cluster(dataPoints, 20, 4);
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
}
