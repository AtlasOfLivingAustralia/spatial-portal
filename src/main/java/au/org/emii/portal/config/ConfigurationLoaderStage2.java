/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.session.PortalSession;

import java.util.Properties;

/**
 * @author geoff
 */
public interface ConfigurationLoaderStage2 {

    /**
     * Check whether an error occurred while reloading
     *
     * @return
     */
    boolean isError();

    PortalSession load();

    /**
     * Cleanup any resources we are holding (portal document and portal session)
     */
    void cleanup();

    void setProperties(Properties portalDocument);
}
