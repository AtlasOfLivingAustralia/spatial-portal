package au.org.emii.portal.composer;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.web.SessionInitImpl;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zul.Window;

/**
 * This composer will automatically autowire and autoforward your composers.
 * <p/>
 * As it extends Window, you can set both the apply and use window attributes to
 * point to a subclass without having to re-implement the afterCompose method
 * each time.
 * <p/>
 * Also includes a handy logger instances
 *
 * @author geoff
 */
public abstract class GenericAutowireAutoforwardComposer extends Window implements AfterCompose, Composer {

    private static final Logger LOGGER = Logger.getLogger(GenericAutowireAutoforwardComposer.class);

    private static final long serialVersionUID = 1L;

    /**
     * This does nothing but is required to keep zk runtime happy - if you
     * remove it you will get classcast errors when the page loads because we
     * are required to implement the do Composer interface even though we
     * already implement AfterCompose
     */
    public void doAfterCompose(Component arg0) throws Exception {
    }

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
        return (PortalSession) Sessions.getCurrent().getAttribute(SessionInitImpl.PORTAL_SESSION_ATTRIBUTE);
    }

    public void setPortalSession(PortalSession portalSession) {
        LOGGER.debug("portal session updated for: " + this.getClass().getName());
        Sessions.getCurrent().setAttribute(SessionInitImpl.PORTAL_SESSION_ATTRIBUTE, portalSession);
    }

    public MapComposer getMapComposer() {
        return (MapComposer) Executions.getCurrent()
                .getDesktop()
                .getPage("MapZul")
                .getFellow(StringConstants.MAPPORTALPAGE);
    }

}
