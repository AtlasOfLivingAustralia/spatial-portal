/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * @author adam
 */
public class AddToMapEvent implements EventListener {

    String type;
    MapComposer mc;

    public AddToMapEvent(MapComposer mc, String type) {
        this.mc = mc;
        this.type = type;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (type.equals("species")) {
            mc.onClick$btnAddSpecies(null);
        } else if (type.equals("area")) {
            mc.onClick$btnAddArea(null);
        } else if (type.equals("layer")) {
            mc.onClick$btnAddLayer(null);
        } else if (type.equals("facet")) {
            mc.onClick$btnAddFacet(null);
        }
    }
}
