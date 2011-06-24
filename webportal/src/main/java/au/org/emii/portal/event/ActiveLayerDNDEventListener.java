package au.org.emii.portal.event;

import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listitem;

/**
 * 
 * @author geoff
 */
public class ActiveLayerDNDEventListener extends PortalEvent implements EventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        logger.debug("active layers item drop received");
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            logger.debug("inside ActiveLayerDNDEventListener.onEvent()");

            if (event instanceof DropEvent) {
                DropEvent dragEvent = (DropEvent) event;

                // we support the treechildren and other list items...
                Component eventType = dragEvent.getDragged();
                if (eventType instanceof Listitem) {
                    reorderList(mapComposer, dragEvent);
                }
                else {
                    logger.info("unsupported dnd event " + eventType.getClass().getName());
                }
            }
            else {
                logger.info("event is not a DropEvent instance: " + event.getClass().getName());
            }
        }
    }

    public void reorderList(MapComposer mapComposer, DropEvent dropEvent) {
        Listitem dragged = (Listitem) dropEvent.getDragged();
        Listitem dropped = (Listitem) dropEvent.getTarget();
        mapComposer.reorderList(dragged, dropped);
//        mapComposer.updateLayerControls();
    }
}
