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

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "ud_header")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Ud_header {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ud_header_id_seq")
    @SequenceGenerator(name = "layers_id_seq", sequenceName = "ud_header_id_seq")
    @Column(name = "ud_header_id", insertable = false, updatable = false)
    private Long ud_header_id;

    @Column(name = "upload_dt")
    private String upload_dt;

    @Column(name = "lastuse_dt")
    private Date lastuse_dt;

    @Column(name = "user_id")
    private String user_id;

    @Column(name = "analysis_id")
    private String analysis_id;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "description")
    private String description;

    @Column(name = "data_size")
    private Integer data_size;

    @Column(name = "record_type")
    private String record_type;

    @Column(name = "mark_for_deletion_dt")
    private Date mark_for_deletion_dt;

    @Column(name = "data_path")
    private String data_path;

    private String facet_id;

    private ArrayList<String> refs;

    public Ud_header() {
    }

    public Ud_header(String user_id, String analysis_id, String description, String metadata, String record_type, String data_path) {
        this.user_id = user_id;
        this.analysis_id = analysis_id;
        this.description = description;
        this.metadata = metadata;
        this.record_type = record_type;
        this.data_path = data_path;
    }

    public Long getUd_header_id() {
        return ud_header_id;
    }

    public void setUd_header_id(Long ud_header_id) {
        this.ud_header_id = ud_header_id;
    }

    public String getUpload_dt() {
        return upload_dt;
    }

    public void setUpload_dt(String upload_dt) {
        this.upload_dt = upload_dt;
    }

    public Date getLastuse_dt() {
        return lastuse_dt;
    }

    public void setLastuse_dt(Date lastuse_dt) {
        this.lastuse_dt = lastuse_dt;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getAnalysis_id() {
        return analysis_id;
    }

    public void setAnalysis_id(String analysis_id) {
        this.analysis_id = analysis_id;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getData_size() {
        return data_size;
    }

    public void setData_size(Integer data_size) {
        this.data_size = data_size;
    }

    public String getRecord_type() {
        return record_type;
    }

    public void setRecord_type(String record_type) {
        this.record_type = record_type;
    }

    public Date getMark_for_deletion_dt() {
        return mark_for_deletion_dt;
    }

    public void setMark_for_deletion_dt(Date mark_for_deletion_dt) {
        this.mark_for_deletion_dt = mark_for_deletion_dt;
    }

    public String getData_path() {
        return data_path;
    }

    public void setData_path(String data_path) {
        this.data_path = data_path;
    }

    public ArrayList<String> getRefs() {
        return refs;
    }

    public void setRefs(ArrayList<String> refs) {
        this.refs = refs;
    }

    public String getFacet_id() {
        return facet_id;
    }

    public void setFacet_id(String facet_id) {
        this.facet_id = facet_id;
    }
}
