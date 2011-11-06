package org.ala.spatial.analysis.layers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

/**
 *
 * @author Adam
 */
public class SpeciesDensity {

    Records records;
    int gridSize;
    double resolution;
    double[] bbox;
    int width, height;

    public SpeciesDensity(int gridSize, double resolution, double[] bbox) {
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

    public void write(Records records, String outputDirectory, String filename) throws IOException {
        if(filename == null) {
            filename = "_species_density_av_" + gridSize + "x" + gridSize + "_" + String.valueOf(resolution).replace(".","");
        }
        //write data
        RandomAccessFile raf = new RandomAccessFile(outputDirectory + filename + ".gri", "rw");
        byte[] bytes = new byte[4 * width];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.mark();

        FileWriter fw = new FileWriter(outputDirectory + filename + ".asc");
        fw.append("ncols " + width + "\n"
                + "nrows " + height + "\n"
                + "xllcorner " + bbox[0] + "\n"
                + "yllcorner " + bbox[1] + "\n"
                + "cellsize " + resolution + "\n"
                + "NODATA_value -9999\n");

        int uniqueSpeciesCount = records.getSpeciesSize();
        
        BitSet [][] bsRows = new BitSet[gridSize][];
        BitSet thisCell = new BitSet(uniqueSpeciesCount);
        int cellcount = 0;
        double max = 0;
        for(int row = 0;row < height; row++) {
            //get rows
            if(row == 0) {
                for(int i=0;i<gridSize;i++) {
                    //System.out.println("getting bitset row " + (row + i) + " or " + height);
                    bsRows[i] = getNextBitSetRow(records, row + i, uniqueSpeciesCount, null);
                }
            } else {
                for(int i=0;i<gridSize && row + i < height;i++) {
                    if(i + 1 < bsRows.length) {
                        bsRows[i] = bsRows[i+1];
                    } else {
                        //System.out.println("getting bitset row " + (row + i) + " or " + height);
                        bsRows[i] = getNextBitSetRow(records, row + i, uniqueSpeciesCount, null);
                    }
                }
            }

            //operate on current row
            int startRow = (row==0)?0:row + gridSize / 2; //gridSize is odd
            int endRow = (row == height-1)?height-1:row + gridSize / 2; //gridSize is odd

            for(int currentRow=startRow;currentRow<=endRow;currentRow++) {
                bb.reset();

                int offset = gridSize/2;
                for(int i=0;i<width;i++) {
                    thisCell.clear();
                    cellcount = 0;
                    for(int j=i-offset;j<=i+offset;j++) {
                        for(int k=currentRow-offset;k<=currentRow+offset;k++) {
                            if(j>=0 && j<width && k>= 0 && k<height) {
                                cellcount++;
                                thisCell.or(bsRows[k-row][j]);
                            }
                        }
                    }
                    if(i > 0) {
                        fw.append(" ");
                    }
                    if(cellcount > 0) {
                        //writeCell(i,thisCell.cardinality()/(double)cellcount);
                        float value = thisCell.cardinality()/(float)cellcount;
                        if(max < value) max = value;
                        bb.putFloat((float) value);
                        fw.append(String.valueOf(value));
                    } else {
                        //writeCell(i,0);
                        fw.append("0");
                    }
                }
                raf.write(bytes);
                fw.append("\n");
                //System.out.println("wrote out row " + currentRow);
            }
        }

        raf.close();
        fw.close();

        DensityLayers.writeHeader(outputDirectory + filename + ".grd", resolution, height, width, bbox[0], bbox[1], bbox[2], bbox[3], 0, max, gridSize);
    }

    BitSet[] getNextBitSetRow(Records records, int row, int uniqueSpeciesCount, BitSet[] bs) {        
        //translate into bitset for each grid cell
        if (bs == null) {
            bs = new BitSet[width];
            for (int i = 0; i < width; i++) {
                bs[i] = new BitSet(uniqueSpeciesCount);
            }
        } else {
            for (int i = 0; i < width; i++) {
                bs[i].clear();
            }
        }

        for (int i = 0; i < records.getRecordsSize(); i++) {
            int y = height - 1 - (int) ((records.getLatitude(i) - bbox[1]) / resolution);

            if (y == row) {
                int x = (int) ((records.getLongitude(i) - bbox[0]) / resolution);

                if (x >= 0 && x < width) {
                    bs[x].set(records.getSpeciesNumber(i));
                }
            }
        }

        return bs;
    }
}
