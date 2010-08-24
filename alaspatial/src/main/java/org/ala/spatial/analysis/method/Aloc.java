package org.ala.spatial.analysis.method;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.SpatialLogger;

/**
 * ALOC
 * 
 * 
 * @author adam
 *
 */
public class Aloc {

    /**
     * ALOC: Gower Metric measure
     */
    public static int[] runGowerMetric(float[][] data, int nNoOfGroups) {

        int nCols = data[0].length;
        int nRows = data.length;

        float[] col_min = new float[nCols];
        float[] col_max = new float[nCols];
        float[] col_range = new float[nCols];

        int[] seedidx = new int[nNoOfGroups + 1];

        int seedidxsize = 0;
        int i, j;
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

            (new SpatialLogger()).log("seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups);

            for (i = 0; i < nRows; i++) {
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
                        //error
                        missing--;
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
        }

        float[] seeds = new float[seedidxsize * nCols];
        float[] seedgroup_nonmissingvalues = new float[seedidxsize * nCols];

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
            (new SpatialLogger()).log("moving (" + iteration + ") > moved " + movement);

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
   
    public static int[] runGowerMetricThreaded(ArrayList<Object> data_pieces, int nNoOfGroups, int nCols, int pieces) {
        int[] rowCounts = new int[pieces];
        int nRowsTotal = 0;
        for (int i = 0; i < pieces; i++) {
            rowCounts[i] = ((float[]) data_pieces.get(i)).length / nCols;
            if (i > 0) {
                rowCounts[i] += rowCounts[i - 1];
            }
        }
        nRowsTotal += rowCounts[pieces - 1];
        int nRows;


        int min_movement = -1;
        int[] min_groups = new int[nRowsTotal];
        float[] min_dists = new float[nRowsTotal];


        float[] col_min = new float[nCols];
        float[] col_max = new float[nCols];
        float[] col_range = new float[nCols];

        int seedidxsize = 0;
        int i, j, k;
        for (i = 0; i < nCols; i++) {
            col_min[i] = Float.MAX_VALUE;
            col_max[i] = Float.MIN_VALUE;
        }
        for (k = 0; k < pieces; k++) {
            float[] data = (float[]) data_pieces.get(k);
            for (i = 0; i < nCols; i++) {
                nRows = data.length / nCols;
                for (j = 0; j < nRows; j++) {
                    float f = data[i + j * nCols];
                    if (Float.isNaN(col_min[i]) || col_min[i] > f) {
                        col_min[i] = f;
                    }
                    if (Float.isNaN(col_max[i]) || col_max[i] < f) {
                        col_max[i] = f;
                    }
                }
            }
        }
        for (i = 0; i < nCols; i++) {
            col_range[i] = col_max[i] - col_min[i];
        }
        for (k = 0; k < pieces; k++) {
            float[] data = (float[]) data_pieces.get(k);
            for (i = 0; i < nCols; i++) {
                nRows = data.length / nCols;
                for (j = 0; j < nRows; j++) {
                    data[i + j * nCols] = (data[i + j * nCols] - col_min[i]) / col_range[i];
                }
            }
        }

        //1. determine correct # of groups by varying radius
        float start_radius = 1;
        float radius = start_radius;
        float step = radius / 2.0f;

        int count = 0;
        int[] seedidx = new int[nNoOfGroups + 1000]; //TODO: check on upper limit for number of groups
        float[] seeds = new float[nCols * (1000 + nNoOfGroups)]; //space for an extra 1000 groups during seeding

        //initial seed as first record
        {
            seedidx[0] = 0;
            seedidxsize = 1;
            float[] data = (float[]) data_pieces.get(0);
            for (i = 0; i < nCols; i++) {
                seeds[i] = data[i];
            }
        }

        int c;
        while (seedidxsize != nNoOfGroups && count < 25) {
            int rowPos = 0;
            for (c = 0; c < pieces; c++) {
                float[] data = (float[]) data_pieces.get(c);
                nRows = data.length / nCols;
                rowPos = rowCounts[c] - nRows;
                for (i = 0; i < nRows; i++, rowPos++) {
                    for (j = 0; j < seedidxsize; j++) {
                        //calc dist between obj(i) & obj(seedidx(j))
                        float dist = 0;
                        int missing = 0;
                        for (k = 0; k < nCols; k++) {
                            float v1 = data[i * nCols + k];
                            float v2 = seeds[j * nCols + k];

                            if (Float.isNaN(v1) || Float.isNaN(v2) || col_range[k] == 0) {
                                missing++;
                            } else {
                                dist += java.lang.Math.abs(v1 - v2);
                            }
                        }

                        //add to seedidx if distance > radius
                        if (nCols == missing) {
                            //error
                            missing--;
                        }

                        dist = dist / (float) (nCols - missing);
                        if (dist < radius) {
                            break;
                        }
                    }
                    if (j == seedidxsize) {
                        seedidx[seedidxsize] = rowPos;
                        for (k = 0; k < nCols; k++) {
                            seeds[seedidxsize * nCols + k] = data[i * nCols + k];
                        }
                        seedidxsize++;
                    }

                    if (seedidxsize > nNoOfGroups) {
                        break;
                    }
                }

                //repeat break if necessary
                if (seedidxsize > nNoOfGroups) {
                    break;
                }
            }
            count++; //force a break

            if (seedidxsize == nNoOfGroups) {
                continue;
            }

            //PERFORM RECONCILIATION OF NUMBER OF GROUPS IF count >= 25
            if (count < 25) {
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

            (new SpatialLogger()).log("seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);
        }
        (new SpatialLogger()).log("seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);

        int threadcount = Runtime.getRuntime().availableProcessors();

        seeds = java.util.Arrays.copyOf(seeds, seedidxsize * nCols);//new float[seedidxsize * nCols];
        int[] seedgroup_nonmissingvalues = new int[seedidxsize * nCols];

        ArrayList<float[]> seeds_adjustments = new ArrayList<float[]>(threadcount);
        ArrayList<int[]> seeds_nmv_adjustments = new ArrayList<int[]>(threadcount);
        ArrayList<int[]> seeds_groupsize_adjustments = new ArrayList<int[]>(threadcount);
        ArrayList<int[]> records_movement = new ArrayList<int[]>(threadcount);
        for (i = 0; i < threadcount; i++) {
            seeds_adjustments.add(new float[seedidxsize * nCols]);
            seeds_nmv_adjustments.add(new int[seedidxsize * nCols]);
            seeds_groupsize_adjustments.add(new int[seedidxsize]);
            records_movement.add(new int[1]);
        }

        //2. allocate all objects to a group
        short[] groups = new short[nRowsTotal];
        float[] groups_dist = new float[nRowsTotal];
        int[] groupsize = new int[seedidxsize];
        for (i = 0; i < seedidxsize; i++) {
            groupsize[i] = 0;
            int rowPos = 0;
            for (int n = 0; n < pieces; n++) {
                float[] data = (float[]) data_pieces.get(n);
                nRows = data.length / nCols;
                rowPos = rowCounts[n] - nRows;
                if (seedidx[i] >= rowPos && seedidx[i] < rowCounts[n]) {
                    for (j = 0; j < nCols; j++) {
                        seeds[i * nCols + j] = data[(seedidx[i] - rowPos) * nCols + j];
                        seedgroup_nonmissingvalues[i * nCols + j] = 0;
                    }
                    break;
                }
            }
        }
        for (i = 0; i < nRowsTotal; i++) {
            groups[i] = -1;
        }

        //setup main loop
        ArrayList<int[]> record_spans = new ArrayList(pieces);
        for (i = 0; i < pieces; i++) {
            int[] r = new int[2];
            r[0] = i;
            r[1] = rowCounts[i];
            record_spans.add(r);
        }
        LinkedBlockingQueue<int[]> lbq = new LinkedBlockingQueue(record_spans);

        AlocInnerLoop2[] ail = new AlocInnerLoop2[threadcount];

        for (i = 0; i < threadcount; i++) {
            ail[i] = new AlocInnerLoop2(
                    lbq, data_pieces, nCols, col_range, seedidxsize, seeds, seedgroup_nonmissingvalues, groups, seeds_adjustments.get(i), seeds_nmv_adjustments.get(i), seeds_groupsize_adjustments.get(i), records_movement.get(i));
        }

        System.out.println("Started AlocInnerLoops (" + threadcount + " threads): " + System.currentTimeMillis());

        int iteration = 0;
        int movement = -1;
        while (movement != 0 && iteration < 100) {
            (new SpatialLogger()).log("moving (" + iteration + ") > moved " + movement);

            //preserve first element from each group
            //- ok for first iteration since centroid movement occurs at end
            int[] preserved_members = new int[seedidxsize];
            if (iteration > 0) {
                for (i = 0; i < seedidxsize; i++) {
                    preserved_members[i] = -1;
                }
                int count_preserved = 0;
                for (i = 0; i < nRowsTotal && count_preserved < seedidxsize; i++) {
                    if (preserved_members[groups[i]] == -1) {
                        preserved_members[groups[i]] = i;
                        count_preserved++;
                    }
                }
            }

            if (record_spans.size() == 0) {
                System.out.print("0");
            }

            //rebuild sync'ed spans
            for (i = 0; i < pieces; i++) {
                int[] r = new int[2];
                r[0] = i;
                r[1] = rowCounts[i];
                lbq.add(r);
            }

            //run
            for (i = 0; i < threadcount; i++) {
                //ail[i].start();
                ail[i].next();
            }

            //run main loop
            try {
                boolean alive = true;
                while (alive) {
                    Thread.sleep(20);
                    alive = false;
                    for (i = 0; i < threadcount; i++) {
                        if (!ail[i].isSleeping()) {
                            alive = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //update seeds and nonmissingvalues counts
            if (iteration == 0) {
                for (i = 0; i < seedidxsize; i++) {
                    for (j = 0; j < nCols; j++) {
                        seeds[i * nCols + j] = Float.NaN;
                    }
                }
            }
            movement = 0;
            for (i = 0; i < threadcount; i++) {
                movement += records_movement.get(i)[0];
                float[] seed_adj = seeds_adjustments.get(i);
                int[] seed_nmv_adj = seeds_nmv_adjustments.get(i);

                for (j = 0; j < seeds.length; j++) {
                    if (Float.isNaN(seeds[j])
                            && seed_adj[j] != 0) {
                        seeds[j] = seed_adj[j];
                    } else {
                        seeds[j] += seed_adj[j];
                    }
                    seedgroup_nonmissingvalues[j] += seed_nmv_adj[j];
                }
            }

            //update group sizes
            for (i = 0; i < threadcount; i++) {
                int[] seed_gs_adj = seeds_groupsize_adjustments.get(i);
                for (j = 0; j < seedidxsize; j++) {
                    groupsize[j] += seed_gs_adj[j];
                }
            }

            //enforce minimum group membership of size 1
            if (iteration > 0) {
                boolean repeat = true;
                while (repeat) {
                    repeat = false;

                    //check
                    for (i = 0; i < seedidxsize; i++) {
                        if (groupsize[i] == 0) {
                            repeat = true;

                            //move original member back here
                            //old group == cg, new group == i
                            //row == preserved_members[i]
                            int row = preserved_members[i];
                            int cg = groups[row];

                            groupsize[i]++;
                            groupsize[cg]--;

                            //update seeds
                            float v1;
                            float[] data = null;
                            for (k = 0; k < pieces; k++) {
                                if (row < rowCounts[k]) {
                                    data = (float[]) data_pieces.get(k);
                                    row = row - (rowCounts[k] - data.length / nCols);
                                    break;
                                }
                            }
                            for (j = 0; j < nCols; j++) {
                                v1 = data[row * nCols + j];

                                if (!Float.isNaN(v1)) {
                                    //old seed
                                    seeds[cg * nCols + j] -= v1;
                                    seedgroup_nonmissingvalues[cg * nCols + j]--;

                                    //new seed
                                    seedgroup_nonmissingvalues[i * nCols + j]++;
                                } else {
                                    i = i + 1 - 1;
                                }
                                //new seed
                                seeds[i * nCols + j] = v1;
                            }
                        }
                    }
                }
            }

            //backup min_movement
            if (min_movement == -1 || min_movement > movement) {
                min_movement = movement;
                //copy groups to min_groups
                for (k = 0; k < nRowsTotal; k++) {
                    min_groups[k] = groups[k];
                    min_dists[k] = groups_dist[k];
                }
            }

            //test for -1 group allocations here

            iteration++;
        }

        //write-back row groups
        return min_groups;
    }
}

/**
 * for data_pieces
 * 
 * @author Adam
 */
class AlocInnerLoop2 extends Thread {

    Thread t;
    LinkedBlockingQueue<int[]> lbq;
    int step;
    int[] target;
    ArrayList<Object> dataPieces;
    int nCols;
    float[] col_range;
    int seedidxsize;
    float[] seeds;
    float[] seeds_adjustment;
    int[] seedgroup_nonmissingvalues;
    short[] groups;
    int[] seeds_nmv_adjustment;
    int[] groupsize;
    int[] records_movement;
    Boolean sleeping;

    public AlocInnerLoop2(LinkedBlockingQueue<int[]> lbq_, ArrayList<Object> dataPieces_, int nCols_, float[] colrange_, int seedidxsize_, float[] seeds_, int[] seedgroup_nonmissingvalues_, short[] groups_, float[] seeds_adjustment_, int[] seeds_nmv_adjustment_, int[] groupsize_, int[] records_movement_) {
        lbq = lbq_;
        dataPieces = dataPieces_;
        nCols = nCols_;
        col_range = colrange_;
        seedidxsize = seedidxsize_;
        seeds = seeds_;
        seedgroup_nonmissingvalues = seedgroup_nonmissingvalues_;
        groups = groups_;
        seeds_adjustment = seeds_adjustment_;
        seeds_nmv_adjustment = seeds_nmv_adjustment_;
        groupsize = groupsize_;
        records_movement = records_movement_;

        sleeping = new Boolean(false);
    }

    public void run() {
        while (true) {
            // get next batch to operate on
            int[] next;

            try {
                //reset seeds_adjustment;
                for (int i = 0; i < seeds_adjustment.length; i++) {
                    seeds_adjustment[i] = 0;
                    seeds_nmv_adjustment[i] = 0;
                }

                //reset group size adjustment;
                for (int i = 0; i < groupsize.length; i++) {
                    groupsize[i] = 0;
                }

                //reset movement
                records_movement[0] = 0;

                //actual loop
                synchronized (lbq) {
                    if (lbq.size() > 0) {
                        next = lbq.take();
                    } else {
                        next = null;
                    }
                }

                while (next != null) {
                    // run
                    alocInnerLoop(next);

                    // get next available
                    synchronized (lbq) {
                        if (lbq.size() > 0) {
                            next = lbq.take();
                        } else {
                            next = null;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (sleeping) {
                sleeping = true;
            }
            while (true) {
                try {
                    this.sleep(100000);
                } catch (Exception e) {
                    break;
                }
            }
            synchronized (sleeping) {
                sleeping = false;
            }
        }
    }

    public boolean isSleeping() {
        boolean s;
        synchronized (sleeping) {
            s = sleeping;
        }
        return s;
    }

    public void next() {
        if (isAlive()) {
            this.interrupt();
        } else {
            start();
        }
    }

    private void alocInnerLoop(int[] next) {
        int i, j, k;
        float min_dist = 0.00001f;
        int min_idx = 0;
        float dist;
        int missing;
        float v2;
        float v1;

        float[] data = (float[]) dataPieces.get(next[0]);
        int nRows = data.length / nCols;
        int rowOffset = next[1] - nRows;

        for (i = 0; i < nRows; i++) {
            for (j = 0; j < seedidxsize; j++) {
                //calc dist between obj(i) & obj(seeds(j))
                dist = 0;
                missing = 0;
                for (k = 0; k < nCols; k++) {
                    v1 = data[i * nCols + k];
                    v2 = seeds[j * nCols + k];
                    if (Float.isNaN(v1) || Float.isNaN(v2) || col_range[k] == 0) {
                        missing++;
                    } else {
                        if (seedgroup_nonmissingvalues[j * nCols + k] > 0) {
                            v2 = v2 / seedgroup_nonmissingvalues[j * nCols + k];
                        }
                        dist += java.lang.Math.abs(v1 - v2);//range == 1 (standardized 0-1); / (float) col_range[k];
                    }
                }
                dist = dist / (float) (nCols - missing);
                if (j == 0 || min_dist > dist) {
                    min_dist = dist;
                    min_idx = j;
                }
            }
            //add this group to group min_idx;
            if (groups[i + rowOffset] != (short) min_idx) {
                records_movement[0]++;

                //remove from previous group
                if (groups[i + rowOffset] >= 0) {
                    groupsize[groups[i + rowOffset]]--;
                    for (j = 0; j < nCols; j++) {
                        if (!Float.isNaN(data[i * nCols + j])) {
                            seeds_adjustment[groups[i + rowOffset] * nCols + j] -= data[i * nCols + j];
                            seeds_nmv_adjustment[groups[i + rowOffset] * nCols + j]--;
                        }
                    }
                }

                //reassign group
                groups[i + rowOffset] = (short) min_idx;

                //add to new group
                groupsize[min_idx]++;

                for (j = 0; j < nCols; j++) {
                    if (!Float.isNaN(data[i * nCols + j])) {
                        seeds_adjustment[min_idx * nCols + j] += data[i * nCols + j];
                        seeds_nmv_adjustment[min_idx * nCols + j]++;
                    }
                }
            }
        }
    }
}
