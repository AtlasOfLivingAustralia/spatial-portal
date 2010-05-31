package org.ala.spatial.analysis.method;

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
    
}
