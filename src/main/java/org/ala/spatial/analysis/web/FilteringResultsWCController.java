package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.io.StringReader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.SelectedArea;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Div;
import org.zkoss.zul.Window;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(FilteringResultsWCController.class);
    RemoteLogger remoteLogger;
    public Button mapspecies;
    public org.zkoss.zul.A viewrecords, viewrecordskosher;
    public Button mapspecieskosher;
    public Label results_label2_occurrences;
    public Label results_label2_species;
    public Label results_label2_endemic_species;
    public Label results_label2_occurrences_kosher;
    public Label results_label2_species_kosher;
    public Label results_label2_endemic_species_kosher;
    public Label sdLabel;
    public Label clLabel;
    public Label aclLabel;
    String[] speciesDistributionText = null;
    String[] speciesChecklistText = null;
    String[] areaChecklistText = null;
    Window window = null;
    public String pid;
    //String shape;
    private SettingsSupplementary settingsSupplementary = null;
    int results_count = 0;
    int results_count_occurrences = 0;
    int endemic_count =0;
    int results_count_kosher = 0;
    int results_count_occurrences_kosher = 0;
    int endemic_count_kosher =0;
    boolean addedListener = false;
    Label lblArea;
    Label lblBiostor;
    //String reportArea = null;
    SelectedArea selectedArea = null;
    String areaName = "Area Report";
    String areaDisplayName = "Area Report";
    String areaSqKm = null;
    boolean includeEndemic;
    double[] boundingBox = null;
    HashMap<String, String> data = new HashMap<String, String>();
    Div divWorldNote;
    Label lblWorldNoteOccurrences;
    Label lblWorldNoteSpecies;
    Label gazLabel;
    JSONArray gazPoints = null;
    Button mapGazPoints;

    
    public boolean shouldIncludeEndemic(){
        return includeEndemic;
    }
    
    public void setReportArea(SelectedArea sa, String name, String displayname, String areaSqKm, double[] boundingBox, boolean includeEndemic) {
        selectedArea = sa;
        areaName = name;
        areaDisplayName = displayname;
        this.areaSqKm = areaSqKm;
        this.boundingBox = boundingBox;
        this.includeEndemic = includeEndemic;
        setTitle(displayname);

        if (name.equals("Current extent")) {
            addListener();
        }

        try {
            refreshCount();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String extras = "";
            extras += "areaSqKm: " + areaSqKm;
            extras += ";boundingBox: " + boundingBox;
            remoteLogger.logMapAnalysis(displayname, "Tool - Area Report", areaName + "__" + sa.getWkt(), "", "", pid, extras, "0");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void afterCompose() {
        super.afterCompose();

//        try {
//            refreshCount();
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void detach() {
        getMapComposer().getLeftmenuSearchComposer().removeViewportEventListener("filteringResults");

        super.detach();
    }

    @Override
    public void redraw(Writer out) throws java.io.IOException {
        super.redraw(out);

        if (selectedArea != null) {
            setUpdatingCount(true);
        }
    }

    void addListener() {
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
                    refreshCount();
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("filteringResults", el);
        }
    }

    void setUpdatingCount(boolean set) {
        if (set) {
            results_label2_occurrences.setValue("updating...");
            results_label2_species.setValue("updating...");
            if(includeEndemic)
                results_label2_endemic_species.setValue("updating...");
            results_label2_occurrences_kosher.setValue("updating...");
            results_label2_species_kosher.setValue("updating...");
            if(includeEndemic)
                results_label2_endemic_species_kosher.setValue("updating...");
            sdLabel.setValue("updating...");
            clLabel.setValue("updating...");
            aclLabel.setValue("updating...");
            lblArea.setValue("updating...");
            lblBiostor.setValue("updating...");
            gazLabel.setValue("updating...");
        }
    }

//    public void populateList() {
//        try {
//            StringBuffer sbProcessUrl = new StringBuffer();
//            sbProcessUrl.append("/filtering/apply");
//            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
//            sbProcessUrl.append("/species/list");
//
//            String out = postInfo(sbProcessUrl.toString());
//            //remove trailing ','
//            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
//                out = out.substring(0, out.length() - 1);
//            }
//            results = out.split("\\|");
//            java.util.Arrays.sort(results);
//
//            if (results.length == 0 || results[0].trim().length() == 0) {
//                //results_label2_species.setValue("0");
//                //results_label2_occurrences.setValue("0");
//                data.put("speciesCount", "0");
//                data.put("occurrencesCount", "0");
//                mapspecies.setVisible(false);
//                results = null;
//                return;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    boolean isTabOpen() {
        return true; //getMapComposer().getPortalSession().getCurrentNavigationTab() == PortalSession.LINK_TAB;
    }

    public void refreshCount() {
        //check if tab is open
//        if (!isTabOpen() || !updateParameters()) {
//            return;
//        }

        setUpdatingCount(true);

        startRefreshCount();

        Events.echoEvent("finishRefreshCount", this, null);
    }
    CountDownLatch counter = null;
    long start = 0;
    Thread biostorThread;

    void startRefreshCount() {
        //countdown includes; intersectWithSpecies, calcuateArea, counts
        counter = new CountDownLatch(5);
        final boolean worldAreaSelected = CommonData.WORLD_WKT.equals(selectedArea.getWkt());
        Thread t1 = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                intersectWithSpeciesDistributions();
                decCounter();
            }
        };

        Thread t2 = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                calculateArea();
                decCounter();
            }
        };

        biostorThread = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                biostor();
            }
        };

        Thread t4 = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                counts(worldAreaSelected);
                decCounter();
            }
        };

        Thread t5 = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                intersectWithSpeciesChecklists();
                decCounter();
            }
        };

        Thread t6 = new Thread() {

            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                countGazPoints();
                decCounter();
            }
        };

        t1.start();
        t2.start();
        biostorThread.start();
        t4.start();
        t5.start();
        t6.start();

        
        divWorldNote.setVisible(worldAreaSelected);
        lblWorldNoteOccurrences.setVisible(worldAreaSelected);
        lblWorldNoteSpecies.setVisible(worldAreaSelected);

        long start = System.currentTimeMillis();

        getMapComposer().updateUserLogAnalysis("species count", "area: " + selectedArea.getWkt(), "", "species list in area");
    }

    public void finishRefreshCount() {
        try {
            counter.await();
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(FilteringResultsWCController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //wait up to 5s for biostor
        while (biostorThread.isAlive() && (System.currentTimeMillis() - start) < 5000) {
        }

        //terminate wait on biostor if still active
        if (biostorThread.isAlive()) {
            biostorThread.interrupt();
            data.put("biostor", "na");
            Clients.evalJavaScript("displayBioStorCount('biostorrow','na');");
        }

        //set labels
        sdLabel.setValue(data.get("intersectWithSpeciesDistributions"));
        clLabel.setValue(data.get("intersectWithSpeciesChecklists"));
        aclLabel.setValue(data.get("intersectWithAreaChecklists"));
        lblBiostor.setValue(data.get("biostor"));
        lblArea.setValue(data.get("area"));
        results_label2_species.setValue(data.get("speciesCount"));
        if(includeEndemic)
            results_label2_endemic_species.setValue(data.get("endemicCount"));
        results_label2_occurrences.setValue(data.get("occurrencesCount"));
        results_label2_species_kosher.setValue(data.get("speciesCountKosher"));
        if(includeEndemic)
            results_label2_endemic_species_kosher.setValue(data.get("endemicCountKosher"));
        results_label2_occurrences_kosher.setValue(data.get("occurrencesCountKosher"));
        gazLabel.setValue(data.get("countGazPoints"));

        //underline?
        if (isNumberGreaterThanZero(lblBiostor.getValue())) {
            lblBiostor.setSclass("underline");
        } else {
            lblBiostor.setSclass("");
        }
        if (isNumberGreaterThanZero(sdLabel.getValue())) {
            sdLabel.setSclass("underline");
        } else {
            sdLabel.setSclass("");
        }
        if (isNumberGreaterThanZero(clLabel.getValue())) {
            clLabel.setSclass("underline");
        } else {
            clLabel.setSclass("");
        }
        if (isNumberGreaterThanZero(aclLabel.getValue())) {
            aclLabel.setSclass("underline");
        } else {
            aclLabel.setSclass("");
        }
        if (isNumberGreaterThanZero(results_label2_species.getValue())) {
            results_label2_species.setSclass("underline");
        } else {
            results_label2_species.setSclass("");
        }
        if (isNumberGreaterThanZero(results_label2_occurrences.getValue())) {
            results_label2_occurrences.setSclass("underline");
        } else {
            results_label2_occurrences.setSclass("");
        }
        if(includeEndemic){
            if (isNumberGreaterThanZero(results_label2_endemic_species.getValue())){
                results_label2_endemic_species.setSclass("underline");
            } else {
                results_label2_endemic_species.setSclass("");
            }
            if (isNumberGreaterThanZero(results_label2_endemic_species_kosher.getValue())){
                results_label2_endemic_species_kosher.setSclass("underline");            
            } else {
                results_label2_endemic_species_kosher.setSclass("");
            }
        }
        

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecies.setVisible(true);
            viewrecords.setVisible(true);
            viewrecords.setHref(data.get("viewRecordsUrl"));
        } else {
            mapspecies.setVisible(false);
            viewrecords.setVisible(false);
        }

        if (isNumberGreaterThanZero(results_label2_species_kosher.getValue())) {
            results_label2_species_kosher.setSclass("underline");
        } else {
            results_label2_species_kosher.setSclass("");
        }
        if (isNumberGreaterThanZero(results_label2_occurrences_kosher.getValue())) {
            results_label2_occurrences_kosher.setSclass("underline");
        } else {
            results_label2_occurrences_kosher.setSclass("");
        }

        // toggle the map button
        if (results_count_kosher > 0 && results_count_occurrences_kosher <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecieskosher.setVisible(true);
            viewrecordskosher.setVisible(true);
            viewrecordskosher.setHref(data.get("viewRecordsKosherUrl"));
        } else {
            mapspecieskosher.setVisible(false);
            viewrecordskosher.setVisible(false);
        }

        mapGazPoints.setVisible(gazPoints != null && gazPoints.size() < 5000);
    }

    void decCounter() {
        if (counter != null) {
            counter.countDown();
        }
    }

    void counts(boolean worldSelected) {
        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, null);
            results_count = sq.getSpeciesCount();
            results_count_occurrences = sq.getOccurrenceCount();
            endemic_count = worldSelected || !includeEndemic? results_count:sq.getEndemicSpeciesCount();

            Query sq2 = QueryUtil.queryFromSelectedArea(null, selectedArea, false, new boolean[]{true, false, false});
            results_count_kosher = sq2.getSpeciesCount();
            results_count_occurrences_kosher = sq2.getOccurrenceCount();
            //based the endemic count on the geospatially kosher - endemic is everything if the world is selected 
            endemic_count_kosher = worldSelected || !includeEndemic?results_count_kosher:sq2.getEndemicSpeciesCount();

            //setUpdatingCount(false);

            if (results_count == 0) {
                //results_label.setValue("no species in active area");
                //results_label2_species.setValue("0");
                //results_label2_occurrences.setValue("0");
                data.put("speciesCount", "0");
                data.put("occurrencesCount", "0");
                mapspecies.setVisible(false);
                viewrecords.setVisible(false);
                return;
            }
            else{
                data.put("viewRecordsUrl",CommonData.biocacheWebServer+"/occurrences/search?q="+sq.getQ()+"&qc="+sq.getQc());
            }

            if (results_count == 0) {
                data.put("speciesCountKosher", "0");
                data.put("occurrencesCountKosher", "0");
                mapspecieskosher.setVisible(false);
                viewrecordskosher.setVisible(false);
                return;
            }
            else{
                data.put("viewRecordsKosherUrl",CommonData.biocacheWebServer+"/occurrences/search?q="+sq2.getQ()+"&qc="+sq2.getQc());
            }

            //results_label2_species.setValue(String.format("%,d", results_count));
            //results_label2_occurrences.setValue(String.format("%,d", results_count_occurrences));
            data.put("speciesCount", String.format("%,d", results_count));
            if(includeEndemic)
                data.put("endemicCount",String.format("%d", endemic_count));
            data.put("occurrencesCount", String.format("%,d", results_count_occurrences));
            data.put("speciesCountKosher", String.format("%,d", results_count_kosher));
            if(includeEndemic)
                data.put("endemicCountKosher", String.format("%d",endemic_count_kosher));
            data.put("occurrencesCountKosher", String.format("%,d", results_count_occurrences_kosher));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onClick$results_label2_species() {
        SpeciesListEvent sle = new SpeciesListEvent(getMapComposer(), areaName, 1, null);
        try {
            sle.onEvent(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onClick$results_label2_endemic_species(){
        if( isNumberGreaterThanZero(results_label2_endemic_species.getValue())){
            SpeciesListEvent sle = new SpeciesListEvent(getMapComposer(),areaName,1, null, true);
            try {
                sle.onEvent(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void onClick$results_label2_endemic_species_kosher(){
        if( isNumberGreaterThanZero(results_label2_endemic_species_kosher.getValue())){
            SpeciesListEvent sle = new SpeciesListEvent(getMapComposer(), areaName, 1, new boolean[]{true, false, false}, true);
              try {
                  sle.onEvent(null);
              } catch (Exception e) {
                  e.printStackTrace();
              }
          }
    }

    public void onClick$results_label2_occurrences() {
        SamplingEvent sle = new SamplingEvent(getMapComposer(), null, areaName, null, 2, null);
        try {
            sle.onEvent(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$results_label2_species_kosher() {
        SpeciesListEvent sle = new SpeciesListEvent(getMapComposer(), areaName, 1, new boolean[]{true, false, false});
        try {
            sle.onEvent(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$results_label2_occurrences_kosher() {
        SamplingEvent sle = new SamplingEvent(getMapComposer(), null, areaName, null, 2, new boolean[]{true, false, false});
        try {
            sle.onEvent(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$mapspecies() {
        //getMapComposer().addToSession("Occurrences in Active area", "lsid=aa");
        onMapSpecies(null);
    }

    public void onClick$mapspecieskosher() {
        //getMapComposer().addToSession("Occurrences in Active area", "lsid=aa");
        onMapSpeciesKosher(null);
    }

    public void onMapSpecies(Event event) {
        try {
            SelectedArea sa = null;
            if (!areaName.equalsIgnoreCase("Current extent")) {
                sa = selectedArea;
            } else {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            }

            Query query = QueryUtil.queryFromSelectedArea(null, sa, true, null);

            String activeAreaLayerName = getMapComposer().getNextActiveAreaLayerName(areaDisplayName);
            getMapComposer().mapSpecies(query, activeAreaLayerName, "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMapSpeciesKosher(Event event) {
        try {
            SelectedArea sa = null;
            if (!areaName.equalsIgnoreCase("Current extent")) {
                sa = selectedArea;
            } else {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            }

            Query query = QueryUtil.queryFromSelectedArea(null, sa, true, new boolean[]{true, false, false});

            String activeAreaLayerName = getMapComposer().getNextActiveAreaLayerName(areaDisplayName + " geospatial kosher");
            getMapComposer().mapSpecies(query, activeAreaLayerName, "species", -1, LayerUtilities.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, MapComposer.nextColour());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void open(SelectedArea sa, String name, String displayName, String areaSqKm, double[] boundingBox, boolean includeEndemic) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("includeEndemic", includeEndemic);
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, params);
        try {
            win.doOverlapped();
            win.setPosition("center");
            win.setReportArea(sa, name, displayName, areaSqKm, boundingBox,includeEndemic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void refreshCount(int newCount, int newOccurrencesCount) {
        results_count = newCount;
        results_count_occurrences = newOccurrencesCount;
        if (results_count == 0) {
            //results_label2_species.setValue(String.format("%,d", results_count));
            //results_label2_occurrences.setValue(String.format("%,d", results_count_occurrences));
            data.put("occurrencesCount", String.format("%,d", results_count_occurrences));
            data.put("speciesCount", String.format("%,d", results_count));
            data.put("occurrencesCountKosher", String.format("%,d", results_count_occurrences_kosher));
            data.put("speciesCountKosher", String.format("%,d", results_count_kosher));
        }

        //results_label2_species.setValue(String.format("%,d", results_count));
        //results_label2_occurrences.setValue(String.format("%,d", results_count_occurrences));
        data.put("occurrencesCount", String.format("%,d", results_count_occurrences));
        data.put("speciesCount", String.format("%,d", results_count));
        data.put("occurrencesCountKosher", String.format("%,d", results_count_occurrences_kosher));
        data.put("speciesCountKosher", String.format("%,d", results_count_kosher));
        setUpdatingCount(false);

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecies.setVisible(true);
            viewrecords.setVisible(true);
        } else {
            mapspecies.setVisible(false);
            viewrecords.setVisible(false);
        }
    }

    public void intersectWithSpeciesDistributions() {
        try {
            String wkt = selectedArea.getWkt();
            if (wkt.contains("ENVELOPE") && selectedArea.getMapLayer() != null) {
                //use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = "POLYGON((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }
            String[] lines = getDistributionsOrChecklists("distributions", wkt, null, null);

            if (lines == null || lines.length <= 1) {
                data.put("intersectWithSpeciesDistributions", "0");
                speciesDistributionText = null;
            } else {
                data.put("intersectWithSpeciesDistributions", String.format("%,d", lines.length - 1));
                speciesDistributionText = lines;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getDistributionsOrChecklists(String type, String wkt, String lsids, String geom_idx) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/" + type);

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.layersServer + sbProcessUrl.toString()); // testurl
            System.out.println(CommonData.layersServer + sbProcessUrl.toString());
            if (wkt != null) {
                post.addParameter("wkt", wkt);
            }
            if (lsids != null) {
                post.addParameter("lsids", lsids);
            }
            if (geom_idx != null) {
                post.addParameter("geom_idx", geom_idx);
            }
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                JSONArray ja = JSONArray.fromObject(txt);
                if (ja == null || ja.size() == 0) {
                    return null;
                } else {
                    String[] lines = new String[ja.size() + 1];
                    lines[0] = "SPCODE,SCIENTIFIC_NAME,AUTHORITY_FULL,COMMON_NAME,FAMILY,GENUS_NAME,SPECIFIC_NAME,MIN_DEPTH,MAX_DEPTH,METADATA_URL,LSID,AREA_NAME,AREA_SQ_KM";
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = ja.getJSONObject(i);
                        String spcode = jo.containsKey("spcode") ? jo.getString("spcode") : "";
                        String scientific = jo.containsKey("scientific") ? jo.getString("scientific") : "";
                        String auth = jo.containsKey("authority_") ? jo.getString("authority_") : "";
                        String common = jo.containsKey("common_nam") ? jo.getString("common_nam") : "";
                        String family = jo.containsKey("family") ? jo.getString("family") : "";
                        String genus = jo.containsKey("genus") ? jo.getString("genus") : "";
                        String name = jo.containsKey("specific_n") ? jo.getString("specific_n") : "";
                        String min = jo.containsKey("min_depth") ? jo.getString("min_depth") : "";
                        String max = jo.containsKey("max_depth") ? jo.getString("max_depth") : "";
                        //String p = jo.containsKey("pelagic_fl")?jo.getString("pelagic_fl"):"";
                        String md = jo.containsKey("metadata_u") ? jo.getString("metadata_u") : "";
                        String lsid = jo.containsKey("lsid") ? jo.getString("lsid") : "";
                        String area_name = jo.containsKey("area_name") ? jo.getString("area_name") : "";
                        String area_km = jo.containsKey("area_km") ? jo.getString("area_km") : "";

                        StringBuilder sb = new StringBuilder();
                        sb.append(spcode).append(",");
                        sb.append(wrap(scientific)).append(",");
                        sb.append(wrap(auth)).append(",");
                        sb.append(wrap(common)).append(",");
                        sb.append(wrap(family)).append(",");
                        sb.append(wrap(genus)).append(",");
                        sb.append(wrap(name)).append(",");
                        sb.append(min).append(",");
                        sb.append(max).append(",");
                        sb.append(wrap(md)).append(",");
                        sb.append(wrap(lsid)).append(",");
                        sb.append(wrap(area_name)).append(",");
                        sb.append(wrap(area_km));

                        lines[i + 1] = sb.toString();
                    }

                    return lines;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray getGazPoints(String wkt) {
        try {
            int limit = Integer.MAX_VALUE;
            String url = CommonData.layersServer + "/objects/inarea/" + CommonData.settings.get("area_report_gaz_field") + "?limit=" + limit;

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            System.out.println(CommonData.layersServer + url);
            if (wkt != null) {
                post.addParameter("wkt", wkt);
            }
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                return JSONArray.fromObject(txt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getAreaChecklists(String geom_idx, String lsids, String wkt) {
        try {
            return getAreaChecklists(getDistributionsOrChecklists("checklists", lsids, wkt, geom_idx));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getAreaChecklists(String[] records) {
        String[] lines = null;
        try {
            if (records != null) {
                String[][] data = new String[records.length - 1][]; //exclude header
                for (int i = 1; i < records.length; i++) {
                    CSVReader csv = new CSVReader(new StringReader(records[i]));
                    data[i - 1] = csv.readNext();
                    csv.close();
                }
                java.util.Arrays.sort(data, new Comparator<String[]>() {

                    @Override
                    public int compare(String[] o1, String[] o2) {
                        //compare WMS urls
                        return CommonData.getSpeciesChecklistWMSFromSpcode(o1[0])[1].compareTo(CommonData.getSpeciesChecklistWMSFromSpcode(o2[0])[1]);
                    }
                });

                lines = new String[records.length];
                lines[0] = lines[0] = "SPCODE,SCIENTIFIC_NAME,AUTHORITY_FULL,COMMON_NAME,FAMILY,GENUS_NAME,SPECIFIC_NAME,MIN_DEPTH,MAX_DEPTH,METADATA_URL,LSID,AREA_NAME,AREA_SQ_KM,SPECIES_COUNT";
                int len = 1;
                int thisCount = 0;
                for (int i = 0; i < data.length; i++) {
                    thisCount++;
                    if (i == data.length - 1
                            || !CommonData.getSpeciesChecklistWMSFromSpcode(data[i][0])[1].equals(CommonData.getSpeciesChecklistWMSFromSpcode(data[i + 1][0])[1])) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < data[i].length; j++) {
                            if (j > 0) {
                                sb.append(",");
                            }
                            if (j == 0 || (j >= 9 && j != 10)) {
                                sb.append(wrap(data[i][j]));
                            }
                        }
                        sb.append(",").append(thisCount);
                        lines[len] = sb.toString();
                        len++;
                        thisCount = 0;
                    }
                }
                lines = java.util.Arrays.copyOf(lines, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            lines = null;
        }
        return lines;
    }

    public static String[] getDistributionOrChecklist(String spcode) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/distribution/" + spcode);

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.layersServer + sbProcessUrl.toString()); // testurl
            System.out.println(CommonData.layersServer + sbProcessUrl.toString());
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                JSONObject jo = JSONObject.fromObject(txt);
                if (jo == null) {
                    return null;
                } else {
                    String[] output = new String[14];
                    //"SPCODE,SCIENTIFIC_NAME,AUTHORITY_FULL,COMMON_NAME,FAMILY,GENUS_NAME,SPECIFIC_NAME,MIN_DEPTH,MAX_DEPTH,METADATA_URL,LSID,AREA_NAME,AREA_SQ_KM";

                    //String spcode = jo.containsKey("spcode") ? jo.getString("spcode") : "";
                    String scientific = jo.containsKey("scientific") ? jo.getString("scientific") : "";
                    String auth = jo.containsKey("authority_") ? jo.getString("authority_") : "";
                    String common = jo.containsKey("common_nam") ? jo.getString("common_nam") : "";
                    String family = jo.containsKey("family") ? jo.getString("family") : "";
                    String genus = jo.containsKey("genus") ? jo.getString("genus") : "";
                    String name = jo.containsKey("specific_n") ? jo.getString("specific_n") : "";
                    String min = jo.containsKey("min_depth") ? jo.getString("min_depth") : "";
                    String max = jo.containsKey("max_depth") ? jo.getString("max_depth") : "";
                    //String p = jo.containsKey("pelagic_fl")?jo.getString("pelagic_fl"):"";
                    String md = jo.containsKey("metadata_u") ? jo.getString("metadata_u") : "";
                    String lsid = jo.containsKey("lsid") ? jo.getString("lsid") : "";
                    String area_name = jo.containsKey("area_name") ? jo.getString("area_name") : "";
                    String area_km = jo.containsKey("area_km") ? jo.getString("area_km") : "";
                    String data_resource_id = jo.containsKey("data_resource_uid") ? jo.getString("data_resource_uid") : "";

                    output[0] = spcode;
                    output[1] = scientific;
                    output[2] = auth;
                    output[3] = common;
                    output[4] = family;
                    output[5] = genus;
                    output[6] = name;
                    output[7] = min;
                    output[8] = max;
                    output[9] = md;
                    output[10] = lsid;
                    output[11] = area_name;
                    output[12] = area_km;
                    output[13] = data_resource_id;

                    return output;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void intersectWithSpeciesChecklists() {
        try {
            String wkt = selectedArea.getWkt();
            if (wkt.contains("ENVELOPE") && selectedArea.getMapLayer() != null) {
                //use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = "POLYGON((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            String[] lines = getDistributionsOrChecklists("checklists", wkt, null, null);

            if (lines == null || lines.length <= 1) {
                data.put("intersectWithSpeciesChecklists", "0");
                data.put("intersectWithAreaChecklists", "0");
                speciesChecklistText = null;
            } else {
                data.put("intersectWithSpeciesChecklists", String.format("%,d", lines.length - 1));

                areaChecklistText = getAreaChecklists(lines);
                data.put("intersectWithAreaChecklists", String.format("%,d", areaChecklistText.length - 1));
                speciesChecklistText = lines;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void countGazPoints() {
        try {
            String wkt = selectedArea.getWkt();
            if (wkt.contains("ENVELOPE") && selectedArea.getMapLayer() != null) {
                //use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = "POLYGON((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            JSONArray ja = getGazPoints(wkt);

            if (ja == null || ja.size() == 0) {
                data.put("countGazPoints", "0");
                speciesChecklistText = null;
            } else {
                data.put("countGazPoints", String.format("%,d", ja.size()));
                gazPoints = ja;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String wrap(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public void onClick$sdLabel(Event event) {
        int c = 0;
        try {
            c = Integer.parseInt(sdLabel.getValue().replace(",", ""));
        } catch (Exception e) {
        }
        if (c > 0 && speciesDistributionText != null) {
            try {
                getMapComposer().getFellowIfAny("distributionresults").detach();
            } catch (Exception e) {
            }
            DistributionsWCController window = (DistributionsWCController) Executions.createComponents("WEB-INF/zul/AnalysisDistributionResults.zul", this, null);

            try {
                window.doModal();
                window.init(speciesDistributionText, "Expert Distributions", sdLabel.getValue(), new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        onClick$sdDownload(event);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick$clLabel(Event event) {
        int c = 0;
        try {
            c = Integer.parseInt(clLabel.getValue().replace(",", ""));
        } catch (Exception e) {
        }
        if (c > 0 && speciesChecklistText != null) {
            DistributionsWCController window = (DistributionsWCController) Executions.createComponents("WEB-INF/zul/AnalysisDistributionResults.zul", this, null);

            try {
                window.doModal();
                window.init(speciesChecklistText, "Species Checklists", clLabel.getValue(), new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        onClick$clDownload(event);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick$aclLabel(Event event) {
        int c = 0;
        try {
            c = Integer.parseInt(aclLabel.getValue().replace(",", ""));
        } catch (Exception e) {
        }
        if (c > 0 && areaChecklistText != null) {
            DistributionsWCController window = (DistributionsWCController) Executions.createComponents("WEB-INF/zul/AnalysisDistributionResults.zul", this, null);

            try {
                window.doModal();
                window.init(areaChecklistText, "Checklist areas", aclLabel.getValue(), new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        onClick$aclDownload(event);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick$sdDownload(Event event) {
        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : speciesDistributionText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), "text/plain", "Species_distributions_" + sdate + "_" + spid + ".csv");
    }

    public void onClick$clDownload(Event event) {
        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : speciesChecklistText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), "text/plain", "Species_checklists_" + sdate + "_" + spid + ".csv");
    }

    public void onClick$aclDownload(Event event) {
        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : areaChecklistText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), "text/plain", "Area_checklists_" + sdate + "_" + spid + ".csv");
    }

    private void calculateArea() {
        if (areaSqKm != null) {
            data.put("area", areaSqKm);
            speciesDistributionText = null;
            return;
        }

        try {
            double totalarea = Util.calculateArea(selectedArea.getWkt());
            DecimalFormat df = new DecimalFormat("###,###.##");

            //lblArea.setValue(String.format("%,d", (int) (totalarea / 1000 / 1000)));
            //data.put("area",String.format("%,f", (totalarea / 1000 / 1000)));
            data.put("area", df.format(totalarea / 1000 / 1000));

        } catch (Exception e) {
            System.out.println("Error in calculateArea");
            e.printStackTrace(System.out);
            data.put("area", "");
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
    String biostorHtml = null;

    private void biostor() {
        try {
            String area = selectedArea.getWkt();

            double lat1 = 0;
            double lat2 = 0;
            double long1 = 0;
            double long2 = 0;
            if (area.contains("ENVELOPE") && selectedArea.getMapLayer() != null) {
                //use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                long1 = bbox.get(0);
                lat1 = bbox.get(1);
                long2 = bbox.get(2);
                lat2 = bbox.get(3);
            } else {
                Pattern coord = Pattern.compile("[+-]?[0-9]*\\.?[0-9]* [+-]?[0-9]*\\.?[0-9]*");
                Matcher matcher = coord.matcher(area);

                boolean first = true;
                while (matcher.find()) {
                    String[] p = matcher.group().split(" ");
                    double[] d = {Double.parseDouble(p[0]), Double.parseDouble(p[1])};

                    if (first || long1 > d[0]) {
                        long1 = d[0];
                    }
                    if (first || long2 < d[0]) {
                        long2 = d[0];
                    }
                    if (first || lat1 > d[1]) {
                        lat1 = d[1];
                    }
                    if (first || lat2 < d[1]) {
                        lat2 = d[1];
                    }

                    first = false;
                }
            }

            String biostorurl = "http://biostor.org/bounds.php?";
            biostorurl += "bounds=" + long1 + "," + lat1 + "," + long2 + "," + lat2;

            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            GetMethod get = new GetMethod(biostorurl);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);

            biostorHtml = null;
            if (result == HttpStatus.SC_OK) {
                String slist = get.getResponseBodyAsString();
                if (slist != null) {

                    JSONArray list = JSONObject.fromObject(slist).getJSONArray("list");
                    StringBuilder sb = new StringBuilder();
                    sb.append("<ol>");
                    for (int i = 0; i < list.size(); i++) {
                        sb.append("<li>");
                        sb.append("<a href=\"http://biostor.org/reference/");
                        sb.append(list.getJSONObject(i).getString("id"));
                        sb.append("\" target=\"_blank\">");
                        sb.append(list.getJSONObject(i).getString("title"));
                        sb.append("</li>");
                    }
                    sb.append("</ol>");

                    if (list.size() > 0) {
                        biostorHtml = sb.toString();
                    }

//                $.getJSON(proxy_script + biostorurl, function(data){
//                            var html = '<ol>';
//                            for(var i=0, item; item=data.list[i]; i++) {
//                                html += '<li>' + '<a href="http://biostor.org/reference/' + item.id + '" target="_blank">' + item.title + '</a></li>';
//                            }
//                            html += '</ol>';
//                            parent.displayHTMLInformation("biostormsg","<u>" + data.list.length + "</u>");
//                            parent.displayHTMLInformation('biostorlist',html);
//                        });
                    //lblBiostor.setValue(String.valueOf(list.size()));
                    data.put("biostor", String.valueOf(list.size()));
                }
            } else {
                //lblBiostor.setValue("BioStor currently down");
                data.put("biostor", "Biostor currently down");
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
            //lblBiostor.setValue("BioStor currently down");
            data.put("biostor", "Biostor currently down");
        }
    }

    public void onClick$lblBiostor(Event event) {
        if (biostorHtml != null) {
            Event ev = new Event("onClick", this, "Biostor Documents\n" + biostorHtml);
            getMapComposer().openHTML(ev);
        }
    }

    private boolean isNumberGreaterThanZero(String value) {
        boolean ret = false;
        try {
            ret = Double.parseDouble(value.replace(",", "")) > 0;
        } catch (Exception e) {
        }

        return ret;
    }

    public void onClick$mapGazPoints() {
        try {
            if (gazPoints == null || gazPoints.size() == 0) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            TreeSet<String> columns = new TreeSet<String>();

            //get columns
            for (int i = 0; i < gazPoints.size(); i++) {
                columns.addAll(gazPoints.getJSONObject(i).keySet());
            }

            //write columns, first two are longitude,latitude
            sb.append("longitude,latitude");
            for (String s : columns) {
                if (!s.equals("longitude") && !s.equals("latitude")) {
                    sb.append(",").append(s);
                }
            }

            for (int i = 0; i < gazPoints.size(); i++) {
                sb.append("\n");

                if (gazPoints.getJSONObject(i).containsKey("geometry")) {
                    String geometry = gazPoints.getJSONObject(i).getString("geometry");
                    geometry = geometry.replace("POINT(", "").replace(")", "").replace(" ", ",");
                    sb.append(geometry);
                } else {
                    sb.append(",");
                }

                for (String s : columns) {
                    if (!s.equals("longitude") && !s.equals("latitude")) {
                        String ss = gazPoints.getJSONObject(i).containsKey(s) ? gazPoints.getJSONObject(i).getString(s) : "";
                        sb.append(",\"").append(ss.replace("\"", "\"\"")).append("\"");
                    }
                }
            }

            getMapComposer().featuresCSV = sb.toString();
            getMapComposer().getOpenLayersJavascript().execute("mapFrame.mapPoints('" + StringEscapeUtils.escapeJavaScript(gazPoints.toString()) + "');");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
