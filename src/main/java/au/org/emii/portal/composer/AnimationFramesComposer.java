package au.org.emii.portal.composer;

import au.org.emii.portal.menu.MapLayer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radiogroup;

public class AnimationFramesComposer extends UtilityComposer {
	
	private static final long serialVersionUID = 1L;
	private MapLayer activeLayer;
	private Label errorMessage;
	private Radiogroup selectedFrameRate;
	
	// ok button
	public void onClick$submit() {
		if (validate()) {
			String selected = selectedFrameRate.getSelectedItem().getValue();
			logger.debug("OK clicked - fetching animation for " + selected);				
			activeLayer.getAnimationSelection().setSelectedTimeStringKey(selected);
			detach();
			
			// show the animation layer now
			getAnimationControlsComposer().activateAnimation(activeLayer);	
		}
		else {
			errorMessage.setVisible(true);
		}
	}
		
	// cancel button
        @Override
	public void onClick$close() {
		detach();
	}

	private boolean validate() {
		boolean valid;
		if (selectedFrameRate.getSelectedItem() != null) {
			valid = true;
		}
		else {
			valid = false;
		}
		return valid;	
	}

        @Override
	public void afterCompose() {
		super.afterCompose();
		//  select the last item in the list (least frames) by default
		selectedFrameRate.setSelectedIndex(selectedFrameRate.getItemCount() - 1);
	}
	
	public AnimationControlsComposer getAnimationControlsComposer() {
		return null; /*((MapComposer) Executions.getCurrent()
				.getDesktop()
					.getPage("MapZul")
						.getFellow("mapPortalPage")
		).getAnimationControlsComposer();*/
	}

	public MapLayer getActiveLayer() {
		return activeLayer;
	}

	public void setActiveLayer(MapLayer activeLayer) {
		this.activeLayer = activeLayer;
	}

}
