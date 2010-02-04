package au.org.emii.portal;

import au.org.emii.portal.config.Config;
import org.apache.log4j.Logger;
import au.org.emii.portal.config.xmlbeans.BaseLayer;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.config.xmlbeans.Service;

public class RemoteMap {
	protected Logger logger = Logger.getLogger(this.getClass());	
	
	/**
	 * When using autodiscovery, will be populated with the uri
	 * last accessed
	 */
	protected String lastUriAttempted = null;
	
	/**
	 * When using autodiscovery, will be populated with the last
	 * WMS version attempted
	 */
	protected int lastWMSVersionAttempted = LayerUtilities.UNSUPPORTED;
	
	/**
	 * If we are required to test an image is valid, this field will be 
	 * bound to an instance of our image tester so that error messages
	 * can be retrieved if there was an error
	 */
	private ImageTester imageTester = null;
	
	private DiscoveryProcessor discoveryProcessor = null;

	private static final int[] autoDiscoveryVersions = {
		LayerUtilities.WMS_1_3_0,
		LayerUtilities.WMS_1_1_1,
		LayerUtilities.WMS_1_1_0,
		LayerUtilities.WMS_1_0_0
	};

        public MapLayer autoDiscover(String name, float opacity, String uri, String version) {
            // Use the URI as an ID - the user can only add each unique URI once
            return autoDiscover(uri, name, opacity, uri, version);
        }

        /**
         * Autodiscover a wms servers layers
         * @param name
         * @param opacity
         * @param uri
         * @param version
         * @return
         */
	public MapLayer autoDiscover(String id, String name, float opacity, String uri, String version) {
		LayerUtilities wmsUtilities = new LayerUtilities();
		MapLayer mapLayer = null;
		Discovery discovery = Discovery.Factory.newInstance();
		discovery.setName(name);
		discovery.setOpacity(opacity);
		discovery.setId(id);
		discovery.setDescription(name);	

		if (version.equals(LayerUtilities.AUTO_DISCOVERY_TYPE)) {
			discovery.setUri(uri);
			mapLayer = autoDiscover(discovery, true, true);
		}
		else {
			discovery.setType("WMS-" + version);
			// fix the version number incase it doesn't match or user
			// forgot it
			discovery.setUri(wmsUtilities.fixVersion(uri, version));
			mapLayer = discover(discovery, true, true, true);
		}
		
		return mapLayer;
		
	}
	
	private MapLayer autoDiscover(	Discovery discovery, 
									boolean descendAllChildren, 
									boolean queryableDisabled) {
		
		LayerUtilities wmsUtilities = new LayerUtilities();
		MapLayer mapLayer = null;
		boolean finished = false;
		int wmsVersionIndex = 0;
		int internalVersion;
		
		String originalUri = discovery.getUri();
		
		while ((! finished) && (wmsVersionIndex < autoDiscoveryVersions.length)) {
			internalVersion = autoDiscoveryVersions[wmsVersionIndex];
			logger.debug(
					"attempting autodiscovery from " + originalUri + 
					" attempt: " + wmsVersionIndex
			);
			discoveryProcessor = getDiscoveryProcessorForWMSVersion(
					internalVersion
			);
			
			// mangle the discovery URI for this WMS version...
			lastUriAttempted = wmsUtilities.mangleUriGetCapabilitiesAutoDiscover(
					originalUri, 
					internalVersion
			);
			lastWMSVersionAttempted = internalVersion;
			discovery.setUri(lastUriAttempted);
			
			mapLayer = discoveryProcessor.discover(
					discovery, 
					descendAllChildren, 
					queryableDisabled, 
					true
			);
			if (mapLayer != null) {
				logger.debug(
						"successfuly parsed GetCapabilities document from " +
						lastUriAttempted + ", attempt " + wmsVersionIndex + 
						" using WMS version: " + lastWMSVersionAttempted +
						" as defined in WMSSupport.java"
				);
				finished = true;
			}
			else if (discoveryProcessor.isReadError()) {
				logger.debug("read error from:  " + discovery.getUri()); 
				// There was a read error but don't give up yet - it might only have been
                                // caused by a missing file (404, 400) or something
			}
			else if (discoveryProcessor.isParseError()) {
				logger.debug("parse error from: " + discovery.getUri());
			}
			
                        // try the next version in the autodiscover list
			wmsVersionIndex++;

			/* final chance - if last char of original uri ends with
			 * '/', then strip this character and try again... 
			 */
			if (	(wmsVersionIndex == autoDiscoveryVersions.length) &&
					(! finished )) {
				// last chance to get something working - first see if there
				String mangled = wmsUtilities.mangleUriApplication(originalUri);
				if (mangled != null) {
					// retry with guessed url
					originalUri = mangled;
					wmsVersionIndex = 0;	
				}
				else if (originalUri.charAt(originalUri.length() - 1) == '/') {
					// strip off trailing slash if there is one and try again
					originalUri = originalUri.substring(0, originalUri.length() - 2);
					wmsVersionIndex = 0;
				}
				// otherwise give up				
			}
		}
		
		return mapLayer;
	}
	
	private DiscoveryProcessor getDiscoveryProcessorForWMSVersion(int version) {
		DiscoveryProcessor dp = null;
		switch (version) {
		case LayerUtilities.WMS_1_3_0:
			dp = new WMSSupport_1_3_0();
			break;
		case LayerUtilities.WMS_1_1_1:
			dp = new WMSSupport_1_1_1();
			break;
		case LayerUtilities.WMS_1_1_0:
			dp = new WMSSupport_1_1_0();
			break;
		case LayerUtilities.WMS_1_0_0:
			dp = new WMSSupport_1_0_0();
			break;
		case LayerUtilities.NCWMS:
			dp = new NcWMSSupport();
			break;
		case LayerUtilities.THREDDS:
			dp = new ThreddsSupport();
			break;
		default:
			logger.warn("no support for discovery type - " + version + " yet!");
		}
		
		return dp;
	}
	

	
	/**
	 * Discovery of nested services
	 * @param discovery
	 * @return
	 */
	public MapLayer discover(	Discovery discovery, 
								boolean displayAllChildren, 
								boolean queryableDisabled,
								boolean quiet) {
		LayerUtilities wmsUtilities = new LayerUtilities();
		MapLayer mapLayer = null;
		
		String requestedType = discovery.getType();
                int internalVersion = wmsUtilities.internalVersion(requestedType);
                if (internalVersion != LayerUtilities.UNSUPPORTED) {

                    // this version of wms is supported - start autodiscovery...
                    lastWMSVersionAttempted = internalVersion;
                    discoveryProcessor = getDiscoveryProcessorForWMSVersion(
                            lastWMSVersionAttempted
                    );

                    if (discoveryProcessor != null) {

                            lastUriAttempted = discovery.getUri();

                            mapLayer = discoverMapLayers(
                                            discovery,
                                            discoveryProcessor,
                                            displayAllChildren,
                                            queryableDisabled,
                                            quiet
                            );
                    }
                }
                else {
                    logger.warn(
                        "discovery '" + discovery.getId() + "' requested an " +
                        "unsupported type: '" + requestedType + "' - supported types are: " +
                        wmsUtilities.getSupportedVersions()
                    );
                }

		return mapLayer;
	}
	
	public MapLayer service(Service service) {
		logger.debug("DATASOURCE (SERVICE)+ " + service.getId());
		MapLayer mapLayer = new GenericServiceAndBaseLayerSupport().service(service);
		return mapLayer;
	}
	
	public MapLayer baseLayer(BaseLayer baseLayer) {
		logger.debug("DATASOURCE (BASELAYER) + " + baseLayer.getId());
		MapLayer mapLayer = new GenericServiceAndBaseLayerSupport().baseLayer(baseLayer);
		return mapLayer;
	}

	
	protected MapLayer discoverMapLayers(Discovery discovery, DiscoveryProcessor discoveryProcessor, boolean displayAllChildren, boolean queryableDisabled, boolean quiet) {
		logger.debug("DATASOURCE (DISCOVERY) + " + discovery.getId());
		
		MapLayer mapLayer = null;

		String uri = discovery.getUri();
		logger.debug("connecting to " + uri + "... - if process hangs here, you have server or network problems!");
		// setup some defaults...

		mapLayer = discoveryProcessor.discover(discovery, displayAllChildren, queryableDisabled, quiet);

		return mapLayer;
	}


	
	public String getDiscoveryErrorMessage() {
		return discoveryProcessor.getLastErrorMessage();
	}

	public String getDiscoveryErrorMessageSimple() {
		String message;
		if (discoveryProcessor.isReadError()) {
			message = Config.getLang("read_error_message");
		}
		else if (discoveryProcessor.isParseError()) {
			message = Config.getLang("get_capabilities_parse_error_message");
		}
		else {
			message = Config.getLang("unknown_error");
		}
		return message;
	}

	public String getLastUriAttempted() {
		return lastUriAttempted;
	}


	public int getLastWMSVersionAttempted() {
		return lastWMSVersionAttempted;
	}
	
	/**
	 * Create a MapLayer instance and test that an image can be read from 
	 * the URI.
	 * 
	 * Image format, and wms layer name and wms type are automatically obtained from the
	 * URI.
	 * 
	 * If there is no version parameter in the uri, we use a sensible 
	 * default (v1.1.1)
	 * 
	 * @param label Label to use for the menu system
	 * @param uri URI to read the map layer from (a GetMap request)
	 * @param opacity Opacity value for this layer
	 */
	public MapLayer createAndTestWMSLayer(String label, String uri, float opacity) {
		/* it is not necessary to construct and parse a service instance for adding
		 * a WMS layer since we already know its a WMS layer, so all we have to do
		 * is populate a MapLayer instance and ask for it to be activated as a 
		 * user defined item
		 */
		MapLayer testedMapLayer = null;
		MapLayer mapLayer = new MapLayer();
		mapLayer.setName(label);
		
		mapLayer.setUri(uri);
		mapLayer.setLayer(LayerUtilities.getLayers(uri));
		mapLayer.setOpacity(opacity);
		mapLayer.setImageFormat(LayerUtilities.getImageFormat(uri));
		
		/* we don't want our user to have to type loads
		 * when adding a new layer so we just assume/generate
		 * values for the id and description
		 */
		mapLayer.setId(uri);
		mapLayer.setDescription(label);
		mapLayer.setDisplayable(true);
		
		// wms version
		LayerUtilities wmsUtilities = new LayerUtilities();
		String version = LayerUtilities.getVersionValue(uri);
		mapLayer.setType(wmsUtilities.internalVersion(version));
		
		// Request a 1px test image from the layer
		imageTester = new ImageTester();
		if (imageTester.testLayer(mapLayer)) {
			testedMapLayer = mapLayer;
		}
		return testedMapLayer;
	}


	public ImageTester getImageTester() {
		return imageTester;
	}
}