/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;


import au.org.emii.portal.factory.PortalDocumentFactory;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.web.ApplicationInit;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration loader
 * ~~~~~~~~~~~~~~~~~~~~
 * This class loads the config file file.  Stage 1 is to create and save a spring
 * application context
 *
 * @author geoff
 */
public class ConfigurationLoaderStage1Impl implements ConfigurationLoaderStage1 {

    /**
     * The config file re-read interval is normally read from the
     * config file, but if the config file is broken, we can't
     * get an initial value, so we will use this one which
     * specifies reloading time in ms
     * <p/>
     * 300000 = 5 mins
     */
    public static final int BROKEN_CONFIG_RELOAD = 300000;
    /**
     * Time to keep configuration file in memory - other components wanting
     * to refresh things from config should read this value
     */
    private static int rereadInterval = BROKEN_CONFIG_RELOAD;
    public static final List<Thread> LOADERS = new ArrayList<Thread>();
    /**
     * Logger instance
     */
    private static final Logger LOGGER = Logger.getLogger(ConfigurationLoaderStage1Impl.class);
    private ConfigurationLoaderStage2 stage2 = null;
    private PortalDocumentFactory portalDocumentFactory = null;
    private Settings settings = null;
    /**
     * Request thread shutdown
     */
    private boolean running = false;

    private ServletContext servletContext = null;

    /**
     * Flag to indicate whether the configuration contains error(s) or not
     */
    private boolean error = false;
    private boolean reloading;

    @Required
    public void setStage2(ConfigurationLoaderStage2 stage2) {
        this.stage2 = stage2;
    }

    private void load() {
        // protect against sticky error flag (remember we're a singleton)
        error = false;
        reloading = true;
        Properties portalDocument = portalDocumentFactory.createPortalDocumentInstance();
        if (portalDocument == null) {
            LOGGER.debug("Configuration file missing or invalid - cannot load portal.  See previous message for cause");
        } else {
            stage2.setProperties(portalDocument);

            // Ask stage2 to load the portal from the PortalDocument instance and
            // pass us back the master session that gets created
            PortalSession masterPortalSession = stage2.load();

            // did any errors in stage2 occur? - if so, export them through our
            // own error flag so that consumers can read the error flag from one
            // place (they could get access to stage 2 as a spring bean if they
            // wanted to)... Note that we don't overwrite our own error flag
            // unless an error condition has occured to prevent a faultless
            // stage2 execution hiding an error in stage1 (although I'm not sure
            // how this could happen!
            if (stage2.isError()) {
                error = true;
            }



            /* Now we need to store an application scope variable storing the
             * relevant details from the config file
             *
             *  Because its stored as method scope variables and published, we
             *  don't need to worry about concurrent modification.
             *
             *  Also give a copy of servlet context to PortalSessionAccessor
             *  so that it can be accessed via spring
             */
            servletContext.setAttribute(ApplicationInit.PORTAL_MASTER_SESSION_ATTRIBUTE, masterPortalSession);
            LOGGER.debug("finished building master portalSession");
        }
        reloading = false;
    }

    @Override
    public void run() {
        running = true;
        boolean firstRun = true;
        while (running) {
            try {
                load();
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
                    LOADERS.add(Thread.currentThread());
                    rereadInterval = settings.getConfigRereadInitialInterval();
                    firstRun = false;
                } else {
                    rereadInterval = settings.getConfigRereadInterval();
                }

                // sanity check the reread interval - it might be 0 if settings didn't load, or config
                // is broken
                if (rereadInterval == 0) {
                    LOGGER.warn("invalid value (0) for configReloadInitial or configReload in config file (or config file not loaded)");
                    rereadInterval = BROKEN_CONFIG_RELOAD;
                }

                LOGGER.debug("menu (re)load attempt finished, goinging to sleep for " + rereadInterval + "ms");

                Thread.sleep(rereadInterval);
            } catch (InterruptedException e) {
                LOGGER.debug(
                        "Configuration Loader was interrupted during its sleep, probably "
                                + "not important");
            }
        }
    }


    /**
     * Request the thread terminates
     */
    @Override
    public void stop() {
        LOGGER.debug("requesting stop for configuration loader thread");
        running = false;

        // remove reference to servlet context to allow the server to be brought
        // down cleanly (?)
        servletContext = null;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public boolean isError() {
        return error;
    }

    public void setPortalDocumentFactory(PortalDocumentFactory portalDocumentFactory) {
        this.portalDocumentFactory = portalDocumentFactory;
    }

    public boolean isReloading() {
        return reloading;
    }
}
