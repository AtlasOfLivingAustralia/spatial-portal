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
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.Type;

/**
 * This class serves as a model object for the "layerpids" table
 *
 * @author ajay
 */
@Entity
@Table(name = "layerpids")
@XmlRootElement(name="layerpid")
@XStreamAlias("layerpid")
public class LayerPid {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "type")
    private int type;

    @Column(name = "unique")
    private String unique;

    @Column(name = "path")
    private String path;

    @Column(name = "pid")
    private String pid;

    @Column(name = "metadata")
    private String metadata;

    @Type(type = "org.hibernatespatial.GeometryUserType")
    @Column(name = "the_geom")
    private Geometry the_geom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Geometry getThe_geom() {
        return the_geom;
    }

    public void setThe_geom(Geometry the_geom) {
        this.the_geom = the_geom;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUnique() {
        return unique;
    }

    public void setUnique(String unique) {
        this.unique = unique;
    }

}
