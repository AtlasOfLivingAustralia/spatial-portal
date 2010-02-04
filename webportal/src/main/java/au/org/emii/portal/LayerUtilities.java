package au.org.emii.portal;

import au.org.emii.portal.config.Config;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;

import org.apache.log4j.Logger;

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
 *      "AUTO"  NOTE: Auto discover WMS server - only has meaning during discovery process
 * @author geoff
 *
 */
public class LayerUtilities {

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
         * 	var wms100 = ${session.attributes.portalSession.layerUtilities.wms100};
	 * 	var wms110 = ${session.attributes.portalSession.layerUtilities.wms110};
	 *	var wms111 = ${session.attributes.portalSession.layerUtilities.wms111};
         *      var wms130 = ${session.attributes.portalSession.layerUtilities.wms130};
	 *	var ncwms = ${session.attributes.portalSession.layerUtilities.ncwms};
	 *	var thredds = ${session.attributes.portalSession.layerUtilities.thredds};
	 *	var georss = ${session.attributes.portalSession.layerUtilities.georss};
	 *	var kml = ${session.attributes.portalSession.layerUtilities.kml};
	 *	var unsupported =${session.attributes.portalSession.layerUtilities.unsupported};
         * </script>
         */
	public final static int UNSUPPORTED             = -1;
	public final static int WMS_1_0_0 		= 0;
	public final static int WMS_1_1_0 		= 1;
	public final static int WMS_1_1_1 		= 2;
	public final static int WMS_1_3_0 		= 3;
	public final static int NCWMS 			= 4;
	public final static int THREDDS 		= 5;
	public final static int GEORSS 			= 6;
	public final static int KML			= 7;

	/* Width and heights are numbers in the config file - no 
	 * need to parse them to int thought because they're going
	 * to be used in a string straight away
	 */
	public final static String ANIMATION_WIDTH = Config.getValue("animation_width");
	public final static String ANIMATION_HEIGHT = Config.getValue("animation_height");

	
	private final static String GEOSERVER_REGEXP = "[Gg][Ee][Oo][Ss][Ee][Rr][Vv][Ee][Rr]";
	private final static String NCWMS_REGEXP = "[Nn][Cc][Ww][Mm][Ss]";
	private final static String IMAGE_FORMAT_REGEXP = "[Ff][Oo][Rr][Mm][Aa][Tt]";
	private final static String LAYERS_REGEXP = "[Ll][Aa][Yy][Ee][Rr][Ss]";
	private final static String LAYER_REGEXP = "[Ll][Aa][Yy][Ee][Rr]";
	private final static String VERSION_REGEXP = "[Vv][Ee][Rr][Ss][Ii][Oo][Nn]";
	
	private ArrayList<String> versions = null;

        /**
         * Keyword to use in the config file for auto discovered wms servers
         */
        public final static String AUTO_DISCOVERY_TYPE="AUTO";
	
	public LayerUtilities() {
		versions = new ArrayList<String>();
		versions.add(WMS_1_0_0, "1.0.0");
		versions.add(WMS_1_1_0, "1.1.0");
		versions.add(WMS_1_1_1, "1.1.1");
		versions.add(WMS_1_3_0, "1.3.0");
		
		versions.add(NCWMS, "NCWMS");
		versions.add(THREDDS, "THREDDS");
		versions.add(GEORSS, "GEORSS");
		versions.add(KML, "KML");
	}
	
	/**
	 * Check whether the passed in type is compatible with any
	 * WMS version.
	 * 
	 * NcWMS and THREDDS are considered to be WMS compatible
	 * @param type
	 * @return
	 */
	public static boolean supportsWms(int type) {
		return (
			(type == LayerUtilities.WMS_1_0_0) ||
			(type == LayerUtilities.WMS_1_1_0) ||
			(type == LayerUtilities.WMS_1_1_1) ||
			(type == LayerUtilities.WMS_1_3_0) ||
			(type == LayerUtilities.NCWMS) ||
			(type == LayerUtilities.THREDDS));
	}
	
	/**
	 * only NCWMS and THREDDS support metadata
	 * @param type
	 * @return
	 */
	public static boolean supportsMetadata(int type) {
		return (
			(type == LayerUtilities.NCWMS) ||
			(type == LayerUtilities.THREDDS)
		);
	}
	
	/**
	 * only NCWMS and THREDDS support animation
	 * @param type
	 * @return
	 */
	public static boolean supportsAnimation(int type) {
		return (
			(type == LayerUtilities.NCWMS) ||
			(type == LayerUtilities.THREDDS)
		);
	}
	
	/**
	 * Convert a string WMS version eg (1.3.0) to its integer
	 * representation within the portal eg WMS_1_3_0
	 * @param requestedType
	 * @return Integer constant representing string representation
	 * of WMS version.  Returns UNSUPPORTED if nothing matches
	 */
	public int internalVersion(String requestedType) {
		int version = UNSUPPORTED;
		if (requestedType != null) {
			// strip WMS- or WMS-LAYER- from version number
			String realVersion = 
				requestedType.replaceAll(
						"[Ww][Mm][Ss]-([Ll][Aa][Yy][Ee][Rr]-)?", ""
				);
			
			// Get the version (integer constant) - handily
			// returns -1 (unsupported) if no match
			version = versions.indexOf(realVersion);
		}
		return version;
	}
	
	public static String getNcWMSTimeStringsUri(String uri, String layer, String startDate, String endDate) {
		return 
			uri +
			queryConjunction(uri) +
			"item=animationTimesteps" +
			"&layerName=" + layer + 
			"&start=" + startDate +
			"&end=" + endDate +
			"&request=GetMetadata";
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
	public String externalVersion(int version) {
		String externalVersion = null;
		if (version != UNSUPPORTED) {
			if (version == NCWMS|| version == THREDDS) {
				// force ncwms to be v 1.3.0
				externalVersion = versions.get(WMS_1_3_0);
			}
			else {
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
				"$1"
		);
	}

	
	public static String getVersionValue(String uri) {
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
	public static String getTestImageUri(String uri) {
		
		// step 1 - strip request param, bbox, width and height
		String testUri = 
			stripUriRequest(stripUriBbox(stripUriWidth(stripUriHeight(uri))));
		
		
		/* LAYERS, CRS/SRS, VERSION and FORMAT should already
		 * be set
		 */
		testUri += 
			queryConjunction(uri) +
			"REQUEST=GetMap" + 
			"&BBOX=1,1,2,2" +
			"&WIDTH=2" + 
			"&HEIGHT=2";
		
		return testUri;
	}
	
	
	/**
	 * Application specific url tweaking - works for geoserver and 
	 * ncwms at the moment
	 * 
	 * Autodetect and tweak uri if we detect the presence of geoserver
	 * or ncwms
	 * @param uri original uri
	 * @return mangled uri or null if geoserver or ncwms are not found
	 */
	public String mangleUriApplication(String uri) {
		String mangled;
		String matchGeoserver = ".*?" + GEOSERVER_REGEXP + ".*?"; 
		String matchNcwms = ".*?" + NCWMS_REGEXP + ".*?";
		if ((uri.matches(matchGeoserver))||(uri.matches(matchNcwms))) {
			if (uri.matches(matchGeoserver)) { 
				// geoserver detected
				mangled = removeAfterSlash(uri, GEOSERVER_REGEXP);
			}
			else {
				mangled = removeAfterSlash(uri, NCWMS_REGEXP);
			}
			mangled += "/wms";
		}
		else {
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
	public String mangleUriGetCapabilitiesAutoDiscover(String uri, int version) {
		LayerUtilities wmsUtilities = new LayerUtilities();
		
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
			"SERVICE=WMS&" +
			"REQUEST=GetCapabilities&" +
			"VERSION=" + wmsUtilities.externalVersion(version); 
		
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
	public static String queryConjunction(String uri) {
		String conjunction = "";
		char last = uri.charAt(uri.length() - 1);
		if ((last != '?') && (last != '&')) { 
			if (uri.contains("?")) {
				conjunction += "&";
			}
			else {
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
	public String fixVersion(String uri, String version) {
		String fixedUri = stripUriVersion(uri);
		fixedUri += queryConjunction(fixedUri);
		fixedUri += "&VERSION=" + version;
		return fixedUri;
	}
	
	public static String stripParameter(String parameter, String uri) {
		return uri.replaceAll(parameter + "=([^&]?)*&?", ""); 
	}
	
	public static String stripUriVersion(String uri) {
		return stripParameter("[Vv][Ee][Rr][Ss][Ii][Oo][Nn]", uri);
	}
	
	public static String stripUriService(String uri) {
		return stripParameter("[Ss][Ee][Rr][Vv][Ii][Cc][Ee]", uri);
	}

	public static String stripUriRequest(String uri) {
		return stripParameter("[Rr][Ee][Qq][Uu][Ee][Ss][Tt]", uri); 
	}
	
	public static String stripUriBbox(String uri) {
		return stripParameter("[Bb][Bb][Oo][Xx]", uri);
	}

	public static String stripUriWidth(String uri) {
		return stripParameter("[Ww][Ii][Dd][Tt][Hh]", uri);
	}

	public static String stripUriHeight(String uri) {
		return stripParameter("[Hh][Ee][Ii][Gg][Hh][Tt]", uri);
	}
	
	public static int getMaxNameLength() {
		int maxLength;
		try{
			maxLength = Integer.parseInt(Config.getValue("layer_name_max_length"));
		}
		catch (NumberFormatException e) {
			maxLength = 20;
			Logger logger = Logger.getLogger(LayerUtilities.class.getName());
			logger.error(
				"unable to parse an integer from layer_name_max_length key " +
				"in config file - your configuration is broken.  Will attempt " +
				"to continue using a sensible default value of: " + maxLength
			);	
		}
		return maxLength;
	}
	
	public static boolean needsChomp(String originalName) {
		boolean needsChomp;
		if (originalName.length() > getMaxNameLength()) {
			needsChomp = true;
		}
		else {
			needsChomp = false;
		}
		return needsChomp;
	}
	
	public static String getTooltip(String name, String description) {
		String tooltip;		
		tooltip = description;
		
		if (needsChomp(name)) {
			// show full name: description as tooltip for names we had to 
			// chomp
			if (! name.equals(description)) {
				tooltip = "("+name+")" + "  " + description; 
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
	public static String chompLayerName(String layerName) {
		//int length = layerName.length();
		
		layerName = layerName.replace( '_',' ' );
		
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
			int first_after = bi.following(getMaxNameLength()-10);	
			layerName = layerName.substring(0, first_after);
			
			// forced limiting
			if (needsChomp(layerName)) {				
				layerName = 
					layerName.substring(0, getMaxNameLength() -1);		
			}
			layerName = layerName + "..." ;	
		}	
		
		return layerName;
	}
	
	/**
	 * Parse the image format from a uri
	 * @param uri 
	 */
	public static String getImageFormat(String uri) {
		return getParameterValue(IMAGE_FORMAT_REGEXP, uri);
	}

	/**
	 * Parse the requested layers from a uri
	 * @param uri
	 */
	public static String getLayers(String uri) {
		return getParameterValue(LAYERS_REGEXP, uri);
	}

	public static String getLayer(String uri) {
		return getParameterValue(LAYER_REGEXP, uri);
	}
	
	public static String getParameterValue(String parameter, String uri) {
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
				value=URLDecoder.decode(value, "UTF-8");
			} 
			catch (UnsupportedEncodingException e) {}
		}
		return value;
	}

	public static String getLegendGraphicUri(String uri, String imageFormat) {
		// kill request=getmap
		String legendUri = stripParameter("[Rr][Ee][Qq][Uu][Ee][Ss][Tt]", uri);

		legendUri += 
				queryConjunction(uri) + 
				"LEGEND_OPTIONS=forceLabels:on" +
				"&REQUEST=GetLegendGraphic" +	
				"&FORMAT=" + imageFormat;
		
		// layer parameter must always be set - guess it from LAYERS
		// if it's currently empty
		if (getLayer(uri) == null) {
			legendUri += "&LAYER=" + getLayers(uri);
		}
		
		return legendUri;
	}
	
	public static String getLegendGraphicUri(String uri, String layer, String imageFormat) {
		return getLegendGraphicUri(
				uri + queryConjunction(uri) + "LAYER=" + layer, 
				imageFormat
		);
	}
	
	/**
	 * Construct a metadata uri.  For use with ncwms/thredds
	 * @param uri
	 * @param layer
	 * @return
	 */
	public static String getMetadataUri(String uri, String layer) {
		return 	uri +
			queryConjunction(uri) +
			"item=layerDetails" + 
			"&layerName=" + layer +
			"&request=GetMetadata";
	}
	
	public static String getTimestepsUri(String uri, String layer, Date date) {
		DateFormat df = Validate.getIsoDateFormatter();
		return 
			uri +
			queryConjunction(uri) +
			"item=timesteps" + 
			"&layerName=" + layer + 
			"&request=GetMetadata" + 
			"&day=" + df.format(date);
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
	private static String getAnimationBaseUri(MapLayer mapLayer) {
		
		String baseUri = 
			getFQUri(mapLayer.getUri()) + 
			queryConjunction(mapLayer.getUri()) +
			"TRANSPARENT=true" + 
//			"&ELEVATION=" + mapLayer.getAnimationParameters().getElevation() +
			"&STYLES=" + mapLayer.getSelectedStyleName() +
			"&CRS=EPSG%3A4326" +
//			"&COLORSCALERANGE=9.405405%2C29.66159" +
//			"&NUMCOLORBANDS=254" +
//			"&LOGSCALE=false" +
			"&SERVICE=WMS" +
			"&VERSION=" + mapLayer.getWmsVersion() +
			"&EXCEPTIONS=XML" +
			"&FORMAT=image/gif";
		
		return baseUri;	
	}
	
	/**
	 * Get the fully qualified URI (if it isn't already...)
	 * - this is to add a hostname to indirect cache requests
	 * which normally store the URI as /RemoteRequest?url=foo
	 */
	public static String getFQUri(String uri) {
		String fqUri;
		if (! uri.startsWith("http://")) {
			fqUri = "http://" + Config.getHostname();
			
			// be tolerant of missing/extra slashes in the uri
			if (! uri.startsWith("/")) {
				fqUri += "/";
			}
			fqUri += uri;
		}
		else {
			fqUri = uri;
		}
		return fqUri;
	}

        public static String getAnimationUriJS(MapLayer mapLayer) {
                return StringEscapeUtils.escapeJavaScript(getAnimationUri(mapLayer));
        }

	public static String getAnimationUri(MapLayer mapLayer) {
		/* safe to assume by now that we have accumulated enough
		 * parameters to need & to join our query...
		 */
		return 
			getAnimationBaseUri(mapLayer) + 
			"&TIME=" + mapLayer.getAnimationSelection().getSelectedTimeString() +
			"&LAYERS=" + mapLayer.getLayer() + 
			"&REQUEST=GetMap" +
			"&WIDTH=" + ANIMATION_WIDTH + 
			"&HEIGHT=" + ANIMATION_HEIGHT + 
			"&BBOX=" + mapLayer.getMapLayerMetadata().getBboxString();
	} 

        public static String getAnimationFeatureInfoUriJS(MapLayer mapLayer) {
                return StringEscapeUtils.escapeJavaScript(getAnimationFeatureInfoUri(mapLayer));
        }

	public static String getAnimationFeatureInfoUri(MapLayer mapLayer) {
		return 
			getAnimationBaseUri(mapLayer) +
			"&TIME=" +
				mapLayer.getAnimationSelection().getAdjustedStartDate() + "/" +
				mapLayer.getAnimationSelection().getAdjustedEndDate() + 
			"&REQUEST=GetFeatureInfo" +
			"&QUERY_LAYERS=" + mapLayer.getLayer();
	}

        public static String getAnimationTimeSeriesPlotUriJS(MapLayer mapLayer) {
                return StringEscapeUtils.escapeJavaScript(getAnimationTimeSeriesPlotUri(mapLayer));
        }

        public static String getAnimationTimeSeriesPlotUri(MapLayer mapLayer) {
		return
			getAnimationFeatureInfoUri(mapLayer) +
			"&INFO_FORMAT=image/png";
	}

	/** indirect caching requested - base url is to be requested
	* through our RemoteRequest servlet so squid can cache it
	 * @throws UnsupportedEncodingException 
	*/
	public static String getIndirectCacheUrl(String baseUri) {
		String cacheUri = 
			Config.getValue("cache_url") +
			"?" + 
			Config.getValue("cache_parameter") + 
			"=";
		try {
			cacheUri += URLEncoder.encode(baseUri, "utf-8");
		} 
		catch (UnsupportedEncodingException e) {
			// should never happen unless your jdk is FUBAR
			Logger.getLogger(LayerUtilities.class).error(
				"missing URL encoder! " + e.getMessage()
			);
			
			// will have to include the naked baseuri and hope for the best
			cacheUri += baseUri;
		}
		return cacheUri;
		 
	}

	public int getWms100() {
		return WMS_1_0_0;
	}

	public int getWms110() {
		return WMS_1_1_0;
	}

	public int getWms111() {
		return WMS_1_1_1;
	}

	public int getWms130() {
		return WMS_1_3_0;
	}

	public int getNcwms() {
		return NCWMS;
	}

	public int getThredds() {
		return THREDDS;
	}

	public int getGeorss() {
		return GEORSS;
	}

	public int getKml() {
		return KML;
	}

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
        public String getAutoDiscoveryType() {
            return AUTO_DISCOVERY_TYPE;
        }
        /**
         * Get the list of supported versions as a human readable string (for
         * debugging) - noone outside this class should need to get r/w access
         * to the list of versions since they can just test for == UNSUPPORTED
         * @return
         */
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
}
