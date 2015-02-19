package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.wms.WMSStyle;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ajay
 */
public class ImportAnalysisController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(ImportAnalysisController.class);
    private RemoteLogger remoteLogger;
    private Textbox refNum;
    private String pid;
    private boolean isAloc = false;
    private boolean isMaxent = false;
    private boolean isSxS = false;
    private boolean isGdm = false;
    private boolean sxsSitesBySpecies = false;
    private boolean sxsOccurrenceDensity = false;
    private boolean sxsSpeciesDensity = false;
    private String[] gdmEnvlist;
    private Div divPriorAnalysis;
    private Listbox lbLog;
    private LanguagePack languagePack = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        try {
            JSONObject jo = remoteLogger.getLogCSV();


            if (jo != null && jo.containsKey("abe") && !((JSONArray) jo.get("abe")).isEmpty()) {
                List<String[]> logEntries = new ArrayList<String[]>();

                for (Object o : (JSONArray) jo.get("abe")) {
                    JSONObject j = (JSONObject) o;

                    String[] r = new String[5];
                    r[0] = j.containsKey(StringConstants.ID) ? j.get(StringConstants.ID).toString() : "";
                    r[1] = j.containsKey("category2") ? j.get("category2").toString() : "";
                    r[2] = j.containsKey("time") ? new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date(Long.parseLong(j.get("time").toString()))) : "";
                    r[3] = "";
                    r[4] = j.containsKey("service") && ((JSONObject) j.get("service")).containsKey("processid") ? ((JSONObject) j.get("service")).get("processid").toString() : "";

                    if (r[4].length() > 0 && !"-1".equals(r[4]) &&
                            (StringConstants.CLASSIFICATION.equalsIgnoreCase(r[1])
                                    || StringConstants.GDM.equalsIgnoreCase(r[1])
                                    || StringConstants.PREDICTION.equalsIgnoreCase(r[1])
                                    || StringConstants.SPECIES_TO_GRID.equalsIgnoreCase(r[1]))) {

                        try {
                            r[3] = StringConstants.TRUE;
                            refNum.setValue(r[4]);

                            pid = r[4];

                            if (!getJobStatus().contains(StringConstants.JOB_DOES_NOT_EXIST)) {
                                //not sure why, but sometimes data is missing even when the job exists
                                if (hasValidMetadata(r)) {
                                    logEntries.add(r);
                                }
                            } else {
                                //(getLogCSV orders with most recent first)
                                // older jobs don't exist, stop checking
                                break;
                            }

                        } catch (Exception e) {
                            r[4] = "unavailable";
                            r[3] = StringConstants.FALSE;
                        }
                    }
                }

                if (!logEntries.isEmpty()) {
                    divPriorAnalysis.setVisible(true);

                    lbLog.setModel(new SimpleListModel(logEntries));

                    setRenderer();
                }

            }
        } catch (Exception e) {
            LOGGER.error("getting log did not work", e);
        }

        refNum.setValue("");
    }

    void setRenderer() {
        lbLog.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data, int itemIdx) {
                String[] d = (String[]) data;

                li.setValue(d);

                Listcell n = new Listcell(d[1]);
                n.setParent(li);

                n = new Listcell(d[2]);
                n.setParent(li);

                n = new Listcell();
                Html info = new Html(languagePack.getLang("layer_info_icon_html"));

                info.addEventListener(StringConstants.ONCLICK, new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {

                        String[] s = ((Listitem) event.getTarget().getParent().getParent()).getValue();

                        String metadata = null;
                        Event ev = null;
                        if (StringConstants.CLASSIFICATION.equalsIgnoreCase(s[1])) {
                            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/aloc/" + refNum.getValue() + "/classification.html";
                        } else if (StringConstants.PREDICTION.equalsIgnoreCase(s[1])) {
                            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/maxent/" + refNum.getValue() + "/species.html";
                        } else if (StringConstants.GDM.equalsIgnoreCase(s[1])) {
                            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/gdm/" + refNum.getValue() + "/gdm.html";
                        } else if ("Species to Grid".equalsIgnoreCase(s[1])) {

                            String link1 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/sxs_metadata.html";
                            //and or
                            String link2 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/odensity_metadata.html";
                            //and or
                            String link3 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/srichness_metadata.html";

                            String html = "<html><body>";
                            if (Util.readUrl(link1).length() > 0) {
                                html += "<a target='_blank' href='" + link1 + "' >Points to Grid (opens in a new tab)</a><br>";
                            }
                            if (Util.readUrl(link2).length() > 0) {
                                html += "<a target='_blank' href='" + link2 + "' >Occurrence Density (opens in a new tab)</a><br>";
                            }
                            if (Util.readUrl(link3).length() > 0) {
                                html += "<a target='_blank' href='" + link3 + "' >Species Richness (opens in a new tab)</a><br>";
                            }
                            html += "</body></html>";

                            ev = new Event(StringConstants.ONCLICK, null, "Points to Grid\n" + html);
                        }
                        LOGGER.debug("metadata: " + metadata);

                        if (metadata != null) {
                            getMapComposer().activateLink(metadata, "Metadata", false);
                        } else if (ev != null) {
                            getMapComposer().openHTML(ev);
                        }
                    }
                });
                info.setParent(n);
                n.setParent(li);
            }
        });
    }

    boolean hasValidMetadata(String[] s) {
        String metadata = null;

        if (StringConstants.CLASSIFICATION.equalsIgnoreCase(s[1])) {
            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/aloc/" + refNum.getValue() + "/classification.html";
        } else if (StringConstants.PREDICTION.equalsIgnoreCase(s[1])) {
            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/maxent/" + refNum.getValue() + "/species.html";
        } else if (StringConstants.GDM.equalsIgnoreCase(s[1])) {
            metadata = CommonData.getSettings().getProperty("sat_url") + "/output/gdm/" + refNum.getValue() + "/gdm.html";
        } else if ("Species to Grid".equalsIgnoreCase(s[1])) {

            String link1 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/sxs_metadata.html";
            //and or
            String link2 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/odensity_metadata.html";
            //and or
            String link3 = CommonData.getSettings().getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/srichness_metadata.html";

            if (Util.readUrl(link1).length() > 0) {
                return true;
            }
            if (Util.readUrl(link2).length() > 0) {
                return true;
            }
            if (Util.readUrl(link3).length() > 0) {
                return true;
            }
        }

        return metadata != null && Util.readUrl(metadata).length() > 0;


    }

    public void onSelect$lbLog(Event event) {
        Listitem li = lbLog.getSelectedItem();
        String[] s = li.getValue();
        refNum.setValue(s[4]);
    }

    public void onClick$btnOk(Event event) {
        pid = refNum.getValue();
        pid = pid.trim();

        if (getParametersAloc()) {
            isAloc = true;
            openProgressBarAloc();
            remoteLogger.logMapAnalysis("Import Classification", "Tool - Restore", "", "", "", pid, "classification", "IMPORTED");
        } else if (getParametersMaxent()) {
            isMaxent = true;
            openProgressBarMaxent();
            remoteLogger.logMapAnalysis("Import Prediction", "Tool - Restore", "", "", "", pid, "prediction", "IMPORTED");
        } else if (getParametersSxS()) {
            isSxS = true;
            openProgressBarSxS();
            remoteLogger.logMapAnalysis("Import Species to Grid", "Tool - Restore", "", "", "", pid, "species to grid", "IMPORTED");
        } else if (getParametersGdm()) {
            isGdm = true;
            openProgressBarGdm();
            remoteLogger.logMapAnalysis("Import GDM", "Tool - Restore", "", "", "", pid, "gdm", "IMPORTED");
        } else {
            getMapComposer().showMessage(CommonData.lang("error_importing_analysis"));
        }
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    boolean getParametersAloc() {
        String txt = get();
        try {
            int pos = 0;
            int p1 = txt.indexOf("pid:", pos);
            if (p1 < 0) {
                return false;
            }
            int p2 = txt.indexOf("gc:", pos);
            if (p2 < 0) {
                return false;
            }
            int p3 = txt.indexOf("area:", pos);
            int p4 = txt.indexOf("envlist:", pos);
            int p5 = txt.indexOf("pid:", p1 + 4);
            if (p5 < 0) {
                p5 = txt.length();
            }

            String pd = txt.substring(p1 + 4, p2).trim();
            String gc = txt.substring(p2 + 3, p3).trim();
            String area = txt.substring(p3 + 5, p4).trim();
            String envlist = txt.substring(p4 + 8, p5).trim();

            if (gc.endsWith(";")) {
                gc = gc.substring(0, gc.length() - 1);
            }
            if (area.endsWith(";")) {
                area = area.substring(0, area.length() - 1);
            }
            if (envlist.endsWith(";")) {
                envlist = envlist.substring(0, envlist.length() - 1);
            }

            LOGGER.debug("got [" + pd + "][" + gc + "][" + area + "][" + envlist + "]");

            return true;
        } catch (Exception e) {
            LOGGER.error("error building aloc parameters", e);
        }
        return false;
    }

    String get() {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getSatServer() + "/ws/jobs/" + "inputs" + "?pid=" + pid);

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);

            return get.getResponseBodyAsString();
        } catch (Exception e) {
            LOGGER.error("error getting job info pid=" + pid, e);
        }
        return "";
    }

    void openProgressBarAloc() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.setParentWindow(this);
        window.start(pid, StringConstants.CLASSIFICATION);
        try {
            window.doModal();

        } catch (Exception e) {
            LOGGER.error("error opening classification progress bar for pid: " + pid, e);
        }
    }

    public void loadMap(Event event) {
        if (isAloc) {
            loadMapAloc(event);
        } else if (isMaxent) {
            loadMapMaxent(event);
        } else if (isSxS) {
            loadMapSxS();
        }
    }

    public void loadMapAloc(Event event) {
        String layerLabel = "Classification - " + pid;

        String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.getGeoServer()
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:aloc_" + pid;
        LOGGER.debug(legendurl);
        getMapComposer().addWMSLayer("aloc_" + pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.ALOC, null, null);
        MapLayer mapLayer = getMapComposer().getMapLayer("aloc_" + pid);
        mapLayer.setPid(pid);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName(StringConstants.DEFAULT);
            style.setDescription("Default style");
            style.setTitle(StringConstants.DEFAULT);
            style.setLegendUri(legendurl);

            LOGGER.debug("legend:" + legendurl);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            String infoUrl = CommonData.getSatServer() + "/output/layers/" + pid + "/metadata.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layerLabel.replaceAll(" ", "_") + ".zip");
            } catch (Exception ex) {
                LOGGER.error("Error generating download for classification model pid=" + pid, ex);
            }
        }

        this.detach();
    }

    double[] getExtents() {
        double[] d = new double[6];
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getSatServer() + "/output/aloc/" + pid + "/aloc.pngextents.txt");

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            LOGGER.debug("getExtents:" + slist);

            String[] s = slist.split("\n");
            for (int i = 0; i < 6 && i < s.length; i++) {
                d[i] = Double.parseDouble(s[i]);
            }
        } catch (Exception e) {
            LOGGER.error("error getting aloc extents, pid=" + pid, e);
        }
        return d;
    }

    String getJob() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/jobs/").append("inputs").append("?pid=").append(pid);

            LOGGER.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            client.executeMethod(get);
            return get.getResponseBodyAsString();

        } catch (Exception e) {
            LOGGER.error("error getting job type for job pid=" + pid, e);
        }
        return "";
    }

    String getJobStatus() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/job").append("?pid=").append(pid);

            LOGGER.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);
            return get.getResponseBodyAsString();
        } catch (Exception e) {
            LOGGER.error("error getting job type for job pid=" + pid, e);
        }
        return "";
    }

    void openProgressBarMaxent() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.setParentWindow(this);
        window.start(pid, StringConstants.PREDICTION);
        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening prediction progress bar pid=" + pid, e);
        }
    }

    boolean getParametersMaxent() {
        String txt = get();
        try {
            int pos = 0;
            int p1 = txt.indexOf("pid:", pos);
            if (p1 < 0) {
                return false;
            }
            int p2 = txt.indexOf("taxonid:", pos);
            return p2 >= 0;

        } catch (Exception e) {
            LOGGER.error("error getting maxent parameters pid=" + pid + ", inputs=" + txt, e);
        }
        return false;
    }

    boolean getParametersSxS() {
        String txt = get();
        try {
            int pos = 0;
            int p1 = txt.indexOf("pid:", pos);
            if (p1 < 0) {
                return false;
            }
            int p2 = txt.indexOf("gridsize:", pos);
            if (p2 < 0) {
                return false;
            }

            if (txt.indexOf("sitesbyspecies") > 0) {
                sxsSitesBySpecies = true;
            }
            if (txt.indexOf("occurrencedensity") > 0) {
                sxsOccurrenceDensity = true;
            }
            if (txt.indexOf("speciesdensity") > 0) {
                sxsSpeciesDensity = true;
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("error getting sites by species inputs, pid=" + pid + ", inputs=" + txt, e);
        }
        return false;
    }

    void openProgressBarSxS() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.setParentWindow(this);
        window.start(pid, "Points to Grid");
        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening sites by species progress bar pid=" + pid, e);
        }
    }

    public void loadMapMaxent(Event event) {

        String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.getGeoServer()
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        LOGGER.debug(legendurl);

        //get job inputs
        String layername = "Maxent - " + pid;
        getMapComposer().addWMSLayer("species_" + pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer("species_" + pid);
        ml.setPid(pid);
        String infoUrl = CommonData.getSatServer() + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layername.replaceAll(" ", "_") + ".zip");
        } catch (Exception ex) {
            LOGGER.error("Error generating download for prediction model pid=" + pid, ex);
        }

        this.detach();
    }

    private void loadMapSxS() {
        try {
            if (sxsOccurrenceDensity) {
                String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:odensity_" + pid + "&styles=odensity_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.getGeoServer()
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:odensity_" + pid
                        + "&STYLE=odensity_" + pid;

                LOGGER.debug(legendurl);

                String layername = getMapComposer().getNextAreaLayerName(StringConstants.OCCURRENCE_DENSITY);
                getMapComposer().addWMSLayer(pid + "_odensity", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.ODENSITY, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_odensity");
                ml.setPid(pid + "_odensity");
                String infoUrl = CommonData.getSatServer() + "/output/sitesbyspecies/" + pid + "/odensity_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                md.setMoreInfo(infoUrl + "\nOccurrence Density\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            if (sxsSpeciesDensity) {
                String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:srichness_" + pid + "&styles=srichness_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.getGeoServer()
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:srichness_" + pid
                        + "&STYLE=srichness_" + pid;

                LOGGER.debug(legendurl);

                String layername = getMapComposer().getNextAreaLayerName(StringConstants.SPECIES_RICHNESS);
                getMapComposer().addWMSLayer(pid + "_srichness", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.SRICHNESS, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_srichness");
                ml.setPid(pid + "_srichness");
                String infoUrl = CommonData.getSatServer() + "/output/sitesbyspecies/" + pid + "/srichness_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                md.setMoreInfo(infoUrl + "\nSpecies Richness\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            // set off the download as well
            String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            LOGGER.error("Error generating download for sites by species pid=" + pid, ex);
        }

        this.detach();
    }

    boolean getParametersGdm() {
        try {
            //TODO: analysis output url into config

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getSatServer().replace("/alaspatial", "") + "/output/gdm/" + pid + "/ala.properties");

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            int result = client.executeMethod(get);

            if (result == 200) {
                String slist = get.getResponseBodyAsString();
                for (String row : slist.split("\n")) {
                    if (row.startsWith("envlist")) {
                        gdmEnvlist = row.replace("envlist=", "").split("\\\\:");
                    }
                }
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting gdm parameters pid=" + pid, e);
        }
        return false;
    }

    void openProgressBarGdm() {
        String[] envlist = gdmEnvlist;

        for (String env : envlist) {
            String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:gdm_" + env + "Tran_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

            String legendurl = CommonData.getGeoServer()
                    + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                    + "&LAYER=ALA:gdm_" + env + "Tran_" + pid
                    + "&STYLE=alastyles";

            LOGGER.debug(legendurl);

            String layername = "Tranformed " + CommonData.getLayerDisplayName(env);
            getMapComposer().addWMSLayer(pid + "_" + env, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.GDM, null, null);
            MapLayer ml = getMapComposer().getMapLayer(pid + "_" + env);
            ml.setPid(pid + "_" + env);
            String infoUrl = CommonData.getSatServer() + "/output/gdm/" + pid + "/gdm.html";
            MapLayerMetadata md = ml.getMapLayerMetadata();
            md.setMoreInfo(infoUrl + "\nGDM Output\npid:" + pid);
            md.setId(Long.valueOf(pid));
        }

        String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
        try {
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "gdm_" + pid + ".zip");
        } catch (Exception e) {
            LOGGER.error("error mapping gdm pid=" + pid, e);
        }

        this.detach();
    }
}
