/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.gazetteer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brendon
 */
public class GazetteerSearchResultSummary {
    private List<String> results = new ArrayList<String>();
    private List<GazetteerSearchResult> gazetteerResults = new ArrayList<GazetteerSearchResult>();

    public List<GazetteerSearchResult> getGazetteerResults() {
        return gazetteerResults;
    }

    public void setGazetteerResults(List<GazetteerSearchResult> gazetteerResults) {
        this.gazetteerResults = gazetteerResults;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = results;
    }

    public void addResult(GazetteerSearchResult gs) {
        this.gazetteerResults.add(gs);
    }
    


}
