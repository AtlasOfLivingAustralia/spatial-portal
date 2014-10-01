package au.org.ala.spatial.dto;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Data structure to house database Layer attributes.
 * <p/>
 * Suitable as a reference to a database table as well as
 * an environmental data file (e.g. WorldClim grid files)
 *
 * @author Adam Collins
 */
public class LayerDTO {

    /**
     * table for file name
     */
    private String name;

    /**
     * short text for UI
     */
    private String displayName;
    /**
     * detailed description for UI
     */
    private String description;
    /**
     * catagory of 'contextual' or 'environmental'
     */
    private String type;
    /**
     * associated table fields
     */
    private FieldDTO[] fieldDTOs;

    /**
     * Constructor for this data structure
     *
     * @param name        table or file name as String
     * @param displayName text as String for UI, keep it short
     * @param description more detailed text for UI as String
     * @param type        one of 'contextual' or 'environmental' as String
     *                    not enforced here.
     * @param fieldDTOs      array of accessible table fields as Field []
     * @see FieldDTO
     */
    public LayerDTO(String name, String displayName, String description,
                    String type, FieldDTO[] fieldDTOs) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.fieldDTOs = fieldDTOs == null ? null : fieldDTOs.clone();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public FieldDTO[] getFieldDTOs() {
        return fieldDTOs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LayerDTO) {
            return this.displayName.equals(((LayerDTO) obj).displayName);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder(23, 17).
                append(displayName).
                toHashCode();
    }


}
