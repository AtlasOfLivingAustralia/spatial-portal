/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ScatterplotDataDTO;
import au.org.ala.spatial.util.Query;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

/**
 * @author a
 */
public class MetadataEvent implements EventListener {

    private String layerName;

    public MetadataEvent(String layerName) {
        this.layerName = layerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mc = (MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE);
        MapLayer mapLayer = mc.getMapLayer(layerName);
        if (mapLayer != null) {
            if (mapLayer.getSpeciesQuery() != null) {
                //TODO: update for scatterplot layers
                Query q = mapLayer.getSpeciesQuery();
                Events.echoEvent(StringConstants.OPEN_HTML, mc, q.getMetadataHtml());
            } else if (mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                String infourl = mapLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                if (mapLayer.getSubType() == LayerUtilitiesImpl.SCATTERPLOT) {
                    ScatterplotDataDTO data = mapLayer.getScatterplotDataDTO();
                    infourl += "?dparam=X-Layer:" + data.getLayer1Name();
                    infourl += "&dparam=Y-Layer:" + data.getLayer2Name();
                }
                // send the user to the BIE page for the species
                Events.echoEvent(StringConstants.OPEN_URL, mc, infourl);

            } else if (mapLayer.getMapLayerMetadata().getMoreInfo() != null
                    && mapLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                Events.echoEvent(StringConstants.OPEN_HTML, mc, mapLayer.getMapLayerMetadata().getMoreInfo());
            } else {
                mc.showMessage("Metadata currently unavailable");
            }
        }
    }
}
