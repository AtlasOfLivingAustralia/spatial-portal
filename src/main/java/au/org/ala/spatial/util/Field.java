package au.org.ala.spatial.util;

/**
 * Data structure to house database Field attributes.
 *
 * @author Adam Collins
 */
public class Field extends java.lang.Object {

    /**
     * name of column in an associated table
     */
    public String name;

    /**
     * text to display in a UI, keep it short
     */
    public String display_name;

    /**
     * more detailed text on this field
     */
    public String description;

    /**
     * Constructor to populate this data structure.
     *
     * @param _name         name of column in an associated table
     * @param _display_name text to display in a UI, keep it short
     * @param _description  more detailed text on this field
     */
    public Field(String _name, String _display_name, String _description) {
        name = _name;
        display_name = _display_name;
        description = _description;
    }
}
