package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.GeoJSONUtilities;
import au.org.emii.portal.value.BoundingBox;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLWriter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.String;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

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
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.GazetteerPointSearch;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;


import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import org.zkoss.zul.Listbox;


import org.zkoss.zul.Popup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Separator;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;



/**
 *
 * @author Angus
 */
public class SelectionController extends UtilityComposer {

    private static final String DEFAULT_AREA = "CURRENTVIEW()";
    private Textbox searchPoint;
    private Textbox selectionGeom;
    private Textbox boxGeom;
    private Textbox displayGeom;
    private Div polygonInfo;
    private Div envelopeInfo;
    //private Label instructions;
    public Div areaInfo;
    public Button download;
    public Button zoomtoextent;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Button results_prev;
    public Button results_next;
    public Label results_label;
    public Label popup_label;
    public Radio rdoEnvironmentalEnvelope;
    private Radiogroup rgAreaSelection;
    HtmlMacroComponent envelopeWindow;
    private SettingsSupplementary settingsSupplementary = null;
    private String geoServer;
    String satServer;
    String[] results = null;
    int results_pos;
    SortedSet speciesSet;
    Combobox cbAreaSelection;
    Comboitem ciBoundingBox;
    Comboitem ciPolygon;
    Comboitem ciMapPolygon;
    Comboitem ciEnvironmentalEnvelope;
    Comboitem ciBoxAustralia;
    Comboitem ciBoxWorld;
    Comboitem ciBoxCurrentView;
    Comboitem ciPointAndRadius;
    Comboitem lastTool;
    Window wInstructions = null;

    public String getGeom() {
        if (displayGeom.getText().contains("ENVELOPE(")) {
            //get PID and return as ENVELOPE(PID)
            String envPid = ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).getPid();
            if (envPid.length() > 0) {
                return "ENVELOPE(" + envPid + ")";
            }
            
            //work around for null polygons to be reported as absence of polygon
        } else if (!displayGeom.getText().contains("CURRENTVIEW()")
                && !displayGeom.getText().contains("NaN NaN")
                && displayGeom.getText().length() > 0) {
            return displayGeom.getText();
        }

        //default to: current view to be dynamically returned on usage
        return getMapComposer().getViewArea();

    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        cbAreaSelection.setSelectedItem(ciBoxCurrentView);
        displayGeom.setValue(DEFAULT_AREA);
    }

    void setInstructions(String toolname, String [] text) {
        if (wInstructions != null) {
            wInstructions.detach();            
        }

        if (text != null && text.length > 0) {
            wInstructions = new Window(toolname, "normal", false);
            wInstructions.setWidth("500px");
            wInstructions.setClosable(false);

            Vbox vbox = new Vbox();
            vbox.setParent(wInstructions);
            for(int i=0;i<text.length;i++){
                Label l = new Label((i+1) + ". " + text[i]);
                l.setParent(vbox);
                l.setMultiline(true);
                l.setSclass("word-wrap");
                l.setStyle("white-space: normal; padding: 5px");
            }

            (new Separator()).setParent(vbox);

            Button b = new Button("Cancel Map Tool");
            b.setParent(vbox);
            b.addEventListener("onClick", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onClick$btnClearSelection(null);
                    wInstructions.detach();
                }
            });

            wInstructions.setParent(getMapComposer().getFellow("mapIframe").getParent());
            wInstructions.setClosable(true);
            wInstructions.doOverlapped();
            wInstructions.setPosition("top,center");
        }
    }

    public void onClick$zoomtoextent(Event event) {
        MapLayer ml = getMapComposer().getMapLayer("Active Area");
        if (ml != null) {
            getMapComposer().zoomToExtent(ml);
        }
    }

    public void onClick$btnClearSelection(Event event) {
        hideAllInfo();
        displayGeom.setValue(DEFAULT_AREA);
        //rgAreaSelection.getSelectedItem().setSelected(false);
        MapComposer mc = getThisMapComposer();
        //  mc.getOpenLayersJavascript().removeAreaSelection();
        displayGeom.setValue(DEFAULT_AREA);

        String script = removeCurrentSelection();

        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Active Area"));

        cbAreaSelection.setText("Box - Current View");
        updateSpeciesList(false);

        lastTool = null;
    }

    public void onOpen$cbAreaSelection() {
        setInstructions(null, null);
        hideAllInfo();
        displayGeom.setValue(DEFAULT_AREA);
        String script = removeCurrentSelection();
        MapComposer mc = getThisMapComposer();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Active Area"));
        lastTool = null;
    }



    public void onChange$cbAreaSelection() {
        lastTool = cbAreaSelection.getSelectedItem();
        System.out.println("cbAreaSelection: " + cbAreaSelection.getSelectedItem().getLabel());
        
        String wkt;

        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            cbAreaSelection.setText("Drawing bounding box");
            String [] text = {
                 "Zoom and pan to the area of interest.",
                 "Using the mouse, position the cursor over the area of interest and hold down the left mouse button and drag a rectangle to the required shape and size.",
                 "Release the mouse button."
                };
            setInstructions("Active Map Tool: Draw bounding box...",text);
            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();
            //mc.getOpenLayersJavascript().addBoxDrawingTool();
            script += mc.getOpenLayersJavascript().addBoxDrawingTool();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            cbAreaSelection.setText("Drawing polygon");
            String [] text = { "Zoom and pan to the area of interest",
                   "Using the mouse, position the cursor at the first point to be digitized and click the left mouse button.",
                   "Move the cursor to the second vertext of the polygon and click the mouse button. Repeat as required to define the area.",
                   "On the last vertex, double click to finalise the polygon."
                };
            setInstructions("Active Map Tool: Draw polygon", text);
            showPolygonInfo();
            MapComposer mc = getThisMapComposer();
            String script = removeCurrentSelection();
            script += mc.getOpenLayersJavascript().addPolygonDrawingTool();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);

            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciPointAndRadius) {
            String [] text = {
                "Zoom and pan to the area of interest.",
                "With the mouse, place the cursor over the centre point of the area of interest.",
                "Hold down the (left) mouse button and drag the radius to define the area of interest.",
                "Release the mouse button."
                };
            cbAreaSelection.setText("Drawing point and radius");
            setInstructions("Active Map Tool: Draw point and radius...", text);

            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();
            script += mc.getOpenLayersJavascript().addRadiusDrawingTool();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            cbAreaSelection.setText("Selecting map polygon");
            String [] text = {
                "Zoom and pan to the area of interest.",
                "Identify the polygon of interest by a (left) mouse click within that polygon.",
                "The area selected will be highlighted red."
            };
            setInstructions("Active Map Tool: Select predefined (displayed) map polygon...", text);
            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();
            script += mc.getOpenLayersJavascript().addFeatureSelectionTool();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciEnvironmentalEnvelope) {
            setInstructions(null, null);
            cbAreaSelection.setText("Defining environmental envelope");
            Events.echoEvent("showEnvelopeInfoEvent", this, null);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxAustralia) {
            setInstructions(null, null);
            showPolygonInfo();
            wkt = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            displayGeom.setValue(wkt);

            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, "Active Area");
            updateSpeciesList(false);
        } else if (cbAreaSelection.getSelectedItem() == ciBoxWorld) {
            setInstructions(null, null);
            showPolygonInfo();
            wkt = "POLYGON((-180 -180,-180 180.0,180.0 180.0,180.0 -180.0,-180.0 -180.0))";
            displayGeom.setValue(wkt);

            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, "Active Area");
            updateSpeciesList(false);
        } else { //if (cbAreaSelection.getSelectedItem() == ciBoxCurrentView) {
            cbAreaSelection.setText("Box - Current View");

            setInstructions(null, null);
            showPolygonInfo();

            /* current view to be re-calculated on use
            BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();

            wkt = "POLYGON(("
            + bb.getMinLongitude() + " " + bb.getMinLatitude() + ","
            + bb.getMinLongitude() + " " + bb.getMaxLatitude() + ","
            + bb.getMaxLongitude() + " " + bb.getMaxLatitude() + ","
            + bb.getMaxLongitude() + " " + bb.getMinLatitude() + ","
            + bb.getMinLongitude() + " " + bb.getMinLatitude() + "))";
            
            displayGeom.setValue(wkt);
            MapLayer mapLayer = getMapComposer().addWKTLayer(displayGeom.getValue(),"Active Area"); */
            wkt = "CURRENTVIEW()";
            displayGeom.setValue(wkt);

            updateSpeciesList(false);
        }
    }

    public void showEnvelopeInfoEvent(Event e) throws Exception {
        showEnvelopeInfo();
    }

    void showEnvelopeInfo() {
        areaInfo.setVisible(true);
        envelopeInfo.setVisible(true);
        polygonInfo.setVisible(false);
    }

    void showPolygonInfo() {
        areaInfo.setVisible(true);
        envelopeInfo.setVisible(false);
        polygonInfo.setVisible(true);
    }

    void hideAllInfo() {
        areaInfo.setVisible(false);
    }

    private String removeCurrentSelection() {
        MapComposer mc = getThisMapComposer();
        MapLayer selectionLayer = mc.getMapLayer("Active Area");

        if (mc.safeToPerformMapAction()) {
            if ((selectionLayer != null)) {
                //  selectionLayer.setDisplayed(false);
                //selectionLayer.
                System.out.println("removing Active Area layer");
                //mc.deactiveLayer(selectionLayer, true,false);
                return mc.getOpenLayersJavascript().removeMapLayer(selectionLayer);
                //mc.getOpenLayersJavascript().execute(script);
                //mc.removeLayer("Area Selection");
            } else {
                return "";
            }
        } else {
            try {
                Messagebox.show("Not Safe?");
            } catch (Exception e) {
            }
            return "Failed";
        }

    }

    /**
     * Searches the gazetter at a given point and then maps the polygon feature 
     * found at the location (for the current top contextual layer).
     * @param event triggered by the usual javascript trickery
     */
    public void onChange$searchPoint(Event event) {
            String lon = searchPoint.getValue().split(",")[0];
            String lat = searchPoint.getValue().split(",")[1];
            Object llist = Sessions.getCurrent().getAttribute("layerlist");
            JSONArray layerlist = JSONArray.fromObject(llist);
            MapComposer mc = getThisMapComposer();

            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);
                if (jo != null && jo.getString("type") != null && jo.getString("type").equalsIgnoreCase("contextual")) {
                    System.out.println("********" + jo.getString("name"));
                    if (mc.getMapLayer(jo.getString("displayname")) != null) {
                        String featureURI = GazetteerPointSearch.PointSearch(lon, lat, jo.getString("name"), geoServer);
                        //add feature to the map as a new layer

                        String json = readGeoJSON(featureURI);
                        String wkt = wktFromJSON(json);
                        if (wkt.contentEquals("none"))
                            break;
                        else {
                            displayGeom.setValue(wkt);
                            MapLayer mapLayer = mc.addWKTLayer(wkt,"Active Area");
                            break;
                        }
                    }
                }
            }
    }

    private String wktFromJSON(String json) {
        try {
        JSONObject obj = JSONObject.fromObject(json);

         String coords = obj.getJSONArray("geometries").getJSONObject(0).getString("coordinates");
        
        String wkt = coords.replace("],[", "*").replace(",", " ").replace("*",",").replace("[[[[", "POLYGON((").replace("]]]]","))");
        return wkt;
        }
        catch (JSONException e) {
            return "none";
        }
    }

    private String readGeoJSON(String feature) {
      StringBuffer content = new StringBuffer();

		try {
	        // Construct data

	        // Send data
	        URL url = new URL(feature);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	              conn.connect();

	        // Get the response
	        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;
	        while ((line = rd.readLine()) != null) {
	           content.append(line);
	    }
		conn.disconnect();
	  } catch (Exception e) {    }
          return content.toString();
	}



    /**
     * 
     * @param event
     */
    public void onChange$selectionGeom(Event event) {
        System.out.println("onchange$selectiongeom");
        setInstructions(null, null);
        try {
            if (selectionGeom.getValue().contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
                lastTool = null;
            } else {
                displayGeom.setValue(selectionGeom.getValue());
            }
            updateComboBoxText();
            updateSpeciesList(false); // true

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            MapLayer mapLayer = mc.addWKTLayer(selectionGeom.getValue(), "Active Area");
            rgAreaSelection.getSelectedItem().setChecked(false);
            
        } catch (Exception e) {//FIXME
        }

    }

    void updateComboBoxText() {
        String txt = "";
        if (lastTool == ciBoundingBox) {
            txt = "Got user drawn box";
        } else if (lastTool == ciPointAndRadius) {
            txt = "Got user drawn point and radius";
        } else if (lastTool == ciPolygon) {
            txt = "Got user drawn polygon";
        } else if (lastTool == ciMapPolygon) {
            txt = "Got mapped polygon";
        } else if (lastTool == ciEnvironmentalEnvelope) {
            txt = "Got environmental envelope";
        } else if (lastTool == ciBoxAustralia) {
            txt = "Got Australia Box";
        } else if (lastTool == ciBoxWorld) {
            txt = "Got World Box";
        } else {
            txt = "Using current view extents";
        }
        cbAreaSelection.setText(txt);
    }

    public void onChange$boxGeom(Event event) {
        setInstructions(null, null);
        try {

            if (boxGeom.getValue().contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
                lastTool = null;
            } else {
                displayGeom.setValue(boxGeom.getValue());                
            }
            updateComboBoxText();
            updateSpeciesList(false); // true


            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            //mc.removeLayer("Area Selection");
            //mc.deactiveLayer(mc.getMapLayer("Area Selection"), true,true);
            MapLayer mapLayer = mc.addWKTLayer(boxGeom.getValue(), "Active Area");

            rgAreaSelection.getSelectedItem().setChecked(false);

            //wfsQueryBBox(boxGeom.getValue());

            
        } catch (Exception e) {//FIXME
        }

    }

    public void onClick$btnShowSpecies() {
        openResults();
        /*
        if (selectionGeom.getValue() != ""
        && !selectionGeom.getValue().contains("NaN NaN")) {
        Clients.showBusy("Filtering species, please wait...", true);
        Events.echoEvent("showSpeciesPoly", this, displayGeom.getValue());
        }
        else if(boxGeom.getValue() != ""
        && !boxGeom.getValue().contains("NaN NaN")) {
        Clients.showBusy("Filtering species, please wait...", true);
        Events.echoEvent("showSpecies", this, displayGeom.getValue());
        }*/
    }

    public void showSpecies(Event event) throws Exception {
        String geomData = (String) event.getData();
        wfsQueryBBox(geomData);
//        Clients.showBusy("", false);
    }

    public void showSpeciesPoly(Event event) throws Exception {
        String geomData = (String) event.getData();
        wfsQueryPolygon(geomData);
  //      Clients.showBusy("", false);
    }

    /**
     * Constructs a wfs 'species within bounding box query' for the given geometry
     * @param selectionWKT geometry of the box
     */
    public void wfsQueryPolygon(String selectionWKT) {
        WKTReader wkt_reader = new WKTReader();
        GMLWriter gml_writer = new GMLWriter();
        WKTWriter wkt_writer = new WKTWriter();
        String baseQueryXML = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName><ogc:Filter><ogc:BBOX><ogc:PropertyName>the_geom</ogc:PropertyName><gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\"><gml:lowerCorner>LOWERCORNER</gml:lowerCorner><gml:upperCorner>UPPERCORNER</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";
        if (selectionWKT.contains("MULTIPOLYGON")) {
            selectionWKT.replace("MULTI", "");
            selectionWKT.replace("(((", "((");
            selectionWKT.replace(")))", "))");
        }
        try {
            Geometry selection = wkt_reader.read(selectionWKT);
            //Envelope selectionBounds = selection.getEnvelope().getEnvelopeInternal();
            Geometry selectionBounds = selection.getEnvelope();
            String selectionBoundsWKT = wkt_writer.write(selectionBounds);
            selectionBoundsWKT = selectionBoundsWKT.replace("(", "");
            selectionBoundsWKT = selectionBoundsWKT.replace(")", "");
            String upperCorner = selectionBoundsWKT.replace("POLYGON", "").split(",")[3].toString();
            String lowerCorner = selectionBoundsWKT.replace("POLYGON", "").split(",")[1].toString();
            baseQueryXML = baseQueryXML.replace("UPPERCORNER", upperCorner);
            baseQueryXML = baseQueryXML.replace("LOWERCORNER", lowerCorner);
            //String selectionBoundsGML = gml_writer.toString(selectionBounds.)
            // Messagebox.show(baseQueryXML);
            String response = POSTRequest(baseQueryXML);
            String speciesList = parseIntersection(response, selection);
//            baseQueryXML = baseQueryXML.replace("ENVELOPE",selectionBoundsGML);
//            String response = POSTRequest(baseQueryXML);
        } catch (Exception e) {
        }
//        String baseQueryXML = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName>";
//        String filterPart = "<ogc:Filter><Intersects><PropertyName>the_geom</PropertyName><gml:Polygon srsName=\"EPSG:4326\"><gml:outerBoundaryIs><gml:LinearRing><gml:coordinates cs=\" \" decimal=\".\" ts=\",\">COORDINATES</gml:coordinates></gml:LinearRing></gml:outerBoundaryIs></gml:Polygon></Intersects></ogc:Filter></wfs:Query></wfs:GetFeature>";
//        String coordinateString = selectionGeom.replace("POLYGON", "").replace(")", "").replace("(", "");
//        String request = baseQueryXML + filterPart.replace("COORDINATES", coordinateString);
//        try {
//            Messagebox.show(request);
//            String response = POSTRequest(request);
//            // Messagebox.show(response);
//
//        } catch (Exception e) { //FIXME
//        }
    }

    /**
     * Constructs a wfs 'species within bounding box query' for the given geometry
     * @param selectionGeom geometry of the box
     */
    public void wfsQueryBBox(String selectionGeom) {
        String baseQueryXML = "<wfs:GetFeature  service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName><ogc:Filter><ogc:BBOX><ogc:PropertyName>the_geom</ogc:PropertyName><gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\"><gml:lowerCorner>LOWERCORNER</gml:lowerCorner><gml:upperCorner>UPPERCORNER</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";
        selectionGeom = selectionGeom.replace("(", "");
        selectionGeom = selectionGeom.replace(")", "");
        String upperCorner = selectionGeom.replace("POLYGON", "").split(",")[3].toString();
        String lowerCorner = selectionGeom.replace("POLYGON", "").split(",")[1].toString();
        baseQueryXML = baseQueryXML.replace("UPPERCORNER", upperCorner);
        baseQueryXML = baseQueryXML.replace("LOWERCORNER", lowerCorner);

        //       String completeQuery = baseQuery + selection + ")";
        try {

            String response = POSTRequest(baseQueryXML);
            String speciesList = parse(response);
            //Messagebox.show(response);

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
            URL url = new URL(geoServer + "/geoserver/wfs?");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/xml");

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(query);
            wr.flush();
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String response = "";
            while ((line = rd.readLine()) != null) {
                response += line;
            }
            //Parse the response
            //   Messagebox.show(response);
            // String speciesList = parse(response);
            wr.close();
            // rd.close();
            return response;
        } catch (Exception e) {
            return e.getMessage();
        }

    }

    /**
     * Takes a WFS response and returns species list
     * @param responseXML The response of an WFS query.
     * @return
     */
    public String parse(String responseXML) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, InterruptedException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(false);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new java.io.ByteArrayInputStream(responseXML.getBytes());
        Document resultDoc = builder.parse(is);
        //Get a list of names
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        //Ugly xpath expression - using local-name() so that I can ignore namespaces
        XPathExpression speciesExpr = xpath.compile("//*[local-name()='FeatureCollection']//*[local-name()='featureMembers']//*[local-name()='occurrences']//*[local-name()='scientificname']/text()");

        NodeList species = (NodeList) speciesExpr.evaluate(resultDoc, XPathConstants.NODESET);
        speciesSet = new TreeSet();
        for (int i = 0; i < species.getLength(); i++) {
            speciesSet.add((String) species.item(i).getNodeValue());
        }


        results = (String[]) speciesSet.toArray(new String[speciesSet.size()]);
        popup_listbox_results.setModel(new SimpleListModel(speciesSet.toArray()));


        popup_label.setValue("Number of species: " + speciesSet.size());

        //popup_results.open(40, 150);
        openResults();

        return String.valueOf(speciesSet.size());
    }

    public String parseIntersection(String responseXML, Geometry selection) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, InterruptedException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(false);
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
        XPathExpression speciesExpr = xpath.compile("//*[local-name()='FeatureCollection']//*[local-name()='featureMembers']//*[local-name()='occurrences']//*[local-name()='scientificname']/text()");

        NodeList species = (NodeList) speciesExpr.evaluate(resultDoc, XPathConstants.NODESET);
        speciesSet = new TreeSet();
        for (int i = 0; i < species.getLength(); i++) {
            speciesSet.add((String) species.item(i).getNodeValue());
        }


        results = (String[]) speciesSet.toArray(new String[speciesSet.size()]);
        popup_listbox_results.setModel(new SimpleListModel(speciesSet.toArray()));


        popup_label.setValue("Number of species: " + speciesSet.size());

        //popup_results.open(40, 150);
        openResults();

        return String.valueOf(speciesSet.size());
    }

    void openResults() {
        java.util.Map args = new java.util.HashMap();
        args.put("pid", "none");
        args.put("shape", getGeom());

        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
        try {            
            win.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    
    private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: test results
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    /**
     * updates species list analysis tab with refreshCount
     */
    void updateSpeciesList(boolean populateSpeciesList) {
        try {
            FilteringResultsWCController win =
                    (FilteringResultsWCController) getMapComposer()
                        .getFellow("leftMenuAnalysis")
                            .getFellow("analysiswindow")
                                .getFellow("speciesListForm")
                                    .getFellow("popup_results");
            //if (!populateSpeciesList) {
                win.refreshCount();
            //} else {
            //    win.onClick$refreshButton2();
           // }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void onEnvelopeDone(boolean hide) {
        try {
            String envPid = ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).getPid();
            
            if (envPid.length() > 0) {
                displayGeom.setText("ENVELOPE(" + envPid + ")");
                updateComboBoxText();
            } else if(hide) {
                onClick$btnClearSelection(null);                
                return;
            }

            if (hide) {
                hideAllInfo();
            }
            updateSpeciesList(false); // true
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        setInstructions(null, null);
    }

    public void checkForAreaRemoval(){
        MapLayer ml = getMapComposer().getMapLayer("Active Area");
        if(ml == null && cbAreaSelection.getSelectedItem() != ciBoxCurrentView){
            onClick$btnClearSelection(null);
            if(wInstructions != null){
                wInstructions.detach();
            }
        }
    }
}
