/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
public class SpeciesIndexRecord {

    /**
     * String value of this record
     */
    public String lsid;

    /*
     * One name for this LSID record
     */
    public String name;

    /*
     * List of common names for this LSID record
     */
    public String[] commonNames;
    /**
     * load order number (unique)
     */
    public int idx;
    /**
     * number of records available
     */
    public int count;
    /**
     * idx of parent SpeciesIndexRecord
     */
    public int parent;
    /**
     * indexed record type / hierarchy depth.
     *
     * current usage as sorted records file column index
     */
    public byte type;
    /**
     * hash of name
     */
    int hash;

    /**
     * constructor for new IndexedRecord
     * @param _name value of this record as String
     * @param _file_start first char position in sorted records file
     * @param _file_end last char position in sorted records file
     * @param _record_start first record position in sorted records file
     * @param _record_end last record position in sorted records file
     * @param _type type as byte, current usage as sorted records file
     * column index
     */
    public SpeciesIndexRecord(String _lsid, String _name, String[] _commonNames, int _idx, int _parent, int _count, byte _type) {
        lsid = _lsid;
        name = _name;
        commonNames = _commonNames;
        idx = _idx;
        parent = _parent;
        count = _count;
        type = _type;
        if(_name != null && _name.length() > 0) {
            hash = _name.hashCode();
        }
    }
}
