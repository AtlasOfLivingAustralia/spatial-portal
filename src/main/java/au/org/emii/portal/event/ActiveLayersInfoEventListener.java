package au.org.emii.portal.event;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.MapComposer;
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
                if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().startsWith("http://")) {
                    // send the user to the BIE page for the species
                    logger.debug("opening the following url " + activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));
                    Events.echoEvent("openUrl", mapComposer, activeLayer.getMapLayerMetadata().getMoreInfo().replace("__", "."));

                } else if (activeLayer.getMapLayerMetadata() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo() != null
                        && activeLayer.getMapLayerMetadata().getMoreInfo().length() > 0) {
                    logger.debug("performing a MapComposer.showMessage for following content " + activeLayer.getMapLayerMetadata().getMoreInfo());
                    //mapComposer.showMessage(activeLayer.getMapLayerMetadata().getMoreInfo());
                    Events.echoEvent("openHTML", mapComposer, activeLayer.getMapLayerMetadata().getMoreInfo());
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
