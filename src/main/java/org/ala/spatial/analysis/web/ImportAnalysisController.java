package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import java.net.URL;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class ImportAnalysisController extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
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

    @Override
    public void afterCompose() {
        super.afterCompose();
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
        String txt = get("inputs");
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

            System.out.println("got [" + pid + "][" + gc + "][" + area + "][" + envlist + "]");

            //apply job input parameters to selection
//            groupCount.setValue(Integer.parseInt(gc));
//
//            lbListLayers.clearSelection();
//            lbListLayers.selectLayers(envlist.split(":"));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    String get(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

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

    void openProgressBarAloc() {
        ProgressWCController window = (ProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Classification");
        try {
            window.doModal();

        } catch (Exception e) {
            e.printStackTrace();
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
        System.out.println(legendurl);
        getMapComposer().addWMSLayer(pid, layerLabel, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ALOC, null, null);
        MapLayer mapLayer = getMapComposer().getMapLayer(pid);
        mapLayer.setData("pid", pid);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendurl);

            System.out.println("legend:" + legendurl);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
            }

            String infoUrl = CommonData.satServer + "/output/layers/" + pid + "/metadata.html" + "\nClassification output\npid:" + pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid));

            getMapComposer().updateLayerControls();

            try {
                // set off the download as well
                String fileUrl = CommonData.satServer + "/ws/download/" + pid;
                Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layerLabel.replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
            } catch (Exception ex) {
                System.out.println("Error generating download for classification model:");
                ex.printStackTrace(System.out);
            }
        }

        this.detach();
    }

    double[] getExtents() {
        double[] d = new double[6];
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/output/aloc/" + pid + "/aloc.pngextents.txt");

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            System.out.println("getExtents:" + slist);

            String[] s = slist.split("\n");
            for (int i = 0; i < 6 && i < s.length; i++) {
                d[i] = Double.parseDouble(s[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }

    String getJob(String type) {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/ws/jobs/").append(type).append("?pid=").append(pid);

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

    void openProgressBarMaxent() {
        ProgressWCController window = (ProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Prediction");
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean getParametersMaxent() {
        String txt = get("inputs");
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
            e.printStackTrace();
        }
        return false;
    }

    boolean getParametersSxS() {
        String txt = get("inputs");
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
            e.printStackTrace();
        }
        return false;
    }

    void openProgressBarSxS() {
        ProgressWCController window = (ProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisProgress.zul", getMapComposer(), null);
        window.parent = this;
        window.start(pid, "Points to Grid");        
        try {
            window.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMapMaxent(Event event) {

        String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

        String legendurl = CommonData.geoServer
                + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
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

        //taxon = "species";


        String layername = "Maxent - " + pid;
        getMapComposer().addWMSLayer(pid, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.MAXENT, null, null);
        MapLayer ml = getMapComposer().getMapLayer(pid);
        ml.setData("pid", pid);
        String infoUrl = CommonData.satServer + "/output/maxent/" + pid + "/species.html";
        MapLayerMetadata md = ml.getMapLayerMetadata();
        if (md == null) {
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setMoreInfo(infoUrl + "\nMaxent Output\npid:" + pid);
        md.setId(Long.valueOf(pid));

        try {
            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", layername.replaceAll(" ", "_") + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception ex) {
            System.out.println("Error generating download for prediction model:");
            ex.printStackTrace(System.out);
        }

        this.detach();

        //getMapComposer().showMessage("Reference number to retrieve results: " + pid);

        //showInfoWindow("/output/maxent/" + pid + "/species.html");
    }

    private void loadMapSxS(Event event) {
        try {
            if (sxsOccurrenceDensity) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:odensity_" + pid + "&styles=odensity_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:odensity_" + pid
                        + "&STYLE=odensity_" + pid;

                System.out.println(legendurl);

                String layername = getMapComposer().getNextAreaLayerName("Occurrence Density");
                getMapComposer().addWMSLayer(pid + "_odensity", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.ODENSITY, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_odensity");
                ml.setData("pid", pid + "_odensity");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/odensity_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    ml.setMapLayerMetadata(md);
                }
                md.setMoreInfo(infoUrl + "\nOccurrence Density\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            if (sxsSpeciesDensity) {
                String mapurl = CommonData.geoServer + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:srichness_" + pid + "&styles=srichness_" + pid + "&FORMAT=image%2Fpng";
                String legendurl = CommonData.geoServer
                        + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                        + "&LAYER=ALA:srichness_" + pid
                        + "&STYLE=srichness_" + pid;

                System.out.println(legendurl);

//                String layername = tToolName.getValue();
                String layername = getMapComposer().getNextAreaLayerName("Species Richness");
                getMapComposer().addWMSLayer(pid + "_srichness", layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.SRICHNESS, null, null);
                MapLayer ml = getMapComposer().getMapLayer(pid + "_srichness");
                ml.setData("pid", pid + "_srichness");
                String infoUrl = CommonData.satServer + "/output/sitesbyspecies/" + pid + "/srichness_metadata.html";
                MapLayerMetadata md = ml.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    ml.setMapLayerMetadata(md);
                }
                md.setMoreInfo(infoUrl + "\nSpecies Richness\npid:" + pid);
                md.setId(Long.valueOf(pid));
            }

            // set off the download as well
            String fileUrl = CommonData.satServer + "/ws/download/" + pid;
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "sites_by_species.zip");
        } catch (Exception ex) {
            System.out.println("Error generating download for prediction model:");
            ex.printStackTrace(System.out);
        }

        this.detach();
    }

    boolean getParametersGdm() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            //TODO: analysis output url into config
            sbProcessUrl.append(CommonData.satServer.replace("/alaspatial", "") + "/output/gdm/").append(pid).append("/ala.properties");

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

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
            e.printStackTrace();
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

            System.out.println(legendurl);

            String layername = "Tranformed " + CommonData.getLayerDisplayName(env);
            //System.out.println("Converting '" + env + "' to '" + layername.substring(10) + "' (" + CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(env)) + ")");
            getMapComposer().addWMSLayer(pid + "_" + env, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilities.GDM, null, null);
            MapLayer ml = getMapComposer().getMapLayer(pid + "_" + env);
            ml.setData("pid", pid + "_" + env);
            String infoUrl = CommonData.satServer + "/output/gdm/" + pid + "/gdm.html";
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setMoreInfo(infoUrl + "\nGDM Output\npid:" + pid);
            md.setId(Long.valueOf(pid));
        }

        String fileUrl = CommonData.satServer + "/ws/download/" + pid;
        try {
            Filedownload.save(new URL(fileUrl).openStream(), "application/zip", "gdm_" + pid + ".zip"); // "ALA_Prediction_"+pid+".zip"
        } catch (Exception e) {
            e.printStackTrace();
        }

         this.detach();
    }
}
