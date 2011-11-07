package org.ala.spatial.analysis.layers;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

class GetValuesOccurrencesThread extends Thread {

    LinkedBlockingQueue<Integer> parts;
    CountDownLatch cdl;
    int partSize;
    int[][] cRows;
    float[] values;
    boolean worldwrap;
    int height, width;
    int offset;
    int currentRow;
    int row;

    public GetValuesOccurrencesThread(LinkedBlockingQueue<Integer> parts) {
        this.parts = parts;
    }

    public void set(CountDownLatch cdl, int partSize, int[][] cRows,
            float[] values, boolean worldwrap,
            int height, int width, int offset, int currentRow, int row) {
        this.cdl = cdl;
        this.partSize = partSize;
        this.cRows = cRows;
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
                    int thisCell = 0;
                    int cellcount = 0;
                    for (int j = i - offset; j <= i + offset; j++) {
                        for (int k = currentRow - offset; k <= currentRow + offset; k++) {
                            if (j >= 0 && j < width && k >= 0 && k < height) {
                                cellcount++;
                                thisCell += cRows[k - row][j];
                            } else if (worldwrap && j < 0 && k >= 0 && k < height) {
                                cellcount++;
                                thisCell += cRows[k - row][j + width];
                            } else if (worldwrap && j >= width && k >= 0 && k < height) {
                                cellcount++;
                                thisCell += cRows[k - row][j - width];
                            }
                        }
                    }
                    if (cellcount == 0) {
                        values[i] = 0;
                    } else {
                        values[i] = thisCell / (float) cellcount;
                    }
                }
                cdl.countDown();
            }
        } catch (InterruptedException e) {
        }
    }
}
