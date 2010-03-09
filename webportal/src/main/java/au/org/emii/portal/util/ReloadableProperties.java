/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.aspect.CheckNotNull;
import java.util.Properties;

/**
 *
 * @author geoff
 */
public interface ReloadableProperties {

    String getFilename();

    /**
     * Get the properties field - perform an initial load or reread if file
     * modified as required
     * @return properties field or null if it could not be loaded
     */
    Properties getProperties();

    @CheckNotNull
    void setFilename(String filename);

    /**
     * Set properties, update lastReloaded timestamp
     * @param properties
     */
    @CheckNotNull
    void setProperties(Properties properties);

}
