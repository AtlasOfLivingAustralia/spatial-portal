/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.legend;

import java.io.File;
import java.io.FileWriter;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.TabulationSettings;

/**
 * Produces legend cutoff values for environmental layers
 * 
 * @author Adam
 */
public class GridFileLegends {

    /**
     *
     * @param filename grid file name.  must reside in
     *  tabulation settings <environmental_data_path> as String
     * @param output_name Base output file path and name as String
     */
    public GridFileLegends(String filename, String output_name, boolean useAreaEvaluation, String[] legendNames, FileWriter cutpointFile) {
        Grid g = Grid.getGrid(TabulationSettings.environmental_data_path + filename);
        float[] d = g.getGrid();

        if (legendNames != null) {
            java.util.Arrays.sort(legendNames);
        }

        float[] dsorted = d.clone();
        java.util.Arrays.sort(dsorted);

        Legend[] legends = new Legend[5];
        legends[4] = new LegendEqualArea();
        legends[3] = new LegendEqualSize();
        legends[1] = new LegendEvenInterval();
        legends[2] = new LegendEvenIntervalLog();
        legends[0] = new LegendEvenIntervalLog10();

        int minI = 0;
        double minE = 0;
        boolean firstTime = true;
        for (int i = 0; i < legends.length; i++) {
            if (legendNames == null || java.util.Arrays.binarySearch(legendNames, legends[i].getTypeName()) < 0) {
                continue;
            }
            legends[i].generate(dsorted);
            legends[i].determineGroupSizes(dsorted);
            double e2 = 0;
            if (useAreaEvaluation) {
                e2 = legends[i].evaluateStdDevArea(dsorted);
            } else {
                e2 = legends[i].evaluateStdDev(dsorted);
            }
            try {
                (new File(output_name + "_" + legends[i].getTypeName() + ".jpg")).delete();
            } catch (Exception e) {
            }
            legends[i].exportImage(d, g.ncols, output_name + "_" + legends[i].getTypeName() + ".jpg", 8, false);
            legends[i].exportLegend(output_name + "_" + legends[i].getTypeName() + "_legend.txt");

            System.out.println(output_name + "," + legends[i].getTypeName() + ": " + String.valueOf(e2));
            if (firstTime || e2 <= minE) {
                minE = e2;
                minI = i;
                firstTime = false;
            }
        }

        try {
            if (cutpointFile != null) {
                cutpointFile.append(filename).append(",").append(legends[minI].getTypeName());
                float[] minmax = legends[minI].getMinMax();
                float[] f = legends[minI].getCutoffFloats();

                cutpointFile.append(",min,").append(String.valueOf(minmax[0]));

                cutpointFile.append(",#cutpoints,").append(String.valueOf(f.length));

                cutpointFile.append(",cutpoints");
                for (int i = 0; i < f.length; i++) {
                    cutpointFile.append(",").append(String.valueOf(f[i]));
                }

                cutpointFile.append(",distribution");
                int[] a;
                if (useAreaEvaluation) {
                    a = legends[minI].groupSizesArea;
                } else {
                    a = legends[minI].groupSizes;
                }
                for (int i = 0; i < a.length; i++) {
                    cutpointFile.append(",").append(String.valueOf(a[i]));
                }

                cutpointFile.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(output_name + ",best=" + legends[minI].getTypeName());
    }

    static public void main(String[] args) {
        if (true || (args.length > 1 && args[0].equalsIgnoreCase("cutpoints"))) {
            TabulationSettings.load();

            String dir = "d:\\legends\\";//args[1];

            //String[] legendTypes = {"Equal Area"};
            String[] legendTypes = {"Even Interval", "Even Interval Log 10", "Equal Size", "Equal Area"};

            try {
                FileWriter fw = new FileWriter(dir + "cutpoints2.csv");

                //produce legend for each environmental file
                for (int i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
                    new GridFileLegends(
                            TabulationSettings.environmental_data_files[i].name,
                            dir + TabulationSettings.environmental_data_files[i].name,
                            true,
                            legendTypes, fw);
                }

                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

//            String[] names = {"worldclim_bio_9"};
//            for (String s : names) {
//                new GridFileLegends(
//                        s,
//                        dir + s,
//                        true,
//                        legendTypes, null);
//            }
        } else if ((args.length > 1 && args[0].equalsIgnoreCase("thumbnails"))) {
            TabulationSettings.load();

            String dir = "d:\\";//args[1];

            String[] legendTypes = {"Even Interval", "Even Interval Log 10"};
            //String [] legendTypes = {"Even Interval","Even Interval Log 10","Even Interval Log","Equal Size"};

            //produce legend for each environmental file
            for (int i = 0; i < TabulationSettings.environmental_data_files.length; i++) {
                new GridFileLegends(
                        TabulationSettings.environmental_data_files[i].name,
                        dir + TabulationSettings.environmental_data_files[i].name,
                        true,
                        legendTypes, null);
            }

//            String [] names = {"totp", "totn","totk","soil_carbon","slope","seifa","ph","Substrate_magnetics"};
//            for(String s : names) {
//                new GridFileLegends(
//                        s,
//                        dir + s,
//                        true,
//                        legendTypes, null);
//            }
        } else if (args.length > 1 && args[0].equalsIgnoreCase("thumbnails")) {
        } else {
            System.out.println("Generates thumbnail image for environmental layer files.");
            System.out.println("parameters: thumbnails output_directory");
        }
    }
}
