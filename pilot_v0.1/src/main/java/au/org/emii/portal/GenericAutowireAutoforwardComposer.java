package au.org.emii.portal;

import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zul.Window;

/**
 * This composer will automatically autowire and autoforward your composers.
 * 
 * As it extends Window, you can set both the apply and use window attributes
 * to point to a subclass without having to re-implement the afterCompose 
 * method each time.
 *  
 * Also includes a handy logger instances 
 * @author geoff
 *
 */
public abstract class GenericAutowireAutoforwardComposer extends Window implements AfterCompose, Composer {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Log4j instance
	 */
	protected Logger logger = Logger.getLogger(this.getClass());

	
	
	/**
	 * This does nothing but is required to keep zk runtime happy - if
	 * you remove it you will get classcast errors when the page loads
	 * because we are required to implement the do Composer interface 
	 * even though we already implement AfterCompose
	 */
	public void doAfterCompose(Component arg0) throws Exception {}
	
	/**
	 * Perform the autowiring - courtesy of the zk mvc 3 tutorial
	 */
    public void afterCompose() {
        //wire variables
        Components.wireVariables(this, this);
        
        //NO need to register onXxx event listeners

        //auto forward
        Components.addForwards(this, this);
    }
    
    public PortalSession getPortalSession() {
        return (PortalSession) Sessions.getCurrent().getAttribute("portalSession");
    }
    


	
	protected MapComposer getMapComposer() {
		return (MapComposer) getRoot();
	}



}
