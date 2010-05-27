/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.gazetteer;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.net.HttpConnection;

import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sf.json.JSONArray;
import org.ala.rest.GazetteerSearch;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author brendon
 * @author angus
 */
public class GazetteerSearchController extends UtilityComposer {

    private static final String GAZ_URL = "geoserver_url";

    private Session sess = (Session) Sessions.getCurrent();
    private String sSearchTerm;
    private String sFeatureType;
    private Listbox gazetteerResults;
    private HttpConnection httpConnection = null;
    private Window gazetteerSearchResults;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;
    private SettingsSupplementary settingsSupplementary = null;

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }
    //TODO get this from the config - done by Ajay

    private String gazServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"; // http://localhost:8080
    private String gazSearchURL = "/geoserver/rest/gazetteer/result.xml?q=";
    private JSONArray arr = null;
    private GazetteerSearch gsr = new GazetteerSearch();

    @Override
    public void afterCompose() {
        super.afterCompose();
        if (settingsSupplementary != null) {
            gazServer = settingsSupplementary.getValue(GAZ_URL);
        }
        sSearchTerm = (String) sess.getAttribute("searchGazetteerTerm");
        // searchGazetteer(getURI());
        renderGazetteerResults();
    }

//    public void searchGazetteer(String p) {
//        InputStream in = null;
//        String json = null;
//        URLConnection connection = null;
//
//        try {
//
//            connection = httpConnection.configureURLConnection(p);
//            in = connection.getInputStream();
//            json = IOUtils.toString(in);
//            parseGazetteerSearchResult(json);
//
//
//
//        } catch (IOException iox) {
//            logger.debug(iox.toString());
//        }
//
//    }
    private String getURI() {
        String uri;


        // add the search term to the uri and append the required json to it
        uri = gazServer + gazSearchURL + forURL(sSearchTerm);

        return uri;
    }

    /**
     * string checking code, for spaces and special characters
     * @param aURLFragment
     * @return String
     */
    public static String forURL(String aURLFragment) {
        String result = null;
        try {
            result = URLEncoder.encode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    /***
     * Renders the search results, does not load into a special object, just reads straight from xml
     */
    public void renderGazetteerResults() {

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        try {
            //Read in the xml response
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            String uri = getURI();
            //Messagebox.show(uri);
            Document resultDoc = builder.parse(uri);

            //Get a list of names,descriptions,states and links from the xml
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression namesExpr = xpath.compile("//search/results/result/name/text()");
            XPathExpression descriptionsExpr = xpath.compile("//search/results/result/description/text()");
            // XPathExpression statesExpr = xpath.compile("//search/results/result/state/text()");
            XPathExpression linksExpr = xpath.compile("//search/results/result/@*");

            NodeList names = (NodeList) namesExpr.evaluate(resultDoc, XPathConstants.NODESET);
            NodeList descriptions = (NodeList) descriptionsExpr.evaluate(resultDoc, XPathConstants.NODESET);
            //  NodeList states = (NodeList) statesExpr.evaluate(resultDoc, XPathConstants.NODESET);
            NodeList links = (NodeList) linksExpr.evaluate(resultDoc, XPathConstants.NODESET);

            for (int i = 0; i < names.getLength(); i++) {
                String name = (String) names.item(i).getNodeValue();
                String description = (String) descriptions.item(i).getNodeValue();
                //  String state = (String) states.item(i).getNodeValue();
                String link = (String) links.item(i).getNodeValue();

                Listitem li = new Listitem();
                Listcell lc = new Listcell();

                //Add hyperlink to the geojson feature resource
                Toolbarbutton tbFull = new Toolbarbutton();
                tbFull.setLabel(name);
                tbFull.setSclass("z-label");
                tbFull.setHref(link);
                tbFull.setTarget("_blank");

                lc.appendChild(tbFull);
                li.appendChild(lc);


                lc = new Listcell();
                Label lb = new Label();
                lb.setValue(description);
                lc.appendChild(lb);
                li.appendChild(lc);

                //Add State
                lc = new Listcell();
                lb = new Label();
                //   lb.setValue(state);
                lc.appendChild(lb);
                li.appendChild(lc);


                //Add to  map button
                lc = new Listcell();
                Button btn = new Button();
                btn.setLabel("Add to map");
                btn.addEventListener("onClick", new myOnClickEventListener());
                btn.setId(name);
                Label ln = new Label();
                ln.setValue(link);
                ln.setVisible(false);
                ln.setId("ln" + name);
                lc.appendChild(btn);
                lc.appendChild(ln);
                li.appendChild(lc);

                // add the row to the listbox
                gazetteerResults.appendChild(li);


            }

            gazetteerResults.setVisible(true);
        } catch (Exception e) {
        }

    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = gazetteerSearchResults.getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    public final class myOnClickEventListener implements EventListener {

        @Override
        public void onEvent(Event event) throws Exception {
            String label = null;
            String uri = null;
            String filter = null;
            String entity = null;
            MapLayer mapLayer = null;

            //get the entity value from the button id
            entity = event.getTarget().getId();
            Label ln = (Label) event.getTarget().getFellow("ln" + entity);
            label = ln.getValue();

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

             logger.debug(gazServer + label);

            //add feature to the map as a new layer
            mapLayer = mc.addGeoJSON(entity, gazServer + label);


//            //zoom to the feature/layer
//            if (mapLayer != null) {
//                mc.getOpenLayersJavascript().zoomGeoJsonExtent(mapLayer);
//            }

            
        }
    }
}

