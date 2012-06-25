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

import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 * Connects to the appropriate citation service and gets the citation details to
 * be included with the samples download
 *
 * @author ajay
 */
public class CitationService {

    //static String NEW_LINE = System.getProperty("line.separator");
    static String NEW_LINE = "\r\n";

    public static String postInfo(String url, Map params, boolean asBody) {
        try {

            HttpClient client = new HttpClient();

            PostMethod post = new PostMethod(url); // testurl

            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            // add the post params
            if (params != null) {
                if (!asBody) {

                    for (Iterator ekeys = params.keySet().iterator(); ekeys.hasNext();) {
                        String key = (String) ekeys.next();
                        String value = (String) params.get(key);
                        post.addParameter(key, URLEncoder.encode(value, "UTF-8"));
                    }
                } else {
                    StringBuilder sbParams = new StringBuilder();

                    for (Iterator ekeys = params.keySet().iterator(); ekeys.hasNext();) {
                        String key = (String) ekeys.next();
                        String value = (String) params.get(key);
                        sbParams.append(value);
                    }

                    RequestEntity entity = new StringRequestEntity(sbParams.toString(), "text/plain", "UTF-8");
                    post.setRequestEntity(entity);
                }
            }

            int result = client.executeMethod(post);

            String slist = post.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            System.out.println("postInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public static void generatePredictionReadme(String outputdir, String pointsfile) {
        generatePredictionReadme(new File(outputdir), pointsfile);
    }

    public static void generatePredictionReadme(File fDir, String pointsfile) {
        try {
            StringBuilder sbReadme = new StringBuilder();
            sbReadme.append("plots\\                                       Folder containing various plots for jackknifing and response curves").append(NEW_LINE);
            sbReadme.append("maxent.log                                    Log of MaxEnt actions and warnings").append(NEW_LINE);
            sbReadme.append("maxentResults.csv                             Summary of 101 statistical values relating to the model").append(NEW_LINE);
            sbReadme.append("prediction.grd                             Diva-GIS grid header file").append(NEW_LINE);
            sbReadme.append("prediction.gri                             Diva-GIS grid file").append(NEW_LINE);
            sbReadme.append("prediction_removedSpecies.txt                 A list of removed sensitive species from the Prediction model").append(NEW_LINE);
            sbReadme.append("prediction_maskedOutSensitiveSpecies.csv      'Sensitive species' have been masked out of the model. See: http://www.ala.org.au/about/program-of-projects/sds/").append(NEW_LINE);
            sbReadme.append("species.html                                  The main output file, containing statistical analyses, plots, pictures of the model, and links to other files. It also documents parameter and control settings that were used to do the run.").append(NEW_LINE);
            sbReadme.append("species_sampleAverages.csv                    Average values for layers in model").append(NEW_LINE);
            sbReadme.append("species_points.csv                            List of all point locations used in the model").append(NEW_LINE);
            sbReadme.append("species.asc                                   Contains the probabilities in ESRI ASCII grid format").append(NEW_LINE);
            sbReadme.append("species.lambdas                               Contains the computed values of the constants").append(NEW_LINE);
            sbReadme.append("species.prj                                   Map projection parameters.").append(NEW_LINE);
            sbReadme.append("species_omission.csv                          Describes the predicted area and training and (optionally) test omission for various raw and cumulative thresholds.").append(NEW_LINE);
            sbReadme.append("species_samplePredictions.csv                 Status and prediction values for each point in model.").append(NEW_LINE);
            sbReadme.append("readme.txt                                    This file.").append(NEW_LINE);
            //sbReadme.append("species_explain.bat                           Generates an explaination of the analysis results. Requires a local instance of MaxEnt.").append(NEW_LINE);

            File temporary_file = new File(fDir, "readme.txt");
            FileWriter fw = new FileWriter(temporary_file);
            fw.write(sbReadme.toString());
            fw.close();

        } catch (Exception e) {
            System.out.println("Error generating Prediction readme");
            e.printStackTrace(System.out);
        }
    }

    public static void generateClassificationReadme(String outputdir, String gridName) {
        generateClassificationReadme(new File(outputdir), gridName);
    }

    public static void generateClassificationReadme(File fDir, String gridName) {
        try {
            StringBuilder sbReadme = new StringBuilder();
            sbReadme.append("classification.asc          ASCII-Grid version of the generated data").append(NEW_LINE);
            sbReadme.append("classification.prj          ASCII-Grid projection header file").append(NEW_LINE);
            sbReadme.append("classification.grd          Diva-GIS Grid header file").append(NEW_LINE);
            sbReadme.append("classification.gri          Diva-GIS Grid data file").append(NEW_LINE);
            sbReadme.append("classification.pgw                   Projection world file for the generated output image").append(NEW_LINE);
            sbReadme.append("classification.png                   Generated output image").append(NEW_LINE);
            sbReadme.append("classification_means.csv   File of group colour values and group means").append(NEW_LINE);
            sbReadme.append("classification.html        Summary of the classification run").append(NEW_LINE);
            sbReadme.append("readme.txt                                    This file.").append(NEW_LINE);

            File temporary_file = new File(fDir, "readme.txt");
            FileWriter fw = new FileWriter(temporary_file);
            fw.write(sbReadme.toString());
            fw.close();

        } catch (Exception e) {
            System.out.println("Error generating Prediction readme");
            e.printStackTrace(System.out);
        }
    }

    public static void generateSitesBySpeciesReadme(String outputdir, boolean sitesBySpecies, boolean occurrenceDensity, boolean speciesRichness) {
        generateSitesBySpeciesReadme(new File(outputdir), sitesBySpecies, occurrenceDensity, speciesRichness);
    }

    public static void generateSitesBySpeciesReadme(File fDir, boolean sitesBySpecies, boolean occurrenceDensity, boolean speciesRichness) {
        try {
            StringBuilder sbReadme = new StringBuilder();

            if (sitesBySpecies) {
                sbReadme.append("SitesBySpecies.csv             Sites by species output csv.").append(NEW_LINE);
                sbReadme.append("sxs_metadata.html              Sites by species metadata").append(NEW_LINE);
                sbReadme.append(NEW_LINE);
            }

            if (occurrenceDensity) {
                sbReadme.append("occurrence_density.asc         Occurrence density layer as ESRI ASCII").append(NEW_LINE);
                sbReadme.append("occurrence_density.grd         Occurrence density layer as Diva grid file header").append(NEW_LINE);
                sbReadme.append("occurrence_density.gri         Occurrence density layer as Diva grid file").append(NEW_LINE);
                sbReadme.append("occurrence_density.png         Occurrence density layer image").append(NEW_LINE);
                sbReadme.append("occurrence_density.prj         Occurrence density layer projection file").append(NEW_LINE);
                sbReadme.append("occurrence_density.sld         Occurrence density layer Geoserver style").append(NEW_LINE);
                sbReadme.append("occurrence_density_legend.png  Occurrence density layer image legend").append(NEW_LINE);
                sbReadme.append("odensity_metadata.html         Occurrence density layer metadata").append(NEW_LINE);
                sbReadme.append(NEW_LINE);
            }
            if (speciesRichness) {
                sbReadme.append("species_richness.asc           Species richness layer as ESRI ASCII").append(NEW_LINE);
                sbReadme.append("species_richness.grd           Species richness layer as Diva grid file header").append(NEW_LINE);
                sbReadme.append("species_richness.gri           Species richness layer as Diva grid file").append(NEW_LINE);
                sbReadme.append("species_richness.png           Species richness layer image").append(NEW_LINE);
                sbReadme.append("species_richness.prj           Species richness layer projection file").append(NEW_LINE);
                sbReadme.append("species_richness.sld           Species richness layer Geoserver style").append(NEW_LINE);
                sbReadme.append("species_richness_legend.png    Species richness layer image legend").append(NEW_LINE);
                sbReadme.append("srichness_metadata.html        Species richness layer metadata").append(NEW_LINE);
                sbReadme.append(NEW_LINE);
            }

            File temporary_file = new File(fDir, "readme.txt");
            FileWriter fw = new FileWriter(temporary_file);
            fw.write(sbReadme.toString());
            fw.close();

        } catch (Exception e) {
            System.out.println("Error generating Prediction readme");
            e.printStackTrace(System.out);
        }
    }
}
