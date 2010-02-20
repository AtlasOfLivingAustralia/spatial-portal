/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.web.Log4jLoader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extend PortalNaming to add support for updating the portal name, which
 * involves updating the .properties file and asking log4j to reload its
 * configuration
 * @author geoff
 */
public class PortalNamingUpdaterImpl implements PortalNamingUpdater {

    private Logger logger = Logger.getLogger(getClass());

    /**
     * Log4j loader - spring autowired
     */
    private Log4jLoader log4jLoader = null;

        /**
     * Language pack - used to build the properties file comment.  Can either be
     * spring autowired or manually set if using outside spring at portal boot
     * time
     */
    private LanguagePack languagePack = null;

    @Autowired
    private PropertiesWriter propertiesWriter = null;

    /**
     * Portal Naming support - so we can update the properties that log4j loader
     * will reread - spring injected
     */
    @Resource(name="portalNaming")
    private ReloadableProperties portalNaming = null;

    @Override
    public boolean updatePortalName(String portalName, String portalUsername) {
        boolean saved = false;
        Properties props = new Properties();
        props.put("portal_name", portalName);

        saved = propertiesWriter.write(PortalNamingImpl.PORTAL_PROPERTIES_URL.getFile(), props, portalUsername);
        if (saved) {
            // update the properties the portalNaming class is holding using the
            // ReloadableProperties interface...
            portalNaming.setProperties(props);
            // then update log4j with the new name
            log4jLoader.load();
            
        }
        return saved;
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public Log4jLoader getLog4jLoader() {
        return log4jLoader;
    }

    public void setLog4jLoader(Log4jLoader log4jLoader) {
        this.log4jLoader = log4jLoader;
    }

    public ReloadableProperties getPortalNaming() {
        return portalNaming;
    }

    public void setPortalNaming(ReloadableProperties portalNaming) {
        this.portalNaming = portalNaming;
    }

    public PropertiesWriter getPropertiesWriter() {
        return propertiesWriter;
    }

    public void setPropertiesWriter(PropertiesWriter propertiesWriter) {
        this.propertiesWriter = propertiesWriter;
    }

    
    
}
