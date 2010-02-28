package org.ala.spatial.domain;

import com.vividsolutions.jts.geom.Geometry;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

/**
 *
 * @author ajayr
 */

@Entity
@Table(name = "OZCAMDATA")
public class Species {
    
    private long id;
    private String genus;
    private String species;
    private String scientificname;
    private String longitude;
    private String latitude;
    //private Geometry geom;

    public Species() {
    }

    @Id
    @GeneratedValue ( strategy = GenerationType.SEQUENCE, generator="ozcamdata_odid_seq")
    @SequenceGenerator(name = "ozcamdata_odid_seq", sequenceName = "ozcamdata_odid_seq")
    @Column(name = "odid", insertable = false, updatable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name="scientificname")
    public String getScientificname() {
        return scientificname;
    }

    public void setScientificname(String scientificname) {
        this.scientificname = scientificname;
    }

    @Column(name="genus")
    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    @Column(name="species")
    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    @Column(name="longitude")
    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @Column(name="latitude")
    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    /*
    @Column(name="the_geom")
    @Type(type="org.hibernatespatial.GeometryUserType")
    public Geometry getGeom() {
        return geom;
    }

    public void setGeom(Geometry geom) {
        this.geom = geom;
    }
    */



}
