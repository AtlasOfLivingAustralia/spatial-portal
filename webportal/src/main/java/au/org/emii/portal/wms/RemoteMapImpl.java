package au.org.emii.portal.wms;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.UriResolver;
import org.apache.log4j.Logger;
import au.org.emii.portal.config.xmlbeans.BaseLayer;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.config.xmlbeans.Service;
import au.org.emii.portal.factory.DiscoveryProcessorFactory;
import au.org.emii.portal.lang.LanguagePack;
import org.springframework.beans.factory.annotation.Required;

public class RemoteMapImpl implements RemoteMap {

    protected Logger logger = Logger.getLogger(this.getClass());
    /**
     * Language pack - spring injected
     */

    protected LanguagePack languagePack = null;
    protected UriResolver uriResolver = null;
    protected LayerUtilities layerUtilities = null;
    protected GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport = null;

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

    private DiscoveryProcessorFactory discoveryProcessorFactory = null;


    @Override
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
    @Override
    public MapLayer autoDiscover(String id, String name, float opacity, String uri, String version) {
        MapLayer mapLayer = null;
        Discovery discovery = Discovery.Factory.newInstance();
        discovery.setName(name);
        discovery.setOpacity(opacity);
        discovery.setId(id);
        discovery.setDescription(name);

        if (version.equals(LayerUtilities.AUTO_DISCOVERY_TYPE)) {
            discovery.setUri(uri);
            mapLayer = autoDiscover(discovery, true, true);
        } else {
            discovery.setType("WMS-" + version);
            // fix the version number incase it doesn't match or user
            // forgot it
            discovery.setUri(layerUtilities.fixVersion(uri, version));
            mapLayer = discover(discovery, true, true, true);
        }

        return mapLayer;

    }

    private MapLayer autoDiscover(Discovery discovery,
            boolean descendAllChildren,
            boolean queryableDisabled) {

        MapLayer mapLayer = null;
        boolean finished = false;
        int wmsVersionIndex = 0;
        int internalVersion;

        String originalUri = discovery.getUri();

        while ((!finished) && (wmsVersionIndex < autoDiscoveryVersions.length)) {
            internalVersion = autoDiscoveryVersions[wmsVersionIndex];
            logger.debug(
                    "attempting autodiscovery from " + originalUri
                    + " attempt: " + wmsVersionIndex);
            discoveryProcessor = discoveryProcessorFactory.getDiscoveryProcessorForWMSVersion(
                    internalVersion);

            // mangle the discovery URI for this WMS version...
            lastUriAttempted = layerUtilities.mangleUriGetCapabilitiesAutoDiscover(
                    originalUri,
                    internalVersion);
            lastWMSVersionAttempted = internalVersion;
            discovery.setUri(lastUriAttempted);

            mapLayer = discoveryProcessor.discover(
                    discovery,
                    descendAllChildren,
                    queryableDisabled,
                    true);
            if (mapLayer != null) {
                logger.debug(
                        "successfuly parsed GetCapabilities document from "
                        + lastUriAttempted + ", attempt " + wmsVersionIndex
                        + " using WMS version: " + lastWMSVersionAttempted
                        + " as defined in WMSSupport.java");
                finished = true;
            } else if (discoveryProcessor.isReadError()) {
                logger.debug("read error from:  " + discovery.getUri());
                // There was a read error but don't give up yet - it might only have been
                // caused by a missing file (404, 400) or something
            } else if (discoveryProcessor.isParseError()) {
                logger.debug("parse error from: " + discovery.getUri());
            }

            // try the next version in the autodiscover list
            wmsVersionIndex++;

            /* final chance - if last char of original uri ends with
             * '/', then strip this character and try again...
             */
            if ((wmsVersionIndex == autoDiscoveryVersions.length)
                    && (!finished)) {
                // last chance to get something working - first see if there
                String mangled = layerUtilities.mangleUriApplication(originalUri);
                if (mangled != null) {
                    // retry with guessed url
                    originalUri = mangled;
                    wmsVersionIndex = 0;
                } else if (originalUri.charAt(originalUri.length() - 1) == '/') {
                    // strip off trailing slash if there is one and try again
                    originalUri = originalUri.substring(0, originalUri.length() - 2);
                    wmsVersionIndex = 0;
                }
                // otherwise give up
            }
        }

        return mapLayer;
    }

    /**
     * Discovery of nested services
     * @param discovery
     * @return
     */
    @Override
    public MapLayer discover(Discovery discovery,
            boolean displayAllChildren,
            boolean queryableDisabled,
            boolean quiet) {
        MapLayer mapLayer = null;

        String requestedType = discovery.getType();
        int internalVersion = layerUtilities.internalVersion(requestedType);
        if (internalVersion == LayerUtilities.UNSUPPORTED) {
            logger.warn(
                    "discovery '" + discovery.getId() + "' requested an "
                    + "unsupported type: '" + requestedType + "' - supported types are: "
                    + layerUtilities.getSupportedVersions());
        } else {
            // this version of wms is supported - start autodiscovery...
            lastWMSVersionAttempted = internalVersion;
            discoveryProcessor = discoveryProcessorFactory.getDiscoveryProcessorForWMSVersion(
                    lastWMSVersionAttempted);

            if (discoveryProcessor == null)  {
                logger.warn(String.format(
                        "No discovery processor found for supported type '%s' " +
                        "(config) '%s' (internal) for discovery id '%s'",
                        requestedType, lastWMSVersionAttempted, discovery.getId()));
            } else {

                lastUriAttempted = uriResolver.resolve(discovery);

                mapLayer = discoverMapLayers(
                        discovery,
                        discoveryProcessor,
                        displayAllChildren,
                        queryableDisabled,
                        quiet);
            }
        }

        return mapLayer;
    }

    @Override
    public MapLayer service(Service service) {
        logger.debug("DATASOURCE (SERVICE)+ " + service.getId());
        MapLayer mapLayer = genericServiceAndBaseLayerSupport.service(service);
        return mapLayer;
    }

    @Override
    public MapLayer baseLayer(BaseLayer baseLayer) {
        logger.debug("DATASOURCE (BASELAYER) + " + baseLayer.getId());
        MapLayer mapLayer = genericServiceAndBaseLayerSupport.baseLayer(baseLayer);
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

    @Override
    public String getDiscoveryErrorMessage() {
        return discoveryProcessor.getLastErrorMessage();
    }

    @Override
    public String getDiscoveryErrorMessageSimple() {
        String message;
        if (discoveryProcessor.isReadError()) {
            message = languagePack.getLang("read_error_message");
        } else if (discoveryProcessor.isParseError()) {
            message = languagePack.getLang("get_capabilities_parse_error_message");
        } else {
            message = languagePack.getLang("unknown_error");
        }
        return message;
    }

    @Override
    public String getLastUriAttempted() {
        return lastUriAttempted;
    }

    @Override
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
    @Override
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
        mapLayer.setLayer(layerUtilities.getLayers(uri));
        mapLayer.setOpacity(opacity);
        mapLayer.setImageFormat(layerUtilities.getImageFormat(uri));

        /* we don't want our user to have to type loads
         * when adding a new layer so we just assume/generate
         * values for the id and description
         */
        mapLayer.setId(uri);
        mapLayer.setDescription(label);
        mapLayer.setDisplayable(true);

        // wms version
        String version = layerUtilities.getVersionValue(uri);
        mapLayer.setType(layerUtilities.internalVersion(version));

        // Request a 1px test image from the layer
        if (imageTester.testLayer(mapLayer)) {
            testedMapLayer = mapLayer;
        }
        return testedMapLayer;
    }


    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    @Required
    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public UriResolver getUriResolver() {
        return uriResolver;
    }

    @Required
    public void setUriResolver(UriResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    public LayerUtilities getLayerUtilities() {
        return layerUtilities;
    }

    @Required
    public void setLayerUtilities(LayerUtilities layerUtilities) {
        this.layerUtilities = layerUtilities;
    }

    public GenericServiceAndBaseLayerSupport getGenericServiceAndBaseLayerSupport() {
        return genericServiceAndBaseLayerSupport;
    }

    @Required
    public void setGenericServiceAndBaseLayerSupport(GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport) {
        this.genericServiceAndBaseLayerSupport = genericServiceAndBaseLayerSupport;
    }

    public void setImageTester(ImageTester imageTester) {
        this.imageTester = imageTester;
    }

    public ImageTester getImageTester() {
        return imageTester;
    }

    public DiscoveryProcessorFactory getDiscoveryProcessorFactory() {
        return discoveryProcessorFactory;
    }

    @Required
    public void setDiscoveryProcessorFactory(DiscoveryProcessorFactory discoveryProcessorFactory) {
        this.discoveryProcessorFactory = discoveryProcessorFactory;
    }


}
