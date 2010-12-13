/*
 * Implementation of GeoJSONUtilities
 * 
 */
package au.org.emii.portal.util;

import au.org.emii.portal.net.HttpConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author brendon
 * the feature checking comes from Nicholas Bergson-Shilcock, The Open Planning Project
 * and the GeoJSONParser code he wrote for GeoTOOLS
 *
 */
public class GeoJSONUtilitiesImpl implements GeoJSONUtilities {

    private static HashMap typeMap;
    protected Logger logger = Logger.getLogger(this.getClass());
    private HttpConnection httpConnection = null;

    @Override
    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    @Autowired
    @Override
    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    @Override
    public String getJson(String url) {
        InputStream in = null;
        String json = null;
        URLConnection connection = null;

        try {

            connection = httpConnection.configureSlowURLConnection(url);
            connection.addRequestProperty("accept", "application/json, text/javascript, */*"); 
            in = connection.getInputStream();
            json = IOUtils.toString(in);
            //json = cleanUpJson(json);
            return json;
        } catch (IOException iox) {
            logger.debug(iox.toString());
            
        }
        return "fail";

    }

    @Override
    public int getFirstFeatureType(JSONObject obj) {
        JSONObject prototype = null;
        int objType = type(obj.getString("type"));

        switch (objType) {
            case FEATURE:
                prototype = obj;
                JSONObject objGeom = prototype.getJSONObject("geometry");
                objType = type(objGeom.getString("type"));

                break;

            case FEATURECOLLECTION:

                if (!obj.containsKey("features")) {
                    logger.debug("no features in this geoJSON object");
                }

                try {
                    prototype = obj.getJSONArray("features").getJSONObject(0);
                    //get the first fe1ature and check that
                    JSONObject objGeom1 = prototype.getJSONObject("geometry");
                    objType = type(objGeom1.getString("type"));

                } catch (IndexOutOfBoundsException ioex) {
                    //no mappable features found
                    objType = -1;
                }

                break;

            default:
                logger.debug("Object must be feature or feature collection");
        }

        return objType;
    }

    @Override
    public String getFirstFeatureValue(JSONObject obj, String key) {
        JSONObject prototype = null;
        int objType = type(obj.getString("type"));
        String value = "";

        switch (objType) {
            case FEATURE:
                prototype = obj;
                JSONObject objProps = prototype.getJSONObject("properties");
                if (objProps.containsKey(key)) {
                    value = objProps.getString(key);
                }

                break;

            case FEATURECOLLECTION:

                if (!obj.containsKey("features")) {
                    logger.debug("no features in this geoJSON object");
                }

                try {
                    int countFeatures = obj.getJSONArray("features").size();
                    logger.debug("Iterating thru' " + countFeatures + " features");
                    for (int i = 0; i < countFeatures; i++) {
                        JSONObject o = obj.getJSONArray("features").getJSONObject(i);
                        JSONObject oprop;
                        try {
                            oprop = o.getJSONObject("properties");

                        } catch (Exception e) {
                            oprop = o.getJSONArray("properties").getJSONObject(0);
                        }
                        
                        //System.out.println("key value: " + oprop.getString(key));
                        if (oprop.containsKey(key)) {
                            value = oprop.getString(key);
                            if (!value.trim().equalsIgnoreCase("")) {
                                break;
                            }
                        }
                    }

                } catch (IndexOutOfBoundsException ioex) {
                    //no mappable features found
                    value = "";
                }

                break;

            default:
                logger.debug("Object must be feature or feature collection");
        }

        System.out.println("geojson.value: " + value); 

        return value;
    }

    private int type(String type) {
        if (typeMap == null) {
            typeMap = new HashMap();
            typeMap.put("feature", Integer.valueOf(FEATURE));
            typeMap.put("featurecollection", Integer.valueOf(FEATURECOLLECTION));
            typeMap.put("point", Integer.valueOf(POINT));
            typeMap.put("linestring", Integer.valueOf(LINESTRING));
            typeMap.put("polygon", Integer.valueOf(POLYGON));
            typeMap.put("multipoint", Integer.valueOf(MULTIPOINT));
            typeMap.put("multilinestring", Integer.valueOf(MULTILINESTRING));
            typeMap.put("multipolygon", Integer.valueOf(MULTIPOLYGON));
            typeMap.put("geometrycollection",
                    Integer.valueOf(GEOMETRYCOLLECTION));
        }

        Integer val = (Integer) typeMap.get(type.toLowerCase());

        if (val == null) {
            logger.debug("Unknown object type '" + type + "'");
        }

        return val.intValue();
    }
}
