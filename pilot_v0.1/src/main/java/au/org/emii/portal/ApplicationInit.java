package au.org.emii.portal;

import java.net.InetAddress;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zk.ui.util.WebAppInit;

/**
 * Housekeeping class for setting up objects in application scope.
 * 
 * Currently loads and processes the xml configuration file
 * @author geoff
 *
 */
public class ApplicationInit implements WebAppInit, WebAppCleanup {

	/**
	 * Logger instance
	 */
	private Logger logger = Logger.getLogger(this.getClass());
	
	/**
	 * Called by ZK when starting the application
	 */
	public void init(WebApp webApp) throws Exception {
		/* first thing to do is set the servername so all our loggers get access to it
		 * if they specify %X{ServerName} in their pattern 
		 */
		String host = InetAddress.getLocalHost().toString();
		MDC.put("ServerName", host);
		
		logger.debug("* APPLICATION init:");
		
		ServletContext servletContext = (ServletContext) webApp.getNativeContext();
		
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
	}

	/**
	 * Called by ZK when shutting down
	 */
	public void cleanup(WebApp webapp) throws Exception {
		logger.debug("APPLICATION shutdown requested");
		ServletContext servletContext = (ServletContext) webapp.getNativeContext();
		ConfigurationLoader configurationLoader = 
			(ConfigurationLoader) 
				servletContext.getAttribute("configurationLoader");
		Thread configurationLoaderThread = 
			(Thread) 
				servletContext.getAttribute("configurationLoaderThread");
		
		/* it's entirely possible that the value hasn't been put in scope yet
		 * so protect against NPEs
		 */ 
		if (configurationLoader != null) {
			configurationLoader.stop();
		}
		// interrupt the thread which is likely in sleep state
		configurationLoaderThread.interrupt();
	}

}
