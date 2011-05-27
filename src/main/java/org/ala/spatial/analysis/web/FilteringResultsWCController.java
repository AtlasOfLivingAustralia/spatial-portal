package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(FilteringResultsWCController.class);
    public Button mapspecies;
    public Label results_label2_occurrences;
    public Label results_label2_species;
    public Label sdLabel;
    String[] speciesDistributionText = null;
    Window window = null;
    public String[] results = null;
    public String pid;
    String shape;
    private SettingsSupplementary settingsSupplementary = null;
    int results_count = 0;
    int results_count_occurrences = 0;
    boolean addedListener = false;
    Label lblArea;
    Label lblBiostor;
    String reportArea = null;
    String areaName = "Area Report";
    String areaDisplayName = "Area Report";
    String areaSqKm = null;
    double [] boundingBox = null;

    HashMap<String, String> data = new HashMap<String, String>();

    public void setReportArea(String wkt, String name, String displayname, String areaSqKm, double [] boundingBox) {
        reportArea = wkt;
        areaName = name;
        areaDisplayName = displayname;
        this.areaSqKm = areaSqKm;
        this.boundingBox = boundingBox;
        setTitle(displayname);

        if (name.equals("Current extent")) {
            addListener();
        }

        try {
            refreshCount();
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

        if (reportArea != null) {
            setUpdatingCount(true);
        }
    }

    void addListener() {
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    reportArea = getMapComposer().getViewArea();
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
            sdLabel.setValue("updating...");
            lblArea.setValue("updating...");
            lblBiostor.setValue("updating...");
        }
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
                //results_label2_species.setValue("0");
                //results_label2_occurrences.setValue("0");
                data.put("speciesCount","0");
                data.put("occurrencesCount","0");
                mapspecies.setVisible(false);
                results = null;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isTabOpen() {
        return true; //getMapComposer().getPortalSession().getCurrentNavigationTab() == PortalSession.LINK_TAB;
    }

    public void refreshCount() {
        //check if tab is open
        if (!isTabOpen() || !updateParameters()) {
            return;
        }

        setUpdatingCount(true);

        startRefreshCount();

        Events.echoEvent("finishRefreshCount", this, null);
    }

    CountDownLatch counter = null;
    long start = 0;
    Thread biostorThread;

    void startRefreshCount() {
        //countdown includes; intersectWithSpecies, calcuateArea, counts
        counter = new CountDownLatch(3);

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
                counts();
                decCounter();
            }

        };

        t1.start();
        t2.start();
        biostorThread.start();
        t4.start();

        long start = System.currentTimeMillis();

        getMapComposer().updateUserLogAnalysis("species count", "area: " + shape, "", "species list in area");
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
        lblBiostor.setValue(data.get("biostor"));
        lblArea.setValue(data.get("area"));
        results_label2_species.setValue(data.get("speciesCount"));
        results_label2_occurrences.setValue(data.get("occurrencesCount"));
        
        //underline?
        if(isNumberGreaterThanZero(lblBiostor.getValue())) {
            lblBiostor.setSclass("underline");
        } else {
            lblBiostor.setSclass("");
        }
        if(isNumberGreaterThanZero(sdLabel.getValue())){
            sdLabel.setSclass("underline");
        } else {
            sdLabel.setSclass("");
        }
        if(isNumberGreaterThanZero(results_label2_species.getValue())){
            results_label2_species.setSclass("underline");
        } else {
            results_label2_species.setSclass("");
        }
        if(isNumberGreaterThanZero(results_label2_occurrences.getValue())){
            results_label2_occurrences.setSclass("underline");
        } else {
            results_label2_occurrences.setSclass("");
        }

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecies.setVisible(true);
        } else {
            mapspecies.setVisible(false);
        }
    }

    void decCounter() {
        if (counter != null) {
            counter.countDown();
        }
    }

    void counts() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/count");

            String[] out = postInfo(sbProcessUrl.toString()).split("\n");

            results_count = Integer.parseInt(out[0]);
            results_count_occurrences = Integer.parseInt(out[1]);

            //setUpdatingCount(false);

            if (results_count == 0) {
                //results_label.setValue("no species in active area");
                //results_label2_species.setValue("0");
                //results_label2_occurrences.setValue("0");
                data.put("speciesCount","0");
                data.put("occurrencesCount","0");
                mapspecies.setVisible(false);
                results = null;
                return;
            }

            //results_label2_species.setValue(String.format("%,d", results_count));
            //results_label2_occurrences.setValue(String.format("%,d", results_count_occurrences));
            data.put("speciesCount",String.format("%,d", results_count));
            data.put("occurrencesCount",String.format("%,d", results_count_occurrences));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onClick$results_label2_species() {
        SpeciesListEvent sle = new SpeciesListEvent(getMapComposer(), areaName, 1);
        try {
            sle.onEvent(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$downloadsamples() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            String samplesfile = postInfo(sbProcessUrl.toString());

            URL u = new URL(CommonData.satServer + "/alaspatial/" + samplesfile);
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

    public void onClick$results_label2_occurrences() {      
        SamplingEvent sle = new SamplingEvent(getMapComposer(), null, areaName, null, 2);
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

    String registerPointsInArea(String area) {
        //register with alaspatial using data.getPid();
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("species/area/register");

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString());
            get.addParameter("area", URLEncoder.encode(area, "UTF-8"));
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onMapSpecies(Event event) {
        try {
            String area = null;
            if (!areaName.equalsIgnoreCase("Current extent")) {
                area = this.reportArea;
            } else {
                area = getMapComposer().getViewArea();
            }

            StringBuffer sbProcessUrl = new StringBuffer();

            //register points with a new id for mapping
            String lsid = registerPointsInArea(area);
            String activeAreaLayerName = getMapComposer().getNextActiveAreaLayerName(areaDisplayName);
            getMapComposer().mapSpeciesByLsid(lsid, activeAreaLayerName, "species", results_count_occurrences, LayerUtilities.SPECIES);

            getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString(), pid, "map species in area");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(CommonData.satServer + "/alaspatial/ws" + urlPart); // testurl
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
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));

            System.out.println("satServer:" + CommonData.satServer + " ** postInfo:" + urlPart + " ** " + shape);

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

    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = reportArea;

        if (area.contains("ENVELOPE(")) {
            shape = "none";
            pid = area.substring(9, area.length() - 1);
            return true;
        } else {
            pid = "none";
            if (shape == null || !shape.equalsIgnoreCase(area)) {
                shape = area;
                return true;
            } else {
                return false;
            }
        }
    }

    static public void open(String wkt, String name, String displayName, String areaSqKm, double [] boundingBox) {
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, null);
        try {
            win.doOverlapped();
            win.setPosition("center");
            win.setReportArea(wkt, name, displayName, areaSqKm, boundingBox);
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
            data.put("occurrencesCount",String.format("%,d", results_count_occurrences));
            data.put("speciesCount",String.format("%,d", results_count));
            results = null;
        }

        //results_label2_species.setValue(String.format("%,d", results_count));
        //results_label2_occurrences.setValue(String.format("%,d", results_count_occurrences));
        data.put("occurrencesCount",String.format("%,d", results_count_occurrences));
        data.put("speciesCount",String.format("%,d", results_count));
        setUpdatingCount(false);

        // toggle the map button
        if (results_count > 0 && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
            mapspecies.setVisible(true);
        } else {
            mapspecies.setVisible(false);
        }
    }
    Textbox taLSIDs;

    public void onClick$btnAddLSIDs(Event event) {
        try {
            String lsids = taLSIDs.getValue().trim();
            lsids = lsids.replace("\n", ",");
            lsids = lsids.replace("\t", ",");
            lsids = lsids.replace(" ", "");

            String[] split = lsids.split(",");

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/species/lsid/register");
            sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            pid = get.getResponseBodyAsString();

            System.out.println("btnAddLSIDs:" + pid);

            getMapComposer().mapSpeciesByLsid(pid, "User entered LSIDs", LayerUtilities.SPECIES);

            //getMapComposer().updateUserLogAnalysis("Sampling", "", "", u.getFile(), pid, "Sampling download");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void intersectWithSpeciesDistributions() {
        if (shape.equals("none")) {
            //env envelope intersect with species distributions
            //sdLabel.setValue("0");
            data.put("intersectWithSpeciesDistributions","0");
            speciesDistributionText = null;
            return;
        }
        try {
            String area = shape;
            if(area.contains("ENVELOPE") && boundingBox != null) {
//                area = "POLYGON((" + boundingBox[0] + " " + boundingBox[1] + ","
//                        + boundingBox[0] + " " + boundingBox[3] + ","
//                        + boundingBox[2] + " " + boundingBox[3] + ","
//                        + boundingBox[2] + " " + boundingBox[1] + ","
//                        + boundingBox[0] + " " + boundingBox[1] + "))";
                //TODO: support marine envelope
                data.put("intersectWithSpeciesDistributions","0");
                speciesDistributionText = null;
                return;
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("ws/intersect/shape");

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addParameter("area", URLEncoder.encode(area, "UTF-8"));
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                String[] lines = txt.split("\n");
                if (lines[0].length() <= 1) {
                    data.put("intersectWithSpeciesDistributions","0");
                    speciesDistributionText = null;
                } else {
                    //sdLabel.setValue(String.format("%,d", lines.length - 1));
                    data.put("intersectWithSpeciesDistributions",String.format("%,d", lines.length - 1));
                    speciesDistributionText = lines;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$sdLabel(Event event) {
        int c = 0;
        try {
            c = Integer.parseInt(sdLabel.getValue());
        } catch (Exception e) {
        }
        if (c > 0 && speciesDistributionText != null) {
            DistributionsWCController window = (DistributionsWCController) Executions.createComponents("WEB-INF/zul/AnalysisDistributionResults.zul", this, null);

            try {
                window.doModal();
                window.init(this);
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

    private void calculateArea() {
        if (areaSqKm != null) {
            data.put("area", areaSqKm);
            speciesDistributionText = null;
            return;
        }
        if (shape.equals("none")) {
            //sdLabel.setValue("0");
            data.put("area", "0");
            speciesDistributionText = null;
        }

        try {
            double totalarea = Util.calculateArea(reportArea);

            //lblArea.setValue(String.format("%,d", (int) (totalarea / 1000 / 1000)));
            data.put("area",String.format("%,f", (totalarea / 1000 / 1000)));

        } catch (Exception e) {
            System.out.println("Error in calculateArea");
            e.printStackTrace(System.out);
            data.put("area","");
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
    String biostorHtml = null;

    private void biostor() {
        try {
            String area = reportArea;

            if(area.contains("ENVELOPE") && boundingBox != null) {
                area = boundingBox[0] + " " + boundingBox[1] + ","
                        + boundingBox[0] + " " + boundingBox[3] + ","
                        + boundingBox[2] + " " + boundingBox[3] + ","
                        + boundingBox[2] + " " + boundingBox[1] + ","
                        + boundingBox[0] + " " + boundingBox[1];
            } else {
                area = StringUtils.replace(area, "MULTIPOLYGON((", "");
                area = StringUtils.replace(area, "POLYGON((", "");
                area = StringUtils.replace(area, "(", "");
                area = StringUtils.replace(area, ")", "");
            }

            String[] areaarr = area.split(",");

            double lat1 = 0;
            double lat2 = 0;
            double long1 = 0;
            double long2 = 0;
            for (int f = 0; f < areaarr.length; ++f) {
                String[] s = areaarr[f].split(" ");
                double long0 = Double.parseDouble(s[0]);
                double lat0 = Double.parseDouble(s[1]);

                if (f == 0 || long0 < long1) {
                    long1 = long0;
                }
                if (f == 0 || long0 > long2) {
                    long2 = long0;
                }
                if (f == 0 || lat0 < lat1) {
                    lat1 = lat0;
                }
                if (f == 0 || lat0 > lat2) {
                    lat2 = lat0;
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
            ret = Double.parseDouble(value.replace(",","")) > 0;
        } catch (Exception e) {

        }

        return ret;
    }
}
