/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.util;

import au.org.emii.portal.aspect.CheckNotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author geoff
 */
public class ReloadablePropertiesImpl implements ReloadableProperties {

    protected Logger logger = Logger.getLogger(getClass());
    private String filename = null;
    private Properties properties = null;

    /**
     * Date of last reload or null if properties have never been reloaded
     */
    protected Date lastReloaded = null;

    /**
     * Get the properties field - perform an initial load or reread if file
     * modified as required
     * @return properties field or null if it could not be loaded
     */
    @Override
    public Properties getProperties() {
        if (updateNeeded()) {
            reloadPropertiesNow();
        }
        return properties;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @CheckNotNull
    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }


    /**
     * Reload properties immediately
     */
    private void reloadPropertiesNow() {
        InputStream is = null;
        try {
            is = new FileInputStream(filename);
            if (is != null) {
                Properties newProperties = new Properties();
                newProperties.load(is);
                lastReloaded = new Date();
                properties = newProperties;
                logger.debug("properties reloaded");
            }
        } catch (FileNotFoundException e) {
            logger.error("file not found: " + filename);
        } catch (IOException e) {
            logger.error("error loading properties", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            // discard error closing input stream
            catch (IOException e) {}
        }
    }

    /**
     * Block execution until safe to read file
     * @param filename
     * @param lastReloaded
     * @param updating
     * @param motd
     */
    protected boolean updateNeeded() {
        boolean updateNeeded = false;
        if (filename == null) {
            logger.error("Internal error inspecting properties file - no filename specified", new NullPointerException());
        } else {
            File file = new File(filename);
            if (file.exists() && file.canRead()) {
                if ((lastReloaded == null) || (file.lastModified() > lastReloaded.getTime())) {
                    // initial load or update
                    updateNeeded = true;
                }
            } else {
                logger.error("properties file not found or not readable: " + filename);
            }
        }
        return updateNeeded;
    }

    /**
     * Set properties, update lastReloaded timestamp
     * @param properties
     */
    @CheckNotNull
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
        lastReloaded = new Date();
    }

    
}
