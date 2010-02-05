package au.org.emii.portal;

import org.apache.log4j.Logger;

import au.org.emii.portal.config.AbstractService;
import au.org.emii.portal.config.BaseLayer;
import au.org.emii.portal.config.Service;

public class GenericServiceAndBaseLayerSupport implements ServiceProcessor, BaseLayerProcessor {
	protected Logger logger = Logger.getLogger(this.getClass());
	
	public MapLayer service(Service service) {
		MapLayer mapLayer = null;
		LayerUtilities wmsUtilities = new LayerUtilities();
		int type = wmsUtilities.internalVersion(service.getType());
		if (type != LayerUtilities.UNSUPPORTED) {
			mapLayer = new MapLayer();	
			copyInto(mapLayer, service);
			mapLayer.setType(type);
			mapLayer.setLayer(service.getLayers());
			mapLayer.setOpacity(service.getOpacity());
			mapLayer.setImageFormat(service.getImageFormat());
			mapLayer.setDisplayable(true);
			mapLayer.setQueryable(service.getQueryable());
			// WMS legend - WMS only
			if (LayerUtilities.supportsWms(type)) {
				/* we only support a single style for services but
				 * there may still be a legend so we use string 
				 * manipulation on the uri to ask for a legend
				 * graphic
				 */
				mapLayer.coerceLegendUri();
			}
		}
		else {
			logger.warn(
				"unupported type " + service.getType() + 
				" requested for service at " + service.getUri()
			);
		}
		
		return mapLayer;
	}

	// for KML layers
	public MapLayer createMapLayer(String description, String name, String type, String uri) {
		
		String id = uri + name; //should be unique enough. name should be unique anyway
		Service  service =  Service.Factory.newInstance();
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
	// for WMS layers
	public MapLayer createMapLayer(String description, String name, String type, String uri, String layer, String format, String sld) {		
				
		MapLayer mapLayer = null;		
		mapLayer = createMapLayer(description, name, type, uri);	
		
		// add the extras
		mapLayer.setLayer(layer);
		mapLayer.setImageFormat(format);
		if (! Validate.empty(sld)) {
			mapLayer.setSld(sld);
		}
		mapLayer.setImageFormat(format);
		
		return mapLayer;
	}
		

	public MapLayer baseLayer(BaseLayer baseLayer) {
		MapLayer mapLayer = null;
		LayerUtilities wmsUtilities = new LayerUtilities();
		int type = wmsUtilities.internalVersion(baseLayer.getType());
		
		if (type != LayerUtilities.UNSUPPORTED) {
			mapLayer = new MapLayer();
			copyInto(mapLayer, baseLayer);
			mapLayer.setType(type);
			mapLayer.setLayer(baseLayer.getLayers());
			// baselayers are always Opaque
			mapLayer.setOpacity(1.0f);
			mapLayer.setImageFormat(baseLayer.getImageFormat());
			mapLayer.setDisplayable(true);
			mapLayer.setBaseLayer(true);
			mapLayer.setQueryable(false);
			// get the WMS version for WMS layers only
		}
		else {
			logger.warn(
					"unupported type " + baseLayer.getType()  + 
					" requested for service at " + baseLayer.getUri()
			);
		}
		return mapLayer;
	}
	
	private void copyInto(MapLayer mapLayer, AbstractService service) {	
		mapLayer.setId(service.getId());
		mapLayer.setName(service.getName());
		mapLayer.setDescription(service.getDescription());
		mapLayer.setUri(service.getUri());
		mapLayer.setDisplayable(true);
		if (service.getCache()) {
			mapLayer.setUri(LayerUtilities.getIndirectCacheUrl(service.getUri()));
			logger.debug("+ indirect caching " + service.getId());
		}
		else {
			mapLayer.setUri(service.getUri());	
		}

	}
}
