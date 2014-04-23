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
package org.ala.spatial.analysis.service;

import java.util.List;

import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.Layers;

/**
 * Settings for the ALOC process. Sets up the basic/default settings allow for
 * more settings to be added as needed
 *
 * @author ajayr
 */
public class AlocSettings {

    private String alocPath;
    private int numberOfGroups;
    private String envVarToggler;
    private String envPath;
    private String envPrefix;
    private String envSuffix;
    private List envList;
    private String outputPath;
    private String defaultCmdVars;

    public AlocSettings() {
        alocPath = AlaspatialProperties.getAnalysisAlocCmd();

        envVarToggler = "";
        envPrefix = "";
        envSuffix = "";

        defaultCmdVars = "";
    }

    public String getDefaultCmdVars() {
        return defaultCmdVars;
    }

    public void setDefaultCmdVars(String defaultCmdVars) {
        this.defaultCmdVars = defaultCmdVars;
    }

    public String getEnvPath() {
        return envPath;
    }

    public void setEnvPath(String envPath) {
        this.envPath = envPath;
    }

    public String getEnvPrefix() {
        return envPrefix;
    }

    public void setEnvPrefix(String envPrefix) {
        this.envPrefix = envPrefix;
    }

    public String getEnvSuffix() {
        return envSuffix;
    }

    public void setEnvSuffix(String envSuffix) {
        this.envSuffix = envSuffix;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(int numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public String getEnvVarToggler() {
        return envVarToggler;
    }

    public void setEnvVarToggler(String envVarToggler) {
        this.envVarToggler = envVarToggler;
    }

    public String getAlocPath() {
        return alocPath;
    }

    public void setAlocPath(String alocPath) {
        this.alocPath = alocPath;
    }

    public List<String> getEnvList() {
        return envList;
    }

    public void setEnvList(List<String> envList) {
        for (int i = 0; i < envList.size(); i++) {
            envList.set(i, Layers.getFieldId(envList.get(i)));
        }
        this.envList = envList;
    }

    @Override
    public String toString() {
        String cmd;

        cmd = "";

        // add the maxent path
        cmd += alocPath;

        // add the env.vars path
        cmd += " " + getEnvPath();

        // set group count
        cmd += " " + numberOfGroups;

        // set thread count
        cmd += " " + AlaspatialProperties.getAnalysisThreadCount();

        // finally add the output path
        cmd += " " + outputPath;

        return cmd;
    }
}
