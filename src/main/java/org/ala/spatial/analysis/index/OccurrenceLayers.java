/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 * creates layers from occurrence information
 *
 * 1. species count on base grid as average of 3x3 grid cells
 * 2. occurrence count on base grid as average of 3x3 grid cells
 *
 * - load enough for occurrences and species counts
 * - make grids
 * - get grid cell values
 * - make average
 * - export
 *
 * @author Adam
 */
public class OccurrenceLayers {

    public static void makeOccurrenceCountLayer(int cell_offset) {
        TabulationSettings.load();
        int[][] actual_grid = new int[TabulationSettings.grd_ncols][TabulationSettings.grd_nrows];

        //region
        SimpleRegion region = new SimpleRegion();
        region.setBox(TabulationSettings.grd_xmin, TabulationSettings.grd_ymin, TabulationSettings.grd_xmax, TabulationSettings.grd_ymax);

        double[] points = OccurrencesCollection.getPoints(new OccurrencesFilter(region, Integer.MAX_VALUE));

        int x, y;
        for (int i = 0; i < points.length; i += 2) {
            x = (int) Math.floor((points[i] - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
            y = (int) Math.floor((points[i + 1] - TabulationSettings.grd_ymin) / TabulationSettings.grd_ydiv);

            if (x >= 0 && x < TabulationSettings.grd_ncols
                    && y >= 0 && y < TabulationSettings.grd_nrows) {
                actual_grid[x][y]++;
            }
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + "layer_occurrence_av_" + cell_offset + ".gri", "rw");

            byte[] b = new byte[TabulationSettings.grd_nrows * TabulationSettings.grd_ncols * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);

            bb.order(ByteOrder.LITTLE_ENDIAN);

            int xend, yend, xstart, ystart;
            int count;
            int sum;
            float max = Float.MAX_VALUE * -1;
            float min = Float.MAX_VALUE;
            float value;
            for (int j = TabulationSettings.grd_nrows - 1; j >= 0; j--) {
                for (int i = 0; i < TabulationSettings.grd_ncols; i++) {
                    count = 0;
                    sum = 0;
                    xend = Math.min(TabulationSettings.grd_ncols, i + cell_offset + 1);
                    yend = Math.min(TabulationSettings.grd_nrows, j + cell_offset + 1);
                    xstart = Math.max(i - cell_offset, 0);
                    ystart = Math.max(j - cell_offset, 0);
                    for (x = xstart; x < xend; x++) {
                        for (y = ystart; y < yend; y++) {
                            sum += actual_grid[x][y];
                            count++;
                        }
                    }
                    if(count == 0) {
                        bb.putFloat(0);
                    } else {
                        value = sum / (float) count;
                        if (max < value) {
                            max = value;
                        }
                        if (min > value) {
                            min = value;
                        }
                        bb.putFloat(value);
                    }
                }
            }
            raf.write(b);
            raf.close();

            //header
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path + "layer_occurrence_av_" + cell_offset + ".grd");
            fw.append("[General]\nCreator=ALASPATIAL\nCreated=00000000\nTitle=layer_occurrence_av_" + cell_offset + "\n\n");
            fw.append("[GeoReference]\nProjection=GEOGRAPHIC\nDatum=WGS84\nMapunits=DEGREES");
            fw.append("\nColumns=").append(String.valueOf(TabulationSettings.grd_ncols));
            fw.append("\nRows=").append(String.valueOf(TabulationSettings.grd_nrows));
            fw.append("\nMinX=").append(String.valueOf(TabulationSettings.grd_xmin));
            fw.append("\nMaxX=").append(String.valueOf(TabulationSettings.grd_xmax));
            fw.append("\nMinY=").append(String.valueOf(TabulationSettings.grd_ymin));
            fw.append("\nMaxY=").append(String.valueOf(TabulationSettings.grd_ymax));
            fw.append("\nResolutionX=").append(String.valueOf(TabulationSettings.grd_xdiv));
            fw.append("\nResolutionY=").append(String.valueOf(TabulationSettings.grd_ydiv));

            fw.append("\n\n[Data]\nDataType=FLT4BYTES");
            fw.append("\nMinValue=").append(String.valueOf((int) Math.floor(min)));
            fw.append("\nMaxValue=").append(String.valueOf((int) Math.ceil(max)));
            fw.append("\nNoDataValue=0");
            fw.append("\nTransparent=0");
            fw.append("\nUnits=");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void makeSpeciesCountLayer(int cell_offset) {
        int pieces = 10;
        int height = TabulationSettings.grd_nrows / pieces;

        float max = Float.MAX_VALUE * -1;
        float min = Float.MAX_VALUE;

        try {            
            ArrayList<SpeciesColourOption> extra = new ArrayList<SpeciesColourOption>();
            extra.add(SpeciesColourOption.fromName("taxon_name", false)); //request for SpeciesIndex lookup number
            //region
            SimpleRegion region = new SimpleRegion();
            region.setBox(TabulationSettings.grd_xmin, TabulationSettings.grd_ymin, TabulationSettings.grd_xmax, TabulationSettings.grd_ymax);
            double[] points = OccurrencesCollection.getPoints(new OccurrencesFilter(region, Integer.MAX_VALUE), extra);
            int[] speciesLookupNumber = extra.get(0).getIntArray();

            RandomAccessFile raf = new RandomAccessFile(
                    TabulationSettings.index_path + "layer_species_av_" + cell_offset + ".gri", "rw");
            int top_height = 0;
            for (int current_height = 0; current_height < TabulationSettings.grd_nrows; current_height += height) {
                top_height = current_height;
            }

            for (int current_height = top_height; current_height >= 0; current_height -= height) {
                System.out.println("ch:" + current_height);

                double grd_ymin = Math.max((current_height - cell_offset) * TabulationSettings.grd_ydiv + TabulationSettings.grd_ymin, TabulationSettings.grd_ymin);
                int grd_nrows = cell_offset * 2 + height;
                if (grd_nrows > TabulationSettings.grd_nrows) {
                    grd_nrows = TabulationSettings.grd_nrows;
                }
                int row_start = (current_height > 0) ? cell_offset : 0;
                int row_end = (current_height > 0) ? cell_offset + height : height;

                BitSet[][] actual_grid = new BitSet[TabulationSettings.grd_ncols][grd_nrows];

                int x, y;
                for (int i = 0; i < points.length; i += 2) {
                    x = (int) Math.floor((points[i] - TabulationSettings.grd_xmin) / TabulationSettings.grd_xdiv);
                    y = (int) Math.floor((points[i + 1] - grd_ymin) / TabulationSettings.grd_ydiv);

                    if (x >= 0 && x < TabulationSettings.grd_ncols
                            && y >= 0 && y < grd_nrows) {
                        if (actual_grid[x][y] == null) {
                            actual_grid[x][y] = new BitSet();
                        }

                        if (speciesLookupNumber[i / 2] >= 0) {
                            actual_grid[x][y].set(speciesLookupNumber[i / 2]);
                        }
                    }
                }

                byte[] b = new byte[(row_end - row_start) * TabulationSettings.grd_ncols * 4];
                ByteBuffer bb = ByteBuffer.wrap(b);

                bb.order(ByteOrder.LITTLE_ENDIAN);

                int xend, yend, xstart, ystart;
                int count;
                BitSet sum = new BitSet();
                float value;
                for (int j = row_end - 1; j >= row_start; j--) {
                    for (int i = 0; i < TabulationSettings.grd_ncols; i++) {
                        count = 0;
                        sum.clear();
                        xend = Math.min(TabulationSettings.grd_ncols, i + cell_offset + 1);
                        yend = Math.min(grd_nrows, j + cell_offset + 1);
                        xstart = Math.max(i - cell_offset, 0);
                        ystart = Math.max(j - cell_offset, 0);
                        for (x = xstart; x < xend; x++) {
                            for (y = ystart; y < yend; y++) {
                                if (actual_grid[x][y] != null) {
                                    sum.or(actual_grid[x][y]);
                                    count++;
                                }
                            }
                        }
                        if(count == 0) {
                            bb.putFloat(0);
                        } else {
                            value = countSet(sum) / (float) count;
                            if (max < value) {
                                max = value;
                            }
                            if (min > value) {
                                min = value;
                            }
                            bb.putFloat(value);
                        }
                    }
                }
                raf.write(b);
            }

            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //header
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path + "layer_species_av_" + cell_offset + ".grd");
            fw.append("[General]\nCreator=ALASPATIAL\nCreated=00000000\nTitle=layer_species_av_" + cell_offset + "\n\n");
            fw.append("[GeoReference]\nProjection=GEOGRAPHIC\nDatum=WGS84\nMapunits=DEGREES");
            fw.append("\nColumns=").append(String.valueOf(TabulationSettings.grd_ncols));
            fw.append("\nRows=").append(String.valueOf(TabulationSettings.grd_nrows));
            fw.append("\nMinX=").append(String.valueOf(TabulationSettings.grd_xmin));
            fw.append("\nMaxX=").append(String.valueOf(TabulationSettings.grd_xmax));
            fw.append("\nMinY=").append(String.valueOf(TabulationSettings.grd_ymin));
            fw.append("\nMaxY=").append(String.valueOf(TabulationSettings.grd_ymax));
            fw.append("\nResolutionX=").append(String.valueOf(TabulationSettings.grd_xdiv));
            fw.append("\nResolutionY=").append(String.valueOf(TabulationSettings.grd_ydiv));

            fw.append("\n\n[Data]\nDataType=FLT4BYTES");
            fw.append("\nMinValue=").append(String.valueOf((int) Math.floor(min)));
            fw.append("\nMaxValue=").append(String.valueOf((int) Math.ceil(max)));
            fw.append("\nNoDataValue=0");
            fw.append("\nTransparent=0");
            fw.append("\nUnits=");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int countSet(BitSet bs) {
        if (bs == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < bs.size(); i++) {
            if (bs.get(i)) {
                count++;
            }
        }
        return count;
    }

    static public void main(String[] args) {
        TabulationSettings.load();
        OccurrencesCollection.init();
        DatasetMonitor dm = new DatasetMonitor();
        dm.initDatasetFiles();

        int size = 4; //spacing is 4 + 1 + 4 = 9x9
        if (args.length > 0) {
            try {
                size = (int) (Integer.parseInt(args[0]) - 1) / 2;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        makeOccurrenceCountLayer(size);
        makeSpeciesCountLayer(size);
    }
}
