package au.org.emii.portal.wms;

import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.menu.MapLayer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * WMS 1.1.0 is very similar to 1.1.1 so all we need to do is define a few
 * XPATHS and we're in business.
 */
public class WMSSupport_1_1_0 extends WMSSupportNonXmlbeans {
	
	public WMSSupport_1_1_0() {
		serviceTitleXpath = "/WMT_MS_Capabilities/Service/Title/text()";
		baseUriXpath = 
			"/WMT_MS_Capabilities/Capability/Request/GetMap/DCPType/HTTP/Get/OnlineResource/@href";
	
		imageFormatXpath = 
			"/WMT_MS_Capabilities/Capability/Request/GetMap/Format[1]/text()";
		rootLayerXpath = 
			"/WMT_MS_Capabilities/Capability/Layer";
		serviceAbstractXpath = "/WMT/MS_Capabilities/Service/Abstract/text()";
		layerLabelXpath = "child::Title/text()";
		layerLayersXpath = "child::Name/text()";
		childLayersXpath = "child::Layer";
		queryableXpath = "@queryable";
		layerDescriptionXpath = "child::Abstract/text()";
		
		styleXpath = "Style";
		styleNameXpath = "Name/text()";
		styleTitleXpath = "Title/text()";
		styleDescriptionXpath = "Abstract/text()";
		styleImageFormat = "LegendURL/format[1]/text()";
		styleImageUri = "LegendURL/OnlineResource/@href";
	}
	
	@Override
	protected void layerSettings(MapLayer mapLayer, Node layer) {
		mapLayer.setType(LayerUtilitiesImpl.WMS_1_1_0);
	}

	@Override
	protected void styleSettings(WMSStyle style, NodeList serverStyles) {}

}