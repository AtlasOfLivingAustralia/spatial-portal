/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ScrollEvent;
import org.zkoss.zul.*;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

/**
 * @author ajay
 */
public class GDMComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(GDMComposer.class);
    private Query query = null;
    private String sbenvsel = "";
    private String area = null;
    private Listbox lbenvlayers;
    private Button btnClearlbenvlayers;
    private Listbox cutpoint;
    private Radiogroup rgdistance;
    private Combobox weighting;
    private Checkbox useSubSample;
    private Textbox sitePairsSize;
    private Slider sitesslider;
    private Label sitesslidermin, sitesslidermax, sitessliderper, sitessliderdef;
    private Hbox sliderbox;
    private double maxScroll = 100;

    private String step1Id = null;
    private String step2Id = null;
    private String statusMsg = "";
    private long startTime;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = StringConstants.GDM;
        this.totalSteps = 5;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true, false, true);
        this.updateWindowTitle();

        try {

            sitesslider.addEventListener("onScrolling", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    if (event instanceof ScrollEvent) {
                        ScrollEvent se = (ScrollEvent) event;
                        double a = se.getPos();
                        double b = a / maxScroll;
                        long p = Math.round(b * 100);
                        sitesslider.setSlidingtext(Math.round(a) + " - " + p + "%");
                    }
                }
            });

        } catch (Exception e) {
            LOGGER.debug("Error in slider");
            LOGGER.debug(e.getMessage());
        }

    }

    public void onClick$btnClearlbenvlayers(Event event) {
        lbenvlayers.clearSelection();

        // check if lbListLayers is empty as well,
        // if so, then disable the next button
        if (lbenvlayers.getSelectedCount() == 0) {
            btnOk.setDisabled(true);
        }
    }

    public void onSelect$lbenvlayers(Event event) {
        btnOk.setDisabled(lbenvlayers.getSelectedCount() < 1);
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My GDM"));

    }

    @Override
    public boolean onFinish() {

        if (query == null) {
            getMapComposer().showMessage("There is a problem selecting the species.  Try to select the species again", this);
            return false;
        }
        if (searchSpeciesACComp.hasValidItemSelected()) {
            getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComp, getSelectedArea(), getGeospatialKosher(), false);
        } else if (rgSpecies.getSelectedItem() != null && StringConstants.MULTIPLE.equals(rgSpecies.getSelectedItem().getValue())) {
            getMapComposer().mapSpecies(query, StringConstants.SPECIES_ASSEMBLAGE, StringConstants.SPECIES, 0, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY, Util.nextColour(), false);
        }

        return rungdm();
    }

    public void onClick$btnOk(Event event) {
        LOGGER.debug("Completing step " + currentStep + " for GDM");
        if (currentStep == 3) {
            LOGGER.debug("checking with server for step 1");
            boolean step1 = runGDMStep1();

            return;
        }

        super.onClick$btnOk(event);

    }

    public boolean runGDMStep1() {

        try {
            SelectedArea sa = getSelectedArea();
            query = QueryUtil.queryFromSelectedArea(getSelectedSpecies(), sa, false, getGeospatialKosher());
            query = query.newFacet(new Facet("occurrence_status_s", "absent", false), false);
            sbenvsel = getSelectedLayersWithDisplayNames();

            if (query.getSpeciesCount() < 2) {
                getMapComposer().showMessage("An list of species with multiple occurrences for each species is required by GDM.", this);
                return false;
            }

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.getSatServer() + "/ws/gdm/step1?"
                    + "envlist=" + URLEncoder.encode(sbenvsel, StringConstants.UTF_8)
                    + "&taxacount=" + query.getSpeciesCount()
                    + "&speciesq=" + URLEncoder.encode(QueryUtil.queryFromSelectedArea(query, sa, false, getGeospatialKosher()).getQ(), StringConstants.UTF_8)
                    + "&bs=" + URLEncoder.encode(query.getBS(), StringConstants.UTF_8));

            if (sa.getMapLayer() != null && sa.getMapLayer().getEnvelope() != null) {
                area = StringConstants.ENVELOPE + "(" + sa.getMapLayer().getEnvelope() + ")";
            } else {
                area = sa.getWkt();
            }
            if (getSelectedArea() != null) {
                get.addParameter(StringConstants.AREA, area);
            }

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            LOGGER.debug("calling gdm ws step 1");
            client.executeMethod(get);

            step1Id = get.getResponseBodyAsString();

            //wait for step 1
            LOGGER.debug(step1Id);

            getFellow("runningMsg1").setVisible(true);
            statusMsg = ((Label) getFellow("runningMsg1")).getValue();
            startTime = System.currentTimeMillis();

            Events.echoEvent("step1Status", this, null);

            btnOk.setDisabled(true);

            return true;

        } catch (Exception e) {
            LOGGER.error("GDM error: ", e);
            getMapComposer().showMessage("Unknown error.", this);
        }

        return false;
    }

    public void step1Status(Event event) {

        try {
            String response = Util.readUrl(CommonData.getSatServer() + "/ws/gdm/step1/status?id=" + step1Id);
            if (response != null) {
                if (response.startsWith("error")) {
                    this.detach();

                    getMapComposer().showMessage("GDM error");

                    LOGGER.error("GDM error for step 1. id=" + step1Id);

                    return;
                } else if (!response.startsWith("running")) {
                    Scanner s = new Scanner(response);

                    pid = s.nextLine();

                    // ignore the header
                    s.nextLine();

                    if (!cutpoint.getItems().isEmpty()) {
                        cutpoint.getItems().clear();

                        Listitem li = new Listitem();
                        Listcell lc;

                        lc = new Listcell("0");
                        lc.setParent(li);

                        lc = new Listcell("All records");
                        lc.setParent(li);

                        lc = new Listcell("All records");
                        lc.setParent(li);

                        li.setValue("0");
                        li.setParent(cutpoint);
                    }

                    while (s.hasNext()) {
                        Listitem li = new Listitem();
                        Listcell lc;
                        String[] sxs = s.nextLine().split(",");

                        lc = new Listcell(sxs[0]);
                        lc.setParent(li);

                        lc = new Listcell(sxs[1]);
                        lc.setParent(li);

                        lc = new Listcell(sxs[2]);
                        lc.setParent(li);

                        li.setValue(sxs[0]);
                        li.setParent(cutpoint);
                    }

                    cutpoint.setSelectedIndex(0);

                    if (cutpoint.getItemCount() < 2) {
                        this.detach();
                        getMapComposer().showMessage("An assemblage of species with multiple occurrences \nfor each species is required by GDM.");
                    }

                    // setup the range slider for the sub samples
                    // 500 * 1024 * 1024 bytes
                    double maxBytes = 524288000;

                    double maxS = maxBytes / ((lbListLayers.getSelectedCount() * 3) + 1) / 8;
                    // 10% of maxScroll
                    double minS = (int) (maxS * 0.1);

                    this.maxScroll = maxS;

                    sitesslider.setCurpos((int) minS);
                    sitesslider.setMaxpos((int) maxS);
                    sitePairsSize.setValue(Long.toString(Math.round(minS)));
                    sitessliderdef.setValue(Long.toString(Math.round(minS)));
                    sitesslidermax.setValue(Long.toString(Math.round(maxS)));

                    //continue GDM
                    btnOk.setDisabled(false);
                    super.onClick$btnOk(event);

                    return;
                }
            }
        } catch (Exception e) {
            //might be a timeout on the progress check, continue
            LOGGER.error("error in GDM step1. id:" + step1Id, e);
        }

        //repeat after 5s wait
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ((Label) getFellow("runningMsg1")).setValue(statusMsg + " " + (System.currentTimeMillis() - startTime) / 1000 + "s");
        Events.echoEvent("step1Status", this, null);
    }

    public void step2Status(Event event) {
        try {
            String response = Util.readUrl(CommonData.getSatServer() + "/ws/gdm/step2/status?id=" + step2Id);
            if (response != null) {
                if (response.startsWith("error")) {
                    this.detach();

                    getMapComposer().showMessage("GDM error");

                    LOGGER.error("GDM error for step 2. id=" + step2Id);

                    return;
                } else if (!response.startsWith("running")) {

                    pid = response.replace("\n", "");

                    loadMap(null);

                    this.setVisible(false);

                    String fileUrl = CommonData.getSatServer() + "/ws/download/" + pid;
                    Filedownload.save(new URL(fileUrl).openStream(), "application/zip", tToolName.getValue().replaceAll(" ", "_") + ".zip");

                    String options = "";
                    options += "cutpoint: " + cutpoint.getSelectedItem().getValue();
                    options += ";useDistance: " + rgdistance.getSelectedItem().getValue();
                    options += ";weighting: " + weighting;
                    options += ";useSubSample: " + (useSubSample.isChecked() ? "1" : "0");
                    options += ";sitePairsSize: " + sitePairsSize.getValue();
                    if (query instanceof BiocacheQuery) {
                        BiocacheQuery bq = (BiocacheQuery) query;
                        options = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + options;
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, bq.getLsids(), sbenvsel, pid, options, StringConstants.STARTED);
                    } else {
                        remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Prediction", area, query.getName() + "__" + query.getQ(), sbenvsel, pid, options, StringConstants.STARTED);
                    }

                    //finished
                    this.detach();

                    return;
                }
            }
        } catch (Exception e) {
            //might be a timeout on the progress check, continue
            LOGGER.error("error checking GDM status. step2Id:" + pid, e);
        }

        //repeat after 1s wait
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ((Label) getFellow("runningMsg2")).setValue(statusMsg + " " + (System.currentTimeMillis() - startTime) / 1000 + "s");
        btnOk.setDisabled(true);
        Events.echoEvent("step2Status", this, null);
    }

    public void onScroll$sitesslider(Event event) {
        double a = sitesslider.getCurpos();
        double b = a / maxScroll;
        long p = Math.round(b * 100);
        sitePairsSize.setValue(Integer.toString(sitesslider.getCurpos()));
        sitessliderper.setValue(p + "%");
    }

    public void onBlur$sitePairsSize(Event event) {
        try {
            sitesslider.setCurpos(Integer.parseInt(sitePairsSize.getValue()));
            onScroll$sitesslider(event);
        } catch (NumberFormatException e) {
            sitesslider.setCurpos(Integer.parseInt(sitessliderdef.getValue()));
        }
    }

    public void onCheck$useSubSample(Event event) {
        if (useSubSample.isChecked()) {
            sitePairsSize.setDisabled(true);
            sliderbox.setVisible(false);
        } else {
            sitePairsSize.setDisabled(false);
            sliderbox.setVisible(true);
            sitesslider.setCurpos(Integer.parseInt(sitePairsSize.getValue()));
        }
    }

    public boolean rungdm() {
        try {
            StringBuilder sbProcessUrl = new StringBuilder();
            sbProcessUrl.append(CommonData.getSatServer()).append("/ws/gdm/step2?");
            sbProcessUrl.append("&pid=").append(pid);
            sbProcessUrl.append("&cutpoint=").append(cutpoint.getSelectedItem().getValue());
            sbProcessUrl.append("&useDistance=").append(rgdistance.getSelectedItem().getValue());
            sbProcessUrl.append("&weighting=").append(weighting.getSelectedItem().getValue());
            sbProcessUrl.append("&useSubSample=").append(useSubSample.isChecked() ? "1" : "0");
            sbProcessUrl.append("&sitePairsSize=").append(sitePairsSize.getValue());
            sbProcessUrl.append("&name=").append(query.getName());

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());

            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.TEXT_PLAIN);

            LOGGER.debug("calling gdm ws: " + sbProcessUrl.toString());
            client.executeMethod(get);

            step2Id = get.getResponseBodyAsString();

            getFellow("runningMsg2").setVisible(true);
            statusMsg = ((Label) getFellow("runningMsg2")).getValue();
            startTime = System.currentTimeMillis();

            Events.echoEvent("step2Status", this, null);

            return false;

        } catch (Exception e) {
            LOGGER.error("error finalizing GDM", e);
        }

        return false;
    }

    public void loadMap(Event event) {

        String[] envlist = getSelectedLayersWithDisplayNames().split(":");

        for (String env2 : envlist) {
            String env = env2.split("\\|")[0];
            String displayName = env2.split("\\|")[1];

            String mapurl = CommonData.getGeoServer() + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:gdm_" + env + "Tran_" + pid + "&styles=alastyles&FORMAT=image%2Fpng";

            String legendurl = CommonData.getGeoServer()
                    + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=1"
                    + "&LAYER=ALA:gdm_" + env + "Tran_" + pid
                    + "&STYLE=alastyles";

            LOGGER.debug(legendurl);

            String layername = "Tranformed " + displayName;
            getMapComposer().addWMSLayer(pid + "_" + env, layername, mapurl, (float) 0.5, null, legendurl, LayerUtilitiesImpl.GDM, null, null);
            MapLayer ml = getMapComposer().getMapLayer(pid + "_" + env);
            ml.setPid(pid + "_" + env);
            String infoUrl = CommonData.getSatServer() + "/output/gdm/" + pid + "/gdm.html";
            MapLayerMetadata md = ml.getMapLayerMetadata();

            md.setMoreInfo(infoUrl + "\nGDM Output\npid:" + pid);
            md.setId(Long.valueOf(pid));

        }
    }


    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                lbListLayers.setFocus(true);
                break;
            case 4:

                break;
            case 5:
                tToolName.setFocus(true);
                break;
            default:
                LOGGER.error("invalid step for GDMComposer: " + currentStep);
        }
    }
}
