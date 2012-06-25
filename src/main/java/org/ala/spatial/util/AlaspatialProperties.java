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
package org.ala.spatial.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Provides access to alaspatial.properties.
 *
 * Parameter details in alaspatial.properties.
 *
 * @author Adam
 */
public class AlaspatialProperties {

    /**
     * Properties object.
     */
    static Properties properties;

    // read in properties file.
    static {
        properties = new Properties();
        try {
            InputStream is = AlaspatialProperties.class.getResourceAsStream("/alaspatial.properties");
            if (is != null) {
                properties.load(is);
            } else {
                String msg = "cannot get properties file: " + AlaspatialProperties.class.getResource("alaspatial.properties").getFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getBaseOutputDir() {
        return properties.getProperty("output.dir");
    }

    public static int getAnalysisThreadCount() {
        return Integer.parseInt(properties.getProperty("threadcount"));
    }

    public static String getBaseOutputURL() {
        return properties.getProperty("output.url");
    }

    public static String getAnalysisWorkingDir() {
        return properties.getProperty("workingdir");
    }

    public static String getAnalysisAlocCmd() {
        return properties.getProperty("aloc.cmd");
    }

    public static String getLayerResolutionDefault() {
        return properties.getProperty("layer.resolution.default");
    }

    public static String getAnalysisGdmCmd() {
        return properties.getProperty("gdm.cmd");
    }

    public static String getAlaspatialUrl() {
        return properties.getProperty("url");
    }

    public static double getAnalysisEstimateSmoothing() {
        return Double.parseDouble(properties.getProperty("estimate.smoothing"));
    }

    public static double getAnalysisAlocEstimateMult0() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.mult0"));
    }

    public static double getAnalysisAlocEstimateAdd0() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.add0"));
    }

    public static double getAnalysisAlocEstimateMult1() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.mult1"));
    }

    public static double getAnalysisAlocEstimateAdd1() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.add1"));
    }

    public static double getAnalysisAlocEstimateMult2() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.mult2"));
    }

    public static double getAnalysisAlocEstimateAdd2() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.add2"));
    }

    public static double getAnalysisAlocEstimateMult3() {
        return Double.parseDouble(properties.getProperty("aloc.estimate.mult3"));
    }

    public static double getAnalysisMaxentEstimateMult0() {
        return Double.parseDouble(properties.getProperty("maxent.estimate.mult0"));
    }

    public static double getAnalysisMaxentEstimateMult1() {
        return Double.parseDouble(properties.getProperty("maxent.estimate.mult1"));
    }

    public static double getAnalysisMaxentEstimateMult2() {
        return Double.parseDouble(properties.getProperty("maxent.estimate.mult2"));
    }

    public static long getAnalysisLimitGridCells() {
        return Long.parseLong(properties.getProperty("limit.gridcells"));
    }

    public static long getAnalysisLimitOccurrences() {
        return Long.parseLong(properties.getProperty("limit.occurrences"));
    }

    public static int GetAnalysisMaxJobs() {
        return Integer.parseInt(properties.getProperty("limit.jobs"));
    }

    public static String getGdalDir() {
        return properties.getProperty("gdal.dir");
    }

    public static String getImageMagickDir() {
        return properties.getProperty("imagemagick.dir");
    }

    public static String getAnalysisLayersDir() {
        return properties.getProperty("layer.dir");
    }

    public static String getAnalysisMaxentCmd() {
        return properties.getProperty("maxent.cmd");
    }

    public static String getGeoserverUrl() {
        return properties.getProperty("geoserver.url");
    }

    public static String getGeoserverUsername() {
        return properties.getProperty("geoserver.username");
    }

    public static String getGeoserverPassword() {
        return properties.getProperty("geoserver.password");
    }

    public static String getBiocacheWsURL() {
        return properties.getProperty("biocache.ws.url");
    }
}
