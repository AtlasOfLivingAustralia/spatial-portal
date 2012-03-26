package org.ala.spatial.util;

import java.io.Serializable;

/**
 * Data structure to house database Layer attributes.
 * 
 * Suitable as a reference to a database table as well as
 * an environmental data file (e.g. WorldClim grid files)
 * 
 * @author Adam Collins
 */
public class Layer extends Object implements Serializable {

    static final long serialVersionUID = 8072828752153146503L;
    /**
     * table for file name
     */
    public String name;
    /**
     * short text for UI
     */
    public String display_name;
    /**
     * detailed description for UI
     */
    public String description;
    /**
     * catagory of 'contextual' or 'environmental'
     */
    public String type;

    /**
     * Constructor for this data structure
     * @param _name table or file name as String
     * @param _display_name text as String for UI, keep it short
     * @param _description more detailed text for UI as String
     * @param _type one of 'contextual' or 'environmental' as String
     *        not enforced here.
     * @param _fields array of accessible table fields as Field []
     * @see Field
     */
    public Layer(String _name, String _display_name, String _description,String _type) {
        name = _name;
        display_name = _display_name;
        description = _description;
        type = _type;
    }

    @Override
    public boolean equals(Object obj) {
        Layer that = (Layer) obj;

        return (this.display_name.equalsIgnoreCase(that.display_name));
    }
}
