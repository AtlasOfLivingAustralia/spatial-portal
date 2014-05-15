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

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * SearchObject dto
 *
 * @author ajay
 */

//@XmlRootElement(name="results")
//@XStreamAlias("results")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class SearchObject {
    private String id;
    private String pid;
    private String description;
    private String name;
    private String fid;
    private String fieldname;

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getFieldname() {
        return fieldname;
    }

    public void setFieldname(String fieldname) {
        this.fieldname = fieldname;
    }

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

    public static SearchObject create(String id, String pid, String name, String description, String fid, String fieldname) {
        SearchObject so = new SearchObject();
        so.id = id;
        so.pid = pid;
        so.description = description;
        so.name = name;
        so.fid = fid;
        so.fieldname = fieldname;
        return so;
    }
}
