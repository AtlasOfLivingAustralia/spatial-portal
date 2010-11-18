package org.ala.rest;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
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
    
    public GazetteerLayer(String layerName) {
        GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);
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
                if (topDocs.totalHits != 1){
                    logger.severe("We are expecting some layer class details in the index - time to re-index?");
                }
                else{
                    ScoreDoc scoreDoc = topDocs.scoreDocs[0];
                    Document doc = is.doc(scoreDoc.doc);
                    classList = doc.getField(classAttribute).stringValue();
                    is.close();
                }

            } catch (IOException e1) {
                logger.severe("Problem reading gaz class index");
                logger.severe(ExceptionUtils.getFullStackTrace(e1));
            } catch (ParseException e3) {
                logger.severe("Problem reading lucene class index");
                logger.severe(ExceptionUtils.getFullStackTrace(e3));

            }

        }
    }

    public Map getMap() {
        Map map = new HashMap();
        map.put(classAttribute,classList);
        return map;
    }
}
