package au.org.ala.spatial.dto;

/**
 * The Data Transfer Object that represents a species list item
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class SpeciesListItemDTO {
    private String name;
    private String lsid;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the lsid
     */
    public String getLsid() {
        return lsid;
    }

    /**
     * @param lsid the lsid to set
     */
    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SpeciesListItemDTO [name=" + name + ", lsid=" + lsid + "]";
    }


}
