package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.GeoJSONUtilities;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabbox;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Row;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(FilteringResultsWCController.class);
    public Button download;
    public Button downloadsamples;
    //public Button refreshButton2;
    public Button mapspecies;
    //public Listbox popup_listbox_results;
    //public Label results_label;
    //public Label results_label2;
    public Label results_label2_occurrences;
    public Label results_label2_species;
    //public Label results_label_extra;
    public int results_pos;
    public String[] results = null;
    public String pid;
    String shape;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;
    Row rowUpdating;
    Row rowCounts;
    int results_count = 0;
    int results_count_occurrences = 0;

    @Override
    public void afterCompose() {
        super.afterCompose();

        // onClick$refreshButton2();

    }
    boolean addedListener = false;

    @Override
    public void redraw(Writer out) throws java.io.IOException {
        super.redraw(out);

        //results_label_extra.setValue("    [Updating...]");
        setUpdatingCount(true);

        System.out.println("redraw:filteringresultswccontroller");
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // refresh count may be required if area is
                    // not an envelope.
                    String area = getMapComposer().getSelectionArea();
                    if (!area.startsWith("ENVELOPE(") && !area.startsWith("LAYER(")) {
                        refreshCount();
                    }
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("filteringResults", el);

        }
        //results_label_extra.setValue("");
    }

    void setUpdatingCount(boolean set) {
        rowUpdating.setVisible(set);
        rowCounts.setVisible(!set);
    }

    public void populateList() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");

            String out = postInfo(sbProcessUrl.toString());
            //remove trailing ','
            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                out = out.substring(0, out.length() - 1);
            }
            results = out.split("\\|");
            java.util.Arrays.sort(results);

            if (results.length == 0 || results[0].trim().length() == 0) {
                //results_label.setValue("no species in area");
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                mapspecies.setVisible(false);
                results = null;
                //popup_listbox_results.setVisible(false);
                //results_label.setVisible(false);
                //refreshButton2.setVisible(true);
                return;
            }

            // results should already be sorted: Arrays.sort(results);
            int length = results.length;
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                //results_label.setValue("preview of first 200 species found");
            } else {
                //results_label.setValue("preview of all " + results.length + " species found");
            }

            /*popup_listbox_results.setModel(new ListModelArray(tmp, false));
            popup_listbox_results.setItemRenderer(
            new ListitemRenderer() {

            public void render(Listitem li, Object data) {
            String s = (String) data;
            String[] ss = s.split("[*]");


            Listcell lc = new Listcell(ss[0]);
            lc.setParent(li);

            if (ss.length > 1) {
            lc = new Listcell(ss[1]);
            lc.setParent(li);
            }

            if (ss.length > 2) {
            lc = new Listcell(ss[2]);
            lc.setParent(li);
            }

            if (ss.length > 3) {
            lc = new Listcell(ss[3]);
            lc.setParent(li);
            }
            }
            });          */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isTabOpen() {
        try {
            Tabbox analysisOptsTabbox =
                    (Tabbox) getParent() //htmlmacrocomponent
                    .getParent() //tabpanel
                    .getParent() //tabpanels
                    .getParent(); //tabbox

            if (analysisOptsTabbox.getSelectedTab().getIndex() == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void refreshCount() {
        //check if tab is open
        if (!isTabOpen() || !updateParameters()) {
            return;
        }

        //results_label2.setValue("    [Updating...]");
        setUpdatingCount(true);
        Events.echoEvent("onRefreshCount", this, null);
    }

    public void onRefreshCount(Event e) throws Exception {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/count");

            getMapComposer().updateUserLogAnalysis("species count", "area: " + shape, "", "species list in area");

            String[] out = postInfo(sbProcessUrl.toString()).split("\n");

            results_count = Integer.parseInt(out[0]);
            results_count_occurrences = Integer.parseInt(out[1]);

            setUpdatingCount(false);

            if (results_count == 0) {
                //results_label.setValue("no species in active area");
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                mapspecies.setVisible(false);
                results = null;
                //popup_listbox_results.setVisible(false);
                //results_label.setVisible(false);
                //refreshButton2.setVisible(true);
                return;
            }

            //results_label.setValue("species in active area: " + results_count);
            //results_label2.setValue(results_count + " (" + results_count_occurrences + " occurrences)");
            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));

            //hide results list, show 'preview list' button
            //popup_listbox_results.setVisible(false);
            //results_label.setVisible(false);
            //refreshButton2.setVisible(true);

            // toggle the map button
            if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
                mapspecies.setVisible(true);
            } else {
                mapspecies.setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onClick$download() {
        //preview species list
        SpeciesListResults window = (SpeciesListResults) Executions.createComponents("WEB-INF/zul/AnalysisSpeciesListResults.zul", this, null);
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$downloadsamples() {
        //  results_label_extra.setValue("    [Generating download...]");
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            String samplesfile = postInfo(sbProcessUrl.toString());

            //results_label_extra.setValue("");

            URL u = new URL(satServer + "/alaspatial/" + samplesfile);
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());

            String spid = pid;
            if (spid == null || spid.equals("none")) {
                spid = String.valueOf(System.currentTimeMillis());
            }

            Filedownload.save(u.openStream(), "application/zip", u.getFile());
            getMapComposer().updateUserLogAnalysis("Sampling", "", "", u.getFile(), pid, "Sampling download");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$downloadsamplesPreview() {
        //  results_label_extra.setValue("    [Generating download...]");
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        //validate with 'occurrences count'
        if (results_count_occurrences > settingsSupplementary.getValueAsInt("max_record_count_download")) {
            getMapComposer().showMessage(results_count_occurrences + " occurrences in the active area.  Cannot produce sample for more than "+settingsSupplementary.getValue("max_record_count_download")+" occurrences.");
            return;
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list/preview");

            String slist = postInfo(sbProcessUrl.toString());

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
                getMapComposer().showMessage("No records available for selected criteria.");
                return;
            }

            //create window
            SamplingAreaResultsWCController window = (SamplingAreaResultsWCController) Executions.createComponents("WEB-INF/zul/AnalysisSamplingAreaResults.zul", this, null);
            window.parent = this;
            window.doModal();

            if (count == 1) {
                window.samplingresultslabel.setValue("preview: 1 record");
            } else {
                window.samplingresultslabel.setValue("preview: " + count + " records");
            }

            // load into the results popup
            String[] top_row = null;
            for (int i = 0; i < aslist.length; i++) {
                if (i == 0) {
                    top_row = aslist[i].split("~");
                }
                String[] rec = aslist[i].split("~");

                System.out.println("Column Count: " + rec.length);

                Row r = new Row();
                r.setParent(window.results_rows);
                // set the value
                for (int k = 0; k < rec.length && k < top_row.length; k++) {
                    Label label = new Label(rec[k]);
                    label.setParent(r);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$mapspecies() {
        //results_label_extra.setValue("    [Mapping...]");
        //Events.echoEvent("onMapSpecies", this, null);
        getMapComposer().addToSession("Species in Active area", "lsid=aa");
        //getMapComposer().loadSpeciesInActiveArea(pid, results_count_occurrences, false);
        onMapSpecies(null);
    }

    public void onMapSpecies(Event event) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            //String area = getMapComposer().getViewArea();
            String area = getMapComposer().getSelectionArea();
            String polygon = getMapComposer().getSelectionAreaPolygon();

            StringBuffer sbProcessUrl = new StringBuffer();
            //use cutoff instead of user option; //if(!getMapComposer().useClustering()){
            if (results_count_occurrences > 20000 || (Executions.getCurrent().isExplorer() && results_count_occurrences > 200)) {
                MapLayer gjLayer = getMapComposer().getMapLayer("Species in Active area");
                if(gjLayer != null) {
                    getMapComposer().deactiveLayer(gjLayer, true, false, true);
                    getMapComposer().getOpenLayersJavascript().setAdditionalScript(getMapComposer().getOpenLayersJavascript().iFrameReferences
                                + getMapComposer().getOpenLayersJavascript().removeMapLayer(gjLayer));                        
                }

                //clustering
                MapLayerMetadata md = new MapLayerMetadata();
                md.setLayerExtent(getMapComposer().getViewArea(), 0.2);

                sbProcessUrl.append("species");
                sbProcessUrl.append("/cluster/area/").append(URLEncoder.encode(area, "UTF-8"));
                String id = String.valueOf(System.currentTimeMillis());
                sbProcessUrl.append("/id/").append(URLEncoder.encode(id, "UTF-8"));
                sbProcessUrl.append("/now");
                sbProcessUrl.append("?z=").append(String.valueOf(getMapComposer().getMapZoom()));
                sbProcessUrl.append("&a=").append(URLEncoder.encode(md.getLayerExtentString(), "UTF-8"));
                sbProcessUrl.append("&m=").append(String.valueOf(8));
                MapLayer ml = getMapComposer().addGeoJSONLayer("Species in Active area", satServer + "/alaspatial/" + sbProcessUrl.toString(), true);

                ml.setClustered(true);

                getMapComposer().btnPointsCluster.setLabel("Display species as points");

                if (ml.getMapLayerMetadata() == null) {
                    ml.setMapLayerMetadata(new MapLayerMetadata());
                }

                ml.getMapLayerMetadata().setLayerExtent(polygon, 0.2);

                //TODO: don't use setUnits
                ml.getMapLayerMetadata().setUnits(area);

                //get bounding box for active area
                try {
                    MapLayerMetadata md2 = new MapLayerMetadata();
                    md2.setLayerExtent(polygon, 0);
                    double[] d = md2.getLayerExtent();

                    List<Double> bb = new ArrayList<Double>();
                    for (int i = 0; i < d.length; i++) {
                        bb.add(d[i]);
                    }
                    md.setBbox(bb);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // reopen the layer controls
                try {
                    getMapComposer().refreshActiveLayer(ml);
                    getMapComposer().setupLayerControls(ml);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                /*
                //points
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
                sbProcessUrl.append("/samples/geojson");
                sbProcessUrl.append("?area=").append(URLEncoder.encode(area, "UTF-8"));
                String slist = getInfo(sbProcessUrl.toString());
                //getMapComposer().addGeoJSONLayer("Species in Active area", satServer + "/alaspatial/" + geojsonfile);
                System.out.println("onMapSpecies: " + slist);
                String[] results = slist.split("\n");
                getMapComposer().addGeoJSONLayerProgressBar("Species in Active area", satServer + "/alaspatial/" + results[0], "", false, Integer.parseInt(results[1]), null);//set progress bar with maximum
                 *
                 */

                MapLayer ml = getMapComposer().mapSpeciesWMSByFilter("Species in Active area", "area='" + area + "'");

                //TODO: don't use setUnits
                ml.getMapLayerMetadata().setUnits(area);
            }
            getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", satServer + "/alaspatial/" + sbProcessUrl.toString(), pid, "map species in area");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMapSpecies2(Event event) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            MapComposer mc = getMapComposer();
            String area = mc.getSelectionArea();
            String polygon = mc.getSelectionAreaPolygon();

            //MapLayer ml = getMapComposer().mapSpeciesByFilter(area, area)

            String uri = satServer + "/geoserver/wms?";
            uri += "service=WMS&version=1.1.0&request=GetMap&styles=&format=image/png";
            uri += "&layers=ALA:occurrences";
            uri += "&transparent=true"; // "&env=" + envString +

            String envString = "color:FFFF00;name:square;size:8;opacity:.8";

            String cql = ""; // CQL_FILTER=
            cql += "WITHIN(the_geom,"+area+")";
            //cql += "speciesconceptid='urn:lsid:biodiversity.org.au:afd.taxon:aa745ff0-c776-4d0e-851d-369ba0e6f537'";


            System.out.println("Mapping activearea: \n" + area);

            MapLayer ml = mc.mapSpeciesWMSByFilter("Species in Active area", cql);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    private String postInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        }
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));

            System.out.println("satServer:" + satServer + " ** postInfo:" + urlPart + " ** " + shape);

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    /*public void onClick$refreshButton2() {
    if (!isTabOpen()) {
    return;
    }
    if (updateParameters()
    //|| !popup_listbox_results.isVisible()
    ) {
    populateList();

    //show update list if count > 0
    //refreshButton2.setVisible(false);
    //popup_listbox_results.setVisible(true);
    //results_label.setVisible(true);
    }
    }*/
    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = getMapComposer().getSelectionArea();

        if (area.contains("ENVELOPE(")) {
            shape = "none";
            pid = area.substring(9, area.length() - 1);
            return true;
        } else {
            pid = "none";
            if (shape != area) {
                shape = area;
                return true;
            } else {
                return false;
            }
        }
    }

    static public void open() {
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, null);
        try {
            win.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void refreshCount(int newCount, int newOccurrencesCount) {
        //results_label_extra.setValue("    [Updating...]");
        results_count = newCount;
        results_count_occurrences = newOccurrencesCount;
        if (results_count == 0) {
            //results_label.setValue("no species in active area");
            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
            results = null;
            //popup_listbox_results.setVisible(false);
            //results_label.setVisible(false);
            //refreshButton2.setVisible(true);
        }

        //results_label.setValue("species in active area: " + results_count);
        results_label2_species.setValue(String.valueOf(results_count));
        results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
        setUpdatingCount(false);

        //hide results list, show 'preview list' button
        //popup_listbox_results.setVisible(false);
        //results_label.setVisible(false);
        //refreshButton2.setVisible(true);

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_download")) {
            mapspecies.setVisible(true);
        } else {
            mapspecies.setVisible(false);
        }
    }
}
