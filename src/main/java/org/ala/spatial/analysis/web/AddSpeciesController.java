package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author Adam
 */
public class AddSpeciesController extends UtilityComposer {
    
    SettingsSupplementary settingsSupplementary;
    SpeciesAutoComplete searchSpeciesAuto;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto);
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
