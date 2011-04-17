/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import java.io.UnsupportedEncodingException;
import org.ala.spatial.util.LayersUtil;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class ALOCWCController extends UtilityComposer {

    private Intbox groupCount;
    Tabbox tabboxclassification;
    int generation_count = 1;
    String pid;
    String layerLabel;
    String legendPath;
    LayersUtil layersUtil;
    EnvironmentalList lbListLayers;
    Button btnProduce;
    Window wInputBox;

    @Override
    public void afterCompose() {
        super.afterCompose();

        layersUtil = new LayersUtil(getMapComposer(), CommonData.satServer);

        lbListLayers.init(getMapComposer(), CommonData.satServer, true);
    }

    public void onClick$btnProduce(Event event) {
        runclassification();
    }

    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
    }

    public void runclassification() {
        try {
            StringBuffer sbenvsel = new StringBuffer();
            String[] selectedLayers = lbListLayers.getSelectedLayers();
            if (selectedLayers.length > 1 && selectedLayers.length <= 50) {
                int i = 0;
                for (i = 0; i < selectedLayers.length; i++) {
                    sbenvsel.append(selectedLayers[i]);
                    if (i < selectedLayers.length - 1) {
                        sbenvsel.append(":");
                    }
                }
            } else {
                if (selectedLayers.length <= 0) {
                    Messagebox.show("Please select two or more environmental layers in step 1.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                } else {
                    Messagebox.show(selectedLayers.length + " layers selected.  Please select fewer than 50 environmental layers in step 1.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                }
                //highlight step 1
                tabboxclassification.setSelectedIndex(0);
                return;
            }

            if (groupCount.getValue() <= 1 || groupCount.getValue() > 200) {
                Messagebox.show("Please enter the number of groups to generate (2 to 200) in step 2.", "ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                //highlight step 2
                tabboxclassification.setSelectedIndex(1);
                return;
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/ws/aloc/processgeoq?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(String.valueOf(groupCount.getValue()), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            
            String area = null;//getMapComposer().getSelectionArea();
            if (area == null || area.length() == 0) {
                area = "none";
            }

            HttpClient client = new HttpClient();
            //GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);

            String slist = get.getResponseBodyAsString();

            layerLabel = "Classification #" + generation_count + " - " + groupCount.getValue() + " groups";

            generation_count++;
            pid = slist;

            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");

            getMapComposer().updateUserLogAnalysis("Classification", "gc: " + groupCount.getValue() + ";area: " + area, sbenvsel.toString(), slist, pid, layerLabel);

            ALOCProgressWCController window = (ALOCProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisALOCProgress.zul", this, null);
            window.parent = this;
            window.start(pid);
            window.doModal();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
    
    public void onClick$previousModel(Event event) {
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
        ALOCProgressWCController window = (ALOCProgressWCController) Executions.createComponents("WEB-INF/zul/AnalysisALOCProgress.zul", this, null);
        window.parent = this;
        window.start(pid);
        try {
            window.doModal();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * populate sampling screen with values from active layers and areas tab
     */
    public void callPullFromActiveLayers() {
        //get list of env/ctx layers
        String[] layers = layersUtil.getActiveEnvCtxLayers();

        /* set as selected each envctx layer found */
        if (layers != null) {
            lbListLayers.selectLayers(layers);
        }
        lbListLayers.updateDistances();
    }

    double[] getExtents() {
        double[] d = new double[6];
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(CommonData.satServer + "/alaspatial/output/aloc/" + pid + "/aloc.pngextents.txt");

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
        String uri = CommonData.satServer + "/alaspatial/output/layers/" + pid + "/img.png";
        float opacity = Float.parseFloat("0.75");

        List<Double> bbox = new ArrayList<Double>();

        double[] d = getExtents();
        bbox.add(d[2]);
        bbox.add(d[3]);
        bbox.add(d[4]);
        bbox.add(d[5]);

        //get job inputs
        try {
            for (String s : getJob("inputs").split(";")) {
                if (s.startsWith("gc")) {
                    layerLabel = "Classification #" + generation_count + " - " + s.split(":")[1] + " groups";
                    generation_count++;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (layerLabel == null) {
            layerLabel = "Classification #" + generation_count;
            generation_count++;
        }
        legendPath = "";
        try {
            legendPath = "/WEB-INF/zul/AnalysisClassificationLegend.zul?pid=" + pid + "&layer=" + URLEncoder.encode(layerLabel, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        getMapComposer().addImageLayer(pid, layerLabel, uri, opacity, bbox, LayerUtilities.ALOC);
        MapLayer mapLayer = getMapComposer().getMapLayer(layerLabel);
        if (mapLayer != null) {
            WMSStyle style = new WMSStyle();
            style.setName("Default");
            style.setDescription("Default style");
            style.setTitle("Default");
            style.setLegendUri(legendPath);

            System.out.println("legend:" + legendPath);
            mapLayer.addStyle(style);
            mapLayer.setSelectedStyleIndex(1);

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
            }

            String infoUrl = CommonData.satServer + "/alaspatial/output/layers/" + pid + "/metadata.html" + "\nClassification output\npid:"+pid;
            md.setMoreInfo(infoUrl);
            md.setId(Long.valueOf(pid)); 

            Events.echoEvent("openUrl", this.getMapComposer(), infoUrl);
        }
    }

    void getParameters() {
        String txt = get("inputs");
        try {
            int pos = 0;
            int p1 = txt.indexOf("pid:", pos);
            if (p1 < 0) {
                return;
            }
            int p2 = txt.indexOf("gc:", pos);
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
            groupCount.setValue(Integer.parseInt(gc));

            lbListLayers.clearSelection();
            lbListLayers.selectLayers(envlist.split(":"));
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
}
