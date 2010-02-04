package au.org.emii.portal.composer;

import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.Executions;

public class ErrorMessageWithDetailAndRawDataComposer extends
		ErrorMessageWithDetailComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void onClick$close() {
		close();
		super.onClick$close();
	}
	
	private void close() {
		/* at this point the user has closed the message box - we
		 * only have one more thing to do to stop the big zk error
		 * and that is to grab the iframe back off the error message
		 * window and put it back where we found it in the index.zul
		 * page - not pretty or efficient but works a treat!  
		 */
		MapComposer mc = getMapComposer();
		mc.getRawMessageIframeHack().setParent(
				mc.getRawMessageHackHolder());
	}
	
	public MapComposer getMapComposer() {
		return (MapComposer) Executions.getCurrent()
				.getDesktop()
					.getPage("MapZul")
						.getFellow("mapPortalPage");
	}
}
