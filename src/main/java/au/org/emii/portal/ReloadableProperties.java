/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal;

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
public class ReloadableProperties {

    private static Logger logger = Logger.getLogger(ReloadableProperties.class);

    /**
     * FIXME - NEEDS TO USE Boolean instead of boolean? because pass by ref/value for
     * objects/primatives is different.  Make testcase!
     * @param properties
     * @param updating
     * @param lastReloaded
     * @param is
     */
    public static void reloadProperties(Properties properties, boolean updating, Date lastReloaded, InputStream is) {
        synchronized (properties) {
            if (!updating) {
                updating = true;
                try {
                    if (is != null) {
                        properties.clear();
                        properties.load(is);
                        lastReloaded = new Date();
                        logger.debug("properties reloaded");
                    }
                } catch (IOException e) {
                    logger.error("error loading properties", e);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                    }

                    updating = false;
                    properties.notifyAll();
                }
            }
        }
    }

    public static InputStream getInputStream(String filename) {

        InputStream is = null;
        try {
            if (filename != null) {
                is = new FileInputStream(filename);
            } else {
                logger.warn(filename + " not found in classpath");
            }
        } catch (FileNotFoundException e) {
            logger.error("file not found: " + filename);
        }
        return is;
    }

    /**
     * Block execution until safe to read file
     * @param filename
     * @param lastReloaded
     * @param updating
     * @param motd
     */
    public static boolean guardUpdateProperties(String filename, Date lastReloaded, boolean updating, Properties properties) {
        boolean safe = false;
        if (filename != null) {
            File file = new File(filename);

            if (file.exists() && file.canRead()) {
                if (((lastReloaded == null) || (file.lastModified() > lastReloaded.getTime())) && !updating) {
                    // initial load or update
                    //reloadMotd();
                    safe = true;
                } else {
                    synchronized (properties) {
                        while (updating) {
                            try {
                                properties.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }
            } else {
                logger.warn(
                        "Properties file (" + filename + ") does not exist or is not readable");
            }
        }
        return safe;
    }
}
