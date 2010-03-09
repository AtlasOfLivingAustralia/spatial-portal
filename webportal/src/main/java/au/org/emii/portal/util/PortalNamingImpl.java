/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.settings.Settings;
import java.io.File;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;


/**
 * Class to handle loading and changing the current portal name from file.
 *
 * Required setters (If not using spring for injection)
 *    + setLanguagePack() - for writing comments
 *
 * @author geoff
 */
public class PortalNamingImpl extends ReloadablePropertiesImpl implements PortalNaming, InitializingBean {

    /**
     * Name of they key in the properties file that holds the portal name
     */
    private final static String KEY_PORTAL_NAME = "portal_name";

    /**
     * Filename for portal properties file within WEB-INF/classes
     */
    private final static String PORTAL_PROPERTIES_FILE = "portal.properties";

    private Settings settings  = null;

    /**
     * Set the properties filename
     */
    public PortalNamingImpl() {
        
    }

    @Override
    public String getPortalName() {
        return getProperties().getProperty(KEY_PORTAL_NAME);
    }

    private String getPortalPropertiesFilename() {
        return settings.getConfigPath() + File.separator + PORTAL_PROPERTIES_FILE;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(settings, "settings property must be set");
        setFilename(getPortalPropertiesFilename());
    }

    
}
