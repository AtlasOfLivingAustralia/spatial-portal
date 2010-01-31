package au.org.emii.portal.web;

import au.org.emii.portal.OpenLayersJavascript;
import au.org.emii.portal.PortalException;
import au.org.emii.portal.PortalSession;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.DesktopInit;
import org.zkoss.zk.ui.util.SessionInit;

public class SessionInitImpl implements SessionInit, DesktopInit {

    protected Logger logger = Logger.getLogger(this.getClass());
    private final static String ERROR_PAGE = "/WEB-INF/jsp/Error.jsp";

    @Override
    public void init(Session session, Object request) throws Exception {
        logger.debug("* SESSION INIT:");
        try {

            // obtain spring context
            ServletContext servletContext = ((HttpSession) session.getNativeSession()).getServletContext();
            ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

            // ask for portal session
            PortalSession masterPortalSession = PortalSessionAccessor.getMasterPortalSession();

            // deep copy of PortalSession - otherwise you will be sharing state with everybody
            List<String> faults = null;
            if ((masterPortalSession != null)
                    && ((faults = masterPortalSession.check()).size() == 0)) {

                PortalSession portalSession = (PortalSession) masterPortalSession.clone();

                // go ahead and create the session
                session.setAttribute("portalSession", portalSession);

                logger.debug("Session: " + portalSession.dump());

                logger.debug("* SESSION INIT OK");

            } else {
                /* application is not setup correctly - either the session has been
                 * created just as the server is loading up (hit refresh in web browser
                 * to fix) or everythings broken and the site is down.  Throw a
                 * RuntimeException here to redirect to the error page.
                 *
                 * We can't use a ZK error page because we can't create a ZK session
                 * (which is why we're in this block of code) so unless there's a way
                 * to tell ZK to forward to an error page right now (haven't seen one)
                 * then we have to load a JSP, as configured in web.xml
                 */
                String errorMessage;
                if (masterPortalSession == null) {
                    errorMessage =
                            "No PortalSession instance found in servlet context - restart servlet "
                            + "container and CHECK APPLICATION INITIALISATION MESSAGES ";
                } else {
                    errorMessage =
                            "PortalSession found but fails check() - CHECK CONFIG FILE!";
                }

                // it's our own exception so don't print the whole stack trace...
                logger.fatal("unable to create session: " + new PortalException(errorMessage, faults).getMessage());
                session.setAttribute("broken", Boolean.valueOf(true));
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void redirectErrorPageAndInvalidateSession(Desktop desktop) throws IOException {
        desktop.getExecution().forward(ERROR_PAGE);
        Session session = desktop.getSession();
        if (session != null) {
            HttpSession nativeSession = (HttpSession) session.getNativeSession();
            if (nativeSession != null) {
                nativeSession.invalidate();
            }
        }
    }

    @Override
    public void init(Desktop desktop, Object request) throws Exception {
        logger.debug("* INIT Desktop");
        Session session = desktop.getSession();
        if (session == null) {
            logger.info(
                    "user has a null session - no idea why (system coming up/going down - "
                    + "concurrency ?) will redirect to error page");
            redirectErrorPageAndInvalidateSession(desktop);
        } else {
            // user has a session...
            Boolean broken = (Boolean) session.getAttribute("broken");
            if (broken != null && broken.booleanValue()) {
                logger.debug("User's session is marked as bad, redirecting to error page and invalidating session");
                redirectErrorPageAndInvalidateSession(desktop);
            } else {
                // Sesssion is OK - do things we want to do before we start
                // composing the page
                PortalSession portalSession = (PortalSession) session.getAttribute("portalSession");
                String script =
                        OpenLayersJavascript.initialiseMap()
                        + OpenLayersJavascript.iFrameReferences
                        + OpenLayersJavascript.activateMapLayer(portalSession.getCurrentBaseLayer())
                        + OpenLayersJavascript.activateMapLayers(portalSession.getActiveLayers())
                        + OpenLayersJavascript.zoomToBoundingBox(portalSession.getCurrentBoundingBox());

                // remove all whitespace
                script = OpenLayersJavascript.minify(script);
                portalSession.setOnIframeMapFullyLoaded(script);
                logger.debug("onIframeMapFullyLoaded set to: " + script);
            }
        }
    }
}
