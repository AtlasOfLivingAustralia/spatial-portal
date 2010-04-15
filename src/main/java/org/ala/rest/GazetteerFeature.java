package org.ala.rest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;

import java.util.Map;
import javax.servlet.ServletContext;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.CatalogReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import org.geoserver.config.GeoServer;

import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.Property;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import org.vfny.geoserver.util.DataStoreUtils;

/**
 *
 *@author angus
 */
public class GazetteerFeature {


    String name;
//    List properties;

    public GazetteerFeature(String layerName, String featureName) throws IOException, Exception {

        //Get the gazetteer config - //TODO: should make this a spring singleton
        GazetteerConfig gc = new GazetteerConfig(new File(GeoserverDataDirectory.getGeoserverDataDirectory(),"gazetteer.xml"));

        GeoServer gs = GeoServerExtensions.bean( GeoServer.class );
        ServletContext sc = GeoServerExtensions.bean( ServletContext.class );
        Catalog catalog = gs.getCatalog();

        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        Map params = layerInfo.getResource().getStore().getConnectionParameters();

        DataStore dataStore = DataStoreUtils.acquireDataStore(params,sc);//DataStoreFinder.getDataStore(params);

        try {

            if (dataStore == null)
                    throw new Exception("Could not find datastore for this layer");
            else {
                FeatureSource layer = dataStore.getFeatureSource(layerName);

                FeatureIterator features = layer.getFeatures(CQL.toFilter(gc.getIdAttributeName(layerName) + "= '" + featureName + "'")).features();
//                System.out.println("*****" + features.size());
//                Feature[] featuresArray = (Feature[])features.toArray();
//                Feature feature = featuresArray[0];
                if (features.hasNext()){
                     Feature feature = (Feature) features.next();
                    this.name = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
    //                Collection<Property> properties = feature.g;
    //                for(Property property : properties) {
    //                    this.properties.add(property); // = feature.getProperties().toArray()[];
    //               }
                }
                else
                    throw new Exception("Could not find feature");
                    
               
            }
        }
        finally {
             dataStore.dispose();
        }
    }
}