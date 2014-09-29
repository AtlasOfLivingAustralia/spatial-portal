package au.org.emii.portal.web;

import au.org.emii.portal.config.ConfigurationLoaderStage1;
import org.apache.log4j.Logger;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.util.resource.Labels;
import zk.extra.BiocacheLabelLocator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * Housekeeping class for setting up objects in application scope.
 * <p/>
 * Currently loads and processes the xml configuration file
 *
 * @author geoff
 *         <p/>
 *         also initialises the webapp from ZK point of view
 */
public class ApplicationInit extends ContextLoaderListener {

    public static final String CONFIGURATION_LOADER_ATTRIBUTE = "configurationLoader";
    public static final String CONFIGURATION_LOADER_THREAD_ATTRIBUTE = "configurationLoaderThread";
    public static final String PORTAL_MASTER_SESSION_ATTRIBUTE = "masterPortalSession";

    /**
     * Logger instance
     */
    private static final Logger LOGGER = Logger.getLogger(ApplicationInit.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // first log message can come from a log4j instance without substituting
        // variables, just so we know we at least got this far...
        LOGGER.debug("================[WEB PORTAL APPLICATION INIT]================");


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

        // set the name for debugging purposes.  We tostring the thread object
        // so that the name will contain the memory address so we can distinguish
        // between diferent instances of the same thread should this happen.
        configurationLoaderThread.setName("ConfigurationLoader-instance-" + configurationLoaderThread.toString());
        servletContext.setAttribute(CONFIGURATION_LOADER_THREAD_ATTRIBUTE, configurationLoaderThread);

        // start the tread running and return control immediately
        configurationLoaderThread.start();

        //NC 2013-11-26: initialise the ZK Labels to include biocache WS i18n version. 
        LOGGER.debug("REGISTERING Biocache Labeller...");
        Labels.register(new BiocacheLabelLocator());
        LOGGER.debug("* APPLICATION INIT: complete");


    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.debug("APPLICATION shutdown requested");
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

        // now remove the attributes from servlet session...
        servletContext.removeAttribute(CONFIGURATION_LOADER_ATTRIBUTE);
        servletContext.removeAttribute(CONFIGURATION_LOADER_THREAD_ATTRIBUTE);

        super.contextDestroyed(sce);
    }

}
