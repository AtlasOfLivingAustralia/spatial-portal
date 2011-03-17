/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.rest;

import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 *
 * @author angus
 */
public class Intersector {

	GazetteerConfig gc = new GazetteerConfig();
	GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
	ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
	Catalog catalog = gs.getCatalog();
	private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");
	private List<FeatureSource> featureSources = new ArrayList();
	private List<String> layers;
	private List<String> shapeFiles = new ArrayList();

	public Intersector(List<String> layers) {
		this.layers = layers;
		//Load all the layers to intersect with?
//
//		try {
//			for (String layerName : layers) {
//				if (!gc.layerNameExists(layerName)) {
//					logger.log(Level.FINER, "layer {0} does not exist - trying aliases.", layerName);
//					layerName = gc.getNameFromAlias(layerName);
//					if (layerName.compareTo("") == 0) {
//						logger.finer("no aliases found for layer, giving up");
//					}
//				}
//					///else {
//					LayerInfo layerInfo = catalog.getLayerByName(layerName);
//					Map params = layerInfo.getResource().getStore().getConnectionParameters();
//
//					DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc);
//
//					FeatureSource layer = dataStore.getFeatureSource(layerName);
//					String shapeFileName = createShapeFile(layerName + ".shp",layer);
//					System.out.println("Loading layer: " + layer.getName());
//					featureSources.add(layer);
//					shapeFiles.add(shapeFileName);
//					//}
//
//			}
//		} catch (IOException e1) {
//			logger.log(Level.SEVERE, "IOException in Intersector: {0}", e1.getMessage());
//		} catch (Exception e2) {
//			logger.log(Level.SEVERE, "Exception while creating shapefile {0}", e2.getMessage());
//		}
	}

//	public List<String[]> intersect(double[][] points) {
//		long [] timing = new long[6];
//		timing[0] = System.currentTimeMillis();
//
//		List<String[]> allResults = new ArrayList();
//		//TODO configurable thread count?
//		int threadCount = 6;
//
//		for(String shapeFileName : shapeFiles) {
//			SimpleShapeFile ssf = new SimpleShapeFile(shapeFileName, threadCount);
//			String [] results = ssf.intersect(points);
//
//			allResults.add(results);
//		}
//		return allResults;
//	}
	public List<String[]> intersect(List<Point> points) {
		
		List<String[]> resultSet = new ArrayList();
		//Check each point against each region in
		System.out.println("Intersecting points ...");
		int threadCount = 1;
		int step = points.size()/threadCount;
		if (step == 0) {
			step = 1;
			threadCount = points.size();
		}

		CountDownLatch latch = new CountDownLatch(threadCount);
		List<IntersectThread> threads = new ArrayList();
		for (int i =0; i < threadCount; i++) {
			String[] results = new String[step];	
			IntersectThread it = new IntersectThread(points.subList(i*step, ((i+1)*step)),layers, results, latch);
			threads.add(it);
			it.start();
		}
		try {
		  latch.await();
		} catch (InterruptedException E) {
		}

		for(IntersectThread it : threads) {
			resultSet.add(it.results);
		}

	return resultSet;

	}

	private String createShapeFile(String fileName,  FeatureSource layer) throws IOException, Exception {
		
		FeatureCollection featureCollection = layer.getFeatures();

		File shapeFile = new File(fileName);
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", shapeFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);


		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema((SimpleFeatureType)layer.getSchema());

		/*
		 * You can comment out this line if you are using the createFeatureType
		 * method (at end of class file) rather than DataUtilities.createType
		 */
		newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

		/*
		 * Write the features to the shapefile
		 */
		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		FeatureSource shapeSource = newDataStore.getFeatureSource(typeName);

		if (shapeSource instanceof FeatureStore) {
			FeatureStore shapeStore = (FeatureStore) shapeSource;
			
			shapeStore.setTransaction(transaction);
			try {
				shapeStore.addFeatures(featureCollection);
				transaction.commit();

			} catch (Exception e) {
				
				logger.severe("Problem writing shapefile: " + e.getMessage());
				transaction.rollback();

			} finally {
				transaction.close();
			}
			//System.exit(0); // success!
		} else {
			throw new Exception(typeName + " does not support read/write access");
			
		}

		return shapeFile.getAbsolutePath().replace(".shp", "");

	}

	private static SimpleFeatureType createFeatureType() {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        builder.add("Location", Point.class);
        builder.length(15).add("Name", String.class); // <- 15 chars width for name field

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();

        return LOCATION;
    }
}
