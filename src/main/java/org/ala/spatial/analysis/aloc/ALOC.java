package org.ala.spatial.analysis.aloc;

import java.awt.image.BufferedImage;
import org.ala.spatial.analysis.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ala.spatial.analysis.tabulation.TabulationSettings;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleRegion;

public class ALOC {

    public static void main(String args[]) {
        TabulationSettings.load();
        run("output.png", TabulationSettings.environmental_data_files, 20);
    }

    public static int[] run(String filename, Layer[] layers, int numberofgroups) {
        int i, j;
        float[][] data = null;
        j = 0;
        int width = 252, height = 210;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) layerPath = "";
        System.out.println("layerPath: " + layerPath); 
        for (Layer l : layers) {
            System.out.println("Loading layer " + l.display_name + " -> " + l.name + " => " + TabulationSettings.environmental_data_path);
            Grid grid = new Grid(
                    layerPath
                    + l.name);
            width = grid.ncols;
            height = grid.nrows;
            System.out.println("WidthxHeight: " + width + "x" + height); 
            double[] d = grid.getGrid();
            if (data == null) {
                data = new float[d.length][layers.length];
            }
            for (i = 0; i < d.length && i < data.length; i++) {
                data[i][j] = (float) d[i];
            }
            j++;
        }

        if (data != null) {
            //roll out missing values
            int count = 0;
            for (i = 0; i < data.length; i++) {
                for (j = 0; j < data[i].length; j++) {
                    if (Float.isNaN(data[i][j])) {
                        break;
                    }
                }
                if (j == data[i].length) {
                    count++;
                }
            }
            System.out.println("clean records=" + count);
            float[][] data_clean = new float[count][data[0].length];
            int[] mapping = new int[count];
            count = 0;
            for (i = 0; i < data.length; i++) {
                for (j = 0; j < data[i].length; j++) {
                    if (Float.isNaN(data[i][j])) {
                        break;
                    }
                }
                if (j == data[i].length) {
                    for (j = 0; j < data[i].length; j++) {
                        data_clean[count][j] = data[i][j];
                        mapping[count] = i;
                    }
                    count++;
                }
            }

            int[] groups = runGowerMetric(data_clean, numberofgroups);

            /* calculate group means */
            double[][] group_means = new double[numberofgroups + 1][data_clean[0].length];
            int[] group_counts = new int[numberofgroups + 1];

            /* TODO: handle numerical overflow */
            for (i = 0; i < groups.length; i++) {
                group_counts[groups[i]]++;
                for (j = 0; j < data_clean[i].length; j++) {
                    group_means[groups[i]][j] += data_clean[i][j];
                }
            }
            for (i = 0; i < group_means.length; i++) {

                /* TODO: this check needs to be removed */
                if (group_counts[i] > 0) {
                    for (j = 0; j < group_means[i].length; j++) {
                        group_means[i][j] /= group_counts[i];
                    }
                }
            }


            /* get RGB for colouring group means */

            /* PCA */
            int[][] colours = PCA.getColours(group_means);
            
            /* export means + colours */
            exportMeansColours(filename + ".csv",group_means,colours,layers);

            /* map back as colours, grey scale for now */
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            int[] image_bytes;

            image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth());

            /* TODO: set missing value other than black */

            /* try transparency */            
            for (i = 0; i < image_bytes.length; i++) {
                image_bytes[i] = 0x00000000;
            }
             
            int group;
            int[] colour = new int[3];
            for (i = 0; i < groups.length; i++) {
                //image_bytes[mapping[i]] = (groups[i] * range) / numberofgroups + min; //set blue gradient
                group = groups[i];
                for (j = 0; j < colour.length; j++) {
                    colour[j] = (int) (colours[groups[i]][j]);
                }

                //set up rgb colour for this group
                //image_bytes[mapping[i]] = 0x00000000 & (colour[0] << 16) & (colour[1] << 8) & (colour[0]);
                image_bytes[mapping[i]] = 0xff000000 | ((colour[0] * 255 + colour[1]) * 255 + colour[2]);
            }

            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_bytes, 0, image.getWidth());

            try {
                ImageIO.write(image, "png",
                        new File(filename));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return groups;
        }

        return null;
    }

    /**
     * ALOC: Gower Metric measure
     */
    public static int[] runGowerMetric(float[][] data, int nNoOfGroups) {
        System.out.println("ALOC.run(data," + nNoOfGroups + ")");

        int nCols = data[0].length;
        int nRows = data.length;

        float[] col_min = new float[nCols];
        float[] col_max = new float[nCols];
        float[] col_range = new float[nCols];

        int[] seedidx = new int[nNoOfGroups + 1];

        int seedidxsize = 0;
        int i, j;

        System.out.println("calculating min/max for columns");

        for (i = 0; i < nCols; i++) {
            col_min[i] = Float.NaN;
            col_max[i] = Float.NaN;

            for (j = 0; j < nRows; j++) {
                if (!Float.isNaN(data[j][i])
                        && (Float.isNaN(col_min[i]) || col_min[i] > data[j][i])) {
                    col_min[i] = data[j][i];
                }
                if (!Float.isNaN(data[j][i])
                        && (Float.isNaN(col_max[i]) || col_max[i] < data[j][i])) {
                    col_max[i] = data[j][i];
                }
            }

            col_range[i] = col_max[i] - col_min[i];

            System.out.println("min/max/range: " + col_min[i] + "/" + col_max[i] + "/" + col_range[i]);
        }

        //1. determine correct # of groups by varying radius
        float start_radius = 1;
        float radius = start_radius;
        float step = radius / 2.0f;

        int count = 0;
        seedidx[0] = 0;
        int k;
        while (seedidxsize != nNoOfGroups && count < 30) {
            seedidxsize = 1;

            for (i = 1; i < nRows; i++) {
                for (j = 0; j < seedidxsize; j++) {
                    //calc dist between obj(i) & obj(seedidx(j))
                    float dist = 0;
                    int missing = 0;
                    for (k = 0; k < nCols; k++) {
                        float v1 = data[i][k];
                        float v2 = data[seedidx[j]][k];

                        if (Float.isNaN(v1) || Float.isNaN(v2)) {
                            missing++;
                        } else {
                            dist += java.lang.Math.abs(v1 - v2) / (float) col_range[k];
                        }
                    }

                    //add to seedidx if distance > radius
                    if (nCols == missing) {
                        //System.out.println("nCols==missing");

                        //error
                        missing--;
                    } else {
                        //	System.out.print(".");
                    }
                    dist = dist / (float) (nCols - missing);
                    if (dist < radius) {
                        break;
                    }
                }
                if (j == seedidxsize) {
                    seedidx[seedidxsize] = i;
                    seedidxsize++;
                }

                if (seedidxsize > nNoOfGroups) {
                    break;
                }
            }
            count++; //force a break

            System.out.println("Groups seeded: " + seedidxsize + ", looking for: " + nNoOfGroups + " at radius=" + radius);

            if (seedidxsize == nNoOfGroups) {
                continue;
            }

            //PERFORM RECONCILIATION OF NUMBER OF GROUPS IF count >= 20
            if (count < 20) {
                if (seedidxsize < nNoOfGroups) {
                    radius -= step;
                } else if (seedidxsize > nNoOfGroups) {
                    radius += step;
                }
                step /= 2.0;
            } else {
                //loop while number of groups is < nNoOfGroups
                if (seedidxsize < nNoOfGroups) {
                    radius -= step;
                } else {
                    break;
                }
            }

            System.out.println("ALOC Gower metric: seed generation iteration=" + count);
        }

        float[] seeds = new float[seedidxsize * nCols];
        float[] seedgroup_nonmissingvalues = new float[seedidxsize * nCols];


        System.out.println("got groups seeds: " + seedidxsize);

        //2. allocate all objects to a group
        int[] groups = new int[nRows];
        float[] groups_dist = new float[nRows];
        int[] groupsize = new int[seedidxsize];
        for (i = 0; i < seedidxsize; i++) {
            groupsize[i] = 0;
            for (j = 0; j < nCols; j++) {
                seeds[i * nCols + j] = Float.NaN;
                seedgroup_nonmissingvalues[i * nCols + j] = 0;
            }
        }
        for (i = 0; i < nRows; i++) {
            groups[i] = -1;
        }

        int iteration = 0;
        int movement = 1;
        int min_movement = -1;
        int[] min_groups = new int[nRows];
        float[] min_dists = new float[nRows];
        while (movement != 0 && iteration < 100) {
            movement = 0;

            for (i = 0; i < nRows; i++) {
                //step 4. pop from current group if current group has > 1 member
                if (iteration != 0) {
                    if (groupsize[groups[i]] == 1) {
                        continue;
                    }

                    groupsize[groups[i]]--;
                    for (k = 0; k < nCols; k++) {
                        float v1 = data[i][k];
                        float v2 = seeds[groups[i] * nCols + k];
                        if (!Float.isNaN(v1) && !Float.isNaN(v2)) {
                            seeds[groups[i] * nCols + k] = v2 - v1;

                        }
                        if (!Float.isNaN(v1)) {
                            seedgroup_nonmissingvalues[groups[i] * nCols + k]--;
                        }
                    }
                }

                float min_dist = 0.00001f;
                int min_idx = 0;
                for (j = 0; j < seedidxsize; j++) {
                    //calc dist between obj(i) & obj(seeds(j))
                    float dist = 0;
                    int missing = 0;
                    if (iteration != 0) {
                        for (k = 0; k < nCols; k++) {
                            float v1 = data[i][k];
                            float v2 = seeds[j * nCols + k];
                            if (Float.isNaN(v1) || Float.isNaN(v2)) {
                                missing++;
                            } else {
                                if (seedgroup_nonmissingvalues[j * nCols + k] > 0) {
                                    v2 = v2 / seedgroup_nonmissingvalues[j * nCols + k];
                                }
                                dist += java.lang.Math.abs(v1 - v2) / (float) col_range[k];
                            }
                        }
                        dist = dist / (float) (nCols - missing);
                        if (j == 0 || min_dist > dist) {
                            min_dist = dist;
                            min_idx = j;
                        }
                    } else {
                        //seeds present in indexed file
                        for (k = 0; k < nCols; k++) {
                            float v1 = data[i][k];
                            float v2 = data[seedidx[j]][k];
                            if (Float.isNaN(v1) || Float.isNaN(v2)) {
                                missing++;
                            } else {
                                dist += java.lang.Math.abs(v1 - v2) / (float) col_range[k];
                            }
                        }
                        dist = dist / (float) (nCols - missing);
                        if (j == 0 || min_dist > dist) {
                            min_dist = dist;
                            min_idx = j;
                        }
                    }
                }
                //add this group to group min_idx;
                if (groups[i] != min_idx) {
                    movement++;
                }
                groups[i] = min_idx;
                groups_dist[i] = min_dist;
                groupsize[min_idx]++;

                for (k = 0; k < nCols; k++) {
                    int idx = groups[i] * nCols + k;
                    float v1 = seeds[idx];
                    if (!Float.isNaN(data[i][k])) {
                        if (Float.isNaN(v1)) {
                            seeds[idx] = data[i][k];
                        } else {
                            seeds[idx] = seeds[idx] + data[i][k];
                        }
                        seedgroup_nonmissingvalues[idx]++;
                    }
                }
            }

            //3. calc group centroid
            //already done!

            //4. pop each object from group (recalc centroid) and allocate again
            iteration++;

            System.out.println("ALOC Gower metric: iteration=" + iteration + " movement=" + movement);

            //backup min_movement
            if (min_movement == -1 || min_movement > movement) {
                min_movement = movement;
                //copy groups to min_groups
                for (k = 0; k < nRows; k++) {
                    min_groups[k] = groups[k];
                    min_dists[k] = groups_dist[k];
                }
            }
        }

        //write-back row groups
        return groups;
    }
    
    public static int[] run(String filename, Layer[] layers, int numberofgroups, SimpleRegion simpleregion) {
    	TabulationSettings.load();
    	
    	if(simpleregion == null){
    		return run(filename,layers,numberofgroups);
    	}
    	
        int i, j;
        float[][] data = null;
        j = 0;
        int width = 252, height = 210;
        String layerPath = TabulationSettings.environmental_data_path;
        if (layerPath == null) layerPath = "";
        Grid grid = null;
        for (Layer l : layers) {
            System.out.println("Loading layer " + l.display_name + " -> " + l.name + " => " + TabulationSettings.environmental_data_path);
            grid = new Grid(
                    layerPath
                    + l.name);
            width = grid.ncols;
            height = grid.nrows;
            System.out.println("WidthxHeight: " + width + "x" + height); 
            double[] d = grid.getGrid();
            if (data == null) {
                data = new float[d.length][layers.length];
            }
            for (i = 0; i < d.length && i < data.length; i++) {
                data[i][j] = (float) d[i];
            }
            j++;
        }

        if (data != null && grid != null) {
            //roll out missing values and values not in region
        	int [][] cells = simpleregion.getOverlapGridCells(
        			grid.xmin, grid.ymin, 
        			grid.xmax, grid.ymax, 
        			grid.ncols, grid.nrows, 
        			null);
        	System.out.println("got cells: " + cells.length);
        	
            int count = 0;
            /* code for non-region
             * for (i = 0; i < data.length; i++) {
                for (j = 0; j < data[i].length; j++) {
                    if (Float.isNaN(data[i][j])) {
                        break;
                    }
                }
                if (j == data[i].length) {
                    count++;
                }
            }*/
            for (i = 0; i < cells.length; i++) {
                for (j = 0; j < data[0].length; j++) {
                    if (Float.isNaN(data[cells[i][0] + (height-cells[i][1]-1)*width][j])) {
                        break;
                    }
                }
                if (j == data[0].length) {
                    count++;
                }
            }
            System.out.println("clean records=" + count);
            float[][] data_clean = new float[count][data[0].length];
            int[] mapping = new int[count];
            count = 0;
            for (i = 0; i < cells.length; i++) {
                for (j = 0; j < data[0].length; j++) {
                	if (Float.isNaN(data[cells[i][0] + (height-cells[i][1]-1)*width][j])) {
                        break;
                    }
                }
                if (j == data[0].length) {
                    for (j = 0; j < data[0].length; j++) {
                        data_clean[count][j] = data[cells[i][0] + (height-cells[i][1]-1)*width][j];
                        mapping[count] = cells[i][0] + (height-cells[i][1]-1)*width;
                    }
                    count++;
                }
            }

            int[] groups = runGowerMetric(data_clean, numberofgroups);

            /* calculate group means */
            double[][] group_means = new double[numberofgroups + 1][data_clean[0].length];
            int[] group_counts = new int[numberofgroups + 1];

            /* TODO: handle numerical overflow */
            for (i = 0; i < groups.length; i++) {
                group_counts[groups[i]]++;
                for (j = 0; j < data_clean[i].length; j++) {
                    group_means[groups[i]][j] += data_clean[i][j];
                }
            }
            for (i = 0; i < group_means.length; i++) {

                /* TODO: this check needs to be removed */
                if (group_counts[i] > 0) {
                    for (j = 0; j < group_means[i].length; j++) {
                        group_means[i][j] /= group_counts[i];
                    }
                }
            }


            /* get RGB for colouring group means */

            /* PCA */
            int[][] colours = PCA.getColours(group_means);
            
            /* export means + colours */
            exportMeansColours(filename + ".csv",group_means,colours,layers);


            /* map back as colours, grey scale for now */
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            int[] image_bytes;

            image_bytes = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                    null, 0, image.getWidth());

            /* TODO: set missing value other than black */

            /* try transparency */            
            for (i = 0; i < image_bytes.length; i++) {
                image_bytes[i] = 0x00000000;
            }
             
            int group;
            int[] colour = new int[3];
            for (i = 0; i < groups.length; i++) {
                //image_bytes[mapping[i]] = (groups[i] * range) / numberofgroups + min; //set blue gradient
                group = groups[i];
                for (j = 0; j < colour.length; j++) {
                    colour[j] = (int) (colours[groups[i]][j]);
                }

                //set up rgb colour for this group
                //image_bytes[mapping[i]] = 0x00000000 & (colour[0] << 16) & (colour[1] << 8) & (colour[0]);
                image_bytes[mapping[i]] = 0xff000000 | ((colour[0] * 255 + colour[1]) * 255 + colour[2]);
            }

            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    image_bytes, 0, image.getWidth());

            try {
                ImageIO.write(image, "png",
                        new File(filename));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return groups;
        }

        return null;
    }
    
    static void exportMeansColours(String filename, double [][] means, int [][] colours, Layer [] layers){
    	try{
    		FileWriter fw = new FileWriter(filename);
    		int i,j;
    		
    		/* header */
    		fw.append("group number");
    		fw.append("red");
    		fw.append("green");
    		fw.append("blue");
    		for(i=0;i<layers.length;i++){
    			fw.append(",");
    			fw.append(layers[i].getDisplay_name());    			
    		}
    		fw.append("\r\n");
    		
    		/* outputs */
    		for(i=0;i<means.length;i++){
    			fw.append(String.valueOf(i));
    			fw.append(",");
    			fw.append(String.valueOf(colours[0]));
    			fw.append(",");
    			fw.append(String.valueOf(colours[1]));
    			fw.append(",");
    			fw.append(String.valueOf(colours[2]));
    			
    			for(j=0;j<means[i].length;j++){
    				fw.append(",");
    				fw.append(String.valueOf(means[i][j]));
    			}
    			
    			fw.append("\r\n");
    		}
    		
    		fw.close();
    	}catch(Exception e){
    		e.printStackTrace();    		
    	}
    }
}
