package org.ala.spatial.data;

/**
 * The Data Transfer Object that represents a species list
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 *
 */
public class SpeciesListDTO {
    private String dataResourceUid;
    private String listName;
    private String listType;
    private String username;
    private String firstName;
    private String surname;
    private String fullName;
    /**
     * @return the dataResourceUid
     */
    public String getDataResourceUid() {
        return dataResourceUid;
    }
    /**
     * @param dataResourceUid the dataResourceUid to set
     */
    public void setDataResourceUid(String dataResourceUid) {
        this.dataResourceUid = dataResourceUid;
    }
    /**
     * @return the listName
     */
    public String getListName() {
        return listName;
    }
    /**
     * @param listName the listName to set
     */
    public void setListName(String listName) {
        this.listName = listName;
    }
    /**
     * @return the listType
     */
    public String getListType() {
        return listType;
    }
    /**
     * @param listType the listType to set
     */
    public void setListType(String listType) {
        this.listType = listType;
    }
    /**
     * @return the username
     */
    public String getUsername() {
      return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }
    /**
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    /**
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }
    /**
     * @param surname the surname to set
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "SpeciesListDTO [dataResourceUid=" + dataResourceUid
          + ", listName=" + listName + ", listType=" + listType + ", username="
          + username + ", firstName=" + firstName + ", surname=" + surname
          + "]";
    }
    /**
     * @return the fullName
     */
    public String getFullName() {
      return fullName;
    }
    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
      this.fullName = fullName;
    }
    
    
    
    
}
