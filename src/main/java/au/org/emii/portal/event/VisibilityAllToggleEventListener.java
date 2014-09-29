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
public class VisibilityAllToggleEventListener implements EventListener {

    private boolean show;

    public VisibilityAllToggleEventListener(boolean show) {
        this.show = show;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        ((MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE)).setLayersVisible(show);
    }
}
