package org.ala.rest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.CatalogReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;

import org.geoserver.config.GeoServer;

import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 *
 *@author angus
 */
public class GazetteerFeature {


    Object theFeature;

    public GazetteerFeature(String layerName, String featureName) throws IOException, Exception {

    GeoServer gs = GeoServerExtensions.bean( GeoServer.class );

    Catalog catalog = gs.getCatalog();

    LayerInfo layerInfo = catalog.getLayerByName(layerName);
    Map params = layerInfo.getResource().getStore().getConnectionParameters(); //.getName().toString();
    // File catalog = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),"catalog.xml");
      // File catalog = new File("/media/raid-storage/ALA Data/ALA - Dave Martin Data/catalog.xml");
//        CatalogReader reader = new CatalogReader();
//                  reader.read( catalog );
//                  List dataStores = reader.dataStores();
//                  Map namespaces = reader.namespaces();
//               System.out.println("****" + dataStores.size());
                  //   Map dataStoreParams = (Map)dataStores.get(0);


        System.out.println("***" + layerName);
     /*  Map params = new HashMap();
params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
params.put(PostgisNGDataStoreFactory.HOST.key, "localhost");
params.put(PostgisNGDataStoreFactory.PORT.key, 2345);
params.put(PostgisNGDataStoreFactory.SCHEMA.key, "public");
params.put(PostgisNGDataStoreFactory.DATABASE.key, "spatialdb");
params.put(PostgisNGDataStoreFactory.USER.key, "postgres");
params.put(PostgisNGDataStoreFactory.PASSWD.key, "postgres");*/

         //DefaultRegistry registry = new DefaultRegistry();
         //registry.addDataStore("spatialdb", params);

        //File file = new File("/media/raid-storage/ALA Data/ALA - Dave Martin Data/data/ibra/ibra_reg_shape.shp");
        //params.put("url",file.toURL());

        try
        {

        DataStore dataStore = DataStoreFinder.getDataStore(params);

        if (dataStore == null)
                this.theFeature = "no data store";
        else
        {
        FeatureSource layer = dataStore.getFeatureSource(layerName);
        FeatureCollection features = layer.getFeatures(Query.ALL);
        Feature feature = (Feature)(features.toArray())[0]; //features().next();
        if (feature == null)
            this.theFeature = "cries";
        else
            this.theFeature = feature.getIdentifier();
        }
        }
        catch(Exception e)
        {
         e.printStackTrace();
         this.theFeature = "fail";
        }
        

    }
}