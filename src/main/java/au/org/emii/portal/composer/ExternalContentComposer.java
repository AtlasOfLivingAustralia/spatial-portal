package au.org.emii.portal.composer;

import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Iframe;

public class ExternalContentComposer extends UtilityComposer {

	private static final long serialVersionUID = 1L;
	private Iframe externalContentIframe;
    private OpenLayersJavascript openLayersJavascript = null;
    String src;

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

        /**
         * resets the src of the iframe
         */
        public void onClick$reset(){
            externalContentIframe.setSrc("/img/loading_small.gif");
            Events.echoEvent("setSrc", this, null);
        }

        public void setSrc(Event event){
            externalContentIframe.setSrc(src);
        }

    @Override
        public void onClick$hide() {
            //reset frame src
            externalContentIframe.setSrc("/img/loading_small.gif");
            super.onClick$hide();
        }

    public OpenLayersJavascript getOpenLayersJavascript() {
        return openLayersJavascript;
    }

    public void setOpenLayersJavascript(OpenLayersJavascript openLayersJavascript) {
        this.openLayersJavascript = openLayersJavascript;
    }


}
