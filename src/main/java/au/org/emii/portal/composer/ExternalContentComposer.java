package au.org.emii.portal.composer;

import au.org.emii.portal.OpenLayersJavascript;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zul.Iframe;

public class ExternalContentComposer extends UtilityComposer {

	private static final long serialVersionUID = 1L;
	private Iframe externalContentIframe;
    private OpenLayersJavascript openLayersJavascript = null;
	/**
	 * Breakout the external content window to be a new browser window
	 * instead (SUBJECT TO POPUP BLOCKING!)
	 */
	public void onClick$breakout() {
		// find the uri from the iframe...
		logger.debug("breakout external content");
		String uri = externalContentIframe.getSrc();
		if (uri != null) {
			setVisible(false);
			openLayersJavascript.popupWindowNow(uri, getTitle());
		}
		else {
			logger.info("onBreakoutExternalContent called when there is no src set in iframe");
		}
		
	}

    public OpenLayersJavascript getOpenLayersJavascript() {
        return openLayersJavascript;
    }

    public void setOpenLayersJavascript(OpenLayersJavascript openLayersJavascript) {
        this.openLayersJavascript = openLayersJavascript;
    }


}
