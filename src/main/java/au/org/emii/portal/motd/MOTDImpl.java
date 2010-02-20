package au.org.emii.portal.motd;

import au.org.emii.portal.config.ReloadablePropertiesImpl;
import au.org.emii.portal.config.PropertiesWriter;
import java.net.URL;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Message Of The Day support
 * 
 * @author geoff
 * 
 */
public class MOTDImpl extends ReloadablePropertiesImpl implements MOTD {

    /**
     * For the moment the MOTD properties filename is hardcoded to be within the
     * classpath, eg .../webportal/WEB-INF/classes/motd.properties on a
     * deployment.  I was about to change this be an external filename but this
     * would require an additional JVM launch parameter due to different paths
     * on windows/unix.  For the moment, I'm going to keep this defective setup
     * since MOTD implementation will likely change in v2 anyway
     */
    private static final String MOTD_FILE = "/motd.properties";
    /**
     * Use the class loader to read the MOTD_FILE from the classpath - this
     * resolves it to a full filesystem path
     */
    private static final URL MOTD_URL = MOTDImpl.class.getResource(MOTD_FILE);

    @Autowired
    private PropertiesWriter propertiesWriter = null;

    public MOTDImpl() {
        setFilename(MOTD_URL.getFile());
    }



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
        return propertiesWriter.write(MOTD_URL.getFile(), props, portalUsername);
    }

    public PropertiesWriter getPropertiesWriter() {
        return propertiesWriter;
    }

    public void setPropertiesWriter(PropertiesWriter propertiesWriter) {
        this.propertiesWriter = propertiesWriter;
    }


}
