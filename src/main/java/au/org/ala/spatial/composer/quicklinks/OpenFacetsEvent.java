/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

/**
 * @author adam
 */
public class OpenFacetsEvent implements EventListener {

    private String layerName;

    public OpenFacetsEvent(String layerName) {
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mc = (MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE);
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if (mapLayer != null) {
            if (StringConstants.GRID.equals(mapLayer.getColourMode())) {
                mapLayer.setColourMode("-1");
                mc.updateLayerControls();
            }
            Events.echoEvent("openFacets", mc, null);
        }
    }
}
