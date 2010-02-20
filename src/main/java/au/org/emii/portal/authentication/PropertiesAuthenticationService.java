/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import java.util.Properties;

/**
 *
 * @author geoff
 */
public interface PropertiesAuthenticationService extends LoginService {

    /**
     * format the password and hash array obtained from hashWithSalt() to the format
     * used in the .properties file
     * @param passwordAndHash
     * @return
     */
    public String formatForProperties(String[] passwordAndHash);

    public Properties getUsers();

    /**
     * Hash cleartext using SHA1 and add random salt.  Return the hashed password
     * and salt as an array of String[]
     * @param cleartext cleartext password - this is what the user typed
     * @return Hex string or null if algorithm is not present or cleartext is null
     */
    public String[] hashWithSalt(String cleartext);

    /**
     * Save passed in properties to the file specified in USER_URL.  If successful,
     * subsequent login requests will use the new data
     * @param properties Properties instance to serialise to file - completely replaces existing content
     * @return true on success, otherwise false
     */
    public boolean updateProperties(Properties properties, String portalUsername);

}
