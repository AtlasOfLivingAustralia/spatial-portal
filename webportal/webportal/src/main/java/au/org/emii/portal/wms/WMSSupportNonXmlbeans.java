/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.wms;

import au.org.emii.portal.net.HttpConnectionImpl;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.Sequence;
import au.org.emii.portal.util.Validate;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.menu.MapLayerMetadata;

/**
 *
 * @author ajay
 */
public abstract class WMSSupportNonXmlbeans extends WMSSupport {
	protected String baseUriXpath = null;

	/**
	 * Set the default image format for this server - we will use the first
	 * format element they provide which is *1* and not *0* in the list of
	 * formats
	 */
	protected String serviceTitleXpath = null;
	protected String imageFormatXpath =  null;
	protected String rootLayerXpath =  null;
	protected String serviceAbstractXpath =  null;
	
	
	protected String layerLabelXpath = null;
	protected String layerLayersXpath = null;
	protected String childLayersXpath = null;
	protected String queryableXpath = null;
	protected String layerDescriptionXpath = null;
	
	protected String styleXpath = null;
	protected String styleNameXpath = null;
	protected String styleTitleXpath = null;
	protected String styleDescriptionXpath = null;
	protected String styleImageFormat = null;
	protected String styleImageUri = null;
		
	protected XPath xpath = XPathFactory.newInstance().newXPath();
	
	protected Document parseXml(String discoveryUri) {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		
		/*
		 * Everything on the internet says set the next variable to true but if I 
		 * do this, I can't select the xpath variable I want (which is in another
		 * namespace) - setting namespace aware to false fixes things...
		 */
		domFactory.setNamespaceAware(false);
		
		
		/*
		 * DISABLE DTD Validation
		 * ======================
		 * By default, the DTD is processed when we parse the XML and this has the effect
		 * of setting queryable="0" as a defalt attribute on all layers.  Popular implementations
		 * (mapserver) just leave off the queryable attribute on layers which ARE queryable, thus
		 * marking them as non-queryable.
		 * 
		 * The solution is to totally disable DTD validation, - here's where I found out how to
		 * do it:
		 * 
		 * http://stackoverflow.com/questions/582352/how-can-i-ignore-dtd-validation-but-keep-the-doctype-when-writing-an-xml-file
		 */
		//careful... this next line will sneakily re-enable namespaces and break everything  
		//domFactory.setAttribute("http://xml.org/sax/features/namespaces", true);
		domFactory.setAttribute("http://xml.org/sax/features/validation", false);
		domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		domFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		DocumentBuilder documentBuilder = null;
		Document document = null;
		try {
			documentBuilder = domFactory.newDocumentBuilder();
			documentBuilder.setEntityResolver(null);
			
			// configure an InputStream with a timeout
			InputStream is = httpConnection.configureURLConnection(discoveryUri).getInputStream();
			
			document = documentBuilder.parse(is);
		
		} 
		catch (SAXException e) {
			parseError = true;
			lastErrorMessage = "Unable to parse a GetCapabilities document from '" + discoveryUri +
                                "' (parser error - is XML well formed?)";
		}
		catch (ParserConfigurationException e) {
			parseError = true;
			lastErrorMessage = "Unable to parse a GetCapabilities document from '" +
                                discoveryUri + "' (parser configuration error)";
		}
		catch (IOException e) {
			readError = true;
                        // for 404 errors, the message will be the requested url
			lastErrorMessage = "IO error connecting to server at '" + discoveryUri + "'.  Root cause: "
                                + e.getMessage();
		}
		
		// discard broken documents
		if (readError || parseError) {
			document = null;
		}
		return document;
	}

        @Override
	public MapLayer discover(Discovery discovery, boolean displayAllChildren, boolean queryableDisabled, boolean quiet) {
		this.quiet = quiet;
		this.queryableDisabled = queryableDisabled;
		this.cache = discovery.getCache();
		MapLayer mapLayer = null;
		String discoveryUri = uriResolver.resolve(discovery);
		try {
			opacity = discovery.getOpacity();
			id = discovery.getId();
			description = discovery.getDescription();
			discoveryName = discovery.getName();
			Document document = parseXml(discoveryUri);
			if (document != null) {
				baseUri(document);
				
				if (cache) {
					logger.debug("+ indirect caching " + discovery.getId());
					baseUri = layerUtilities.getIndirectCacheUrl(baseUri);
				}
				
				imageFormat(document);
				descriptionAndTitle(document);
				
			
				
				mapLayer = processLayers(document, displayAllChildren, queryableDisabled);				
				logger.debug("discovered services from " + id + " will read data from " + baseUri);
			}
		}

		catch (XPathExpressionException e) {
			lastErrorMessage = "Unable to parse a GetCapabilities document at '" + discoveryUri + 
                                "' (a required field was missing).  Root cause: " + e.getMessage();
			parseError = true;
		}
               
		return checkErrorFlags(discovery, mapLayer);
	}
	

	
	
	/**
	 * Configure the base uri
	 * @throws XPathExpressionException 
	 */
	private void baseUri(Document document) throws XPathExpressionException {
		XPathExpression expr = null;
		
		// base uri
		expr = xpath.compile(baseUriXpath);
		baseUri =  expr.evaluate(document);
		if (baseUri == null) {
			logger.debug("null base uri detected - service is broken!");
                        lastErrorMessage = "Unable to parse a valid OnlineResource";
			parseError = true;
		}
	
	}
	
	/**
	 * Configure the image format
	 * @throws XPathExpressionException 
	 */
	private void imageFormat(Document document) throws XPathExpressionException {
		XPathExpression expr = null;
		
		expr = xpath.compile(imageFormatXpath);
		imageFormat = expr.evaluate(document);
	}

	/**
	 * Configure the description
	 * @throws XPathExpressionException 
	 */
	private void descriptionAndTitle(Document document) throws XPathExpressionException {
		XPathExpression expr = null;
		
		// description
		expr = xpath.compile(serviceAbstractXpath);
		String abstractText = expr.evaluate(document);
		description = 
			((abstractText != null) && (! abstractText.equals(""))) 
					? abstractText: description;
		
		// title - only if user didn't specify one
		if (discoveryName == null) {
			expr = xpath.compile(serviceTitleXpath);
			discoveryName = expr.evaluate(document);
			
			if (Validate.empty(discoveryName)) {
				/* discovery name is still empty (server misconfiguration)
				 * use our default name from config file
				 */
				discoveryName = languagePack.getLang("wms_unamed_server");
			}
		}
		
	}
	
	private MapLayer processLayers(	Document document, 
										boolean displayAllChildren, 
										boolean queryableDisabled) throws XPathExpressionException {
		XPathExpression expr = null;
		MapLayer mapLayer = null;

		// get the root layer - there should be one layer containing everything else...
		expr = xpath.compile(rootLayerXpath);
		Node rootLayer = (Node) expr.evaluate(document, XPathConstants.NODE);

		if (rootLayer == null) {
			lastErrorMessage = 
				"Unable to parse a GetCapabilities document (missing root Layer element)";
			parseError = true;
		}
		else if (baseUri == null) {
			lastErrorMessage =
				"Unable to parse a GetCapabilities document (missing OnlineResource href attribute)"; 
			parseError = true;
		}
		else {
			mapLayer = new MapLayer();		
			processLayer(
					mapLayer, 
					rootLayer, 
					new Sequence(), 
					displayAllChildren
			);
		}	
		return mapLayer;
	}
	
	private void descendAllChildren(Node layer, 
					Sequence sequence, 
					boolean displayAllChildren, 
					MapLayer mapLayer,
					boolean disableQueryable)  throws XPathExpressionException{
		// process child layers
		XPathExpression expr = xpath.compile(childLayersXpath);
		NodeList children = (NodeList) expr.evaluate(layer, XPathConstants.NODESET);
		for (int i = 0 ; i < children.getLength() ; i++) {
			sequence.increment();
			processLayer(
					mapLayer, 
					children.item(i), 
					sequence, 
					displayAllChildren
			);
		}
	}
	
	/**
	 * Recursively process layers from the capabilities document.
	 * 
	 * 1	Layers that have a both name and title elements are displayable on the map
	 * 2	Layers that have only a title are to be used as grouping titles for their child
	 * 		layers.
	 * 3	Displayable layers (1) can also have displayable child layers - these are
	 * 		often different zoom levels of the same data and can be displayed by just
	 *		requesting the parent.  As an optimisation we do not recurse these layers
	 * 
	 * 
	 * @param parent
	 * @param layer
	 * @param root
	 * @throws XPathExpressionException
	 */
	private void processLayer(	MapLayer parent, 
									Node layer, 
									Sequence sequence, 
									boolean displayAllChildren) throws XPathExpressionException {
		XPathExpression expr = null;
		MapLayer mapLayer = null;
		String label;
		if (sequence.getValue() == 0) {
			mapLayer = parent;
			
			/*
			 * Use the name from the config file for the root discovery name
			 */
			label = discoveryName;
		}
		else {
			mapLayer = new MapLayer();
			parent.addChild(mapLayer);
			
			expr = xpath.compile(layerLabelXpath); 
			label = expr.evaluate(layer);
		}	
		
		// label
		mapLayer.setName(label);


		/* layers ('Name' in xml document, can be null or an empty string 
		 * indicating that this layer doesn't get displayed)
		 */
		expr = xpath.compile(layerLayersXpath);
		mapLayer.setLayer(expr.evaluate(layer));

                // attempt to find LatLonBoundingBox under layer Node
                Node n = layer.getFirstChild();
                while (n != layer.getLastChild()) {
                    if (n.getNodeName().equalsIgnoreCase("LatLonBoundingBox")) {
                        ArrayList<Double> bbox = new ArrayList<Double>(4);
                        bbox.add(Double.parseDouble(n.getAttributes().getNamedItem("minx").getNodeValue()));
                        bbox.add(Double.parseDouble(n.getAttributes().getNamedItem("miny").getNodeValue()));
                        bbox.add(Double.parseDouble(n.getAttributes().getNamedItem("maxx").getNodeValue()));
                        bbox.add(Double.parseDouble(n.getAttributes().getNamedItem("maxy").getNodeValue()));      

                        //add bbox to layer
                        MapLayerMetadata md = new MapLayerMetadata();
                        md.setBbox(bbox);
                        mapLayer.setMapLayerMetadata(md);
                        break;
                    }
                    n = n.getNextSibling();
                }

		/* sometimes people don't setup servers right and leave off the label
		 * names - if this is the case, substitute the layer name instead
		 */
		if (Validate.empty(mapLayer.getName())) {
			mapLayer.setName(mapLayer.getLayer());
		}
		
		// idBase - always from config file
		mapLayer.setId(id + WMSSupport.sequenceFragment(sequence));
		
		/* description - use the layer defined abstract if there is one, 
		 * otherwise use the description from the config file
		 */
		expr = xpath.compile(layerDescriptionXpath);
		String layerAbstract = expr.evaluate(layer);
		if (layerAbstract != null && ! layerAbstract.equals("")) {
			mapLayer.setDescription(layerAbstract);	
		}
		else {
			mapLayer.setDescription(description);	
		}
		
		if (	(mapLayer.getLayer() == null) || 
				(mapLayer.getLayer().equals(""))) {
			/* the name element was not set in the capabilities document
			 * so we should not display this layer, instead we will use it
			 * for grouping only and investigate it's children for 
			 * displayable layers
			 */
			mapLayer.setDisplayable(false);			
			descendAllChildren(
					layer, 
					sequence, 
					displayAllChildren, 
					mapLayer,
					queryableDisabled
			);

		}
		else { 
			/* we have reached a map layer we can display.  Setup a MapLayer
			 * instance to write javascript for openlayers from and DO NOT
			 * parse any children of this layer.  This avoids having long 
			 * lists of layers for the same data at different zoom levels, etc
			 */
			mapLayer.setDisplayable(true);
			mapLayer.setOpacity(opacity);
			mapLayer.setImageFormat(imageFormat);
			mapLayer.setUri(baseUri);

			// check whether this layer is getfeatureinfo queryable
			expr = xpath.compile(queryableXpath);
			mapLayer.setQueryable(expr.evaluate(layer).equals("true") && (!queryableDisabled));

			
			/* select the styles as a nodeset and then process 
			 * them
			 */
			expr = xpath.compile(styleXpath);
			NodeList serverStyles = (NodeList) expr.evaluate(layer, XPathConstants.NODESET);
			mapLayer.addStyles(processStyles(serverStyles));
			
			// generate a URI to get the legend for the default style
			mapLayer.setDefaultStyleLegendUri(layerUtilities.coerceLegendUri(mapLayer));
			
			/* apply any implementation specific layer tweaks (eg 
			 * concrete classes set the implementation here
			 */
			layerSettings(mapLayer, layer);
			
			if (displayAllChildren) {
				descendAllChildren(
						layer, 
						sequence, 
						displayAllChildren, 
						mapLayer,
						queryableDisabled
				);
			}
			
		}			
		
		logger.debug(
				"...adding layer: " + mapLayer.getName() + 
				" displayable==" + mapLayer.isDisplayable() 
		);
	}
	
	private List<WMSStyle> processStyles(NodeList serverStyles) throws XPathExpressionException {
		List<WMSStyle> styles = new ArrayList<WMSStyle>();
		XPathExpression expr = null;

		for (int i = 0 ; i < serverStyles.getLength() ; i++) {
			Node serverStyle = serverStyles.item(i);
			WMSStyle style = new WMSStyle();
	
			// name
		
			expr = xpath.compile(styleNameXpath);
			style.setName(expr.evaluate(serverStyle));
		
			// title
			expr = xpath.compile(styleTitleXpath);
			style.setTitle(expr.evaluate(serverStyle));
			
			// description
			expr = xpath.compile(styleDescriptionXpath);
			style.setDescription(expr.evaluate(serverStyle));
			
			// legend URI + indirect cache
			expr = xpath.compile(styleImageUri);
			String uri = expr.evaluate(serverStyle);
			if (cache) {
				uri = layerUtilities.getFQUri(
						layerUtilities.getIndirectCacheUrl(uri)
				);
			}
			style.setLegendUri(uri);
			
			// legend format
			expr = xpath.compile(styleImageFormat);
			style.setLegendFormat(expr.evaluate(serverStyle));
			
			// implementation settings
			styleSettings(style, serverStyles);
		}
		
		return styles;
	}

	/**
	 * Implementation specific style settings (will be executed once per style per layer)
	 * @param style
	 * @param serverStyles
	 */
	protected abstract void styleSettings(WMSStyle style, NodeList serverStyles);

	/**
	 * Implementation specific layer settings (will be executed once per layer)
	 * @param mapLayer
	 * @param layer
	 */
	protected abstract void layerSettings(MapLayer mapLayer, Node layer);
}
