/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.util.ScatterplotData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

/**
 * @author a
 */
public class MetadataEvent implements EventListener {

    String layerName;
    MapComposer mc;

    public MetadataEvent(MapComposer mc, String layerName) {
        this.mc = mc;
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if (mapLayer != null) {
            if (mapLayer.getSpeciesQuery() != null) {
                //TODO: update for scatterplot layers
                Query q = mapLayer.getSpeciesQuery();
                Events.echoEvent("openHTML", mc, q.getMetadataHtml());
            } else if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                String infourl = mapLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                if (mapLayer.getSubType() == LayerUtilities.SCATTERPLOT) {
                    ScatterplotData data = mapLayer.getScatterplotData();
                    infourl += "?dparam=X-Layer:" + data.getLayer1Name();
                    infourl += "&dparam=Y-Layer:" + data.getLayer2Name();
                }
                // send the user to the BIE page for the species
                Events.echoEvent("openUrl", mc, infourl);

            } else if (mapLayer.getMapLayerMetadata() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                //logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());
                Events.echoEvent("openHTML", mc, mapLayer.getMapLayerMetadata().getMoreInfo());
            } else {
                //logger.debug("no metadata is available for current layer");
                mc.showMessage("Metadata currently unavailable");
            }
        }
    }
}
