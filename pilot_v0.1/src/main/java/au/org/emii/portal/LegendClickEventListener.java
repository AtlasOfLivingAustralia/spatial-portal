package au.org.emii.portal;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Popup;


public class LegendClickEventListener extends LegendEventListener {
	
	public LegendClickEventListener(MapLayer mapLayer) {
		super(mapLayer);
	}
	
	/**
	 * Show legend as an overlay window
	 * @param event
	 */
	protected void createComponents(Event event) {
		Popup popup = (Popup) Executions.createComponents("/WEB-INF/zul/LegendPopup.zul", event.getTarget().getRoot(), null);
		LegendComposer window = (LegendComposer) popup.getFirstChild();
		
		window.setMapLayer(mapLayer);
		window.extractFromPopup();
		
	}

}
