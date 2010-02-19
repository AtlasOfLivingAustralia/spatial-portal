package au.org.emii.portal;

import au.org.emii.portal.config.Config;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Message Of The Day support
 * 
 * @author geoff
 * 
 */
public class MOTD {

    private static Logger logger = Logger.getLogger(MOTD.class);
    /**
     * message of the day language pack and configuration
     *
     * @param values
     */
    private static final Properties motd = new Properties();
    /**
     * Date we last reloaded the motd
     */
    private static Date lastReloaded = null;
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
    private static final URL MOTD_URL = MOTD.class.getResource(MOTD_FILE);
    /**
     * Flag to indicate whether the MOTD is currently being reread from the
     * properties file
     */
    private static boolean updating = false;

    /**
     * Return a value from the motd language pack
     *
     * @param key
     * @return
     */

    public static String getMotd(String key) {
        //updateMotd();

        if (ReloadableProperties.guardUpdateProperties(getMotdFilename(), lastReloaded, updating, motd)) {
            ReloadableProperties.reloadProperties(motd, updating, lastReloaded, ReloadableProperties.getInputStream(getMotdFilename()));
        }

        String property = motd.getProperty(key);
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
    public static boolean isMotdEnabled() {
        return Boolean.parseBoolean(getMotd("enable_motd"));
    }

    private static String getMotdFilename() {
        return (MOTD_URL != null) ? MOTD_URL.getFile() : null;
    }

    /*
    private static InputStream getMotd() {

        InputStream is = null;
        String filename = getMotdFilename();
        try {
            if (filename != null) {
                is = new FileInputStream(filename);
            } else {
                logger.warn(MOTD_FILE + " not found in classpath");
            }
        } catch (FileNotFoundException e) {
            logger.error("file not found: " + filename);
        }
        return is;
    }
     * 
     */

    /*
    private static void reloadMotd() {
        synchronized (motd) {
            if (!updating) {
                updating = true;
                InputStream is = null;
                try {
                    is = getMotd();
                    if (is != null) {
                        motd.clear();
                        motd.load(is);
                        lastReloaded = new Date();
                        logger.debug("motd reloaded");
                    }
                } catch (IOException e) {
                    logger.error("error loading MOTD", e);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                    }

                    updating = false;
                    motd.notifyAll();
                }
            }
        }
    }
     * 
     */

    /*
    private static void updateMotd() {
        String filename = getMotdFilename();
        if (filename != null) {
            File file = new File(filename);

            if (file.exists() && file.canRead()) {
                if (((lastReloaded == null) || (file.lastModified() > lastReloaded.getTime())) && !updating) {
                    // initial load or update
                    reloadMotd();
                } else {
                    synchronized (motd) {
                        while (updating) {
                            try {
                                motd.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }
            } else {
                logger.warn(
                        "MOTD file (" + filename + ") does not exist or is not readable");
            }
        }
    }
*/

    /**
     * Save the passed in parameters to the MOTD file
     * @param enabled whether this message is enabled or not
     * @param title title to display for motd
     * @param message message to display for motd
     * @return true if save was successful otherwise false
     */
    public static boolean updateMotd(boolean enabled, String title, String message, String portalUsername) {
        boolean saved = true;
        Properties props = new Properties();
        props.put("enable_motd", String.valueOf(enabled));
        props.put("title", title);
        props.put("message", message);
        try {
            props.store(new FileOutputStream(MOTD_URL.getFile()),  Config.getCompoundLang("properties_file_update_comment", new Object[] {portalUsername}));
        } catch (IOException ex) {
            saved = false;
            logger.error("error saving updated MOTD", ex);
        }
        return saved;
    }
}
