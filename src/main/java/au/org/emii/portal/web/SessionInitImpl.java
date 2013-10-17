package au.org.emii.portal.web;

import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.config.ConfigurationLoaderStage1Impl;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.PortalSessionCloner;
import au.org.emii.portal.util.PortalSessionIO;
import au.org.emii.portal.util.PortalSessionUtilities;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.DesktopInit;
import org.zkoss.zk.ui.util.SessionInit;

import zk.extra.BiocacheLabelLocator;

public class SessionInitImpl implements SessionInit, DesktopInit {

    public final static String PORTAL_SESSION_ATTRIBUTE = "portalSession";
    protected Logger logger = Logger.getLogger(this.getClass());
    private final static String ERROR_PAGE = "/WEB-INF/jsp/Error.jsp";
    // Max time to wait while portal is reloading
    private final static int MAX_TIME_RELOADING_SECONDS = 30;

    private PortalSession getMasterPortalSession(Session session) {
        return (PortalSession) getServletContext(session).getAttribute(ApplicationInit.PORTAL_MASTER_SESSION_ATTRIBUTE);
    }

    private ServletContext getServletContext(Session session) {
        return ((HttpSession) session.getNativeSession()).getServletContext();
    }

    private ApplicationContext getApplicationContext(Session session) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext(session));
    }

    private ConfigurationLoaderStage1Impl getConfigurationLoaderStage1(Session session) {
        return getApplicationContext(session).getBean(ConfigurationLoaderStage1Impl.class);
    }

    private PortalSessionUtilities getPortalSessionUtilities(Session session) {
        return getApplicationContext(session).getBean(PortalSessionUtilities.class);
    }

    private PortalSessionCloner getPortalSessionCloner(Session session) {
        return getApplicationContext(session).getBean(PortalSessionCloner.class);
    }
    

    private void waitForPortalReload(ConfigurationLoaderStage1Impl stage1) {
        int seconds = MAX_TIME_RELOADING_SECONDS;
        while (stage1.isReloading() && (seconds > 0)) {
            try {
                Thread.sleep(1000);
                logger.debug("waited " + (MAX_TIME_RELOADING_SECONDS - seconds) + " for portal to come up...");
            } catch (InterruptedException ex) {
            }
            seconds--;
        }

    }

    @Override
    public void init(Session session, Object request) throws Exception {
        Labels.register(new BiocacheLabelLocator());
        logger.debug("* SESSION INIT:");

        // obtain stage1 loader - check for errors
        ConfigurationLoaderStage1Impl stage1 = getConfigurationLoaderStage1(session);

        // wait for portal to finish if its reloading
        waitForPortalReload(stage1);

        if (stage1.isReloading()) {
            // portal is taking too long to load - MAX_TIME_RELOADING_SECONDS exceeded!
            redirectAndInvalidateSession(session, "/WEB-INF/jsp/Reloading.jsp");

        } else if (stage1.isError()) {
            // portal reloaded but has errors
            redirectAndInvalidateSession(session,ERROR_PAGE);
        } else {
            // see if there is a master session
            PortalSession masterPortalSession = getMasterPortalSession(session);

            if (masterPortalSession == null) {
                // hmm master portal session never got created - don't know why, check
                // output for previous errors
                logger.error("masterPortalSession is null - redirecting user to error page");

                // redirect to error page - nothing we can do and portal doesn't look to
                // be coming up any time soon
                redirectAndInvalidateSession(session, ERROR_PAGE);
            } else {
                // all good :-D

                // deep copy of PortalSession - otherwise you will be sharing state with everybody
                // after cloning, save a the independent clone in SESSION scope
                PortalSessionCloner cloner = getPortalSessionCloner(session);
                PortalSession portalSession = cloner.clone(masterPortalSession);
                session.setAttribute(PORTAL_SESSION_ATTRIBUTE, portalSession);
                logger.debug("* SESSION INIT OK");
            }
        }
    }

    private void redirectAndInvalidateSession(Session session, String page) {
        try {
            Executions.getCurrent().forward(page);
            if (session != null) {
                HttpSession nativeSession = (HttpSession) session.getNativeSession();
                if (nativeSession != null) {
                    nativeSession.invalidate();
                }
            }
        } catch (IOException ex) {
            logger.error("Error redirecting", ex);
        }
    }

    @Override
    public void init(Desktop desktop, Object request) throws Exception {
        //NC 20131015 - register the BiocacheLabelLocator as a source for URL style i18n labels
        Labels.register(new BiocacheLabelLocator());
        logger.debug("* INIT Desktop");
        Session session = desktop.getSession();
        
        if (session == null) {
            logger.info(
                    "user has a null session - no idea why (system coming up/going down - "
                    + "concurrency ?) will redirect to error page");
            redirectAndInvalidateSession(desktop.getSession(), ERROR_PAGE);
        } else {
            
            //copy existing session
            String qs = desktop.getQueryString();
            if (qs != null &&
                qs.toLowerCase().contains("session=")) {
                String s = qs.substring(qs.indexOf("session=") + "session=".length());
                if (s.indexOf('&') > 0) {
                    s = s.substring(0,s.indexOf('&'));
                }
                loadSession(desktop.getSession(), s);
            }
            
            // user has a session...

            // Sesssion is OK - do things we want to do before we start
            // composing the page
            PortalSession portalSession = (PortalSession) session.getAttribute(PORTAL_SESSION_ATTRIBUTE);
            PortalSessionUtilities portalSessionUtilities = getPortalSessionUtilities(session);
            OpenLayersJavascript openLayersJavascript = getApplicationContext(session).getBean(OpenLayersJavascript.class);
            String script =
                    openLayersJavascript.initialiseMap()
                    + openLayersJavascript.iFrameReferences
                    + openLayersJavascript.setBaseLayer(portalSession.getBaseLayer())
                    + openLayersJavascript.activateMapLayers(portalSession.getActiveLayers())
                    + openLayersJavascript.zoomToBoundingBox(portalSessionUtilities.getCurrentBoundingBox(portalSession));

            // remove all whitespace
            script = openLayersJavascript.minify(script);
            portalSession.setOnIframeMapFullyLoaded(script);
            logger.debug("onIframeMapFullyLoaded set to: " + script);
        }
        logger.debug("...session init complete");
    }

    void loadSession(Session session, String sessionId) {
        SettingsSupplementary settingsSupplementary = getApplicationContext(session).getBean(SettingsSupplementary.class);
        PortalSession ps = PortalSessionIO.readPortalSession(settingsSupplementary.getValue("session_path"), sessionId);
        
        try {
            PortalSessionCloner cloner = getPortalSessionCloner(session);
            PortalSession portalSession = cloner.clone(ps);
            session.setAttribute(PORTAL_SESSION_ATTRIBUTE, portalSession);
            logger.debug("* SESSION LOAD OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
