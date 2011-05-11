package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
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
    boolean setCustomArea = false;
    boolean hasCustomArea = false;
    boolean setUploadSpecies = false;
    boolean hasUploadSpecies = false;
    boolean setUploadSpeciesBk = false;
    boolean hasUploadSpeciesBk = false;
    MapLayer prevTopArea = null;

    @Override
    public void afterCompose() {
        super.afterCompose();

        winTop = this.getTop();
        winLeft = this.getLeft();

        setupDefaultParams();
        setParams(Executions.getCurrent().getArg());

        //loadStepLabels();
        updateWindowTitle();
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
                rgSpecies.getItemAtIndex(1).setSelected(true);
            } else {
                for (int i = 0; i < rgSpecies.getItemCount(); i++) {
                    if (rgSpecies.getItemAtIndex(i).isVisible()) {
                        rgSpecies.getItemAtIndex(i).setSelected(true);
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
            Radio rSpeciesMapped = (Radio) getFellowIfAny("rSpeciesMappedBk");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

//            Radio selectedSpecies = null;
//            String selectedLsid = (String) params.get("lsidBackground");
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

//                if(selectedLsid != null && rSp.getValue().equals(selectedLsid)) {
//                    selectedSpecies = rSp;
//                }
            }

            if (speciesLayersCount > 1) {
                rSpeciesMapped.setLabel("All " + speciesLayersCount + " species currently mapped (excludes coordinate uploads)");
            } else {
                rSpeciesMapped.setVisible(false);
            }

            rSpeciesNoneBk.setSelected(true);

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
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            int indexToSelect = 0;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                    indexToSelect = i;
                }
            }

            if (rSelectedLayer != null) {
                rSelectedLayer.setSelected(true);
                rAreaSelected = rSelectedLayer;
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArea.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
            } else {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rgArea.getItemAtIndex(i).setSelected(true);
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        break;
                    }
                }
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rgArea.getItemAtIndex(i).setSelected(true);
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        System.out.println("2.resetting indexToSelect = " + i);
                        indexToSelect = i;
                        break;
                    }
                }
            }
            
            rgArea.setSelectedIndex(indexToSelect);
            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

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

    public void loadGridLayers(boolean fullList) {
        try {

            if (fullList) {
                lbListLayers.init(getMapComposer(), CommonData.satServer, true);
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
        setCustomArea = false;
        hasCustomArea = false;
        if (rgArea.getSelectedItem() == rAreaCustom) {
            setCustomArea = true;
            hasCustomArea = false;
            //this.setHeight("700px");
        }
        rAreaSelected = rgArea.getSelectedItem();
    }

    public void onCheck$rgSpecies(Event event) {
        try {
            if (rgSpecies != null && rgSpecies.getSelectedItem() == rSpeciesSearch) {
                if (divSpeciesSearch != null) {
                    divSpeciesSearch.setVisible(true);
                    return;
                }
            }
            if (divSpeciesSearch != null) {
                divSpeciesSearch.setVisible(false);
            }

            //Check to see if we are perform a normal or background upload
            setUploadSpecies = false;
            hasUploadSpecies = false;
            if (rgSpecies.getSelectedItem() == rSpeciesUploadSpecies
                    || rgSpecies.getSelectedItem() == rSpeciesUploadLSID) {
                setUploadSpecies = true;
                hasUploadSpecies = false;
            }

            if (event != null) {
                toggles();
            }
        } catch (Exception e) {
        }
    }

    public void onCheck$rgSpeciesBk(Event event) {
        try {
            if (rgSpeciesBk != null && rgSpeciesBk.getSelectedItem() == rSpeciesSearchBk) {
                if (divSpeciesSearchBk != null) {
                    divSpeciesSearchBk.setVisible(true);
                    return;
                }
            }

            if (divSpeciesSearchBk != null) {
                divSpeciesSearchBk.setVisible(false);
            }

            setUploadSpeciesBk = false;
            hasUploadSpeciesBk = false;
            if (rgSpeciesBk.getSelectedItem() == rSpeciesUploadSpeciesBk || rgSpeciesBk.getSelectedItem() == rSpeciesUploadLSIDBk) {
                setUploadSpeciesBk = true;
                hasUploadSpeciesBk = false;
            }

            if (event != null) {
                toggles();
            }
        } catch (Exception e) {
        }
    }

    public void onChange$searchSpeciesAuto(Event event) {
        /*
        if (searchSpeciesAuto.getSelectedItem() != null) {

        String taxon = searchSpeciesAuto.getValue();
        String rank = "";

        String spVal = searchSpeciesAuto.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
        taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
        rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

        } else {
        rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
        }
        if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
        rank = "taxon";
        }

        String lsid = (String) (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));

        Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpecies");
        Radio rSpeciesAll = (Radio) getFellowIfAny("rSpeciesAll");
        Radio rSp = new Radio(taxon);
        rSp.setId(taxon.replaceAll(" ", ""));
        rSp.setValue(lsid);
        rgSpecies.insertBefore(rSp, rgSpecies.getItemAtIndex(1));
        rSp.setSelected(true);
        }
         * 
         */
        //getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto);

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
            if (type.compareTo("normal") == 0) {
                setLsid(lsid);
                hasUploadSpecies = true;
            }
            if (type.compareTo("bk") == 0) {
                hasUploadSpeciesBk = true;
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
                    loadAreaLayers(curTopArea.getDisplayName());
                    ok = true;
                }
            }
            this.setTop(winTop);
            this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                onClick$btnOk(null);                
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
            if (setUploadSpecies && !hasUploadSpecies) {
                this.doOverlapped();
                this.setTop("-9999px");
                this.setLeft("-9999px");

                HashMap<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddTool");
                winProps.put("addToMap", "false");
                usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", this, winProps);
                if (rSpeciesUploadLSID != null && rSpeciesUploadLSID.isChecked()) {
                    usc.setTbInstructions("3. Select file (text file, one LSID per line)");
                }
                if (rSpeciesUploadLSID != null && rSpeciesUploadSpecies.isChecked()) {
                    usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
                }

                usc.doModal();
                return;
            }

            if (setUploadSpeciesBk && !hasUploadSpeciesBk) {
                this.doOverlapped();
                this.setTop("-9999px");
                this.setLeft("-9999px");

                HashMap<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddTool");
                winProps.put("addToMap", "false");
                usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", this, winProps);
                if (rSpeciesUploadLSIDBk != null && rSpeciesUploadLSIDBk.isChecked()) {
                    usc.setTbInstructions("3. Select file (text file, one LSID per line)");
                    usc.setUploadType("bk");
                }
                if (rSpeciesUploadSpeciesBk != null && rSpeciesUploadSpeciesBk.isChecked()) {
                    usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
                    usc.setUploadType("bk");
                }
                usc.doModal();
                return;
            }


            if (setCustomArea && !hasCustomArea) {
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
            } else if (species.equals("other")) {
                if (searchSpeciesAuto.getSelectedItem() != null) {
                    species = (String) (searchSpeciesAuto.getText());
                }
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

    public void onClick$btnUpload(Event event) {
        try {
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("addToMap", "false");
            usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", getMapComposer(), hm);
            usc.setEventListener(new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    setLsid((String) event.getData());
                }
            });
            usc.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$btnUploadBk(Event event) {
        try {
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("addToMap", "false");
            usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", getMapComposer(), hm);
            usc.setEventListener(new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    setLsidBk((String) event.getData());
                }
            });
            usc.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        }
        //usc.detach();
    }

    void setLsidBk(String lsidName) {
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
        }
        //usc.detach();
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

        onSelect$lbListLayers(null);
        if ((!hasUploadSpecies && !setUploadSpecies)) {
            onCheck$rgSpecies(null);
        }
        if ((!hasUploadSpeciesBk && !setUploadSpeciesBk)) {
            onCheck$rgSpeciesBk(null);
        }

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        if (currentDiv.getZclass().contains("layers2auto")) {
            cbLayer2 = (EnvLayersCombobox) getFellowIfAny("cbLayer2");
            cbLayer1 = (EnvLayersCombobox) getFellowIfAny("cbLayer1");
            btnOk.setDisabled(cbLayer2.getSelectedItem() == null
                    || cbLayer1.getSelectedItem() == null);
        }

        if (currentDiv.getZclass().contains("species")) {
            btnOk.setDisabled(
                    divSpeciesSearch.isVisible()
                    && (searchSpeciesAuto.getSelectedItem() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                    || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0));
        }

        if (currentDiv.getZclass().contains("optional")) {
            btnOk.setDisabled(false);
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
}
