package au.org.emii.portal.composer;


import au.org.emii.portal.motd.MOTD;
import org.zkoss.zul.api.Label;

public class MOTDComposer extends UtilityComposer {

    private MOTD motd = null;
	private static final long serialVersionUID = 1L;
	
	// autowire controls
	private Label message;
	private Label title;

    public MOTD getMotd() {
        return motd;
    }

    public void setMotd(MOTD motd) {
        this.motd = motd;
    }


	
    @Override
	public void afterCompose() {
		super.afterCompose();
		update();
	}
	
	
	public void update() {
		title.setValue(motd.getMotd("title"));
		message.setValue(motd.getMotd("message"));
	}
	
    @Override
	public void onClick$close() {
		logger.debug("close motd");
		detach();
	}
}
