package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.composer.progress.ProgressController;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.ListEntry;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;

import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author ajay
 */
public class ImportAnalysisController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(ImportAnalysisController.class);

    private LanguagePack languagePack = null;
    RemoteLogger remoteLogger;
    Textbox refNum;
    String pid;
    boolean isAloc = false;
    boolean isMaxent = false;
    boolean isSxS = false;
    boolean isGdm = false;
    boolean sxsSitesBySpecies = false;
    boolean sxsOccurrenceDensity = false;
    boolean sxsSpeciesDensity = false;
    String[] gdmEnvlist;
    Div divPriorAnalysis;

    Listbox lbLog;

    @Override
    public void afterCompose() {
        super.afterCompose();

        try {
            JSONObject jo = remoteLogger.getLogCSV();


            if (jo != null && jo.containsKey("abe") && jo.getJSONArray("abe").size() > 0) {
                ArrayList<String[]> logEntries = new ArrayList<String[]>();

                for (Object o : jo.getJSONArray("abe")) {
                    JSONObject j = (JSONObject) o;

                    String[] r = new String[5];
                    r[0] = j.containsKey("id") ? j.getString("id") : "";
                    r[1] = j.containsKey("category2") ? j.getString("category2") : "";
                    r[2] = j.containsKey("time") ? new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date(j.getLong("time"))) : "";
                    r[3] = "";
                    r[4] = j.containsKey("service") && j.getJSONObject("service").containsKey("processid")? j.getJSONObject("service").getString("processid") : "";

                    if(r[4].length() > 0 && !r[4].equals("-1") &&
                            (r[1].equalsIgnoreCase("Classification")
                            || r[1].equalsIgnoreCase("GDM")
                            || r[1].equalsIgnoreCase("Prediction")
                            || r[1].equalsIgnoreCase("Species to Grid"))) {

                        try {
                            r[3] = "true";
                            refNum.setValue(r[4]);

                            pid = r[4];

                            if (!getJobStatus().contains("job does not exist")) {
                                //not sure why, but sometimes data is missing even when the job exists
                                if( hasValidMetadata(r) ) {
                                    logEntries.add(r);
                                }
                            } else {
                                //(getLogCSV orders with most recent first)
                                // older jobs don't exist, stop checking
                                break;
                            }

                        } catch (Exception e) {
                            r[4] = "unavailable";
                            r[3] = "false";
                        }
                    }
                }

                if(logEntries.size() > 0) {
                    divPriorAnalysis.setVisible(true);

                    lbLog.setModel(new SimpleListModel(logEntries));

                    setRenderer();
                }

            }
        } catch (Exception e) {
            logger.error("getting log did not work", e);
        }

        refNum.setValue("");
    }

    void setRenderer() {
        lbLog.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data, int item_idx) {
                String[] d = (String[]) data;

                li.setValue(d);

                Listcell n = new Listcell(d[1]);
                n.setParent(li);

                n = new Listcell(d[2]);
                n.setParent(li);

                n = new Listcell();
                Html info = new Html(languagePack.getLang("layer_info_icon_html"));

                info.addEventListener("onClick", new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {

                        String[] s = (String[]) ((Listitem) event.getTarget().getParent().getParent()).getValue();

                        String metadata = null;
                        Event ev = null;
                        if(s[1].equalsIgnoreCase("Classification")) {
                            metadata = CommonData.settings.getProperty("sat_url") + "/output/aloc/" + refNum.getValue() + "/classification.html";
                        } else if(s[1].equalsIgnoreCase("Prediction")) {
                            metadata = CommonData.settings.getProperty("sat_url") + "/output/maxent/" + refNum.getValue() + "/species.html";
                        } else if(s[1].equalsIgnoreCase("GDM")) {
                            metadata = CommonData.settings.getProperty("sat_url") + "/output/gdm/" +refNum.getValue() + "/gdm.html";
                        } else if(s[1].equalsIgnoreCase("Species to Grid")) {

                            String link1 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/sxs_metadata.html";
                            //and or
                            String link2 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/odensity_metadata.html";
                            //and or
                            String link3 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/srichness_metadata.html";

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

                            ev = new Event("onClick", null, "Points to Grid\n" + html);
                        }
                        logger.debug("metadata: " + metadata);

                        if(metadata != null) {
                            getMapComposer().activateLink(metadata, "Metadata", false);
                        } else if(ev != null) {
                            getMapComposer().openHTML(ev);
                        }
                    }
                });
                info.setParent(n);
                n.setParent(li);
            }
        });
    }

    boolean hasValidMetadata(String [] s) {
        String metadata = null;
        Event ev = null;
        if(s[1].equalsIgnoreCase("Classification")) {
            metadata = CommonData.settings.getProperty("sat_url") + "/output/aloc/" + refNum.getValue() + "/classification.html";
        } else if(s[1].equalsIgnoreCase("Prediction")) {
            metadata = CommonData.settings.getProperty("sat_url") + "/output/maxent/" + refNum.getValue() + "/species.html";
        } else if(s[1].equalsIgnoreCase("GDM")) {
            metadata = CommonData.settings.getProperty("sat_url") + "/output/gdm/" +refNum.getValue() + "/gdm.html";
        } else if(s[1].equalsIgnoreCase("Species to Grid")) {

            String link1 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/sxs_metadata.html";
            //and or
            String link2 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/odensity_metadata.html";
            //and or
            String link3 = CommonData.settings.getProperty("sat_url") + "/output/sitesbyspecies/" + refNum.getValue() + "/srichness_metadata.html";

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

        if(metadata != null) {
            return Util.readUrl(metadata).length() > 0;
        }


        return false;
    }

    public void onSelect$lbLog(Event event) {
        Listitem li = lbLog.getSelectedItem();
        String[] s = (String[]) li.getValue();
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
            getMapComposer().showMessage("Invalid reference number.");
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

            pos = p5 - 5;

            String pid = txt.substring(p1 + 4, p2).trim();
            String gc = txt.substring(p2 + 3, p3).trim();
            String area = txt.substring(p3 + 5, p4).trim();
            String envlist = txt.substring(p4 + 8, p5).trim();

            //remove ';' from end
            if (gc.endsWith(";")) {
                gc = gc.substring(0, gc.length() - 1);
            }
            if (area.endsWith(";")) {
                area = area.substring(0, area.length() - 1);
            }
            if (envlist.endsWith(";")) {
                envlist = envlist.substring(0, envlist.length() - 1);
            }

            logger.debug("got [" + pid + "][" + gc + "][" + area + "][" + envlist + "]");

            return true;
        } catch (Exception e) {
            logger.error("error building aloc parameters", e);
        }
        return false;
    }

    String get() {
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/ws/jobs/" + "inputs" + "?pid=" + pid);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);

            return get.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("error getting job info pid=" + pid, e);
        }
        return "";
    }

    void openProgressBarAloc() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Classification");
        try {
            window.doModal();

        } catch (Exception e) {
            logger.error("error opening classification progress bar for pid: " + pid, e);
        }
    }

    public void loadMap(Event event) {
        if (isAloc) {
            loadMapAloc(event);
        } else if (isMaxent) {
            loadMapMaxent(event);
        } else if (isSxS) {
            loadMapSxS(event);
        }
    }

    public void loadMapAloc(Event event) {
        String layerLabel = "Classification - " + pid;

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_" + pid + "&FORMAT=image%2Fpng";
        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:aloc_" + pid;
        logger.debug(legendurl);
        getMapComposer().addWMSLayer("aloc_" + pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ALOC, null, null);
        MapLayer mapLayer = getMapComposer().getMapLayer("aloc_" + pid);
        mapLayer.setPid(pid);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendurl);

            logger.debug("legend:" + legendurl);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            String infoUrl = CommonData.satServer + "/output/layers/" + pid + "/metadata.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.satServer + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layerLabel.replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
            } catch (Exception ex) {
                logger.error("Error generating download for classification model pid=" + pid, ex);
            }
        }

        this.detach();
    }

    double[] getExtents() {
        double[] d = new double[6];
        try {

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer + "/output/aloc/" + pid + "/aloc.pngextents.txt");

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            logger.debug("getExtents:" + slist);

            String[] s = slist.split("\n");
            for (int i = 0; i < 6 && i < s.length; i++) {
                d[i] = Double.parseDouble(s[i]);
            }
        } catch (Exception e) {
            logger.error("error getting aloc extents, pid=" + pid, e);
        }
        return d;
    }

    String getJob() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer).append("/ws/jobs/").append("inputs").append("?pid=").append(pid);

            logger.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            logger.debug(slist);
            return slist;
        } catch (Exception e) {
            logger.error("error getting job type for job pid=" + pid, e);
        }
        return "";
    }

    String getJobStatus() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.satServer).append("/ws/job").append("?pid=").append(pid);

            logger.debug(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            //logger.debug(slist);
            return slist;
        } catch (Exception e) {
            logger.error("error getting job type for job pid=" + pid, e);
        }
        return "";
    }

    void openProgressBarMaxent() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Prediction");
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening prediction progress bar pid=" + pid, e);
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
            if (p2 < 0) {
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("error getting maxent parameters pid=" + pid + ", inputs=" + txt, e);
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
            logger.error("error getting sites by species inputs, pid=" + pid + ", inputs=" + txt, e);
        }
        return false;
    }

    void openProgressBarSxS() {
        ProgressController window = (ProgressController) Executions.createComponents("WEB-INF/zul/progress/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Points to Grid");
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening sites by species progress bar pid=" + pid, e);
        }
    }

    public void loadMapMaxent(Event event) {

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                + "&LAYER=ALA:species_" + pid
                + "&STYLE=alastyles";

        logger.debug(legendurl);

        //get job inputs
        String speciesName = "";
        try {
            for (String s : getJob().split(";")) {
                if (s.startsWith("scientificName")) {
                    speciesName = s.split(":")[1];
                    if (speciesName != null && speciesName.length() > 1) {
                        speciesName = speciesName.substring(0, 1).toUpperCase() + speciesName.substring(1);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("error getting maxent job species names pid=" + pid, e);
        }

        String layername = "Maxent - " + pid;
        getMapComposer().addWMSLayer("species_" + pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer("species_" + pid);
        ml.setPid(pid);
        String infoUrl = CommonData.satServer + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layername.replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception ex) {
            logger.error("Error generating download for prediction model pid=" + pid, ex);
        }

        this.detach();
    }

    private void loadMapSxS(Event event) {
        try {
            if (sxsOccurrenceDensity) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:odensity_" + pid + "&styles=odensity_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:odensity_" + pid
                        + "&STYLE=odensity_" + pid;

                logger.debug(legendurl);

                String layername = getMapComposer().getNextAreaLayerName("Occurrence Density");
                getMapComposer().addWMSLayer(pid + "_odensity", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ODENSITY, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_odensity");
                ml.setPid(pid + "_odensity");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/odensity_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                md.setMoreInfo(infoUrl + "\nOccurrence Density\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            if (sxsSpeciesDensity) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:srichness_" + pid + "&styles=srichness_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:srichness_" + pid
                        + "&STYLE=srichness_" + pid;

                logger.debug(legendurl);

                String layername = getMapComposer().getNextAreaLayerName("Species Richness");
                getMapComposer().addWMSLayer(pid + "_srichness", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.SRICHNESS, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_srichness");
                ml.setPid(pid + "_srichness");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/srichness_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                md.setMoreInfo(infoUrl + "\nSpecies Richness\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            logger.error("Error generating download for sites by species pid=" + pid, ex);
        }

        this.detach();
    }

    boolean getParametersGdm() {
        try {
            //TODO: analysis output url into config

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.satServer.replace("/alaspatial", "") + "/output/gdm/" + pid + "/ala.properties");

            get.addRequestHeader("Accept", "text/plain");

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
            logger.error("Error getting gdm parameters pid=" + pid, e);
        }
        return false;
    }

    void openProgressBarGdm() {
        String[] envlist = gdmEnvlist;

        for (String env : envlist) {
            String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:gdm_" + env + "Tran_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

            String legendurl = CommonData.geoServer
                    + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                    + "&LAYER=ALA:gdm_" + env + "Tran_" + pid
                    + "&STYLE=alastyles";

            logger.debug(legendurl);

            String layername = "Tranformed " + CommonData.getLayerDisplayName(env);
            //logger.debug("Converting '" + env + "' to '" + layername.substring(10) + "' (" + CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(env)) + ")");
            getMapComposer().addWMSLayer(pid + "_" + env, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.GDM, null, null);
            MapLayer ml = getMapComposer().getMapLayer(pid + "_" + env);
            ml.setPid(pid + "_" + env);
            String infoUrl = CommonData.satServer + "/output/gdm/" + pid + "/gdm.html";
            MapLayerMetadata md = ml.getMapLayerMetadata();
            md.setMoreInfo(infoUrl + "\nGDM Output\npid:" + pid);
            md.setId(Long.valueOf(pid));
        }

        String fileUrl = CommonData.satServer + "/ws/download/" + pid;
        try {
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "gdm_" + pid + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception e) {
            logger.error("error mapping gdm pid=" + pid, e);
        }

        this.detach();
    }
}
