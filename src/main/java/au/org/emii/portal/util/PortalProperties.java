package au.org.emii.portal.util;

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Created by a on 22/04/2014.
 */
public class PortalProperties extends Properties {

    private static final Logger LOGGER = Logger.getLogger(PortalProperties.class);

    @Override
    public String getProperty(String key) {
        String property = super.getProperty(key);

        if (property == null) {
            LOGGER.error("****** MISSING PROPERTY in webportal-config.properties: " + key);
        }

        return property;
    }
}
