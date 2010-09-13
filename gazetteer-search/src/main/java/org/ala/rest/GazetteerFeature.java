package org.ala.rest;

import com.vividsolutions.jts.geom.Geometry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    List<String> geometries = new ArrayList();

    public GazetteerFeature(String layerName, String id1, String id2) throws IOException, Exception {
        //System.out.println(">>>>>>>>>>>" + id1);
        //Get the gazetteer config
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
                System.out.println("*********" + layerName);
                FeatureSource layer = dataStore.getFeatureSource(layerName);

                String cql = gc.getIdAttribute1Name(layerName) + "='" + id1.replace('_',' ');
                if (id2 != null)
                    cql += "' AND " +  gc.getIdAttribute2Name(layerName) + "='" + id2.replace('_',' ') + "'";
                else
                    cql += "'";
                //System.out.println(cql);
                FeatureIterator features = layer.getFeatures(CQL.toFilter(cql)).features();

                try
                {
                    if (features.hasNext()) {
                        while (features.hasNext()) {
                            Feature feature = (Feature) features.next();
                            System.out.println("*********" + gc.getNameAttributeName(layerName));
                            this.id = feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString();
                            System.out.println("*********" + this.id);
                            //Construct a geoJSON reperesntation of the geometry uing GeoJSONBuilder
                            StringWriter w = new StringWriter();
                            GeoJSONBuilder geoJson = new GeoJSONBuilder(w);
                            geoJson.writeGeom((Geometry) feature.getDefaultGeometryProperty().getValue());
                            this.geometries.add(w.toString());

                            //Add all the feature properties to the geojson properties object
                            Collection<Property> featureProperties = feature.getProperties();
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                            this.properties = new HashMap();
                            for (Property property : featureProperties) {
                                System.out.println("*********" + property.toString());
                                if ((property.getName() != null) && (property.getValue() != null) && (!(property.getName().toString().contentEquals(geomName)))) {
                                    this.properties.put(property.getName().toString(), property.getValue().toString());
                                }
                            }
                        }
                    } else {
                        throw new Exception("Could not find feature");
                    }
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

    public Map getJSONMap() {
        Map map = new HashMap();
       // map.put("type","Feature");
        map.put("type","GeometryCollection");
        map.put("id",this.id);
        map.put("properties",this.properties);
        //map.put("geometry",this.geometry);
        map.put("geometries", this.geometries);
        return map;
    }
}