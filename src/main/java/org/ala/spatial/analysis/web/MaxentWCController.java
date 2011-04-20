package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
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
    EnvironmentalList lbListLayers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    LayersUtil layersUtil;
    private String pid;
    String taxon;    
    Window wInputBox;

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
            layersUtil = new LayersUtil(getMapComposer(), CommonData.satServer);
            
            lbListLayers.init(getMapComposer(), CommonData.satServer, true);
        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    public void onChange$sac(Event event) {
        loadSpeciesOnMap();
    }

    public void onClick$btnRunMaxent(Event event) {
        runmaxent();
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

            if (isSensitiveSpecies(taxon)) {
                return;
            }
            
            StringBuffer sbenvsel = new StringBuffer();

            String[] selectedLayers = lbListLayers.getSelectedLayers();
            if (selectedLayers.length > 0) {
                for (int i = 0; i < selectedLayers.length; i++) {
                    sbenvsel.append(selectedLayers[i]);
                    if (i < selectedLayers.length - 1) {
                        sbenvsel.append(":");
                    }
                }
            }

            System.out.println("Selected species: " + taxon);
            System.out.println("Selected env vars");
            System.out.println(sbenvsel.toString());
            System.out.println("Selected options: ");
            System.out.println("Jackknife: " + chkJackknife.isChecked());
            System.out.println("Response curves: " + chkRCurves.isChecked());
            System.out.println("Test per: " + txtTestPercentage.getValue());

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/maxent/processgeoq?");
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
            String area = null;//getMapComposer().getSelectionArea();
            if (area == null || area.length() == 0) {                
                area = "none";
            }


            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);
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
            getMapComposer().updateUserLog(attrs, "analysis result: " + CommonData.satServer + "/alaspatial" + "/output/maxent/" + pid + "/species.html");
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }
    }
    
    public void onClick$btnPreviousMaxent(){
        previousModel();
    }

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
                getParameters();
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
        MaxentProgressWCController window = (MaxentProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisMaxentProgress.zul", this, null);
        //window.parent = this;
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
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

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

        String mapurl = CommonData.geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.geoServer
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
        getMapComposer().addWMSLayer(layername, mapurl, (float) 0.5, "", legendurl, LayerUtilities.MAXENT);
        MapLayer ml = getMapComposer().getMapLayer(layername);
        String infoUrl = CommonData.satServer + "/alaspatial" + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        if (md == null) {
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:"+pid);
        md.setId(Long.valueOf(pid));

        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        showInfoWindow("/output/maxent/" + pid + "/species.html");
    }

    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
    }

    private void showInfoWindow(String url) {
        String infoUrl = CommonData.satServer + "/alaspatial" + url;
        Events.echoEvent("openUrl", this.getMapComposer(), infoUrl + "\nMaxent output\npid:"+pid);
    }
   
    private void loadSpeciesOnMap() {

        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (sac.getSelectedItem() == null) {
            return;
        }
        
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
        String lsid = (String) sac.getSelectedItem().getAnnotatedProperties().get(0);
        if (!isSensitiveSpecies(lsid)) {
            System.out.println("mapping rank and species: " + rank + " - " + taxon);
            getMapComposer().mapSpeciesByLsid(lsid, taxon, rank, 0);
        }
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
            isSensitiveSpecies(lsid);
        }

        /* set as selected each envctx layer found */
        if (layers != null) {
            lbListLayers.selectLayers(layers);
        }

        lbListLayers.updateDistances();
    }

    void getParameters() {
        String txt = get("inputs");
        try {
            int pos = 0;
            int p1 = txt.indexOf("pid:", pos);
            if (p1 < 0) {
                return;
            }
            int p2 = txt.indexOf("taxonid:", pos);
            int p3 = txt.indexOf("scientificName:", pos);
            int p4 = txt.indexOf("taxonRank:", pos);
            int p5 = txt.indexOf("area:", pos);
            int p6 = txt.indexOf("envlist:", pos);
            int p7 = txt.indexOf("txtTestPercentage:", pos);
            int p8 = txt.indexOf("chkJackknife:", pos);
            int p9 = txt.indexOf("chkResponseCurves:", pos);
            int p10 = txt.length();

            String pid = txt.substring(p1 + "pid:".length(), p2).trim();
            String taxonid = txt.substring(p2 + "taxonid:".length(), p3).trim();
            String scientificName = txt.substring(p3 + "scientificName:".length(), p4).trim();
            String taxonRank = txt.substring(p4 + "taxonRank:".length(), p5).trim();
            String area = txt.substring(p5 + "area:".length(), p6).trim();
            String envlist = txt.substring(p6 + "envlist:".length(), p7).trim();
            String txtTestPercentage = txt.substring(p7 + "txtTestPercentage".length(), p8).trim();
            String chkJackknife = txt.substring(p8 + "chkJackknife".length(), p9).trim();
            String chkResponseCurves = txt.substring(p9 + "chkResponseCurves".length(), p10).trim();

            if (taxonid.endsWith(";")) {
                taxonid = taxonid.substring(0, taxonid.length() - 1);
            }
            if (scientificName.endsWith(";")) {
                scientificName = scientificName.substring(0, scientificName.length() - 1);
            }
            if (taxonRank.endsWith(";")) {
                taxonRank = taxonRank.substring(0, taxonRank.length() - 1);
            }
            if (area.endsWith(";")) {
                area = area.substring(0, area.length() - 1);
            }
            if (envlist.endsWith(";")) {
                envlist = envlist.substring(0, envlist.length() - 1);
            }
            if (txtTestPercentage.endsWith(";")) {
                txtTestPercentage = txtTestPercentage.substring(0, txtTestPercentage.length() - 1);
            }
            if (chkJackknife.endsWith(";")) {
                chkJackknife = chkJackknife.substring(0, chkJackknife.length() - 1);
            }
            if (chkResponseCurves.endsWith(";")) {
                chkResponseCurves = chkResponseCurves.substring(0, chkResponseCurves.length() - 1);
            }

            System.out.println("got [" + pid + "][" + taxonid + "][" + scientificName + "][" + taxonRank + "][" + area + "][" + envlist + "][" + txtTestPercentage + "][" + chkJackknife + "][" + chkResponseCurves + "]");

            //apply job input parameters to selection
            sac.setValue(scientificName);
            sac.refresh(scientificName);
            cleanTaxon();

            lbListLayers.clearSelection();
            lbListLayers.selectLayers(envlist.split(":"));
            try {
                this.txtTestPercentage.setValue(String.valueOf(Double.parseDouble(txtTestPercentage)));
            } catch (Exception e) {
            }

            this.chkJackknife.setChecked(chkJackknife.equalsIgnoreCase("on"));
            this.chkRCurves.setChecked(chkResponseCurves.equalsIgnoreCase("on"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/jobs/").append(type).append("?pid=").append(pid);

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isSensitiveSpecies(String taxon) {
        try {

            if (LayersUtil.isPestSpecies(taxon) || layersUtil.isSensitiveSpecies(taxon).equals("1")) {
                Messagebox.show("Warning: Prediction is disabled for sensitive species where access to precise location information is restricted. Sensitive species include those that are endangered or threatened, pests or potential threats to Australian agriculture or other industries.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                tabboxmaxent.setSelectedIndex(0);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return true;
        }

        return false;
    }
}
