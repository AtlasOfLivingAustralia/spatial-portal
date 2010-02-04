/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.authentication;

import au.org.emii.portal.PortalSession;
import au.org.emii.portal.ReloadableProperties;
import au.org.emii.portal.Validate;
import au.org.emii.portal.config.Config;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.webservice.UserManagementWebService;
import com.sun.jersey.api.client.Client;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

/**
 *
 * Login service reading from properties file backend - only used for the admin
 * console at the moment
 *
 * Data is stored as:
 *
 * username=password
 *
 * eg
 *
 * administrator=topsecret1
 *
 * @author geoff
 */
public class LoginServicePropertiesImpl implements LoginService {

    private final static Properties users = new Properties();
    private final static String USER_FILENAME = "/users.properties";
    private final static URL USER_URL = LoginServicePropertiesImpl.class.getResource(USER_FILENAME);
    private Logger logger = Logger.getLogger(this.getClass());
    /**
     * Date of last reload of properties file
     */
    private Date lastReloaded = null;
    /**
     * Flag to indicatate if we are reloading;
     */
    private boolean updating = false;
    /**
     * String to delimit password hash from salt
     */
    private String SALT_SEPARATOR = "\\:";
    /**
     * Index of the hash in the array returned from hashWithSalt()
     */
    private final static int HASH = 0;
    /**
     * Index of the salt in the array returned from hashWithSalt()
     */
    private final static int SALT = 1;

    /**
     * Load the properties from properties file
     */
    public LoginServicePropertiesImpl() {
        updateProperties();
    }

    /**
     * Update the users Property instance if required
     */
    private void updateProperties() {
        if (ReloadableProperties.guardUpdateProperties(USER_URL.getFile(), lastReloaded, updating, users)) {
            ReloadableProperties.reloadProperties(users, updating, lastReloaded, ReloadableProperties.getInputStream(USER_URL.getFile()));
        }
    }

    /**
     * Hash cleartext using SHA1 and add random salt.  Return the hashed password
     * and salt as an array of String[]
     * @param cleartext cleartext password - this is what the user typed
     * @return Hex string or null if algorithm is not present or cleartext is null
     */
    public String[] hashWithSalt(String cleartext) {
        String hash = null;

        // generate 2 character random string for salt.  Append it to cleartext
        // before hashing
        String salt = RandomStringUtils.randomAscii(2);
        if (cleartext != null) {
            // use apache commons hex encoder to get a sha1 string back...
            hash = hash(cleartext + salt);
        } else {
            logger.debug("hash request to return null because cleartext was null");
        }
        return new String[]{hash, salt};
    }

    public String hash(String cleartext) {
        String hash = null;

        if (cleartext != null) {
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                byte[] digest = sha1.digest(cleartext.getBytes());

                // use apache commons hex encoder to get a sha1 string back...
                hash = new String(Hex.encodeHex(digest));

                logger.debug(String.format("sha1 hash for '%s' is '%s'", cleartext, hash));

            } catch (NoSuchAlgorithmException ex) {
                logger.error("Unable to use SHA1 algorithm to hash passwords - your JRE is broken and no one can login to portal admin console");
            }
        } else {
            logger.debug("hash request to return null because cleartext was null");
        }
        return hash;
    }

    /**
     * Reread properties file if required, then try to login the passed in username
     * and password against the values from properties.  Note - salt is stored in
     * the properties file so doesn't get passed in here
     * @param service interface requirement - not currently used
     * @param session session to mark as logged in on success
     * @param username username
     * @param password password (cleartext)
     * @return
     */
    @Override
    public int login(String service, PortalSession session, String username, String password) {
        int result;
        updateProperties();
        if (users.size() == 0) {
            result = LoginService.FAIL_UNKNOWN;
            logger.error(String.format(
                    "There are no users enabled in '%s'.  You can create an admin user by adding the line "
                    + "'admin=8e5b37ce5556ee82922cd864ae031ad456b9c8f6\\:AA' to this file (without the quotes) "
                    + "which will allow you to login with username 'admin' and password 'letmein'", USER_URL.getFile()));
        } else if (Validate.empty(username) || Validate.empty(password)) {
            // refuse to process null or empty username/passwords
            result = LoginService.FAIL_INVALID;
        } else {
            String passwordAndSalt = users.getProperty(username);
            if ((passwordAndSalt != null) && (password != null)) {
                String[] passwordAndSaltSplit = passwordAndSalt.split(SALT_SEPARATOR);

                // check there are 2 entries (password and salt - otherwise
                // skip this entry and inform admin of broken entry
                if (passwordAndSaltSplit.length == 2) {
                    String passwordFromFile = passwordAndSaltSplit[HASH];
                    String salt = passwordAndSaltSplit[SALT];

                    // check hashed passwords match
                    if (passwordFromFile.equals(hash(password + salt))) {
                        // user found with valid entry, password matches
                        result = LoginService.SUCCESS;

                        // set user to be an admin
                        session.getPortalUser().setType(PortalUser.USER_ADMIN);

                        // store the username too
                        session.getPortalUser().setUsername(username);
                    } else {
                        // wrong password
                        result = LoginService.FAIL_INVALID;
                    }
                } else {
                    logger.error(String.format("Properties file entry for user %s is invalid - login disallowed for this user", username));
                    result = LoginService.FAIL_UNKNOWN;
                }
            } else {
                // user not found in props file or password null
                result = LoginService.FAIL_INVALID;
            }
        }
        return result;
    }

    @Override
    public int login(PortalSession session, String username, String password) {
        return login(null, session, username, password);


    }

    @Override
    public int systemLogin(Client client, String service, String username, String password) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public int systemLogin(Client client, String username, String password) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public Map<String, UserManagementWebService> getUserManagementServices() {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public void setUserManagementServices(Map<String, UserManagementWebService> services) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public void setDefaultUserManagementService(UserManagementWebService service) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public UserManagementWebService getDefaultUserManagementService() {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public void setDefaultUserManagementServiceId(String service) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public String getDefaultUserManagementServiceId() {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    @Override
    public UserManagementWebService getUserManagementWebServiceById(String id) {
        throw new UnsupportedOperationException("Not supported yet.");


    }

    public Properties getUsers() {
        return users;


    }

    /**
     * Save passed in properties to the file specified in USER_URL.  If successful,
     * subsequent login requests will use the new data
     * @param properties Properties instance to serialise to file - completely replaces existing content
     * @return true on success, otherwise false
     */
    public boolean updateProperties(Properties properties, String portalUsername) {
        boolean saved = true;


        try {
            properties.store(
                    new FileOutputStream(USER_URL.getFile()),
                    Config.getCompoundLang("properties_file_update_comment", new Object[] {portalUsername}));


        } catch (IOException ex) {
            saved = false;
            logger.error("error saving updated " + USER_FILENAME, ex);


        }
        return saved;


    }

    /**
     * format the password and hash array obtained from hashWithSalt() to the format
     * used in the .properties file
     * @param passwordAndHash
     * @return
     */
    public String formatForProperties(String[] passwordAndHash) {
        return passwordAndHash[HASH] + SALT_SEPARATOR.replace("\\", "") + passwordAndHash[SALT];

    }
}
