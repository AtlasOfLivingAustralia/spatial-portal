/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.rest;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brendon
 */
public class GazetteerSearch {
    private List<SearchResultItem> results = new ArrayList<SearchResultItem>();

    public void addResult(SearchResultItem sri) {
        results.add(sri);
    }

    public List<SearchResultItem> getResults() {
        return results;
    }

    public void setResults(List<SearchResultItem> results) {
        this.results = results;
    }

    

}
