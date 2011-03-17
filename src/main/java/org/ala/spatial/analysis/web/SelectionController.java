package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLWriter;
import geo.google.GeoAddressStandardizer;
import geo.google.datamodel.GeoAddress;
import geo.google.datamodel.GeoCoordinate;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.httpclient.methods.PostMethod;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
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
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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
import java.io.File;
import java.io.StringReader;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ShapefileReader;
import org.ala.spatial.util.Zipper;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Fileupload;

/**
 *
 * @author Angus
 */
public class SelectionController extends UtilityComposer {

    private static final String DEFAULT_AREA = "CURRENTVIEW()";
    private String searchPoint;
    private String searchSpeciesPoint;
    private String selectionGeom;
    private String boxGeom;
    private Textbox addressBox;
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
    private Label addressLabel;
    public Radio rdoEnvironmentalEnvelope;
    private Fileupload fileUpload;
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
    Comboitem ciUploadShapefile;
    Comboitem ciAddressRadiusSelection;
    Comboitem ciMapPolygon;
    Comboitem ciEnvironmentalEnvelope;
    Comboitem ciBoxAustralia;
    Comboitem ciBoxWorld;
    Comboitem ciBoxCurrentView;
    Comboitem ciPointAndRadius;
    Comboitem lastTool;
    Combobox cbRadius;
    Comboitem ci1km;
    Comboitem ci5km;
    Comboitem ci10km;
    Comboitem ci20km;
    //displays area of active area
    //Label lblArea; //moved to FilteringResultsWCController
    private String storedSize;
    boolean viewportListenerAdded = false;
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

    void setInstructions(String toolname, String[] text) {
        if (wInstructions != null) {
            wInstructions.detach();
        }

        if (text != null && text.length > 0 && toolname.contains("address")) {
            wInstructions = new Window(toolname, "normal", false);
            wInstructions.setWidth("500px");
            wInstructions.setClosable(false);

            Vbox vbox = new Vbox();
            vbox.setParent(wInstructions);
            // for (int i = 0; i < text.length; i++) {
            Label l1 = new Label((1) + ". " + text[0]);
            l1.setParent(vbox);
            l1.setMultiline(true);
            l1.setSclass("word-wrap");
            l1.setStyle("white-space: normal; padding: 5px");
            // }

            Hbox hbxAddress = new Hbox();
            hbxAddress.setParent(vbox);

            (new Separator()).setParent(vbox);
            addressBox = new Textbox();
            addressBox.setTooltiptext("eg. Black Mountain, Canberra");
            addressBox.setWidth("95%");
            addressBox.setParent(hbxAddress);

            Button btnFind = new Button("Find");
            btnFind.setParent(hbxAddress);
            btnFind.addEventListener("onClick", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onClick$btnFindAddress(null);

                }
            });

            addressLabel = new Label();
            addressLabel.setParent(vbox);

            Label l2 = new Label((2) + ". " + text[1]);
            l2.setParent(vbox);
            l2.setMultiline(true);
            l2.setSclass("word-wrap");
            l2.setStyle("white-space: normal; padding: 5px");

            cbRadius = new Combobox();
            cbRadius.setParent(vbox);
            ci1km = new Comboitem("1km radius");
            ci1km.setParent(cbRadius);
            ci5km = new Comboitem("5km radius");
            ci5km.setParent(cbRadius);
            ci10km = new Comboitem("10km radius");
            ci10km.setParent(cbRadius);
            ci20km = new Comboitem("20km radius");
            ci20km.setParent(cbRadius);
            cbRadius.setSelectedItem(ci1km);

            Hbox hbox = new Hbox();
            hbox.setParent(vbox);

            Button btnCreate = new Button("Create radius area");
            btnCreate.setParent(hbox);
            btnCreate.addEventListener("onClick", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onClick$btnRadiusFromAddress(null);
                    wInstructions.detach();
                }
            });

            Button b = new Button("Cancel Map Tool");
            b.setParent(hbox);
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

            return;
        }

        if (text != null && text.length > 0 && toolname.toLowerCase().contains("shapefile")) {
            wInstructions = new Window(toolname, "normal", false);
            wInstructions.setWidth("500px");
            wInstructions.setClosable(false);

            Vbox vbox = new Vbox();
            vbox.setParent(wInstructions);
            //  for (int i = 0; i < text.length; i++) {
            Label l1 = new Label((1) + ". " + text[0]);
            l1.setParent(vbox);
            l1.setMultiline(true);
            l1.setSclass("word-wrap");
            l1.setStyle("white-space: normal; padding: 5px");
            //  }

            (new Separator()).setParent(vbox);

           

            fileUpload = new Fileupload();
            //fileUpload.setMaxsize(5000000);
            fileUpload.setLabel("Upload Shapefile");
            fileUpload.setUpload("true");
            fileUpload.setParent(vbox);

            fileUpload.addEventListener("onUpload", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onUpload$btnFileUpload(event);
                    wInstructions.detach();
                }
            });
            fileUpload.addEventListener("onClose", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    System.out.println("Cancelling");
                    wInstructions.detach();
                }
            });

            (new Separator()).setParent(vbox);
            (new Separator()).setParent(vbox);
            Button b = new Button("Cancel Map Tool");
            b.setParent(vbox);
            b.setSclass("goButton");
            b.addEventListener("onClick", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onClick$btnClearSelection(null);
                    wInstructions.detach();
                }
            });

            wInstructions.setParent(getMapComposer().getFellow("mapIframe").getParent());
            wInstructions.setClosable(true);            
            wInstructions.setPosition("top,center");
            try {
                wInstructions.doModal();
            } catch (Exception e) {
                wInstructions.doOverlapped();
                e.printStackTrace();
            }

            return;
        }

        if (text != null && text.length > 0) {
            wInstructions = new Window(toolname, "normal", false);
            wInstructions.setWidth("500px");
            wInstructions.setClosable(false);

            Vbox vbox = new Vbox();
            vbox.setParent(wInstructions);
            for (int i = 0; i < text.length; i++) {
                Label l = new Label((i + 1) + ". " + text[i]);
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

    public void showInstructions(Event event) {
        wInstructions.setParent(getMapComposer().getFellow("mapIframe").getParent());
        wInstructions.setClosable(true);
        wInstructions.doOverlapped();
        wInstructions.setPosition("top,center");
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

        ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).removeAllSelectedLayers(false);

        lastTool = null;
    }

    public void onOpen$cbAreaSelection(Event event) {
        OpenEvent openEvent = (OpenEvent) ((ForwardEvent) event).getOrigin();
        if (openEvent.isOpen()) {
            setInstructions(null, null);
            hideAllInfo();
            displayGeom.setValue(DEFAULT_AREA);
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            mc.removeFromList(mc.getMapLayer("Active Area"));
            ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).removeAllSelectedLayers(false);
            lastTool = null;
        }
    }

    public void onSelect$cbAreaSelection(Event event) {
        lastTool = cbAreaSelection.getSelectedItem();
        System.out.println("cbAreaSelection: " + cbAreaSelection.getSelectedItem().getLabel());

        String wkt;

        if (cbAreaSelection.getSelectedItem() == ciBoundingBox) {
            cbAreaSelection.setText("Drawing bounding box");
            String[] text = {
                "Zoom and pan to the area of interest.",
                "Using the mouse, position the cursor over the area of interest and hold down the left mouse button and drag a rectangle to the required shape and size.",
                "Release the mouse button."
            };
            setInstructions("Active Map Tool: Draw bounding box...", text);
            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();
            //mc.getOpenLayersJavascript().addBoxDrawingTool();
            script += mc.getOpenLayersJavascript().addBoxDrawingTool();
            mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciPolygon) {
            cbAreaSelection.setText("Drawing polygon");
            String[] text = {"Zoom and pan to the area of interest",
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
            String[] text = {
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
        } else if (cbAreaSelection.getSelectedItem() == ciAddressRadiusSelection) {
            String[] text = {
                "Enter address ....", "Select radius"};
            cbAreaSelection.setText("Select radius around an address");
            setInstructions("Active Map Tool: Create radius from address...", text);

            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();

            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciUploadShapefile) {
            cbAreaSelection.setText("Upload shapefile...");
            String[] text = {
                "Select a shapefile with a single polygon"
            };
            setInstructions("Active Map Tool: Upload shapefile (single polygon)...", text);
            showPolygonInfo();
            String script = removeCurrentSelection();
            MapComposer mc = getThisMapComposer();

            mc.removeFromList(mc.getMapLayer("Active Area"));
        } else if (cbAreaSelection.getSelectedItem() == ciMapPolygon) {
            cbAreaSelection.setText("Selecting map polygon");
            String[] text = {
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
            //Events.echoEvent("showEnvelopeInfoEvent", this, null);
            this.showEnvelopeInfo();
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
            wkt = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
            displayGeom.setValue(wkt);

            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, "Active Area");
            updateSpeciesList(false);
        } else { //if (cbAreaSelection.getSelectedItem() == ciBoxCurrentView) {
            cbAreaSelection.setText("Box - Current View");

            setInstructions(null, null);
            showPolygonInfo();

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

        //redraw filter layers
        ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).showAllSelectedLayers();
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
                System.out.println("removing Active Area layer");
                return mc.getOpenLayersJavascript().removeMapLayer(selectionLayer);
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

    public void onClick$btnMetadata(Event event) {
        //do something specific for environmental envelope
        if (cbAreaSelection.getText().contains("envelope")) {
            if (areaInfo.isVisible()) {
                ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).removeAllSelectedLayers(true);
            } else {
                ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).showAllSelectedLayers();
            }
        }
        areaInfo.setVisible(!areaInfo.isVisible());

    }

    /**
     * Searches the gazetter at a given point and then maps the polygon feature 
     * found at the location (for the current top contextual layer).
     * @param event triggered by the usual javascript trickery
     */
    public void onSearchPoint(Event event) {
        searchPoint = (String) event.getData();
        String lon = searchPoint.split(",")[0];
        String lat = searchPoint.split(",")[1];
        Object llist = CommonData.getLayerListJSONArray();
        JSONArray layerlist = JSONArray.fromObject(llist);
        MapComposer mc = getThisMapComposer();

        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        Boolean searchComplete = false;
        for (int i = 0; i < activeLayers.size(); i++) {
            MapLayer ml = activeLayers.get(i);

            String activeLayerName = ml.getUri().replaceAll("^.*ALA:", "").replaceAll("&.*", "");
            System.out.println("ACTIVE LAYER: " + activeLayerName);
            if (ml.isDisplayed()) {
                for (int j = 0; j < layerlist.size(); j++) {
                    if (searchComplete) {
                        break;
                    }

                    JSONObject jo = layerlist.getJSONObject(j);
                   // System.out.println("********" + jo.getString("name"));
                    if (ml != null && jo.getString("type") != null
                            && jo.getString("type").length() > 0
                            && jo.getString("type").equalsIgnoreCase("contextual")
                            && jo.getString("name").equalsIgnoreCase(activeLayerName)) {

                        searchComplete = true;
                        System.out.println(ml.getName());
                        String featureURI = GazetteerPointSearch.PointSearch(lon, lat, activeLayerName, geoServer);
                        System.out.println(featureURI);
                        if(featureURI == null) {
                            continue;
                        }
                        //if it is a filtered layer, expect the filter as part of the new uri.
                        boolean passedFilterCheck = true;
                        try {
                            String filter = ml.getUri().replaceAll("^.*cql_filter=", "").replaceFirst("^.*='","").replaceAll("'.*","");
                            if(filter != null && filter.length() > 0) {
                                passedFilterCheck = featureURI.toLowerCase().contains(filter.toLowerCase().replace(" ","_"));
                            }
                        } catch (Exception e) {
                        }
                        if(!passedFilterCheck) {
                            continue;
                        }


                        //add feature to the map as a new layer
                        String feature_text = getWktFromURI(featureURI, true);

                        String json = readGeoJSON(featureURI);
                        String wkt = wktFromJSON(json);
                        if (wkt.contentEquals("none")) {

                            break;
                        } else {
                            displayGeom.setValue(feature_text);
                            //     mc.removeFromList(mc.getMapLayer("Active Area"));
                            MapLayer mapLayer = mc.addWKTLayer(wkt, "Active Area");
                            updateSpeciesList(false);
                            //searchPoint.setValue("");
                            setInstructions(null, null);
                            break;

                        }
                    }
                }
            }
        }
    }

    /**
     * Searches the occurrences at a given point and then maps the polygon feature
     * found at the location (for the current top contextual layer).
     * @param event triggered by the usual javascript trickery
     */
    public void onSearchSpeciesPoint(Event event) {
        searchSpeciesPoint = (String) event.getData();

        String params[] = searchSpeciesPoint.split(",");
        double lon = Double.parseDouble(params[0]);
        double lat = Double.parseDouble(params[1]);

        int zoom = getMapComposer().getMapZoom();

        double BUFFER_DISTANCE = 0.1;

        String response = "";

        try {

            Map speciesfilters = (Map) Sessions.getCurrent().getAttribute("speciesfilters");
            if (speciesfilters == null) {
                return;
            }

            boolean hasActiveArea = false;
            String lsidtypes = "";
            String lsids = "";
            Iterator it = speciesfilters.keySet().iterator();
            while (it.hasNext()) {
                String lt = (String) it.next();
                String li = (String) speciesfilters.get(lt);
                li = li.split("=")[1];
                li = li.replaceAll("'", "");

                lsidtypes += "type=" + lt;
                if (li.equalsIgnoreCase("aa")) {
                    hasActiveArea = true;
                }
                if (it.hasNext()) {
                    lsidtypes += "&";
                }

                lsids += "lsid=" + URLEncoder.encode(li, "UTF-8");
                if (it.hasNext()) {
                    lsids += "&";
                }
            }

            String reqUri;

//            double radius = 20000;
//            if (zoom > -1 && zoom <= 1) {
//                radius = 500000;
//            } else if (zoom > 1 && zoom <= 3) {
//                radius = 100000;
//            } else if (zoom > 3 && zoom <= 5) {
//                radius = 50000;
//            } else if (zoom > 5 && zoom <= 7) {
//                radius = 10000;
//            } else if (zoom > 7 && zoom <= 9) {
//                radius = 5000;
//            } else if (zoom > 9 && zoom <= 12) {
//                radius = 1000;
//            } else if (zoom > 12 && zoom <= 14) {
//                radius = 100;
//            } else if (zoom > 14) {
//                radius = 50;
//            }

            //get max radius for visible points layers
            int maxSize = 0;
            List udl = getMapComposer().getPortalSession().getActiveLayers();
            Iterator iudl = udl.iterator();
            MapLayer mapLayer = null;
            while (iudl.hasNext()) {
                MapLayer ml = (MapLayer) iudl.next();
                MapLayerMetadata md = ml.getMapLayerMetadata();
                if (md != null && md.getSpeciesLsid() != null
                        && !ml.isClustered() && ml.isDisplayed()) {
                    if (ml.getSizeVal() > maxSize) {
                        maxSize = ml.getSizeVal();
                    }
                }
            }

            //small buffer for circles not being circles
            maxSize *= 1.2;

            //convert to radius in m at zoom, then back to longitude
            String[] va = getMapComposer().getViewArea().replace("POLYGON((", "").replace("))", "").split(",");
            String[] xy1 = va[0].split(" ");
            String[] xy2 = va[2].split(" ");
            double y1 = Double.parseDouble(xy1[1]);
            double y2 = Double.parseDouble(xy2[1]);
            double radius = convertPixelsToMeters(maxSize, Math.abs(y1 - y2) / 2 + Math.min(y1, y2), zoom);

            String wkt2 = createCircle(lon, lat, radius);

            reqUri = settingsSupplementary.getValue(CommonData.SAT_URL) + "/alaspatial";
            //reqUri += "/filtering/apply/pid/none/samples/geojson";
            reqUri += "/species/info/now";
            reqUri += "?area=" + URLEncoder.encode(wkt2, "UTF-8");
            reqUri += "&" + lsids;

            if (hasActiveArea) {
                reqUri += "&aa=" + URLEncoder.encode(getMapComposer().getSelectionArea(), "UTF-8");
            }


            System.out.println("locfeat calling: " + reqUri);

            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(reqUri);
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            response = slist;
            System.out.println("locfeat data: " + slist);
        } catch (Exception e) {
            System.out.println("error loading new geojson:");
            e.printStackTrace(System.out);
        }

        //response = "alert('"+response+"'); ";
        response = "showSpeciesInfo('" + response + "'," + lon + "," + lat + "); ";
        Clients.evalJavaScript(response);
    }

    public double convertPixelsToMeters(int pixels, double latitude, int zoom) {
        return ((Math.cos(latitude * Math.PI / 180.0) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, zoom))) * pixels;
    }

    /**
     * transform json string with geometries into wkt.
     *
     * extracts 'shape_area' if available and assigns it to storedSize.
     *
     * @param json
     * @return
     */
    private String wktFromJSON(String json) {
        try {
            JSONObject obj = JSONObject.fromObject(json);
            JSONArray geometries = obj.getJSONArray("geometries");
            String wkt = "";
            for (int i = 0; i < geometries.size(); i++) {
                String coords = geometries.getJSONObject(i).getString("coordinates");

                if (geometries.getJSONObject(i).getString("type").equalsIgnoreCase("multipolygon")) {
                    wkt += coords.replace("]]],[[[", "))*((").replace("]],[[", "))*((").replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "MULTIPOLYGON(((").replace("]]]]", ")))");

                } else {
                    wkt += coords.replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[", "POLYGON((").replace("]]]", "))").replace("],[", "),(");
                }

                wkt = wkt.replace(")))MULTIPOLYGON(", ")),");
            }
            return wkt;
        } catch (JSONException e) {
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
        } catch (Exception e) {
        }
        return content.toString();
    }

    /**
     * 
     * @param event
     */
    public void onSelectionGeom(Event event) {
        selectionGeom = (String) event.getData();
        System.out.println("onchange$selectiongeom");
        setInstructions(null, null);
        try {
            String wkt = "";
            if (selectionGeom.contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
                lastTool = null;
            } else if (selectionGeom.startsWith("LAYER(")) {
                //reset stored size
                storedSize = null;
                //get WKT from this feature
                String v = selectionGeom.replace("LAYER(", "");
                //FEATURE(table name if known, class name)
                v = v.substring(0, v.length() - 1);
                wkt = getLayerGeoJsonAsWkt(v, true);
                displayGeom.setValue(wkt);

                //for display
                wkt = getLayerGeoJsonAsWkt(v, false);

                //calculate area is not populated
                if (storedSize == null) {
                    storedSize = getAreaOfWKT(wkt);
                }
            } else {
                wkt = selectionGeom;
                displayGeom.setValue(wkt);
            }
            updateComboBoxText();
            updateSpeciesList(false); // true

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            if (wkt.length() > 0) {
                MapLayer mapLayer = mc.addWKTLayer(wkt, "Active Area");
            }
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
        } else if (lastTool == ciAddressRadiusSelection) {
            txt = "Got user radius from address";
        } else if (lastTool == ciUploadShapefile) {
            txt = "Got user polygon from shapefile";
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

    public void onBoxGeom(Event event) {
        boxGeom = (String) event.getData();
        setInstructions(null, null);
        try {
            if (boxGeom.contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
                lastTool = null;
            } else {
                displayGeom.setValue(boxGeom);
            }
            updateComboBoxText();
            updateSpeciesList(false); // true


            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            //mc.removeLayer("Area Selection");
            //mc.deactiveLayer(mc.getMapLayer("Area Selection"), true,true);
            MapLayer mapLayer = mc.addWKTLayer(boxGeom, "Active Area");

            rgAreaSelection.getSelectedItem().setChecked(false);

            //wfsQueryBBox(boxGeom.getValue());


        } catch (Exception e) {//FIXME
        }

    }

    public void onClick$btnShowSpecies() {
        openResults();
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
                    (FilteringResultsWCController) getMapComposer().getFellow("leftMenuAnalysis").getFellow("analysiswindow").getFellow("sf").getFellow("selectionwindow").getFellow("speciesListForm").getFellow("popup_results");
            //if (!populateSpeciesList) {
            win.refreshCount();
            //} else {
            //    win.onClick$refreshButton2();
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateAreaLabel();
    }

    void updateAreaLabel() {
        //treat as WKT
        String area = getGeom();

        String size = null;

        if (area.toLowerCase().contains("envelope") || area.toLowerCase().contains("layer")) {
            size = storedSize;
        } else if (cbAreaSelection.getText().toLowerCase().contains("world")) {
            size = "509600000"; //fixed value
        } else {
            //try as WKT
            size = getAreaOfWKT(area);
        }

//        if (size == null) {
//            lblArea.setValue("");
//        } else {
//            lblArea.setValue(size + " sq km");
//        }
    }

    void onEnvelopeDone(boolean hide) {
        try {
            String envPid = ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).getPid();
            storedSize = "";
            String size = ((FilteringWCController) envelopeWindow.getFellow("filteringwindow")).getAreaSize();

            try {
                double d = Double.parseDouble(size);
                storedSize = String.format("%,.1f", d / 1000000.0);  //convert m^2 to km^2
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (envPid.length() > 0) {
                displayGeom.setText("ENVELOPE(" + envPid + ")");
                updateComboBoxText();
            } else if (hide) {
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

    public void checkForAreaRemoval() {
        MapLayer ml = getMapComposer().getMapLayer("Active Area");
        if (ml == null && cbAreaSelection.getSelectedItem() != ciBoxCurrentView
                && !cbAreaSelection.getText().contains("envelope")) {
            onClick$btnClearSelection(null);
            if (wInstructions != null) {
                wInstructions.detach();
            }
        }

        attachViewportListener();
    }

    /**
     * get Active Area as WKT string, from a layer name
     *
     * @param layer name of layer as String
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    String getLayerGeoJsonAsWkt(String layer, boolean register_shape) {
        String wkt = DEFAULT_AREA;

        if (!register_shape) {
            return wktFromJSON(getMapComposer().getMapLayer(layer).getGeoJSON());
        }

        try {
            //try to get table name from uri like gazetteer/aus1/Queensland.json
            String uri = getMapComposer().getMapLayer(layer).getUri();
            String gaz = "gazetteer/";
            int i1 = uri.indexOf(gaz);
            int i2 = uri.indexOf("/", i1 + gaz.length() + 1);
            int i3 = uri.lastIndexOf(".json");
            String table = uri.substring(i1 + gaz.length(), i2);
            String value = uri.substring(i2 + 1, i3);
            //test if available in alaspatial
            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/lookup");
            get.addParameter("table", table);
            get.addParameter("value", value);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register table and value with alaspatial: " + slist);

            if (slist != null && result == 200) {
                wkt = "LAYER(" + layer + "," + slist + ")";

                return wkt;
            }
        } catch (Exception e) {
            System.out.println("no alaspatial shape for layer: " + layer);
            e.printStackTrace();
        }
        try {
            //class_name is same as layer name
            wkt = wktFromJSON(getMapComposer().getMapLayer(layer).getGeoJSON());

            if (!register_shape) {
                return wkt;
            }

            //register wkt with alaspatial and use LAYER(layer name, id)
            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/register");
            get.addParameter("area", wkt);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register wkt shape with alaspatial: " + slist);

            wkt = "LAYER(" + layer + "," + slist + ")";
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SelectionController.getLayerGeoJsonAsWkt(" + layer + "): " + wkt);
        return wkt;
    }

    /**
     * get Active Area as WKT string, from a layer name and feature class
     *
     * @param layer name of layer as String
     * @param classification value of feature classification
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    String getWktFromURI(String layer, boolean register_shape) {
        String feature_text = DEFAULT_AREA;

        if (!register_shape) {
            String json = readGeoJSON(layer);
            return feature_text = wktFromJSON(json);
        }

        try {
            String uri = layer;
            String gaz = "gazetteer/";
            int i1 = uri.indexOf(gaz);
            int i2 = uri.indexOf("/", i1 + gaz.length() + 1);
            int i3 = uri.lastIndexOf(".json");
            String table = uri.substring(i1 + gaz.length(), i2);
            String value = uri.substring(i2 + 1, i3);
            //test if available in alaspatial
            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/lookup");
            get.addParameter("table", table);
            get.addParameter("value", value);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register table and value with alaspatial: " + slist);

            if (slist != null && result == 200) {
                feature_text = "LAYER(" + layer + "," + slist + ")";

                return feature_text;
            }
        } catch (Exception e) {
            System.out.println("no alaspatial shape for layer: " + layer);
            e.printStackTrace();
        }
        try {
            //class_name is same as layer name
            String json = readGeoJSON(layer);
            feature_text = wktFromJSON(json);

            if (!register_shape) {
                return feature_text;
            }

            //register wkt with alaspatial and use LAYER(layer name, id)
            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(satServer + "/alaspatial/species/shape/register");
            get.addParameter("area", feature_text);
            get.addRequestHeader("Accept", "text/plain");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("register wkt shape with alaspatial: " + slist);

            feature_text = "LAYER(" + layer + "," + slist + ")";
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SelectionController.getLayerGeoJsonAsWkt(" + layer + "): " + feature_text);
        return feature_text;
    }

    private String createCircle(double x, double y, final double RADIUS) {
        return createCircle(x, y, RADIUS, 50);

    }

    private String createCircle(double x, double y, final double RADIUS, int sides) {

        try {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

//            CoordinateReferenceSystem dataCRS = CRS.decode("EPSG:4326");
//            CoordinateReferenceSystem googleCRS = CRS.decode("EPSG:900913");
            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
                    + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
                    + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
                    + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
                    + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
                    + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            String wkt900913 = "PROJCS[\"WGS84 / Google Mercator\", "
                    + "  GEOGCS[\"WGS 84\", "
                    + "   DATUM[\"World Geodetic System 1984\", "
                    + "   SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], "
                    + "  AUTHORITY[\"EPSG\",\"6326\"]], "
                    + " PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], "
                    + " UNIT[\"degree\", 0.017453292519943295], "
                    + " AXIS[\"Longitude\", EAST], "
                    + " AXIS[\"Latitude\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"4326\"]], "
                    + " PROJECTION[\"Mercator_1SP\"], "
                    + " PARAMETER[\"semi_minor\", 6378137.0], "
                    + " PARAMETER[\"latitude_of_origin\", 0.0],"
                    + " PARAMETER[\"central_meridian\", 0.0], "
                    + " PARAMETER[\"scale_factor\", 1.0], "
                    + " PARAMETER[\"false_easting\", 0.0], "
                    + " PARAMETER[\"false_northing\", 0.0], "
                    + " UNIT[\"m\", 1.0], "
                    + " AXIS[\"x\", EAST], "
                    + " AXIS[\"y\", NORTH], "
                    + " AUTHORITY[\"EPSG\",\"900913\"]] ";
            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            CoordinateReferenceSystem googleCRS = CRS.parseWKT(wkt900913);
            MathTransform transform = CRS.findMathTransform(wgsCRS, googleCRS);
            Point point = geometryFactory.createPoint(new Coordinate(y, x));
            Geometry geom = JTS.transform(point, transform);
            Point gPoint = geometryFactory.createPoint(new Coordinate(geom.getCoordinate()));

            System.out.println("Google point:" + gPoint.getCoordinate().x + "," + gPoint.getCoordinate().y);

            MathTransform reverseTransform = CRS.findMathTransform(googleCRS, wgsCRS);
            final int SIDES = sides;
            Coordinate coords[] = new Coordinate[SIDES + 1];
            for (int i = 0; i < SIDES; i++) {
                double angle = ((double) i / (double) SIDES) * Math.PI * 2.0;
                double dx = Math.cos(angle) * RADIUS;
                double dy = Math.sin(angle) * RADIUS;
                geom = JTS.transform(geometryFactory.createPoint(new Coordinate((double) gPoint.getCoordinate().x + dx, (double) gPoint.getCoordinate().y + dy)), reverseTransform);
                coords[i] = new Coordinate(geom.getCoordinate().y, geom.getCoordinate().x);
            }
            coords[SIDES] = coords[0];

            LinearRing ring = geometryFactory.createLinearRing(coords);
            Polygon polygon = geometryFactory.createPolygon(ring, null);


            //Geometry polyGeom = JTS.transform(coords,reverseTransform);
            WKTWriter writer = new WKTWriter();
            String wkt = writer.write(polygon);
            return wkt.replaceAll("POLYGON ", "POLYGON").replaceAll(", ", ",");

        } catch (Exception e) {
            System.out.println("Circle fail!");
            return "none";
        }

    }

    private String radiusFromAddress(String address) {
        try {

            GeoAddressStandardizer st = new GeoAddressStandardizer("AABBCC");

            List<GeoAddress> addresses = st.standardizeToGeoAddresses(address + ", Australia");
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

            GeoCoordinate gco = addresses.get(0).getCoordinate();
            cbAreaSelection.setText("address: " + addresses.get(0).getAddressLine());

            //Point point = geometryFactory.createPoint(new Coordinate(
            double radius = 1000;
            if (cbRadius.getSelectedItem() == ci1km) {
                radius = 1000;
            }
            if (cbRadius.getSelectedItem() == ci5km) {
                radius = 5000;
            }
            if (cbRadius.getSelectedItem() == ci10km) {
                radius = 10000;
            }
            if (cbRadius.getSelectedItem() == ci20km) {
                radius = 20000;
            }
            return createCircle(gco.getLongitude(), gco.getLatitude(), radius);

        } catch (geo.google.GeoException ge) {
            return "none";
        }



    }

    public void onClick$btnRadiusFromAddress(Event event) {
        String wkt = radiusFromAddress(addressBox.getText());
        if (wkt.contentEquals("none")) {
            return;
        } else {

            displayGeom.setValue(wkt);
            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, "Active Area");
            updateSpeciesList(false);

            setInstructions(null, null);
        }
    }

    public void onClick$btnFindAddress(Event event) {
        try {
            GeoAddressStandardizer st = new GeoAddressStandardizer("AABBCC");

            List<GeoAddress> addresses = st.standardizeToGeoAddresses(addressBox.getText() + ", Australia");
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

            GeoCoordinate gco = addresses.get(0).getCoordinate();

            addressLabel.setValue(addresses.get(0).getAddressLine());
        } catch (geo.google.GeoException ge) {
        }
    }

    private String getAreaOfWKT(String wkt) {
        String area = null;

        try {
            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
                    + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
                    + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
                    + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
                    + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
                    + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            String wkt3577 = "PROJCS[\"GDA94 / Australian Albers\","
                    + "    GEOGCS[\"GDA94\","
                    + "        DATUM[\"Geocentric_Datum_of_Australia_1994\","
                    + "            SPHEROID[\"GRS 1980\",6378137,298.257222101,"
                    + "                AUTHORITY[\"EPSG\",\"7019\"]],"
                    + "            TOWGS84[0,0,0,0,0,0,0],"
                    + "            AUTHORITY[\"EPSG\",\"6283\"]],"
                    + "        PRIMEM[\"Greenwich\",0,"
                    + "            AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "        UNIT[\"degree\",0.01745329251994328,"
                    + "            AUTHORITY[\"EPSG\",\"9122\"]],"
                    + "        AUTHORITY[\"EPSG\",\"4283\"]],"
                    + "    UNIT[\"metre\",1,"
                    + "        AUTHORITY[\"EPSG\",\"9001\"]],"
                    + "    PROJECTION[\"Albers_Conic_Equal_Area\"],"
                    + "    PARAMETER[\"standard_parallel_1\",-18],"
                    + "    PARAMETER[\"standard_parallel_2\",-36],"
                    + "    PARAMETER[\"latitude_of_center\",0],"
                    + "    PARAMETER[\"longitude_of_center\",132],"
                    + "    PARAMETER[\"false_easting\",0],"
                    + "    PARAMETER[\"false_northing\",0],"
                    + "    AUTHORITY[\"EPSG\",\"3577\"],"
                    + "    AXIS[\"Easting\",EAST],"
                    + "    AXIS[\"Northing\",NORTH]]";

            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            CoordinateReferenceSystem GDA94CRS = CRS.parseWKT(wkt3577);
            MathTransform transform = CRS.findMathTransform(wgsCRS, GDA94CRS);

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            WKTReader reader = new WKTReader(geometryFactory);

            //there is a need to swap longitude and latitude in the wkt
            StringBuffer sb = new StringBuffer();
            String x = "";
            String y = "";
            int inNumber = 0;
            for (int i = 0; i < wkt.length(); i++) {
                char c = wkt.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                    if (inNumber == 0) {
                        //start first number
                        inNumber = 1;

                        x = "" + c;
                    } else if (inNumber == 1) {
                        //continue first number
                        x = x + c;
                    } else {
                        //continue 2nd number
                        y = y + c;
                    }
                } else if (inNumber == 1 && c == ' ') {
                    //moving to 2nd number
                    inNumber = 2;

                    //reset 2nd number
                    y = "";
                } else {
                    if (inNumber == 2) {
                        //write swapped numbers
                        sb.append(y).append(" ").append(x);

                        //reset position
                        inNumber = 0;
                    }
                    //write current value
                    sb.append(c);
                }
            }

            Geometry geom = reader.read(sb.toString());
            Geometry geomT = JTS.transform(geom, transform);

            area = String.format("%,.1f", geomT.getArea() / 1000000.0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return area;
    }

    public void attachViewportListener() {
        if (!viewportListenerAdded) {
            viewportListenerAdded = true;

            //listen for map extents changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // refresh count may be required if area is CURRENTVIEW
                    if (displayGeom.getText().contains("CURRENTVIEW()")) {
                        updateAreaLabel();
                    }
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("areaLabel", el);

            //run it now
            updateAreaLabel();
        }
    }

    public void onUpload$btnFileUpload(Event event) {
        //UploadEvent ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        UploadEvent ue = null;
        if (event.getName().equals("onUpload")) {
            ue = (UploadEvent) event;
        } else if (event.getName().equals("onForward")) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            System.out.println("m.getName(): " + m.getName());
            System.out.println("getContentType: " + m.getContentType());
            System.out.println("getFormat: " + m.getFormat());


            // check the content-type
            if (m.getContentType().equalsIgnoreCase("text/plain") || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV) || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV_EXCEL)) {
                getMapComposer().loadUserPoints(m.getName(), m.getReaderData());
            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_EXCEL)) {
                byte[] csvdata = m.getByteData();
                getMapComposer().loadUserPoints(m.getName(), new StringReader(new String(csvdata)));
            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_KML)) {
                System.out.println("isBin: " + m.isBinary());
                System.out.println("inMem: " + m.inMemory());
                if (m.inMemory()) {
                    getMapComposer().loadUserLayerKML(m.getName(), m.getByteData());
                } else {
                    getMapComposer().loadUserLayerKML(m.getName(), m.getStreamData());
                }

            } else if (m.getFormat().equalsIgnoreCase("zip")) { //else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_ZIP)) {
                // "/data/ala/runtime/output/layers/"
                // "/Users/ajay/projects/tmp/useruploads/"
                Map input = Zipper.unzipFile(m.getName(), m.getStreamData(), "/data/ala/runtime/output/layers/");
                String type = "";
                String file = "";
                if (input.containsKey("type")) {
                    type = (String) input.get("type");
                }
                if (input.containsKey("file")) {
                    file = (String) input.get("file");
                }
                if (type.equalsIgnoreCase("shp")) {
                    System.out.println("Uploaded file is a shapefile. Loading...");
                    Map shape = ShapefileReader.loadShapefile(new File(file));

                    if (shape == null) {
                        return;
                    } else {
                        String wkt = (String) shape.get("wkt");
                        wkt = wkt.replace("MULTIPOLYGON (((", "POLYGON((").replaceAll(", ", ",").replace(")))", "))");
                        displayGeom.setValue(wkt);
                        System.out.println("Got shapefile wkt...");
                        updateComboBoxText();
                        MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, "Active Area");
                        mapLayer.setMapLayerMetadata(new MapLayerMetadata());
                        mapLayer.getMapLayerMetadata().setMoreInfo("User uploaded shapefile. \n Used polygon: " + shape.get("id"));
                        updateSpeciesList(false);
                        setInstructions(null, null);
                    }

                } else {
                    System.out.println("Unknown file type. ");
                    getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, KML or Shapefile. ");
                }
            }

        } catch (Exception ex) {
            getMapComposer().showMessage("Unable to load file. Please try again. ");
            ex.printStackTrace();
        }
    }

    void updateActiveAreaInfo() {
        updateSpeciesList(false); // true
    }
}
