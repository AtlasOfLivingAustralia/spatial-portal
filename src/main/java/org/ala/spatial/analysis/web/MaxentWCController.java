package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
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
    private SpeciesAutoComplete sac;
    Tabbox tabboxmaxent;
    private Label status;
    private Label infourl;
    private Button btnInfo;
    EnvironmentalList lbListLayers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Window maxentInfoWindow;
    private MapComposer mc;
    private String geoServer = null;
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    LayersUtil layersUtil;
    private String pid;
    String taxon;

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
            mc = getThisMapComposer();
            if (settingsSupplementary != null) {
                geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
                satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
            }

            layersUtil = new LayersUtil(mc, satServer);

            //setupEnvironmentalLayers();
            lbListLayers.init(mc, satServer, true);

        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    public void onChange$sac(Event event) {
        loadSpeciesOnMap();
    }

    public void onClick$btnMapSpecies(Event event) {
        try {
            loadSpeciesOnMap();

        } catch (Exception ex) {
            System.out.println("Got an error clicking button!!");
            ex.printStackTrace(System.out);
        }
    }

    public void onDoInit(Event event) throws Exception {
        runmaxent();
        //Clients.showBusy("", false);
    }

    public void produce() {
        onClick$startmaxent(null);
    }

    public void onClick$startmaxent(Event event) {
        try {
            onDoInit(null);
        } catch (Exception e) {
        }
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
            if (lbListLayers.getSelectedCount() > 50) {
                Messagebox.show(lbListLayers.getSelectedCount() + " layers selected.  Please select fewer than 50 environmental layers in step 2.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                tabboxmaxent.setSelectedIndex(1);
                return;
            }

            if (LayersUtil.isPestSpecies(taxon)) {
                Messagebox.show("Warning: Invasive species will rarely be in equilibrium with the environment at their observed locations so modelling distributions should only be attempted by experienced analysts.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
            }

            if (layersUtil.isSensitiveSpecies(taxon).equals("1")) {
                Messagebox.show("Warning: There are sensitive records within the dataset that you've chosen to model. Please select non-sensitive species for MaxEnt. For more information on the sensitive species, please refer to http://www.ala.org.au/about/program-of-projects/sds/", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }


            String msg = "";
            String[] envsel = null;
            StringBuffer sbenvsel = new StringBuffer();

            status.setValue("Status: Running Maxent, please wait... ");
            btnInfo.setVisible(false);

            String[] selectedLayers = lbListLayers.getSelectedLayers();
            if (selectedLayers.length > 0) {
                envsel = new String[selectedLayers.length];
                msg = "Selected " + selectedLayers.length + " items \n ";
                for (int i = 0; i < selectedLayers.length; i++) {
                    sbenvsel.append(selectedLayers[i]);
                    if (i < selectedLayers.length - 1) {
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
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon.replace(".", "__"), "UTF-8"));
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
            get.addParameter("area", area);
            //  get.addRequestHeader("Content-type", "application/json");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();
            this.taxon = taxon;

            openProgressBar();

            StringBuffer sbParams = new StringBuffer();
            sbParams.append("Species: " + taxon);
            sbParams.append("Jackknife: " + chkJackknife.isChecked());
            sbParams.append(";Response curves: " + chkRCurves.isChecked());
            sbParams.append(";Test per: " + txtTestPercentage.getValue());


            Map attrs = new HashMap();
            attrs.put("actionby", "user");
            attrs.put("actiontype", "analysis");
            attrs.put("lsid", taxon);
            attrs.put("useremail", "spatialuser");
            attrs.put("processid", pid);
            attrs.put("sessionid", "");
            attrs.put("layers", sbenvsel.toString());
            attrs.put("method", "maxent");
            attrs.put("params", sbParams.toString());
            attrs.put("downloadfile", "");
            mc.updateUserLog(attrs, "analysis result: " + satServer + "/alaspatial" + "/output/maxent/" + pid + "/species.html");

            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }
    }
    Window wInputBox;

    public void previousModel() {
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
                pid = ((Textbox) wInputBox.getFellow("txtBox")).getValue();
                pid = pid.trim();
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

    void openProgressBar() {
        if (maxentInfoWindow != null) {
            maxentInfoWindow.detach();
        }
        MaxentProgressWCController window = (MaxentProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisMaxentProgress.zul", this, null);
        window.parent = this;
        window.start(pid);
        try {
            window.doModal();
        } catch (Exception e) {
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

    public void loadMap(Event event) {

        String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = geoServer
                + "/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=20"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        System.out.println(legendurl);

        //get job inputs
        String speciesName = "";
        try {
            for (String s : getJob("inputs").split(";")) {
                if (s.startsWith("scientificName")) {
                    speciesName = s.split(":")[1];
                    if (speciesName != null && speciesName.length() > 1) {
                        speciesName = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (taxon == null) {
            taxon = "species";
        }

        String layername = "Maxent model for " + speciesName;
        mc.addWMSLayer(layername, mapurl, (float) 0.5, "", legendurl);
        MapLayer ml = mc.getMapLayer(layername);
        String infoUrl = satServer + "/alaspatial" + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        if (md == null) {
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setMoreInfo(infoUrl + "\nMaxent Output");

        getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        showInfoWindow("/output/maxent/" + pid + "/species.html");

    }

    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
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

        //String taxon = sac.getValue();
        taxon = sac.getValue(); 
        String rank = "";
      
        String spVal = sac.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

            if (rank.equalsIgnoreCase("scientific name")) {
                rank = "taxon";
            }
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
        }
        System.out.println("mapping rank and species: " + rank + " - " + taxon);
        mc.mapSpeciesByLsid((String) (sac.getSelectedItem().getAnnotatedProperties().get(0)), taxon, rank);

    }
    

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

        System.out.println("Starting with cleanTaxon: " + sac.getValue());

        // make the sac.getValue() a selected value if it appears in the list
        // - fix for common names entered but not selected
        if (sac.getSelectedItem() == null) {
            List list = sac.getItems();
            for (int i = 0; i < list.size(); i++) {
                Comboitem ci = (Comboitem) list.get(i);
                if (ci.getLabel().equalsIgnoreCase(sac.getValue())) {
                    System.out.println("cleanTaxon: set selected item");
                    sac.setSelectedItem(ci);
                    break;
                }
            }
        }

        if (sac.getSelectedItem() != null && sac.getSelectedItem().getAnnotatedProperties() != null) {
            taxon = (String) sac.getSelectedItem().getAnnotatedProperties().get(0);
        }

        return taxon;
    }

    /**
     * populate sampling screen with values from active layers
     * 
     */
    public void callPullFromActiveLayers() {
        //get top species and list of env/ctx layers
        //String species = layersUtil.getFirstSpeciesLayer();
        String speciesandlsid = layersUtil.getFirstSpeciesLsidLayer();
        String species = null;
        String lsid = null;
        if (StringUtils.isNotBlank(speciesandlsid)) {
            species = speciesandlsid.split(",")[0];
            lsid = speciesandlsid.split(",")[1];
        }
        if (StringUtils.isNotBlank(lsid)) {
            taxon = lsid; 
        }
        String[] layers = layersUtil.getActiveEnvCtxLayers();


        /* set species from layer selector */
        if (species != null) {
            String tmpSpecies = species;
            if (species.contains(" (")) {
                tmpSpecies = StringUtils.substringBefore(species, " (");
            }
            sac.setValue(tmpSpecies);
            sac.refresh(tmpSpecies);
        }

        /* set as selected each envctx layer found */
        if (layers != null) {
            lbListLayers.selectLayers(layers);
        }

        lbListLayers.updateDistances();
    }
}
