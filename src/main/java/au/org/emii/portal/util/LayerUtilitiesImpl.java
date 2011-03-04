package au.org.emii.portal.util;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.log4j.Priority;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * WMS and ncWMS/THREDDS url manipulation class
 *
 * Supported types (string representation):
 *
 *      "WMS-1.0.0"
 *      "WMS-LAYER-1.0.0"               
 *      "WMS-1.1.0"
 *      "WMS-LAYER-1.1.0"
 *      "WMS-1.1.1"
 *      "WMS-LAYER-1.1.1"
 *      "WMS-1.3.0"
 *      "WMS-LAYER-1.3.0"
 *      "NCWMS"
 *      "THREDDS"
 *      "GEORSS"
 *      "KML"
 *      "WKT"
 *      "AUTO"  NOTE: Auto discover WMS server - only has meaning during discovery process
 * @author geoff
 *
 */
public class LayerUtilitiesImpl implements LayerUtilities {

    private Logger logger = Logger.getLogger(getClass());
    private final static String GEOSERVER_REGEXP = "[Gg][Ee][Oo][Ss][Ee][Rr][Vv][Ee][Rr]";
    private final static String NCWMS_REGEXP = "[Nn][Cc][Ww][Mm][Ss]";
    private final static String IMAGE_FORMAT_REGEXP = "[Ff][Oo][Rr][Mm][Aa][Tt]";
    private final static String LAYERS_REGEXP = "[Ll][Aa][Yy][Ee][Rr][Ss]";
    private final static String LAYER_REGEXP = "[Ll][Aa][Yy][Ee][Rr]";
    private final static String VERSION_REGEXP = "[Vv][Ee][Rr][Ss][Ii][Oo][Nn]";
    private ArrayList<String> versions = null;
    private Settings settings = null;
    private SettingsSupplementary settingsSupplementary = null;
    private ResolveHostName resolveHostname = null;
    private List<Double> worldBBox = null;

    public LayerUtilitiesImpl() {
        versions = new ArrayList<String>();
        versions.add(WMS_1_0_0, "1.0.0");
        versions.add(WMS_1_1_0, "1.1.0");
        versions.add(WMS_1_1_1, "1.1.1");
        versions.add(WMS_1_3_0, "1.3.0");

        versions.add(NCWMS, "NCWMS");
        versions.add(THREDDS, "THREDDS");
        versions.add(GEORSS, "GEORSS");
        versions.add(KML, "KML");
        versions.add(GEOJSON, "GEOJSON");

        worldBBox = new ArrayList<Double>(4);
        worldBBox.add(-180.0);
        worldBBox.add(-90.0);
        worldBBox.add(180.0);
        worldBBox.add(90.0);
    }

    /**
     * Check whether the passed in type is compatible with any
     * WMS version.
     *
     * NcWMS and THREDDS are considered to be WMS compatible
     * @param type
     * @return
     */
    @Override
    public boolean supportsWms(int type) {
        return ((type == LayerUtilitiesImpl.WMS_1_0_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_1)
                || (type == LayerUtilitiesImpl.WMS_1_3_0)
                || (type == LayerUtilitiesImpl.NCWMS)
                || (type == LayerUtilitiesImpl.THREDDS));
    }

    /**
     * only NCWMS and THREDDS support metadata
     * @param type
     * @return
     */
    @Override
    public boolean supportsMetadata(int type) {
        return ((type == LayerUtilitiesImpl.NCWMS)
                || (type == LayerUtilitiesImpl.THREDDS));
    }

    /**
     * only NCWMS and THREDDS support animation
     * @param type
     * @return
     */
    @Override
    public boolean supportsAnimation(int type) {
        return ((type == LayerUtilitiesImpl.NCWMS)
                || (type == LayerUtilitiesImpl.THREDDS));
    }

    /**
     * Convert a string WMS version eg (1.3.0) to its integer
     * representation within the portal eg WMS_1_3_0
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

    @Override
    public String getNcWMSTimeStringsUri(String uri, String layer, String startDate, String endDate) {
        return uri
                + queryConjunction(uri)
                + "item=animationTimesteps"
                + "&layerName=" + layer
                + "&start=" + startDate
                + "&end=" + endDate
                + "&request=GetMetadata";
    }

    /**
     * Convert the internal integer constant representation of the
     * WMS version to an external string as used in the version=
     * URI parameter.  This is not the same as the version string
     * used in the config file.
     *
     * NCWMS and THREDDS are special cases and will return "1.3.0"
     * @param version
     * @return
     */
    @Override
    public String externalVersion(int version) {
        String externalVersion = null;
        if (version != UNSUPPORTED) {
            if (version == NCWMS || version == THREDDS) {
                // force ncwms to be v 1.3.0
                externalVersion = versions.get(WMS_1_3_0);
            } else {
                externalVersion = versions.get(version);
            }
        }
        return externalVersion;
    }

    /**
     * Remove Trailing garbage after lookbehind string
     *
     * http://www.foo.com/geoserver_special/wrongpage.do =>
     * 	http://www.foo.com/geoserver_special/
     *
     * http://www.bar.com/my_ncwms/data/wrongpage.html =>
     * 	http://www.bar.com/my_ncwms/
     */
    private String removeAfterSlash(String uri, String lookBehind) {
        return uri.replaceAll(
                "(" + lookBehind + "[^/]*/).*$?",
                "$1");
    }

    @Override
    public String getVersionValue(String uri) {
        return getParameterValue(VERSION_REGEXP, uri);
    }

    /**
     * Construct and return a URI for a 2x2 px test image
     * from a GetMap uri.  This can be used to test individual
     * wms layers are at least returning images when they are
     * added to the map by the user - we don't do this for
     * our own layers as we assume that they are working.
     * @param uri
     * @return
     */
    @Override
    public String getTestImageUri(String uri) {

        // step 1 - strip request param, bbox, width and height
        String testUri =
                stripUriRequest(stripUriBbox(stripUriWidth(stripUriHeight(uri))));


        /* LAYERS, CRS/SRS, VERSION and FORMAT should already
         * be set
         */
        testUri +=
                queryConjunction(uri)
                + "REQUEST=GetMap"
                + "&BBOX=1,1,2,2"
                + "&WIDTH=2"
                + "&HEIGHT=2";

        return testUri;
    }

    /**
     * Application specific url tweaking - works for geoserver and
     * ncwms at the moment
     *
     * Autodetect and tweak uri if we detect the presence of geoserver
     * or ncwms
     * @param uri original uri
     * @return mangled uri or null if geoserver or ncwms are not found or if
     * mangleing the uri would return the same value
     */
    @Override
    public String mangleUriApplication(String uri) {
        String mangled;
        String matchGeoserver = ".*?" + GEOSERVER_REGEXP + ".*?";
        String matchNcwms = ".*?" + NCWMS_REGEXP + ".*?";
        if ((uri.matches(matchGeoserver)) || (uri.matches(matchNcwms))) {
            if (uri.matches(matchGeoserver)) {
                // geoserver detected
                mangled = removeAfterSlash(uri, GEOSERVER_REGEXP);
            } else {
                mangled = removeAfterSlash(uri, NCWMS_REGEXP);
            }
            mangled += "/wms";

            // final check - only return the mangled uri if its different to the
            // one we started with - so that consumers can detect whether to bother
            // with further processing by testing for a null value
            if (mangled.equals(uri)) {
                mangled = null;
            }
        } else {
            mangled = null;
        }

        return mangled;
    }

    /**
     * Attempt to automatically construct a get capabilities uri for
     * the passed in WMS version
     * @param uri
     * @return
     */
    @Override
    public String mangleUriGetCapabilitiesAutoDiscover(String uri, int version) {
        LayerUtilitiesImpl wmsUtilities = new LayerUtilitiesImpl();

        // strip any existing version,request and service params
        String mangled = stripUriRequest(uri);
        mangled = stripUriService(mangled);
        mangled = stripUriVersion(mangled);

        /* if last char is a '?' or '&' we can append our query
         * directly, otherwise we need to append one ourself
         */
        mangled += queryConjunction(uri);

        // replace with our own params
        mangled +=
                "SERVICE=WMS&"
                + "REQUEST=GetCapabilities&"
                + "VERSION=" + wmsUtilities.externalVersion(version);

        return mangled;
    }

    /**
     * Return either "", "&" or "?" as a conjunction to be
     * used when generating urls based on the last character
     * in the passed in uri according to the following rules:
     *
     * 1) 	uri ends with '?' or '&' - uri is ready to use,  return ""
     * 2) 	uri doesn't end with '?' or '&' and contains and '?'
     * 		somewhere within the string - return '&'
     * 3)	uri doesn't end with '?' or '&' and doesn't contain '&'
     * 		anywhere within the string - return '?'
     * @param uri
     * @return
     */
    @Override
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

    /**
     * Strip any version information from the URI and use
     * the passed in version string instead
     * @param uri
     * @param version
     * @return
     */
    @Override
    public String fixVersion(String uri, String version) {
        String fixedUri = stripUriVersion(uri);
        fixedUri += queryConjunction(fixedUri);
        fixedUri += "&VERSION=" + version;
        return fixedUri;
    }

    @Override
    public String stripParameter(String parameter, String uri) {
        return uri.replaceAll(parameter + "=([^&]?)*&?", "");
    }

    @Override
    public String stripUriVersion(String uri) {
        return stripParameter("[Vv][Ee][Rr][Ss][Ii][Oo][Nn]", uri);
    }

    @Override
    public String stripUriService(String uri) {
        return stripParameter("[Ss][Ee][Rr][Vv][Ii][Cc][Ee]", uri);
    }

    @Override
    public String stripUriRequest(String uri) {
        return stripParameter("[Rr][Ee][Qq][Uu][Ee][Ss][Tt]", uri);
    }

    @Override
    public String stripUriBbox(String uri) {
        return stripParameter("[Bb][Bb][Oo][Xx]", uri);
    }

    @Override
    public String stripUriWidth(String uri) {
        return stripParameter("[Ww][Ii][Dd][Tt][Hh]", uri);
    }

    @Override
    public String stripUriHeight(String uri) {
        return stripParameter("[Hh][Ee][Ii][Gg][Hh][Tt]", uri);
    }

    @Override
    public int getMaxNameLength() {
        int maxLength;
        try {
            maxLength = Integer.parseInt(settingsSupplementary.getValue("layer_name_max_length"));
        } catch (NumberFormatException e) {
            maxLength = 20;
            logger.error(
                    "unable to parse an integer from layer_name_max_length key "
                    + "in config file - your configuration is broken.  Will attempt "
                    + "to continue using a sensible default value of: " + maxLength);
        }
        return maxLength;
    }

    @Override
    public boolean needsChomp(String originalName) {
        boolean needsChomp;
        if (originalName.length() > getMaxNameLength()) {
            needsChomp = true;
        } else {
            needsChomp = false;
        }
        return needsChomp;
    }

    @Override
    public String getTooltip(String name, String description) {
        String tooltip;
        tooltip = description;

        if (needsChomp(name)) {
            // show full name: description as tooltip for names we had to
            // chomp
            if (!name.equals(description)) {
                tooltip = "(" + name + ")" + "  " + description;
            }
        }
        return tooltip;
    }

    /**
     * Chomp the layer name if we need to
     * @param originalName
     * @return The original name if under maxLength or a chomped name
     * if over
     */
    @Override
    public String chompLayerName(String layerName) {
        //int length = layerName.length();

        layerName = layerName.replace('_', ' ');

        /* Chomp very long names, eg :
         * "mystupildylongandbrokennamethatscompletelyunreadable_v1"
         * "mystupildylongandbrokennamethatscompletelyunreadable_v2"
         *
         * become:
         * "mystupildylongandbrokennametha...v1"
         * "mystupildylongandbrokennametha...v2"
         */

        // limit it gracefully if possible
        if (needsChomp(layerName)) {

            BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(layerName);
            int first_after = bi.following(getMaxNameLength() - 10);
            layerName = layerName.substring(0, first_after);

            // forced limiting
            if (needsChomp(layerName)) {
                layerName =
                        layerName.substring(0, getMaxNameLength() - 1);
            }
            layerName = layerName + "...";
        }

        return layerName;
    }

    /**
     * Parse the image format from a uri
     * @param uri
     */
    @Override
    public String getImageFormat(String uri) {
        return getParameterValue(IMAGE_FORMAT_REGEXP, uri);
    }

    /**
     * Parse the requested layers from a uri
     * @param uri
     */
    @Override
    public String getLayers(String uri) {
        return getParameterValue(LAYERS_REGEXP, uri);
    }

    @Override
    public String getLayer(String uri) {
        return getParameterValue(LAYER_REGEXP, uri);
    }

    @Override
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
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("missing URL encoder! ", e);
            }
        }
        return value;
    }

    @Override
    public String getLegendGraphicUri(String uri, String imageFormat) {
        // kill request=getmap
        String legendUri = stripParameter("[Rr][Ee][Qq][Uu][Ee][Ss][Tt]", uri);

        legendUri +=
                queryConjunction(uri)
                + "LEGEND_OPTIONS=forceLabels:on"
                + "&REQUEST=GetLegendGraphic"
                + "&FORMAT=" + imageFormat;

        // layer parameter must always be set - guess it from LAYERS
        // if it's currently empty
        if (getLayer(uri) == null) {
            legendUri += "&LAYER=" + getLayers(uri);
        }

        return legendUri;
    }

    @Override
    public String getLegendGraphicUri(String uri, String layer, String imageFormat) {
        return getLegendGraphicUri(
                uri + queryConjunction(uri) + "LAYER=" + layer,
                imageFormat);
    }

    @Override
    public String getLegendGraphicUri(String uri, String layer, String imageFormat, String envParams) {

        //get the template and add the envParams into it

        return getLegendGraphicUri(
                uri + queryConjunction(uri) + "LAYER=" + layer,
                imageFormat);
    }

    /**
     * Construct a metadata uri.  For use with ncwms/thredds
     * @param uri
     * @param layer
     * @return
     */
    @Override
    public String getMetadataUri(String uri, String layer) {
        return uri
                + queryConjunction(uri)
                + "item=layerDetails"
                + "&layerName=" + layer
                + "&request=GetMetadata";
    }

    @Override
    public String getTimestepsUri(String uri, String layer, Date date) {
        DateFormat df = Validate.getIsoDateFormatter();
        return uri
                + queryConjunction(uri)
                + "item=timesteps"
                + "&layerName=" + layer
                + "&request=GetMetadata"
                + "&day=" + df.format(date);
    }

    /**
     * Generate the URI to request an animated image.
     *
     * Doesn't currently support:
     * 	elevation
     * 	colorscalerange
     * 	numcolorbands
     * 	logscale
     * But probably will in the future
     * @param mapLayer
     * @return
     */
    private String getAnimationBaseUri(MapLayer mapLayer) {

        String baseUri =
                getFQUri(mapLayer.getUri())
                + queryConjunction(mapLayer.getUri())
                + "TRANSPARENT=true"
                + //			"&ELEVATION=" + mapLayer.getAnimationParameters().getElevation() +
                "&STYLES=" + mapLayer.getSelectedStyleName()
                + "&CRS=EPSG%3A4326"
                + //			"&COLORSCALERANGE=9.405405%2C29.66159" +
                //			"&NUMCOLORBANDS=254" +
                //			"&LOGSCALE=false" +
                "&SERVICE=WMS"
                + "&VERSION=" + getWmsVersion(mapLayer)
                + "&EXCEPTIONS=XML"
                + "&FORMAT=image/gif";

        return baseUri;
    }

    /**
     * Get the fully qualified URI (if it isn't already...)
     * - this is to add a hostname to indirect cache requests
     * which normally store the URI as /RemoteRequest?url=foo
     */
    @Override
    public String getFQUri(String uri) {
        String fqUri;
        if (!uri.startsWith("http://")) {
            fqUri = "http://" + resolveHostname.resolveHostName();

            // be tolerant of missing/extra slashes in the uri
            if (!uri.startsWith("/")) {
                fqUri += "/";
            }
            fqUri += uri;
        } else {
            fqUri = uri;
        }
        return fqUri;
    }

    @Override
    public String getAnimationUriJS(MapLayer mapLayer) {
        return StringEscapeUtils.escapeJavaScript(getAnimationUri(mapLayer));
    }

    /**
     * safe to assume by now that we have accumulated enough
     * parameters to need & to join our query...
     *
     * Width and heights are numbers in the config file - no
     * need to parse them to int thought because they're going
     * to be used in a string straight away
     */
    @Override
    public String getAnimationUri(MapLayer mapLayer) {


        return getAnimationBaseUri(mapLayer)
                + "&TIME=" + mapLayer.getAnimationSelection().getSelectedTimeString()
                + "&LAYERS=" + mapLayer.getLayer()
                + "&REQUEST=GetMap"
                + "&WIDTH=" + settingsSupplementary.getValue("animation_width")
                + "&HEIGHT=" + settingsSupplementary.getValue("animation_height")
                + "&BBOX=" + mapLayer.getMapLayerMetadata().getBboxString();
    }

    @Override
    public String getAnimationFeatureInfoUriJS(MapLayer mapLayer) {
        return StringEscapeUtils.escapeJavaScript(getAnimationFeatureInfoUri(mapLayer));
    }

    @Override
    public String getAnimationFeatureInfoUri(MapLayer mapLayer) {
        return getAnimationBaseUri(mapLayer)
                + "&TIME="
                + mapLayer.getAnimationSelection().getAdjustedStartDate() + "/"
                + mapLayer.getAnimationSelection().getAdjustedEndDate()
                + "&REQUEST=GetFeatureInfo"
                + "&QUERY_LAYERS=" + mapLayer.getLayer();
    }

    @Override
    public String getAnimationTimeSeriesPlotUriJS(MapLayer mapLayer) {
        return StringEscapeUtils.escapeJavaScript(getAnimationTimeSeriesPlotUri(mapLayer));
    }

    @Override
    public String getAnimationTimeSeriesPlotUri(MapLayer mapLayer) {
        return getAnimationFeatureInfoUri(mapLayer)
                + "&INFO_FORMAT=image/png";
    }

    /** indirect caching requested - base url is to be requested
     * through our RemoteRequest servlet so squid can cache it
     * @throws UnsupportedEncodingException
     */
    @Override
    public String getIndirectCacheUrl(String baseUri) {
        String cacheUri =
                settings.getCacheUrl()
                + "?"
                + settings.getCacheParameter()
                + "=";
        try {
            cacheUri += URLEncoder.encode(baseUri, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen unless your jdk is FUBAR
            logger.error("missing URL encoder! ", e);

            // will have to include the naked baseuri and hope for the best
            cacheUri += baseUri;
        }
        return cacheUri;

    }

    @Override
    public int getWms100() {
        return WMS_1_0_0;
    }

    @Override
    public int getWms110() {
        return WMS_1_1_0;
    }

    @Override
    public int getWms111() {
        return WMS_1_1_1;
    }

    @Override
    public int getWms130() {
        return WMS_1_3_0;
    }

    @Override
    public int getNcwms() {
        return NCWMS;
    }

    @Override
    public int getThredds() {
        return THREDDS;
    }

    @Override
    public int getGeorss() {
        return GEORSS;
    }

    @Override
    public int getKml() {
        return KML;
    }

    @Override
    public int getGeojson() {
        return GEOJSON;
    }

    @Override
    public int getWkt() {
        return WKT;
    }

    @Override
    public int getUnsupported() {
        return UNSUPPORTED;
    }

    /**
     * Accessor to get the magic string currently being used to trigger
     * auto discovery when used as 'type' in a Discovery instance.  Gets
     * used in the AddLayer.zul file since ZUL EL can't access static
     * variables (booooo!)
     * @return
     */
    @Override
    public String getAutoDiscoveryType() {
        return AUTO_DISCOVERY_TYPE;
    }

    /**
     * Get the list of supported versions as a human readable string (for
     * debugging) - noone outside this class should need to get r/w access
     * to the list of versions since they can just test for == UNSUPPORTED
     * @return
     */
    @Override
    public String getSupportedVersions() {
        StringBuffer sb = new StringBuffer();
        boolean once = false;
        for (String version : versions) {
            if (once) {
                sb.append(", ");
            }
            sb.append("'" + version + "'");
            once = true;
        }
        sb.append(" for automatic discovery, use type '" + AUTO_DISCOVERY_TYPE + "'");
        return sb.toString();
    }

    /**
     * Attempt to turn a regular getmap URI into a legend uri,
     * then set the default legend uri to the generated value
     * @param uri
     * @return
     */
    @Override
    public String coerceLegendUri(MapLayer mapLayer) {
        String legendUri = getFQUri(mapLayer.getUri());
        /* FIXME! Temporary hack to get legend working for thredds -
         * we need to send LAYER= and LAYERS= or it returns an
         * error
         */
        if (mapLayer.getType() == LayerUtilities.THREDDS) {
            legendUri += queryConjunction(mapLayer.getUri()) + "LAYERS=" + mapLayer.getLayer();
        }

        return getLegendGraphicUri(legendUri, mapLayer.getLayer(), mapLayer.getImageFormat());

    }

    /**
     * Get the external wms version string for this layer, eg "1.3.0"
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

    public ResolveHostName getResolveHostname() {
        return resolveHostname;
    }

    @Required
    public void setResolveHostname(ResolveHostName resolveHostname) {
        this.resolveHostname = resolveHostname;
    }

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    @Required
    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    /**
     * get bounding box for wms getlayer uri from GetCapabilities response
     *
     * @param uri wms server get layer uri as String, must contain "layers="
     * @return bounding box as List<Double>
     */
    @Override
    public List<Double> getBBox(String uri) {
        try {
            List<Double> bbox = new ArrayList<Double>();

            //extract server uri
            String server = "";
            int q = uri.indexOf('?');
            if (q > 0) {
                server = uri.substring(0, uri.substring(0, q).lastIndexOf('/') + 1);
            } else {
                server = uri.substring(0, uri.lastIndexOf('/') + 1);
            }

            //extract layer name
            String name = "";
            int a = uri.toLowerCase().indexOf("layers=");
            if (a > 0) {
                int b = uri.toLowerCase().substring(a, uri.length()).indexOf("&");
                if (b > 0) {
                    //name is between a+len(layer=) and a+b
                    name = uri.substring(a + 7, a + b);
                } else {
                    //name is between a+len(layer=) and len(uri)
                    name = uri.substring(a + 7, uri.length());
                }
            }

            //don't use gwc/service/ because it is returning the wrong boundingbox
            server = server.replace("gwc/service/","");

            //make getcapabilities uri
            String wmsget = mangleUriGetCapabilitiesAutoDiscover(server + "wms", WMS_1_0_0);

            //get boundingbox for this layer by checking against each title and name
            Document doc = parseXml(wmsget);
            if (doc == null) {
                return worldBBox;
            }
            NodeList nl = doc.getElementsByTagName("Layer");
            int i, j;
            for (i = 0; i < nl.getLength(); i++) {
                NodeList layer = nl.item(i).getChildNodes();
                boolean match = false;
                for (j = 0; j < layer.getLength(); j++) {
                    if (layer.item(j).getNodeName().equals("Name")
                            || layer.item(j).getNodeName().equals("Title")) {
                        if (layer.item(j).getTextContent().equalsIgnoreCase(name)) {
                            match = true;
                        }
                    } else if (match
                            && (layer.item(j).getNodeName().equals("BoundingBox")
                            || layer.item(j).getNodeName().equals("LatLonBoundingBox"))) {
                        bbox.add(Double.parseDouble(layer.item(j).getAttributes().getNamedItem("minx").getNodeValue()));
                        bbox.add(Double.parseDouble(layer.item(j).getAttributes().getNamedItem("miny").getNodeValue()));
                        bbox.add(Double.parseDouble(layer.item(j).getAttributes().getNamedItem("maxx").getNodeValue()));
                        bbox.add(Double.parseDouble(layer.item(j).getAttributes().getNamedItem("maxy").getNodeValue()));
                        break;
                    }
                }
            }
            return bbox;
        } catch (Exception ex) {
            // java.util.logging.Logger.getLogger(LayerUtilitiesImpl.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return worldBBox;
    }

    public List<Double> getBBoxIndex(String uri) {
        //get bounds of layer
        List<Double> bbox = new ArrayList(4);

        try {
            JSONArray layerlist = CommonData.getLayerListJSONArray();

            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);
                if (jo.getString("displaypath").equals(uri)) {
                    bbox.add(Double.parseDouble(jo.getString("minlongitude")));
                    bbox.add(Double.parseDouble(jo.getString("minlatitude")));
                    bbox.add(Double.parseDouble(jo.getString("maxlongitude")));
                    bbox.add(Double.parseDouble(jo.getString("maxlatitude")));

                    return bbox;
                }
            }
        } catch (Exception e) {
        }
        bbox = getBBox(uri);
        return bbox;
    }

    /**
     * parse XML uri to Document
     *
     * original in: WMSSupportNonXmlBeans.java
     */
    protected Document parseXml(String discoveryUri) {
        boolean parseError = false;
        boolean readError = false;
        String lastErrorMessage = "";

        HttpConnection httpConnection = null;

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

        /*
         * Everything on the internet says set the next variable to true but if I
         * do this, I can't select the xpath variable I want (which is in another
         * namespace) - setting namespace aware to false fixes things...
         */
        domFactory.setNamespaceAware(false);


        /*
         * DISABLE DTD Validation
         * ======================
         * By default, the DTD is processed when we parse the XML and this has the effect
         * of setting queryable="0" as a defalt attribute on all layers.  Popular implementations
         * (mapserver) just leave off the queryable attribute on layers which ARE queryable, thus
         * marking them as non-queryable.
         *
         * The solution is to totally disable DTD validation, - here's where I found out how to
         * do it:
         *
         * http://stackoverflow.com/questions/582352/how-can-i-ignore-dtd-validation-but-keep-the-doctype-when-writing-an-xml-file
         */
        //careful... this next line will sneakily re-enable namespaces and break everything
        //domFactory.setAttribute("http://xml.org/sax/features/namespaces", true);
        domFactory.setAttribute("http://xml.org/sax/features/validation", false);
        domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder documentBuilder = null;
        Document document = null;
        try {
            documentBuilder = domFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(null);
            document = documentBuilder.parse(discoveryUri);//.parse(is);

        } catch (SAXException e) {
            parseError = true;
            lastErrorMessage = "Unable to parse a GetCapabilities document from '" + discoveryUri
                    + "' (parser error - is XML well formed?)";
        } catch (ParserConfigurationException e) {
            parseError = true;
            lastErrorMessage = "Unable to parse a GetCapabilities document from '"
                    + discoveryUri + "' (parser configuration error)";
        } catch (IOException e) {
            readError = true;
            // for 404 errors, the message will be the requested url
            lastErrorMessage = "IO error connecting to server at '" + discoveryUri + "'.  Root cause: "
                    + e.getMessage();
        }

        // discard broken documents
        if (readError || parseError) {
            document = null;
        }
        return document;
    }
}
