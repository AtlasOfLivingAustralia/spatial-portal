/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.wms;

import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.menu.MapLayer;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * WMS 1.0.0 is very similar to WMS 1.1.1 - all we have to do is parse the
 * image format differently and we're up and running
 * @author ajay
 *
 */
public class WMSSupport_1_0_0 extends WMSSupportNonXmlbeans {

        public WMSSupport_1_0_0() {
                serviceTitleXpath = "/WMT_MS_Capabilities/Service/Title/text()"; 
                baseUriXpath = 
                        "/WMT_MS_Capabilities/Capability/Request/Map/DCPType/HTTP/Get/@onlineResource";

                imageFormatXpath = 
                        "/WMT_MS_Capabilities/Capability/Request/Map/Format";
                rootLayerXpath = 
                        "/WMT_MS_Capabilities/Capability/Layer";
                serviceAbstractXpath = "/WMT_MS_Capabilities/Capability/Service/Abstract/text()";
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
        
        protected boolean testIfNodeExists(Document document, String path) throws XPathExpressionException {
                XPathExpression expr = null;
                Node node = null;

                boolean exists = false;
                expr = xpath.compile(path);
                node = (Node) expr.evaluate(document, XPathConstants.NODE);
                if (node != null) {
                        exists = true;
                }       
                return exists;
        }
        
        /**
         * Image format for WMS 1.0.0 is handled badly by the xml as the real MIME type
         * is never included.  To workaround this, we will attempt to match an XPATH and
         * if it exists, we will use the following mapping:
         * 
         *      o       ./PNG   --> image/png
         *      o       ./JPEG  --> image/jpeg
         *      o       ./GIF   --> image/gif
         * 
         * Whichever format is matched first will be selected as the image type.  If
         * nothing matches, log a warning and disable the service
         */
        protected void imageFormat(Document document) throws XPathExpressionException {
                if (testIfNodeExists(document, imageFormatXpath + "/PNG")) {
                        imageFormat = "image/png";
                }
                else if (testIfNodeExists(document, imageFormatXpath + "/JPEG")) {
                        imageFormat = "image/jpeg";
                }
                else if (testIfNodeExists(document, imageFormatXpath + "/GIF")) {
                        imageFormat = "image/gif";
                }
                else {
                        logger.debug("No supported image formats found - mark service as broken");
                        broken = true;
                }
                
        }

        @Override
        protected void layerSettings(MapLayer mapLayer, Node layer) {
                mapLayer.setType(LayerUtilitiesImpl.WMS_1_0_0);
        }

        @Override
        protected void styleSettings(WMSStyle style, NodeList serverStyles) {}
}