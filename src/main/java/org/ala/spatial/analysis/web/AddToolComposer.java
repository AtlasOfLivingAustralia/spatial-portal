package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.UserData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
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
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddToolComposer extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    int currentStep = 1, totalSteps = 5;
    Map<String, Object> params;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea, rgAreaHighlight, rgSpecies, rgSpeciesBk;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    Radio rSpeciesAll, rSpeciesMapped, rSpeciesSearch, rSpeciesUploadLSID, rSpeciesUploadSpecies;
    Radio rSpeciesNoneBk, rSpeciesAllBk, rSpeciesMappedBk, rSpeciesSearchBk, rSpeciesUploadLSIDBk, rSpeciesUploadSpeciesBk;
    Radio rAreaWorld, rAreaCustom, rAreaWorldHighlight, rAreaSelected;
    Button btnCancel, btnOk, btnBack, btnHelp;
    Textbox tToolName;
    SpeciesAutoComplete searchSpeciesAuto, bgSearchSpeciesAuto;
    EnvironmentalList lbListLayers;
    Div divSpeciesSearch, divSpeciesSearchBk;
    UploadSpeciesController usc;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
    String winTop = "300px";
    String winLeft = "500px";
    //boolean setCustomArea = false;
    boolean hasCustomArea = false;
    MapLayer prevTopArea = null;
    Fileupload fileUpload;

    @Override
    public void afterCompose() {
        super.afterCompose();

        winTop = this.getTop();
        winLeft = this.getLeft();

        setupDefaultParams();
        setParams(Executions.getCurrent().getArg());

        //loadStepLabels();
        updateWindowTitle();

        if (fileUpload != null) {
            fileUpload.addEventListener("onUpload", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    doFileUpload(null, event);
                }
            });
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
        try {

            Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpecies");
            Radio rSpeciesMapped = (Radio) getFellowIfAny("rSpeciesMapped");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

            Radio selectedSpecies = null;
            String selectedLsid = (String) params.get("lsid");
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getMapLayerMetadata().getSpeciesLsid());
                rSp.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rgSpecies.insertBefore(rSp, rSpeciesMapped);

                if (selectedLsid != null && rSp.getValue().equals(selectedLsid)) {
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
            } else if (selectedLsid != null && selectedLsid.equals("none")) {
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
                rSp.setValue(lyr.getMapLayerMetadata().getSpeciesLsid());
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
            for (int i=rgArea.getItemCount()-1;i>=0;i--) {
                String id = ((Radio)rgArea.getItems().get(i)).getId();
                if(id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            String allWKT = "";
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                //rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());

                if(!allWKT.contains("ENVELOPE")) {
                    if (!allWKT.isEmpty())
                        allWKT+=",";
                    count_not_envelopes ++;
                    allWKT += lyr.getWKT();
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
                         + ((count_not_envelopes < layers.size())?" (excluding Environmental Envelopes)": ""));
                //rAr.setId("AllActiveAreas");
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT + ")");
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
            }else if (rSelectedLayer != null) {
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
            for (int i=rgArea.getItemCount()-1;i>=0;i--) {
                String id = ((Radio)rgArea.getItems().get(i)).getId();
                if(id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrentHighlight = (Radio) getFellowIfAny("rAreaCurrentHighlight");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            String allWKT = "";
            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                //rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());

                if(!allWKT.contains("ENVELOPE")) {
                    if (!allWKT.isEmpty())
                        allWKT+=",";
                    count_not_envelopes ++;
                    allWKT += lyr.getWKT();
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
                         + ((count_not_envelopes < layers.size())?" (excluding Environmental Envelopes)": ""));
                //rAr.setId("AllActiveAreas");
                rAr.setValue("GEOMETRYCOLLECTION(" + allWKT + ")");
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
            }else if (rSelectedLayer != null) {
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
        if(rAreaSelected == rAreaCustom) {
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
        if(rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight")) {
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

    }

    public void resetWindowFromSpeciesUpload(String lsid, String type) {
        try {
            if (type.compareTo("cancel") == 0) {
                this.setTop(winTop);
                this.setLeft(winLeft);
                this.doModal();
                return;
            }
            if (type.compareTo("normal") == 0) {
                setLsid(lsid);
            }
            if (type.compareTo("bk") == 0) {
                setLsidBk(lsid);
            }
            this.setTop(winTop);
            this.setLeft(winLeft);
            this.doModal();
            onClick$btnOk(null);
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
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException when resetting analysis window");
            ex.printStackTrace(System.out);
        } catch (SuspendNotAllowedException ex) {
            System.out.println("Exception when resetting analysis window");
            ex.printStackTrace(System.out);
        }
    }

    public void onClick$btnOk(Event event) {

        try {
            if (!hasCustomArea &&(isAreaCustom() || isAreaHighlightCustom())){
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
            } else {
                currentStep = 1;
                onFinish();
            }

            //btnCancel.setLabel("< Back");
            btnBack.setDisabled(false);
            updateWindowTitle();

        } catch (Exception ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        }

        toggles();
    }

    public void onLastPanel() {
    }

    public void onFinish() {
        try {
            this.detach();
            Messagebox.show("Running your analysis tool: " + selectedMethod);

        } catch (InterruptedException ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
        }
    }

    public void loadMap(Event event) {
    }

    public String getSelectedArea() {
        //String area = rgArea.getSelectedItem().getValue();
        String area = rAreaSelected.getValue();

        try {
            if (area.equals("current")) {
                area = getMapComposer().getViewArea();
            } else if (area.equals("australia")) {
                area = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            } else if (area.equals("world")) {
                area = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getDisplayName())) {
                        area = ml.getWKT();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return area;
    }

    public String getSelectedAreaHighlight() {
        String area = rgAreaHighlight.getSelectedItem().getValue();

        try {
            if (area.equals("none")) {
                area = null;
            } else if (area.equals("current")) {
                area = getMapComposer().getViewArea();
            } else if (area.equals("australia")) {
                area = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            } else if (area.equals("world")) {
                area = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getDisplayName())) {
                        area = ml.getWKT();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return area;
    }

    public String getSelectedSpecies() {
        String species = rgSpecies.getSelectedItem().getValue();
        try {
            if (species.equals("allspecies")) {
                species = "none";
            } else if (species.equals("allmapped")) {
                species = "";
                List<MapLayer> layers = getMapComposer().getSpeciesLayers();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                        sb.append(lyr.getMapLayerMetadata().getSpeciesLsid() + ",");
                    }
                }
                String lsids = sb.toString().substring(0, sb.length() - 1);

                //get lsid to match
                StringBuilder sbProcessUrl = new StringBuilder();
                sbProcessUrl.append("/species/lsid/register");
                sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");
                int result = client.executeMethod(get);
                String pid = get.getResponseBodyAsString();

                if (result == 200 && pid != null && pid.length() > 0) {
                    species = pid;
                } else {
                    //TODO: error
                }

            } else if (species.equals("search") || species.equals("uploadSpecies") || species.equals("uploadLsid")) {
                if (searchSpeciesAuto.getSelectedItem() != null) {
                    species = (String) (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected species");
            e.printStackTrace(System.out);
        }

        return species;
    }

    public String getSelectedSpeciesBk() {
        String species = rgSpeciesBk.getSelectedItem().getValue();
        try {
            if (species.equals("none")) {
                species = null;
            } else if (species.equals("allspecies")) {
                species = "none";
            } else if (species.equals("allmapped")) {
                species = "";
                List<MapLayer> layers = getMapComposer().getSpeciesLayers();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                        sb.append(lyr.getMapLayerMetadata().getSpeciesLsid() + ",");
                    }
                }
                String lsids = sb.toString().substring(0, sb.length() - 1);

                //get lsid to match
                StringBuilder sbProcessUrl = new StringBuilder();
                sbProcessUrl.append("/species/lsid/register");
                sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");
                int result = client.executeMethod(get);
                String pid = get.getResponseBodyAsString();

                if (result == 200 && pid != null && pid.length() > 0) {
                    species = pid;
                } else {
                    //TODO: error
                }

            } else if (species.equals("search") || species.equals("uploadSpecies") || species.equals("uploadLsid")) {
                if (bgSearchSpeciesAuto == null) {
                    bgSearchSpeciesAuto = (SpeciesAutoComplete) getFellowIfAny("bgSearchSpeciesAuto");
                }
                if (bgSearchSpeciesAuto.getSelectedItem() != null) {
                    species = (String) (bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected species");
            e.printStackTrace(System.out);
        }

        return species;
    }

    public String getSelectedSpeciesName() {
        String species = rgSpecies.getSelectedItem().getValue();
        try {
            if (species.equals("allspecies")) {
            } else if (species.equals("allmapped")) {
                species = "";
                List<MapLayer> layers = getMapComposer().getSpeciesLayers();

                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    Radio rSp = new Radio(lyr.getDisplayName());
                    species += lyr.getMapLayerMetadata().getSpeciesLsid() + ",";
                }
                species = species.substring(0, species.length() - 1);
            } else if (species.equals("search")) {
                if (searchSpeciesAuto.getSelectedItem() != null) {
                    species = (String) (searchSpeciesAuto.getText());
                }
            } else {
                species = rgSpecies.getSelectedItem().getLabel();
//                    species = species.substring(0,species.lastIndexOf(" "));
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

            if(!btnOk.isDisabled()) {
                rgSpecies.setSelectedItem(rSpeciesSearch);
                Clients.evalJavaScript("jq('#" + rSpeciesSearch.getUuid() + "-real').attr('checked', true);");
                toggles();
                onClick$btnOk(null);
            }
        }
    }

    void setLsidBk(String lsidName) {
        if(lsidName == null) {
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
            rgSpecies.setSelectedItem(rSpeciesSearch);
            Clients.evalJavaScript("jq('#" + rSpeciesSearch.getUuid() + "-real').attr('checked', true);");

            if(!btnOk.isDisabled()) {
                onClick$btnOk(null);
            }
        }
    }

    public void onSelect$lbListLayers(Event event) {
        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        if (currentDiv.getZclass().contains("minlayers1")
                && lbListLayers.getSelectedCount() >= 1) {
            btnOk.setDisabled(false);
        } else if (currentDiv.getZclass().contains("minlayers2")
                && lbListLayers.getSelectedCount() >= 2) {
            btnOk.setDisabled(false);
        } else if (currentDiv.getZclass().contains("optional")) {
            btnOk.setDisabled(false);
        }
    }

    void toggles() {
        btnOk.setDisabled(true);
        btnOk.setVisible(true);

        if(fileUpload != null) {
            fileUpload.setVisible(false);
        }

        onSelect$lbListLayers(null);
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
            btnOk.setDisabled(
                    divSpeciesSearch.isVisible()
                    && (searchSpeciesAuto.getSelectedItem().getValue() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0));
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
    }

    public void doFileUpload(UserData ud, Event event) {
        UploadEvent ue = null;
        if (event.getName().equals("onUpload")) {
            ue = (UploadEvent) event;
        } else if (event.getName().equals("onForward")) {
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
            if (ud == null) {
                ud = new UserData(m.getName());
            }
            if (ud.getName().trim().equals("")) {
                ud.setName(m.getName());
            }
            ud.setFilename(m.getName());

            if (ud.getName() == null || ud.getName().length() == 0) {
                ud.setName(m.getName());
            }
//            if (ud.getDescription() == null || ud.getDescription().length() == 0) {
//                ud.setDescription(date/time + #lsids or #coordinates);
//            }

            String name = ud.getName();

            System.out.println("Got file '" + ud.getName() + "' with type '" + m.getContentType() + "'");

            // check the content-type
            // TODO: check why LB is sending 'application/spc' mime-type. remove from future use.
            if (m.getContentType().equalsIgnoreCase("text/plain") || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV) || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV_EXCEL)) {
                loadUserPoints(ud, m.getReaderData());
            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_EXCEL) || m.getContentType().equalsIgnoreCase("application/spc")) {
                byte[] csvdata = m.getByteData();
                loadUserPoints(ud, new StringReader(new String(csvdata)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }
    public void loadUserPoints(UserData ud, Reader data) {
        try {
            // Read a line in to check if it's a valid file
            // if it throw's an error, then it's not a valid csv file
            CSVReader reader = new CSVReader(data);

            List userPoints = reader.readAll();

            //if only one column treat it as a list of LSID's
            if (((String[]) userPoints.get(0)).length == 1) {
                continueLoadUserLSIDs(ud, data, reader, userPoints);
                return;
            }

            boolean hasHeader = false;

            // check if it has a header
            String[] upHeader = (String[]) userPoints.get(0);
            try {
                Double d1 = new Double(upHeader[1]);
                Double d2 = new Double(upHeader[2]);
            } catch (Exception e) {
                hasHeader = true;
            }

            System.out.println("hasHeader: " + hasHeader);

            // check if the count of points goes over the threshold.
            int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();
            System.out.println("Checking user points size: " + sizeToCheck + " -> " + settingsSupplementary.getValueAsInt("max_record_count_upload"));
            if (sizeToCheck > settingsSupplementary.getValueAsInt("max_record_count_upload")) {
                getMapComposer().showMessage(settingsSupplementary.getValue("max_record_count_upload_message"));
                return;
            }

            StringBuffer sbUIds = new StringBuffer();
            StringBuffer sbUPoints = new StringBuffer();
            int counter = 1;
            for (int i = 0; i < userPoints.size(); i++) {
                String[] up = (String[]) userPoints.get(i);
                if (up.length > 2) {
                    sbUIds.append(up[0] + "\n");
                    sbUPoints.append(up[1] + "," + up[2] + "\n");
                } else if (up.length > 1) {
                    sbUIds.append(counter + "\n");
                    sbUPoints.append(up[0] + "," + up[1] + "\n");
                    counter++;
                }
            }

            System.out.println("Loading points into alaspatial");
            System.out.println(sbUPoints.toString());

            // Post it to alaspatial app
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.satServer + "/alaspatial/ws/points/register"); // testurl
            post.addRequestHeader("Accept", "text/plain");
            post.addParameter("name", ud.getName());
            post.addParameter("points", sbUPoints.toString());
            post.addParameter("ids", sbUIds.toString());

            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            //uploadLSID = slist + "\t" + ud.getName();

            System.out.println("uploaded points name: " + ud.getName() + " lsid: " + slist);

            ud.setFeatureCount(userPoints.size());
            Long did = new Long(slist);
            System.out.println("lval: " + did.longValue());
            ud.setUploadedTimeInMs(did.longValue());

            ud.setDescription(ud.getFeatureCount() + " coordinates, " + ud.getDisplayTime());

            String metadata = "";
            metadata += "User uploaded points \n";
            metadata += "Name: " + ud.getName() + " \n";
            metadata += "Description: " + ud.getDescription() + " \n";
            metadata += "Date: " + ud.getDisplayTime() + " \n";
            metadata += "Number of Points: " + ud.getFeatureCount() + " \n";

            ud.setMetadata(metadata);
            ud.setSubType(LayerUtilities.SPECIES_UPLOAD);
            ud.setLsid(slist);

            // add it to the user session
            Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
            if (htUserSpecies == null) {
                htUserSpecies = new Hashtable<String, UserData>();
            }
            htUserSpecies.put(slist, ud);
            getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

            // close the reader and data streams
            reader.close();
            data.close();

            if(rSpeciesUploadSpecies.isSelected() || rSpeciesUploadLSID.isSelected()){
                setLsid(slist + "\t" + ud.getName());
            } else {
                setLsidBk(slist + "\t" + ud.getName());
            }
        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");
            e.printStackTrace(System.out);
        }
    }

    public void continueLoadUserLSIDs(UserData ud, Reader data, CSVReader reader, List userPoints) {
        try {
            //don't care if it has a header

            // check if the count of LSIDs goes over the threshold (+1).
            int sizeToCheck = userPoints.size();
            System.out.println("Checking user LSIDs size: " + sizeToCheck + " -> " + 50);
            if (sizeToCheck > 50) {
                getMapComposer().showMessage("Cannot upload more than 50 LSIDs");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < userPoints.size(); i++) {
                String[] up = (String[]) userPoints.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(up[0].replace(",", "").trim().toLowerCase());
            }

            String lsids = sb.toString();

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/species/lsid/register");
            sbProcessUrl.append("?lsids=" + URLEncoder.encode(lsids.replace(".", "__"), "UTF-8"));

            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(CommonData.satServer + "/alaspatial/" + sbProcessUrl.toString()); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            String pid = get.getResponseBodyAsString();

            System.out.println("uploaded points name: " + ud.getName() + " lsid: " + pid);

            ud.setFeatureCount(userPoints.size());
            Long did = new Long(pid);
            System.out.println("lval: " + did.longValue());
            ud.setUploadedTimeInMs(did.longValue());

            ud.setDescription(ud.getFeatureCount() + " LSIDS, " + ud.getDisplayTime());

            String metadata = "";
            metadata += "User uploaded points \n";
            metadata += "Name: " + ud.getName() + " \n";
            metadata += "Description: " + ud.getDescription() + " \n";
            metadata += "Date: " + ud.getDisplayTime() + " \n";
            metadata += "Number of Points: " + ud.getFeatureCount() + " \n";

            ud.setMetadata(metadata);
            ud.setSubType(LayerUtilities.SPECIES);
            ud.setLsid(pid);

            // add it to the user session
            Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
            if (htUserSpecies == null) {
                htUserSpecies = new Hashtable<String, UserData>();
            }
            htUserSpecies.put(pid, ud);
            getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

            // close the reader and data streams
            reader.close();
            data.close();

            if(rSpeciesUploadSpecies.isSelected() || rSpeciesUploadLSID.isSelected()){
                setLsid(pid + "\t" + ud.getName());
            } else {
                setLsidBk(pid + "\t" + ud.getName());
            }

        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");
            e.printStackTrace(System.out);
        }
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
}
