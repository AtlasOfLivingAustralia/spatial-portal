/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.analysis.layers;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.ala.layers.intersect.Grid;
import org.ala.layers.intersect.SimpleRegion;

/**
 * Generate a sites by species table.
 *
 * @author Adam
 */
public class SitesBySpecies {

    /**
     * all occurrence records for this occurrence density grid.
     */
    Records records;
    /**
     * output grid resolution as decimal degrees.
     */
    double resolution;
    /**
     * output grid bounds as xmin,ymin,xmax,ymax.
     */
    double[] bbox;
    /**
     * output grid dimensions.
     */
    int width, height;

    public SitesBySpecies(double resolution, double[] bbox) {
        this.resolution = resolution;
        this.bbox = bbox;

        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    /**
     * @param resolution
     */
    void setResolution(double resolution) {
        this.resolution = resolution;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    /**
     * @param bbox
     */
    void setBBox(double[] bbox) {
        this.bbox = bbox;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    /**
     * Generate and write the sites by species list.
     * <p/>
     * Output file is named "SitesBySpecies.csv"
     *
     * @param records         all occurrence records for this density grid as Records.
     * @param outputDirectory path to the output directory for the list.
     * @param region          area restriction, or null for everywhere the occurrences
     *                        appear.
     * @param envelopeGrid    area restriction as an envelope grid, or null for
     *                        everywhere the occurrences appear.
     * @return array as int[] with { grid cells with an occurrence, grid cells
     * in the area }
     * @throws IOException
     */
    public int[] write(Records records, String outputDirectory, SimpleRegion region, Grid envelopeGrid) throws IOException {
        FileWriter fw = new FileWriter(outputDirectory + "fullSitesBySpecies.csv");

        for (int j = 0; j < 5; j++) {
            if (j > 0) {
                fw.append("\n");
            }
            if (j == 0) {
                fw.append("LSID,Longitude,Latitude");
            } else if (j == 1) {
                fw.append("Common Name,Longitude,Latitude");
            } else if (j == 2) {
                fw.append("Kingdom,Longitude,Latitude");
            } else if (j == 3) {
                fw.append("Family,Longitude,Latitude");
            } else if (j == 4) {
                fw.append("Species,Longitude,Latitude");
            }
            for (int i = 0; i < records.lsids.length; i++) {
                fw.append(",\"");
                String[] split = records.lsids[i].split("\\|");
                if (j == 4) {
                    if (split.length > 0) {
                        fw.append(split[0].replace("\"", "\"\""));
                    }
                } else {
                    if (split.length > j + 1) {
                        fw.append(split[j + 1].replace("\"", "\"\""));
                    }
                }
                fw.append("\"");
            }
        }

        int uniqueSpeciesCount = records.getSpeciesSize();

        int[] totalSpeciesCounts = new int[uniqueSpeciesCount];

        int[][][] bsRows = new int[1][][];
        int[] counts = new int[2];
        for (int row = 0; row < height; row++) {
            bsRows[0] = getNextIntArrayRow(records, row, uniqueSpeciesCount, bsRows[0]);
            for (int i = 0; i < width; i++) {

                double longitude = (row + 0.5) * resolution + bbox[0];
                double latitude = (i + 0.5) * resolution + bbox[1];
                if ((region == null || region.isWithin_EPSG900913(longitude, latitude))
                        && (envelopeGrid == null || envelopeGrid.getValues2(new double[][]{{longitude, latitude}})[0] > 0)) {

                    int sum = 0;
                    for (int n = 0; n < uniqueSpeciesCount; n++) {
                        sum += bsRows[0][i][n];
                        totalSpeciesCounts[n] += bsRows[0][i][n];
                    }

                    if (sum > 0) {
                        fw.append("\n\"");
                        fw.append(String.valueOf(i * resolution + this.bbox[0]));
                        fw.append("_");
                        fw.append(String.valueOf(row * resolution + this.bbox[1]));
                        fw.append("\",");
                        fw.append(String.valueOf(i * resolution + this.bbox[0]));
                        fw.append(",");
                        fw.append(String.valueOf(row * resolution + this.bbox[1]));
                        for (int n = 0; n < uniqueSpeciesCount; n++) {
                            fw.append(",");
                            fw.append(String.valueOf(bsRows[0][i][n]));
                        }
                        counts[0]++;
                    }
                    counts[1]++;
                }
            }
        }
        fw.close();

        //remove columns without species
        fw = new FileWriter(outputDirectory + "SitesBySpecies.csv");
        CSVReader r = new CSVReader(new FileReader(outputDirectory + "fullSitesBySpecies.csv"));
        String[] line;
        int row = 0;
        while ((line = r.readNext()) != null) {
            for (int i = 0; i < line.length; i++) {
                if (i < 3 || totalSpeciesCounts[i - 3] > 0) {
                    if (i > 0) {
                        fw.append(",");
                    }

                    if (row == 0) {
                        fw.append("\"");
                        fw.append(line[i].replace("\"", "\"\""));
                        fw.append("\"");
                    } else {
                        if (i == 0) {
                            fw.append("\n");
                            fw.append("\"");
                            fw.append(line[i].replace("\"", "\"\""));
                            fw.append("\"");
                        } else {
                            fw.append(line[i]);
                        }
                    }
                }
            }
            row++;
        }
        r.close();
        fw.close();

        new File(outputDirectory + "fullSitesBySpecies.csv").delete();

        return counts;
    }

    /**
     * get data for the next row.
     *
     * @param records
     * @param row
     * @param uniqueSpeciesCount
     * @param bs
     * @return
     */
    int[][] getNextIntArrayRow(Records records, int row, int uniqueSpeciesCount, int[][] bs) {
        //translate into bitset for each grid cell
        if (bs == null) {
            bs = new int[width][uniqueSpeciesCount];
        } else {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < uniqueSpeciesCount; j++) {
                    bs[i][j] = 0;
                }
            }
        }

        for (int i = 0; i < records.getRecordsSize(); i++) {
            int y = (int) ((records.getLatitude(i) - bbox[1]) / resolution);

            if (y == row) {
                int x = (int) ((records.getLongitude(i) - bbox[0]) / resolution);

                if (x >= 0 && x < width) {
                    bs[x][records.getSpeciesNumber(i)]++;
                }
            }
        }

        return bs;
    }
}
