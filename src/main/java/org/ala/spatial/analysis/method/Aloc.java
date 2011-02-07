package org.ala.spatial.analysis.method;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.AnalysisJobAloc;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * ALOC
 *
 *
 * @author adam
 *
 */
public class Aloc {

    /**
     * produces group allocations by ALOC with gower metric measure
     *
     * - reduces iteration duration dependance on #groups
     * - increases memory usage
     *  from
     *      #cells*#layers
     *  to
     *      #cells*#layers + #cells*#groups
     *
     * @param data_pieces
     * @param nNoOfGroups
     * @param nCols
     * @param pieces
     * @param job
     * @return
     */
    public static int[] runGowerMetricThreadedMemory(ArrayList<Object> data_pieces, int nNoOfGroups, int nCols, int pieces, AnalysisJobAloc job) {

        if(job != null) job.setStage(1);    //seeding stage
        if(job != null && job.isCancelled()) return null;


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
        double[] min_dists = new double[nRowsTotal];

        //range standardize columns 0-1
        float[] col_min = new float[nCols];
        float[] col_max = new float[nCols];
        double[] col_range = new double[nCols];
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
                    data[i + j * nCols] = (float)((data[i + j * nCols] - col_min[i]) / col_range[i]);
                }
            }
        }

        //1. determine correct # of groups by varying radius
        double start_radius = 1;
        double radius = start_radius;
        double step = radius / 2.0f;

        int count = 0;
        int[] seedidx = new int[nNoOfGroups + 1000]; //TODO: check on upper limit for number of groups
        double[] seeds = new double[nCols * (1000 + nNoOfGroups)]; //space for an extra 1000 groups during seeding
        double[] oldSeeds = new double[nCols * (1000 + nNoOfGroups)];
        int[] oldCount = new int[nCols * (1000 + nNoOfGroups)];


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
                        double dist = 0;
                        int missing = 0;
                        for (k = 0; k < nCols; k++) {
                            double v1 = data[i * nCols + k];
                            double v2 = seeds[j * nCols + k];

                            if (Double.isNaN(v1) || Double.isNaN(v2) || col_range[k] == 0) {
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

                        dist = dist / (double) (nCols - missing);
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

            if(job != null) job.setProgress(count / 25.0,"seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);
            if(job != null && job.isCancelled()) return null;
            SpatialLogger.log("seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);
        }
        if(job != null) job.setProgress(count / 25.0,"seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);
        if(job != null && job.isCancelled()) return null;
        SpatialLogger.log("seeding (" + count + ") " + seedidxsize + " != " + nNoOfGroups + " radius:" + radius);

        if(job != null) job.setStage(2); //iterations

        int threadcount = TabulationSettings.analysis_threads;

        //setup piece data
        List apdList = java.util.Collections.synchronizedList(new ArrayList());
        for(i=0;i<pieces;i++){
            int rowcount = ((float[]) data_pieces.get(i)).length / nCols;
            apdList.add(new AlocPieceData(
                    (float[]) data_pieces.get(i),
                    new double[rowcount * seedidxsize],
                    new short[rowcount],
                    new double[rowcount]));
        }
        
        //setup shared data
        seeds = java.util.Arrays.copyOf(seeds, seedidxsize * nCols);
        int [] seedgroup_nonmissingvalues = new int[seedidxsize * nCols];
        double [] otherGroupMovement = new double[seedidxsize];
        double [] groupMovement = new double[seedidxsize];

        AlocSharedData [] asdCopies = new AlocSharedData[threadcount];
        for(i=0;i<threadcount;i++){
            asdCopies[i] = new AlocSharedData(
                    otherGroupMovement.clone(),
                    groupMovement.clone(),
                    nCols,
                    col_range.clone(),
                    seedidxsize,
                    seeds.clone(),
                    seedgroup_nonmissingvalues.clone()
                    );
        }

        //setup thread data
        AlocThreadData [] atdArray = new AlocThreadData[threadcount];
        for(i=0;i<threadcount;i++){
            atdArray[i] = new AlocThreadData(
                    new int[seedidxsize], new int[seedidxsize * nCols],
            new double[seedidxsize * nCols]
                    );
        }

        //2. allocate all objects to a group
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
        for (i = 0; i < pieces; i++) {
            AlocPieceData apd = (AlocPieceData)apdList.get(i);
            for(j=0;j<apd.groups.length;j++){
                apd.groups[j] = -1;
            }
        }

        LinkedBlockingQueue<AlocPieceData> lbq = new LinkedBlockingQueue(new ArrayList<AlocPieceData>());

        AlocInnerLoop3[] ail = new AlocInnerLoop3[threadcount];
        for (i = 0; i < threadcount; i++) {
            ail[i] = new AlocInnerLoop3(lbq, atdArray[i], asdCopies[i]);
        }

        System.out.println("Started AlocInnerLoops (" + threadcount + " threads): " + System.currentTimeMillis());

        int iteration = 0;
        int movement = -1;
        while (movement != 0 && iteration < 100) {
            SpatialLogger.log("moving (" + iteration + ") > moved " + movement);

            //preserve first element from each group
            //- ok for first iteration since centroid movement occurs at end
            int[] preserved_members = new int[seedidxsize];
            if (iteration > 0) {
                for (i = 0; i < seedidxsize; i++) {
                    preserved_members[i] = -1;
                }
                int count_preserved = 0;
                int pos = 0;
                for (i = 0; i < pieces && count_preserved < seedidxsize; i++) {
                    short [] grps = ((AlocPieceData)apdList.get(i)).groups;
                    for(j=0;j<grps.length;j++,pos++){
                        if (preserved_members[grps[j]] == -1) {
                            preserved_members[grps[j]] = pos;
                            count_preserved++;
                        }
                    }
                }
            }

            //get copies of shared data
            if(iteration == 0){
                for(i=0;i<threadcount;i++){
                    asdCopies[i].otherGroupMovement = otherGroupMovement;
                    asdCopies[i].groupMovement = groupMovement;
                    asdCopies[i].seeds = seeds;
                    asdCopies[i].seedgroup_nonmissingvalues = seedgroup_nonmissingvalues;
                }
            }
            //rebuild spans
            CountDownLatch cdl = new CountDownLatch(pieces);
            for (i = 0; i < threadcount; i++) {
                ail[i].next(cdl);
            }
            for (i = 0; i < pieces; i++) {                
                lbq.add((AlocPieceData)apdList.get(i));
            }

            //wait for pieces to be finished
            try {
                cdl.await();
            } catch (Exception e) {
                e.printStackTrace();
            }

             //init (for reducing distance checks)
            for(i=0;i<seedidxsize * nCols;i++){
                oldSeeds[i] = seeds[i];
                oldCount[i] = seedgroup_nonmissingvalues[i];
            }

            //update seeds and nonmissingvalues counts
            //TODO: alternatives to seed updating
            if (iteration == 0) {
                for (i = 0; i < seedidxsize; i++) {
                    for (j = 0; j < nCols; j++) {
                        seeds[i * nCols + j] = Float.NaN;
                    }
                }
            }
            movement = 0;
            for (i = 0; i < threadcount; i++) {
                movement += atdArray[i].movement;
                double[] seed_adj = atdArray[i].seeds_adjustment;
                int[] seed_nmv_adj = atdArray[i].seeds_nmv_adjustment;

                for (j = 0; j < seedidxsize * nCols; j++) {
                    if (Double.isNaN(seeds[j])
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
                int[] seed_gs_adj = atdArray[i].groupsize;
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
                            System.out.print("*");
                            repeat = true;

                            //move original member back here
                            //old group == cg, new group == i
                            //row == preserved_members[i]
                            int row = preserved_members[i];
                            int pos = 0;
                            int cg = -1;
                            for(j=0;j<pieces;j++){
                                short [] grps = ((AlocPieceData)apdList.get(i)).groups;
                                if(pos + grps.length > row){
                                    cg = grps[row - pos];
                                    break;
                                }
                                pos += grps.length;
                            }

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

            //calc max movement (for reducing distance checks)
            double mov;
            double maxmov = -1 * Double.MAX_VALUE;
            for(i=0;i<seedidxsize;i++){
                mov = 0;
                int nmissing = 0;
                if(iteration == 0){
                    for(j=0;j<nCols;j++){
                        k = i*nCols + j;
                        if (seedgroup_nonmissingvalues[k] > 0) {
                            mov += Math.abs(seeds[k] / (double)seedgroup_nonmissingvalues[k]);
                        } else {
                            nmissing++;
                        }
                    }
                } else {
                    for(j=0;j<nCols;j++){
                        k = i*nCols + j;
                        if(oldCount[k] == 0 || seedgroup_nonmissingvalues[k] == 0){
                            nmissing++;
                        }else {
                            double v1 = oldSeeds[k] / (double)oldCount[k];
                            double v2 = seeds[k] / (double)seedgroup_nonmissingvalues[k];                           
                            mov += Math.abs(v1 - v2);
                        }
                    }
                }
                mov /= (double)(nCols - nmissing);
                if(mov > maxmov){
                    maxmov = mov;
                }
                otherGroupMovement[i] = mov;
                groupMovement[i] = mov;
            }
            for(i=0;i<seedidxsize;i++){
               otherGroupMovement[i] += maxmov;
            }

            //backup min_movement
            if (min_movement == -1 || min_movement > movement) {
                min_movement = movement;
                //copy groups to min_groups
                k = 0;
                for(i=0;i<pieces;i++){
                    short [] grps = ((AlocPieceData)apdList.get(i)).groups;
                    double [] dist = ((AlocPieceData)apdList.get(i)).rowDist;
                    for(j=0;j<grps.length;j++,k++){
                        min_groups[k] = grps[j];
                        min_dists[k] = dist[j];
                    }
                }
            }

            //test for -1 group allocations here

            iteration++;

            //job progress is non-linear, use something else so estimates are better
            if(job != null) job.setProgress(Math.sqrt(iteration / 100.0), "moving (" + iteration + ") > moved " + movement);
            if(job != null && job.isCancelled()) {
                for (i = 0; i < threadcount; i++) {
                    ail[i].kill();
                }
                return null;
            }
        }

        if(job != null) job.setProgress(1);
        
        for (i = 0; i < threadcount; i++) {
            ail[i].kill();
        }

        //reverse column range standardization
        for (k = 0; k < pieces; k++) {
            float[] data = (float[]) data_pieces.get(k);
            for (i = 0; i < nCols; i++) {
                nRows = data.length / nCols;
                for (j = 0; j < nRows; j++) {
                    data[i + j * nCols] = (float)((data[i + j * nCols] * col_range[i]) + col_min[i]);
                }
            }
        }
        //reverse row range standardization
        double [] extents = (double[]) data_pieces.get(data_pieces.size()-1);
        for (k = 0; k < pieces; k++) {
            float[] data = (float[]) data_pieces.get(k);
            for (i = 0; i < nCols; i++) {
                nRows = data.length / nCols;
                for (j = 0; j < nRows; j++) {
                    data[i + j * nCols] = (float)((data[i + j * nCols] * (extents[6+i*2+1]-extents[6+i*2])) + extents[6+i*2]);
                }
            }
        }

        //write-back row groups
        return min_groups;

    }
}

/**
 * for data_pieces + larger memory usage (#cells * #groups)
 *
 * @author Adam
 */
class AlocInnerLoop3 extends Thread {
    LinkedBlockingQueue<AlocPieceData> lbq;
    
    AlocThreadData alocThreadData;
    AlocSharedData alocSharedData;
    CountDownLatch countDownLatch;


    public AlocInnerLoop3(LinkedBlockingQueue<AlocPieceData> lbq_, AlocThreadData alocThreadData_, AlocSharedData alocSharedData_) {
        lbq = lbq_;
        alocThreadData = alocThreadData_;
        alocSharedData = alocSharedData_;

        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        try{
            while(true){
                // run on next batch
                AlocPieceData next = lbq.take();
                alocInnerLoop(next);
                countDownLatch.countDown();
            }
        } catch (InterruptedException ex){             
        }catch (Exception e)        {
            e.printStackTrace();
        }
    }

    public void next(CountDownLatch newCountDownLatch) {
        //reset movement
        alocThreadData.movement = 0;        
        countDownLatch = newCountDownLatch;

        if(!isAlive()){
            this.start();
        }
    }

    private void alocInnerLoop(AlocPieceData apd) {
        float[] data = apd.data;
        double[] distances = apd.distances;
        short[] groups = apd.groups;

        final double[] groupMovement = alocSharedData.groupMovement;
        final int nCols = alocSharedData.nCols;
        final double [] col_range = alocSharedData.col_range;
        final int seedidxsize = alocSharedData.seedidxsize;
        final double[] seeds = alocSharedData.seeds;
        final int[] seedgroup_nonmissingvalues = alocSharedData.seedgroup_nonmissingvalues;

        int i, j, k;
        double min_dist_value = 0.00001f;
        int min_idx = 0;
        double dist;
        int missing;
        double v2;
        double v1;

        double min_dist;
        
        int nRows = data.length / nCols;
        int rws;
        double gm;
        int grp;
        //int skips = 0;

        for (i = 0; i < nRows; i++) {            
            rws = i * seedidxsize;
            grp = groups[i];

            if(grp >= 0){
                distances[rws + grp] += groupMovement[grp]+min_dist_value;
                gm = distances[rws + grp];
                min_idx = groups[i];
                if(Double.isNaN(gm)){
                    gm = Float.MAX_VALUE;
                }
            }
            else gm = 0;

            min_dist = Float.MAX_VALUE;

            for (j = 0; j < seedidxsize; j++) {
                distances[rws + j] -= groupMovement[j];
                if( j == grp || !(distances[rws + j] > gm)){
                    //calc dist between obj(i) & obj(seeds(j))
                    dist = 0;
                    missing = 0;
                    for (k = 0; k < nCols; k++) {
                        v1 = data[i * nCols + k];
                        v2 = seeds[j * nCols + k];
                        if (Double.isNaN(v1) || Double.isNaN(v2) || col_range[k] == 0) {
                            missing++;
                        } else {
                            if (seedgroup_nonmissingvalues[j * nCols + k] > 0) {
                                v2 = v2 / seedgroup_nonmissingvalues[j * nCols + k];
                            }
                            dist += java.lang.Math.abs(v1 - v2);//range == 1 (standardized 0-1); / (float) col_range[k];
                        }
                    }
                    dist = dist / (double) (nCols - missing);
                    if (min_dist > dist) {
                        min_dist = dist;
                        min_idx = j;
                    }
                    distances[rws + j] = dist;
                }
                //else{
                 //   skips++;
                //}
            }

            //loop for checking
            /*
            for (j = 0; j < seedidxsize; j++) {
                if( j != grp && !(distances[rws + j] <= gm)){
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
                    if (min_dist >= dist && j != grp) {
                        //should NEVER get here
                        min_dist = dist;
                        min_idx = j;
                    }
                    //do not store distance in test; distances[rws + j] = dist;
                }
            }*/

            //add this group to group min_idx;
            if (grp != (short) min_idx) {
                alocThreadData.movement++;

                //remove from previous group
                if (grp >= 0) {
                    alocThreadData.groupsize[grp]--;
                    for (j = 0; j < nCols; j++) {
                        if (!Float.isNaN(data[i * nCols + j])) {
                            alocThreadData.seeds_adjustment[grp * nCols + j] -= data[i * nCols + j];
                            alocThreadData.seeds_nmv_adjustment[grp * nCols + j]--;
                        }
                    }
                }

                //reassign group
                groups[i] = (short) min_idx;

                //add to new group
                alocThreadData.groupsize[min_idx]++;

                for (j = 0; j < nCols; j++) {
                    if (!Float.isNaN(data[i * nCols + j])) {
                        alocThreadData.seeds_adjustment[min_idx * nCols + j] += data[i * nCols + j];
                        alocThreadData.seeds_nmv_adjustment[min_idx * nCols + j]++;
                    }
                }
            }
        }
        //System.out.print("[" + Math.round((skips / (double)(nRows*seedidxsize)) * 100.0) + "%]");
    }

    void kill() {        
        this.interrupt();
    }
}


class AlocPieceData {
    public float [] data;
    public double [] distances;
    public short[] groups;
    public double[] rowDist;

    public AlocPieceData(float [] data_,
    double [] distances_,
    short[] groups_,
    double[] rowDist_){
        data = data_;
        distances = distances_;
        groups = groups_;
        rowDist = rowDist_;
    }
}

class AlocThreadData {
    public int[] groupsize;
    public int[] seeds_nmv_adjustment;
    public double[] seeds_adjustment;
    public int movement;

    public AlocThreadData(int[] groupsize_, int [] seeds_nvm_adjustment_,
            double[] seeds_adjustment_){
        groupsize = groupsize_;
        seeds_nmv_adjustment = seeds_nvm_adjustment_;
        seeds_adjustment = seeds_adjustment_;
        movement = 0;
    }
}

class AlocSharedData {
    public double[] otherGroupMovement;
    public double[] groupMovement;
    public int nCols;
    public double[] col_range;
    public int seedidxsize;
    public double[] seeds;
    public int[] seedgroup_nonmissingvalues;
    
    public AlocSharedData(
    double[] otherGroupMovement_,
    double[] groupMovement_,
    int nCols_,
    double[] col_range_,
    int seedidxsize_,
    double[] seeds_,
    int[] seedgroup_nonmissingvalues_
    ){
        otherGroupMovement = otherGroupMovement_;
        groupMovement = groupMovement_;
        nCols = nCols_;
        col_range = col_range_;
        seedidxsize = seedidxsize_;
        seeds = seeds_;
        seedgroup_nonmissingvalues = seedgroup_nonmissingvalues_; 
    }
}