package org.ala.rest;

import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import java.io.File;
import org.apache.lucene.util.Version;
import java.util.List;
import org.apache.lucene.store.FSDirectory;
import java.net.URL;
import org.apache.lucene.document.Fieldable;
//import org.geoserver.web.GeoServerApplication;
//import org.geoserver.platform.GeoServerResourceLoader;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import org.apache.lucene.index.CorruptIndexException;
import java.io.IOException;
import org.apache.lucene.queryParser.ParseException;

public class GazetteerSearch {

   ArrayList<SearchResultItem> results;

   public GazetteerSearch( String searchString ) {
       results = new ArrayList<SearchResultItem>();
 
	try
	{       
   		//Get the geoserver data directory from the geoserver instance 
		File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),"test-index");
	    IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));



                    QueryParser qp  = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));

                    Query nameQuery = qp.parse(searchString.toLowerCase());




                    TopDocs topDocs = is.search(nameQuery, 20);



                    for(ScoreDoc scoreDoc: topDocs.scoreDocs){

                            Document doc = is.doc(scoreDoc.doc);



                            List<Fieldable> fields = doc.getFields();
                            
                            results.add(new SearchResultItem(fields.get(1).stringValue(),fields.get(0).stringValue(),fields.get(3).stringValue(),fields.get(4).stringValue()));

                    }

         }
	catch(IOException e1)
	{
		//FIXME
		//Log error - return http code?
		System.out.println(e1.getMessage());	
	}	     
	catch(ParseException e3)
	{
		//FIXME
	}

  }
}

