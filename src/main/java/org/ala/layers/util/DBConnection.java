package org.ala.layers.util;


import javax.servlet.ServletContextEvent;
import org.springframework.web.context.ContextLoaderListener;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 * Active DB connections.
 *
 * @author Adam
 *
 */
public class DBConnection extends ContextLoaderListener {
    
     /**
     * Log4j instance
     */
    protected static Logger logger = Logger.getLogger("org.ala.layers.util.DBConnection");
    final static int maxConnections = 20;

    //active connection
    static LinkedBlockingQueue<Connection> connections = new LinkedBlockingQueue<Connection>();
    static LinkedBlockingQueue<Object> spaces = new LinkedBlockingQueue<Object>();
    static Thread makeConnections;
    static Object obj = new Object();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //start creating connections
        for(int i=0;i<maxConnections;i++) {
            try {
                spaces.put(obj);
            } catch (InterruptedException ex) {
                logger.error("An error occurred creating database connections.");
                logger.error(ExceptionUtils.getFullStackTrace(ex));
            }
        }

        makeConnections = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        spaces.take();
                        Connection c = newConnection();
                        if(c == null) {
                            spaces.put(obj);
                        } else {
                            connections.put(c);
                        }
                    }
                } catch (Exception e) {
                }
            }
        };

        makeConnections.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {        
        //stop creating new connections
        makeConnections.interrupt();

        //close existing connections
        for(int i=0;i<connections.size();i++) {
            try {
                connections.take().close();
            } catch (Exception ex) {
                logger.error("An error occurred closing database connections.");
                logger.error(ExceptionUtils.getFullStackTrace(ex));
            }
        }
    }

    /**
     * make a new postgres db connection
     * @return
     */
    private Connection newConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection("jdbc:postgresql://ala-devmaps-db.vm.csiro.au:5432/layersdb","postgres","postgres");
        } catch (Exception ex) {
            logger.error("An error occurred creating database connection.");
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }
        return null;
    }
   
    /**
     * execute a query
     *
     * @return
     */
    static public ResultSet query(String query) {
        
        ResultSet r = null;
        try {

            System.out.println("Executing query: " + query);

            Statement s = connections.take().createStatement();
            System.out.println("statement created...");
            r = s.executeQuery(query);
            System.out.println("query executed...");
        } catch (Exception ex) {
            logger.error("An error occurred executing database query.");
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }

        //consume a connection, make a space for another
        try {
            spaces.put(obj);
        } catch (Exception ex) {
            logger.error("An error occurred executing database query.");
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }

        return r;
    }
}
