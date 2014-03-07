package au.org.emii.portal.event;

import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.util.ScatterplotData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listitem;

public class ActiveLayersInfoEventListener extends PortalEvent implements EventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            MapLayer activeLayer = listItem.getValue();

            if (activeLayer != null) {
                if (activeLayer.getSpeciesQuery() != null) {

                    Query q = activeLayer.getSpeciesQuery();
                    Events.echoEvent("openHTML", mapComposer, q.getMetadataHtml());
                } else if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                    String infourl = activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                    if (activeLayer.getSubType() == LayerUtilities.SCATTERPLOT) {
                        ScatterplotData data = activeLayer.getScatterplotData();
                        infourl += "?dparam=X-Layer:" + data.getLayer1Name();
                        infourl += "&dparam=Y-Layer:" + data.getLayer2Name();
                    }
                    // send the user to the BIE page for the species
                    logger.debug("opening the following url " + infourl);
                    Events.echoEvent("openUrl", mapComposer, activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));

                } else if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                    logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());

                    String metadata = activeLayer.getMapLayerMetadata().getMoreInfo();

                    Events.echoEvent("openHTML", mapComposer, metadata);
                } else {
                    logger.debug("no metadata is available for current layer");
                    mapComposer.showMessage("Metadata currently unavailable");
                }
            }
        } else {
            logger.debug("MapController reports unsafe to perform action");
        }
    }
}
