/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.menu.MapLayer;

import java.util.List;

/**
 * @author geoff
 */
public interface LayerUtilities {
    /**
     * Keyword to use in the config file for auto discovered wms servers
     */
    public int KML = 4;
    public int GEOJSON = 5;
    public int WKT = 11;
    public int IMAGELAYER = 9;
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
    public int UNSUPPORTED = -1;
    public int WMS_1_0_0 = 0;
    public int WMS_1_1_0 = 1;
    public int WMS_1_1_1 = 2;
    public int WMS_1_3_0 = 3;

    /**
     * Chomp the layer name if we need to
     *
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
     * <p/>
     * NCWMS and THREDDS are special cases and will return "1.3.0"
     *
     * @param version
     * @return
     */
    public String externalVersion(int version);

    /**
     * Strip any version information from the URI and use
     * the passed in version string instead
     *
     * @param uri
     * @param version
     * @return
     */
    public String fixVersion(String uri, String version);

    /**
     * Parse the image format from a uri
     *
     * @param uri
     */
    public String getImageFormat(String uri);

    public int getKml();

    public String getLayer(String uri);

    /**
     * Parse the requested layers from a uri
     *
     * @param uri
     */
    public String getLayers(String uri);

    public String getLegendGraphicUri(String uri, String imageFormat);

    public String getLegendGraphicUri(String uri, String layer, String imageFormat);

    public String getLegendGraphicUri(String uri, String layer, String imageFormat, String envParams);

    public int getMaxNameLength();

    /**
     * Construct a metadata uri.  For use with ncwms/thredds
     *
     * @param uri
     * @param layer
     * @return
     */
    public String getMetadataUri(String uri, String layer);

    public String getParameterValue(String parameter, String uri);

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
    public String getTestImageUri(String uri);

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
     *
     * @param requestedType
     * @return Integer constant representing string representation
     * of WMS version.  Returns UNSUPPORTED if nothing matches
     */
    public int internalVersion(String requestedType);

    /**
     * Application specific url tweaking - works for geoserver and
     * ncwms at the moment
     * <p/>
     * Autodetect and tweak uri if we detect the presence of geoserver
     * or ncwms
     *
     * @param uri original uri
     * @return mangled uri or null if geoserver or ncwms are not found
     */
    public String mangleUriApplication(String uri);

    /**
     * Attempt to automatically construct a get capabilities uri for
     * the passed in WMS version
     *
     * @param uri
     * @return
     */
    public String mangleUriGetCapabilitiesAutoDiscover(String uri, int version);

    public boolean needsChomp(String originalName);

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
    public String queryConjunction(String uri);

    public String stripParameter(String parameter, String uri);

    public String stripUriBbox(String uri);

    public String stripUriHeight(String uri);

    public String stripUriRequest(String uri);

    public String stripUriService(String uri);

    public String stripUriVersion(String uri);

    public String stripUriWidth(String uri);

    /**
     * Check whether the passed in type is compatible with any
     * WMS version.
     * <p/>
     * NcWMS and THREDDS are considered to be WMS compatible
     *
     * @param type
     * @return
     */
    public boolean supportsWms(int type);

    public String getWmsVersion(MapLayer mapLayer);

    public List<Double> getBBox(String uri);

    public List<Double> getBBoxIndex(String uri);
}
