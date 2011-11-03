package org.ala.spatial.analysis.layers;

import java.io.File;

/**
 *
 * @author Adam
 */
public class DensityLayers {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        args = new String[] {
//            //"http://localhost:8083/biocache-service",
//            "http://biocache.ala.org.au/ws",
//            "e:\\grid_ala",
//            "9",
//            "-180,-90,180,90",
//            //"112,-42,155,-9",
//            "0.5",
//            "1"
//        };

        if (args.length < 3) {
            printUsage();
            return;
        }

        try {
            int gridSize = Integer.parseInt(args[2]);
            double resolution = Double.parseDouble(args[4]);
            int maxThreads = Integer.parseInt(args[5]);
            
            String [] sbbox = args[3].split(",");
            double [] bbox = new double[4];
            bbox[0] = Double.parseDouble(sbbox[0]);
            bbox[1] = Double.parseDouble(sbbox[1]);
            bbox[2] = Double.parseDouble(sbbox[2]);
            bbox[3] = Double.parseDouble(sbbox[3]);

            String recordsFile = "d:\\records_saved.csv";
            Records records = null;
            if(new File(recordsFile).exists()) {
                records = new Records(recordsFile);
            } else {
                records = new Records(args[0], "*:*",bbox,recordsFile);
            }

//            OccurrenceDensity occurrenceDensity = new OccurrenceDensity(gridSize, resolution, bbox);
//            occurrenceDensity.write(records, args[1]);
//
//            occurrenceDensity.setResolution(0.01);
//            occurrenceDensity.write(records, args[1]);

            SpeciesDensity speciesDensity = new SpeciesDensity(gridSize, resolution, bbox);
            speciesDensity.write(records, args[1]);

            speciesDensity.setResolution(0.01);
            speciesDensity.write(records, args[1]);

            OccurrenceDensity occurrenceDensity2 = new OccurrenceDensity(gridSize, resolution, bbox);
            occurrenceDensity2.write(records, args[1]);

            occurrenceDensity2.setResolution(0.01);
            occurrenceDensity2.write(records, args[1]);

        } catch (Exception e) {
            printUsage();
            e.printStackTrace();
        }
    }

    static void printUsage() {
        System.out.println("args[0] = biocache-service url, "
                + "args[1] = output prefix, "
                + "args[2] = number of grids for average.  e.g. 9 for 9x9), "
                + "args[3] = extents, e.g. -180,-90,180,90"
                + "args[4] = grid resolution, e.g. 0.5"
                + "args[5] = max threads, e.g. 1");
    }
}

