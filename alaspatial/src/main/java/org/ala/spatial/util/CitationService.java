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

/**
 * Connects to the appropriate citation service and gets
 * the citation details to be included with the samples download
 * 
 * @author ajay
 */
public class CitationService {

    public static String generateCitationDataProviders(String file) {
        String citation = "";

        try {
            Vector dpList = new Vector();

            OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();
            String[] cols = ofu.getOutputColumnNames();
            int i = 0;
            for (i = 0; i < cols.length; i++) {
                if (cols[i].equalsIgnoreCase(TabulationSettings.occurrences_dp_uid)) {
                    break;
                }
            }

            CSVReader reader = new CSVReader(new FileReader(file));
            List rows = reader.readAll();
            Iterator it = rows.iterator();

            // remove the header by calling it.next();
            if (it.hasNext()) it.next();
            
            for (; it.hasNext();) {
                String[] row = (String[]) it.next();

                if (!dpList.contains(row[i])) {
                    dpList.add(row[i]);
                }

            }

            System.out.println("retrieving citation for: ");
            System.out.println(dpList); 

            citation = generateCitationDataProviders(dpList);
            
            String citationpath = file.substring(0,file.lastIndexOf(".csv")) + "_citation.csv"; 

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

            String result = postInfo(url, params);

            return result;

        } catch (Exception e) {
            System.out.println("Error retrieving citation details for data providers");
            e.printStackTrace(System.out);
        }

        return citation;
    }

    private static String postInfo(String url, Map params) {
        try {

            HttpClient client = new HttpClient();

            PostMethod post = new PostMethod(url); // testurl

            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            // add the post params
            if (params != null) {
                for (Iterator ekeys = params.keySet().iterator(); ekeys.hasNext();) {
                    String key = (String) ekeys.next();
                    String value = (String) params.get(key);
                    post.addParameter(key, URLEncoder.encode(value, "UTF-8"));
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
}
