package au.org.emii.portal.composer;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;

public class ErrorMessageWithDetailComposer extends UtilityComposer {

	private static final long serialVersionUID = 1L;

	private Div moreInfoDiv;
	private Button showDetailsButton;
	private Button hideDetailsButton;
	
	public void onClick$showDetailsButton() {
		showDetails(true);
	}
	
	public void onClick$hideDetailsButton() {
		showDetails(false);
	}
		
	public void showDetails(boolean show) {
		moreInfoDiv.setVisible(show);
		showDetailsButton.setVisible(! show);
		hideDetailsButton.setVisible(show);
		
		if (show) {
			
			setWidth("80%");
			setHeight("575px");
		}
		else {
			setWidth("450px");
			setHeight("225px");
		}						
		setPosition("center");
	}
}

