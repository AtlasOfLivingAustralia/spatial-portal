package au.org.ala.spatial.composer.gazetteer;

import au.org.ala.spatial.util.CommonData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Angus
 */
public class GazetteerPointSearch {

    private static Logger logger = Logger.getLogger(GazetteerPointSearch.class);

    /**
     * *
     * Given a lon,lat and layer - queries the gaz for a polygon
     *
     * @param lon   longitude
     * @param lat   latitude
     * @param layer geoserver layer to search
     * @return returns a link to a geojson feature in the gaz
     */
    public static Map<String, String> PointSearch(String lon, String lat, String layer, String geoserver) {
        try {
            String uri = CommonData.layersServer + "/intersect/" + layer + "/" + lat + "/" + lon;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(uri);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            logger.debug("*************************************");
            logger.debug("GazetteerPointSearch.PointSearch");
            logger.debug("URI: " + uri);
            logger.debug("result: " + result);
            logger.debug("slist: " + slist);
            logger.debug("*************************************");

            JSONArray ja = JSONArray.fromObject(slist);

            if (ja != null && ja.size() > 0) {
                JSONObject jo = ja.getJSONObject(0);

                HashMap<String, String> map = new HashMap<String, String>();
                for (Object k : jo.keySet()) {
                    map.put((String) k, jo.getString((String) k));
                }

                return map;
            }
        } catch (Exception e1) {
            logger.error("error with gaz point search", e1);
        }
        return null;
    }
}
