package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class MaxentWCController extends UtilityComposer {

    private static final long serialVersionUID = 165701023268014945L;
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String SAT_URL = "sat_url";
    private SpeciesAutoComplete sac;
    Tabbox tabboxmaxent;
    private Label status;
    private Label infourl;
    private Button btnInfo;
    private Textbox tbenvfilter;
    //private Listbox lbenvlayers;
    EnvironmentalList lbListLayers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Window maxentInfoWindow;
    private MapComposer mc;
    private String geoServer = "http://spatial-dev.ala.org.au";  // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    LayersUtil layersUtil;
    private String pid;
    String taxon;

    //Checkbox useArea;
    //String previousArea = "";

    @Override
    public void doAfterCompose(Component component) throws Exception {
        super.doAfterCompose(component);
    }

    /**
     * When the page is loaded, setup the various settings that are needed
     * throughtout the page action
     *
     * @param comp The page component itself
     * @throws Exception
     */
    @Override
    public void afterCompose() {
        super.afterCompose();

        try {
            //Messagebox.show("Hello world afterCompose!");
            mc = getThisMapComposer();
            if (settingsSupplementary != null) {
                geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
                satServer = settingsSupplementary.getValue(SAT_URL);
            }

            layersUtil = new LayersUtil(mc, satServer);

            //setupEnvironmentalLayers();
            lbListLayers.init(mc, satServer,true);

        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     *
    private void setupEnvironmentalLayers() {
        try {
            String[] aslist = layersUtil.getEnvironmentalLayers();

            if (aslist.length > 0) {

                lbenvlayers.setItemRenderer(new ListitemRenderer() {

                    public void render(Listitem li, Object data) {
                        li.setWidth(null);
                        new Listcell((String) data).setParent(li);
                    }
                });

                lbenvlayers.setModel(new SimpleListModel(aslist));
            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }*/
/*
    public void onCheck$rdoCommonSearch() {
        sac.setSearchCommon(true);
        sac.getItems().clear();
    }

    public void onCheck$rdoScientificSearch() {
        sac.setSearchCommon(false);
        sac.getItems().clear();
    }
*/
    public void onChange$sac(Event event) {
        loadSpeciesOnMap();
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

    /**
     * On changing the text box value, iterate thru' the listbox
     * and display only the matched options.
     *
     * @param event The event attached to the component
     */
    /*public void onChanging$tbenvfilter(InputEvent event) {
        String filter = event.getValue().toLowerCase();
        System.out.println("checking for: " + filter);
        System.out.print("Number of list items to iterate thru: ");
        System.out.println(lbenvlayers.getItems().size());
        for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
            if (li.getLabel().toLowerCase().contains(filter)) {
                if (!li.isVisible()) {
                    li.setVisible(true);
                }
            } else {
                if (li.isVisible()) {
                    li.setVisible(false);
                }
            }
        }
    }*/

    /**
     * Clear the filter text box
     *
     * @param event The event attached to the component
     */
    /*public void onClick$btenvfilterclear(Event event) {
        try {
            tbenvfilter.setValue("");
            for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
                li.setVisible(true);
            }
            //Messagebox.show("Cleared env list");
        } catch (Exception ex) {
            Logger.getLogger(MaxentWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Clicked on clear env list button");
        }
    }*/

    public void onDoInit(Event event) throws Exception {
        runmaxent();
        //Clients.showBusy("", false);
    }

    public void produce() {
        onClick$startmaxent(null);
    }

    public void onClick$startmaxent(Event event) {
        //Clients.showBusy("Maxent running...", true);
        //Events.echoEvent("onDoInit", this, (event==null)? null : event.toString());
        try{onDoInit(null);}catch(Exception e){}
    }

    public void runmaxent() {
        try {
            String taxon = cleanTaxon();
            
            if (taxon == null || taxon.equals("")) {
                Messagebox.show("Please select a species in step 1.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                //highlight step 1
                tabboxmaxent.setSelectedIndex(0);
                return;
            }

            if (lbListLayers.getSelectedCount() <= 0) {
                Messagebox.show("Please select one or more layers in step 2.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                //highlight step 2
                tabboxmaxent.setSelectedIndex(1);
                return;
            }
            if(lbListLayers.getSelectedCount() > 100){
                Messagebox.show(lbListLayers.getSelectedCount() + " layers selected.  Please select fewer than 100 environmental layers in step 2.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                tabboxmaxent.setSelectedIndex(1);
                return;
            }

            if(LayersUtil.isPestSpecies(taxon)){
                Messagebox.show("arning: Invasive species will rarely be in equilibrium with the environment at their observed locations so modelling distributions should only be attempted by experienced analysts.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
            }


            String msg = "";
            String[] envsel = null;
            StringBuffer sbenvsel = new StringBuffer();

            status.setValue("Status: Running Maxent, please wait... ");
            btnInfo.setVisible(false);

            String [] selectedLayers = lbListLayers.getSelectedLayers();
            if (selectedLayers.length > 0) {
                envsel = new String[selectedLayers.length];
                msg = "Selected " + selectedLayers.length + " items \n ";
                for(int i=0;i<selectedLayers.length;i++){
                    sbenvsel.append(selectedLayers[i]);
                    if (i < selectedLayers.length -1) {
                        sbenvsel.append(":");
                    }
                }
            }

            //process(envsel);

            System.out.println("Selected species: " + taxon);
            System.out.println("Selected env vars");
            System.out.println(sbenvsel.toString());
            System.out.println("Selected options: ");
            System.out.println("Jackknife: " + chkJackknife.isChecked());
            System.out.println("Response curves: " + chkRCurves.isChecked());
            System.out.println("Test per: " + txtTestPercentage.getValue());


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/maxent/processgeoq?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon.replace(".","__"), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());

            /* user selected region support */
            String user_polygon;
            if (true) { //an area always exists; useArea.isChecked()) {
                user_polygon = mc.getSelectionArea();
            } else {
                user_polygon = "";
            }
            System.out.println("user_polygon: " + user_polygon);
            String area;
            if (user_polygon.length() > 0) {
                //sbProcessUrl.append("&area=" + URLEncoder.encode(user_polygon, "UTF-8"));
                area = user_polygon;//URLEncoder.encode(user_polygon,"UTF-8");
            } else {
                //sbProcessUrl.append("&area=" + URLEncoder.encode("none", "UTF-8"));
                area = "none";
            }


            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString());
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area",area);
          //  get.addRequestHeader("Content-type", "application/json");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();
            this.taxon = taxon;

            openProgressBar();

            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }
    }
    Window wInputBox;
    public void previousModel(){
        wInputBox = new Window("Enter reference number", "normal", false);
        wInputBox.setWidth("300px");
        wInputBox.setClosable(true);
        Textbox t = new Textbox();
        t.setId("txtBox");
        t.setWidth("280px");
        t.setParent(wInputBox);
        Button b = new Button();
        b.setLabel("Ok");
        b.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    pid = ((Textbox)wInputBox.getFellow("txtBox")).getValue();
                    taxon = "";
                    openProgressBar();
                    wInputBox.detach();
                }
            });
        b.setParent(wInputBox);
        wInputBox.setParent(getMapComposer().getFellow("mapIframe").getParent());
        wInputBox.setPosition("top,center");
        try {
            wInputBox.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void openProgressBar(){
        if(maxentInfoWindow != null){
            maxentInfoWindow.detach();
        }
        MaxentProgressWCController window = (MaxentProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisMaxentProgress.zul", this, null);
        window.parent = this;
        window.start(pid);
        try{
            window.doModal();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    String getJob(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println(slist);
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void loadMap(Event event){

        String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = geoServer
                + "/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=20"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        System.out.println(legendurl);

        //get job inputs
        String speciesName = "";
        try{
            for(String s : getJob("inputs").split(";")) {
                if(s.startsWith("scientificName")){
                    speciesName = s.split(":")[1];
                    if(speciesName != null && speciesName.length() > 1){
                        speciesName = speciesName.substring(0,1).toUpperCase() + speciesName.substring(1);
                    }
                    break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        if(taxon == null){
            taxon = "species";
        }

        String layername = "Maxent model for " + speciesName;
        mc.addWMSLayer(layername, mapurl, (float) 0.5, "", legendurl);
        MapLayer ml = mc.getMapLayer(layername);
        String infoUrl = satServer + "/alaspatial" + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        if(md == null){
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setMoreInfo(infoUrl + "\nMaxent Output");

        getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        showInfoWindow("/output/maxent/" + pid + "/species.html");

        /*infourl.setValue("Show process information");
        showInfoWindow("/output/maxent/" + pid + "/species.html");

        infourl.setValue("/output/maxent/" + pid + "/species.html");
        btnInfo.setVisible(true);*/
    }

    public void onClick$btnInfo(Event event) {
        try {
            showInfoWindow(infourl.getValue());
        } catch (Exception e) {
            System.out.println("opps");
        }
    }

    private void showInfoWindow(String url) {
        String infoUrl = satServer + "/alaspatial" + url;

        Events.echoEvent("openUrl", this.getMapComposer(), infoUrl + "\nMaxent output");

        //Executions.getCurrent().sendRedirect(infoUrl, "_blank");

        /*
        Map args = new Hashtable();
        args.put("url", satServer + "/alaspatial" + url);
        if (maxentInfoWindow == null) {
            maxentInfoWindow = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisMaxentInfo.zul", this, args);
        } else {
            maxentInfoWindow.detach();
            maxentInfoWindow = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisMaxentInfo.zul", this, args);
        }

        maxentInfoWindow.setId(java.util.UUID.randomUUID().toString());
        maxentInfoWindow.setMaximizable(true);
        maxentInfoWindow.setPosition("center");
        maxentInfoWindow.doOverlapped();*/
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

        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (sac.getSelectedItem() == null) {
            return;
        }


        String taxon = sac.getValue();
        String rank = "";
        String spVal = sac.getSelectedItem().getDescription();
        if (spVal.trim().startsWith("Scientific name")) {
            //myci.setValue(spVal[1].trim().substring(spVal[1].trim().indexOf(":")).trim());
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim();
            rank = "common name";
            //mc.mapSpeciesByName(taxon, sac.getValue());
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
            //mc.mapSpeciesByName(taxon);
            //mc.mapSpeciesByNameRank(taxon, rank, null);
        }
        mc.mapSpeciesByLsid((String)(sac.getSelectedItem().getAnnotatedProperties().get(0)), taxon);
    }
/*
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
*/
    /**
     * get rid of the common name if present
     * 2 conditions here
     *  1. either species is automatically filled in from the Layer selector
     *     and is a common name and is in format Scientific name (Common name)
     *
     *  2. or user has searched for a common name from the analysis tab itself
     *     in which case we need to grab the scientific name for analysis
     *
     *  * condition 1 should also parse the proper taxon if its a genus, for eg
     *
     * @param taxon
     * @return
     */
    private String cleanTaxon() {

        if(sac.getSelectedItem() == null && sac.getValue() != null){
            sac.refresh(sac.getValue());
        }
        
        // make the sac.getValue() a selected value if it appears in the list
        // - fix for common names entered but not selected
        if (sac.getSelectedItem() == null) {
            List list = sac.getItems();
            for (int i=0;i<list.size();i++) {
                Comboitem ci = (Comboitem) list.get(i);
                if (ci.getLabel().equalsIgnoreCase(sac.getValue())) {
                    System.out.println("cleanTaxon: set selected item");
                    sac.setSelectedItem(ci);
                    break;
                }
            }
        }
        
/*        if (StringUtils.isNotBlank(taxon)) {

            // check for condition 1
            System.out.println("Checking for cond.1: " + taxon);
            if (taxon.contains(" (")) {
                taxon = StringUtils.substringBefore(taxon, " (");
            }
            System.out.println("After checking for cond.1: " + taxon);

            // check for condition 2
            if (sac.getSelectedItem() != null) {
                String spVal = sac.getSelectedItem().getDescription();
                System.out.println("Checking for cond.2: " + taxon + " -- " + spVal);
                if (spVal.trim().startsWith("Scientific name")) {
                    //myci.setValue(spVal[1].trim().substring(spVal[1].trim().indexOf(":")).trim());
                    taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim();
                }
                System.out.println("After checking for cond.2: " + taxon);
            }

        }*/

        if(sac.getSelectedItem() != null && sac.getSelectedItem().getAnnotatedProperties() != null){
            taxon = (String)sac.getSelectedItem().getAnnotatedProperties().get(0);
        }

        return taxon;
    }

    /**
     * populate sampling screen with values from active layers
     * 
     */
    public void callPullFromActiveLayers() {
        //get top species and list of env/ctx layers
        String species = layersUtil.getFirstSpeciesLayer();
        String[] layers = layersUtil.getActiveEnvCtxLayers();


        /* set species from layer selector */
        if (species != null) {
            sac.setValue(species);
        }

        /* set as selected each envctx layer found */
        if (layers != null) {
            lbListLayers.selectLayers(layers);
        }

        /*//an area always exists;  validate the area box presence, check if area updated
        String currentArea = mc.getSelectionArea();
        if (currentArea.length() > 0) {
            useArea.setDisabled(false);
            if (!currentArea.equalsIgnoreCase(previousArea)) {
                useArea.setChecked(true);
            }
        } else {
            useArea.setDisabled(true);
            useArea.setChecked(false);
        }
        previousArea = currentArea;*/
    }
}
