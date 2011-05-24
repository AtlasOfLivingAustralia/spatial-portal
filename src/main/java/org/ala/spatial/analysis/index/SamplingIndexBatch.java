package org.ala.spatial.analysis.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * builder for sampling index.
 *
 * requires OccurrencesIndex to be up to date.
 *
 * operates on GridFiles
 * operates on Shape Files
 *
 * @author adam
 *
 */
public class SamplingIndexBatch {

    /**
     * prefix for continous/environmental sampling index files
     */
    static public final String CONTINOUS_PREFIX = "SAM_D_";
    /**
     * prefix for catagorical/contextual sampling index files
     */
    static public final String CATAGORICAL_PREFIX = "SAM_I_";
    /**
     * postfix for sampling index data files
     */
    static public final String VALUE_POSTFIX = ".dat";
    /**
     * prefix for catagorical/contextual sampling index files values
     * lists
     */
    static public final String CATAGORY_LIST_PREFIX = "SAM_C_";
    /**
     * postfix for catagorical/contextual sampling index files values
     * lists
     */
    static public final String CATAGORY_LIST_POSTFIX = ".csv";

    /**
     * all points
     */
    double[][] points = null;
    String index_path;
    String [] ids;

    SamplingIndexBatch(String directoryName, double[][] p, String [] ids) {
        index_path = directoryName;
        points = p;
        this.ids = ids;
    }
    /**
     * performs update of 'indexing' for a new layer (grid or shapefile)
     *
     * @param layername name of the layer to update as String.  To update
     * all layers use null.
     */
    public void layersUpdate(String layername) {
        System.out.println("layersUpdate:" + layername);

        /*
         * for grid files
         */
        intersectGrid(layername);

        /*
         * for shape files of catagorical layers,
         * shape files instead of grids
         */
        intersectCatagories(layername);
    }

    /**
     * joins sorted points to GridFiles
     *
     * TODO: load groups of whole rasters at a time
     */
    void intersectGrid(String layername) {
        int i;

        /* for each grid file intersect and export in points order */
        Layer layer;

        for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            if (layername != null
                    && !layername.equalsIgnoreCase(TabulationSettings.environmental_data_files[i].name)) {
                continue;
            }

            layer = TabulationSettings.environmental_data_files[i];
            try {
                Grid grid = new Grid(
                        TabulationSettings.environmental_data_path
                        + layer.name);

                float[] values = grid.getValues2(points);

                /* export values */
                FileWriter fw = new FileWriter(index_path + layer.name + ".csv");

                for (i = 0; i < values.length; i++) {
                    if(Float.isNaN(values[i])) {
                        fw.append(ids[i]).append(",\n");
                    } else {
                        fw.append(ids[i]).append(",").append(String.valueOf(values[i])).append("\n");
                    }
                }

                fw.close();

            } catch (Exception e) {
                SpatialLogger.log("intersectGrid writing", e.toString());
            }
        }

    }

    /**
     * join number for each catagory name with all points
     *
     * export
     */
    void intersectCatagories(String layername) {
        String tablename;
        String fieldname;

        int i = 0;

        /*
         * TODO: fix assumption that only catagorical fields are specified
         * in the tables
         */
        for (Layer l : TabulationSettings.geo_tables) {
            if (layername != null
                    && !layername.equalsIgnoreCase(l.name)) {
                continue;
            }

            String query = "";
            String longitude = "";
            String latitude = "";
            i = 0;
            try {
                tablename = l.name;

                SimpleShapeFile ssf;
                //attempt to load pre-indexed shape file
                File preBuiltShapeFile = new File(TabulationSettings.index_path + l.name);
                if (preBuiltShapeFile.exists()) {
                    ssf = new SimpleShapeFile(TabulationSettings.index_path + l.name);
                } else {
                    ssf = new SimpleShapeFile(TabulationSettings.environmental_data_path + l.name);
                }

                SpatialLogger.log("shapefile open: " + l.name);

                /* export catagories
                 * TODO: operate on more than one field and remove assumption
                 * that there is one
                 */
                fieldname = l.fields[0].name;

                String[] catagories;
                int column_idx = 0;
                if (!preBuiltShapeFile.exists()) {
                    column_idx = ssf.getColumnIdx(fieldname);

                    /* column not found, log and substitute first column */
                    if (column_idx < 0) {
                        SpatialLogger.log("intersectCatagories, missing col:" + fieldname + " in " + l.name);
                    }

                    catagories = ssf.getColumnLookup(column_idx);
                } else {
                    catagories = ssf.getColumnLookup();
                }

                //repeat for each point
                SpatialLogger.log("intersectCatagories, begin intersect: " + points.length);

                int[] values;
                if (!preBuiltShapeFile.exists()) {
                    values = ssf.intersect(points, catagories, column_idx);
                } else {
                    values = ssf.intersect(points, TabulationSettings.analysis_threads);
                }

                /* export values */
                FileWriter fw = new FileWriter(index_path + tablename + ".csv");

                for (i = 0; i < values.length; i++) {
                    if(values[i] >= 0) {
                        fw.append(ids[i]==null?"":ids[i]).append(",").append(catagories[values[i]]==null?"":catagories[values[i]]).append("\n");
                    } else {
                        fw.append(ids[i]==null?"":ids[i]).append(",\n");
                    }
                }

                fw.close();

                SpatialLogger.log("shapefile done: " + l.name);
            } catch (Exception e) {
                SpatialLogger.log("intersectCatagories",
                        e.toString() + "\r\n>query=" + query + "\r\n>i=" + i
                        + "\r\n>longitude, latitude=" + longitude + "," + latitude);
                e.printStackTrace();
            }
        }
    }
    /**
     * args[0] = points.csv input (id, longitude, latitude)
     * args[1] = layer names comma separated (layer1,layer2,layer3) or (all)
     * args[2] = output directory
     *
     * @param args
     */
    public static void main(String [] args) {
        System.out.println("args[0] = points.csv input (id, longitude, latitude)\n"
            + "args[1] = layer names comma separated (layer1,layer2,layer3) or (all)\n"
            + "args[2] = output directory");

        //load points and ids
        double [][] points = loadPoints(args[0]);
        String [] ids = loadIds(args[0]);

        //layer names
        String layersList = "," + args[1] + ",";

        TabulationSettings.load();

        //output path
        String output_path = args[2];

        int threadcount = TabulationSettings.analysis_threads;
        ArrayList<String> layers = new ArrayList();
        int i;
        for (i = 0; i < TabulationSettings.geo_tables.length; i++) {
            if (layersList.contains(",all,") || layersList.contains("," + TabulationSettings.geo_tables[i].name + ",")) {
                layers.add(TabulationSettings.geo_tables[i].name);
            }
        }
        for (i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
            if (layersList.contains(",all,") || layersList.contains("," + TabulationSettings.environmental_data_files[i].name + ",")) {
                layers.add(TabulationSettings.environmental_data_files[i].name);
            }
        }

        if (layers.size() == 0) {
            return;
        }

        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue(layers);

        SamplingIndexBatchThread[] it = new SamplingIndexBatchThread[threadcount];

        for (i = 0; i < threadcount; i++) {
            it[i] = new SamplingIndexBatchThread(lbq, output_path, points, ids);
        }

        System.out.println("Start SamplingIndexBatch.build_all (" + threadcount + " threads): " + System.currentTimeMillis());

        //wait until all done
        try {
            boolean alive = true;
            while (alive) {
                Thread.sleep(2000);
                alive = false;
                for (i = 0; i < threadcount; i++) {
                    if (it[i].isAlive()) {
                        alive = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("End SamplingIndex.build_all: " + System.currentTimeMillis());
    }


    private static double[][] loadPoints(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            ArrayList<Double> data = new ArrayList<Double>();

            String line;
            while((line = br.readLine()) != null) {
                double a = Double.NaN, b = Double.NaN;
                String [] cells = line.split(",");
                try {
                    a = Double.parseDouble(cells[1]);
                    b = Double.parseDouble(cells[0]);
                } catch (Exception e) {
                    //...
                }
                data.add(a);
                data.add(b);
            }

            double [][] points = new double[data.size() / 2][2];
            for(int i=0;i<data.size();i+=2) {
                points[i/2][0] = data.get(i);
                points[i/2][1] = data.get(i+1);
            }

            //fix range
            for(int i=0;i<points.length;i++) {
                if (points[i][0] < -360 || points[i][0] > 360) {
                    points[i][0] = Double.NaN;
                } else if (points[i][0] < -180) {
                    points[i][0] += 360;
                } else if (points[i][0] >= 180) {
                    points[i][0] -= 360;
                }

                if (points[i][1] < -90 || points[i][1] > 90) {
                    points[i][1] = Double.NaN;
                }
            }

            return points;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String [] loadIds(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));

            ArrayList<String> data = new ArrayList<String>();

            String line;
            while((line = br.readLine()) != null) {
                String s = "";
//                String [] cells = line.split(",");
//                try {
//                    s = cells[0];
//                } catch (Exception e) {
//                    //...
//                }
                s = line;
                data.add(s);
            }

            String [] ss = new String[data.size()];
            data.toArray(ss);

            return ss;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
