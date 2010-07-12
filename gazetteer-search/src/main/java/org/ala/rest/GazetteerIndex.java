package org.ala.rest;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.springframework.beans.factory.InitializingBean;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.vfny.geoserver.util.DataStoreUtils;

/***
 * Builds the Gazetter index based on the gazetter config and layers/features in Geoserver
 * @author Angus
 */
public class GazetteerIndex implements InitializingBean {

    /***
     * The Gazetteer index is built here if one does not exist already
     */
    @Override
    public void afterPropertiesSet() {
        //Get geoserver catalog from Geoserver config
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        Catalog catalog = gs.getCatalog();

        ServletContext sc = GeoServerExtensions.bean(ServletContext.class);


        GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);
        //GazetteerConfig gc = new GazetteerConfig();

        gc.getLayerNames();
        DataStore dataStore = null;
        FeatureIterator features = null;
        try {

            //Initialize lucene index
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            if (file.exists()) {
                return;//FileUtils.forceDelete(file);
            } else {
                FileUtils.forceMkdir(file);

                StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
                IndexWriter iw = new IndexWriter(FSDirectory.open(file), analyzer /*Version.LUCENE_CURRENT)*/, true, IndexWriter.MaxFieldLength.UNLIMITED);


                for (String layerName : gc.getLayerNames()) {
                    LayerInfo layerInfo = catalog.getLayerByName(layerName);

                    ResourceInfo layerResource = layerInfo.getResource();
                    StoreInfo layerStore = layerResource.getStore();
                    Map params = layerStore.getConnectionParameters();//layerInfo.getResource().getStore().getConnectionParameters();

                    dataStore = DataStoreUtils.acquireDataStore(params, sc);//DataStoreFinder.getDataStore(params);

                    if (dataStore == null) {
                        throw new Exception("Could not find datastore for this layer");
                    } else {
                        FeatureSource layer = dataStore.getFeatureSource(layerName);
                        features = layer.getFeatures().features();
                        List<String> descriptionAttributes = gc.getDescriptionAttributes(layerName);
                        String idAttribute = gc.getIdAttributeName(layerName);
                        while (features.hasNext()) {
                            Feature feature = features.next();
                            Document doc = new Document();
                            //Add name and type to the index for searching

                            if (feature.getProperty(gc.getIdAttributeName(layerName)).getValue() != null) {
                                doc.add(new Field("id", feature.getProperty(gc.getIdAttributeName(layerName)).getValue().toString(), Store.YES, Index.ANALYZED));
                            }
                            if (feature.getProperty(gc.getNameAttributeName(layerName)).getValue() != null) {
                                doc.add(new Field("name", feature.getProperty(gc.getNameAttributeName(layerName)).getValue().toString().toLowerCase(), Store.YES, Index.ANALYZED));
                            }

                            doc.add(new Field("type", layerName, Store.YES, Index.ANALYZED));

                            //Add all the other feature properties to the index as well but not for searching
                            String geomName = feature.getDefaultGeometryProperty().getName().toString();
                           // String idString = "";
                            for (Property property : feature.getProperties()) {
                                System.out.println(property.getName().toString());
                                if ((descriptionAttributes.contains(property.getName().toString())) && (property.getValue() != null)) { //&& (!(property.getName().toString().contentEquals(geomName)))) {
                                    doc.add(new Field(property.getName().toString(), property.getValue().toString(), Store.YES, Index.NO));

                                }
//                                // where there is more than one id attribute - the id becomes a concatenation
//                                if ((idAttributes.contains(property.getName().toString()))) {
//                                    idString += property.getValue().toString();
//                                }
                            }
                            
//                            doc.add(new Field("id", idString, Store.YES, Index.ANALYZED));

                            iw.addDocument(doc);
                            System.out.print(".");
                        }
                        features.close();
                    }
                    dataStore.dispose();
                }
                iw.close();
            }
        } catch (Exception e) {
            //FIXME
            e.printStackTrace();
        } finally {
            if (features != null) {
                features.close();
            }
            if (dataStore != null) {
                dataStore.dispose();
            }

        }
    }
}
