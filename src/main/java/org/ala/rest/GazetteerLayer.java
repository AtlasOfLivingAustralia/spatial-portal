package org.ala.rest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 *
 * @author Angus
 */
public class GazetteerLayer {

    String classList;
    String classAttribute;
    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerLayer");
    Map layerMap = new HashMap();
    Map classMap = new HashMap();
    GazetteerConfig gc = new GazetteerConfig();
    GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
    ServletContext sc = GeoServerExtensions.bean(ServletContext.class);
    Catalog catalog = gs.getCatalog();
    String layerName = "";

    public GazetteerLayer(String layerName) {

        GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);
        //check to see if layer exists
        if (!gc.layerNameExists(layerName)) {
            //if not, check to see if layer alias exists
            layerName = gc.getNameFromAlias(layerName);
            if (layerName.compareTo("") == 0) {
                //otherwise log an error
                logger.severe("No such layer or layer alias " + layerName + " ignoring ...");
            }
        }

        this.layerName = layerName;

        classAttribute = gc.getClassAttributeName(layerName);
        if (classAttribute.contentEquals("none")) {
            logger.info("No layer classes are defined");
            classList = "No layer classes are defined";
        } else {
            try {
                //Get the geoserver data directory from the geoserver instance
                File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-class-index");
                IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

                QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "layer", new StandardAnalyzer(Version.LUCENE_CURRENT));

                Query nameQuery = qp.parse(layerName);

                TopDocs topDocs = is.search(nameQuery, 1);
                if (topDocs.totalHits != 1) {
                    logger.severe("We are expecting some layer class details in the index - time to re-index?");
                } else {
                    ScoreDoc scoreDoc = topDocs.scoreDocs[0];
                    Document doc = is.doc(scoreDoc.doc);
                    classList = doc.getField(classAttribute).stringValue();
                    is.close();
                }
                classMap.put(classAttribute, classList);

            } catch (IOException e1) {
                logger.severe("Problem reading gaz class index");
                logger.severe(ExceptionUtils.getFullStackTrace(e1));
            } catch (ParseException e3) {
                logger.severe("Problem reading lucene class index");
                logger.severe(ExceptionUtils.getFullStackTrace(e3));
            }
        }
        logger.finer("fetching layer properties for " + layerName);
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        layerMap.put("layer_name", layerInfo.getName());
        layerMap.put("alias", gc.getLayerAlias(layerName));
        layerMap.put("default", new Boolean(gc.isDefaultLayer(layerName)).toString());
    }

    public Map getMap() {
        Map map = new HashMap();
        map.put("layer_details", layerMap);
        map.put("layer_classes", classMap);
        return map;
    }

    @Deprecated
    public Map getLegacyMap() {
        
        return classMap;
    }
}
