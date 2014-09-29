/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import javax.servlet.ServletContext;

/**
 * @author geoff
 */
public interface ConfigurationLoaderStage1 extends Runnable {

    /**
     * Check if errors occurred anywhere within the loading process (stage 1 + 2)
     *
     * @return
     */
    boolean isError();

    /**
     * Maintain a reference to the servlet context within the thread
     *
     * @param servletContext
     */
    void setServletContext(ServletContext servletContext);

    /**
     * Request the reload thread be stopped - normally invoked as part of
     * webapp shutdown
     */
    void stop();

}
