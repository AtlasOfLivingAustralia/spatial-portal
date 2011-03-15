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
    public GridFileLegends(String filename, String output_name) {
        Grid g = Grid.getGrid(TabulationSettings.environmental_data_path + filename);
        float[] d = g.getGrid();

        float[] dsorted = d.clone();
        java.util.Arrays.sort(dsorted);

//        Legend[] legends = new Legend[4];
//        legends[3] = new LegendEqualSize();
//        legends[1] = new LegendEvenInterval();
//        legends[2] = new LegendEvenIntervalLog();
//        legends[0] = new LegendEvenIntervalLog10();
        Legend [] legends = new Legend[1];
        legends[0] = new LegendEqualSize();

        System.out.println(output_name);
        for (int i = 0; i < legends.length; i++) {
            legends[i].generate(dsorted);
            legends[i].determineGroupSizes(dsorted);
            double e2 = legends[i].evaluateStdDev(dsorted);
            //System.out.print(legends[i].getCutoffs());
            try {
                (new File(output_name + "_" + legends[i].getTypeName() + ".jpg")).delete();
            } catch (Exception e) {}
            legends[i].exportImage(d, g.ncols, output_name + "_" + legends[i].getTypeName() + ".jpg", 8);
            legends[i].exportLegend(output_name + "_" + legends[i].getTypeName() + "_legend.txt");
            //System.out.println("stddev: " + e2);
        }
    }

    static public void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Generates thumbnail image for environmental layer files.");
            System.out.println("parameters: output_directory");
            return;
        }
        TabulationSettings.load();

        String dir = args[0];

        //produce legend for each environmental file
        for(int i=0;i<TabulationSettings.environmental_data_files.length;i++) {
            new GridFileLegends(
                TabulationSettings.environmental_data_files[i].name,
                dir + TabulationSettings.environmental_data_files[i].name);
        }

//        String [] layer = {"add_nrm"};
//        for(int i=0;i<layer.length;i++) {
//        new GridFileLegends(
//        layer[i],
//        dir + layer[i]);
//        }
    }
}
