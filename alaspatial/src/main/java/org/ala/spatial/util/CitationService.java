package org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 * Connects to the appropriate citation service and gets
 * the citation details to be included with the samples download
 * 
 * @author ajay
 */
public class CitationService {

    static String NEW_LINE = System.getProperty("line.separator");

    public static String generateCitationDataProviders(String file) {
        String citation = "";

        try {
            Vector dpList = getDataResources(file, TabulationSettings.occurrences_dr_uid);
            
            System.out.println("retrieving citation for: ");
            System.out.println(dpList);

            citation = generateCitationDataProviders(dpList);

            String citationpath = file.substring(0, file.lastIndexOf(".csv")) + "_citation.csv";

            File temporary_file = new File(citationpath);
            FileWriter fw = new FileWriter(temporary_file);
            fw.write(citation);
            fw.close();

            return citationpath;

        } catch (Exception e) {
            System.out.println("Unable to read the samples file to retrieve citation details:");
            System.out.println(System.out);
        }



        return citation;
    }

    public static String generateCitationDataProviders(Vector dpList) {

        String citation = "";

        try {

            if (dpList == null) {
                return "";
            }

            String url = TabulationSettings.citation_url_data_provider;
            StringBuilder sbDp = new StringBuilder();
            sbDp.append("[");
            for (int i = 0; i < dpList.size(); i++) {
                if (i > 0) {
                    sbDp.append(",");
                }
                sbDp.append("\"").append((String) dpList.get(i)).append("\"");
            }
            sbDp.append("]");

            Map params = new HashMap();
            params.put("uids", sbDp.toString());

            System.out.println("generateCitationDataProviders: ");
            System.out.println("url: " + url);
            System.out.println("params: " + dpList.toString());

            String result = postInfo(url, params, true);

            return result;

        } catch (Exception e) {
            System.out.println("Error retrieving citation details for data providers");
            e.printStackTrace(System.out);
        }

        return citation;
    }

    public static Vector getDataResources(String file, String columnName) {
        try {
            Vector dpList = new Vector();

            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
            String[] cols = ofu.getOutputColumnNames();
            int i = 0;
            for (i = 0; i < cols.length; i++) {
                if (cols[i].equalsIgnoreCase(columnName)) {
                    break;
                }
            }

            CSVReader reader = new CSVReader(new FileReader(file));
            List rows = reader.readAll();
            Iterator it = rows.iterator();

            // remove the header by calling it.next();
            if (it.hasNext()) {
                it.next();
            }

            for (; it.hasNext();) {
                String[] row = (String[]) it.next();

                if (!dpList.contains(row[i])) {
                    dpList.add(row[i]);
                }

            }

            return dpList; 

        } catch (Exception e) {
            System.out.println("Error parsing for data resources:");
            e.printStackTrace(System.out); 
        }

        return null; 
    }

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

                    RequestEntity entity = new StringRequestEntity(sbParams.toString(),"text/plain", "UTF-8");
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
            sbReadme.append("plots\\                           Folder containing various plots for jackknifing and response curves").append(NEW_LINE);
            sbReadme.append("maxent.log                       Log of MaxEnt actions and warnings").append(NEW_LINE);
            sbReadme.append("maxentResults.csv                Summary of 101 statistical values relating to the model").append(NEW_LINE);
            sbReadme.append("species.html                     The main output file, containing statistical analyses, plots, pictures of the model, and links to other files.Ê It also documents parameter and control settings that were used to do the run.").append(NEW_LINE);
            sbReadme.append("species_sampleAverages.csv       Average values for layers in model").append(NEW_LINE);
            sbReadme.append("species_points.csv               List of all point locations used in the model").append(NEW_LINE);
            sbReadme.append("species.asc                      Contains the probabilities in ESRI ASCII grid format").append(NEW_LINE);
            sbReadme.append("species.lambdas                  Contains the computed values of the constants").append(NEW_LINE);
            sbReadme.append("species.prj                      Map projection parameters.").append(NEW_LINE);
            sbReadme.append("species.zip                      Zip of species.asc and species.prj").append(NEW_LINE);
            sbReadme.append("species_omission.csv             Describes the predicted area and training and (optionally) test omission for various raw and cumulative thresholds.").append(NEW_LINE);
            sbReadme.append("species_samplePredictions.csv    Status and prediction values for each point in model.").append(NEW_LINE);
            sbReadme.append("species_explain.bat              Generates an explaination of the analysis results. Requires a local instance of MaxEnt.").append(NEW_LINE);

            File temporary_file = new File(fDir, "readme.txt");
            FileWriter fw = new FileWriter(temporary_file);
            fw.write(sbReadme.toString());
            fw.close();

        } catch (Exception e) {
            System.out.println("Error generating Prediction readme");
            e.printStackTrace(System.out);
        }
    }

    public static void generateClassificationReadme(String outputdir) {
        generateClassificationReadme(new File(outputdir)); 
    }
    public static void generateClassificationReadme(File fDir) {
        try {
            StringBuilder sbReadme = new StringBuilder();
            sbReadme.append("aloc.pgw                   Projection world file for the generated output image").append(NEW_LINE);
            sbReadme.append("aloc.png                   Generated output image").append(NEW_LINE);
            sbReadme.append("extents.txt                Extents for the generated image").append(NEW_LINE);
            sbReadme.append("aloc.prj                   Projection file for the generated image").append(NEW_LINE);
            sbReadme.append("classification_means.csv   File of group colour values and group means").append(NEW_LINE);
            sbReadme.append("classification.html        Summary of the classification run").append(NEW_LINE);
            sbReadme.append("t_aloc.png                 Temporary classification image, PNG format").append(NEW_LINE);
            sbReadme.append("t_aloc.tif                 Temporary classification image, GeoTIFF format").append(NEW_LINE);

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
