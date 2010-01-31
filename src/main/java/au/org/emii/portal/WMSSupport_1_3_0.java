package au.org.emii.portal;

import net.opengis.wms.LayerDocument.Layer;
import net.opengis.wms.StyleDocument.Style;


/**
 * Support for auto-discovery of WMS 1.3.0 capabilities - uses XMLBEANS
 * @author geoff
 *
 */
public class WMSSupport_1_3_0 extends WMSSupportXmlbeans {

	@Override
	protected void layerSettings(MapLayer mapLayer, Layer layer) {
		mapLayer.setType(LayerUtilities.WMS_1_3_0);
		mapLayer.setQueryable(layer.getQueryable());
	}

	@Override
	protected void styleSettings(WMSStyle style, Style serverStyle) {}
	
	

}
