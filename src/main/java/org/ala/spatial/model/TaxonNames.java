package org.ala.spatial.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 *
 * @author ajayr
 */

@Entity
@Table(name = "TAXONNAMES")
public class TaxonNames {

    @Id
    @GeneratedValue ( strategy = GenerationType.SEQUENCE, generator="taxonnames_mnlid_seq")
    @SequenceGenerator(name = "taxonnames_mnlid_seq", sequenceName = "taxonnames_mnlid_seq")
    @Column(name = "mnlid", insertable = false, updatable = false)
    private long id;

    @Column(name="tname")
    private String tname;

    @Column(name="tlevel")
    private String tlevel;

    public TaxonNames() {
    }

    public TaxonNames(long id, String tname, String tlevel) {
        this.id = id;
        this.tname = tname;
        this.tlevel = tlevel;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTlevel() {
        return tlevel;
    }

    public void setTlevel(String tlevel) {
        this.tlevel = tlevel;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }

    @Override
    public String toString() {
        return this.tname + " - " + this.tlevel;
    }


}
