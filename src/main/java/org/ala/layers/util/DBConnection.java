/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.layers.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import org.springframework.web.context.ContextLoaderListener;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Active DB connections.
 *
 * @author Adam
 *
 */
public class DBConnection extends ContextLoaderListener {
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
                Logger.getLogger(DBConnection.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(DBConnection.class.getName()).log(Level.SEVERE, null, ex);
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
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/layersdb","postgres","postgres");
        } catch (Exception ex) {
            Logger.getLogger(DBConnection.class.getName()).log(Level.SEVERE, null, ex);
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
            Statement s = connections.take().createStatement();
            r = s.executeQuery(query);
        } catch (Exception ex) {
            Logger.getLogger(DBConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        //consume a connection, make a space for another
        try {
            spaces.put(obj);
        } catch (Exception ex) {
            Logger.getLogger(DBConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        return r;
    }
}