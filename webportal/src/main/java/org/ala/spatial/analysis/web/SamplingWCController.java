package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkmax.zul.Filedownload;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Listgroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleGroupsModel;

/**
 *
 * @author ajay
 */
public class SamplingWCController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    private SpeciesAutoComplete sac;
    private Listbox lbenvlayers;
    private Popup p;
    private Html h;
    private List layers;
    //private Checkbox useArea;
    private Map layerdata;
    private String selectedLayer;
    private MapComposer mc;
    private String satServer = "";
    private SettingsSupplementary settingsSupplementary = null;
    private String user_polygon = "";
    private String[] groupLabels = null;
    //String previousArea = "";
    LayersUtil layersUtil;

    @Override
    public void afterCompose() {
        super.afterCompose();

        mc = getThisMapComposer();
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        layersUtil = new LayersUtil(mc, satServer);

        layers = new Vector();
        layerdata = new Hashtable<String, String[]>();

        String[][] datas = new String[][]{
            setupEnvironmentalLayers(),
            setupContextualLayers()
        };

        lbenvlayers.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data) {
                try {
                    String layername = (String) data;
                    li.setWidth(null);
                    Listcell lc = new Listcell(layername);
                    lc.setParent(li);

                    /* onclick event for popup content update */
                    lc.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            showLayerExtents(event.getTarget());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        });
        groupLabels = new String[]{"Environmental", "Contextual"};
        lbenvlayers.setModel(new SimpleGroupsModel(datas, groupLabels));

        // need to renderAll() as being a group list
        // items hidden initially weren't being selected
        // when selecting all layers without scrolling
        lbenvlayers.renderAll();

        // disable the checkboxes for the groups
        List groups = lbenvlayers.getGroups();
        Iterator<Listgroup> itGroups = groups.iterator();
        while (itGroups.hasNext()) {
            Listgroup lg = itGroups.next();
            System.out.println("ListGroup: " + lg.getLabel() + " - " + lg.isListenerAvailable("select", true));
            lg.setCheckable(false);
        }
    }

    private void showLayerExtents(Object o) {
        Listcell lc = (Listcell) o;

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
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + selectedLayer + "/extents");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            slist = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        h.setContent(slist);

        p.open(l);
    }

    public void onChange$sac(Event event) {
        loadSpeciesOnMap();
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
    private String[] setupEnvironmentalLayers() {
        String[] aslist = null;
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
    private String[] setupContextualLayers() {
        String[] aslist = null;
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
        SamplingResultsWCController window = (SamplingResultsWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingResults.zul", this, null);
        window.parent = this;

        try {

            String taxon = cleanTaxon(sac.getValue());
            // check if its a common name, if so, grab the scientific name
            //if (rdoCommonSearch.isChecked()) {
            //    taxon = getScientificName();
            //}

            StringBuffer sbenvsel = new StringBuffer();

            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    if (li.getLabel() == null) {
                        // seems to be null, shouldn't be, but let's ignore it
                        continue;
                    } else if (ArrayUtils.contains(groupLabels, li.getLabel())) {
                        // this must be the group header, let's ignore it
                        continue;
                    }

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
            if (true) { //an area always exists; useArea.isChecked()) {
                user_polygon = mc.getSelectionArea();
            } else {
                user_polygon = "";
            }
            System.out.println("user_polygon: " + user_polygon);
            String area = "none";
            if (user_polygon.length() > 0) {
                //sbProcessUrl.append("&area=" + URLEncoder.encode(user_polygon, "UTF-8"));
                area = user_polygon;
            } else {
                //sbProcessUrl.append("&area=" + URLEncoder.encode("none", "UTF-8"));
            }

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
//            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: " + result + "\n" + slist);

            //error condition, for example, when no combobox item is selected
            if (result != 200) {
                mc.showMessage("no records available");
                window.detach();
                return;
            }

            String[] aslist = slist.split(";");
            System.out.println("Result count: " + aslist.length);
            int count = 0;
            for (int i = 0; i < aslist.length; i++) {
                String[] rec = aslist[i].split("~");
                if (rec.length > 0) {
                    count++;
                }
            }
            count--; //don't include header in count

            if (slist.trim().length() == 0 || count == 0) {
                mc.showMessage("No records available for selected criteria.");

                window.detach();
                return;
            }

            //don't count header

            window.doModal();

            if (count == 1) {
                window.samplingresultslabel.setValue("preview: 1 record");
            } else {
                window.samplingresultslabel.setValue("preview: " + count + " records");
            }


            // load into the results popup
            int j;

            /* map of top row, contextual columns data lists for value lookups */
            Map contextualLists = new Hashtable<Integer, String[]>();

            // add rows
            String[] top_row = null;
            for (int i = 0; i < aslist.length; i++) {
                if (i == 0) {
                    top_row = aslist[i].split("~");

                    for (int k = 0; k < top_row.length; k++) {
                        if (isContextual(top_row[k])) {
                            try {
                                String layername = top_row[k].trim().replaceAll(" ", "_");
                                client = new HttpClient();
                                GetMethod getmethod = new GetMethod(satServer + "/alaspatial/ws/spatial/settings/layer/" + layername + "/extents"); // testurl
                                getmethod.addRequestHeader("Accept", "text/plain");

                                result = client.executeMethod(getmethod);
                                String[] salist = getmethod.getResponseBodyAsString().split("<br>");
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
                r.setParent(window.results_rows);
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
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception calling sampling.preview:");
            e.printStackTrace(System.out);
        }
    }

    public void download() {
        try {

            String taxon = cleanTaxon(sac.getValue());

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
            if (true) { //an area always exists; useArea.isChecked()) {
                user_polygon = mc.getSelectionArea();
            } else {
                user_polygon = "";
            }
            System.out.println("user_polygon: " + user_polygon);
            String area;
            if (user_polygon.length() > 0) {
                //sbProcessUrl.append("&area=" + URLEncoder.encode(user_polygon, "UTF-8"));
                area = user_polygon;
            } else {
                //sbProcessUrl.append("&area=" + URLEncoder.encode("none", "UTF-8"));
                area = "none";
            }

            //String testurl = satServer + "/alaspatial/ws/sampling/test";

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from SamplingWSController: \n" + slist);


            if (slist.equalsIgnoreCase("")) {
                Messagebox.show("Unable to download sample file. Please try again", "ALA Spatial Analysis Toolkit - Sampling", Messagebox.OK, Messagebox.ERROR);
            } else {
                System.out.println("Sending file to user: " + satServer + "/alaspatial" + slist);
                Filedownload.save(new URL(satServer + "/alaspatial" + slist), "application/zip");
            }
        } catch (Exception e) {
            System.out.println("Exception calling sampling.download:");
            e.printStackTrace(System.out);
        }
    }

    public void onClick$btnDownload(Event event) {
        download();
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
            mc.mapSpeciesByName(taxon, sac.getValue());
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
            //mc.mapSpeciesByName(taxon);
            mc.mapSpeciesByNameRank(taxon, rank, null);
        }
        //taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        //mc.mapSpeciesByName(taxon);
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
    private String cleanTaxon(String taxon) {
        //make the sac.getValue() a selected value if it appears in the list
        // - fix for common names entered but not selected
        if (sac.getSelectedItem() == null) {
            List list = sac.getItems();
            for (int i=0;i<list.size();i++) {
                Comboitem ci = (Comboitem) list.get(i);
                if (ci.getLabel().equalsIgnoreCase(taxon)) {
                    System.out.println("cleanTaxon: set selected item");
                    sac.setSelectedItem(ci);
                    break;
                }
            }
        }

        if (StringUtils.isNotBlank(taxon)) {

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

        }

        return taxon;
    }

    /**
     * populate sampling screen with values from active layers and area tab
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
