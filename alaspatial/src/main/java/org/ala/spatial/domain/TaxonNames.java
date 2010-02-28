package org.ala.spatial.domain;

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

    private long id;
    private String tname;
    private String tlevel;

    public TaxonNames() {
    }

    @Id
    @GeneratedValue ( strategy = GenerationType.SEQUENCE, generator="taxonnames_mnlid_seq")
    @SequenceGenerator(name = "taxonnames_mnlid_seq", sequenceName = "taxonnames_mnlid_seq")
    @Column(name = "mnlid", insertable = false, updatable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name="tlevel")
    public String getTlevel() {
        return tlevel;
    }

    public void setTlevel(String tlevel) {
        this.tlevel = tlevel;
    }

    @Column(name="tname")
    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }



}
