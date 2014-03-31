package au.org.ala.spatial.sampling;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.util.CommonData;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

/**
 * builder for sampling index.
 * <p/>
 * requires OccurrencesIndex to be up to date.
 * <p/>
 * operates on GridFiles
 * operates on Shape Files
 *
 * @author adam
 */
public class Sampling {
    private static Logger logger = Logger.getLogger(Sampling.class);

    public static ArrayList<String[]> sampling(ArrayList<String> facetIds, double[][] points) {
        //form request and get from layers-service

        //TODO: progress indicator

        try {
            HttpClient client = new HttpClient();

            PostMethod post = new PostMethod(CommonData.layersServer + "/intersect/batch");

            StringBuilder facets = new StringBuilder();
            for (String f : facetIds) {
                if (facets.length() == 0) {
                    facets.append(",");
                }
                facets.append(f);
            }

            StringBuilder coordinates = new StringBuilder();
            for (double[] p : points) {
                if (coordinates.length() == 0) {
                    coordinates.append(",");
                }
                coordinates.append(p[1]).append(",").append(p[0]);
            }

            post.addParameter("fids", facets.toString());
            post.addParameter("points", coordinates.toString());

            post.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(post);
            JSONObject jo = JSONObject.fromObject(post.getResponseBodyAsString());

            String statusUrl = jo.getString("statusUrl");

            //wait until done, or until it fails
            String downloadUrl = null;
            int count = 0;
            while ((downloadUrl = getDownloadUrl(statusUrl)) != null) {

                //wait 10s, but not forever (timeout at 300s, so just under)
                if (!downloadUrl.isEmpty() || count >= 25) {
                    break;
                }

                Executions.getCurrent().wait(10000);
            }

            if (downloadUrl != null) {
                return getDownloadData(downloadUrl, points.length);
            }

        } catch (Exception e) {
            logger.error("error with sampling", e);
        }

        return null;
    }

    static String getDownloadUrl(String statusUrl) {
        String downloadUrl = null;

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(statusUrl);

            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);

            JSONObject jo = JSONObject.fromObject(get.getResponseBodyAsString());

            if (jo.getString("status").equals("finished")) {
                downloadUrl = jo.getString("downloadUrl");
            } else {
                downloadUrl = "";
            }
        } catch (Exception e) {
            logger.error("error getting response from : " + statusUrl, e);
        }

        return downloadUrl;
    }

    static ArrayList<String[]> getDownloadData(String downloadUrl, int number_of_points) {
        ArrayList<String[]> output = new ArrayList<String[]>();

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(downloadUrl);

            get.addRequestHeader("Content-type", "application/zip");

            int result = client.executeMethod(get);

            String s = null;
            try {
                ZipInputStream zip = new ZipInputStream(get.getResponseBodyAsStream());
                CSVReader csv = new CSVReader(new InputStreamReader(zip));

                String[] line = csv.readNext(); //read first line

                //setup
                for (int i = 2; i < line.length; i++) {
                    output.add(new String[number_of_points]);
                }

                int row = 0;
                while ((line = csv.readNext()) != null && row < number_of_points) {

                    for (int i = 2; i < line.length && i - 2 < output.size(); i++) {
                        output.get(i - 2)[row] = line[i];
                    }

                    row = row + 1;
                }

                zip.close();
            } catch (Exception e) {
                logger.error("failed to read zipped stream from: " + downloadUrl, e);
            }
        } catch (Exception e) {
            logger.error("error getting response from url: " + downloadUrl, e);
        }

        return output;
    }
}
