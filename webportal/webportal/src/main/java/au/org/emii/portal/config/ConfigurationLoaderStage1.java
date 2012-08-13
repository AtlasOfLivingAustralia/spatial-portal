/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import java.util.ArrayList;
import java.util.Date;
import javax.servlet.ServletContext;

/**
 *
 * @author geoff
 */
public interface ConfigurationLoaderStage1 extends Runnable {
    public static final ArrayList<Thread> loaders = new ArrayList<Thread>();

    /**
     * The config file re-read interval is normally read from the
     * config file, but if the config file is broken, we can't
     * get an initial value, so we will use this one which
     * specifies reloading time in ms
     *
     * 300000 = 5 mins
     */
    public static final int BROKEN_CONFIG_RELOAD = 300000;

    /**
     * Get the date the configuration (not the server!) was last reloaded.  This
     * is computed by the loader so there is no corresponding setter
     * @return
     */
    public Date getLastReloaded();

    /**
     * Get the stage 2 loader - not currently used, added to allow future
     * novel uses
     * @return
     */
    public ConfigurationLoaderStage2 getStage2();

    /**
     * Check if errors occurred anywhere within the loading process (stage 1 + 2)
     * @return
     */
    public boolean isError();

    /*
     * Check if the configuration is currently reloading
     */
    public boolean isReloading();

    /**
     * Set the stage2 loader - normally called by spring
     * @param stage2
     */
    public void setStage2(ConfigurationLoaderStage2 stage2);


    /**
     * Get the servlet context - not actually used by anything at the moment
     * but we have a setter, so a getter has been provided for easy future
     * expansion
     * @return
     */
    public ServletContext getServletContext();

    /**
     * Maintain a reference to the servlet context within the thread
     * @param servletContext
     */
    public void setServletContext(ServletContext servletContext);

    /**
     * Request the reload thread be stopped - normally invoked as part of
     * webapp shutdown
     */
    public void stop();

}
