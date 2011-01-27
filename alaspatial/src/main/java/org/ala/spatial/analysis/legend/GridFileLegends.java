/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.legend;

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

        Legend[] legends = new Legend[2];
        legends[0] = new LegendEqualSize();
        legends[1] = new LegendEvenInterval();

        for (int i = 0; i < legends.length; i++) {
            legends[i].generate(dsorted);
            legends[i].determineGroupSizes(dsorted);
            double e2 = legends[i].evaluateStdDev(dsorted);
            System.out.print(legends[i].getCutoffs());
            legends[i].exportImage(d, g.ncols, output_name + "_" + legends[i].getTypeName() + ".png");
            legends[i].exportLegend(output_name + "_" + legends[i].getTypeName() + "_legend.txt");
            System.out.println("stddev: " + e2);
        }
    }

    static public void main(String[] args) {
        TabulationSettings.load();

        String dir = "d:\\";

        /*//produce legend for each environmental file
        for(int i=0;i<TabulationSettings.environmental_data_files.length;i++) {
        new GridFileLegends(
        TabulationSettings.environmental_data_files[i].name,
        dir + TabulationSettings.environmental_data_files[i].display_name);
        }*/

        //specific environmental files
        new GridFileLegends("slope_length", dir + "SlopeLength");
        new GridFileLegends("substrate_mrrtf", dir + "RidgeTopFlatness");
        new GridFileLegends("aspect", dir + "Aspect");
        new GridFileLegends("substrate_mrvbf", dir + "Valley bottom flatness");
        new GridFileLegends("substrate_roughness", dir + "Topographic roughness");
        new GridFileLegends("substrate_relief", dir + "Topographic relief");

    }
}
