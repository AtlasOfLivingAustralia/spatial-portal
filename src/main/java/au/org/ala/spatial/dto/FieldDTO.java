package au.org.ala.spatial.dto;

/**
 * Data structure to house database Field attributes.
 *
 * @author Adam Collins
 */
public class FieldDTO {

    /**
     * name of column in an associated table
     */
    private String name;

    /**
     * text to display in a UI, keep it short
     */
    private String displayName;

    /**
     * more detailed text on this field
     */
    private String description;

    /**
     * Constructor to populate this data structure.
     */
    public FieldDTO(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
