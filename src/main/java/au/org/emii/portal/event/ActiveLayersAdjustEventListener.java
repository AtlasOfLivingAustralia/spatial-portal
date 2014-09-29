package au.org.emii.portal.event;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Listener attached to the checkboxes for layers listed in active layers
 *
 * @author geoff
 */
public class ActiveLayersAdjustEventListener implements EventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        ((MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE)).adjustActiveLayersList();
    }
}
