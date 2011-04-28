/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

import java.util.ArrayList;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class OccurrencesFilter {
    //searching filters (names, specific records, geographic regions)

    String searchTerm = null;
    ArrayList<OccurrenceRecordNumbers> records = null;
    SimpleRegion region = null;
    //return filter (columns to return)
    ArrayList<String> columns = null;
    //limits
    int maxRecords = TabulationSettings.MAX_RECORD_COUNT_DOWNLOAD;

    public OccurrencesFilter(String lsid, int maxRecords) {
        this.searchTerm = lsid;
        this.maxRecords = maxRecords;
    }

    public OccurrencesFilter(SimpleRegion region, int maxRecords) {
        this.region = region;
        this.maxRecords = maxRecords;
    }

    public OccurrencesFilter(ArrayList<OccurrenceRecordNumbers> records, int maximum_records) {
        this.records = records;
        this.maxRecords = maximum_records;
    }

    public OccurrencesFilter(String lsid, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> rk, int i) {
        this.searchTerm = lsid;
        this.region = region;
        this.records = rk;
        this.maxRecords = i;
    }

    public OccurrencesFilter(String lsid, SimpleRegion region, ArrayList<OccurrenceRecordNumbers> records, String[] layers, int max_rows) {
        this.searchTerm = lsid;
        this.region = region;
        this.records = records;
        this.maxRecords = max_rows;

        if (layers != null) {
            this.columns = new ArrayList<String>(layers.length);
            for (String s : layers) {
                if (s != null && s.length() > 0) {
                    this.columns.add(Layers.getLayer(s).name);
                }
            }
        }
    }
}
