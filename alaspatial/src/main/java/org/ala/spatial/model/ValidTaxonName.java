package org.ala.spatial.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Model for a valid taxon name view
 * 
 * @author ajay
 */

@Entity
@Table(name = "validatednames")
public class ValidTaxonName {

    @Column(name="guid", insertable = false, updatable = false)
    @Id
    private String guid;

    @Column(name="scientificname")
    private String scientificname;

    @Column(name="rankstring")
    private String rankstring;

    public ValidTaxonName() {
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getRankstring() {
        return rankstring;
    }

    public void setRankstring(String rankstring) {
        this.rankstring = rankstring;
    }

    public String getScientificname() {
        return scientificname;
    }

    public void setScientificname(String scientificname) {
        this.scientificname = scientificname;
    }

    


}
