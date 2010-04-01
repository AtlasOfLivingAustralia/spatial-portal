package org.ala.spatial.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
//import com.vividsolutions.jts.geom.Geometry;
//import org.hibernate.annotations.Type;

/**
 *
 * @author ajay
 */

@Entity
@Table(name = "OCCURRENCESV1")
public class Species {

    @Id
    @GeneratedValue ( strategy = GenerationType.SEQUENCE, generator="occurrences_ocid_seq")
    @SequenceGenerator(name = "occurrences_ocid_seq", sequenceName = "occurrences_ocid_seq")
    @Column(name = "ocid", insertable = false, updatable = false)
    private long id;

    @Column(name="genus")
    private String genus;

    @Column(name="species")
    private String species;

    //@Column(name="scientificname")
    //private String scientificname;

    @Column(name="longitude")
    private String longitude;

    @Column(name="latitude")
    private String latitude;

    //@Column(name="the_geom")
    //@Type(type="org.hibernatespatial.GeometryUserType")
    //private Geometry geom;

    public Species() {
    }

    public Species(long id, String genus, String species, String scientificname, String longitude, String latitude) {
        this.id = id;
        this.genus = genus;
        this.species = species;
        //this.scientificname = scientificname;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    /*
    public String getScientificname() {
        return scientificname;
    }

    public void setScientificname(String scientificname) {
        this.scientificname = scientificname;
    }
    */

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    /*
    public Geometry getGeom() {
        return geom;
    }

    public void setGeom(Geometry geom) {
        this.geom = geom;
    }
    */


}
