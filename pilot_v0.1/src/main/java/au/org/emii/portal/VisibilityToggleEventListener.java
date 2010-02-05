package au.org.emii.portal;

import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listitem;

/**
 * Listener attached to the checkboxes for layers listed in active layers
 * @author geoff
 *
 */
public class VisibilityToggleEventListener implements EventListener {
	private Logger logger = Logger.getLogger(this.getClass());
	
	public void onEvent(Event event) throws Exception {
		logger.debug("VisibilityToggleEventListener.onEvent() fired ");
		Checkbox checkbox = (Checkbox) event.getTarget();
		MapComposer mapComposer = (MapComposer) event.getPage().getFellow("mapPortalPage");
		if (mapComposer.safeToPerformMapAction()) {	
			MapLayer layer = (MapLayer)((Listitem) checkbox.getParent().getParent()).getValue();
			boolean checked = checkbox.isChecked();
	
			/* checkbox state will be saved automatically in MapLayer instances
			 * with calls to activate/remove in OpenLayersJavascript
			 */  		
			if (checked) {
				
				PortalSession portalSession = (PortalSession)
					Executions.getCurrent()
						.getDesktop()
							.getSession()
								.getAttribute("portalSession");
					
				OpenLayersJavascript.execute(
						OpenLayersJavascript.iFrameReferences + 
						OpenLayersJavascript.activateMapLayer(layer) +
						OpenLayersJavascript.updateMapLayerIndexes(
								portalSession.getActiveLayers()
						)
				);
				
				checkbox.setTooltiptext("Hide");
			}
			else {
				OpenLayersJavascript.removeMapLayerNow(layer);
				checkbox.setTooltiptext("Show");
			}	
			
			// hide the layer controls if we need to 
			mapComposer.updateLayerControls();
		}
		else {
			/* there was a problem performing the action - 'undo' 
			 * the user's click on the checkbox
			 */
			checkbox.setChecked(! checkbox.isChecked());
		}
	}

}
