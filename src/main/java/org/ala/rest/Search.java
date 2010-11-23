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
import java.util.logging.Logger;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geotools.util.logging.Logging;

/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class Search {

    private static final Logger logger = Logging.getLogger("org.ala.rest.Search");
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
        resultsMap.put("results", this.results);
        return resultsMap;
    }

    public ArrayList<SearchResultItem> getResults() {
        return this.results;
    }


    public Search(String searchTerms, String[] layers) {
        results = new ArrayList<SearchResultItem>();
        for (String layerName : layers) {
            try {
                //Get the geoserver data directory from the geoserver instance
                File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
                IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

                String[] searchFields = {"id", "layerName"};

                MultiFieldQueryParser qp = new MultiFieldQueryParser(Version.LUCENE_CURRENT, searchFields, new StandardAnalyzer(Version.LUCENE_CURRENT));//Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));
                qp.setDefaultOperator(qp.AND_OPERATOR);
                Query nameQuery = qp.parse(searchTerms.toLowerCase() + " AND " + layerName);

                //TODO: instead of 20 - should be variable and paging?
                TopDocs topDocs = is.search(nameQuery, 20);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = is.doc(scoreDoc.doc);
                    List<Fieldable> fields = doc.getFields();
                    results.add(new SearchResultItem(fields, true, new Float(scoreDoc.score)));
                }
                is.close();
            } catch (IOException e1) {
                //FIXME: Log error - return http error code?
                logger.severe("An error occurred in search.");
                logger.severe(ExceptionUtils.getFullStackTrace(e1));
            } catch (ParseException e3) {
                logger.severe("An error occurred parsing search terms.");
                logger.severe(ExceptionUtils.getFullStackTrace(e3));
            }
        }
    }

    public Search(String searchTerms) {
        logger.finer("search terms are " + searchTerms);
        results = new ArrayList<SearchResultItem>();

        try {
            //Get the geoserver data directory from the geoserver instance
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

            QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "id", new StandardAnalyzer(Version.LUCENE_CURRENT));

            Query nameQuery = qp.parse(searchTerms.toLowerCase());

            //TODO: instead of 20 - should be variable and paging?
            TopDocs topDocs = is.search(nameQuery, 20);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                logger.finer("document score match is " + new Float(scoreDoc.score).toString());
                Document doc = is.doc(scoreDoc.doc);
                List<Fieldable> fields = doc.getFields();
                results.add(new SearchResultItem(fields, true, new Float(scoreDoc.score)));
            }
            is.close();
        } catch (IOException e1) {
            //FIXME: Log error - return http error code?
            logger.severe("An error occurred in search.");
            logger.severe(ExceptionUtils.getFullStackTrace(e1));
        } catch (ParseException e3) {
            logger.severe("An error occurred parsing search terms.");
            logger.severe(ExceptionUtils.getFullStackTrace(e3));
        }

    }
}
