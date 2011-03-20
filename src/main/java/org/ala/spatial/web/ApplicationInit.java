package org.ala.spatial.web;


import javax.servlet.ServletContextEvent;
import org.ala.spatial.analysis.index.DatasetMonitor;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.ala.spatial.analysis.service.ShapeIntersectionService;
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
    //DatasetMonitor dm = null;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        TabulationSettings.load();
        OccurrencesCollection.init();

        ShapeIntersectionService.start();

        DatasetMonitor dm = new DatasetMonitor();
        dm.start();

        //OccurrencesIndex.loadIndexes();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //if(dm != null) {
        //    dm.end();
        //}
    }

}
