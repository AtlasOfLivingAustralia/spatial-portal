/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import java.io.File;

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
    public static String cut(Layer[] layers, SimpleRegion region) {
    	TabulationSettings.load();


        //mkdir in index location
        String newPath = null;
        try{
            newPath = TabulationSettings.index_path + System.currentTimeMillis() + "/";
            File directory = new File(newPath);
            directory.mkdir();
        } catch (Exception e){
            e.printStackTrace();
        }

    	/* get data, remove missing values, restrict by optional region */
        int i, j;
        float[][] data = null;
        j = 0;
        int width = 252, height = 210;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) layerPath = "";
        Grid grid = null;

        //identify cells to keep
        int [][] cells = null;
        
        for (Layer l : layers) {
            grid = new Grid(layerPath + l.name);

            if(region != null && cells == null){
	            //roll out missing values and values not in region
	        	cells = region.getOverlapGridCells(
	        			grid.xmin, grid.ymin,
	        			grid.xmax, grid.ymax,
	        			grid.ncols, grid.nrows,
	        			null);
        }


            width = grid.ncols;
            height = grid.nrows;

            double[] d = grid.getGrid();
            double[] dfiltered = new double[d.length];

            //set all as missing values
            for (i=0;i<dfiltered.length;i++){
                dfiltered[i] = Double.NaN;
            }

            //popuplate non-missing values
            if(cells == null){
            grid.writeGrid(newPath + l.name, d);
            }else{
            for (i = 0; i < cells.length; i++) {
                int p = cells[i][0] + (height - cells[i][1] - 1) * width;
                dfiltered[p] = d[p];
            }

            //mkdir in index location
            grid.writeGrid(newPath + l.name, dfiltered);
            }
            
        }

        return newPath;
    }
}
