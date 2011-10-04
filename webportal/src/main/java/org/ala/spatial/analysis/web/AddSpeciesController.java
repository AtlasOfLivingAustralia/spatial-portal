package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Vbox;

/**
 *
 * @author Adam
 */
public class AddSpeciesController extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    SpeciesAutoComplete searchSpeciesAuto;
    Button btnOk;
    Radio rSearch;
    Radio rUploadCoordinates;
    Radio rUploadLSIDs;
    Radio rAllSpecies;
    Radiogroup rgAddSpecies;
    Vbox vboxSearch;
    Checkbox chkArea;
    
    Query query;
    String rank;
    String taxon;

    boolean prevAreaState = true;

    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
        chkArea.setChecked(true);
    }

    public void onClick$btnOk(Event event) {
        if(btnOk.isDisabled()) {
            return;
        }
        if(rAllSpecies.isSelected()) {
            AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
            window.setAllSpecies(true);
            window.loadAreaLayers();
            try {
                window.doModal();
            } catch (InterruptedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SuspendNotAllowedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if(chkArea.isChecked()) {
            getFromAutocomplete();

            if (rSearch.isSelected()) {
                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
                window.setSpeciesParams(query, rank, taxon);
                window.loadAreaLayers();
                try {
                    window.doModal();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SuspendNotAllowedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                onClick$btnUpload(event);
            }
        } else {
            if (rSearch.isSelected()) {
                getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, null);
            } else {
                onClick$btnUpload(event);
            }
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$searchSpeciesAuto(Event event) {
        btnOk.setDisabled(true);

        refreshBtnOkDisabled();
    }

    public void onClick$btnUpload(Event event) {
        try {
            UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", getMapComposer(), null);

            if (rUploadCoordinates.isSelected()) {
                usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
            } else if (rUploadLSIDs.isSelected()) {
                usc.setTbInstructions("3. Select file (text file, one LSID per line)");
            } else {
                usc.setTbInstructions("3. Select file");
            }
            usc.setDefineArea(chkArea.isChecked());
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
        }
        
        refreshBtnOkDisabled();
    }

    public void onCheck$rgAddSpecies(Event event) {
        if(rSearch.isSelected()) {
           vboxSearch.setVisible(true);
//           searchSpeciesAuto.setFocus(true);
        } else {
            vboxSearch.setVisible(false);
        }

        refreshBtnOkDisabled();
    }

    private void refreshBtnOkDisabled() {
        if (rSearch.isSelected()) {
            btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() == null || searchSpeciesAuto.getSelectedItem().getValue() == null);
        } else {
            btnOk.setDisabled(false);
        }
    }

    void getFromAutocomplete() {
        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (searchSpeciesAuto.getSelectedItem() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0) {
            return;
        }

        //btnSearchSpecies.setVisible(true);
        taxon = searchSpeciesAuto.getValue();
        rank = "";

        String spVal = searchSpeciesAuto.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

        } else {
            rank = StringUtils.substringBefore(spVal, ",").toLowerCase();
            System.out.println("mapping rank and species: " + rank + " - " + taxon);
        }
        if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
            rank = "taxon";
        }

        query = QueryUtil.get((String)searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), true);
    }
}
