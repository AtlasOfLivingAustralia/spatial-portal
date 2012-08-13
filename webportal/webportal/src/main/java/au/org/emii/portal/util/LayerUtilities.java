/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.menu.MapLayer;
import java.util.Date;
import java.util.List;

/**
 *
 * @author geoff
 */
public interface LayerUtilities {
    /**
     * Keyword to use in the config file for auto discovered wms servers
     */
    public String AUTO_DISCOVERY_TYPE = "AUTO";
    public int GEORSS = 6;
    public int KML = 7;
    public int GEOJSON = 8;
    public int WKT = 11;
    public int IMAGELAYER = 9;
    public int NCWMS = 4;
    public int THREDDS = 5;
    public int BING = 10;
    /**
     * layer dummy and sub types
     */
    public int MAP = 12;
    public int ALOC = 14;
    public int MAXENT = 15;
    public int GDM = 16;
    public int SCATTERPLOT = 17;
    public int TABULATION = 18;
    public int CONTEXTUAL = 19;
    public int GRID = 20;
    public int SPECIES = 21;
    public int ENVIRONMENTAL_ENVELOPE = 22;
    public int SPECIES_UPLOAD = 23;
    public int ODENSITY = 24;
    public int SRICHNESS = 25;
    /**
     * Constants used to indicate layer type.  Do not hard code these
     * value into other applications - they may change.  Instead, for
     * external non-java applications use the String representations
     * (above).
     *
     * Java applications may reference the constants in this class
     * statically.  If you need to access through EL (for .zul), the
     * LayerUtilies class is instantiated and stored in the users session
     * with the static fields exposed by getters, e.g:
     *
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
    public int UNSUPPORTED = -1;
    public int WMS_1_0_0 = 0;
    public int WMS_1_1_0 = 1;
    public int WMS_1_1_1 = 2;
    public int WMS_1_3_0 = 3;

    public String BING_SHADED = "VEMapStyle.Shaded";
    public String BING_AERIAL = "VEMapStyle.Aerial";
    public String BING_HYBRID = "VEMapStyle.Hybrid";

    /**
     * Chomp the layer name if we need to
     * @param originalName
     * @return The original name if under maxLength or a chomped name
     * if over
     */
    public String chompLayerName(String layerName);

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
    public String externalVersion(int version);

    /**
     * Strip any version information from the URI and use
     * the passed in version string instead
     * @param uri
     * @param version
     * @return
     */
    public String fixVersion(String uri, String version);

    public String getAnimationFeatureInfoUri(MapLayer mapLayer);

    public String getAnimationFeatureInfoUriJS(MapLayer mapLayer);

    public String getAnimationTimeSeriesPlotUri(MapLayer mapLayer);

    public String getAnimationTimeSeriesPlotUriJS(MapLayer mapLayer);

    public String getAnimationUri(MapLayer mapLayer);

    public String getAnimationUriJS(MapLayer mapLayer);

    /**
     * Accessor to get the magic string currently being used to trigger
     * auto discovery when used as 'type' in a Discovery instance.  Gets
     * used in the AddLayer.zul file since ZUL EL can't access static
     * variables (booooo!)
     * @return
     */
    public String getAutoDiscoveryType();

    /**
     * Get the fully qualified URI (if it isn't already...)
     * - this is to add a hostname to indirect cache requests
     * which normally store the URI as /RemoteRequest?url=foo
     */
    public String getFQUri(String uri);

    public int getGeorss();

    /**
     * Parse the image format from a uri
     * @param uri
     */
    public String getImageFormat(String uri);

    /**
     * indirect caching requested - base url is to be requested
     * through our RemoteRequest servlet so squid can cache it
     * @throws UnsupportedEncodingException
     */
    public String getIndirectCacheUrl(String baseUri);

    public int getKml();

    public String getLayer(String uri);

    /**
     * Parse the requested layers from a uri
     * @param uri
     */
    public String getLayers(String uri);

    public String getLegendGraphicUri(String uri, String imageFormat);

    public String getLegendGraphicUri(String uri, String layer, String imageFormat);

    public String getLegendGraphicUri(String uri, String layer, String imageFormat, String envParams);

    public int getMaxNameLength();

    /**
     * Construct a metadata uri.  For use with ncwms/thredds
     * @param uri
     * @param layer
     * @return
     */
    public String getMetadataUri(String uri, String layer);

    public String getNcWMSTimeStringsUri(String uri, String layer, String startDate, String endDate);

    public int getNcwms();

    public String getParameterValue(String parameter, String uri);

    /**
     * Get the list of supported versions as a human readable string (for
     * debugging) - noone outside this class should need to get r/w access
     * to the list of versions since they can just test for == UNSUPPORTED
     * @return
     */
    public String getSupportedVersions();

    /**
     * Construct and return a URI for a 2x2 px test image
     * from a GetMap uri.  This can be used to test individual
     * wms layers are at least returning images when they are
     * added to the map by the user - we don't do this for
     * our own layers as we assume that they are working.
     * @param uri
     * @return
     */
    public String getTestImageUri(String uri);

    public int getThredds();

    public String getTimestepsUri(String uri, String layer, Date date);

    public String getTooltip(String name, String description);

    public int getUnsupported();

    public String getVersionValue(String uri);

    public int getWms100();

    public int getWms110();

    public int getWms111();

    public int getWms130();
    
    public int getGeojson();

    public int getWkt();

    /**
     * Convert a string WMS version eg (1.3.0) to its integer
     * representation within the portal eg WMS_1_3_0
     * @param requestedType
     * @return Integer constant representing string representation
     * of WMS version.  Returns UNSUPPORTED if nothing matches
     */
    public int internalVersion(String requestedType);

    /**
     * Application specific url tweaking - works for geoserver and
     * ncwms at the moment
     *
     * Autodetect and tweak uri if we detect the presence of geoserver
     * or ncwms
     * @param uri original uri
     * @return mangled uri or null if geoserver or ncwms are not found
     */
    public String mangleUriApplication(String uri);

    /**
     * Attempt to automatically construct a get capabilities uri for
     * the passed in WMS version
     * @param uri
     * @return
     */
    public String mangleUriGetCapabilitiesAutoDiscover(String uri, int version);

    public boolean needsChomp(String originalName);

    /**
     * Return either "", "&" or "?" as a conjunction to be
     * used when generating urls based on the last character
     * in the passed in uri according to the following rules:
     *
     * 1) 	uri ends with '?' or '&' - uri is ready to use,  return ""
     * 2) 	uri doesn't end with '?' or '&' and contains and '?'
     * somewhere within the string - return '&'
     * 3)	uri doesn't end with '?' or '&' and doesn't contain '&'
     * anywhere within the string - return '?'
     * @param uri
     * @return
     */
    public String queryConjunction(String uri);

    public String stripParameter(String parameter, String uri);

    public String stripUriBbox(String uri);

    public String stripUriHeight(String uri);

    public String stripUriRequest(String uri);

    public String stripUriService(String uri);

    public String stripUriVersion(String uri);

    public String stripUriWidth(String uri);

    /**
     * only NCWMS and THREDDS support animation
     * @param type
     * @return
     */
    public boolean supportsAnimation(int type);

    /**
     * only NCWMS and THREDDS support metadata
     * @param type
     * @return
     */
   public boolean supportsMetadata(int type);

    /**
     * Check whether the passed in type is compatible with any
     * WMS version.
     *
     * NcWMS and THREDDS are considered to be WMS compatible
     * @param type
     * @return
     */
    public boolean supportsWms(int type);

    public String coerceLegendUri(MapLayer mapLayer);

    public String getWmsVersion(MapLayer mapLayer);

    public List<Double> getBBox(String uri);

    public List<Double> getBBoxIndex(String uri);
}
