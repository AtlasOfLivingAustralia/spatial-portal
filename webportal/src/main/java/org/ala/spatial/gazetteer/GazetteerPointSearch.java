package org.ala.spatial.gazetteer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
    public static String PointSearch(String lon, String lat, String layer, String geoserver) {
        String featureURL = "none";

        try {
            String uri = CommonData.layersServer + "/layers-index/intersect/" + layer + "/" + lat + "/" + lon;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(uri);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            if (ja != null && ja.size() > 0) {
                JSONObject jo = ja.getJSONObject(0);

                featureURL = CommonData.layersServer + "/layers=index/shape/geojson/" + jo.getString("pid");
            }
        } catch (Exception e1) {
            //FIXME: log something
            System.out.println(e1.getMessage());
        }
        return featureURL;
    }
}
