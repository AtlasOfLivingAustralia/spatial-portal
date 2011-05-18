package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

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

    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
    }

    public void onClick$btnOk(Event event) {
        if (rSearch.isSelected()) {
            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto);
        } else if(rAllSpecies.isSelected()) {
            Window window = (Window) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
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
//            usc.setEventListener(new EventListener() {
//
//                    @Override
//                    public void onEvent(Event event) throws Exception {
//                        setLsid((String)event.getData());
//                    }
//                });
            if (rUploadCoordinates.isSelected()) {
                usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
            } else if (rUploadLSIDs.isSelected()) {
                usc.setTbInstructions("3. Select file (text file, one LSID per line)");
            } else {
                usc.setTbInstructions("3. Select file");
            }
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
}
