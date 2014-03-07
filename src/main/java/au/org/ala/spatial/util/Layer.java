package au.org.ala.spatial.util;

/**
 * Data structure to house database Layer attributes.
 * <p/>
 * Suitable as a reference to a database table as well as
 * an environmental data file (e.g. WorldClim grid files)
 *
 * @author Adam Collins
 */
public class Layer extends java.lang.Object {

    /**
     * table for file name
     */
    public String name;

    /**
     * short text for UI
     */
    public String display_name;

    public String getName() {
        return name;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public Field[] getFields() {
        return fields;
    }

    /**
     * detailed description for UI
     */
    public String description;

    /**
     * catagory of 'contextual' or 'environmental'
     */
    public String type;

    /**
     * associated table fields
     */
    public Field[] fields;

    /**
     * Constructor for this data structure
     *
     * @param _name         table or file name as String
     * @param _display_name text as String for UI, keep it short
     * @param _description  more detailed text for UI as String
     * @param _type         one of 'contextual' or 'environmental' as String
     *                      not enforced here.
     * @param _fields       array of accessible table fields as Field []
     * @see Field
     */
    public Layer(String _name, String _display_name, String _description,
                 String _type, Field[] _fields) {
        name = _name;
        display_name = _display_name;
        description = _description;
        type = _type;
        fields = _fields;
    }

    @Override
    public boolean equals(Object obj) {
        Layer that = (Layer) obj;

        return (this.display_name.equals(that.display_name));
    }


}
