/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.vividsolutions.jts.geom.Geometry;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.Type;

/**
 * This class serves as a model object for a list of objects
 * served by the ALA Spatial Portal
 *
 * @author ajay
 */

@Entity
@Table(name = "objects")
@XmlRootElement(name="objects")
@XStreamAlias("objects")
public class Objects {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    //@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "objects_id_seq")
    //@SequenceGenerator(name = "objects_id_seq", sequenceName = "objects_id_seq")
    private String id;

    @Column(name = "pid")
    private String pid;

    @Column(name = "desc")
    private String description;

    @Column(name = "name")
    private String name;

    @Column(name = "fid")
    private String fid;

    @Type(type = "org.hibernatespatial.GeometryUserType")
    @Column(name = "the_geom")
    private Geometry geometry;

    @Column(name = "name_id")
    private int nameId;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNameId() {
        return nameId;
    }

    public void setNameId(int nameId) {
        this.nameId = nameId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }


}
