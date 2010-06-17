package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchResult;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.ala.spatial.util.Layer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Iframe;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ajay
 */
public class MaxentWCController extends UtilityComposer {

    private static final long serialVersionUID = 165701023268014945L;
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
    private Radio rdoCommonSearch;
    private SpeciesAutoComplete sac;
    private Button btnMapSpecies;
    private Label status;
    private Label infourl;
    private Button btnInfo;
    private Textbox tbenvfilter;
    private Button btenvfilterclear;
    private Listbox lbenvlayers;
    private Button startmaxent;
    private List<Layer> _layers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Textbox trun;
    //private SpatialSettings ssets;
    private Tabbox outputtab;
    private Iframe mapframe;
    private Iframe infoframe;
    private Window maxentWindow;
    private Window maxentInfoWindow;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;
    private MapComposer mc;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    LayersUtil layersUtil;
    Checkbox useArea;

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

            setupEnvironmentalLayers();
        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
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
    }

    public void onCheck$rdoCommonSearch() {
        sac.setSearchCommon(true);
        sac.getItems().clear();
    }

    public void onCheck$rdoScientificSearch() {
        sac.setSearchCommon(false);
        sac.getItems().clear();
    }

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
    public void onChanging$tbenvfilter(InputEvent event) {
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
    }

    /**
     * Clear the filter text box
     *
     * @param event The event attached to the component
     */
    public void onClick$btenvfilterclear(Event event) {
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
    }

    public void onDoInit(Event event) throws Exception {
        runmaxent();
        Clients.showBusy("", false);
    }

    public void onClick$startmaxent(Event event) {
        Clients.showBusy("Maxent running...", true);
        Events.echoEvent("onDoInit", this, event.toString());
    }

    public void runmaxent() {
        try {
            String taxon = cleanTaxon(sac.getValue());
            // check if its a common name, if so, grab the scientific name
            // if (rdoCommonSearch.isChecked()) {
            //    taxon = getScientificName();
            // }

            String msg = "";
            String[] envsel = null;
            StringBuffer sbenvsel = new StringBuffer();

            status.setValue("Status: Running Maxent, please wait... ");
            btnInfo.setVisible(false);

            if (lbenvlayers.getSelectedCount() > 0) {
                envsel = new String[lbenvlayers.getSelectedCount()];
                msg = "Selected " + lbenvlayers.getSelectedCount() + " items \n ";
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
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
            sbProcessUrl.append(satServer + "/alaspatial/ws/maxent/processgeo?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
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
            if (useArea.isChecked()) {
                user_polygon = convertGeoToPoints(mc.getSelectionArea());
            } else {
                user_polygon = "";
            }
            System.out.println("user_polygon: " + user_polygon);
            if (user_polygon.length() > 0) {
                sbProcessUrl.append("&points=" + URLEncoder.encode(user_polygon, "UTF-8"));
            } else {
                sbProcessUrl.append("&points=" + URLEncoder.encode("none", "UTF-8"));
            }


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from MaxentWSController: \n" + slist);

            String[] maxentresponse = slist.split(";");
            String[] status = maxentresponse[0].split(":");
            String[] pid = maxentresponse[1].split(":");
            String[] info = maxentresponse[2].split(":");

            this.status.setValue("Status: " + status[1]);
            if (status[1].equalsIgnoreCase("success")) {
                String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid[1] + "&styles=alastyles&srs=EPSG:4326&TRANSPARENT=true&FORMAT=image%2Fpng";

                String legendurl = geoServer
                        + "/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=20"
                        + "&LAYER=ALA:species_" + pid[1]
                        + "&STYLE=alastyles";

                System.out.println(legendurl);

                //get the current MapComposer instance
                //MapComposer mc = getThisMapComposer();

                mc.addWMSLayer("Maxent model for " + taxon, mapurl, (float) 0.5, "", legendurl);

                if (info.length == 2) {
                    infourl.setValue("Show process information");
                    showInfoWindow(info[1]);
                }

                infourl.setValue(info[1]);
                btnInfo.setVisible(true);

            } else {
                Messagebox.show("Unable to process Maxent", "Maxent", Messagebox.OK, Messagebox.INFORMATION);
            }

            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }


    }

    public void onClick$btnInfo(Event event) {
        try {
            showInfoWindow(infourl.getValue());
        } catch (Exception e) {
            System.out.println("opps");
        }
    }

    private void showInfoWindow(String url) {
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
        maxentInfoWindow.doOverlapped();
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
        if (sac.getSelectedItem() == null) return;


        String taxon = sac.getValue();

        String spVal = sac.getSelectedItem().getDescription();
        if (spVal.trim().startsWith("Scientific")) {
            //myci.setValue(spVal[1].trim().substring(spVal[1].trim().indexOf(":")).trim());
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim();
            mc.mapSpeciesByName(taxon, sac.getValue());
        } else {
            mc.mapSpeciesByName(taxon);
        }
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
     * get rid of the common name if present
     *
     * @param taxon
     * @return
     */
    private String cleanTaxon(String taxon) {
        if (StringUtils.isNotBlank(taxon)) {

            if (taxon.contains(" (")) {
                taxon = StringUtils.substringBefore(taxon, " (");
            }
        }

        return taxon;
    }

    /**
     * populate sampling screen with values from active layers
     * 
     * TODO: run this on 'tab' open
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
            List<Listitem> lis = lbenvlayers.getItems();
            for (int i = 0; i < lis.size(); i++) {
                for (int j = 0; j < layers.length; j++) {
                    if (lis.get(i).getLabel().equalsIgnoreCase(layers[j])) {
                        lbenvlayers.addItemToSelection(lis.get(i));
                        break;
                    }
                }
            }
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
}
