package au.org.emii.portal.util;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.Settings;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WMS and ncWMS/THREDDS url manipulation class
 * <p/>
 * Supported types (string representation):
 * <p/>
 * "WMS-1.0.0"
 * "WMS-LAYER-1.0.0"
 * "WMS-1.1.0"
 * "WMS-LAYER-1.1.0"
 * "WMS-1.1.1"
 * "WMS-LAYER-1.1.1"
 * "WMS-1.3.0"
 * "WMS-LAYER-1.3.0"
 * "NCWMS"
 * "THREDDS"
 * "GEORSS"
 * "KML"
 * "WKT"
 * "AUTO"  NOTE: Auto discover WMS server - only has meaning during discovery process
 *
 * @author geoff
 */
public class LayerUtilitiesImpl implements LayerUtilities {

    /**
     * Keyword to use in the config file for auto discovered wms servers
     */
    public static final int KML = 4;
    public static final int GEOJSON = 5;
    public static final int WKT = 11;
    public static final int IMAGELAYER = 9;
    /**
     * layer dummy and sub types
     */
    public static final int MAP = 12;
    public static final int ALOC = 14;
    public static final int MAXENT = 15;
    public static final int GDM = 16;
    public static final int SCATTERPLOT = 17;
    public static final int TABULATION = 18;
    public static final int CONTEXTUAL = 19;
    public static final int GRID = 20;
    public static final int SPECIES = 21;
    public static final int ENVIRONMENTAL_ENVELOPE = 22;
    public static final int SPECIES_UPLOAD = 23;
    public static final int ODENSITY = 24;
    public static final int SRICHNESS = 25;
    public static final int POINT = 25;
    /**
     * Constants used to indicate layer type.  Do not hard code these
     * value into other applications - they may change.  Instead, for
     * external non-java applications use the String representations
     * (above).
     * <p/>
     * Java applications may reference the constants in this class
     * statically.  If you need to access through EL (for .zul), the
     * LayerUtilies class is instantiated and stored in the users session
     * with the static fields exposed by getters, e.g:
     * <p/>
     * <script type="text/javascript>
     * var wms100 = ${session.attributes.portalSession.layerUtilities.wms100};
     * var wms110 = ${session.attributes.portalSession.layerUtilities.wms110};
     * var wms111 = ${session.attributes.portalSession.layerUtilities.wms111};
     * var wms130 = ${session.attributes.portalSession.layerUtilities.wms130};
     * var ncwms = ${session.attributes.portalSession.layerUtilities.ncwms};
     * var thredds = ${session.attributes.portalSession.layerUtilities.thredds};
     * var georss = ${session.attributes.portalSession.layerUtilities.georss};
     * var kml = ${session.attributes.portalSession.layerUtilities.kml};
     * var unsupported =${session.attributes.portalSession.layerUtilities.unsupported};
     * </script>
     */
    public static final int UNSUPPORTED = -1;
    public static final int WMS_1_0_0 = 0;
    public static final int WMS_1_1_0 = 1;
    public static final int WMS_1_1_1 = 2;
    public static final int WMS_1_3_0 = 3;
    private static final Logger LOGGER = Logger.getLogger(LayerUtilitiesImpl.class);
    private static final String IMAGE_FORMAT_REGEXP = "[Ff][Oo][Rr][Mm][Aa][Tt]";
    private static final String LAYERS_REGEXP = "[Ll][Aa][Yy][Ee][Rr][Ss]";
    private static final String LAYER_REGEXP = "[Ll][Aa][Yy][Ee][Rr]";
    private static final String VERSION_REGEXP = "[Vv][Ee][Rr][Ss][Ii][Oo][Nn]";
    private List<String> versions = null;
    private Settings settings = null;
    private List<Double> worldBBox = null;

    public LayerUtilitiesImpl() {
        versions = new ArrayList<String>();

        versions.add(WMS_1_0_0, "1.0.0");
        versions.add(WMS_1_1_0, "1.1.0");
        versions.add(WMS_1_1_1, "1.1.1");
        versions.add(WMS_1_3_0, "1.3.0");
        versions.add(KML, "KML");
        versions.add(GEOJSON, "GEOJSON");

        worldBBox = new ArrayList<Double>(4);
        worldBBox.add(-179.999);
        worldBBox.add(-89.999);
        worldBBox.add(179.999);
        worldBBox.add(89.999);
    }

    /**
     * Check whether the passed in type is compatible with any
     * WMS version.
     * <p/>
     * NcWMS and THREDDS are considered to be WMS compatible
     *
     * @param type
     * @return
     */
    public boolean supportsWms(int type) {
        return (type == LayerUtilitiesImpl.WMS_1_0_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_1)
                || (type == LayerUtilitiesImpl.WMS_1_3_0);
    }


    /**
     * Convert a string WMS version eg (1.3.0) to its integer
     * representation within the portal eg WMS_1_3_0
     *
     * @param requestedType
     * @return Integer constant representing string representation
     * of WMS version.  Returns UNSUPPORTED if nothing matches
     */
    @Override
    public int internalVersion(String requestedType) {
        int version = UNSUPPORTED;
        if (requestedType != null) {
            // strip WMS- or WMS-LAYER- from version number
            String realVersion =
                    requestedType.replaceAll(
                            "[Ww][Mm][Ss]-([Ll][Aa][Yy][Ee][Rr]-)?", "");

            // Get the version (integer constant) - handily
            // returns -1 (unsupported) if no match
            version = versions.indexOf(realVersion);
        }
        return version;
    }


    /**
     * Convert the internal integer constant representation of the
     * WMS version to an external string as used in the version=
     * URI parameter.  This is not the same as the version string
     * used in the config file.
     * <p/>
     * NCWMS and THREDDS are special cases and will return "1.3.0"
     *
     * @param version
     * @return
     */
    public String externalVersion(int version) {
        String externalVersion = null;
        if (version != UNSUPPORTED) {
            externalVersion = versions.get(version);
        }
        return externalVersion;
    }

    @Override
    public String getVersionValue(String uri) {
        return getParameterValue(VERSION_REGEXP, uri);
    }

    /**
     * Parse the image format from a uri
     *
     * @param uri
     */
    @Override
    public String getImageFormat(String uri) {
        return getParameterValue(IMAGE_FORMAT_REGEXP, uri);
    }

    /**
     * Return either "", "&" or "?" as a conjunction to be
     * used when generating urls based on the last character
     * in the passed in uri according to the following rules:
     * <p/>
     * 1)  uri ends with '?' or '&' - uri is ready to use,  return ""
     * 2)  uri doesn't end with '?' or '&' and contains and '?'
     * somewhere within the string - return '&'
     * 3) uri doesn't end with '?' or '&' and doesn't contain '&'
     * anywhere within the string - return '?'
     *
     * @param uri
     * @return
     */
    public String queryConjunction(String uri) {
        String conjunction = "";
        char last = uri.charAt(uri.length() - 1);
        if ((last != '?') && (last != '&')) {
            if (uri.contains("?")) {
                conjunction += "&";
            } else {
                conjunction += "?";
            }
        }
        return conjunction;
    }

    public String stripParameter(String parameter, String uri) {
        return uri.replaceAll(parameter + "=([^&]?)*&?", "");
    }

    public String stripUriVersion(String uri) {
        return stripParameter("[Vv][Ee][Rr][Ss][Ii][Oo][Nn]", uri);
    }

    public String stripUriService(String uri) {
        return stripParameter("[Ss][Ee][Rr][Vv][Ii][Cc][Ee]", uri);
    }

    public String stripUriRequest(String uri) {
        return stripParameter("[Rr][Ee][Qq][Uu][Ee][Ss][Tt]", uri);
    }

    public String getLayer(String uri) {
        String layer = getParameterValue(LAYER_REGEXP, uri);

        //found some layers are defined with both layer and layers
        if (layer == null) {
            layer = getParameterValue(LAYERS_REGEXP, uri);
        }

        return layer;
    }

    /**
     * Parse the requested layers from a uri
     *
     * @param uri
     */
    @Override
    public String getLayers(String uri) {
        String layers = getParameterValue(LAYERS_REGEXP, uri);

        //found some layers are defined with both layer and layers
        if (layers == null) {
            layers = getParameterValue(LAYER_REGEXP, uri);
        }
        return layers;
    }

    public String getParameterValue(String parameter, String uri) {
        String value = null;

        // parameter value will be held in $1
        String fullRegexp = ".*?" + parameter + "=([^&]*)&?.*";
        Pattern pattern = Pattern.compile(fullRegexp);
        Matcher matcher = pattern.matcher(uri);
        while (matcher.find()) {
            // got a match
            value = matcher.group(1);
        }
        // base20 decode it as well
        if (value != null) {
            try {
                value = URLDecoder.decode(value, StringConstants.UTF_8);
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("missing URL encoder! ", e);
            }
        }
        return value;
    }

    /**
     * Get the external wms version string for this layer, eg "1.3.0"
     *
     * @return the wms version string if this layer supports WMS
     * otherwise returns null
     */
    @Override
    public String getWmsVersion(MapLayer mapLayer) {
        String version;
        if (supportsWms(mapLayer.getType())) {
            version = externalVersion(mapLayer.getType());
        } else {
            version = null;
        }
        return version;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * get bounding box for wms getlayer uri from GetCapabilities response
     *
     * @param uri wms server get layer uri as String, must contain "layers="
     * @return bounding box as List<Double>
     */
    @Override
    public List<Double> getBBox(String uri) {
        return getBBoxWCSWFS(uri);
    }

    /**
     * get bounding box for wcs or wfs getlayer uri from GetCapabilities response
     *
     * @param uri wms server get layer uri as String, must contain "layers="
     * @return bounding box as List<Double>
     */
    private List<Double> getBBoxWCSWFS(String uri) {
        try {
            List<Double> bbox = new ArrayList<Double>();

            //extract server uri
            String server;
            int q = uri.indexOf('?');
            if (q > 0) {
                server = uri.substring(0, uri.substring(0, q).lastIndexOf('/') + 1);
            } else {
                server = uri.substring(0, uri.lastIndexOf('/') + 1);
            }

            //extract layer name
            String name = getLayer(uri);

            //don't use gwc/service/ because it is returning the wrong boundingbox
            server = server.replace("gwc/service/", "");

            //make getcapabilities uri
            LayerUtilitiesImpl wmsUtilities = new LayerUtilitiesImpl();

            // strip any existing version,request and service params
            String mangled = stripUriRequest(server + "wcs");
            mangled = stripUriService(mangled);
            mangled = stripUriVersion(mangled);

            /* if last char is a '?' or '&' we can append our query
             * directly, otherwise we need to append one ourself
             */
            mangled += queryConjunction(server + "wcs");

            // replace with our own params
            mangled +=
                    "SERVICE=WCS&"
                            + "REQUEST=GetCapabilities&"
                            + "VERSION=" + wmsUtilities.externalVersion(WMS_1_1_1);

            //get boundingbox for this layer by checking against each title and name
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(mangled);
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            int startPos = slist.indexOf("<ows:Title>" + name.replace("ALA:", "") + "</ows:Title>");
            if (startPos == -1) {
                //attempt to find by identifier
                startPos = slist.indexOf("<wcs:Identifier>" + name.replace("ALA:", "") + "</wcs:Identifier>");
                //shift startPos backwards to Title
                startPos = slist.lastIndexOf("<ows:Title>", startPos);
            }

            //not found, try WFS
            if (startPos == -1) {
                //get boundingbox for this layer by checking against each title and name
                client = new HttpClient();
                get = new GetMethod(mangled.replace("WCS", "WFS").replace("wcs", "wfs"));
                get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);
                client.executeMethod(get);
                slist = get.getResponseBodyAsString();

                startPos = slist.indexOf("<Name>" + name + "</Name>");
            }

            if (startPos == -1) {
                LOGGER.debug("BoundingBox not found for layer: " + name);
                return worldBBox;
            }

            String lc = "ows:LowerCorner>";
            String uc = "ows:UpperCorner>";
            int lowerCornerPosStart = slist.indexOf(lc, startPos) + lc.length();
            int lowerCornerPosEnd = slist.indexOf("</" + lc, lowerCornerPosStart);
            int upperCornerPosStart = slist.indexOf(uc, startPos) + uc.length();
            int upperCornerPosEnd = slist.indexOf("</" + uc, upperCornerPosStart);

            String[] lowerCorner = slist.substring(lowerCornerPosStart, lowerCornerPosEnd).split(" ");
            String[] upperCorner = slist.substring(upperCornerPosStart, upperCornerPosEnd).split(" ");

            bbox.add(Double.parseDouble(lowerCorner[0]));
            bbox.add(Double.parseDouble(lowerCorner[1]));
            bbox.add(Double.parseDouble(upperCorner[0]));
            bbox.add(Double.parseDouble(upperCorner[1]));

            return bbox;
        } catch (Exception e) {
            return worldBBox;
        }
    }
}
