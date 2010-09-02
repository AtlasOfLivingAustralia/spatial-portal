package org.ala.rest;

import java.util.ArrayList;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import java.io.File;
import org.apache.lucene.util.Version;
import java.util.List;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Fieldable;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import java.io.IOException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import java.util.Map;
import java.util.HashMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import javax.servlet.ServletContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import org.vfny.geoserver.util.DataStoreUtils;
/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class PointSearch {
    @XStreamAlias("results")
    ArrayList<SearchResultItem> results;

    @XStreamAlias("xmlns:xlink")
    @XStreamAsAttribute
    String xlink = "http://www.w3.org/1999/xlink";

    /***
     *
     * @return a HashMap representation of the resource - which will be serialized into xml/json
     */
    public Map getMap() {
        HashMap resultsMap = new HashMap();
        resultsMap.put("results",this.results);
        return resultsMap;
    }

    public PointSearch(String lon, String lat, String layerName) {
        results = new ArrayList<SearchResultItem>();
         GazetteerConfig gc = new GazetteerConfig();

        GeoServer gs = GeoServerExtensions.bean( GeoServer.class );
        ServletContext sc = GeoServerExtensions.bean( ServletContext.class );
        Catalog catalog = gs.getCatalog();

        try {

           // for (String layerName : gc.getLayerNames()) {
                LayerInfo layerInfo = catalog.getLayerByName(layerName);
                Map params = layerInfo.getResource().getStore().getConnectionParameters();

                DataStore dataStore = DataStoreUtils.acquireDataStore(params, sc);//DataStoreFinder.getDataStore(params);
                FeatureSource layer = dataStore.getFeatureSource(layerName);

//                Coordinate point = new Coordinate(Float.parseFloat(lat),Float.parseFloat(lon));
//
//                FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
//                Filter filter = ff.contains(ff.property("POLYGON"), ff.literal(lon + ", " +lat));
//                System.out.println(filter.toString());

//                String cql = gc.getIdAttribute1Name(layerName) + "='" + id1.replace('_',' ');
//                if (id2 != null)
//                    cql += "' AND " +  gc.getIdAttribute2Name(layerName) + "='" + id2.replace('_',' ') + "'";
//                else
//                    cql += "'";
                //System.out.println(cql);
                FeatureIterator features = layer.getFeatures(CQL.toFilter("CONTAINS(the_geom,POINT(" + lon + " " +lat +"))")).features();

                if (features.hasNext()) {
                    System.out.println("Found one!!!");
                    while (features.hasNext()) {
                        Feature feature = (Feature) features.next();
                        String id = feature.getProperty(gc.getIdAttribute1Name(layerName)).getValue().toString();
                        results.add(new SearchResultItem(layerName,id));
                    }
                }
          //  }
        }
    catch (IOException e1) {
            //FIXME: Log error - return http error code?
            System.out.println(e1.getMessage());
        }
   catch (Exception e2) {
        System.out.println(e2.getMessage());
    }
    }

//    public Search(String searchTerms) {
//        results = new ArrayList<SearchResultItem>();
//
//        try {
//            //Get the geoserver data directory from the geoserver instance
//            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
//            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));
//
//            QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));
//
//            Query nameQuery = qp.parse(searchTerms.toLowerCase());
//
//            //TODO: instead of 20 - should be variable and paging?
//            TopDocs topDocs = is.search(nameQuery, 20);
//
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                Document doc = is.doc(scoreDoc.doc);
//                List<Fieldable> fields = doc.getFields();
//                results.add(new SearchResultItem(fields,true));
//            }
//        } catch (IOException e1) {
//            //FIXME: Log error - return http error code?
//            System.out.println(e1.getMessage());
//        } catch (ParseException e3) {
//            //FIXME
//        }
//
//    }
//}


}
