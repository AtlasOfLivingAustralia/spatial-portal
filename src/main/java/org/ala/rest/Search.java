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
import org.apache.lucene.queryParser.ParseException;

public class Search {

    ArrayList<SearchResultItem> results;

    public Search(String searchString) {
        results = new ArrayList<SearchResultItem>();

        try {
            //Get the geoserver data directory from the geoserver instance
            File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));

            QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));

            Query nameQuery = qp.parse(searchString.toLowerCase());

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
