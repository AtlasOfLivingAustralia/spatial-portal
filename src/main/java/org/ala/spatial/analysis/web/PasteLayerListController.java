package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class PasteLayerListController extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    Textbox layerList;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        if (getParent() instanceof AddToolComposer) {
            ((AddToolComposer) getParent()).selectLayerFromList(layerList.getText());
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
