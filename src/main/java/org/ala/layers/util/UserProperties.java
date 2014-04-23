package org.ala.layers.util;

import org.ala.layers.client.Client;
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.web.IntersectService;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class UserProperties {

    protected Logger logger = Logger.getLogger(this.getClass());

    private final String USER_PROPERTIES = "/data/layers-service/config/layers-service-config.properties";

    private Properties userProperties = null;

    /**
     * @return null if successful or error as String
     */
    String initUserProperties() {
        String error = null;
        userProperties = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(USER_PROPERTIES);
            if (is != null) {
                userProperties.load(is);
            } else {
                logger.error("failed to load " + USER_PROPERTIES);
                error = "failed to load " + USER_PROPERTIES;
            }
        } catch (IOException e) {
            logger.error("failed to load " + USER_PROPERTIES, e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            error = "failed to load " + USER_PROPERTIES + "\n" + sw.getBuffer().toString();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("failed to close " + USER_PROPERTIES, e);
                }
            }
        }

        //init layers-store pre-loaded shape files
        if (userProperties.containsKey("layers-store.PRELOADED_SHAPE_FILES")) {
            //Client.getLayerIntersectDao().getConfig().addToShapeFileCache(userProperties.getProperty("layers-store.PRELOADED_SHAPE_FILES"));
            IntersectConfig.setPreloadedShapeFiles(userProperties.getProperty("layers-store.PRELOADED_SHAPE_FILES"));
        }
        return error;
    }

    public Properties getProperties() {
        if (userProperties == null)
            initUserProperties();
        return userProperties;
    }
}
