package au.org.emii.portal.wms;

import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.factory.DiscoveryProcessorFactory;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.UriResolver;
import org.apache.log4j.Logger;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.util.GeoJSONUtilities;
import java.util.Hashtable;
import java.util.List;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

public class RemoteMapImpl implements RemoteMap {

    protected Logger logger = Logger.getLogger(this.getClass());
    /**
     * Language pack - spring injected
     */
    protected LanguagePack languagePack = null;
    protected UriResolver uriResolver = null;
    protected LayerUtilities layerUtilities = null;
    private HttpConnection httpConnection = null;
    private GeoJSONUtilities geoJSONUtilities = null;

    public GeoJSONUtilities getGeoJSONUtilities() {
        return geoJSONUtilities;
    }

    @Autowired
    public void setGeoJSONUtilities(GeoJSONUtilities geoJSONUtilities) {
        this.geoJSONUtilities = geoJSONUtilities;
    }

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    @Autowired
    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    public MapLayer createWKTLayer(String wkt, String label) {
        MapLayer wktLayer = new MapLayer();

        wktLayer.setPolygonLayer(true);

        logger.debug("adding WKT feature layer " + label);
        wktLayer.setName(label);
        wktLayer.setLayer(label);
        wktLayer.setId(label);

        wktLayer.setEnvColour("red");

        int r = 255;
        int g = 0;
        int b = 0;

        wktLayer.setBlueVal(b);
        wktLayer.setGreenVal(g);
        wktLayer.setRedVal(r);

        wktLayer.setType(layerUtilities.WKT);
        wktLayer.setWKT(wkt);

        return wktLayer;

    }

    public MapLayer createGeoJSONLayer(String label, String uri, boolean points_type, int colour) {
        return createGeoJSONLayer(label, uri, points_type, null, colour);
    }

    public MapLayer createGeoJSONLayer(String label, String uri, boolean points_type, Hashtable properties, int colour) {
        MapLayer geoJSON = new MapLayer();
        geoJSON.setPolygonLayer(true);

        // just check if properties is null,
        // if so, just create an empty object
        // and this won't throw any errors.
        // probably ugly, but should work
        if (properties == null) {
            properties = new Hashtable();
        }

        geoJSON.setName(label);

        if (uri.indexOf("?") == -1) {
            geoJSON.setUri(uri);
        } else {
            geoJSON.setUri(uri.substring(0, uri.lastIndexOf("?")));
        }

        geoJSON.setId(uri);
        geoJSON.setLayer(label);

        logger.debug(uri);
        //get colour as label hash
        //TODO: when SPECIES use hash of ID instead of name
        //int hash = Math.abs(label.hashCode());
        int hash = colour;
        int r = (hash >> 16) & 0x000000ff;
        int g = (hash >> 8) & 0x000000ff;
        int b = (hash) & 0x000000ff;

        geoJSON.setSizeUncertain(false);
        String rgbColour = "rgb(" + String.valueOf(r) + "," + String.valueOf(g) + "," + String.valueOf(b) + ")";
        if (properties.containsKey("blue")) {
            int blue = ((Integer) properties.get("blue")).intValue();
            geoJSON.setBlueVal(blue);
        } else {
            geoJSON.setBlueVal(b);
        }
        if (properties.containsKey("green")) {
            int green = ((Integer) properties.get("green")).intValue();
            geoJSON.setGreenVal(green);
        } else {
            geoJSON.setGreenVal(g);
        }
        if (properties.containsKey("red")) {
            int red = ((Integer) properties.get("red")).intValue();
            geoJSON.setRedVal(red);
        } else {
            geoJSON.setRedVal(r);
        }
        if (properties.containsKey("size")) {
            int size = ((Integer) properties.get("size")).intValue();
            geoJSON.setSizeVal(size);
        } else {
            geoJSON.setSizeVal(3);
        }
        if (properties.containsKey("envColour")) {
            String envColour = (String) properties.get("envColour");
            geoJSON.setEnvColour(envColour);
        } else {
            geoJSON.setEnvColour(rgbColour);
        }


        geoJSON.setType(layerUtilities.GEOJSON);
        System.out.println("getting json .... " + uri);

        geoJSON.setGeoJSON(geoJSONUtilities.getJson(uri));

        System.out.println("got json");

        if (geoJSON.getMapLayerMetadata() == null) {
            geoJSON.setMapLayerMetadata(new MapLayerMetadata());
        }

        if (points_type) {
            geoJSON.setGeometryType(geoJSONUtilities.POINT);    //for clustering only
            geoJSON.setQueryable(true);
            geoJSON.setDynamicStyle(true);
        } else {
            //Parsing is taking too long...
            //lets parse the json to find out what type of feature it is
            //JSONObject jo = JSONObject.fromObject(geoJSON.getGeoJSON());
            //int geomTypeCheck = geoJSONUtilities.getFirstFeatureType(jo);
            String typeStart = "\"type\":\"";
            String typeEnd = "\"";
            int start = geoJSON.getGeoJSON().indexOf(typeStart) + typeStart.length();
            int end = geoJSON.getGeoJSON().indexOf('\"', start);
            int geomTypeCheck = geoJSONUtilities.type(geoJSON.getGeoJSON().substring(start, end));

            geoJSON.getMapLayerMetadata().setMoreInfo("");

            //skip checking since initial geojson may be empty (restricted by view extent)
            if (geomTypeCheck >= 0) {
                geoJSON.setGeometryType(geomTypeCheck);
                geoJSON.setQueryable(true);
                geoJSON.setDynamicStyle(true);
            } else {
                geoJSON = null;
            }
        }
        return geoJSON;
    }

    public String getJson(String uri) {
        return geoJSONUtilities.getJson(uri);
    }

    @Override
    public MapLayer createGeoJSONLayerWithGeoJSON(String label, String uri, String geojson) {
        MapLayer geoJSON = new MapLayer();

        geoJSON.setName(label);

        geoJSON.setId(uri);
        geoJSON.setLayer(label);

        if (uri.indexOf("?") == -1) {
            geoJSON.setUri(uri);
        } else {
            geoJSON.setUri(uri.substring(0, uri.lastIndexOf("?")));
        }

        logger.debug(uri);

        //get a random colour
        //Random rand = new java.util.Random();
        //int r = rand.nextInt(255);
        //int g = rand.nextInt(255);
        //int b = rand.nextInt(255);

        //get colour as label hash
        //TODO: when SPECIES use hash of ID instead of name
        int hash = Math.abs(label.hashCode());
        int r = (hash >> 16) & 0x000000ff;
        int g = (hash >> 8) & 0x000000ff;
        int b = (hash) & 0x000000ff;

        geoJSON.setBlueVal(b);
        geoJSON.setGreenVal(g);
        geoJSON.setRedVal(r);

        geoJSON.setSizeVal(4); //TODO: default point size
        geoJSON.setSizeUncertain(false);

        //Color c =new Color(r,g,b);
        //String hexColour = Integer.toHexString( c.getRGB() & 0x00ffffff );

        String rgbColour = "rgb(" + String.valueOf(r) + "," + String.valueOf(g) + "," + String.valueOf(b) + ")";

        geoJSON.setEnvColour(rgbColour);

        geoJSON.setType(layerUtilities.GEOJSON);
        System.out.println("getting json .... " + uri);


        //if (geomTypeCheck >= 0) {
        geoJSON.setGeometryType(geoJSONUtilities.POINT);
        geoJSON.setQueryable(true);
        geoJSON.setDynamicStyle(true);
        //} else {
        //  geoJSON = null;
        //}

        return geoJSON;
    }

    public MapLayer createKMLLayer(String label, String name, String uri) {
        MapLayer kml = new MapLayer();
        kml.setPolygonLayer(true);

        kml.setName(label);

        if (uri.indexOf("?") == -1) {
            kml.setUri(uri);
        } else {
            kml.setUri(uri.substring(0, uri.lastIndexOf("?")));
        }

        kml.setId(uri);
        kml.setLayer(label);

        logger.debug(uri);
        kml.setType(layerUtilities.KML);
        System.out.println("getting json .... " + uri);

        if (kml.getMapLayerMetadata() == null) {
            kml.setMapLayerMetadata(new MapLayerMetadata());
        }

        return kml;
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

        /* attempt to retrieve bounding box */
        List<Double> bbox = layerUtilities.getBBox(uri);
        if (bbox != null) {
            MapLayerMetadata md = new MapLayerMetadata();
            md.setBbox(bbox);
            mapLayer.setMapLayerMetadata(md);
        }

        /* we don't want our user to have to type loads
         * when adding a new layer so we just assume/generate
         * values for the id and description
         */

        mapLayer.setId(uri + label.replaceAll("\\s+", ""));
        mapLayer.setDescription(label);
        mapLayer.setDisplayable(true);

        // wms version
        String version = layerUtilities.getVersionValue(uri);
        mapLayer.setType(layerUtilities.internalVersion(version));

        // Request a 1px test image from the layer
        //if (imageTester.testLayer(mapLayer)) {
        //    testedMapLayer = mapLayer;
        //}
        //return testedMapLayer;
        return mapLayer;
    }

    @Override
    public MapLayer createAndTestWMSLayer(String label, String uri, float opacity, boolean queryable) {
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

        mapLayer.setId(uri + label.replaceAll("\\s+", ""));
        mapLayer.setDescription(label);
        mapLayer.setDisplayable(true);
        mapLayer.setQueryable(queryable);



        // wms version
        String version = layerUtilities.getVersionValue(uri);
        mapLayer.setType(layerUtilities.internalVersion(version));

        // Request a 1px test image from the layer
        //if (imageTester.testLayer(mapLayer)) {
        //    testedMapLayer = mapLayer;
        //}
        //return testedMapLayer;
        return mapLayer;
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

    public MapLayer createLocalLayer(int type, String label) {
        MapLayer layer = new MapLayer();

        layer.setName(label);
        layer.setLayer(label);
        layer.setId(label);

        layer.setEnvColour("red");

        int r = 255;
        int g = 0;
        int b = 0;

        layer.setBlueVal(b);
        layer.setGreenVal(g);
        layer.setRedVal(r);

        layer.setType(type);
        layer.setSubType(type);

        layer.setDisplayable(true);

        return layer;

    }
    
    
    
    
    
    
    /**
     * When using autodiscovery, will be populated with the uri last accessed
     */
    protected String lastUriAttempted = null;
    /**
     * When using autodiscovery, will be populated with the last WMS version
     * attempted
     */
    protected int lastWMSVersionAttempted = LayerUtilities.UNSUPPORTED;

    private DiscoveryProcessor discoveryProcessor = null;
    private static final int[] autoDiscoveryVersions = {
        LayerUtilities.WMS_1_3_0,
        LayerUtilities.WMS_1_1_1,
        LayerUtilities.WMS_1_1_0,
        LayerUtilities.WMS_1_0_0
    };
    private DiscoveryProcessorFactory discoveryProcessorFactory = null;

    public DiscoveryProcessorFactory getDiscoveryProcessorFactory() {
        return discoveryProcessorFactory;
    }

    @Required
    public void setDiscoveryProcessorFactory(DiscoveryProcessorFactory discoveryProcessorFactory) {
        this.discoveryProcessorFactory = discoveryProcessorFactory;
    }
    
    @Override
    public MapLayer autoDiscover(String name, float opacity, String uri, String version) {
        // Use the URI as an ID - the user can only add each unique URI once
        return autoDiscover(uri, name, opacity, uri, version);
    }

    /**
     * Autodiscover a wms servers layers
     *
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
            
            if (discoveryProcessorFactory == null) {
                System.out.println("oh noes: discoveryProcessorFactory IS null");
            } else {
                System.out.println("discoveryProcessorFactory is fine.");
            }
            
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

            /*
             * final chance - if last char of original uri ends with '/', then
             * strip this character and try again...
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
     *
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

            if (discoveryProcessor == null) {
                logger.warn(String.format(
                        "No discovery processor found for supported type '%s' "
                        + "(config) '%s' (internal) for discovery id '%s'",
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
}
