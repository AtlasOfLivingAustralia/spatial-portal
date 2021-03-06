package au.org.ala.spatial.sampling;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
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
public final class Sampling {
    private static final Logger LOGGER = Logger.getLogger(Sampling.class);

    private Sampling() {
        //to hide public constructor
    }

    public static List<String[]> sampling(List<String> facetIds, double[][] points) {
        //form request and get from layers-service

        try {
            long start = System.currentTimeMillis();
            URL url = new URL(CommonData.getLayersServer() + "/intersect/batch");
            URLConnection c = url.openConnection();
            c.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(c.getOutputStream());
            out.write("fids=");
            for (int i = 0; i < facetIds.size(); i++) {
                if (i > 0) {
                    out.write(",");
                }
                out.write(facetIds.get(i));
            }
            out.write("&points=");
            for (int i = 0; i < points.length; i++) {
                if (i > 0) {
                    out.write(",");
                }
                out.write(String.valueOf(points[i][1]));
                out.write(",");
                out.write(String.valueOf(points[i][0]));
            }
            out.write("&pw=");
            out.write(CommonData.getSettings().getProperty("batch.sampling.pw"));
            out.close();

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(IOUtils.toString(c.getInputStream()));

            String statusUrl = jo.get("statusUrl").toString();

            //wait until done, or until it fails
            String downloadUrl = null;
            int count = 0;
            int retryMax = 10;
            int retry = 0;
            while (retry < retryMax) {
                Thread.sleep(2000);
                while ((downloadUrl = getDownloadUrl(statusUrl)) != null) {

                    retry = 0;

                    if (!downloadUrl.isEmpty() || downloadUrl == null) {
                        retry = retryMax;
                        break;
                    }

                    count++;
                }
                retry++;
            }

            if (downloadUrl != null) {
                return getDownloadData(downloadUrl, points.length);
            }

        } catch (Exception e) {
            LOGGER.error("error with sampling", e);
        }

        return new ArrayList();
    }

    static String getDownloadUrl(String statusUrl) {
        String downloadUrl = null;

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(statusUrl);

            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(get.getResponseBodyAsString());

            if ("finished".equals(jo.get(StringConstants.STATUS))) {
                downloadUrl = jo.get("downloadUrl").toString();
            } else if ("cancelled".equals(jo.get(StringConstants.STATUS)) || "error".equals(jo.get(StringConstants.STATUS))) {
                downloadUrl = null;
            } else {
                downloadUrl = "";
            } 
        } catch (Exception e) {
            LOGGER.error("error getting response from : " + statusUrl, e);
        }

        return downloadUrl;
    }

    static List<String[]> getDownloadData(String downloadUrl, int numberOfPoints) {
        List<String[]> output = new ArrayList<String[]>();

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(downloadUrl);

            get.addRequestHeader(StringConstants.CONTENT_TYPE, "application/zip");

            client.executeMethod(get);

            try {
                ZipInputStream zip = new ZipInputStream(get.getResponseBodyAsStream());
                zip.getNextEntry();
                CSVReader csv = new CSVReader(new InputStreamReader(zip));

                //read first line
                String[] line = csv.readNext();

                //setup
                for (int i = 2; i < line.length; i++) {
                    output.add(new String[numberOfPoints]);
                }

                int row = 0;
                while ((line = csv.readNext()) != null && row < numberOfPoints) {

                    for (int i = 2; i - 2 < output.size() && i < line.length; i++) {
                        output.get(i - 2)[row] = line[i];
                    }

                    row = row + 1;
                }

                zip.close();
            } catch (Exception e) {
                LOGGER.error("failed to read zipped stream from: " + downloadUrl, e);
            }
        } catch (Exception e) {
            LOGGER.error("error getting response from url: " + downloadUrl, e);
        }

        return output;
    }
}
