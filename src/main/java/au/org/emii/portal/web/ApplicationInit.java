package au.org.emii.portal.web;

import au.org.emii.portal.config.ConfigurationLoaderStage1;
import au.org.emii.portal.config.PortalNamingImpl;
import au.org.emii.portal.config.ResolveHostNameImpl;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.log4j.Logger;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Housekeeping class for setting up objects in application scope.
 * 
 * Currently loads and processes the xml configuration file
 * @author geoff
 *
 */
public class ApplicationInit extends ContextLoaderListener {

    public static final String CONFIGURATION_LOADER_ATTRIBUTE = "configurationLoader";
    public static final String CONFIGURATION_LOADER_THREAD_ATTRIBUTE = "configurationLoaderThread";
    public static final String PORTAL_MASTER_SESSION_ATTRIBUTE = "masterPortalSession";

    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * setup log4j - since we have no spring at this point, we have to call
     * some setters on it ourself...
     */
    private void initLog4j() {
        Log4jLoader log4jLoader = new Log4jLoader();
        log4jLoader.setPortalNaming(new PortalNamingImpl());
        log4jLoader.setResolveHostname(new ResolveHostNameImpl());
        log4jLoader.load();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // first log message can come from a log4j instance without substituting
        // variables, just so we know we at least got this far...
        logger.debug("* APPLICATION init:");

        // set log4j with app name and host name
        initLog4j();

        // now the spring context gets loaded by superclass...
        super.contextInitialized(sce);
        ServletContext servletContext = sce.getServletContext();
        WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        /* configurationLoader is a daemon thread that runs for the duration
         * of the application - it is set to periodically reload the configuration.
         * We store the both the thread and the runnable in application scope so
         * we can kill the thread cleanly
         */
        ConfigurationLoaderStage1 configurationLoader = context.getBean(ConfigurationLoaderStage1.class);
        configurationLoader.setServletContext(servletContext);
        servletContext.setAttribute(CONFIGURATION_LOADER_ATTRIBUTE, configurationLoader);
        Thread configurationLoaderThread = new Thread(configurationLoader);
        servletContext.setAttribute(CONFIGURATION_LOADER_THREAD_ATTRIBUTE, configurationLoaderThread);

        // start the tread running and return control immediately
        configurationLoaderThread.start();

        logger.debug("* APPLICATION INIT: complete");



    }

    /**
     * Called by servlet container when shutting down
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.debug("APPLICATION shutdown requested");
        ServletContext servletContext = sce.getServletContext();
        ConfigurationLoaderStage1 configurationLoader =
                (ConfigurationLoaderStage1) servletContext.getAttribute(CONFIGURATION_LOADER_ATTRIBUTE);
        Thread configurationLoaderThread =
                (Thread) servletContext.getAttribute(CONFIGURATION_LOADER_THREAD_ATTRIBUTE);

        /* it's entirely possible that the value hasn't been put in scope yet
         * so protect against NPEs
         */
        if (configurationLoader != null) {
            configurationLoader.stop();
        }

        // interrupt the thread which is likely in sleep state
        if (configurationLoaderThread != null) {
            configurationLoaderThread.interrupt();
        }

        super.contextDestroyed(sce);
    }

}
