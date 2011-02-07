/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.BitSet;
import org.ala.spatial.analysis.index.LayerFilter;

/**
 * Class for region cutting test data grids
 *
 * @author adam
 */
public class GridCutter {

    /**
     * exports a list of layers cut against a region
     * 
     * Cut layer files generated are input layers with
     * grid cells outside of region set as missing.
     * 
     * Headers are copied, for test data only.
     * 
     * @param layers list of layers to cut as Layer
     * @param region 
     * @return
     */
    public static String cut(Layer[] layers, SimpleRegion region, LayerFilter[] envelopes, String extentsFilename) {
        TabulationSettings.load();

        //mkdir in index location
        String newPath = null;
        try {
            newPath = TabulationSettings.index_path + System.currentTimeMillis() + java.io.File.separator;
            File directory = new File(newPath);
            directory.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* get data, remove missing values, restrict by optional region */
        int i;
        int width;
        int height;
        int xmin;
        int ymin;
        int xmax;
        int ymax;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) {
            layerPath = "";
        }
        Grid grid = null;

        //- all grids sit on the same grid, although origin
        // may not be aligned

        //identify cells to keep
        int[][] cells = null;
        if (envelopes == null) {
            cells = (int[][]) region.getAttribute("cells");
            if(cells == null){
                cells = region.getOverlapGridCells(
                        TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                        TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                        TabulationSettings.grd_ncols, TabulationSettings.grd_nrows,
                        null);
            }
        } else {
            
            cells = getOverlapGridCells(envelopes, 
                    TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                    TabulationSettings.grd_xmax, TabulationSettings.grd_ymax);
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
        double[] dfiltered = new double[width * height];

        //process layers
        for (Layer l : layers) {
            grid = Grid.getGrid(layerPath + l.name);

            float[] d = grid.getGrid(); //get whole layer

            //set all as missing values
            for (i = 0; i < dfiltered.length; i++) {
                dfiltered[i] = Double.NaN;
            }

            //Translate between data source grid and output grid
            int xoff = (int) ((grid.xmin - (xmin * TabulationSettings.grd_xdiv + TabulationSettings.grd_xmin)) / TabulationSettings.grd_xdiv);
            int yoff = (int) ((grid.ymin - (TabulationSettings.grd_ymin + ymin * TabulationSettings.grd_ydiv)) / TabulationSettings.grd_ydiv);

            //popuplate non-missing values
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
                        dfiltered[pDest] = d[pSrc];
                    }
                }

                double minx = Math.rint(xmin + TabulationSettings.grd_xmin/TabulationSettings.grd_xdiv) * TabulationSettings.grd_xdiv;
                double miny = Math.rint(ymin + TabulationSettings.grd_ymin/TabulationSettings.grd_ydiv) * TabulationSettings.grd_ydiv;
                double maxx = Math.rint(width + minx/TabulationSettings.grd_xdiv) * TabulationSettings.grd_xdiv;
                double maxy = Math.rint(height + miny/TabulationSettings.grd_ydiv) * TabulationSettings.grd_ydiv;
                grid.writeGrid(newPath + l.name, dfiltered,
                        minx,
                        miny,
                        maxx,
                        maxy,
                        TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv, height, width);

                //write extents into a file now
                if (extentsFilename != null && l == layers[0]) {
                    try {
                        FileWriter fw = new FileWriter(extentsFilename);
                        fw.append(String.valueOf(width)).append("\n");
                        fw.append(String.valueOf(height)).append("\n");
                        fw.append(String.valueOf(minx)).append("\n");
                        fw.append(String.valueOf(miny)).append("\n");
                        fw.append(String.valueOf(maxx)).append("\n");
                        fw.append(String.valueOf(maxy));
                        fw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return newPath;
    }

    /**
     * exports a list of layers cut against an environmental envelope
     *
     * Cut layer files generated are input layers with
     * grid cells outside of region set as missing.
     *
     * Headers are copied, for test data only.
     *
     * @param layers list of layers to cut as Layer
     * @param envelope environmental layer filters list as LayerFilter[]
     * @return
     */
    public static String cut(Layer[] layers, LayerFilter[] envelope) {
        TabulationSettings.load();

        //mkdir in index location
        String newPath = null;
        try {
            newPath = TabulationSettings.index_path + System.currentTimeMillis() + File.separator;
            File directory = new File(newPath);
            directory.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* get data, remove missing values, restrict by optional region */
        int i, j;
        j = 0;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) {
            layerPath = "";
        }
        Grid grid = null;

        double[] missingCells = null;

        /* make list of missing cells */
        int k;
        for (k = 0; k < envelope.length; k++) {
            System.out.println("cutting with: " + envelope[k].layer.name);
            grid = Grid.getGrid(layerPath + envelope[k].layer.name);

            float[] d = grid.getGrid();

            if (missingCells == null) {
                //init missing cells
                missingCells = new double[d.length];
                for (j = 0; j < missingCells.length; j++) {
                    missingCells[j] = 0;
                }
            }

            //apply filter for this envelope
            LayerFilter lf = envelope[k];
            for (i = 0; i < d.length && i < missingCells.length; i++) {
                if (!(lf.maximum_value >= d[i] && lf.minimum_value <= d[i])) {
                    missingCells[i] = Float.NaN;
                }
            }
        }

        /* process provided layers */
        for (Layer l : layers) {
            grid = Grid.getGrid(layerPath + l.name);

            float[] d = grid.getGrid();
            double[] dfiltered = new double[d.length];

            //set all as missing values
            for (i = 0; i < dfiltered.length; i++) {
                dfiltered[i] = Double.NaN;
            }

            //popuplate non-missing values
            if (missingCells == null) {
                grid.writeGrid(newPath + l.name, d,
                        TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                        TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                        TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv,
                        TabulationSettings.grd_nrows, TabulationSettings.grd_ncols);
            } else {
                for (i = 0; i < d.length; i++) {
                    if (!Double.isNaN(missingCells[i])) {
                        //TODO: test for min/max extents

                        dfiltered[i] = d[i];
                    }
                }

                //write to filtered data
                grid.writeGrid(newPath + l.name, dfiltered,
                        TabulationSettings.grd_xmin, TabulationSettings.grd_ymin,
                        TabulationSettings.grd_xmax, TabulationSettings.grd_ymax,
                        TabulationSettings.grd_xdiv, TabulationSettings.grd_ydiv,
                        TabulationSettings.grd_nrows, TabulationSettings.grd_ncols);
            }
        }

        return newPath;
    }

    public static int[][] getOverlapGridCells(LayerFilter[] envelope, double xmin, double ymin, double xmax, double ymax) {
        TabulationSettings.load();

        int xrange = (int) Math.ceil((xmax - xmin) / TabulationSettings.grd_xdiv);
        int yrange = (int) Math.ceil((ymax - ymin) / TabulationSettings.grd_ydiv);

        //output structure; false == passes filter, true == does not pass filter
        BitSet bs = new BitSet(xrange * yrange);
        double [][] points = new double[xrange * yrange][2];
        for(int i=0;i<xrange;i++){
            for(int j=0;j<yrange;j++){
                points[i + j*xrange][0] = (double)(xmin + i * TabulationSettings.grd_xdiv);
                points[i + j*xrange][1] = (double)(ymin + j * TabulationSettings.grd_ydiv);
            }
        }
        for (int k = 0; k < envelope.length; k++) {
            System.out.println("cutting with: " + envelope[k].layer.name);
            Grid grid = Grid.getGrid(TabulationSettings.getPath(envelope[k].layer.name));

            float[] d = grid.getValues2(points);
            //float[] d = grid.getValues2(xmin, xmax, ymin, ymax);

            LayerFilter lf = envelope[k];
            for (int i = 0; i < d.length; i++) {
                if (Float.isNaN(d[i]) || lf.maximum_value < d[i] || lf.minimum_value > d[i]) {
                    bs.set(i);
                }
            }
        }

        //make output
        int size = 0;
        for (int i = 0; i < bs.size(); i++) {
            if (!bs.get(i)) {
                size++;
            }
        }
        int pos = 0;
        int[][] cells = new int[size][2];
        for (int i = 0; i < bs.size(); i++) {
            if (!bs.get(i)) {
                cells[pos][0] = i % xrange;
                cells[pos][1] = (int) (i / xrange);
                pos++;
            }
        }

        return cells;
    }

    public static ArrayList<Object> cut(Layer[] layers, SimpleRegion region, int pieces, String extentsFilename, LayerFilter[] envelopes) {
        return cut(layers, region, pieces, extentsFilename, envelopes, null);
    }

    public static ArrayList<Object> cut(Layer[] layers, SimpleRegion region, int pieces, String extentsFilename, LayerFilter[] envelopes, AnalysisJob job) {
        ArrayList<Object> data = new ArrayList<Object>();

        if(job != null) job.setProgress(0);

        //determine outer bounds of layers
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = Double.MAX_VALUE*-1;
        double ymax = Double.MAX_VALUE*-1;
        for (Layer l : layers) {
            Grid g = Grid.getGrid(TabulationSettings.getPath(l.name));
            if (xmin > g.xmin) {
                xmin = g.xmin;
            }
            if (xmax < g.xmax) {
                xmax = g.xmax;
            }
            if (ymin > g.ymin) {
                ymin = g.ymin;
            }
            if (ymax < g.ymax) {
                ymax = g.ymax;
            }
        }

        //restrict bounds by region
        if(region != null){
            xmax = Math.min(xmax, region.getBoundingBox()[1][0]);
            ymax = Math.min(ymax, region.getBoundingBox()[1][1]);
            xmin = Math.max(xmin, region.getBoundingBox()[0][0]);
            ymin = Math.max(ymin, region.getBoundingBox()[0][1]);
        }

        //transform x/y min/max to grid size
        xmin = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv
                * Math.floor((xmin - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
        ymin = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv
                * Math.floor((ymin - TabulationSettings.grd_ymin) / TabulationSettings.grd_ydiv);
        xmax = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv
                * Math.ceil((xmax - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
        ymax = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv
                * Math.ceil((ymax - TabulationSettings.grd_ymin) / TabulationSettings.grd_ydiv);

        //determine range and width's
        double xrange = xmax - xmin;
        double yrange = ymax - ymin;
        int width = (int) Math.ceil(xrange / TabulationSettings.grd_xdiv);
        int height = (int) Math.ceil(yrange / TabulationSettings.grd_ydiv);

        //write extents into a file now
        if (extentsFilename != null) {
            try {
                FileWriter fw = new FileWriter(extentsFilename);
                fw.append(String.valueOf(width)).append("\n");
                fw.append(String.valueOf(height)).append("\n");
                fw.append(String.valueOf(xmin)).append("\n");
                fw.append(String.valueOf(ymin)).append("\n");
                fw.append(String.valueOf(xmax)).append("\n");
                fw.append(String.valueOf(ymax));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(job != null) job.setProgress(0.1,"exported extents");

        //make cells list for outer bounds
        int[][] cells;
        if(envelopes == null){
            cells = (int[][]) region.getAttribute("cells");
            if(cells == null){
                cells = region.getOverlapGridCells(xmin, ymin, xmax, ymax,
                    width, height, null);
            } else {
                //translate to xmin, ymin ,xmax, ymax, width, height
                int dx = (int) Math.round((TabulationSettings.grd_xmin - xmin) / TabulationSettings.grd_xdiv);
                int dy = (int) Math.round((TabulationSettings.grd_ymin - ymin) / TabulationSettings.grd_ydiv);
                int pos = 0;
                int x,y;
                for(int i=0;i<cells.length;i++){
                    x = cells[i][0] + dx;
                    y = cells[i][1] + dy;
                    //only use cells within extents
                    if(x >= 0 && x < width && y >= 0 && y < height){
                        cells[pos][0] = x;
                        cells[pos][1] = y;
                        pos++;
                    }
                }             
                cells = java.util.Arrays.copyOf(cells, pos);
            }

        } else {
            cells = getOverlapGridCells(envelopes,xmin, ymin, xmax, ymax);
        }

        if(job != null) job.setProgress(0.2,"determined target cells");

//TODO: test for zero length cells
        System.out.println("Cut cells count: " + cells.length);

        //transform cells numbers to long/lat numbers
        double[][] points = new double[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            points[i][0] = xmin + cells[i][0] * TabulationSettings.grd_xdiv;
            points[i][1] = ymin + cells[i][1] * TabulationSettings.grd_ydiv;
        }

        //initialize data structure to hold everything
        // each data piece: row1[col1, col2, ...] row2[col1, col2, ...] row3...
        //TODO: new class to house pieces, writing to disk instead of
        //keeping all of it in memory.
        int remainingLength = cells.length;
        int step = (int) Math.floor(remainingLength / (double) pieces);
        for (int i = 0; i < pieces; i++) {
            if (i == pieces - 1) {
                data.add(new float[remainingLength * layers.length]);
            } else {
                data.add(new float[step * layers.length]);
                remainingLength -= step;
            }
        }

        //iterate for layers
        double [] layerExtents = new double[layers.length*2];
        for (int j = 0; j < layers.length; j++) {
            Grid g = Grid.getGrid(TabulationSettings.getPath(layers[j].name));
            float[] v = g.getValues2(points);

            //row range standardization
            float minv = Float.MAX_VALUE;
            float maxv = Float.MAX_VALUE*-1;
            for(int i=0;i<v.length;i++){
                if(v[i] < minv) minv = v[i];
                if(v[i] > maxv) maxv = v[i];
            }
            float range = maxv - minv;
            if(range > 0){
                for(int i=0;i<v.length;i++){
                    v[i] = (v[i] - minv) / range;
                }
            } else {
                for(int i=0;i<v.length;i++){
                    v[i] = 0;
                }
            }
            layerExtents[j*2] = minv;
            layerExtents[j*2+1] = maxv;

            //iterate for pieces
            for (int i = 0; i < pieces; i++) {
                float[] d = (float[]) data.get(i);                
                for (int k = j, n = i * step; k < d.length; k += layers.length, n++) {
                    d[k] = v[n];
                }
            }

            if(job != null) job.setProgress(0.2 + j/(double)layers.length*7/10.0, layers[j].name);
        }
        
        if(job != null) job.log("loaded data");

        //remove null rows from data and cells
        int newCellPos = 0;
        int currentCellPos = 0;
        for (int i = 0; i < pieces; i++) {
            float[] d = (float[]) data.get(i);
            int newPos = 0;
            for (int k = 0; k < d.length; k += layers.length) {
                int nMissing = 0;
                for (int j = 0; j < layers.length; j++) {
                    if (Float.isNaN(d[k + j])) {
                        nMissing++;
                    }
                }
                if (nMissing < layers.length) {
                    if (newPos < k) {
                        for (int j = 0; j < layers.length; j++) {
                            d[newPos + j] = d[k + j];
                        }
                    }
                    newPos += layers.length;
                    if (newCellPos < currentCellPos) {
                        cells[newCellPos][0] = cells[currentCellPos][0];
                        cells[newCellPos][1] = cells[currentCellPos][1];
                    }
                    newCellPos++;
                }
                currentCellPos++;
            }
            if (newPos < d.length) {
                d = java.util.Arrays.copyOf(d, newPos);
                data.set(i, d);
            }
        }

        //remove zero length data pieces
        for (int i = pieces-1; i >= 0; i--) {
            float[] d = (float[]) data.get(i);
            if(d.length == 0){
                data.remove(i);
            }
        }

        //add cells reference to output
        data.add(cells);

        //add extents to output
        double[] extents = new double[6 + layerExtents.length];
        extents[0] = width;
        extents[1] = height;
        extents[2] = xmin;
        extents[3] = ymin;
        extents[4] = xmax;
        extents[5] = ymax;
        for(int i=0;i<layerExtents.length;i++){
            extents[6+i] = layerExtents[i];
        }
        data.add(extents);
        
        if(job != null) job.setProgress(1,"cleaned data");

        return data;
    }

    static int countCells(SimpleRegion region, LayerFilter[] envelopes) {
        long start = System.currentTimeMillis();

        //determine outer bounds of layers
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double xmax = Double.MAX_VALUE*-1;
        double ymax = Double.MAX_VALUE*-1;
        if(envelopes != null){
            for (LayerFilter lf : envelopes) {
                Grid g = new Grid(TabulationSettings.getPath(lf.layer.name));
                if (xmin > g.xmin) {
                    xmin = g.xmin;
                }
                if (xmax < g.xmax) {
                    xmax = g.xmax;
                }
                if (ymin > g.ymin) {
                    ymin = g.ymin;
                }
                if (ymax < g.ymax) {
                    ymax = g.ymax;
                }
            }
        } else if(region != null){
            //restrict bounds by region
            xmax = region.getBoundingBox()[1][0];
            ymax = region.getBoundingBox()[1][1];
            xmin = region.getBoundingBox()[0][0];
            ymin = region.getBoundingBox()[0][1];
        }

        //transform x/y min/max to grid size
        xmin = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv
                * Math.floor((xmin - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
        ymin = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv
                * Math.floor((ymin - TabulationSettings.grd_ymin) / TabulationSettings.grd_ydiv);
        xmax = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv
                * Math.ceil((xmax - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
        ymax = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv
                * Math.ceil((ymax - TabulationSettings.grd_ymin) / TabulationSettings.grd_ydiv);

        //determine range and width's
        double xrange = xmax - xmin;
        double yrange = ymax - ymin;
        int width = (int) Math.ceil(xrange / TabulationSettings.grd_xdiv);
        int height = (int) Math.ceil(yrange / TabulationSettings.grd_ydiv);

        
        int[][] cells;
        if(envelopes == null){
            cells = region.getOverlapGridCells(xmin, ymin, xmax, ymax,
                width, height, null);
        } else {
            cells = getOverlapGridCells(envelopes,xmin, ymin, xmax, ymax);
        }

        long end = System.currentTimeMillis();

        System.out.println("counted grid cells in: " + (end - start) + "ms");

        return cells.length;
    }
}
