package au.org.emii.portal.event;

import au.org.emii.portal.composer.MapComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listitem;

public class ActiveLayersLegendEventListener extends PortalEvent implements EventListener {
    private static final Logger LOGGER = Logger.getLogger(PortalEvent.class);

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mapComposer = getMapComposer(event);
        if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
            // get reference to the label/image the user clicked on 
            Component target = event.getTarget();
            Listitem listItem = (Listitem) target.getParent().getParent();
            listItem.setSelected(true);
        } else {
            LOGGER.debug("MapController reports unsafe to perform action");
        }
    }
}
