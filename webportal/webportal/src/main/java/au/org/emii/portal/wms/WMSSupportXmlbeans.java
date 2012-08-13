package au.org.emii.portal.wms;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.Sequence;
import au.org.emii.portal.util.Validate;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.opengis.wms.LayerDocument;
import net.opengis.wms.WMSCapabilitiesDocument;
import net.opengis.wms.LayerDocument.Layer;
import net.opengis.wms.StyleDocument.Style;
import net.opengis.wms.WMSCapabilitiesDocument.WMSCapabilities;
import org.apache.xmlbeans.XmlException;
import au.org.emii.portal.config.xmlbeans.Discovery;
import au.org.emii.portal.menu.MapLayerMetadata;
import net.opengis.wms.AuthorityURLDocument.AuthorityURL;
import net.opengis.wms.MetadataURLDocument.MetadataURL;

public abstract class WMSSupportXmlbeans extends WMSSupport {



    @Override
	public MapLayer discover(Discovery discovery, boolean displayAllChildren, boolean queryableDisabled, boolean quiet) {
		this.quiet = quiet;
		this.queryableDisabled = queryableDisabled;
		this.cache = discovery.getCache();
		MapLayer mapLayer = null;
		
		// setup defaults provided in discovery object
		opacity = discovery.getOpacity();
		String discoveryUri = uriResolver.resolve(discovery);
		id = discovery.getId();
		description = discovery.getDescription();
		discoveryName = discovery.getName();
		
		/*
		 * find out the base URI for http requests - its a nested attribute with
		 * the xpath: /WMS_Capabilities/Capability/Request/GetCapabilities/DCPType/HTTP/Get/OnlineResource[@href]
		 * 
		 * Since the xpath is so long with scope for various parts being null, we need to
		 * protect against NPEs here... 
		 */
		try {
			
			// configure an InputStream with a timeout
			InputStream is = httpConnection.configureURLConnection(discoveryUri).getInputStream();
			
			WMSCapabilitiesDocument doc = WMSCapabilitiesDocument.Factory.parse(is);
			WMSCapabilities type = doc.getWMSCapabilities();
			
			// the first listed DCP HTTP server will be used as a base uri
			baseUri = 
				type.getCapability().
					getRequest().
						getGetCapabilities()
							.getDCPTypeList()
								.get(0)
									.getHTTP()
										.getGet()
											.getOnlineResource()
												.getHref();
		
			// the first listed imageformat will be used for rendering
			imageFormat = 
				type.getCapability()
					.getRequest()
						.getGetMap()
							.getFormatList()
								.get(0);
			
			/* no name for the menu specified - try and find one from the
			 * capabilities document
			 */
			if (discoveryName == null) {
				discoveryName =
					type.getService()
						.getTitle();
				
				if (Validate.empty(discoveryName)) {
					/* discovery name is still empty (server misconfiguration)
					 * use our default name from config file
					 */
					discoveryName = languagePack.getLang("wms_unamed_server");
				}
			}
			
			if (baseUri != null) {
				if (cache) {
					logger.debug("+ indirect caching " + discovery.getId());
					baseUri = layerUtilities.getIndirectCacheUrl(baseUri);
				}
				
				// root layer for this server
				mapLayer = new MapLayer();				
				LayerDocument.Layer layer = type.getCapability().getLayer();
				processLayer(mapLayer, layer, new Sequence(), displayAllChildren);
			}
			else {
				lastErrorMessage = "Unable to parse a GetCapabilities document from '" +
                                        discoveryUri + "'(missing OnlineResource href attribute) ";
                        }
		
		}
		catch (NullPointerException e) {
			parseError = true;
			lastErrorMessage = 
                                "Unable to parse a GetCapabilities document at '" +
                                discoveryUri + "' (a required field was missing)";
		}
		catch (XmlException e) {
			parseError = true;
			lastErrorMessage = 
					"Unable to parse the GetCapabilities document " +
                                        "from '" + discoveryUri + "' " + 
					"(parse error - did you set the " +
					"correct WMS version if manually specified?). " +
                                        "Root cause: " + e.getMessage();
		}
		catch (IOException e) {
                        // for 404 errors, the message will be the requested url
			readError = true;
			lastErrorMessage = "IO error connecting to server at '" +
                                discoveryUri + "'.  Root cause: " + e.getMessage();
		}

                return checkErrorFlags(discovery, mapLayer);
        }

	
	private void processLayer(MapLayer parent, Layer layer, Sequence sequence, boolean displayAllChildren) {
		MapLayer mapLayer;
		String label;
        
		if (sequence.getValue() == 0) {
			mapLayer = parent;
			
			/* use the name supplied in the config file for the discovery
			 * root label
			 */
			label = discoveryName;
		}
		else {
			mapLayer = new MapLayer();
			parent.addChild(mapLayer);
			label = layer.getTitle();
		}
		
	
		
		mapLayer.setName(label);
		mapLayer.setId(id + WMSSupport.sequenceFragment(sequence));

		
		// if the abstract has been set for this layer, use it instead of the
		// description from the config file
		mapLayer.setDescription(
				(layer.getAbstract() != null) ? 
						layer.getAbstract(): description
		);
		
		// displayable layers have a 'name' element that is not null
		String name = layer.getName();
		
		
		
		if (name != null) {
			// add this layer and do not process its children
			mapLayer.setLayer(name);
			logger.debug("...adding layer: " + mapLayer.getName());
			mapLayer.setUri(baseUri);
			mapLayer.setImageFormat(imageFormat);
			mapLayer.setDisplayable(true);
			mapLayer.setOpacity(opacity);
			mapLayer.setQueryable(
					(! queryableDisabled) &&  
					layer.isSetQueryable()
			);

                        //attempt to add boundingbox, TODO: test & fix
                        try {
                            List<Double> bbox = new ArrayList<Double>();
                            bbox.add(new Double(layer.getBoundingBoxArray(0).getMinx()));
                            bbox.add(new Double(layer.getBoundingBoxArray(0).getMiny()));
                            bbox.add(new Double(layer.getBoundingBoxArray(0).getMaxx()));
                            bbox.add(new Double(layer.getBoundingBoxArray(0).getMaxy()));
                            MapLayerMetadata md = new MapLayerMetadata();
                            md.setBbox(bbox);
                            mapLayer.setMapLayerMetadata(md);
                        } catch (Exception e) {
                        }

			/* sometimes people don't setup servers right and leave off the label
			 * names - if this is the case, substitute the layer name instead
			 */
			if (Validate.empty(mapLayer.getName())) {
				mapLayer.setName(mapLayer.getLayer());
			}

			layerSettings(mapLayer, layer);
			mapLayer.addStyles(
					processStyles(layer.getStyleList())
			);
			
			// generate a URI to get the legend for the default style
			mapLayer.setDefaultStyleLegendUri(layerUtilities.coerceLegendUri(mapLayer));
		}		
		else {
			mapLayer.setDisplayable(false);
		}
        
        boolean hasMetadata = false; 
        List<MetadataURL> metadataURLList = layer.getMetadataURLList();
        if (metadataURLList != null) {
            if (metadataURLList.size() > 0) {
                MapLayerMetadata mlmd = mapLayer.getMapLayerMetadata();
                if (mlmd == null) {
                    mlmd = new MapLayerMetadata();
                }
                mlmd.setMoreInfo(metadataURLList.get(0).getOnlineResource().getHref());
                mapLayer.setMapLayerMetadata(mlmd);
                
                hasMetadata = true;
            }
        }
        if (!hasMetadata) {
            List<AuthorityURL> authorityURLList = layer.getAuthorityURLList();
            if (authorityURLList != null) {
                if (authorityURLList.size() > 0) {
                    MapLayerMetadata mlmd = mapLayer.getMapLayerMetadata();
                    if (mlmd == null) {
                        mlmd = new MapLayerMetadata();
                    }
                    mlmd.setMoreInfo(authorityURLList.get(0).getOnlineResource().getHref());
                    mapLayer.setMapLayerMetadata(mlmd);
                }
            }
        }

		/* process all children if this layer is not displayable
		 * (no name field) or we are forced to display all children
		 */
		if (name == null || displayAllChildren) {
			// process any children...
			List<Layer> layerChildren = layer.getLayerList();
			Iterator<Layer> it = layerChildren.iterator();
			while (it.hasNext()) {
				sequence.increment();
				processLayer(mapLayer, it.next(), sequence, displayAllChildren);
			}
		}
	}
	
	private List<WMSStyle> processStyles(List<Style> serverStyles) {
		List<WMSStyle> styles = new ArrayList<WMSStyle>();
		for (Style serverStyle : serverStyles) {

			// adjust the uri for caching if necessary
			String uri = serverStyle.getLegendURLList().get(0).getOnlineResource().getHref();
			if (cache) {
				uri = 
					layerUtilities.getFQUri(
							layerUtilities.getIndirectCacheUrl(uri)
					);
			}
			
			WMSStyle style = new WMSStyle();
			style.setName(serverStyle.getName());
			style.setTitle(serverStyle.getTitle());
			style.setDescription(serverStyle.getAbstract());
			// use the first listed online resource as the HREF
			style.setLegendUri(uri);
			style.setLegendFormat(serverStyle.getLegendURLList().get(0).getFormat());
			
			styleSettings(style, serverStyle);
			
			styles.add(style);
		}
		return styles;
	}
	
	/**
	 * Implementation specific layer settings (will be executed once per layer)
	 * @param mapLayer
	 * @param layer
	 */
	protected abstract void layerSettings(MapLayer mapLayer, Layer layer);

	/**
	 * Implementation specific layer settings (will be executed once per layer)
	 * @param style
	 * @param serverStyle
	 */
	protected abstract void styleSettings(WMSStyle style, Style serverStyle);


    
}
