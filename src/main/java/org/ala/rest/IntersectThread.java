package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
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
import org.opengis.feature.simple.SimpleFeature;
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
	HashMap name_geoms = new HashMap();

	IntersectThread(List<Point> points, List<String> layers, String[] results, CountDownLatch latch)  {
		this.points = points;
		System.out.println("Constructor POINTS: " + points.size());
	
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
		System.out.println("Constructing STRtree ...");
		STRtree index = new STRtree();
		
		while(fi.hasNext()) {
			SimpleFeature f = (SimpleFeature)fi.next();
			Geometry g = (Geometry)f.getDefaultGeometry();
			String name = f.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();

			//IndexedPointInAreaLocator locator = new IndexedPointInAreaLocator(g);
			PreparedGeometry p = null;
			if (g.getGeometryType().equalsIgnoreCase("Polygon")) {
				p = new PreparedPolygon( (Polygon)g );
			}
			else {
				p = new PreparedPolygon( (MultiPolygon)g );
			}
			index.insert(ReferencedEnvelope.reference(g.getEnvelopeInternal()), name);
			if (name_geoms.containsKey(name)) {
				((List)name_geoms.get(name)).add(p);
			}
			else {
				List polygons = new ArrayList();
				polygons.add(p);
				name_geoms.put(name,polygons); 
			}//
		}
		fi.close();
		
		return index;

	}
	
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
					if (items.size() == 1) {
						name = items.get(0);
					}
					if (items.size() > 1) {	
						for (String n : items) {
							//IndexedPointInAreaLocator l = (IndexedPointInAreaLocator)name_geoms.get(n);
							for (PreparedGeometry p : (List<PreparedGeometry>)name_geoms.get(n)) {
								if (p.containsProperly(point.getEnvelope())) {
									name = n;
								}
							}
						}
						
					}
					sb.append(",").append(name);
					//sb.append(",").append(String.valueOf(point.getX())).append(",").append(String.valueOf(point.getY()));
					//sb.append(",").append(point.getEnvelopeInternal().toString());
				}
				
				results[i] = sb.toString();
				
			}
		
		} catch (Exception e) {
			logger.severe("Failure while querying: " + e.getMessage());
		}

		latch.countDown();

	}
}

