package au.org.emii.portal.web;

import au.org.emii.portal.config.ConfigurationLoader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.test.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Housekeeping class for setting up objects in application scope.
 * 
 * Currently loads and processes the xml configuration file
 * @author geoff
 *
 */
public class ApplicationInit extends ContextLoaderListener {

    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.debug("* APPLICATION init:");

        /* first thing to do is set the servername so all our loggers get access to it
         * if they specify %X{ServerName} in their pattern
         */
        String host;
        try {
            host = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException ex) {
            host = "unresolvable_hostname";
        }
        MDC.put("ServerName", host);


        ServletContext servletContext = sce.getServletContext();

        /* configurationLoader is a daemon thread that runs for the duration
         * of the application - it is set to periodically reload the configuration.
         * We store the both the thread and the runnable in application scope so
         * we can kill the thread cleanly
         */
        ConfigurationLoader configurationLoader = new ConfigurationLoader(servletContext);
        servletContext.setAttribute("configurationLoader", configurationLoader);
        Thread configurationLoaderThread = new Thread(configurationLoader);
        servletContext.setAttribute("configurationLoaderThread", configurationLoaderThread);

        // start the tread running and return control immediately
        configurationLoaderThread.start();

        logger.debug("* APPLICATION INIT: complete");

        super.contextInitialized(sce);

    }

    /**
     * Called by servlet container when shutting down
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.debug("APPLICATION shutdown requested");
        ServletContext servletContext = sce.getServletContext();
        ConfigurationLoader configurationLoader =
                (ConfigurationLoader) servletContext.getAttribute("configurationLoader");
        Thread configurationLoaderThread =
                (Thread) servletContext.getAttribute("configurationLoaderThread");

        /* it's entirely possible that the value hasn't been put in scope yet
         * so protect against NPEs
         */
        if (configurationLoader != null) {
            configurationLoader.stop();
        }
        // interrupt the thread which is likely in sleep state
        configurationLoaderThread.interrupt();

        super.contextDestroyed(sce);
    }
}
