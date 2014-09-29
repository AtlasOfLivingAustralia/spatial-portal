package au.org.ala.spatial.util;

import au.org.ala.spatial.StringConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * joins active layer names to
 * - species names
 * - environmental layer names
 * - contextual layer names
 * <p/>
 * can maintain minimum distance between selected and remaining layers.
 * <p/>
 * used to autocomplete layer entries
 *
 * @author adam
 */
public final class LayersUtil {

    public static final String LAYER_TYPE_PLAIN = StringConstants.TEXT_PLAIN;
    public static final String LAYER_TYPE_CSV = "text/csv";
    public static final String LAYER_TYPE_KML = "application/vnd.google-earth.kml+xml";
    public static final String LAYER_TYPE_CSV_EXCEL = "text/x-comma-separated-values";
    public static final String LAYER_TYPE_EXCEL = "application/vnd.ms-excel";
    public static final String LAYER_TYPE_ZIP = "application/zip";

    private LayersUtil() {
        //to hide public constructor
    }

    /**
     * generate basic HTML for metadata of a WKT layer.
     *
     * @param wkt as String
     * @return html as String
     */
    public static String getMetadataForWKT(String method, String wkt) {
        SimpleDateFormat sdf = new SimpleDateFormat(StringConstants.DATE_TIME_FORMAT);
        String date = sdf.format(new Date());
        String area = String.format("%,.2f", Util.calculateArea(wkt) / 1000000.0);
        String shortWkt = (wkt.length() > 300) ? wkt.substring(0, 300) + "..." : wkt;

        return getAreaMetadata(method + "<br>" + date + "<br>" + area + " sq km<br>" + shortWkt);
    }

    /**
     * generate basic HTML for metadata of a layer with a description.
     *
     * @return html as String
     */
    public static String getMetadata(String description) {
        SimpleDateFormat sdf = new SimpleDateFormat(StringConstants.DATE_TIME_FORMAT);
        String date = sdf.format(new Date());
        return getAreaMetadata(description + "<br>" + date);
    }

    static String getAreaMetadata(String data) {
        return "Area metadata\n" + data;
    }


}
