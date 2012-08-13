package au.org.emii.portal.event;

import au.org.emii.portal.request.DesktopState;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.LegendComposer;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;


public abstract class LegendEventListener extends PortalEvent implements EventListener {

	protected MapLayer mapLayer = null;
	
	@SuppressWarnings("unused")
	private LegendEventListener() {}
	
	public LegendEventListener(MapLayer mapLayer) {
		this.mapLayer = mapLayer;
	}
	
	/**
	 * Check if we are already displaying the legend as an overlay window.
	 * 
	 * @return handle to the window if the legend is being displayed,
	 * otherwise null
	 */
	protected LegendComposer legendDisplaying(Event event) {
		MapComposer mc = getMapComposer(event);
		DesktopState ds = mc.getDesktopState();

		LegendComposer window = ds.getVisibleLegendByMapLayer(mapLayer);
		return window;
	}
	

	
	
	public void onEvent(Event event) throws Exception {
		logger.debug("display legend");

		// see if we are already displaying the legend.  If we are, just
		// reposition it, otherwise display a new one
		LegendComposer window = legendDisplaying(event);
		if (window == null) {
			createComponents(event);
		}
		else {
			legendAlreadyDisplayed(window, event);
		}
	}
	
	protected abstract void createComponents(Event event);
	
	protected void legendAlreadyDisplayed(LegendComposer window, Event event) {
		window.reposition();	
	}
	
}
