package au.org.emii.portal;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listitem;


public class ActiveLayersRemoveEventListener extends PortalEvent implements EventListener {

	public void onEvent(Event event) throws Exception {	
		MapComposer mapComposer = getMapComposer(event);
		if (mapComposer != null && mapComposer.safeToPerformMapAction()) {
			// get reference to the label/image the user clicked on 
			Component target = event.getTarget();
			Listitem listItem = (Listitem) target.getParent().getParent();
			MapLayer activeLayer = (MapLayer) listItem.getValue();

			if (activeLayer != null) {
				mapComposer.deactiveLayer(activeLayer, true, false);
			}
			else {
				logger.debug("nothing selected in active layers list will do nothing");
			}
		}
		else {
			logger.debug("MapController reports unsafe to perform action");
		}
	}
}
