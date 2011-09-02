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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import org.springframework.stereotype.Service;

/**
 * This class serves as a model object for the "fields" table
 *
 * @author ajay
 */

@Entity
@Table(name = "fields")
@XmlRootElement(name="field")
@XStreamAlias("field")
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
    private String sourceName;

    @Column(name = "sdesc")
    private String sourceDescription;

    @Column(name = "indb")
    private boolean indb;

    @Column(name = "enabled")
    private boolean enabled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update")
    private Date lastUpdated;

    @Column(name = "nameSearch")
    private boolean nameSearch;

    @Column(name = "defaultLayer")
    private boolean defaultLayer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDefaultLayer() {
        return defaultLayer;
    }

    public void setDefaultLayer(boolean defaultLayer) {
        this.defaultLayer = defaultLayer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIndb() {
        return indb;
    }

    public void setIndb(boolean indb) {
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

    public boolean isNameSearch() {
        return nameSearch;
    }

    public void setNameSearch(boolean nameSearch) {
        this.nameSearch = nameSearch;
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
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
