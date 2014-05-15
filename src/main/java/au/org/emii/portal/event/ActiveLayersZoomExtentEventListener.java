package au.org.emii.portal.event;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listitem;

public class ActiveLayersZoomExtentEventListener extends PortalEvent implements EventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on 
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            MapLayer activeLayer = listItem.getValue();

            if (activeLayer != null) {
                mapComposer.zoomToExtent(activeLayer);
            }
        } else {
            logger.debug("MapController reports unsafe to perform action");
        }
    }
}
