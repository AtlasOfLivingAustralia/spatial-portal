/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test;

import org.apache.log4j.Logger;

import java.net.URL;

/**
 * @author geoff
 */
public class FilenameResolver {

    private String usersProperties = null;
    private Logger logger = Logger.getLogger(getClass());

    public String getUsersProperties() {
        return usersProperties;
    }

    public void setUsersProperties(String usersProperties) {
        this.usersProperties = usersProperties;
    }

    public String resolveResource(String filename) {
        String fqFilename = null;
        URL url = getClass().getResource(filename);
        if (url == null) {
            logger.error("file not found on classpath: " + filename);
        } else {
            fqFilename = url.getFile();
        }
        return fqFilename;
    }

    public String resolveUsersProperties() {
        return resolveResource(usersProperties);
    }

}
