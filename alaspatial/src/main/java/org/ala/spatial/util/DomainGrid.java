package org.ala.spatial.util;

/**
 * Generates a integer-based domain grid based on a set of grid values
 * 
 * 1 for positive intersecting values
 * -9999 for no intersecting values
 *
 * @author ajay
 */
public class DomainGrid {

    public static void generate(String envPath, Layer[] layers, SimpleRegion region, String outputdir) {

        TabulationSettings.load();

//        TabulationSettings.index_path = "/Users/ajay/Downloads/gdm/test/work/";
//        TabulationSettings.environmental_data_path = "/Users/ajay/Downloads/gdm/test/layers/";
//        TabulationSettings.grd_xmin = 112.900000;
//        TabulationSettings.grd_ymin = -43.800000;
//        TabulationSettings.grd_xmax = 153.640000;
//        TabulationSettings.grd_ymax = -9.000000;
//        TabulationSettings.grd_ncols = 4074;
//        TabulationSettings.grd_nrows = 3480;
//        TabulationSettings.grd_xdiv = 0.010000;
//        TabulationSettings.grd_ydiv = 0.010000;

//        //mkdir in index location
//        String newPath = null;
//        try {
//            newPath = TabulationSettings.index_path + System.currentTimeMillis() + java.io.File.separator;
//            File directory = new File(newPath);
//            directory.mkdir();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        /* get data, remove missing values, restrict by optional region */
        int i;
        int width;
        int height;
        int xmin;
        int ymin;
        int xmax;
        int ymax;
        String layerPath = envPath;
        if (layerPath == null) {
            layerPath = "";
        }
        Grid grid = null;

        //identify cells to keep
        int[][] cells = (int[][]) region.getAttribute("cells");
        if (cells == null) {
            cells = region.getOverlapGridCells(
                    TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                    TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                    TabulationSettings.grd_ncols, TabulationSettings.grd_nrows,
                    null);
        }

        //find minx, miny, width and height
        xmin = TabulationSettings.grd_ncols;
        ymin = TabulationSettings.grd_nrows;
        xmax = 0;
        ymax = 0;
        for (i = 0; i < cells.length; i++) {
            if (cells[i][0] < xmin) {
                xmin = cells[i][0];
            }
            if (cells[i][1] < ymin) {
                ymin = cells[i][1];
            }
            if (cells[i][0] > xmax) {
                xmax = cells[i][0];
            }
            if (cells[i][1] > ymax) {
                ymax = cells[i][1];
            }
        }

        width = xmax - xmin + 1;
        height = ymax - ymin + 1;

        //layer output container
        int[] dfiltered = new int[width * height];

        // initially set all the grid cell values
        // to 1, and when iterating thru' layers
        // if there is a no_data then it gets set
        for (i = 0; i < dfiltered.length; i++) {
            dfiltered[i] = 1;
        }

        //process layers
        for (Layer l : layers) {
            System.out.println("loading predictor: " + layerPath + l.name);
            grid = Grid.getGrid(layerPath + l.name);
//            grid = new Grid(layerPath + l.name, false);

            float[] d = grid.getGrid(); //get whole layer

            //Translate between data source grid and output grid
            int xoff = (int) ((grid.xmin - (xmin * TabulationSettings.grd_xdiv + TabulationSettings.grd_xmin)) / TabulationSettings.grd_xdiv);
            int yoff = (int) ((grid.ymin - (TabulationSettings.grd_ymin + ymin * TabulationSettings.grd_ydiv)) / TabulationSettings.grd_ydiv);

            //popuplate missing values
            if (cells == null) {
                //TODO: common grids source
                //grid.writeGrid(newPath + l.name, d,xmin,ymin,xmax,ymax, TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv,height,width);
            } else {
                for (i = 0; i < cells.length; i++) {
                    int x = cells[i][0] - xmin - xoff;
                    int y = cells[i][1] - ymin - yoff;
                    if (x >= 0 && x < grid.ncols
                            && y >= 0 && y < grid.nrows) {
                        int pSrc = x + (grid.nrows - y - 1) * grid.ncols;
                        int pDest = (cells[i][0] - xmin) + (height - (cells[i][1] - ymin) - 1) * width;
                        if (Float.isNaN(d[pSrc])) {
                            dfiltered[pDest] = -9999;
                        }
                    }
                }
            }
        }

        double minx = Math.rint(xmin + TabulationSettings.grd_xmin / TabulationSettings.grd_xdiv) * TabulationSettings.grd_xdiv;
        double miny = Math.rint(ymin + TabulationSettings.grd_ymin / TabulationSettings.grd_ydiv) * TabulationSettings.grd_ydiv;
        double maxx = Math.rint(width + minx / TabulationSettings.grd_xdiv) * TabulationSettings.grd_xdiv;
        double maxy = Math.rint(height + miny / TabulationSettings.grd_ydiv) * TabulationSettings.grd_ydiv;
        grid.writeGrid(outputdir + "domain", dfiltered,
                minx,
                miny,
                maxx,
                maxy,
                TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv, height, width);


    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        List<Layer> layerlist = new ArrayList<Layer>();
//
//        layerlist.add(new Layer("evap_mean", "Evaporation - average", "", "environmental", null));
//        layerlist.add(new Layer("evapm", "Evaporation - annual mean", "", "environmental", null));
//
//        Layer[] layers = layerlist.toArray(new Layer[layerlist.size()]);
//
//        String area = "POLYGON((89.264 -50.06,89.264 6.322,178.736 6.322,178.736 -50.06,89.264 -50.06))";
//
//        SimpleRegion region = SimpleShapeFile.parseWKT(area);
//
//        generate(layers, region);
    }
}
