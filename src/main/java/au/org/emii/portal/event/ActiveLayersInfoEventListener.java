package au.org.emii.portal.event;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.util.LayerUtilities;
import org.ala.spatial.data.Query;
import org.ala.spatial.util.ScatterplotData;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listitem;

public class ActiveLayersInfoEventListener extends PortalEvent implements EventListener {

    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            MapLayer activeLayer = (MapLayer) listItem.getValue();

            if (activeLayer != null) {
                if(activeLayer.getData("query") != null) {
                    //TODO: update for scatterplot layers
                    Query q = (Query) activeLayer.getData("query");
                    Events.echoEvent("openHTML", mapComposer, q.getMetadataHtml());
                } else if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                    String infourl = activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", ".");
                    if (activeLayer.getSubType()==LayerUtilities.SCATTERPLOT) {
                        ScatterplotData data = (ScatterplotData) activeLayer.getData("scatterplotData");
                        infourl += "?dparam=X-Layer:"+data.getLayer1Name();
                        infourl += "&dparam=Y-Layer:"+data.getLayer2Name();
                    }
                    // send the user to the BIE page for the species
                    logger.debug("opening the following url " + infourl);
                    Events.echoEvent("openUrl", mapComposer, activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));

                } else if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                    logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());
                    //mapComposer.showMessage(activeLayer.getMapLayerMetadata().getMoreInfo());

                    String metadata = activeLayer.getMapLayerMetadata().getMoreInfo();
//                    if (activeLayer.getType() == LayerUtilities.WKT) {
//                        metadata += "Extent: " + mapComposer.getLayerBoundingBox(activeLayer) + " <br />\n";
//                    }

                    Events.echoEvent("openHTML", mapComposer, metadata);
                } else {
                    logger.debug("no metadata is available for current layer");
                    mapComposer.showMessage("Metadata currently unavailable");
                }
            } else {
                //logger.debug("nothing selected in active layers list will do nothing");
            }
        } else {
            logger.debug("MapController reports unsafe to perform action");
        }
    }
}
