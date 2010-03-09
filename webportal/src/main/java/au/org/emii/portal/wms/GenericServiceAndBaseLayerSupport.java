package au.org.emii.portal.wms;

import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.util.UriResolver;
import org.apache.log4j.Logger;

import au.org.emii.portal.config.xmlbeans.AbstractService;
import au.org.emii.portal.config.xmlbeans.BaseLayer;
import au.org.emii.portal.config.xmlbeans.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

public class GenericServiceAndBaseLayerSupport implements ServiceProcessor, BaseLayerProcessor {

    protected Logger logger = Logger.getLogger(this.getClass());
    private LayerUtilities layerUtilities = null;
    /**
     * Uri Resolver - spring autowired
     */
    @Autowired
    private UriResolver uriResolver = null;

    @Override
    public MapLayer service(Service service) {
        MapLayer mapLayer = null;
        int type = layerUtilities.internalVersion(service.getType());
        if (type != LayerUtilities.UNSUPPORTED) {

            mapLayer = new MapLayer();
            if (copyInto(mapLayer, service)) {
                mapLayer.setType(type);
                mapLayer.setCql(service.getCql());
                mapLayer.setLayer(service.getLayers());
                mapLayer.setOpacity(service.getOpacity());
                mapLayer.setImageFormat(service.getImageFormat());
                mapLayer.setDisplayable(true);
                mapLayer.setQueryable(service.getQueryable());
                // WMS legend - WMS only
                if (layerUtilities.supportsWms(type)) {
                    /* we only support a single style for services but
                     * there may still be a legend so we use string
                     * manipulation on the uri to ask for a legend
                     * graphic
                     */
                    mapLayer.setDefaultStyleLegendUri(layerUtilities.coerceLegendUri(mapLayer));
                }
            } else {
                // bad layer - get rid
                mapLayer = null;
            }

        } else {
            logger.warn(
                    "unupported type " + service.getType()
                    + " requested for service at " + uriResolver.resolve(service));
        }

        return mapLayer;
    }

    // for KML layers
    public MapLayer createMapLayer(String description, String name, String type, String uri) {

        String id = uri + name; //should be unique enough. name should be unique anyway
        Service service = Service.Factory.newInstance();
        service.setDescription(description);
        service.setDisabled(false);
        service.setId(id);
        service.setName(name);
        service.setOpacity(1.0f);
        service.setQueryable(false);
        service.setType(type);
        service.setUri(uri);

        return service(service);
    }

    public MapLayer createMapLayer(String description, String name, String type, String uri, String layer, String format, String sld, String cql) {

        MapLayer mapLayer = createMapLayer(description, name, type, uri);
        // add the extras
        mapLayer.setLayer(layer);
        mapLayer.setImageFormat(format);

        if (!Validate.empty(cql)) {
            mapLayer.setCql(cql);
        }

        mapLayer.setQueryable(true);

        WMSStyle style = new WMSStyle();
        style.setName(name);
        style.setTitle(name);
        style.setDescription(description);
        style.setLegendFormat(format);

        mapLayer.addStyle(style);
        mapLayer.setDefaultStyleLegendUri(layerUtilities.coerceLegendUri(mapLayer));

        return mapLayer;
    }

    @Override
    public MapLayer baseLayer(BaseLayer baseLayer) {
        MapLayer mapLayer = null;
        int type = layerUtilities.internalVersion(baseLayer.getType());

        if (type != LayerUtilities.UNSUPPORTED) {
            mapLayer = new MapLayer();
            if (copyInto(mapLayer, baseLayer)) {
                mapLayer.setType(type);
                mapLayer.setLayer(baseLayer.getLayers());
                // baselayers are always Opaque
                mapLayer.setOpacity(1.0f);
                mapLayer.setImageFormat(baseLayer.getImageFormat());
                mapLayer.setDisplayable(true);
                mapLayer.setBaseLayer(true);
                mapLayer.setQueryable(false);
                // get the WMS version for WMS layers only

            } else {
                // something went wrong in copyInto() - this layer is bad, get rid
                mapLayer = null;
            }
        } else {
            logger.warn(
                    "unupported type " + baseLayer.getType()
                    + " requested for service at " + uriResolver.resolve(baseLayer));
        }
        return mapLayer;
    }

    private boolean copyInto(MapLayer mapLayer, AbstractService service) {
        boolean ok;
        // resolve the target uri if incase mapping was used
        String targetUri = uriResolver.resolve(service);
        if (targetUri == null) {
            ok = false;
            logger.error(
                String.format(
                    "Unable to resolve uri or uriIdRef to real URI for service or discovey %s " +
                    "uri '%s', uriIdRef '%s'", service.getId(), service.getUri(), service.getUriIdRef()));
        } else {
            ok = true;
            mapLayer.setId(service.getId());
            mapLayer.setName(service.getName());
            mapLayer.setDescription(service.getDescription());
            mapLayer.setDisplayable(true);

            // rewrite the uri to take account of cache reflection if needed
            if (service.getCache()) {
                mapLayer.setUri(layerUtilities.getIndirectCacheUrl(targetUri));
                logger.debug("+ indirect caching " + service.getId());
            } else {
                mapLayer.setUri(targetUri);
            }
        }
        return ok;
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
}
