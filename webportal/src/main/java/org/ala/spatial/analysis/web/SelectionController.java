package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.String;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.zkoss.zhtml.Messagebox;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;

import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;

import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Listbox;


import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;


/**
 *
 * @author Angus
 */
public class SelectionController extends UtilityComposer {

    private Textbox selectionGeom;
    public Button download;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Button results_prev;
    public Button results_next;
    public Label results_label;
    String[] results = null;
    int results_pos;
    SortedSet speciesSet;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    /**
     * Activate the polygon selection tool on the map
     * @param event
     */
    public void onClick$btnPolygonSelection(Event event) {
        MapComposer mc = getThisMapComposer();

        mc.getOpenLayersJavascript().addPolygonDrawingTool();

    }

    /**
     * Activate the box selection tool on the map
     * @param event
     */
    public void onClick$btnBoxSelection(Event event) {
        MapComposer mc = getThisMapComposer();

        mc.getOpenLayersJavascript().addBoxDrawingTool();

    }

    /**
     * 
     * @param event
     */
    public void onChange$selectionGeom(Event event) {
        try {
            //Messagebox.show(selectionGeom.getValue());
            wfsQuerySelection(selectionGeom.getValue());
            
        } catch (Exception e) {//FIXME
        }

    }

    public void wfsQuerySelection(String selectionGeom) {
        String baseQueryXML = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrencesv1\"> <wfs:PropertyName>ALA:species</wfs:PropertyName><ogc:Filter><ogc:BBOX><ogc:PropertyName>the_geom</ogc:PropertyName><gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\"><gml:lowerCorner>LOWERCORNER</gml:lowerCorner><gml:upperCorner>UPPERCORNER</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";
        String upperCorner = selectionGeom.replace("POLYGON(","").replace(")","").split(",")[3].toString();
        String lowerCorner = selectionGeom.replace("POLYGON(","").replace("(","").split(",")[1].toString(); 
        baseQueryXML = baseQueryXML.replace("UPPERCORNER", upperCorner);
        baseQueryXML = baseQueryXML.replace("LOWERCORNER", lowerCorner);
        
        //       String completeQuery = baseQuery + selection + ")";
        try {
           
            String response = POSTRequest(baseQueryXML);
           // Messagebox.show(response);
         
        } catch (Exception e) { //FIXME
        }
    }

    /**
     * Sends and POST to the wfs
     * @param query
     * @return
     */
    public String POSTRequest(String query) {
        try {
            // Construct data
            //String data = URLEncoder.encode(query, "UTF-8");// + "=" + URLEncoder.encode(query, "UTF-8");
            

            // Send data 
            URL url = new URL("http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com/geoserver/wfs?");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput (true);
            conn.setUseCaches (false);
            conn.setRequestProperty("Content-Type", "application/xml");

            DataOutputStream wr = new DataOutputStream (conn.getOutputStream());
            wr.writeBytes(query);
            wr.flush();
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String response = "";
            while ((line = rd.readLine()) != null) {
                response+=line;
            }
            //Parse the response
            String speciesList = parse(response);
            wr.close();
           // rd.close();
            return speciesList;
        } catch (Exception e) {
            return e.getMessage();
        }

    }

    /**
     * Takes a WFS response and returns species list
     * @param xmlResponse The response of an WFS query.
     * @return
     */
    public String parse(String responseXML) throws ParserConfigurationException, XPathExpressionException,SAXException, IOException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        
         DocumentBuilder builder = domFactory.newDocumentBuilder();
         
        InputStream is = new java.io.ByteArrayInputStream(responseXML.getBytes());
        
        Document resultDoc = builder.parse(is);

//        try {
//        Messagebox.show(responseXML);
//        } catch (Exception e) {}
        //Get a list of names
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        //Ugly xpath expression - using local-name() so that I can ignore namespaces
        XPathExpression speciesExpr = xpath.compile("//*[local-name()='FeatureCollection']//*[local-name()='featureMembers']//*[local-name()='occurrencesv1']//*[local-name()='species']/text()");

        NodeList species = (NodeList) speciesExpr.evaluate(resultDoc, XPathConstants.NODESET);
        speciesSet = new TreeSet();
        for (int i = 0; i < species.getLength(); i++) {
                 speciesSet.add((String) species.item(i).getNodeValue());
        }

        results = (String[])speciesSet.toArray(new String[speciesSet.size()]);
        popup_listbox_results.setModel(new SimpleListModel(speciesSet.toArray()));
        popup_results.open(30, 30);
//        for(Object speciesName : speciesSet) {
//            Listitem li = new Listitem();
//            Listcell lc = new Listcell();
//            lc.setLabel((String)speciesName);
//            lc.setParent(li);
//            popup_listbox_results.addItemToSelection(li);
//        }
        return String.valueOf(speciesSet.size());
        
        

    }

    /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

//    /**
//     *
//     * @param cmdId
//     * @return
//     */
//    public Command getCommand(String cmdId) {
//        if (cmdId.equals("onNotifyServer")) {
//            return new CustomCommand(cmdId);
//        }
//        return super.getCommand(cmdId);
//    }
//
//    private class CustomCommand extends ComponentCommand {
//
//        public CustomCommand(String command) {
//            super(command,
//                    Command.SKIP_IF_EVER_ERROR | Command.CTRL_GROUP);
//        }
//
//        protected void process(AuRequest request) {
//            Events.postEvent(new Event(request.getCommand().getId(), request.getComponent(), request.getData()));
//        }
//    }
    public void onClick$download() {
//		SPLFilter [] layer_filters = getSelectedFilters();
//		if(layer_filters != null){
			StringBuffer sb = new StringBuffer();
			for(String s : results){
				sb.append(s);
				sb.append("\r\n");
			}
			org.zkoss.zhtml.Filedownload.save(sb.toString(),"text/plain","species.csv");
//		}else{

    }
    

//    public void onClick$results_prev(Event event) {
//        if (results_pos == 0) {
//            return;
//        }
//
//        seekToResultsPosition(results_pos - 15);
//    }
//
//    public void onClick$results_next(Event event) {
//        if (results_pos + 15 >= results.length) {
//            return;
//        }
//
//        seekToResultsPosition(results_pos + 15);
//    }
//
//    public void onChanging$popup_results_seek(InputEvent event) {
//        //seek results list
//        String search_for = event.getValue();
//        //if(search_for.length() > 1){
//        //	search_for = search_for.substring(0,1).toUpperCase() + search_for.substring(1,search_for.length()).toLowerCase();
//		/*}else*/
//        if (search_for.length() > 0) {
//            search_for = search_for.toLowerCase();
//        }
//
//        int pos = java.util.Arrays.binarySearch(results, search_for);
//        System.out.println("seek to: " + pos + " " + search_for);
//        if (pos < 0) {
//            pos = (pos * -1) - 1;
//        }
//        seekToResultsPosition(pos);
//    }
//
//    void seekToResultsPosition(int newpos) {
//        results_pos = newpos;
//
//        if (results_pos < 0) {
//            results_pos = 0;
//        }
//        if (results_pos >= results.length) {
//            results_pos = results.length - 1;
//        }
//
//        int sz = results_pos + 15;
//        if (results.length < sz) {
//            sz = results.length;
//        }
//
//        String[] list = new String[sz - results_pos];
//        int i;
//        for (i = results_pos; i < sz; i++) {
//            list[i - results_pos] = results[i];
//        }
//
//        ListModelArray slm = new ListModelArray(list, true);
//
//        popup_listbox_results.setModel(slm);
//        results_label.setValue(results_pos + " to " + (sz) + " of " + results.length);
//    }
}
