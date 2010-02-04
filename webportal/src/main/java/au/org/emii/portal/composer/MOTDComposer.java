package au.org.emii.portal.composer;


import au.org.emii.portal.MOTD;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zul.api.Label;

public class MOTDComposer extends UtilityComposer {

	private static final long serialVersionUID = 1L;
	
	// autowire controls
	private Label message;
	private Label title;
	
	
	public void afterCompose() {
		super.afterCompose();
		update();
	}
	
	
	public void update() {
		title.setValue(MOTD.getMotd("title"));
		message.setValue(MOTD.getMotd("message"));
	}
	
	public void onClick$close() {
		logger.debug("close motd");
		detach();
	}
}
