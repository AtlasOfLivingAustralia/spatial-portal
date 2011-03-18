/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
class OccurrenceSpecies implements Comparable<OccurrenceSpecies> {

    String speciesName;
    String rankName;
    String commonName;
    int count;

    /**
     * only valid for comparisons with OccurenceSpecies class
     * 
     * @param o
     * @return
     */
    @Override
    public int compareTo(OccurrenceSpecies o) {
        return speciesName.compareTo(o.speciesName);
    }
}
