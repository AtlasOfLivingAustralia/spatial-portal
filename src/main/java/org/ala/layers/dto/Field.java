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
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This class serves as a model object for the "fields" table
 *
 * @author ajay
 */

@Entity
@Table(name = "fields")
//@XStreamAlias("field")
@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
public class Field {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "spid")
    private String spid;

    @Column(name = "sid")
    private String sourceId;

    @Column(name = "sname")
    private String sname;

    @Column(name = "sdesc")
    private String sourceDescription;

    @Column(name = "indb")
    private Boolean indb;

    @Column(name = "enabled")
    private Boolean enabled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update")
    private Date lastUpdated;

    @Column(name = "namesearch")
    private Boolean namesearch;

    @Column(name = "defaultlayer")
    private Boolean defaultlayer;

    private List<Objects> objects;

    public List<Objects> getObjects() {
        return objects;
    }

    public void setObjects(List<Objects> objects) {
        this.objects = objects;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean isDefaultlayer() {
        return defaultlayer;
    }

    public void setDefaultlayer(Boolean defaultLayer) {
        this.defaultlayer = defaultLayer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isIndb() {
        return indb;
    }

    public void setIndb(Boolean indb) {
        this.indb = indb;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isNamesearch() {
        return namesearch;
    }

    public void setNamesearch(Boolean nameSearch) {
        this.namesearch = nameSearch;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sourceName) {
        this.sname = sourceName;
    }

    public String getSpid() {
        return spid;
    }

    public void setSpid(String spid) {
        this.spid = spid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



}
