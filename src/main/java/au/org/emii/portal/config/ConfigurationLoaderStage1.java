/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.config.xmlbeans.PortalDocument;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.web.ApplicationInit;
import java.util.Date;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Configuration loader
 * ~~~~~~~~~~~~~~~~~~~~
 * This class loads the config file file.  Stage 1 is to create and save a spring
 * application context
 * 
 * @author geoff
 */
public class ConfigurationLoaderStage1 implements Runnable {

    /**
     * The config file re-read interval is normally read from the
     * config file, but if the config file is broken, we can't
     * get an initial value, so we will use this one which
     * specifies reloading time in ms
     *
     * 300000 = 5 mins
     */
    public final static int BROKEN_CONFIG_RELOAD = 300000;
    
    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());
    

    @Autowired
    private ConfigurationLoaderStage2 stage2 = null;

    @Autowired
    private ConfigurationFile configurationFile = null;

    private Settings settings = null;

    /**
     * Request thread shutdown
     */
    private boolean running = false;

    /**
     * Flag to indicate whether configuration is currently reloading
     */
    private boolean reloading = false;

    /**
     * Time to keep configuration file in memory - other components wanting
     * to refresh things from config should read this value
     */
    public static int rereadInterval = BROKEN_CONFIG_RELOAD;

    private Date lastReloaded = null;

    private ServletContext servletContext = null;

    /**
     * Flag to indicate whether the configuration contains error(s) or not
     */
    private boolean error = false;

    private PortalSessionUtilities portalSessionUtilities = null;

    public ConfigurationLoaderStage2 getStage2() {
        return stage2;
    }

    @Required
    public void setStage2(ConfigurationLoaderStage2 stage2) {
        this.stage2 = stage2;
    }

    private void load() {
        reloading = true;
        PortalDocument portalDocument = configurationFile.readConfigFile();
        if (portalDocument == null) {
            logger.info("Configuration file missing or invalid - cannot load portal.  See previous message for cause");
        } else {
            stage2.setPortalDocument(portalDocument);
            // always a new portal session - do not use spring bean
            PortalSession portalSession = new PortalSession();
            stage2.setPortalSession(portalSession);
            stage2.load();

            /* Now we need to store an application scope variable storing the
             * relevant details from the config file
             *
             *  Because its stored as method scope variables and published, we
             *  don't need to worry about concurrent modification.
             *
             *  Also give a copy of servlet context to PortalSessionAccessor
             *  so that it can be accessed via spring
             */
            servletContext.setAttribute(ApplicationInit.PORTAL_MASTER_SESSION_ATTRIBUTE, portalSession);
            logger.debug("finished building master portalSession - final structure is: \n" +
                    portalSessionUtilities.dump(portalSession));
            lastReloaded = new Date();
        }
        reloading = false;
    }

    @Override
    public void run() {
        running = true;
        boolean firstRun = true;
        while (running) {
            load();
            try {

                /* if this is the first run, then set rereadInterval to a smallish
                 * value of the order of one minute or so and then do a re-read of
                 * the config file.
                 *
                 * This is to handle attempting the case when the server we want
                 * to connect to exists within the same servlet container we are
                 * running from and the servlet container is not fully up yet.
                 *
                 * Therefore we will wait a little while and reload everything.
                 * Hopefully the server is now up and running, if not it must have a
                 * problem or we the config_reread_initial_interval value is too
                 * small.
                 *
                 * In any case, we will recheck it at our next scheduled config
                 * file re-read
                 */

                if (firstRun) {
                    rereadInterval = settings.getConfigRereadInitialInterval();
                    firstRun = false;
                } else {
                    rereadInterval = settings.getConfigRereadInterval();
                }

                // sanity check the reread interval - it might be 0 if settings didn't load, or config
                // is broken
                if (rereadInterval == 0) {
                    logger.warn("invalid value (0) for configReloadInitial or configReload in config file (or config file not loaded)");
                    rereadInterval = BROKEN_CONFIG_RELOAD;
                }

                logger.debug("menu (re)load attempt finished, goinging to sleep for " + rereadInterval + "ms");

                Thread.sleep(rereadInterval);
            } catch (InterruptedException e) {
                logger.debug(
                        "Configuration Loader was interrupted during its sleep, probably "
                        + "not important");
            }
        }
    }




        /**
     * Request the thread terminates
     */
    public void stop() {
        logger.debug("requesting stop for configuration loader thread");
        running = false;
    }

    public Date getLastReloaded() {
        return lastReloaded;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public ConfigurationFile getConfigurationFile() {
        return configurationFile;
    }

    @Required
    public void setConfigurationFile(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public boolean isError() {
        return error;
    }

    public boolean isReloading() {
        return reloading;
    }

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    @Required
    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }


}
