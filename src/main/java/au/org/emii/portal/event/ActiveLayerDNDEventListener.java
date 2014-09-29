package au.org.emii.portal.event;

import au.org.emii.portal.composer.MapComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listitem;

/**
 * @author geoff
 */
public class ActiveLayerDNDEventListener extends PortalEvent implements EventListener {
    private static final Logger LOGGER = Logger.getLogger(PortalEvent.class);

    @Override
    public void onEvent(Event event) throws Exception {
        LOGGER.debug("active layers item drop received");
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            LOGGER.debug("inside ActiveLayerDNDEventListener.onEvent()");

            if (event instanceof DropEvent) {
                DropEvent dragEvent = (DropEvent) event;

                // we support the treechildren and other list items...
                Component eventType = dragEvent.getDragged();
                if (eventType instanceof Listitem) {
                    reorderList(mapComposer, dragEvent);
                } else {
                    LOGGER.debug("unsupported dnd event " + eventType.getClass().getName());
                }
            } else {
                LOGGER.debug("event is not a DropEvent instance: " + event.getClass().getName());
            }
        }
    }

    public void reorderList(MapComposer mapComposer, DropEvent dropEvent) {
        Listitem dragged = (Listitem) dropEvent.getDragged();
        Listitem dropped = (Listitem) dropEvent.getTarget();
        mapComposer.reorderList(dragged, dropped);
    }
}
