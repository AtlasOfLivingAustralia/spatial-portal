package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddModelController extends UtilityComposer {
    
    SettingsSupplementary settingsSupplementary;
    Radiogroup rgModel;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        String label = "New Model";
        int type = LayerUtilities.ALOC;
        if (rgModel.getSelectedItem() == rMaxent) {
            type = LayerUtilities.MAXENT;
            label = "New Maxent Model";
        } else if (rgModel.getSelectedItem() == rAloc) {
            type = LayerUtilities.ALOC;
            label = "New ALOC Model";
        } else if (rgModel.getSelectedItem() == rScatterplot) {
            type = LayerUtilities.SCATTERPLOT;
            label = "New Scatterplot Model";
        } else if (rgModel.getSelectedItem() == rGdm) {
            type = LayerUtilities.GDM;
            label = "New GDM Model";
        } else if (rgModel.getSelectedItem() == rTabulation) {
            type = LayerUtilities.TABULATION;
            label = "New Tabulation Model";
        }

        getMapComposer().addUserDefinedLayerToMenu(getMapComposer().getRemoteMap().createLocalLayer(type, label), true);

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
