package au.org.emii.portal.util;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.BreakIterator;
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

    private static Logger logger = Logger.getLogger(LayerUtilitiesImpl.class);
    private final static String GEOSERVER_REGEXP = "[Gg][Ee][Oo][Ss][Ee][Rr][Vv][Ee][Rr]";
    private final static String NCWMS_REGEXP = "[Nn][Cc][Ww][Mm][Ss]";
    private final static String IMAGE_FORMAT_REGEXP = "[Ff][Oo][Rr][Mm][Aa][Tt]";
    private final static String LAYERS_REGEXP = "[Ll][Aa][Yy][Ee][Rr][Ss]";
    private final static String LAYER_REGEXP = "[Ll][Aa][Yy][Ee][Rr]";
    private final static String VERSION_REGEXP = "[Vv][Ee][Rr][Ss][Ii][Oo][Nn]";
    private ArrayList<String> versions = null;
    private Settings settings = null;
    private SettingsSupplementary settingsSupplementary = null;
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
    @Override
    public boolean supportsWms(int type) {
        return ((type == LayerUtilitiesImpl.WMS_1_0_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_0)
                || (type == LayerUtilitiesImpl.WMS_1_1_1)
                || (type == LayerUtilitiesImpl.WMS_1_3_0));
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
    @Override
    public String externalVersion(int version) {
        String externalVersion = null;
        if (version != UNSUPPORTED) {
            externalVersion = versions.get(version);
        }
        return externalVersion;
    }

    /**
     * Remove Trailing garbage after lookbehind string
     * <p/>
     * http://www.foo.com/geoserver_special/wrongpage.do =>
     * http://www.foo.com/geoserver_special/
     * <p/>
     * http://www.bar.com/my_ncwms/data/wrongpage.html =>
     * http://www.bar.com/my_ncwms/
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
     *
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
     * <p/>
     * Autodetect and tweak uri if we detect the presence of geoserver
     * or ncwms
     *
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
     *
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
     * <p/>
     * 1) 	uri ends with '?' or '&' - uri is ready to use,  return ""
     * 2) 	uri doesn't end with '?' or '&' and contains and '?'
     * somewhere within the string - return '&'
     * 3)	uri doesn't end with '?' or '&' and doesn't contain '&'
     * anywhere within the string - return '?'
     *
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
     *
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
        return originalName.length() > getMaxNameLength();
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
     *
     * @param layerName
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
     *
     * @param uri
     */
    @Override
    public String getImageFormat(String uri) {
        return getParameterValue(IMAGE_FORMAT_REGEXP, uri);
    }

    /**
     * Parse the requested layers from a uri
     *
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
     *
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
        return getBBoxWCSWFS(uri);
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
     * <p/>
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
            server = server.replace("gwc/service/", "");

            //make getcapabilities uri
            //String wmsget = mangleUriGetCapabilitiesAutoDiscover(server + "wms", WMS_1_0_0);
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
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
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
                get.addRequestHeader("Accept", "text/plain");
                result = client.executeMethod(get);
                slist = get.getResponseBodyAsString();

                startPos = slist.indexOf("<Name>" + name + "</Name>");
            }

            if (startPos == -1) {
                logger.debug("BoundingBox not found for layer: " + name);
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
