/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Adam
 */
public class LsidCountsDynamic {
    private static final Logger LOGGER = Logger.getLogger(LsidCountsDynamic.class);

    private static Map<String, Integer> counts = new ConcurrentHashMap<String, Integer>();
    private static LinkedBlockingQueue<String> lsidsToFetch = new LinkedBlockingQueue<String>();
    private static Thread[] consumeLsids = new Thread[4];

    static {
        for (int i = 0; i < consumeLsids.length; i++) {
            consumeLsids[i] = new Thread() {
                @Override
                public void run() {
                    super.run();

                    try {
                        while (true) {
                            String lsid = lsidsToFetch.take();
                            counts.put(lsid, count(lsid));
                        }
                    } catch (InterruptedException e) {

                    }

                }
            };
            consumeLsids[i].start();
        }
    }

    public static void clear() {
        counts = new ConcurrentHashMap<String, Integer>();
        allSpecies();
    }

    public static Integer getCount(String lsid) {
        Map<String, Integer> c = counts;

        if (!c.containsKey(lsid)) {
            try {
                lsidsToFetch.put(lsid);
            } catch (InterruptedException e) {

            }
        }

        return c.get(lsid);
    }

    private static int count(String lsid) {
        HttpClient client = new HttpClient();

        String url = null;
        try {
            url = CommonData.getBiocacheServer()
                    + "/occurrences/search?facet=off&pageSize=0&fq="
                    + URLEncoder.encode("geospatial_kosher:*", StringConstants.UTF_8)
                    + "&q="
                    + URLEncoder.encode("lsid:" + lsid, StringConstants.UTF_8)
                    + CommonData.getBiocacheQc();

            LOGGER.debug(url);
            GetMethod get = new GetMethod(url);

            client.getHttpConnectionManager().getParams().setSoTimeout(30000);

            client.executeMethod(get);

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(get.getResponseBodyAsString());

            return ((Long) jo.get("totalRecords")).intValue();
        } catch (Exception e) {
            LOGGER.error("error getting LSID count for : " + url, e);
        }

        return -1;
    }

    private static void allSpecies() {
        HttpClient client = new HttpClient();

        //get counts at genus, species, subspecies
        String url = null;
        try {
            String[] lsidLevels = new String[]{"genus_guid", "species_guid", "subspecies_guid"};

            for (String lsidLevel : lsidLevels) {
                url = CommonData.getBiocacheServer()
                        + "/occurrences/search?facet=on&facets=" + lsidLevel + "&pageSize=0&flimit=1000000&q="
                        + URLEncoder.encode("geospatial_kosher:*", StringConstants.UTF_8)
                        + CommonData.getBiocacheQc();

                LOGGER.debug(url);
                GetMethod get = new GetMethod(url);

                client.executeMethod(get);
                JSONParser jp = new JSONParser();
                org.json.simple.JSONObject jo = (org.json.simple.JSONObject) jp.parse(get.getResponseBodyAsString());
                org.json.simple.JSONArray ja = (org.json.simple.JSONArray) ((org.json.simple.JSONObject) ((org.json.simple.JSONArray) jo.get("facetResults")).get(0)).get("fieldResult");
                for (int i = 0; i < ja.size(); i++) {
                    String lsid = ((org.json.simple.JSONObject) ja.get(i)).get("label").toString();
                    int count = Integer.parseInt(((org.json.simple.JSONObject) ja.get(i)).get("count").toString());

                    counts.put(lsid, count);
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting LSID count for : " + url, e);
        }

        //fill in zeros
        try {
            String[] lsidLevels = new String[]{"genus_guid", "species_guid", "subspecies_guid"};

            for (String lsidLevel : lsidLevels) {
                url = CommonData.getBiocacheServer()
                        + "/occurrences/search?facet=on&facets=" + lsidLevel + "&pageSize=0&flimit=1000000&q=*:*"
                        + CommonData.getBiocacheQc();

                LOGGER.debug(url);
                GetMethod get = new GetMethod(url);

                client.executeMethod(get);
                JSONParser jp = new JSONParser();
                org.json.simple.JSONObject jo = (org.json.simple.JSONObject) jp.parse(get.getResponseBodyAsString());
                org.json.simple.JSONArray ja = (org.json.simple.JSONArray) ((org.json.simple.JSONObject) ((org.json.simple.JSONArray) jo.get("facetResults")).get(0)).get("fieldResult");
                for (int i = 0; i < ja.size(); i++) {
                    String lsid = ((org.json.simple.JSONObject) ja.get(i)).get("label").toString();
                    if (!counts.containsKey(lsid)) {
                        counts.put(lsid, 0);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting LSID count for : " + url, e);
        }

    }
}
