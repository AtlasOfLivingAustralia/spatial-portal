package org.ala.spatial.web;


import javax.servlet.ServletContextEvent;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.util.TabulationSettings;

import org.springframework.web.context.ContextLoaderListener;

/**
 * Housekeeping class for setting up objects in application scope.
 * 
 * Currently loads and processes the xml configuration file
 * @author geoff
 *
 */
public class ApplicationInit extends ContextLoaderListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        TabulationSettings.load();
        OccurrencesIndex.loadIndexes();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        
    }

}
