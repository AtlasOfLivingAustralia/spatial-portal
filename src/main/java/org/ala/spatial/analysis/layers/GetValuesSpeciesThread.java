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

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread operating to calculate species richness, with a moving average, grid
 * values from a list of parts.
 *
 * @author Adam
 */
class GetValuesSpeciesThread extends Thread {

    /**
     * List of parts that need a calculation, shared with other threads.
     */
    LinkedBlockingQueue<Integer> parts;
    /**
     * Countdown for finished parts.
     */
    CountDownLatch cdl;
    /**
     * current part size.
     */
    int partSize;
    /**
     * source rows data.
     */
    BitSet[][] bsRows;
    /**
     * current cell. Supplied instead of constructed each use. Unique to this
     * object.
     */
    BitSet thisCell;
    /**
     * for output values.
     */
    float[] values;
    /**
     * does world wrap apply.
     */
    boolean worldwrap;
    /**
     * dimensions of output grid.
     */
    int height, width;
    /**
     * current offset.
     */
    int offset;
    /**
     * current row.
     */
    int currentRow;
    /**
     * active row.
     */
    int row;

    /**
     * Construct with a list of parts.
     *
     * Must use function 'set' before running this thread.
     *
     * @param parts
     */
    public GetValuesSpeciesThread(LinkedBlockingQueue<Integer> parts) {
        this.parts = parts;
    }

    /**
     * Setup for this iteration.
     *
     * @param cdl
     * @param partSize
     * @param bsRows
     * @param thisCell
     * @param values
     * @param worldwrap
     * @param height
     * @param width
     * @param offset
     * @param currentRow
     * @param row
     */
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
                    //apply moving average
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
