package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerSelection;
import au.org.emii.portal.util.LayerUtilities;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.logger.client.RemoteLogger;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.SelectedArea;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddToolComposer extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    RemoteLogger remoteLogger;
    int currentStep = 1, totalSteps = 5;
    Map<String, Object> params;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea, rgAreaHighlight, rgSpecies, rgSpeciesBk;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    Radio rSpeciesAll, rSpeciesMapped, rSpeciesSearch, rSpeciesUploadLSID, rSpeciesUploadSpecies, rMultiple;
    Radio rSpeciesNoneBk, rSpeciesAllBk, rSpeciesMappedBk, rSpeciesSearchBk, rSpeciesUploadLSIDBk, rSpeciesUploadSpeciesBk, rMultipleBk;
    Radio rAreaWorld, rAreaCustom, rAreaWorldHighlight, rAreaSelected;
    Button btnCancel, btnOk, btnBack, btnHelp;
    Textbox tToolName;
    SpeciesAutoComplete searchSpeciesAuto, bgSearchSpeciesAuto;
    EnvironmentalList lbListLayers;
    Div divSpeciesSearch, divSpeciesSearchBk;    
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
    String winTop = "300px";
    String winLeft = "500px";
    //boolean setCustomArea = false;
    boolean hasCustomArea = false;
    MapLayer prevTopArea = null;
    Fileupload fileUpload;
    SelectedLayersCombobox selectedLayersCombobox;
    Div tlinfo;
    Textbox tLayerList;
    Div dLayerSummary;
    EnvLayersCombobox cbLayer;
    Button bLayerListDownload1;
    Button bLayerListDownload2;
    Label lLayersSelected;
    Button btnClearSelection;
    Menupopup mpLayer2, mpLayer1;
    Doublebox dResolution;
    Vbox vboxMultiple,vboxMultipleBk;
    SpeciesAutoComplete mSearchSpeciesAuto, mSearchSpeciesAutoBk;
    Textbox tMultiple, tMultipleBk;
    Listbox lMultiple, lMultipleBk;

    @Override
    public void afterCompose() {
        super.afterCompose();

        winTop = this.getTop();
        winLeft = this.getLeft();

        setupDefaultParams();
        setParams(Executions.getCurrent().getArg());

        //loadStepLabels();
        updateWindowTitle();

        fixFocus();

        if(lbListLayers != null) {
            lbListLayers.clearSelection();
            lbListLayers.updateDistances();
        }

        //init mpLayer1 and mpLayer2
        if(mpLayer1 != null && mpLayer2 != null) {
            for(MapLayer ml : getMapComposer().getGridLayers()) {
                //get layer name
                String name = null;
                String url = ml.getUri();
                int p1 = url.indexOf("ALA:") + 4;
                int p2 = url.indexOf("&",p1);
                if(p1 > 4) {
                    if(p2 < 0) p2 = url.length();
                    name = url.substring(p1,p2);
                }

                //cbLayer1
                Menuitem mi = new Menuitem(ml.getDisplayName());
                mi.setValue(name);
                mi.addEventListener("onClick", new EventListener() {

                    public void onEvent(Event event) throws Exception {
                        Menuitem mi = (Menuitem) event.getTarget();
                        cbLayer1.setValue(mi.getValue());
                        cbLayer1.refresh(mi.getValue());
                        for(Object o: cbLayer1.getItems()) {
                            Comboitem ci = (Comboitem) o;
                            JSONObject jo = (JSONObject) ci.getValue();
                            if(jo.getString("name").equals(mi.getValue())) {
                                cbLayer1.setSelectedItem(ci);
                                cbLayer1.setText(ci.getLabel());
                                toggles();
                                return;
                            }
                        }
                    }
                });
                mi.setParent(mpLayer1);

                //cbLayer2
                mi = new Menuitem(ml.getDisplayName());
                mi.setValue(name);
                mi.addEventListener("onClick", new EventListener() {

                    public void onEvent(Event event) throws Exception {
                        Menuitem mi = (Menuitem) event.getTarget();
                        cbLayer2.setValue(mi.getValue());
                        cbLayer2.refresh(mi.getValue());
                        for(Object o: cbLayer2.getItems()) {
                            Comboitem ci = (Comboitem) o;
                            JSONObject jo = (JSONObject) ci.getValue();
                            if(jo.getString("name").equals(mi.getValue())) {
                                cbLayer2.setSelectedItem(ci);
                                cbLayer2.setText(ci.getLabel());
                                toggles();
                                return;
                            }
                        }
                    }
                });
                mi.setParent(mpLayer2);
            }
        }

        if(mSearchSpeciesAuto != null) {
            mSearchSpeciesAuto.setBiocacheOnly(true);
        }
        if(mSearchSpeciesAutoBk != null) {
            mSearchSpeciesAutoBk.setBiocacheOnly(true);
        }
    }

    private void setupDefaultParams() {
        Hashtable<String, Object> p = new Hashtable<String, Object>();
        p.put("step1", "Select area(s)");
        p.put("step2", "Select species(s)");
        p.put("step3", "Select grid(s)");
        p.put("step4", "Select your analytical options");
        p.put("step5", "Name your output for");

        if (params == null) {
            params = p;
        } else {
            setParams(p);
        }

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
//        if(currentDiv.getZclass().contains("download")) {
//            btnOk.setLabel("Download");
//        } else if (currentDiv.getZclass().contains("last")) {
//            btnOk.setLabel("Finish");
//        } else {
//            btnOk.setLabel("Next >");
//        }
        btnOk.setLabel("Next >");
    }

    public void updateWindowTitle() {
        this.setTitle("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
    }

    public void updateName(String name) {
        if (tToolName != null) {
            tToolName.setValue(name);
        }
    }

    private void loadSummaryDetails() {
        try {
            Div atsummary = (Div) getFellowIfAny("atsummary");
            if (atsummary != null) {
                String summary = "";
                summary += "<strong>Analytical tool</strong>: " + selectedMethod;
                summary += "<strong>Area</strong>: ";
                summary += "<strong>Species</strong>: ";
                summary += "<strong>Grids</strong>: ";
                summary += "<strong>Additional options</strong>: ";
                atsummary.setContext(summary);
            }
        } catch (Exception e) {
        }
    }

    public void setParams(Map<String, Object> params) {
        //this.params = params;

        // iterate thru' the passed params and load them into the
        // existing default params
        if (params == null) {
            setupDefaultParams();
        }
        if (params != null && params.keySet() != null && params.keySet().iterator() != null) {
            Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                this.params.put(key, params.get(key));
            }
        } else {
            this.params = params;
        }
    }

    public void loadSpeciesLayers() {
        loadSpeciesLayers(false);
    }
    public void loadSpeciesLayers(boolean biocacheOnly) {
        try {

            Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpecies");
            Radio rSpeciesMapped = (Radio) getFellowIfAny("rSpeciesMapped");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

            Radio selectedSpecies = null;
            String selectedSpeciesLayer = (String) params.get("speciesLayerName");
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if(biocacheOnly
                        && lyr.getData("query") != null
                        && !(lyr.getData("query") instanceof BiocacheQuery)) {
                    continue;
                }
                if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rgSpecies.insertBefore(rSp, rSpeciesMapped);

                if (selectedSpeciesLayer != null && rSp.getValue().equals(selectedSpeciesLayer)) {
                    selectedSpecies = rSp;
                }
            }

            if (speciesLayersCount > 1) {
                rSpeciesMapped.setLabel("All " + speciesLayersCount + " species currently mapped (excludes coordinate uploads)");
            } else {
                rSpeciesMapped.setVisible(false);
            }

            if (selectedSpecies != null) {
                rgSpecies.setSelectedItem(selectedSpecies);
            } else if (selectedSpeciesLayer != null && selectedSpeciesLayer.equals("none")) {
                rgSpecies.setSelectedItem(rSpeciesAll);
            } else if (layers.size() > 0) {
                rgSpecies.setSelectedItem(rgSpecies.getItemAtIndex(1));
            } else {
                for (int i = 0; i < rgSpecies.getItemCount(); i++) {
                    if (rgSpecies.getItemAtIndex(i).isVisible()
                            && rgSpecies.getItemAtIndex(i) != rSpeciesAll) {
                        rgSpecies.setSelectedItem(rgSpecies.getItemAtIndex(i));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadSpeciesLayersBk() {
        try {
            Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpeciesBk");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rgSpecies.insertBefore(rSp, rSpeciesMapped);
            }

            if (speciesLayersCount > 1) {
                rSpeciesMapped.setLabel("All " + speciesLayersCount + " species currently mapped (excludes coordinate uploads)");
            } else {
                rSpeciesMapped.setVisible(false);
            }
        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaLayers() {
        loadAreaLayers(null);
    }

    public void loadAreaLayers(String selectedAreaName) {
        try {
            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgArea");
            //remove all radio buttons that don't have an id
            for (int i = rgArea.getItemCount() - 1; i >= 0; i--) {
                String id = ((Radio) rgArea.getItems().get(i)).getId();
                if (id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            StringBuilder allWKT = new StringBuilder();
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                //rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());

                if (!lyr.getWKT().contains("ENVELOPE")) {
                    if (count_not_envelopes > 0) {
                        allWKT.append(',');
                    }
                    count_not_envelopes++;
                    String wkt = lyr.getWKT();
                    if (wkt.startsWith("GEOMETRYCOLLECTION(")) {
                        wkt = wkt.substring("GEOMETRYCOLLECTION(".length(), wkt.length() - 1);
                    }
                    allWKT.append(wkt);
                }

                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                }
            }

            if (!layers.isEmpty() && count_not_envelopes > 1) {
                Radio rAr = new Radio("All area layers"
                        + ((count_not_envelopes < layers.size()) ? " (excluding Environmental Envelopes)" : ""));
                //rAr.setId("AllActiveAreas");
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT.toString() + ")");
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        //rgArea.getItemAtIndex(i).setSelected(true);
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        System.out.println("2.resetting indexToSelect = " + i);
                        rgArea.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rAreaSelected = rSelectedLayer;
                rgArea.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArea.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
                rgArea.setSelectedItem(rAreaSelected);
            } else {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaHighlightLayers(String selectedAreaName) {
        try {
            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            //remove all radio buttons that don't have an id
            for (int i = rgArea.getItemCount() - 1; i >= 0; i--) {
                String id = ((Radio) rgArea.getItems().get(i)).getId();
                if (id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrentHighlight = (Radio) getFellowIfAny("rAreaCurrentHighlight");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            StringBuilder allWKT = new StringBuilder();
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                //rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());

                if (!lyr.getWKT().contains("ENVELOPE")) {
                    if (count_not_envelopes > 0) {
                        allWKT.append(',');
                    }
                    count_not_envelopes++;
                    String wkt = lyr.getWKT();
                    if (wkt.startsWith("GEOMETRYCOLLECTION(")) {
                        wkt = wkt.substring("GEOMETRYCOLLECTION(".length(), wkt.length() - 1);
                    }
                    allWKT.append(wkt);
                }


                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrentHighlight);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    //rAreaSelected = rAr;
                }
            }

            if (!layers.isEmpty() && count_not_envelopes > 1) {
                Radio rAr = new Radio("All area layers"
                        + ((count_not_envelopes < layers.size()) ? " (excluding Environmental Envelopes)" : ""));
                //rAr.setId("AllActiveAreas");
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT.toString() + ")");
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrentHighlight);
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        //rgArea.getItemAtIndex(i).setSelected(true);
                        //rAreaSelected = rgArea.getItemAtIndex(i);
                        System.out.println("2.resetting indexToSelect = " + i);
                        rgArea.setSelectedItem(rgArea.getItemAtIndex(i));
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                //rAreaSelected = rSelectedLayer;
                rgArea.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArea.setSelectedItem(rAreaWorld);
                //rAreaSelected = rAreaWorld;
                //rgArea.setSelectedItem(rAreaSelected);
            } else {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        //rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rgArea.getItemAtIndex(i));
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rgArea.getSelectedItem().getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaLayersHighlight() {
        try {

            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrentHighlight");
            Radio rAreaNone = (Radio) getFellowIfAny("rAreaNoneHighlight");

//            String selectedLayerName = (String) params.get("polygonLayerName");
//            Radio rSelectedLayer = null;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);

//                if(selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
//                    rSelectedLayer = rAr;
//                }
            }

            rAreaNone.setSelected(true);
        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadGridLayers(boolean environmentalOnly, boolean fullList) {
        if (selectedLayersCombobox != null) {
            selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer());
        }
        try {

            if (fullList) {
                lbListLayers.init(getMapComposer(), CommonData.satServer, environmentalOnly);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    //System.out.println(lyr.getDisplayName());
                }
            }

            String layers = (String) params.get("environmentalLayerName");
            if (layers != null) {
                lbListLayers.selectLayers(layers.split(","));
            }

            lbListLayers.renderAll();
        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        //setCustomArea = false;
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        if (rAreaSelected == rAreaCustom) {
            //setCustomArea = true;
            hasCustomArea = false;
        }
    }

    public void onCheck$rgAreaHighlight(Event event) {
        if (rgAreaHighlight == null) {
            return;
        }
        //setCustomArea = false;
        hasCustomArea = false;
        if (rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight")) {
            //setCustomArea = true;
            hasCustomArea = false;
        }
    }

    public void onCheck$rgSpecies(Event event) {
        if (rgSpecies == null) {
            return;
        }
        Radio selectedItem = rgSpecies.getSelectedItem();
        try {
            selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        try {
            //Check to see if we are perform a normal or background upload
            if (rgSpecies != null && selectedItem == rSpeciesSearch) {
                if (divSpeciesSearch != null) {
                    divSpeciesSearch.setVisible(true);
                    vboxMultiple.setVisible(false);
                    if (event != null) {
                        toggles();
                    }
                    return;
                }
            }
            if (divSpeciesSearch != null) {
                divSpeciesSearch.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadSpecies
                    || selectedItem == rSpeciesUploadLSID) {
                btnOk.setVisible(false);
                fileUpload.setVisible(true);
                vboxMultiple.setVisible(false);
            } else if(rMultiple != null && rMultiple.isSelected()) {
                vboxMultiple.setVisible(true);
            } else {
                btnOk.setDisabled(false);
                vboxMultiple.setVisible(false);
            }

            if (event != null) {
                toggles();
            }
        } catch (Exception e) {
        }
    }

    public void onCheck$rgSpeciesBk(Event event) {
        if (rgSpeciesBk == null) {
            return;
        }
        Radio selectedItem = rgSpeciesBk.getSelectedItem();
        try {
            selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        try {            
            if (rgSpeciesBk != null && selectedItem == rSpeciesSearchBk) {
                if (divSpeciesSearchBk != null) {
                    divSpeciesSearchBk.setVisible(true);
                    vboxMultipleBk.setVisible(false);
                    if (event != null) {
                        toggles();
                    }
                    return;
                }
            }

            if (divSpeciesSearchBk != null) {
                divSpeciesSearchBk.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadSpeciesBk || selectedItem == rSpeciesUploadLSIDBk) {
                btnOk.setVisible(false);
                fileUpload.setVisible(true);
            }

            if(rMultipleBk != null && rMultipleBk.isSelected()) {
                vboxMultipleBk.setVisible(true);
            } else {
                vboxMultipleBk.setVisible(false);
            }

            if (event != null) {
                toggles();
            }
        } catch (Exception e) {
        }
    }

    public void onChange$searchSpeciesAuto(Event event) {
        toggles();
    }

    public void onClick$btnHelp(Event event) {
        String helpurl = "";

        if (selectedMethod.equals("Prediction")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-prediction-tab/";
        } else if (selectedMethod.equals("Sampling")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-sampling-tab/";
        } else if (selectedMethod.equals("Classification")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-classification-tab/";
        } else if (selectedMethod.equals("Scatterplot")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/scatterplot-tab/";
        }

        if (StringUtils.isNotBlank(helpurl)) {
            getMapComposer().activateLink(helpurl, "Help", false, "");
        }
    }

    public void onClick$btnCancel(Event event) {
        currentStep = 1;
        if (lbListLayers != null) {
            lbListLayers.clearSelection();            
            toggles();
        }
        this.detach();
    }

    public void onClick$btnBack(Event event) {

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
        Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep - 1))) : null;



        if (currentDiv.getZclass().contains("first")) {
            //currentStep = 1;
            //this.detach();
            btnBack.setDisabled(true);
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);

            Image currentStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep - 1));
            currentStepCompletedImg.setVisible(false);

            Label nextStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
            nextStepLabel.setStyle("font-weight:normal");

            Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep - 1));
            currentStepLabel.setStyle("font-weight:bold");

            currentStep--;

            if (previousDiv != null) {
                //btnCancel.setLabel(((!previousDiv.getZclass().equalsIgnoreCase("first")) ? "< Back" : "Cancel"));
                btnBack.setDisabled(((!previousDiv.getZclass().contains("first")) ? false : true));
            }
        }

        btnOk.setLabel("Next >");
        toggles();
        updateWindowTitle();
        displayTrafficLightInfo();

    }

    private void displayTrafficLightInfo() {
        if (tlinfo != null) {
            if (selectedMethod.equalsIgnoreCase("Prediction") && currentStep == 3) {
                tlinfo.setVisible(true);
            } else if (selectedMethod.equalsIgnoreCase("Classification") && currentStep == 2) {
                tlinfo.setVisible(true);
            } else {
                tlinfo.setVisible(false);
            }
        }
    }

    public void resetWindowFromSpeciesUpload(String lsid, String type) {
        try {
            if (type.compareTo("cancel") == 0) {
//                this.setTop(winTop);
//                this.setLeft(winLeft);
//                this.doModal();
                fixFocus();
                return;
            }
            if (type.compareTo("normal") == 0) {
                setLsid(lsid);
            }
            if (type.compareTo("bk") == 0) {
                setLsidBk(lsid);
            }
//            this.setTop(winTop);
//            this.setLeft(winLeft);
//            this.doModal();
//            onClick$btnOk(null);

//            fixFocus();
        } catch (Exception e) {
            System.out.println("Exception when resetting analysis window");
            e.printStackTrace();
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            if (selectedArea == null) {
                hasCustomArea = false;
            } else if (selectedArea.trim().equals("")) {
                hasCustomArea = false;
            } else {
                hasCustomArea = true;
            }

            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea = null;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    curTopArea = layers.get(0);
                } else {
                    curTopArea = null;
                }

                if (curTopArea != prevTopArea) {
                    if (isAreaHighlightTab()) {
                        loadAreaHighlightLayers(curTopArea.getDisplayName());
                    } else if (isAreaTab()) {
                        loadAreaLayers(curTopArea.getDisplayName());
                    }

                    ok = true;
                }
            }
            this.setTop(winTop);
            this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                onClick$btnOk(null);
                hasCustomArea = false;
                //setCustomArea = false;
            }

            fixFocus();
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException when resetting analysis window");
            ex.printStackTrace(System.out);
        } catch (SuspendNotAllowedException ex) {
            System.out.println("Exception when resetting analysis window");
            ex.printStackTrace(System.out);
        }
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }
        boolean successful = false;
        try {
            if (!hasCustomArea && (isAreaCustom() || isAreaHighlightCustom())) {
                this.doOverlapped();
                this.setTop("-9999px");
                this.setLeft("-9999px");

                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddTool");
                winProps.put("selectedMethod", selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
            Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep + 1))) : null;

            if (!currentDiv.getZclass().contains("last")) {
                currentDiv.setVisible(false);
                nextDiv.setVisible(true);

                Image previousStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep));
                previousStepCompletedImg.setVisible(true);

                Label previousStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
                previousStepLabel.setStyle("font-weight:normal");

                Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep + 1));
                currentStepLabel.setStyle("font-weight:bold");

                // now include the extra options for step 4
                if (nextDiv != null) {

                    if (nextDiv.getZclass().contains("last")) {
                        loadSummaryDetails();
                        onLastPanel();
                    }

//                    if(nextDiv.getZclass().contains("download")) {
//                        btnOk.setLabel("Download");
//                    } else if (currentDiv.getZclass().contains("last")) {
//                        btnOk.setLabel("Finish");
//                    } else {
//                        btnOk.setLabel("Next >");
//                    }
                    btnOk.setLabel("Next >");
                }

                currentStep++;

                successful = true;
            } else {
                saveLayerSelection();

                successful = onFinish();

                if(successful) {
                    currentStep = 1;
                }
            }

            if(successful) {
                if (nextDiv != null && nextDiv.getZclass().contains("last")) {
                    updateLayerListText();
                }

                btnBack.setDisabled(false);
                updateWindowTitle();
            }

        } catch (Exception ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        }

        toggles();
        displayTrafficLightInfo();

        fixFocus();
    }

    void fixFocus() {
        //set element of focus
    }

    public void onLastPanel() {
    }

    public boolean onFinish() {
        try {
            this.detach();
            Messagebox.show("Running your analysis tool: " + selectedMethod);

        } catch (InterruptedException ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
        }

        return true;
    }

    public void loadMap(Event event) {
    }

    public SelectedArea getSelectedArea() {
        //String area = rgArea.getSelectedItem().getValue();
        String area = rAreaSelected.getValue();
        SelectedArea sa = null;
        try {
            if (area.equals("current")) {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (area.equals("australia")) {
                sa = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
            } else if (area.equals("world")) {
                sa = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getWKT())) {
                        sa = new SelectedArea(ml, null);
                        break;
                    }
                }

                //for 'all areas'
                if (sa == null) {
                    sa = new SelectedArea(null, area);
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return sa;
    }

    public SelectedArea getSelectedAreaHighlight() {
        String area = rgAreaHighlight.getSelectedItem().getValue();

        SelectedArea sa = null;
        try {
            if (area.equals("none")) {
            } else if (area.equals("current")) {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            } else if (area.equals("australia")) {
                sa = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
            } else if (area.equals("world")) {
                sa = new SelectedArea(null, CommonData.WORLD_WKT);
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getDisplayName())) {
                        sa = new SelectedArea(ml, null);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return sa;
    }

    public Query getSelectedSpecies() {
        return getSelectedSpecies(false);
    }

    public Query getSelectedSpecies(boolean mapspecies) {
        Query q = null;

        String species = rgSpecies.getSelectedItem().getValue();

        MapLayer ml = getMapComposer().getMapLayer(species);
        if (ml != null) {
            q = (Query) ml.getData("query");
        } else {
            try {
                System.out.println("getSelectedSpecies: " + species);
                if (species.equals("allspecies")) {
                    species = "none";
                    q = new BiocacheQuery(null, null, null, null, false);
                } else if (species.equals("allmapped")) {

                    //                species = "";
                    //                List<MapLayer> layers = getMapComposer().getSpeciesLayers();
                    //
                    //                BiocacheQuery sq = new BiocacheQuery();
                    //                for (int i = 0; i < layers.size(); i++) {
                    //                    MapLayer lyr = layers.get(i);
                    //                    if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    //                        sq.addLsid(lyr.getMapLayerMetadata().getSpeciesLsid());
                    //                    }
                    //                }
                    //
                    //                species = sq.getShortQuery();
                    throw new UnsupportedOperationException("Not yet implemented");
                } else if(species.equals("multiple")) {
                    String lsids = getMultipleLsids();
                    if(lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids,null,null,null,false);
                    }
                } else if (species.equals("search") || species.equals("uploadSpecies") || species.equals("uploadLsid")) {
                    if (searchSpeciesAuto.getSelectedItem() != null) {
                        species = (String) (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
                        q = QueryUtil.get(species, getMapComposer(), false);
                    }
                }
            } catch (Exception e) {
                System.out.println("Unable to retrieve selected species");
                e.printStackTrace(System.out);
            }
        }

        return q;
    }

    public Query getSelectedSpeciesBk() {
        Query q = null;

        String species = rgSpeciesBk.getSelectedItem().getValue();

        MapLayer ml = getMapComposer().getMapLayer(species);
        if (ml != null) {
            q = (Query) ml.getData("query");
        } else {
            try {
                if (species.equals("none")) {
                    species = null;
                } else if (species.equals("allspecies")) {
                    species = "none";
                } else if (species.equals("allmapped")) {
                    //                species = "";
                    //                List<MapLayer> layers = getMapComposer().getSpeciesLayers();
                    //
                    //                BiocacheQuery sq = new BiocacheQuery();
                    //                for (int i = 0; i < layers.size(); i++) {
                    //                    MapLayer lyr = layers.get(i);
                    //                    if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    //                        sq.addLsid(lyr.getMapLayerMetadata().getSpeciesLsid());
                    //                    }
                    //                }
                    //
                    //                species = sq.getShortQuery();
                    throw new UnsupportedOperationException("Not yet implemented");
                } else if(species.equals("multiple")) {
                    String lsids = getMultipleLsidsBk();
                    if(lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids,null,null,null,false);
                    }
                } else if (species.equals("search") || species.equals("uploadSpecies") || species.equals("uploadLsid")) {
                    if (bgSearchSpeciesAuto == null) {
                        bgSearchSpeciesAuto = (SpeciesAutoComplete) getFellowIfAny("bgSearchSpeciesAuto");
                    }
                    if (bgSearchSpeciesAuto.getSelectedItem() != null) {
                        species = (String) (bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
                        q = QueryUtil.get(species, getMapComposer(), false);
                    }
                }
            } catch (Exception e) {
                System.out.println("Unable to retrieve selected species");
                e.printStackTrace(System.out);
            }
        }

        return q;
    }

    public String getSelectedSpeciesName() {
        String species = rgSpecies.getSelectedItem().getValue();
        try {
            if (species.equals("allspecies")) {
            } else if (species.equals("allmapped")) {
//                species = "";
//                List<MapLayer> layers = getMapComposer().getSpeciesLayers();
//
//                for (int i = 0; i < layers.size(); i++) {
//                    MapLayer lyr = layers.get(i);
//                    Radio rSp = new Radio(lyr.getDisplayName());
//                    species += lyr.getMapLayerMetadata().getSpeciesLsid() + ",";
//                }
//                species = species.substring(0, species.length() - 1);

                species = "All mapped species";
            } else if (species.equals("search")) {
                if (searchSpeciesAuto.getSelectedItem() != null) {
                    species = (String) (searchSpeciesAuto.getText());
                }
            } else {
                species = rgSpecies.getSelectedItem().getLabel();
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected species");
            e.printStackTrace(System.out);
        }

        return species;
    }

    public String getSelectedLayers() {
        String layers = "";

        try {
            if (lbListLayers.getSelectedLayers().length > 0) {
                String[] sellayers = lbListLayers.getSelectedLayers();
                for (String l : sellayers) {
                    layers += l + ":";
                }
                layers = layers.substring(0, layers.length() - 1);
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected layers");
            e.printStackTrace(System.out);
        }

        return layers;
    }

    void setLsid(String lsidName) {
        String[] s = lsidName.split("\t");
        String species = s[1];
        String lsid = s[0];

        /* set species from layer selector */
        if (species != null) {
            String tmpSpecies = species;
            searchSpeciesAuto.setValue(tmpSpecies);
            searchSpeciesAuto.refresh(tmpSpecies);

            if (searchSpeciesAuto.getSelectedItem() == null) {
                List list = searchSpeciesAuto.getItems();
                for (int i = 0; i < list.size(); i++) {
                    Comboitem ci = (Comboitem) list.get(i);
                    //compare name
                    if (ci.getLabel().equalsIgnoreCase(searchSpeciesAuto.getValue())) {
                        //compare lsid
                        if (ci.getAnnotatedProperties() != null
                                && ((String) ci.getAnnotatedProperties().get(0)).equals(lsid)) {
                            searchSpeciesAuto.setSelectedItem(ci);
                            break;
                        }
                    }
                }
            }
            btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() == null);

//            if (!btnOk.isDisabled()) {
                rgSpecies.setSelectedItem(rSpeciesSearch);
                Clients.evalJavaScript("jq('#" + rSpeciesSearch.getUuid() + "-real').attr('checked', true);");
                toggles();
                onClick$btnOk(null);
//            }
        }
    }

    void setLsidBk(String lsidName) {
        if (lsidName == null) {
            return;
        }
        String[] s = lsidName.split("\t");
        String species = s[1];
        String lsid = s[0];

        /* set species from layer selector */
        if (species != null) {
            if (bgSearchSpeciesAuto == null) {
                bgSearchSpeciesAuto = (SpeciesAutoComplete) getFellowIfAny("bgSearchSpeciesAuto");
            }

            String tmpSpecies = species;
            bgSearchSpeciesAuto.setValue(tmpSpecies);
            bgSearchSpeciesAuto.refresh(tmpSpecies);

            if (bgSearchSpeciesAuto.getSelectedItem() == null) {
                List list = bgSearchSpeciesAuto.getItems();
                for (int i = 0; i < list.size(); i++) {
                    Comboitem ci = (Comboitem) list.get(i);
                    //compare name
                    if (ci.getLabel().equalsIgnoreCase(bgSearchSpeciesAuto.getValue())) {
                        //compare lsid
                        if (ci.getAnnotatedProperties() != null
                                && ((String) ci.getAnnotatedProperties().get(0)).equals(lsid)) {
                            bgSearchSpeciesAuto.setSelectedItem(ci);
                            break;
                        }
                    }
                }
            }
            btnOk.setDisabled(bgSearchSpeciesAuto.getSelectedItem() == null);
            rgSpeciesBk.setSelectedItem(rSpeciesSearchBk);
            Clients.evalJavaScript("jq('#" + rSpeciesSearchBk.getUuid() + "-real').attr('checked', true);");

//            if (!btnOk.isDisabled()) {
                onClick$btnOk(null);
//            }
        }
    }

    public void onSelect$lbListLayers(Event event) { 
        toggles();
    }

    void toggles() {
        btnOk.setDisabled(true);
        btnOk.setVisible(true);

        if (fileUpload != null) {
            fileUpload.setVisible(false);
        }

        if(lbListLayers != null) {
            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            if (currentDiv.getZclass().contains("minlayers1")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 1);
            } else if (currentDiv.getZclass().contains("minlayers2")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 2);
            } else if (currentDiv.getZclass().contains("optional")) {
                btnOk.setDisabled(false);
            }
            updateLayerSelectionCount();
        }

        if (rgSpecies != null) {
            onCheck$rgSpecies(null);
        }
        if (rgSpeciesBk != null) {
            onCheck$rgSpeciesBk(null);
        }

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        if (currentDiv.getZclass().contains("layers2auto")) {
            cbLayer2 = (EnvLayersCombobox) getFellowIfAny("cbLayer2");
            cbLayer1 = (EnvLayersCombobox) getFellowIfAny("cbLayer1");
            btnOk.setDisabled(cbLayer2.getSelectedItem() == null
                    || cbLayer1.getSelectedItem() == null);
        }

        if (currentDiv.getZclass().contains("optional")) {
            btnOk.setDisabled(false);
        }

        if (currentDiv.getZclass().contains("species")) {
            //if (divSpeciesSearch != null && divSpeciesSearch.isVisible()){
            if(divSpeciesSearch.isVisible()) {
                btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() != null
                    && (searchSpeciesAuto.getSelectedItem().getValue() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0));
            } else if(vboxMultiple.isVisible() ) {
                btnOk.setDisabled(getMultipleLsids().length() == 0);
            }
        }
        
        if(lbListLayers != null) {
            if(bLayerListDownload1 != null && bLayerListDownload2 != null) {
                bLayerListDownload1.setDisabled(lbListLayers.getSelectedCount() == 0);
                bLayerListDownload2.setDisabled(lbListLayers.getSelectedCount() == 0);
            }
        }
    }

    public void onChange$cbLayer2(Event event) {
        toggles();
    }

    public void onChange$cbLayer1(Event event) {
        toggles();
    }

    public String getSelectedAreaName() {
        String area = rAreaSelected.getLabel();
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (MapLayer ml : layers) {
            if (area.equals(ml.getDisplayName())) {
                area = ml.getName();
                break;
            }
        }

        return area;
    }

    public String getSelectedAreaDisplayName() {
        String areaName = rAreaSelected.getLabel();

        return areaName;
    }

    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
        lbListLayers.updateDistances();
        toggles();
        btnOk.setDisabled(true);
    }

    private boolean isAreaHighlightTab() {
        return rgAreaHighlight != null && rgAreaHighlight.getParent().isVisible();
    }

    boolean isAreaTab() {
        return rgArea != null && rgArea.getParent().isVisible();
    }

    boolean isAreaCustom() {
        return isAreaTab() && rAreaCustom != null && rAreaCustom.isSelected();
    }

    boolean isAreaHighlightCustom() {
        return isAreaHighlightTab() && rgAreaHighlight != null
                && rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight");
    }

    public void onSelect$selectedLayersCombobox(Event event) {
        Comboitem ci = selectedLayersCombobox.getSelectedItem();
        if (ci != null && lbListLayers != null) {
            String layersList = null;
            if (ci.getValue() != null && ci.getValue() instanceof LayerSelection) {
                layersList = ((LayerSelection) ci.getValue()).getLayers();
            } else {
                if (ci.getValue() == null) {
                    if(ci.getLabel().toLowerCase().contains("paste")) {
                        Window window = (Window) Executions.createComponents("WEB-INF/zul/PasteLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else if(ci.getLabel().toLowerCase().contains("upload")) {
                        Window window = (Window) Executions.createComponents("WEB-INF/zul/UploadLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    selectedLayersCombobox.setSelectedIndex(-1);
                }
            }
            selectLayerFromList(layersList);
        }
    }

    public void selectLayerFromList(String layersList) {
        if (layersList == null) {
            return;
        }

        //check the whole layer string as well as the one at the end
        String[] layers = layersList.split(",");
        String [] list = new String[layers.length * 2];
        for (int i = 0; i < layers.length; i++) {
            int p1 = layers[i].lastIndexOf('(');
            int p2 = layers[i].lastIndexOf(')');
            if (p1 >= 0 && p2 >= 0 && p1 < p2) {
                list[i*2] = layers[i].substring(p1 + 1, p2).trim();
            }
            list[i*2+1] = layers[i];
        }
        lbListLayers.selectLayers(list);        
        toggles();
    }

    public void saveLayerSelection() {
        //save layer selection
        if (lbListLayers != null && lbListLayers.getSelectedCount() > 0) {
            String list = getLayerListShortText();
            LayerSelection ls = new LayerSelection(selectedMethod, tToolName.getText(), System.currentTimeMillis(), list);
            getMapComposer().addLayerSelection(ls);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < getMapComposer().getLayerSelections().size(); i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(getMapComposer().getLayerSelections().get(i).toString());
                sb.append(" // ");
                sb.append(getMapComposer().getLayerSelections().get(i).getLayers());
            }

            try {
                Cookie c = new Cookie("analysis_layer_selections", URLEncoder.encode(sb.toString(),"UTF-8"));
                c.setMaxAge(Integer.MAX_VALUE);
                ((HttpServletResponse) Executions.getCurrent().getNativeResponse()).addCookie(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void updateLayerListText() {
        try {
            if (lbListLayers != null && lbListLayers.getSelectedCount() > 0
                    && tLayerList != null) {
                tLayerList.setText(getLayerListText());
                if(dLayerSummary != null) {
                    dLayerSummary.setVisible(tLayerList.getText().length() > 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getLayerListText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(displayname).append(" (").append(s).append(")");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    String getLayerListShortText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void onChange$cbLayer(Event event) {
        //seek to and select the same layer in the list
        if(lbListLayers != null && cbLayer.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) cbLayer.getSelectedItem().getValue();
            String [] layer = jo.getString("name").split("/");
            lbListLayers.selectLayers(layer);            
        }
        toggles();
    }

    public void onClick$bLayerListDownload1(Event event) {
        downloadLayerList();
    }
    
    public void onClick$bLayerListDownload2(Event event) {
        downloadLayerList();
    }

    void downloadLayerList() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddmmyyyy_hhmm");
        Filedownload.save(getLayerListShortText(), "text/plain", "layer_selection_" + sdf.format(new Date()) + ".txt");
    }

    void updateLayerSelectionCount() {
        if(lLayersSelected != null && lbListLayers != null) {
            if(lbListLayers.getSelectedCount() == 1) {
                lLayersSelected.setValue("1 layer selected");
            } else {
                lLayersSelected.setValue(lbListLayers.getSelectedCount() + " layers selected");
            }
        }
    }

    public void onUpload$uploadLayerList(Event event) {
        doFileUpload(event);
    }

    public void onUpload$fileUpload(Event event) {
        UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", this, null);
        usc.addToMap = false;
        
        //is it bk or normal lsid
        if(rgSpeciesBk != null &&
                (rgSpeciesBk.getSelectedItem() == rSpeciesUploadLSIDBk
                || rgSpeciesBk.getSelectedItem() == rSpeciesUploadSpeciesBk)) {
            usc.setUploadType("bk");
        }

        usc.setVisible(false);
        usc.doOverlapped();
        usc.doFileUpload("",event);
        usc.detach();
    }

    public void doFileUpload(Event event) {
        UploadEvent ue = null;
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                System.out.println("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));
                    loaded = true;
                    System.out.println("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    System.out.println("unable to load user layer list: ");
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadLayerList(Reader r) throws Exception {
        CSVReader reader = new CSVReader(r);
        //one line, read it
        StringBuilder sb = new StringBuilder();
        for(String s : reader.readNext()) {
            if(sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }        
        reader.close();
        selectLayerFromList(sb.toString());
        updateLayerSelectionCount();
    }

    public void onChange$mSearchSpeciesAuto(Event event) {
        //add to lMultiple
        Comboitem ci = mSearchSpeciesAuto.getSelectedItem();
        if(ci != null && ci.getAnnotatedProperties() != null
                && ((String) ci.getAnnotatedProperties().get(0)) != null) {
            String lsid = ((String) ci.getAnnotatedProperties().get(0));

            try {
                Map<String, String> searchResult = BiocacheQuery.getClassification(lsid);

                String sciname= searchResult.get("scientificName");
                String family = searchResult.get("family");
                String kingdom = searchResult.get("kingdom");
                if(sciname == null) sciname = "";
                if(family == null) family = "";
                if(kingdom == null) kingdom = "";

                if(sciname != null && sciname.length() > 0) {
                    addTolMultiple(lsid, sciname, family, kingdom, true);

                    mSearchSpeciesAuto.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        toggles();
    }
    
    public void onChange$mSearchSpeciesAutoBk(Event event) {
        //add to lMultiple
        Comboitem ci = mSearchSpeciesAutoBk.getSelectedItem();
        if(ci != null && ci.getAnnotatedProperties() != null
                && ((String) ci.getAnnotatedProperties().get(0)) != null) {
            String lsid = ((String) ci.getAnnotatedProperties().get(0));

            try {
                Map<String, String> searchResult = BiocacheQuery.getClassification(lsid);

                String sciname= searchResult.get("scientificName");
                String family = searchResult.get("family");
                String kingdom = searchResult.get("kingdom");
                if(sciname == null) sciname = "";
                if(family == null) family = "";
                if(kingdom == null) kingdom = "";

                if(sciname != null && sciname.length() > 0) {
                    addTolMultipleBk(lsid, sciname, family, kingdom, true);

                    mSearchSpeciesAutoBk.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        toggles();
    }

    public void onClick$bMultiple(Event event) {
        String [] speciesNames = tMultiple.getText().replace("\n", ",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for(int i=0;i<speciesNames.length;i++) {
            String s = speciesNames[i].trim();
            if(s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = searchResult.getJSONArray("values");

                    String sciname= "", family="", kingdom="", lsid = null;
                    for(int j=0;j<ja.size();j++) {
                        if(ja.getJSONObject(j).getString("name").equals("scientificName")) {
                            sciname = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("family")) {
                            family = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("kingdom")) {
                            kingdom = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("taxonConceptID")) {
                            lsid = ja.getJSONObject(j).getString("processed");
                        }
                    }

                    //is 's' an LSID?
                    if(lsid == null || lsid.length() == 0) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if(sr.size() > 0
                                && sr.get("scientificName") != null
                                && sr.get("scientificName").length() > 0) {
                            lsid = s;
                            sciname= sr.get("scientificName");
                            family = sr.get("family");
                            kingdom = sr.get("kingdom");
                            if(sciname == null) sciname = "";
                            if(family == null) family = "";
                            if(kingdom == null) kingdom = "";
                        }
                    }

                    if(lsid != null && lsid.length() > 0) {
                        addTolMultiple(lsid,sciname,family,kingdom, false);
                    } else {
                        addTolMultiple(null,s,"","", false);
                    }

                    if(lsid == null || lsid.length() == 0) {
                        notFound.add(s);
                        notFoundSb.append(s + "\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s + "\n");
                }
            }
        }

        if(notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    public void onClick$bMultipleBk(Event event) {
        String [] speciesNames = tMultipleBk.getText().replace("\n", ",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for(int i=0;i<speciesNames.length;i++) {
            String s = speciesNames[i].trim();
            if(s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = searchResult.getJSONArray("values");

                    String sciname= "", family="", kingdom="", lsid = null;
                    for(int j=0;j<ja.size();j++) {
                        if(ja.getJSONObject(j).getString("name").equals("scientificName")) {
                            sciname = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("family")) {
                            family = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("kingdom")) {
                            kingdom = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("taxonConceptID")) {
                            lsid = ja.getJSONObject(j).getString("processed");
                        }
                    }

                    //is 's' an LSID?
                    if(lsid == null || lsid.length() == 0) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if(sr.size() > 0
                                && sr.get("scientificName") != null
                                && sr.get("scientificName").length() > 0) {
                            lsid = s;
                            sciname= sr.get("scientificName");
                            family = sr.get("family");
                            kingdom = sr.get("kingdom");
                            if(sciname == null) sciname = "";
                            if(family == null) family = "";
                            if(kingdom == null) kingdom = "";
                        }
                    }

                    if(lsid != null && lsid.length() > 0) {
                        addTolMultipleBk(lsid,sciname,family,kingdom, false);
                    } else {
                        addTolMultipleBk(null,s,"","", false);
                    }

                    if(lsid == null || lsid.length() == 0) {
                        notFound.add(s);
                        notFoundSb.append(s + "\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s + "\n");
                }
            }
        }

        if(notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    JSONObject processAdhoc(String scientificName) {
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.biocacheServer + "/process/adhoc");
            StringRequestEntity sre = new StringRequestEntity("{ \"scientificName\": \"" + scientificName.replace("\"","'") + "\" } ", "application/json", "UTF-8");
            post.setRequestEntity(sre);
            int result = client.executeMethod(post);
            if(result == 200) {
                return JSONObject.fromObject(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addTolMultiple(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for(Listitem li : (List<Listitem>)lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if((lsid != null && lsidCell.getLabel().equals(lsid))
                    || (sciname != null && scinameCell.getLabel().replace("(not found)","").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        //remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                toggles();
            }
        });
        lc.setParent(li);

        //sci name
        if(lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        //family
        if(lsid == null) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)","").trim();                    
                    mSearchSpeciesAuto.refresh(sciname);                    
                    mSearchSpeciesAuto.open();
                    mSearchSpeciesAuto.setText(sciname + " ");
                    li.detach();
                }
            });
        } else {
            lc = new Listcell(family);
        }
        lc.setParent(li);

        //kingdom
        lc = new Listcell(kingdom);
        lc.setParent(li);

        //count
        if(lsid != null) {
            int count = new BiocacheQuery(lsid,null,null,null,false).getOccurrenceCount();
            if(count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        //lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if(insertAtBeginning && lMultiple.getChildren().size() > 0) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }

    private void addTolMultipleBk(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for(Listitem li : (List<Listitem>)lMultipleBk.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if((lsid != null && lsidCell.getLabel().equals(lsid))
                    || (sciname != null && scinameCell.getLabel().replace("(not found)","").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        //remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                toggles();
            }
        });
        lc.setParent(li);

        //sci name
        if(lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        //family
        if(lsid == null) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)","").trim();                    
                    mSearchSpeciesAutoBk.refresh(sciname);                    
                    mSearchSpeciesAutoBk.open();
                    mSearchSpeciesAutoBk.setText(sciname + " ");
                    li.detach();
                }
            });
        } else {
            lc = new Listcell(family);
        }
        lc.setParent(li);

        //kingdom
        lc = new Listcell(kingdom);
        lc.setParent(li);

        //count
        if(lsid != null) {
            int count = new BiocacheQuery(lsid,null,null,null,false).getOccurrenceCount();
            if(count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        //lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if(insertAtBeginning && lMultipleBk.getChildren().size() > 0) {
            lMultipleBk.insertBefore(li, lMultipleBk.getFirstChild());
        } else {
            li.setParent(lMultipleBk);
        }
    }

    private String getMultipleLsids() {
        StringBuilder sb = new StringBuilder();
        for(Listitem li : (List<Listitem>)lMultiple.getItems()) {
            Listcell lc = (Listcell) li.getLastChild();
            if(lc.getLabel() != null && lc.getLabel().length() > 0) {
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lc.getLabel());
            }
        }
        return sb.toString();
    }

    private String getMultipleLsidsBk() {
        StringBuilder sb = new StringBuilder();
        for(Listitem li : (List<Listitem>)lMultipleBk.getItems()) {
            Listcell lc = (Listcell) li.getLastChild();
            if(lc.getLabel() != null && lc.getLabel().length() > 0) {
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lc.getLabel());
            }
        }
        return sb.toString();
    }
}
