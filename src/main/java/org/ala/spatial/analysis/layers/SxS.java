/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.layers;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
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
}
