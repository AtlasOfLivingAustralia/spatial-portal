package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import org.geotools.data.DataStore;

import java.util.Map;

import javax.servlet.ServletContext;

import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import org.geoserver.config.GeoServer;

import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.Property;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import org.vfny.geoserver.util.DataStoreUtils;
import org.geoserver.wfs.response.GeoJSONBuilder;


/**
 *
 *@author angus
 */
public class GazetteerFeature {


    String id;
    Map properties;
    String geometry;

    public GazetteerFeature(String layerName, String featureName) throws IOException, Exception {

        //Get the gazetteer config - //TODO: should make this a spring singleton
        GazetteerConfig gc = new GazetteerConfig();

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

                FeatureIterator features = layer.getFeatures(CQL.toFilter(gc.getIdAttributeName(layerName) + "= '" + featureName.replace('_',' ') + "'")).features();

                try
                {
                    if (features.hasNext()) {
                        Feature feature = (Feature) features.next();
                        this.id = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();

                        //Construct a geoJSON reperesntation of the geometry uing GeoJSONBuilder
                        StringWriter w = new StringWriter();
                        GeoJSONBuilder geoJson = new GeoJSONBuilder(w);
                        geoJson.writeGeom((Geometry)feature.getDefaultGeometryProperty().getValue());
                        this.geometry = w.toString();

                        //Add all the feature properties to the geojson properties object
                        Collection<Property> featureProperties = feature.getProperties();
                        String geomName = feature.getDefaultGeometryProperty().getName().toString();
                        this.properties = new HashMap();
                        for(Property property : featureProperties) {
                            if ((property.getName() != null)&&(property.getValue() != null)&&(!(property.getName().toString().contentEquals(geomName)))) {
                                this.properties.put(property.getName().toString(),property.getValue().toString());
                            }
                        }
                    }
                    else
                        throw new Exception("Could not find feature");
                    }
                finally {
                    features.close();
                }
               
            }
        }
        finally {
             dataStore.dispose();
        }
    }

    public Map getMap() {
        Map map = new HashMap();
        map.put("type","Feature");
        map.put("id",this.id);
        map.put("properties",this.properties);
        map.put("geometry",this.geometry);
        return map;
    }
}