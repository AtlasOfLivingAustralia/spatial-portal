package au.org.emii.portal.util;


import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

public class RemoteMapImpl implements RemoteMap {

    private static final Logger LOGGER = Logger.getLogger(RemoteMapImpl.class);
    /**
     * Language pack - spring injected
     */
    protected LanguagePack languagePack = null;
    protected LayerUtilities layerUtilities = null;

    public MapLayer createWKTLayer(String wkt, String label) {
        MapLayer wktLayer = new MapLayer();

        wktLayer.setPolygonLayer(true);

        LOGGER.debug("adding WKT feature layer " + label);
        wktLayer.setName(label);
        wktLayer.setLayer(label);
        wktLayer.setId(label);

        wktLayer.setEnvColour(StringConstants.RED);

        int colour = Util.nextColour();
        int r = (colour >> 16) & 0x000000ff;
        int g = (colour >> 8) & 0x000000ff;
        int b = (colour) & 0x000000ff;

        wktLayer.setRedVal(r);
        wktLayer.setGreenVal(g);
        wktLayer.setBlueVal(b);

        wktLayer.setType(LayerUtilitiesImpl.WKT);
        wktLayer.setWKT(wkt);

        if (wktLayer.getMapLayerMetadata() == null) {
            wktLayer.setMapLayerMetadata(new MapLayerMetadata());
        }
        wkt = wkt.replace(" (", "(").replace(", ", ",");
        double[][] bbox = SimpleShapeFile.parseWKT(wkt).getBoundingBox();
        List<Double> bb = new ArrayList<Double>();
        bb.add(bbox[0][0]);
        bb.add(bbox[0][1]);
        bb.add(bbox[1][0]);
        bb.add(bbox[1][1]);
        wktLayer.getMapLayerMetadata().setBbox(bb);

        return wktLayer;

    }

    /**
     * Create a MapLayer instance and test that an image can be read from
     * the URI.
     * <p/>
     * Image format, and wms layer name and wms type are automatically obtained from the
     * URI.
     * <p/>
     * If there is no version parameter in the uri, we use a sensible
     * default (v1.1.1)
     *
     * @param label   Label to use for the menu system
     * @param uri     URI to read the map layer from (a GetMap request)
     * @param opacity Opacity value for this layer
     */
    @Override
    public MapLayer createAndTestWMSLayer(String label, String uri, float opacity) {
        /* it is not necessary to construct and parse a service instance for adding
         * a WMS layer since we already know its a WMS layer, so all we have to do
         * is populate a MapLayer instance and ask for it to be activated as a
         * user defined item
         */
        MapLayer mapLayer = new MapLayer();
        mapLayer.setName(label);

        mapLayer.setUri(uri);
        mapLayer.setLayer(layerUtilities.getLayer(uri));
        mapLayer.setOpacity(opacity);
        mapLayer.setImageFormat(layerUtilities.getImageFormat(uri));

        /* attempt to retrieve bounding box */
        List<Double> bbox = layerUtilities.getBBox(uri);
        if (bbox != null) {
            mapLayer.getMapLayerMetadata().setBbox(bbox);
        }

        /* we don't want our user to have to type loads
         * when adding a new layer so we just assume/generate
         * values for the id and description
         */

        mapLayer.setId(uri + label.replaceAll("\\s+", ""));
        mapLayer.setDescription(label);


        // wms version
        String version = layerUtilities.getVersionValue(uri);
        mapLayer.setType(layerUtilities.internalVersion(version));

        return mapLayer;
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    @Required
    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
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

        layer.setEnvColour(StringConstants.RED);

        int colour = Util.nextColour();
        int r = (colour >> 16) & 0x000000ff;
        int g = (colour >> 8) & 0x000000ff;
        int b = (colour) & 0x000000ff;

        layer.setBlueVal(b);
        layer.setGreenVal(g);
        layer.setRedVal(r);

        layer.setType(type);
        layer.setSubType(type);


        return layer;

    }


}
