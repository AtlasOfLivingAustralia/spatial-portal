/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.legend;

import java.io.File;
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
    public GridFileLegends(String filename, String output_name, boolean useAreaEvaluation, String [] legendNames) {
        Grid g = Grid.getGrid(TabulationSettings.environmental_data_path + filename);
        float[] d = g.getGrid();

        if(legendNames != null) {
            java.util.Arrays.sort(legendNames);
        }

        float[] dsorted = d.clone();
        java.util.Arrays.sort(dsorted);

        Legend[] legends = new Legend[4];
        legends[3] = new LegendEqualSize();
        legends[1] = new LegendEvenInterval();
        legends[2] = new LegendEvenIntervalLog();
        legends[0] = new LegendEvenIntervalLog10();

        int minI = 0;
        double minE = 0;
        for (int i = 0; i < legends.length; i++) {
            if(legendNames == null || java.util.Arrays.binarySearch(legendNames, legends[i].getTypeName()) < 0) {
                continue;
            }
            legends[i].generate(dsorted);
            legends[i].determineGroupSizes(dsorted);
            double e2 = 0;
            if(useAreaEvaluation) {
                e2 = legends[i].evaluateStdDevArea(dsorted);
            } else {
                e2 = legends[i].evaluateStdDev(dsorted);
            }
            try {
                (new File(output_name + "_" + legends[i].getTypeName() + ".jpg")).delete();
            } catch (Exception e) {}
            legends[i].exportImage(d, g.ncols, output_name + "_" + legends[i].getTypeName() + ".jpg", 8);
            legends[i].exportLegend(output_name + "_" + legends[i].getTypeName() + "_legend.txt");

            System.out.println(output_name + "," + legends[i].getTypeName() + ": " + String.valueOf(e2));
            if(i == 0 || e2 < minE) {
                minE = e2;
                minI = i;
            }
        }

        System.out.println(output_name + ",best=" + legends[minI].getTypeName());
    }

    static public void main(String[] args) {
        if(true || (args.length > 1 && args[0].equalsIgnoreCase("thumbnails"))) {
            TabulationSettings.load();

            String dir = "d:\\";//args[1];

            String [] legendTypes = {"Even Interval","Even Interval Log 10"};
            //String [] legendTypes = {"Even Interval","Even Interval Log 10","Even Interval Log","Equal Size"};

            //produce legend for each environmental file
            for(int i=0;i<TabulationSettings.environmental_data_files.length;i++) {
                new GridFileLegends(
                    TabulationSettings.environmental_data_files[i].name,
                    dir + TabulationSettings.environmental_data_files[i].name,
                    true,
                    legendTypes);
            }
        } else if(args.length > 1 && args[0].equalsIgnoreCase("thumbnails")) {
            
        } else {
            System.out.println("Generates thumbnail image for environmental layer files.");
            System.out.println("parameters: thumbnails output_directory");
        }
    }
}
