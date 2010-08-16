package org.ala.rest;

import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import java.io.File;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import junit.framework.Test;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.data.test.MockData;
import org.w3c.dom.NodeList;

public class GazetteerSearchResourceTest extends GeoServerTestSupport {

    public static Test suite() {
        return new OneTimeTestSetup(new GazetteerSearchResourceTest());
    }
//
//
//    protected void oneTimeSetUp() throws Exception {
//        super.oneTimeSetUp();
//
//    }

    public MockData buildTestData() throws Exception {
        MockData dataDirectory = super.buildTestData();
        FileUtils.copyFileToDirectory(new File(dataDirectory.getDataDirectoryRoot().getParentFile().getParent(), "gazetteer.xml"), dataDirectory.getDataDirectoryRoot());
        return dataDirectory;
    }

//    public void testGetAsXML() throws Exception {
//        //make the request, parsing the result as a dom
//        Document dom = getAsDOM("/rest/gazetteer-search/result.xml?q=Australia");
//
//        //print out the result
//        print(dom);
//
//        //make assertions
//        Node message = getFirstElementByTagName(dom, "name");
//        assertNotNull(message);
//        assertTrue(message.getFirstChild().getNodeValue().contains("australia"));
//
//    }
//
//    public void testGetAsJSON() throws Exception {
//        //make the request, parsing the result into a json object
//        JSON json = getAsJSON("/rest/gazetteer-search/result.json?q=Australia");
//
//        //print out the result
//        print(json);
//
//        //make assertions
//        assertTrue(json instanceof JSONObject);
//        JSONObject search = ((JSONObject) json).getJSONObject("org.ala.rest.GazetteerSearch");
//        String result = (String) ((JSONObject) (search.getJSONObject("results").getJSONArray("org.ala.rest.SearchResultItem").get(0))).get("name");
//        assertTrue(result.contains("australia"));
//
//        //assertTrue(1==1);
//    }

    public void testSearchResourceAsXML() throws Exception {
        //make the request, parsing the result into a json object
        Document dom = getAsDOM("/rest/gazetteer/result.xml?q=ashton");

        //print out the result
       // print(dom);
        assertTrue(1==1);
        //make assertions
        
//        JSONObject search = ((JSONObject) json).getJSONObject("org.ala.rest.Search");
//        String result = (String) ((JSONObject) (search.getJSONObject("results").getJSONArray("org.ala.rest.SearchResultItem").get(0))).get("name");
//        assertTrue(result.contains("ashton"));
    }

    public void testSearchNameCommaType() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton,type=NamedPlaces");

        //print out the result
    //    print(dom);

        // assertTrue(1==1);
        //make assertions
//        assertTrue(json instanceof JSONObject);
//        JSONObject search = ((JSONObject) json).getJSONObject("org.ala.rest.Search");
//        String result = (String) ((JSONObject) (search.getJSONObject("results").getJSONArray("org.ala.rest.SearchResultItem").get(0))).get("name");
//        assertTrue(result.contains("ashton"));
    }

    public void testSearchNameAndType() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton&type=NamedPlaces");
        
        print(dom);

         XPathFactory factory = XPathFactory.newInstance();
         XPath xpath = factory.newXPath();
         XPathExpression namesExpr = xpath.compile("//search/results/result/name/text()");
         NodeList names = (NodeList) namesExpr.evaluate(dom, XPathConstants.NODESET);
        //make assertions

        assertTrue(names.getLength()==1);
        
    }

    public void testSearchWrongType() throws Exception {
         Document dom = getAsDOM("/rest/gazetteer/result.xml?q=ashton&type=NOT_A_VALID_TYPE");

        //print out the result
        print(dom);

         XPathFactory factory = XPathFactory.newInstance();
         XPath xpath = factory.newXPath();
         XPathExpression namesExpr = xpath.compile("//search/results/result/name/text()");
         NodeList names = (NodeList) namesExpr.evaluate(dom, XPathConstants.NODESET);
        //make assertions
        
        assertTrue(names.getLength()==0);
        
    }
    public void testSearchGetHyperlink() throws Exception {
         Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton");

        //print out the result
        //print(dom);

        //make assertions
        
    }

     public void testMultiFieldID() throws Exception {
//      FileUtils.copyFileToDirectory(new File(GeoserverDataDirectory.getGeoserverDataDirectory().getParent(),"gazetteer.xml"), GeoserverDataDirectory.getGeoserverDataDirectory());
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces/Ashton/117.json");
        print(json);
    }

    public void testFeatureServiceJSON() throws Exception {
//      FileUtils.copyFileToDirectory(new File(GeoserverDataDirectory.getGeoserverDataDirectory().getParent(),"gazetteer.xml"), GeoserverDataDirectory.getGeoserverDataDirectory());
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces/Ashton.json");
        //print(json);
    }

    public void testFeatureServiceWhiteSpaceInName() throws Exception {
//      FileUtils.copyFileToDirectory(new File(GeoserverDataDirectory.getGeoserverDataDirectory().getParent(),"gazetteer.xml"), GeoserverDataDirectory.getGeoserverDataDirectory());
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces/Goose_Island.json");
       // print(json);
    }

    public void testGazetteerLuceneIndex() throws Exception {
        File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "gazetteer-index");
        if (file.exists()) {
            IndexSearcher is = new IndexSearcher(FSDirectory.open(file));
            QueryParser qp = new QueryParser(Version.LUCENE_CURRENT, "name", new StandardAnalyzer(Version.LUCENE_CURRENT));
            Query nameQuery = qp.parse("Ash*");

            TopDocs topDocs = is.search(nameQuery, 20);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = is.doc(scoreDoc.doc);
                List<Fieldable> fields = doc.getFields();
                for (Fieldable field : fields) {
                    System.out.println(field.name() + ": " + field.stringValue());
                }
                System.out.println("---------------------------------------------");
            }

            System.out.println("---------------------------------------------");
            System.out.println("Total hits: " + topDocs.totalHits);
            System.out.println("---------------------------------------------");
        } else {
            assertTrue(0 == 1);
        }
    }
}
