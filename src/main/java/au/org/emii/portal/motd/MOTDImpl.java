package au.org.emii.portal.motd;

import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.util.ReloadablePropertiesImpl;
import au.org.emii.portal.util.PropertiesWriter;
import java.io.File;
import java.util.Properties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.Assert;

/**
 * Message Of The Day support
 * 
 * @author geoff
 * 
 */
public class MOTDImpl extends ReloadablePropertiesImpl implements MOTD, InitializingBean  {

    /**
     * Name of file within the config dir
     */
    private static final String MOTD_FILE = "motd.properties";

    private Settings settings = null;

    private PropertiesWriter propertiesWriter = null;




    /**
     * Return a value from the motd language pack
     *
     * @param key
     * @return
     */

    @Override
    public String getMotd(String key) {
        String property = getProperties().getProperty(key);
        if (property == null) {
            // key was not found - all keys are required so log an error
            logger.warn(
                    "MOTD key '" + key
                    + "' not found classpath properties file: " + MOTD_FILE);
        }
        return property;
    }

    /**
     * Check if the motd has been enabled
     *
     * @return
     */
    @Override
    public boolean isMotdEnabled() {
        return Boolean.parseBoolean(getMotd("enable_motd"));
    }

    

    /**
     * Save the passed in parameters to the MOTD file
     * @param enabled whether this message is enabled or not
     * @param title title to display for motd
     * @param message message to display for motd
     * @return true if save was successful otherwise false
     */
    @Override
    public boolean updateMotd(boolean enabled, String title, String message, String portalUsername) {      
        Properties props = new Properties();
        props.put("enable_motd", String.valueOf(enabled));
        props.put("title", title);
        props.put("message", message);
        return propertiesWriter.write(getMotdFilename(), props, portalUsername);
    }

    public PropertiesWriter getPropertiesWriter() {
        return propertiesWriter;
    }

    @Required
    public void setPropertiesWriter(PropertiesWriter propertiesWriter) {
        this.propertiesWriter = propertiesWriter;
    }

    public Settings getSettings() {
        return settings;
    }

    @Required
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    private String getMotdFilename() {
        return settings.getConfigPath() + File.separator + MOTD_FILE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(settings);
        Assert.notNull(propertiesWriter);
        setFilename(getMotdFilename());
    }





}
