package org.ala.layers.legend;


import java.io.File;
import java.io.FileWriter;

import org.ala.layers.intersect.Grid;

public class GridLegend {

    /**
     * @param filename    grid file name.  must reside in
     *                    tabulation settings <environmental_data_path> as String
     * @param output_name Base output file path and name as String
     */
    public GridLegend(String filename, String output_name, boolean useAreaEvaluation, String[] legendNames, FileWriter cutpointFile, int scaleDown, boolean minAsTransparent) {
        Grid g = new Grid(filename);
        float[] d = g.getGrid();

        if (legendNames != null) {
            java.util.Arrays.sort(legendNames);
        }

        java.util.Arrays.sort(d);

        Legend[] legends = new Legend[1];
        legends[0] = new LegendEqualArea();
//        legends[3] = new LegendEqualSize();
//        legends[1] = new LegendEvenInterval();
//        legends[2] = new LegendEvenIntervalLog();
//        legends[4] = new LegendEvenIntervalLog10();

        int minI = 0;
        double minE = 0;
        boolean firstTime = true;
        for (int i = 0; i < legends.length; i++) {
            if (legendNames == null || java.util.Arrays.binarySearch(legendNames, legends[i].getTypeName()) < 0) {
                continue;
            }
            legends[i].generate(d);
            legends[i].determineGroupSizes(d);
            double e2 = 0;
            if (useAreaEvaluation) {
                e2 = legends[i].evaluateStdDevArea(d);
            } else {
                e2 = legends[i].evaluateStdDev(d);
            }
            try {
                (new File(output_name + /*"_" + legends[i].getTypeName().replace(" ","_") +*/ ".png")).delete();
            } catch (Exception e) {
            }

            //must 'unsort' d
            d = null;
            g = null;
            System.gc();
            g = new Grid(filename);
            d = g.getGrid();
            legends[i].exportImage(d, g.ncols, output_name + /*"_" + legends[i].getTypeName().replace(" ","_") +*/ ".png", scaleDown, minAsTransparent);
            legends[i].exportLegend(output_name + /*"_" + legends[i].getTypeName().replace(" ","_") +*/ "_legend.txt");

            legends[i].exportSLD(g, output_name + /*"_" + legends[i].getTypeName().replace(" ","_") +*/ ".sld", g.units, true, minAsTransparent);

            System.out.println(output_name + ", " + legends[i].getTypeName() + ": " + String.valueOf(e2));
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
    }

    static public void main(String[] args) {

        if (args.length == 0) {
            System.out.println("args[0]=grid file without .grd or .gri\n"
                    + "args[1]=output prefix for +_cutpoints.csv +.jpg +_legend.txt\n"
                    + "args[2]=thumbnail scale down factor (optional) e.g. 1 (default), 2, 4, 8, 16 (16x16 times smaller)\n"
                    + "args[3]=min as transparent (optional) e.g. 0=false (default), 1=true");
            return;
        }

        String gridfilename = args[0];
        String outputfilename = null;
        if (args.length < 2) {
            outputfilename = args[0];
        } else {
            outputfilename = args[1];
        }

        int scaleDown = 1;
        if (args.length >= 3) {
            scaleDown = Integer.parseInt(args[2]);
        }
        boolean minAsTransparent = false;
        if (args.length >= 4) {
            minAsTransparent = args[3].equals("1");
        }

        generateGridLegend(gridfilename, outputfilename, scaleDown, minAsTransparent);
    }

    public static boolean generateGridLegend(String gridfilename, String outputfilename, int scaleDown, boolean minAsTransparent) {
        boolean ret = true;

        String[] legendTypes = {"Equal Area"};
        //String [] legendTypes = {"Even Interval","Even Interval Log 10","Equal Size","Equal Area"};

        try {
            FileWriter fw = new FileWriter(outputfilename + "_cutpoints.csv");

            new GridLegend(
                    gridfilename,
                    outputfilename,
                    true,
                    legendTypes, fw, scaleDown, minAsTransparent);

            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }
}

