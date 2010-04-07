import org.apache.commons.io.FileUtils;
//import org.apache.lucene.analysis.KeywordAnalyzer;
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

import java.io.StringReader;
import org.apache.lucene.document.Fieldable;

import java.sql.*;

public class MyLuceneTest {

    public static void main(String[] args)  {

	String url = "jdbc:postgresql://localhost:2345/spatialdb?user=postgres&password=postgres";
	try {
	    Connection conn = DriverManager.getConnection(url);
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery("SELECT serial,name,source,feature_code,state_id FROM \"GeoRegionFeatures\"");
	    
	    File file = new File("test-index");
	    if(file.exists()){
		FileUtils.forceDelete(file);
	    }
	    FileUtils.forceMkdir(file);

	    int i=0;

	    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
	    IndexWriter iw = new IndexWriter(FSDirectory.open(file), analyzer /*Version.LUCENE_CURRENT)*/, true, IndexWriter.MaxFieldLength.UNLIMITED);

	    long start = System.currentTimeMillis();

	    //add the relationships
	    // TabReader tr = new TabReader("gazetteer-index.csv");
	    String[] keyValue = null;
	    while (rs.next()) {
		Document doc = new Document();
		doc.add(new Field("serial", rs.getString(1).toLowerCase(), Store.YES, Index.ANALYZED));

		if(rs.getString(2) != null)
		    doc.add(new Field("name", rs.getString(2).toLowerCase(), Store.YES, Index.ANALYZED));
		else
		    doc.add(new Field("name", "unknown", Store.YES, Index.ANALYZED));
		
		doc.add(new Field("source", rs.getString(3).toLowerCase(), Store.YES, Index.ANALYZED));

		if(rs.getString(4) != null)
		    doc.add(new Field("type", rs.getString(4).toLowerCase(), Store.YES, Index.ANALYZED));
		else
		    doc.add(new Field("type", "unknown", Store.YES, Index.ANALYZED));

		if(rs.getString(5) != null)
		    doc.add(new Field("state", rs.getString(5).toLowerCase(), Store.YES, Index.ANALYZED));
		else
		    doc.add(new Field("state", "unknown", Store.YES, Index.ANALYZED));


		iw.addDocument(doc);
		System.out.println("working");

	    }
	    rs.close();
	    st.close();
	    iw.close();	
	    /* while((keyValue=tr.readNext())!=null){
	       System.out.println(keyValue[0]);
	       if(keyValue.length==4){

	       i++;

	       Document doc = new Document();

	       doc.add(new Field("serial", (keyValue[0]).toLowerCase(), Store.YES, Index.ANALYZED));

	       if(keyValue[1] != null)
	       doc.add(new Field("name", (keyValue[1]).toLowerCase(), Store.YES, Index.ANALYZED));
	    //doc.add(new Field("name", new StringReader(keyValue[1].toLowerCase())));
	    else
	    doc.add(new Field("name", "unknown", Store.YES, Index.ANALYZED));

	    doc.add(new Field("source", (keyValue[2]).toLowerCase(), Store.YES, Index.ANALYZED));

	    iw.addDocument(doc);
	    System.out.println("working");

	    }

	    }*/

	    //tr.close();

	    long finish = System.currentTimeMillis();

	    System.out.println(i+" loaded relationships, Time taken "+(((finish-start)/1000)/60)+" minutes, "+(((finish-start)/1000) % 60)+" seconds.");

	 


	search("\"ben\" OR \"lomond\"");
	search("australia");
	search("lomond");
	search("ben");
	search("aus*");
	}
	catch (Exception e)
	{
	    System.out.println(e.getMessage());
	}

	
    }

	public static void search(String input) throws Exception
	{
	    File file = new File("test-index");

	    IndexSearcher is = new IndexSearcher(FSDirectory.open(file));



	    QueryParser qp  = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));

	    //                               Query nameQuery = qp.parse("\""+input+"\"");
	    Query nameQuery = qp.parse(input);




	    TopDocs topDocs = is.search(nameQuery, 20);



	    for(ScoreDoc scoreDoc: topDocs.scoreDocs){

		Document doc = is.doc(scoreDoc.doc);



		List<Fieldable> fields = doc.getFields();

		for(Fieldable field: fields){

		    System.out.println(field.name()+": "+field.stringValue());

		}

		System.out.println("---------------------------------------------");

	    }

	    System.out.println("Total hits: "+topDocs.totalHits);

	    //long finish = System.currentTimeMillis();

	    //System.out.println("Time taken: "+ ((float)(finish-start))/1000+" seconds.");

	    System.out.println("---------------------------------------------");



	    System.out.println();

	}

    }
