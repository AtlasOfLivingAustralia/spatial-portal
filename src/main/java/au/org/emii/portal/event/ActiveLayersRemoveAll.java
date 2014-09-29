package au.org.emii.portal.event;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Messagebox;

public class ActiveLayersRemoveAll extends PortalEvent implements EventListener {

    @Override
    public void onEvent(Event event) throws Exception {
        Messagebox.show("All layers will be deleted, are you sure?", "Warning", Messagebox.OK | Messagebox.CANCEL, Messagebox.EXCLAMATION
                , new EventListener() {
            public void onEvent(Event evt) {
                if ((Integer) evt.getData() == Messagebox.OK) {
                    ((MapComposer) evt.getPage().getFellow(StringConstants.MAPPORTALPAGE)).onClick$removeAllLayers();
                }
            }
        });
    }
}
