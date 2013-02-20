package org.ala.layers.util;

import org.ala.layers.web.IntersectService;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class UserProperties {

    protected Logger logger = Logger.getLogger(this.getClass());

    private final String USER_PROPERTIES = "user.properties";

    private Properties userProperties = null;
    /**
     *
     * @return null if successful or error as String
     */
    String initUserProperties() {
        String error = null;
        userProperties = new Properties();
        try {
            InputStream is = IntersectService.class.getResourceAsStream("/" + USER_PROPERTIES);
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
        }
        return error;
    }

    public Properties getProperties(){
        if(userProperties ==null)
            initUserProperties();
        return userProperties;
    }
}
