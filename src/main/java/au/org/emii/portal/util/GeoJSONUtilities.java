/*
 * Interface for GeoJSONUtilities
 *
 * used to parse the geojson and extract some
 * metadata about the layer
 */
package au.org.emii.portal.util;

import au.org.emii.portal.net.HttpConnection;
import net.sf.json.JSONObject;

/**
 * @author brendon
 */
public interface GeoJSONUtilities {

    //possible feature types
    public static final int FEATURE = 0;
    public static final int FEATURECOLLECTION = 1;
    public static final int POINT = 2;
    public static final int LINESTRING = 3;
    public static final int POLYGON = 4;
    public static final int MULTIPOINT = 5;
    public static final int MULTILINESTRING = 6;
    public static final int MULTIPOLYGON = 7;
    public static final int GEOMETRYCOLLECTION = 8;


    public HttpConnection getHttpConnection();

    public void setHttpConnection(HttpConnection httpConnection);

    public int getFirstFeatureType(JSONObject obj);

    public String getFirstFeatureValue(JSONObject obj, String key);

    public String getJson(String url);

    public int type(String type);
}
