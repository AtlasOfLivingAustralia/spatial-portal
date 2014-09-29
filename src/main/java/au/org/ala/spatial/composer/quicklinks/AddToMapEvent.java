/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * @author adam
 */
public class AddToMapEvent implements EventListener {

    private String type;

    public AddToMapEvent(String type) {
        this.type = type;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mc = (MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE);
        if (StringConstants.SPECIES.equals(type)) {
            mc.onClick$btnAddSpecies(null);
        } else if (StringConstants.AREA.equals(type)) {
            mc.onClick$btnAddArea(null);
        } else if (StringConstants.LAYER.equals(type)) {
            mc.onClick$btnAddLayer(null);
        } else if (StringConstants.FACET.equals(type)) {
            mc.onClick$btnAddFacet(null);
        }
    }
}
