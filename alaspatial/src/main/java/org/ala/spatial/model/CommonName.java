package org.ala.spatial.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Model for common names view
 * 
 * @author ajay
 */

@Entity
@Table(name = "commonnames")
public class CommonName {

    @Column(name="guid", insertable = false, updatable = false)
    @Id
    private String guid;

    @Column(name="scientificname")
    private String scientificname;

    @Column(name="commonname")
    private String commonname;

    public CommonName() {
    }

    public String getCommonname() {
        return commonname;
    }

    public void setCommonname(String commonname) {
        this.commonname = commonname;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getScientificname() {
        return scientificname;
    }

    public void setScientificname(String scientificname) {
        this.scientificname = scientificname;
    }

}
