package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.util.CommonData;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class AddToolComposer extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    int currentStep = 1, totalSteps = 5;
    Hashtable<String, Object> params;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea, rgSpecies;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    Radio rSpeciesAll, rSpeciesMapped, rSpeciesOther;
    Button btnCancel, btnOk, btnBack, btnHelp;
    Textbox tToolName;
    SpeciesAutoComplete searchSpeciesAuto;
    EnvironmentalList lbListLayers;

    @Override
    public void afterCompose() {
        super.afterCompose();

        setupDefaultParams();

        //loadStepLabels();
        updateWindowTitle();
    }

    private void setupDefaultParams() {
        params = new Hashtable<String, Object>();
        params.put("step1", "Select area(s)");
        params.put("step2", "Select species(s)");
        params.put("step3", "Select grid(s)");
        params.put("step4", "Select your analytical options");
        params.put("step5", "Name your output for");
    }

    public void updateWindowTitle() {
        this.setTitle("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
    }

    public void updateName(String name) {
        tToolName.setValue(name);
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
            }
        } catch (Exception e) {
        }
    }

    public void setParams(Hashtable<String, Object> params) {
        //this.params = params;

        // iterate thru' the passed params and load them into the
        // existing default params
        if (params != null) {
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

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getMapLayerMetadata().getSpeciesLsid());
                rSp.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rgSpecies.insertBefore(rSp, rSpeciesMapped);
            }

            if (layers.size() > 1) {
                rSpeciesMapped.setLabel("All " + layers.size() + " species currently mapped");
            } else {
                rSpeciesMapped.setVisible(false);
            }

            if (layers.size() > 0) {
                rgSpecies.getItemAtIndex(1).setSelected(true);
            } else {
                rgSpecies.getItemAtIndex(0).setSelected(true);
            }

        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }

    public void loadAreaLayers() {
        try {

            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgArea");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            rAreaCurrent.setSelected(true);
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
                    System.out.println(lyr.getDisplayName());
                }
            }


        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
            e.printStackTrace(System.out);
        }
    }

    public void onCheck$rgSpecies(Event event) {
        try {
            System.out.println("onCheck$rgSpeces activated");
            if (searchSpeciesAuto != null) {

                if (rgSpecies.getSelectedItem() == rSpeciesOther) {
                    searchSpeciesAuto.setDisabled(false);
                } else {
                    searchSpeciesAuto.setDisabled(true);
                }
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
        getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto);
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
        this.detach();
    }

    public void onClick$btnBack(Event event) {

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
        Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep - 1))) : null;

        if (currentDiv.getZclass().equalsIgnoreCase("first")) {
            //currentStep = 1;
            //this.detach();
            btnBack.setDisabled(true);
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);
            currentStep--;

            if (previousDiv != null) {
                //btnCancel.setLabel(((!previousDiv.getZclass().equalsIgnoreCase("first")) ? "< Back" : "Cancel"));
                btnBack.setDisabled(((!previousDiv.getZclass().equalsIgnoreCase("first")) ? false : true));
            }
        }

        btnOk.setLabel("Next >");
        updateWindowTitle();

    }

    public void onClick$btnOk(Event event) {

        try {

            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
            Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep + 1))) : null;

            System.out.println("Current step: " + currentStep);
            System.out.println("Current zclass: " + currentDiv.getZclass());
            System.out.println("Next step zclass: " + ((nextDiv != null) ? nextDiv.getZclass() : "-na-"));
            System.out.println("Previous step zclass: " + ((previousDiv != null) ? previousDiv.getZclass() : "-na-"));


            if (!currentDiv.getZclass().equalsIgnoreCase("last")) {
                currentDiv.setVisible(false);
                nextDiv.setVisible(true);


                // now include the extra options for step 4 
                if (nextDiv != null) {

                    if (nextDiv.getZclass().equalsIgnoreCase("last")) {
                        loadSummaryDetails();
                        onLastPanel();
                    }

                    btnOk.setLabel(((!nextDiv.getZclass().equalsIgnoreCase("last")) ? "Next >" : "Finish"));
                }

                currentStep++;
            } else {
                System.out.println("In the last step. Let's run the analytical tool!!!");
                currentStep = 1;
                onFinish();
            }

            //btnCancel.setLabel("< Back");
            btnBack.setDisabled(false);
            updateWindowTitle();

        } catch (Exception ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        String area = rgArea.getSelectedItem().getValue();

        try {
            if (area.equals("current")) {
                area = getMapComposer().getViewArea();
            } else if (area.equals("australia")) {
                area = "POLYGON((112.0 -44.0,112.0 -9.0,154.0 -9.0,154.0 -44.0,112.0 -44.0))";
            } else if (area.equals("world")) {
                area = "POLYGON((-180 -90,-180 90.0,180.0 90.0,180.0 -90.0,-180.0 -90.0))";
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
                    species = (String) (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
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
            String[] sellayers = lbListLayers.getSelectedLayers();
            for (String l : sellayers) {
                layers += l + ":";
            }
            layers = layers.substring(0, layers.length() - 1);
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected layers");
            e.printStackTrace(System.out);
        }

        return layers;
    }
}
