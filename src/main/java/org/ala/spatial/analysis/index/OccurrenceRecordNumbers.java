/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.io.Serializable;

/**
 *
 * @author Adam
 */
public class OccurrenceRecordNumbers implements Serializable {

    String datasetId;
    int[] records;

    public OccurrenceRecordNumbers(String id, int[] r) {
        this.datasetId = id;
        this.records = r;
    }

    public int[] getRecords() {
        return records;
    }

    public String getName() {
        return datasetId;
    }

    public void setRecords(int[] newrec) {
        records = newrec;
    }
}
