package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import au.org.emii.portal.menu.MapLayer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.ArrayList;

//import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchResult;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.ala.spatial.analysis.web.SpeciesAutoComplete;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkmax.zul.Filedownload;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleGroupsModel;

/**
 *
 * @author ajay
 */
public class SamplingWCController extends UtilityComposer {

    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
    private Radio rdoCommonSearch;
    private SpeciesAutoComplete sac;
    private Listbox lbenvlayers;
    private Button btnMapSpecies;
    private Button btnPreview;
    private Popup results;
    private Popup p;
    private Html h;
    private Rows results_rows;
    private Button btnDownload;
    private List layers;
    private Map layerdata;
    private String selectedLayer;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;
    private MapComposer mc;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    private String user_polygon = "";
    private Textbox selectionGeomSampling;
    Button pullFromActiveLayers;
    
    LayersUtil layersUtil;

    @Override
    public void afterCompose() {
        super.afterCompose();

        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
        
        layersUtil = new LayersUtil(mc,satServer);
        
        layers = new Vector();
        layerdata = new Hashtable<String, String[]>();

        String[][] datas = new String[][]{
            setupEnvironmentalLayers(),
            setupContextualLayers()
        };

        lbenvlayers.setItemRenderer(new ListitemRenderer() {

            public void render(Listitem li, Object data) {
                try {
                    String layername = (String) data;
                    li.setWidth(null);
                    Listcell lc = new Listcell(layername);
                    lc.setParent(li);

                    /* onclick event for popup content update */
                    lc.addEventListener("onClick", new EventListener() {

                        public void onEvent(Event event) throws Exception {
                            showLayerExtents(event.getTarget());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        });
        lbenvlayers.setModel(new SimpleGroupsModel(datas, new String[]{"Environmental", "Contextual"}));
    }

    private void showLayerExtents(Object o) {
        Listcell lc = (Listcell) o;
        Listitem li = (Listitem) lc.getParent();

        selectedLayer = (String) lc.getLabel();
        selectedLayer = selectedLayer.trim();
        String slist = "";
        try {
            selectedLayer = selectedLayer.replaceAll(" ", "_");
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents"); // testurl
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            slist = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        h.setContent(slist);


        p.open(lc);
    }

    private void showLayerExtentsLabel(Object o) {
        Label l = (Label) o;

        selectedLayer = (String) l.getValue();
        selectedLayer = selectedLayer.trim();
        String slist = "";
        try {
            selectedLayer = selectedLayer.replaceAll(" ", "_");
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents"); // testurl
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            slist = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        h.setContent(slist);

        p.open(l);
    }

   

    public void onCheck$rdoCommonSearch() {
        sac.setSearchCommon(true);
        sac.getItems().clear();
    }

    public void onCheck$rdoScientificSearch() {
        sac.setSearchCommon(false);
        sac.getItems().clear();
    }

    public void onClick$btnMapSpecies(Event event) {
        try {
            //status.setValue("clicked new value selected: " + sac.getText() + " - " + sac.getValue());
            //System.out.println("Looking up taxon names for " + sac.getValue());
            //Messagebox.show("Hello world!, i got clicked");

            loadSpeciesOnMap();

        } catch (Exception ex) {
            System.out.println("Got an error clicking button!!");
            ex.printStackTrace(System.out);
        }
    }

    public void onClick$btnDownloadMetadata(Event event) {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents"); // testurl
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();


            Filedownload.save(slist, "text/plain", selectedLayer + "_metadata.txt");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private boolean isEnvironmental(String layername) {
        layername = layername.trim();
        String[] layernames = (String[]) layerdata.get("Environmental");
        if (layernames != null) {
            for (int i = 0; i < layernames.length; i++) {
                if (layernames[i].trim().equals(layername)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isContextual(String layername) {
        layername = layername.trim();
        String[] layernames = (String[]) layerdata.get("Contextual");
        if (layernames != null) {
            for (int i = 0; i < layernames.length; i++) {
                if (layernames[i].trim().equals(layername)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onDoInit(Event event) throws Exception {
        runsampling();
        Clients.showBusy("", false);
    }

    public void onClick$btnPreview(Event event) {
        Clients.showBusy("Sampling...", true);
        Events.echoEvent("onDoInit", this, event.toString());
    }
    
    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private String [] setupEnvironmentalLayers() {
    	String [] aslist = null;
        try {          
            aslist = layersUtil.getEnvironmentalLayers();


            if (aslist.length > 0) {
                layers.addAll(Arrays.asList(aslist));
                layerdata.put("Environmental", aslist);

                System.out.println("env:");
                for (int k = 0; k < aslist.length; k++) {
                    System.out.println(aslist[k] + ", ");
                }
            }

        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
        
        return aslist;
    }
    
    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private String [] setupContextualLayers() {
    	String [] aslist = null;
        try {          
            aslist = layersUtil.getContextualLayers();

            if (aslist.length > 0) {
                layers.addAll(Arrays.asList(aslist));
                layerdata.put("Contextual", aslist);
                
                for (int k = 0; k < aslist.length; k++) {
                    System.out.println(aslist[k] + ", ");
                }
            }

        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
        
        return aslist;
    }


    public void runsampling() {
        try {

            String taxon = sac.getValue();
            // check if its a common name, if so, grab the scientific name
            if (rdoCommonSearch.isChecked()) {
                taxon = getScientificName();
            }

            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }
                    i++;

                }
                if (i == 0) {
                    sbenvsel.append("none");
                }
            } else {
                sbenvsel.append("none");
            }


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/sampling/process/preview?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (user_polygon.length() > 0) {
                sbProcessUrl.append("&points=" + URLEncoder.encode(user_polygon, "UTF-8"));
            } else {
                sbProcessUrl.append("&points=" + URLEncoder.encode("none", "UTF-8"));
            }

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);

            String[] aslist = slist.split(";");
            System.out.println("Result count: " + aslist.length);
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                System.out.println("Column Count: " + rec.length);
                //System.out.println()

            }

            /*
            //JSONObject jsonObject = JSONObject.fromObject( slist );
            //List expected = JSONArray.toList( jsonObject.getJSONArray( "array" ) );
            JSONArray ja = JSONArray.fromObject(slist);
            //System.out.println("JSON RESPONSE: \n" + ja.toString(2));
            List respData = (List) JSONArray.toCollection(ja);
            System.out.println("Result return size: " + respData.size());
            Iterator it = respData.iterator();
            while (it.hasNext()) {
            Object o = it.next();
            System.out.println(o.getClass());
            System.out.println(o);
            MorphDynaBean bean = ((MorphDynaBean) o);
            System.out.println(bean.toString());
            }
             */

            // load into the results popup
            int j;
            //remove existing rows
            List l = results_rows.getChildren();
            System.out.println(l);
            if (l != null) {
                for (j = l.size() - 1; j >= 0; j--) {
                    Row r = (Row) l.get(j);
                    //  System.out.println("detaching: " + ((Label) r.getChildren().get(0)).getValue());
                    r.detach();
                }
            }

            /* map of top row, contextual columns data lists for value lookups */
            Map contextualLists = new Hashtable<Integer, String[]>();

            // add rows
            String[] top_row = null;
            for (int i = 0; i < aslist.length; i++) {
                if (i == 0) {
                    top_row = aslist[i].split("~");

                    for (int k = 0; k < top_row.length; k++) {
                        if (isContextual(top_row[k])) {
                            System.out.println("contextual column=" + top_row[k]);
                            try {
                                String layername = top_row[k].trim().replaceAll(" ", "_");
                                client = new HttpClient();
                                get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + layername + "/extents"); // testurl
                                get.addRequestHeader("Accept", "text/plain");

                                result = client.executeMethod(get);
                                String[] salist = get.getResponseBodyAsString().split("<br>");
                                contextualLists.put(new Integer(k), salist);

                                System.out.println("# records=" + salist.length);
                            } catch (Exception e) {
                                e.printStackTrace(System.out);
                            }
                        }
                    }
                }
                String[] rec = aslist[i].split("~");
                System.out.println("Column Count: " + rec.length);
                //System.out.println()

                Row r = new Row();
                r.setParent(results_rows);
                // set the value
                for (int k = 0; k < rec.length && k < top_row.length; k++) {
                    Label label = new Label(rec[k]);
                    label.setParent(r);

                    /* onclick event for popup content update */
                    boolean iscontextual = isContextual(top_row[k]);
                    boolean isenvironmental = isEnvironmental(top_row[k]);

                    System.out.println("label=" + top_row[k] + " iscontextual=" + iscontextual + " isenvironmental=" + isenvironmental);
                    if (iscontextual || isenvironmental) {
                        if (i == 0) {
                            label.addEventListener("onClick", new EventListener() {

                                public void onEvent(Event event) throws Exception {
                                    showLayerExtentsLabel(event.getTarget());
                                }
                            });
                        }/*//no longer required
                        else if (iscontextual) {
                        try {
                        String[] salist = (String[]) contextualLists.get(new Integer(k));
                        int idx = Integer.parseInt(rec[k]);
                        if (salist != null && salist.length > idx) {
                        label.setTooltiptext(salist[idx]);
                        }
                        } catch (Exception e) {
                        }
                        }//isenvironmental else{}*/
                    }


                    /*
                    //add event listener for contextual columns
                    if (j == 0) { //add for header row
                    label.addEventListener("onClick", new EventListener() {

                    public void onEvent(Event event) throws Exception {
                    String display_name = ((Label) event.getTarget()).getValue().trim();
                    for (int k = 0; k < _layers.size(); k++) {
                    Layer layer = (Layer) _layers.get(k);
                    if (layer.display_name.equals(display_name)) {
                    popup_layer = layer;

                    Html h = (Html) getFellow("h");
                    String csv = SpeciesListIndex.getLayerExtents(layer.name);
                    h.setContent(csv);

                    Popup p = (Popup) getFellow("p");
                    //li.setPopup(p);
                    p.open(event.getTarget());
                    }
                    }
                    }
                    });
                    } else {
                    // is catagorical layer
                    if (i < splfilters.length && splfilters[i] != null && splfilters[i].catagory_names != null) {
                    try {
                    int idx = Integer.parseInt(csv_filename[j][i]);
                    label.setTooltiptext(splfilters[i].catagory_names[idx]);
                    } catch (Exception e) {
                    System.out.println(e.toString());
                    }
                    }
                    }
                     */

                }
            }



        } catch (Exception e) {
            System.out.println("Exception calling sampling.preview:");
            e.printStackTrace(System.out);
        }

        results.open(100, 100);

    }

    public void onClick$btnDownload(Event event) {
        try {

            String taxon = sac.getValue();
            // check if its a common name, if so, grab the scientific name
            if (rdoCommonSearch.isChecked()) {
                taxon = getScientificName();
            }

            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }
                    i++;
                }
                if (i == 0) {
                    sbenvsel.append("none");
                }
            } else {
                sbenvsel.append("none");
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/sampling/process/download?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (user_polygon.length() > 0) {
                sbProcessUrl.append("&points=" + URLEncoder.encode(user_polygon, "UTF-8"));
            } else {
                sbProcessUrl.append("&points=" + URLEncoder.encode("none", "UTF-8"));
            }

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);


            if (slist.equalsIgnoreCase("")) {
                Messagebox.show("Unable to download sample file. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
            } else {
                /*
                URL url = new URL(satServer + "/alaspatial" + slist);
                url.openStream();
                org.zkoss.zhtml.Filedownload.save(url.openStream(), "application/zip", sac.getValue() + "_sample.zip");
                 *
                 */

                //Filedownload.save(satServer + "/alaspatial" + slist, null);
                //Messagebox.show("Downloading sample file...", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
                System.out.println("Sending file to user: " + satServer + "/alaspatial" + slist);
                Filedownload.save(new URL(satServer + "/alaspatial" + slist), "application/zip");

            }


        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }

    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        //Page page = maxentWindow.getPage();
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    private void loadSpeciesOnMap() {
        /*
        String taxon = sac.getValue();
        String uri = null;
        String filter = null;

        // capitalise the taxon name
        System.out.print("Changing taxon name from '" + taxon);
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        System.out.println("' to '" + taxon + "' ");


        //uri = "http://ec2-184-73-34-104.compute-1.amazonaws.com/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";
        uri = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";

        //get the current MapComposer instance
        MapComposer mc = getThisMapComposer();

        //contruct the filter
        //filter = "<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+mapWMS+entity+"&type=1&unit=1]]></Literal></PropertyIsEqualTo></Filter>";
        //lets try it in cql
        //filter = "species eq '" + taxon + "'";
        String desc = sac.getSelectedItem().getDescription();
        String tlevel = desc.split("-")[0].trim();
        filter =  tlevel + " eq '" + taxon + "'";

        logger.debug(filter);
        //mc.addWMSLayer(label, uri, 1, filter);
        mc.addKnownWMSLayer(taxon, uri, 1, filter);
         *
         */

        /*
        String taxon = sac.getValue();
        // check if its a common name, if so, grab the scientific name
        if (rdoCommonSearch.isChecked()) {
            taxon = getScientificName();
        }
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        String uri = null;
        String filter = null;
        String entity = null;
        MapLayer mapLayer = null;

        //TODO these paramaters need to read from the config
        String layerName = "ALA:occurrencesv1";
        String sld = "species_point";
        uri = geoServer + "/geoserver/wms?service=WMS";
        String format = "image/png";

        //get the current MapComposer instance
        //MapComposer mc = getThisMapComposer();

        //contruct the filter in cql
        filter = "species eq '" + taxon + "'";
        mapLayer = genericServiceAndBaseLayerSupport.createMapLayer("Species occurrence for " + taxon, taxon, "1.1.1", uri, layerName, format, sld, filter);

        //create a random colour

        Random rand = new java.util.Random();
        int r = rand.nextInt(99);
        int g = rand.nextInt(99);
        int b = rand.nextInt(99);
        String hexColour = String.valueOf(r) + String.valueOf(g) + String.valueOf(b);
        mapLayer.setEnvParams("color:" + hexColour + ";name:circle;size:6");
        mc.addUserDefinedLayerToMenu(mapLayer, true);
         *
         */

        String taxon = sac.getValue();
        String spVal = sac.getSelectedItem().getDescription();
        if (spVal.trim().startsWith("Scientific")) {
            //myci.setValue(spVal[1].trim().substring(spVal[1].trim().indexOf(":")).trim());
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim();
        }
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        mc.mapSpeciesByName(taxon);
    }

    private String getScientificName() {
        String taxon = "";
        try {

            String nuri = "http://data.ala.org.au/search/commonNames/" + URLEncoder.encode(sac.getValue(), "UTF-8") + "/json";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nuri);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String snlist = get.getResponseBodyAsString();

            TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
            jsonConfig.setJavaPropertyFilter(new PropertyFilter() {

                @Override
                public boolean apply(Object source, String name, Object value) {
                    if ("result".equals(name)) {
                        return true;
                    }
                    return false;
                }
            });

            JSONObject jo = JSONObject.fromObject(snlist);

            tss = (TaxaCommonSearchSummary) JSONSerializer.toJava(jo, jsonConfig);

            if (tss.getRecordsReturned() > 1) {

                JSONArray joResult = jo.getJSONArray("result");

                JsonConfig jsonConfigResult = new JsonConfig();
                jsonConfigResult.setRootClass(TaxaCommonSearchResult.class);

                for (int i = 0; i < joResult.size(); i++) {
                    TaxaCommonSearchResult tr = (TaxaCommonSearchResult) JSONSerializer.toJava(joResult.getJSONObject(i), jsonConfigResult);
                    tss.addResult(tr);
                }
            }

            //taxon = tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")";
            taxon = tss.getResultList().get(0).getScientificName();
            //status.setValue("Got: " + tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")");

        } catch (Exception e) {
            System.out.println("Oopps, error getting scientific name from common name");
            e.printStackTrace(System.out);
        }

        return taxon;
    }

    /**
     * Activate the polygon selection tool on the map
     * @param event
     */
    public void onClick$btnPolygonSelection(Event event) {
        //MapComposer mc = getThisMapComposer();

        mc.getOpenLayersJavascript().addPolygonDrawingToolSampling();
    }

    /**
     * clear the polygon selection tool on the map
     * @param event
     */
    public void onClick$btnPolygonSelectionClear(Event event) {
        user_polygon = "";
        selectionGeomSampling.setValue("");

        //MapComposer mc = getThisMapComposer();

        mc.getOpenLayersJavascript().removePolygonSampling();
    }

    /**
     * 
     * @param event
     */
    public void onChange$selectionGeomSampling(Event event) {
        try {

            user_polygon = convertGeoToPoints(selectionGeomSampling.getValue());

        } catch (Exception e) {//FIXME
            e.printStackTrace();
        }

    }

    String convertGeoToPoints(String geometry) {
        if (geometry == null) {
            return "";
        }
        geometry = geometry.replace(" ", ":");
        geometry = geometry.replace("POLYGON((", "");
        geometry = geometry.replace(")", "");
        return geometry;
    }
    
      
    /**
     * populate sampling screen with values from active layers
     * 
     * TODO: run this on 'tab' open
     */
    public void onClick$pullFromActiveLayers(){
    	//get top species and list of env/ctx layers
    	String species = layersUtil.getFirstSpeciesLayer();
    	String [] layers = layersUtil.getActiveEnvCtxLayers();    	
    	
    	/* set species from layer selector */
    	if (species != null) {
    		sac.setValue(species);
    	}
    	
    	/* set as selected each envctx layer found */
    	if (layers != null) {
	    	List<Listitem> lis = lbenvlayers.getItems();
	    	for (int i = 0; i < lis.size(); i++) {
	    		for (int j = 0; j < layers.length; j++) {
	    			if(lis.get(i).getLabel().equalsIgnoreCase(layers[j])) {
	    				lbenvlayers.addItemToSelection(lis.get(i));
	    				break;
	    			}
	    		}
	    	}    	
    	}
    }
    
    public void callPullFromActiveLayers(){
    	//get top species and list of env/ctx layers
    	String species = layersUtil.getFirstSpeciesLayer();
    	String [] layers = layersUtil.getActiveEnvCtxLayers();    	
    	
    	/* set species from layer selector */
    	if (species != null) {
    		sac.setValue(species);
    	}
    	
    	/* set as selected each envctx layer found */
    	if (layers != null) {
	    	List<Listitem> lis = lbenvlayers.getItems();
	    	for (int i = 0; i < lis.size(); i++) {
	    		for (int j = 0; j < layers.length; j++) {
	    			if(lis.get(i).getLabel().equalsIgnoreCase(layers[j])) {
	    				lbenvlayers.addItemToSelection(lis.get(i));
	    				break;
	    			}
	    		}
	    	}    	
    	}
    }  
    
}
