package au.org.emii.portal.mest;

import au.org.emii.portal.Website;
import au.org.emii.portal.wms.WMSServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MESTGetRecord {

    private final String sWebMap111 = "OGC:WMS-1.1.1-http-get-map";
    private final String sWebMap130 = "OGC:WMS-1.3.0-http-get-map";
    private final String sWebMap100 = "OGC:WMS-1.0.0-http-get-map";
    private final String sWebServer111 = "OGC:WMS-1.1.1-http-get-capabilities";
    private final String sMetaURL = "WWW:LINK-1.0-http--metadata-URL";
    private final String sHTTPURL = "WWW:LINK-1.0-http--link";
    private final String sDownload = "WWW:DOWNLOAD-1.0-http--downloaddata";
    private String SearchParameter;
    private Document SearchFilter;
    private Document SearchResult;
    private String SearchKVP;
    private HttpClient client = new HttpClient();
    private int resultCount = 0;
    private String SearchURL;
    private Logger logger = Logger.getLogger(this.getClass());
    private List<MESTRecord> MESTRecords = new ArrayList<MESTRecord>();
    private MESTRecord MestRecord = new MESTRecord();

    //Getters and Setters
    public MESTRecord getMestRecord() {
        return MestRecord;
    }

    public void setMestRecord(MESTRecord mestRecord) {
        MestRecord = mestRecord;
    }

    public List<MESTRecord> getMESTRecords() {
        return MESTRecords;
    }

    public void setMESTRecords(List<MESTRecord> records) {
        MESTRecords = records;
    }

    public Document getSearchFilter() {
        return SearchFilter;
    }

    public void setSearchFilter(Document searchFilter) {
        SearchFilter = searchFilter;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getSearchURL() {
        return SearchURL;
    }

    public void setSearchURL(String searchURL) {
        SearchURL = searchURL;

    }

    public Document getSearchResult() {
        return SearchResult;
    }

    public void setSearchResult(Document searchResult) {
        SearchResult = searchResult;
    }

    public String getSearchParameter() {
        return SearchParameter;
    }

    public void setSearchParameter(String searchParameter) {
        AddSearchParameter(searchParameter);
    }

    public boolean RunSearch() {
        Document d = null;


        boolean resultSuccess = false;



        SearchResult = getDocument(SearchKVP, false);



        return resultSuccess;
    }

    public void AddSearchParameter(String s) {

        try {

            String feed = SearchURL + "/xml.metadata.get?uuid=" + s;
            SearchKVP = feed;

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

    }

    @SuppressWarnings("unchecked")
    public boolean ParseResult() {

        boolean bResult = true;
        String sURL = null;
        String sProtocol = null;
        String sName = null;
        String sDesc = null;
        Node nNode = null;
        Node nOnline = null;

        Document doc = getSearchResult();

        NodeList nx = doc.getElementsByTagName("gmd:MD_DigitalTransferOptions");
        MESTRecord mr = new MESTRecord();

        for (int m = 0; m < nx.getLength(); m++) {

            NodeList nj = nx.item(m).getChildNodes();


            for (int k = 0; k < nj.getLength(); k++) {


                if (nj.item(k).getNodeName().equalsIgnoreCase("gmd:onLine")) {
                    nOnline = nj.item(k);

                    NodeList nl = nOnline.getChildNodes();
                    mr.setIdentifier(SearchParameter);

                    for (int i = 0; i < nl.getLength(); i++) {
                        nNode = nl.item(i);

                        if (nNode.getNodeName().equalsIgnoreCase("gmd:CI_OnlineResource")) {

                            NodeList nc = nNode.getChildNodes();

                            for (int x = 0; x < nc.getLength(); x++) {
                                Node xn = nc.item(x);

                                if (xn.getNodeName().equalsIgnoreCase("gmd:protocol")) {
                                    NodeList p = xn.getChildNodes();
                                    for (int j = 0; j < p.getLength(); j++) {
                                        if (p.item(j).getNodeName().equalsIgnoreCase("gco:CharacterString")) {
                                            sProtocol = p.item(j).getTextContent();

                                        }
                                    }

                                } else if (xn.getNodeName().equalsIgnoreCase("gmd:linkage")) {
                                    NodeList p = xn.getChildNodes();
                                    for (int j = 0; j < p.getLength(); j++) {
                                        if (p.item(j).getNodeName().equalsIgnoreCase("gmd:URL")) {
                                            sURL = p.item(j).getTextContent();

                                        }
                                    }

                                } else if (xn.getNodeName().equalsIgnoreCase("gmd:name")) {
                                    NodeList p = xn.getChildNodes();
                                    for (int j = 0; j < p.getLength(); j++) {
                                        if (p.item(j).getNodeName().equalsIgnoreCase("gco:CharacterString")) {
                                            sName = p.item(j).getTextContent();

                                        }
                                    }

                                } else if (xn.getNodeName().equalsIgnoreCase("gmd:description")) {
                                    NodeList p = xn.getChildNodes();
                                    for (int j = 0; j < p.getLength(); j++) {
                                        if (p.item(j).getNodeName().equalsIgnoreCase("gco:CharacterString")) {
                                            sDesc = p.item(j).getTextContent();

                                        }
                                    }

                                }



                            }



                            if (sProtocol.contains("OGC:WMS")) {
                                //it must be a WMS layer or service lets see
                                //which one

                                if (sProtocol.equalsIgnoreCase(sWebMap111)) {
                                    WMSLayer wl = new WMSLayer();
                                    wl.setServerURL(sURL);
                                    wl.setName(sName);
                                    mr.addWMSLayer(wl);
                                } else if (sProtocol.equalsIgnoreCase(sWebMap130)) {
                                    WMSLayer wl = new WMSLayer();
                                    wl.setServerURL(sURL);
                                    wl.setName(sName);
                                    mr.addWMSLayer(wl);
                                } else if (sProtocol.equalsIgnoreCase(sWebMap100)) {
                                    WMSLayer wl = new WMSLayer();
                                    wl.setServerURL(sURL);
                                    wl.setName(sName);
                                    mr.addWMSLayer(wl);
                                } else {
                                    WMSServer ws = new WMSServer();

                                    if (sProtocol.contains("1.1.1")) {
                                        ws.setVersion("1.1.1");
                                    } else if (sProtocol.contains("1.3.0")) {
                                        ws.setVersion("1.3.0");
                                    } else {
                                        ws.setVersion("1.0.0");
                                    }

                                    ws.setURL(sURL);
                                    ws.setName(sName);
                                    mr.addWMSServer(ws);
                                }


                            } else {
                                //its a link, let the browser sort it out
                                //System.out.println(sDesc);
                                // System.out.println(sURL);
                                Website ws = new Website();
                                ws.setURL(sURL);
                                ws.setDescription(sDesc);
                                mr.addWebsite(ws);
                            }

                        }
                    }

                }
            }

        }

        MestRecord = mr;

        return bResult;
    }

    public Document getDocument(String feed, boolean RemoveFluff) {
        Document d = null;
        HttpURLConnection connection = null;
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        StringBuilder sb = null;
        String line = null;

        URL serverAddress = null;
        try {

            serverAddress = new URL(feed);

            // set up out communications stuff
            connection = null;

            // Set up the initial connection
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setReadTimeout(60000);

            connection.connect();

            rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            sb = new StringBuilder();

            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }

            // want to get rid of all the superflous stuff

            String document = sb.toString();

            // now turn it into a document

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = factory.newDocumentBuilder();
            org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();

            inStream.setCharacterStream(new java.io.StringReader(document));
            d = db.parse(inStream);

            connection.disconnect();

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return d;
    }

    public static org.w3c.dom.Document loadXMLFrom(String xml)
            throws org.xml.sax.SAXException, java.io.IOException {
        return loadXMLFrom(new java.io.ByteArrayInputStream(xml.getBytes()));
    }

    public static org.w3c.dom.Document loadXMLFrom(java.io.InputStream is)
            throws org.xml.sax.SAXException, java.io.IOException {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
        }
        org.w3c.dom.Document doc = builder.parse(is);
        is.close();
        return doc;
    }

    public static String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerException ex) {
            java.util.logging.Logger.getLogger(MESTGetRecord.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
