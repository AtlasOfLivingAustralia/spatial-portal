package org.ala.spatial.analysis.layers;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

class GetValuesSpeciesThread extends Thread {

    LinkedBlockingQueue<Integer> parts;
    CountDownLatch cdl;
    int partSize;
    BitSet[][] bsRows;
    BitSet thisCell;
    float[] values;
    boolean worldwrap;
    int height, width;
    int offset;
    int currentRow;
    int row;

    public GetValuesSpeciesThread(LinkedBlockingQueue<Integer> parts) {
        this.parts = parts;
    }

    public void set(CountDownLatch cdl, int partSize, BitSet[][] bsRows,
            BitSet thisCell, float[] values, boolean worldwrap,
            int height, int width, int offset, int currentRow, int row) {
        this.cdl = cdl;
        this.partSize = partSize;
        this.bsRows = bsRows;
        this.thisCell = thisCell;
        this.values = values;
        this.worldwrap = worldwrap;
        this.height = height;
        this.width = width;
        this.offset = offset;
        this.currentRow = currentRow;
        this.row = row;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int n = parts.take();
                int len = Math.min((n + 1) * partSize, width);
                for (int i = n * partSize; i < len; i++) {
                    thisCell.clear();
                    int cellcount = 0;
                    for (int j = i - offset; j <= i + offset; j++) {
                        for (int k = currentRow - offset; k <= currentRow + offset; k++) {
                            if (j >= 0 && j < width && k >= 0 && k < height) {
                                cellcount++;
                                thisCell.or(bsRows[k - row][j]);
                            } else if (worldwrap && j < 0 && k >= 0 && k < height) {
                                cellcount++;
                                thisCell.or(bsRows[k - row][j + width]);
                            } else if (worldwrap && j >= width && k >= 0 && k < height) {
                                cellcount++;
                                thisCell.or(bsRows[k - row][j - width]);
                            }
                        }
                    }
                    if (cellcount == 0) {
                        values[i] = 0;
                    } else {
                        values[i] = thisCell.cardinality() / (float) cellcount;
                    }
                }
                cdl.countDown();
            }
        } catch (InterruptedException e) {
        }
    }
}
