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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * SearchObject DTO
 * 
 * @author ajay
 */

@Entity
@org.hibernate.annotations.NamedNativeQuery(name = "searchobjects", query = "SELECT * searchobjects(:q, :lim) at t1", callable = true, resultClass = SearchObject.class)
@XmlRootElement(name="results")
@XStreamAlias("results")
public class SearchObject {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "pid")
    private String pid;

    @Column(name = "desc")
    private String description;

    @Column(name = "name")
    private String name;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

}
