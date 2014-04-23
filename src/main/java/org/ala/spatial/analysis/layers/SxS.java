/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.analysis.layers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Used to reference SitesBySpeciesTabulated requests.
 *
 * @author Adam
 */
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
public class SxS {

    String value;
    String analysisId;
    String status;

    public SxS(String value, String analysisId, String status) {
        this.value = value;
        this.analysisId = analysisId;
        this.status = status;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateTime() {
        String date = "";
        try {
            date = new SimpleDateFormat("dd/MM/yyyy hh:mm:SS").format(new Date(Long.valueOf(analysisId)));
        } catch (Exception e) {
        }
        return date;
    }
}
