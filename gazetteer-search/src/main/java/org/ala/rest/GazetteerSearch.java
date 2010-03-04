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

public class GazetteerSearch {

   ArrayList<SearchResultItem> results;

   public GazetteerSearch( String searchString ) throws Exception{
       results = new ArrayList<SearchResultItem>();
 
	try
	{       
    
//	URL url = getClass().getResource("/test-index");//new File("test-index");
//	System.out.println(url);
//		File file = new File(url.toString().replace("file:",""));
//		File file = new File("/home/gus/SRC/lucene-test/test-index");
//	    GeoServerResourceLoader loader = GeoServerApplication.get().getResourceLoader();
	
//	    File file = loader.find( "test-index" );
		//String fileName = this.getServletConfig().getContextParameter("indexLocation");
		File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory() + "/test-index");
	    IndexSearcher is = new IndexSearcher(FSDirectory.open(file));//url.toString().replace("file:","")));



                    QueryParser qp  = new QueryParser(Version.LUCENE_CURRENT, "name", new KeywordAnalyzer());

                    Query nameQuery = qp.parse("\""+searchString+"\"");




                    TopDocs topDocs = is.search(nameQuery, 20);



                    for(ScoreDoc scoreDoc: topDocs.scoreDocs){

                            Document doc = is.doc(scoreDoc.doc);



                            List<Fieldable> fields = doc.getFields();
                            
                           // results.add(new SearchResultItem("Australia",1));
                            results.add(new SearchResultItem(fields.get(1).stringValue(),fields.get(0).stringValue()));
                            
                            /*for(Fieldable field: fields){

                                    System.out.println(field.name()+": "+field.stringValue());

                            }

                            System.out.println("---------------------------------------------");
				*/
                    }

         }
	catch(Exception e)
	{
		throw(e);	//	
	}	     
	
}

   }

