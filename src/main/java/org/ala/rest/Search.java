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
/***
 *
 * @author angus
 */
@XStreamAlias("search")
public class Search {

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

    public Search(String searchTerms, String type) {
        results = new ArrayList<SearchResultItem>();

        try {
            //Get the geoserver data directory from the geoserver instance
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

            String[] searchFields = {"name","type"};

            MultiFieldQueryParser qp = new MultiFieldQueryParser(Version.LUCENE_CURRENT,searchFields,new StandardAnalyzer(Version.LUCENE_CURRENT));//Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));
            qp.setDefaultOperator(qp.AND_OPERATOR);
            Query nameQuery = qp.parse(searchTerms.toLowerCase() + " AND " + type);

            //TODO: instead of 20 - should be variable and paging?
            TopDocs topDocs = is.search(nameQuery, 20);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = is.doc(scoreDoc.doc);
                List<Fieldable> fields = doc.getFields();
                results.add(new SearchResultItem(fields));
            }
        } catch (IOException e1) {
            //FIXME: Log error - return http error code?
            System.out.println(e1.getMessage());
        } catch (ParseException e3) {
            //FIXME
        }
    }

    public Search(String searchTerms) {
        results = new ArrayList<SearchResultItem>();

        try {
            //Get the geoserver data directory from the geoserver instance
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

            QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));

            Query nameQuery = qp.parse(searchTerms.toLowerCase());

            //TODO: instead of 20 - should be variable and paging?
            TopDocs topDocs = is.search(nameQuery, 20);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = is.doc(scoreDoc.doc);
                List<Fieldable> fields = doc.getFields();
                results.add(new SearchResultItem(fields));
            }
        } catch (IOException e1) {
            //FIXME: Log error - return http error code?
            System.out.println(e1.getMessage());
        } catch (ParseException e3) {
            //FIXME
        }

    }
}
