package au.org.emii.portal;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class UserDefinedMapLayerRemoveEventListener extends PortalEvent implements EventListener {

	/**
	 * Handle the user clicking the remove layer garbage can
	 * for user defined menu items that have been added with
	 * add wms server.
	 * 
	 * If the layer is currently being displayed, it will be
	 * removed from active layers first, along with all it's
	 * children
	 */
	public void onEvent(Event event) throws Exception {
		logger.debug("request remove user defined layer");
		MapComposer mc = getMapComposer(event);
		MenuItem mi = null;
		String script = "";
		if ((mc != null) && mc.safeToPerformMapAction()) {
			// step 1, remove from active layers
			mi = getMenuItem(event);
			if ((mi != null) && mi.isValueMapLayerInstance()) {
				MapLayer mapLayer = mi.getValueAsMapLayer();
				
				// can't exececute js immediately or you get errors
				script += OpenLayersJavascript.removeMapLayer(mapLayer, true);				
				mc.deactiveLayer(mi.getValueAsMapLayer(), false, true);
			
				// step 2, remove the mapLayer from portalSession
				mc.removeUserDefinedMenuItem(mi);
				
				// update layer controls and remove the layers from the map
				mc.updateLayerControls();
				mc.updateUserDefinedView();
				/* removing menu items can stop the user defined view from
				 * being displayable any more - switch to facilities tab 
				 * if so
				 */
				PortalSession ps = mc.getPortalSession();
				if (! ps.isUserDefinedViewDisplayable()) {
					// set application state for selecting the view...
					mc.selectAndActivateTab(PortalSession.VIEW_FACILITY);
				}
				
				/* try to add the layer back to the list of available layers
				 * in AddLayerComposer in case the user added an individual
				 * discovered layer and then removed it (and will later want
				 * to add it again) 
				 */
				((AddLayerComposer) mc
						.getFellow("leftMenuSearch")
							.getFellow("leftSearch")
								.getFellow("addLayer")
									.getFellow("addLayerWindow")
				).relist(mapLayer);
				
				OpenLayersJavascript.execute(
						OpenLayersJavascript.iFrameReferences + 
						OpenLayersJavascript.wrapWithSafeToProceed(script)
				);
			}
		}
	}

}
