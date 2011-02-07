/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
/**
 * grid data object; value, record number
 *
 * @author adam
 *
 */
class SPLGridRecord extends Object {

    /**
     * record value
     */
    public double value;
    /**
     * record number
     */
    public int record_number;

    /**
     * constructor for SPLGridRecord
     *
     * @param _value as double
     * @param _species_number as int
     * @param _record_number as int
     */
    public SPLGridRecord(double _value, int _record_number) {
        value = _value;
        record_number = _record_number;
    }
};
