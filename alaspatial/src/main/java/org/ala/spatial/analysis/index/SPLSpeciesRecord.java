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
/**
 * species record, name and ranking number
 *
 * @author adam
 *
 */
class SPLSpeciesRecord implements Serializable {

    static final long serialVersionUID = -6084623663963314054L;
    /**
     * species name
     */
    public String name;
    /**
     * species ranking number
     */
    public int rank;

    /**
     * constructor
     * @param _name as String
     * @param _rank as int
     */
    public SPLSpeciesRecord(String _name, int _rank) {
        name = _name;
        rank = _rank;
    }
}
