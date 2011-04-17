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
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;

/**
 *
 * @author ajay
 */
public class AddToolComposer extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    Radiogroup rgModel, rgSpecies;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    Radio rSpeciesAll, rSpeciesMapped, rSpeciesOther;
    Button btnCancel, btnOk, btnBack;
    int currentStep = 1, totalSteps = 5;
    Hashtable<String, Object> params;
    String selectedMethod = "";
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

            Radiogroup rgSpeces = (Radiogroup) getFellowIfAny("rgSpeces");
            Radio rSpeciesMapped = (Radio) getFellowIfAny("rSpeciesMapped");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getMapLayerMetadata().getSpeciesLsid());
                rgSpeces.insertBefore(rSp, rSpeciesMapped);
            }

            if (layers.size() > 1) {
                rSpeciesMapped.setLabel("All " + layers.size() + " species currently mapped");
            } else {
                rSpeciesMapped.setVisible(false);
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
                System.out.println(lyr.getDisplayName());
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getWKT());
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

        } catch (Exception e) {
            System.out.println("Unable to load species layers:");
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
                    }

                    btnOk.setLabel(((!nextDiv.getZclass().equalsIgnoreCase("last")) ? "Next >" : "Finish"));
                }

                currentStep++;
            } else {
                System.out.println("In the last step. Let's run the analytical tool!!!");
                currentStep = 1;
                this.detach();
                Messagebox.show("Running your analysis tool: " + selectedMethod);
            }

            //btnCancel.setLabel("< Back");
            btnBack.setDisabled(false);
            updateWindowTitle();

        } catch (InterruptedException ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(AddToolComposer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
