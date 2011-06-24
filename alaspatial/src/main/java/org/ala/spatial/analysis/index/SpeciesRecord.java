package org.ala.spatial.analysis.index;

import java.io.Serializable;

/**
 * SpeciesRecord, or species number and record number 
 * 
 * for FilteringIndex
 * 
 * @author adam
 *
 */
public class SpeciesRecord extends Object implements Serializable {

    static final long serialVersionUID = -3928286294432132413L;
    /**
     * species number
     */
    public int species;
    /**
     * record number
     */
    public int record;

    /**
     * constructor
     *
     * @param species_ species number as int
     * @param key_ record number as int
     */
    public SpeciesRecord(int species_, int record_) {
        species = species_;
        record = record_;
    }
}
