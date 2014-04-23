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
import org.postgresql.largeobject.LargeObject;

import javax.persistence.*;
import java.sql.Blob;
import java.util.Date;

@Entity
@Table(name = "ud_data_x")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Ud_data_x {
    @Column(name = "ud_header_id")
    private Long ud_header_id;

    @Column(name = "ref")
    private String ref;

    @Column(name = "data")
    private Blob data;

    @Column(name = "data_type")
    private String data_type;

    public Ud_data_x() {
    }

    public Ud_data_x(Long ud_header_id, String ref, Blob data) {
        this.ud_header_id = ud_header_id;
        this.ref = ref;
        this.data = data;
    }

    public Long getUd_header_id() {
        return ud_header_id;
    }

    public void setUd_header_id(Long ud_header_id) {
        this.ud_header_id = ud_header_id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Blob getData() {
        return data;
    }

    public void setData(Blob data) {
        this.data = data;
    }

    public String getData_type() {
        return data_type;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
    }
}
