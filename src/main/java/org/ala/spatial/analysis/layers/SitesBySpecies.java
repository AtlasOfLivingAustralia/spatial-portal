package org.ala.spatial.analysis.layers;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Adam
 */
public class SitesBySpecies {

    Records records;
    int gridSize;
    double resolution;
    double[] bbox;
    int width, height;

    public SitesBySpecies(int gridSize, double resolution, double[] bbox) {
        this.gridSize = gridSize;
        this.resolution = resolution;
        this.bbox = bbox;

        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    void setResolution(double resolution) {
        this.resolution = resolution;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    void setBBox(double[] bbox) {
        this.bbox = bbox;
        width = (int) ((bbox[2] - bbox[0]) / resolution);
        height = (int) ((bbox[3] - bbox[1]) / resolution);
    }

    public void write(Records records, String outputDirectory) throws IOException {
        FileWriter fw = new FileWriter(outputDirectory + "fullSitesBySpecies.csv");

        for (int j = 0; j < 5; j++) {
            if(j > 0) {
                fw.append("\n");
            }
            if(j == 0) {
                 fw.append("LSID,Longitude,Latitude");
            } else if(j == 1) {
                fw.append("Common Name,,");
            } else if(j == 2) {
                fw.append("Kingdom,,");
            } else if(j == 3) {
                fw.append("Family,,");
            } else if(j == 4) {
                fw.append("Species,,");
            }
            for (int i = 0; i < records.lsids.length; i++) {
                fw.append(",\"");
                String[] split = records.lsids[i].split("\\|");
                if(j == 4) {
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
        for (int row = 0; row < height; row++) {
            bsRows[0] = getNextIntArrayRow(records, row, uniqueSpeciesCount, bsRows[0]);
            for (int i = 0; i < width; i++) {
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
    }

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
