package org.ala.spatial.gazetteer;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author Angus
 */
public class GazetteerPointSearch {

    /***
     * Given a lon,lat and layer - queries the gaz for a polygon
     * @param lon longitude
     * @param lat latitude
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

            System.out.println("*************************************");
            System.out.println("GazetteerPointSearch.PointSearch");
            System.out.println("URI: " + uri);
            System.out.println("result: " + result);
            System.out.println("slist: " + slist);
            System.out.println("*************************************");

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
            //FIXME: log something
            System.out.println(e1.getMessage());
        }
        return null;
    }
}
