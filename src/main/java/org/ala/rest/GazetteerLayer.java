package org.ala.rest;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.platform.GeoServerExtensions;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 *
 * @author Angus
 */
public class GazetteerLayer {

    String classList;
    String classAttribute;

    public GazetteerLayer(String layerName) {
        GazetteerConfig gc = GeoServerExtensions.bean(GazetteerConfig.class);
        classAttribute = gc.getClassAttributeName(layerName);
        if (classAttribute.contentEquals("none")) {
            classList = "none";
        } else {
            try {
                //Get the geoserver data directory from the geoserver instance
                File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-class-index");
                IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

                QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "layer", new StandardAnalyzer(Version.LUCENE_CURRENT));

                Query nameQuery = qp.parse(layerName);

                //TODO: instead of 20 - should be variable and paging?
                TopDocs topDocs = is.search(nameQuery, 1);

//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                Document doc = is.doc(scoreDoc.doc);
//                List<Fieldable> fields = doc.getFields();
//                results.add(fields.toString());
//            }

                ScoreDoc scoreDoc = topDocs.scoreDocs[0];
                Document doc = is.doc(scoreDoc.doc);
                classList = doc.getField(classAttribute).stringValue();


                is.close();


            } catch (IOException e1) {
                //FIXME: Log error - return http error code?
                System.out.println(e1.getMessage());
            } catch (ParseException e3) {
                //FIXME
            }

        }
    }

    public Map getMap() {
        Map map = new HashMap();
        map.put(classAttribute,classList);
        return map;
    }
}
