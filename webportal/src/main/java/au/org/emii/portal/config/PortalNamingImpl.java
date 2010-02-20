/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.web.Log4jLoader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;


/**
 * Class to handle loading and changing the current portal name from file.
 *
 * Required setters (If not using spring for injection)
 *    + setLanguagePack() - for writing comments
 *
 * @author geoff
 */
public class PortalNamingImpl extends ReloadablePropertiesImpl implements PortalNaming {

    /**
     * Name of they key in the properties file that holds the portal name
     */
    private final static String KEY_PORTAL_NAME = "portal_name";

    /**
     * Filename for portal properties file within WEB-INF/classes
     */
    private final static String PORTAL_PROPERTIES_FILE = "/portal.properties";

    /**
     * Fully qualified reference to PORTAL_PROPERTIES_FILE - used for IO
     */
    protected final static URL PORTAL_PROPERTIES_URL = PortalNamingImpl.class.getResource(PORTAL_PROPERTIES_FILE);

    /**
     * Set the properties filename
     */
    public PortalNamingImpl() {
        setFilename(PORTAL_PROPERTIES_URL.getFile());
    }

    @Override
    public String getPortalName() {
        return getProperties().getProperty(KEY_PORTAL_NAME);
    }

    
}
