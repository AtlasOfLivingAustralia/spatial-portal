package org.ala.rest;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.geotools.data.FeatureSource;
import org.geotools.util.logging.Logging;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;

import org.geotools.data.DataStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.vfny.geoserver.util.DataStoreUtils;
/**
 *
 * @author Angus
 */
public class IntersectThread  extends Thread {
	List<Point> points;
	private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");
	private List<FeatureSource> featureSources;
	private List<STRtree> indexes = new ArrayList();
	GazetteerConfig gc = new GazetteerConfig();
	GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
	Catalog catalog = gs.getCatalog();
	ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
	public String[] results;
	private List<String> layers;
	CountDownLatch latch;

	IntersectThread(List<Point> points, List<String> layers, String[] results, CountDownLatch latch)  {
		this.points = points;
		System.out.println("Constructor POINTS: " + points.size());
	//	this.featureSources = featureSources;
		featureSources = new ArrayList();
		this.results = results;
		this.latch = latch;
		this.layers = layers;
		try {
			for (String layerName : layers) {
				if (!gc.layerNameExists(layerName)) {
					logger.finer("layer " + layerName + " does not exist - trying aliases.");
					layerName = gc.getNameFromAlias(layerName);
					if (layerName.compareTo("") == 0) {
						logger.finer("no aliases found for layer, giving up");
					}
				}
					///else {
					LayerInfo layerInfo = catalog.getLayerByName(layerName);
					Map params = layerInfo.getResource().getStore().getConnectionParameters();

					DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc);
					//MemoryDataStore memStore = new MemoryDataStore();
					//memStore.addFeatures(dataStore.getFeatureSource(layerName).getFeatures().features());
					//FeatureSource layer = memStore.getFeatureSource(layerName);
					
					STRtree index = constructGeoTree(dataStore.getFeatureSource(layerName).getFeatures().features(),layerName);
					indexes.add(index);
					
					//System.out.println("Loading layer: " + layer.getName());
					//featureSources.add(layer);
					//}

			}
		} catch (IOException e) {
			logger.severe("IOException in Intersector: " + e.getMessage());
		}
	}

	private STRtree constructGeoTree(FeatureIterator fi, String layerName) {
		
		STRtree index = new STRtree();
		while(fi.hasNext()) {
			Feature f = fi.next();
		    	index.insert(ReferencedEnvelope.reference(f.getBounds()), f.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString());
		}
		fi.close();
		
		return index;

	}
//	@Override
//	public void run() {
//		String cqlFilter = "CONTAINS(the_geom,WKT)";
//		WKTWriter wkt = new WKTWriter();
//		results = new String[points.size()];
//		try {
//			for (int i = 0; i < points.size(); i++) {
//				Point point = points.get(i);
//				//Geometry polygon = point.buffer(1);
//
//				String pointWKT = wkt.write(point);
//				Filter filter = CQL.toFilter(cqlFilter.replace("WKT", pointWKT));
//				StringBuilder sb = new StringBuilder();
//
//				for (FeatureSource layer : featureSources) {
//
//					String layerName = layer.getName().getLocalPart();
//					//System.out.println("SEARCHING: " + layerName);
//					FeatureIterator features = layer.getFeatures(filter).features();
//
//
//					String name = "null";
//					if (features.hasNext()) {
//						Feature feature = features.next();
//						//	System.out.println("Geom property;" + feature.getProperty(feature.getDefaultGeometryProperty().toString()));
//						//Geometry geom = (Geometry)feature.getProperty("the_geom").getValue();
//						//	if (geom.contains(point)) {
//						name = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
//					}
//					sb.append(name).append(",");
//					features.close();
//				}
//				results[i] = sb.toString();
//				//System.out.println(featureNames);
//			}
//		} catch (IOException e1) {
//			logger.severe("IOException while intersecting: " + e1.getMessage());
//		} catch (CQLException e2) {
//			logger.severe("CQLException while intersecting: " + e2.getMessage());
//		} catch (Exception e) {
//		}
//
//		latch.countDown();
//
//	}

	@Override
	public void run() {
		results = new String[points.size()];
		try {
			System.out.println("Thread working");
			for (int i = 0; i < points.size(); i++) {
				Point point = points.get(i);
				StringBuilder sb = new StringBuilder();
				String latitude = String.valueOf(point.getCoordinate().x);
				String longitude = String.valueOf(point.getCoordinate().y);
				sb.append(latitude).append(',').append(longitude);
				//for (String layerName : layers) {
				for (STRtree index : indexes) {
					//STRtree index = indexes.get(layerName);
					List<String> items = index.query(point.getEnvelopeInternal());

					//System.out.println("SEARCHING: " + layerName);
					String name = "null";
					if (items.size() > 0) {
						name = items.get(0);
					}
					sb.append(",").append(name);

				}
				//System.out.println(sb.toString());
				results[i] = sb.toString();
				//System.out.println(featureNames);
			}
		
		} catch (Exception e) {
			logger.severe("Failure while querying: " + e.getMessage());
		}

		latch.countDown();

	}
}

