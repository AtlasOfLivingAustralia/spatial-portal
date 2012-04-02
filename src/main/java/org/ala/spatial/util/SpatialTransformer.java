package org.ala.spatial.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.ala.layers.intersect.Grid;

/**
 * SpatialTransformer
 *
 * Transforms DIVA grids to ASCII grids
 *
 * @author ajay
 */
public class SpatialTransformer {

    public static void convertDivaToAsc(String filename, String ascfilename) throws FileNotFoundException, Exception {
        convertDivaToAsc(new Grid(filename), ascfilename);
    }

    public static void convertDivaToAsc(Grid grid, String ascfilename) throws FileNotFoundException, Exception {
        if (grid == null) {
            throw new FileNotFoundException("The specified grid file was not available at " + grid.filename + ". Please check the grid file exists.");
        }

        BufferedWriter fw = null;
        try {
            fw = new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(ascfilename), "US-ASCII"));
            fw.append("ncols ").append(String.valueOf(grid.ncols)).append("\n");
            fw.append("nrows ").append(String.valueOf(grid.nrows)).append("\n");
            fw.append("xllcorner ").append(String.valueOf(grid.xmin)).append("\n");
            fw.append("yllcorner ").append(String.valueOf(grid.ymin)).append("\n");
            fw.append("cellsize ").append(String.valueOf(grid.xres)).append("\n");

            fw.append("NODATA_value ").append(String.valueOf(-1));

            float [] grid_data = grid.getGrid();
            System.out.println("grid_data: " + grid_data.length);
            System.out.println("grid_data[0]: " + grid_data[0]);
            for (int i = 0; i < grid.nrows; i++) {
                fw.append("\n");
                for (int j = 0; j < grid.ncols; j++) {
                    if (j > 0) {
                        fw.append(" ");
                    }
                    if (Float.isNaN(grid_data[i * grid.ncols + j])) {
                        fw.append("-1");
                    } else {
                        fw.append(String.valueOf(grid_data[i * grid.ncols + j]));
                    }
                }
            }

            fw.append("\n");
            fw.close();


            //projection file
            writeProjectionFile(ascfilename.replace(".asc", ".prj"));
        } catch (Exception e) {
            throw e;
        } 
    }

    private static void writeProjectionFile(String filename) {
        try {
            PrintWriter spWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

            StringBuffer sbProjection = new StringBuffer();
            sbProjection.append("GEOGCS[\"WGS 84\", ").append("\n");
            sbProjection.append("    DATUM[\"WGS_1984\", ").append("\n");
            sbProjection.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            sbProjection.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            sbProjection.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            sbProjection.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            sbProjection.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.write(sbProjection.toString());
            spWriter.close();

        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }
    }

    public static void main(String[] args) {
        try {
            convertDivaToAsc("/data/ala/runtime/output/aloc/1323844048457/1323844048457", "/data/ala/runtime/output/aloc/1323844048457/myascfile.asc");
        } catch (Exception e) {
            System.out.println("Opps");
            e.printStackTrace(System.out);
        }
    }

}
