package au.org.emii.portal.wms;

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

    public MapLayer createGeoJSONLayer(String label, String uri, boolean points_type) {
        return createGeoJSONLayer(label, uri, points_type,null);
    }

    public MapLayer createGeoJSONLayer(String label, String uri, boolean points_type, Hashtable properties) {
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
        int hash = Math.abs(label.hashCode());
        int r = (hash >> 16) % 255;
        int g = (hash >> 8) % 255;
        int b = (hash) % 255;

        geoJSON.setSizeUncertain(false);
        String rgbColour = "rgb(" + String.valueOf(r) + "," + String.valueOf(g) + "," + String.valueOf(b) + ")";
        if (properties.containsKey("blue")) {
            int blue = ((Integer)properties.get("blue")).intValue();
            geoJSON.setBlueVal(blue);
        } else {
            geoJSON.setBlueVal(b);
        }
        if (properties.containsKey("green")) {
            int green = ((Integer)properties.get("green")).intValue();
            geoJSON.setGreenVal(green);
        } else {
            geoJSON.setGreenVal(g);
        }
        if (properties.containsKey("red")) {
            int red = ((Integer)properties.get("red")).intValue();
            geoJSON.setRedVal(red);
        } else {
            geoJSON.setRedVal(r);
        }
        if (properties.containsKey("size")) {
            int size = ((Integer)properties.get("size")).intValue();
            geoJSON.setSizeVal(size);
        } else {
            geoJSON.setSizeVal(3);
        }
        if (properties.containsKey("envColour")) {
            String envColour = (String)properties.get("envColour");
            geoJSON.setEnvColour(envColour);
        } else {
            geoJSON.setEnvColour(rgbColour);
        }
        

        geoJSON.setType(layerUtilities.GEOJSON);
        System.out.println("getting json .... " + uri);
       
        geoJSON.setGeoJSON(geoJSONUtilities.getJson(uri));

        if (geoJSON.getMapLayerMetadata() == null) {
            geoJSON.setMapLayerMetadata(new MapLayerMetadata());
        }

        if(points_type){
            geoJSON.setGeometryType(geoJSONUtilities.POINT);    //for clustering only
            geoJSON.setQueryable(true);
            geoJSON.setDynamicStyle(true);
        }else{
            //lets parse the json to find out what type of feature it is
            JSONObject jo = JSONObject.fromObject(geoJSON.getGeoJSON());
            int geomTypeCheck = geoJSONUtilities.getFirstFeatureType(jo);

            //do this at the add level
            //String taxonconceptid = geoJSONUtilities.getFirstFeatureValue(jo, "ti" /*"taxonconceptid"*/);
            //if (!taxonconceptid.equals("")) {
            //    System.out.println("species: " + "http://bie.ala.org.au/species/" + taxonconceptid);
             //   geoJSON.getMapLayerMetadata().setMoreInfo("http://bie.ala.org.au/species/" + taxonconceptid + "\n" + label);
            //} else {
                System.out.println("not species");
                geoJSON.getMapLayerMetadata().setMoreInfo("");
            //}

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

    public String getJson(String uri){
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
        int r = (hash >> 16) % 255;
        int g = (hash >> 8) % 255;
        int b = (hash) % 255;

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
}
