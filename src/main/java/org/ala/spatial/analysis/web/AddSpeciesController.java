package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.List;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
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
    private String lsid;
    Radio rSearch;
    Radiogroup rgAddSpecies;
    Vbox vboxSearch;

    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
    }

    public void onClick$btnOk(Event event) {
        if(rSearch.isSelected()) {
            getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto);
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

        if (searchSpeciesAuto.getSelectedItem() != null) {
            btnOk.setDisabled(false);
        }
    }

    public void onClick$btnUpload(Event event) {
        try {
            UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", getMapComposer(), null);
            usc.setEventListener(new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        setLsid((String)event.getData());
                    }
                });
            usc.doModal();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    void setLsid(String lsidName) {
        String [] s = lsidName.split("\t");
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
                                && ((String)ci.getAnnotatedProperties().get(0)).equals(lsid)){
                            searchSpeciesAuto.setSelectedItem(ci);
                            break;
                        }
                    }
                }
            }
            btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() == null);
        }
    }

    public void onCheck$rgAddSpecies(Event event) {
        if(rSearch.isSelected()) {
           btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() == null);
           btnOk.setLabel("OK");
           vboxSearch.setVisible(true);
        } else {
           btnOk.setDisabled(false);
           btnOk.setLabel("Next");
           vboxSearch.setVisible(false);
        }
    }
}
