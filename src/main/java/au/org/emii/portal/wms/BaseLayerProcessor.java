package au.org.emii.portal.wms;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.config.xmlbeans.BaseLayer;

public interface BaseLayerProcessor {
	public MapLayer baseLayer(BaseLayer baseLayer);
}
