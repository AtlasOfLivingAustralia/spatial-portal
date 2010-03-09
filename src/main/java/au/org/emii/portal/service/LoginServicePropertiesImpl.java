/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.service;

import au.org.emii.portal.service.PropertiesAuthenticationService;
import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.util.ReloadablePropertiesImpl;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.util.PropertiesWriter;
import au.org.emii.portal.session.PortalUser;
import com.sun.jersey.api.client.Client;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Required;

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
public class LoginServicePropertiesImpl extends ReloadablePropertiesImpl implements PropertiesAuthenticationService {

    private PropertiesWriter propertiesWriter = null;
    /**
     * Date of last reload of properties file

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
     * Hash cleartext using SHA1 and add random salt.  Return the hashed password
     * and salt as an array of String[]
     * @param cleartext cleartext password - this is what the user typed
     * @return Hex string or null if algorithm is not present or cleartext is null
     */
    @Override
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

    private String hash(String cleartext) {
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
    public int login(PortalSession session, String username, String password) {
        int result;
        Properties properties = getProperties();
        if (properties == null) {
            logger.error("No properties were loaded from " + getFilename());
            result = LoginService.FAIL_UNKNOWN;
        } else {
            if (properties.size() == 0) {
                result = LoginService.FAIL_UNKNOWN;
                logger.error(String.format(
                        "There are no users enabled in '%s'.  You can create an admin user by adding the line "
                        + "'admin=8e5b37ce5556ee82922cd864ae031ad456b9c8f6\\:AA' to this file (without the quotes) "
                        + "which will allow you to login with username 'admin' and password 'letmein'", getFilename()));
            } else if (Validate.empty(username) || Validate.empty(password)) {
                // refuse to process null or empty username/passwords
                result = LoginService.FAIL_INVALID;
            } else {
                String passwordAndSalt = properties.getProperty(username);
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
        }
        return result;
    }


    @Override
    public int systemLogin(Client client, String username, String password) {
        throw new UnsupportedOperationException("Not supported");
    }


    @Override
    public Properties getUsers() {
        return getProperties();
    }

    /**
     * Save passed in properties to the file specified in USER_URL.  If successful,
     * subsequent login requests will use the new data
     * @param properties Properties instance to serialise to file - completely replaces existing content
     * @return true on success, otherwise false
     */
    @Override
    public boolean updateProperties(Properties properties, String portalUsername) {
        return propertiesWriter.write(getFilename(), properties, portalUsername);
    }

    /**
     * format the password and hash array obtained from hashWithSalt() to the format
     * used in the .properties file
     * @param passwordAndHash
     * @return
     */
    @Override
    public String formatForProperties(String[] passwordAndHash) {
        return passwordAndHash[HASH] + SALT_SEPARATOR.replace("\\", "") + passwordAndHash[SALT];

    }

    public PropertiesWriter getPropertiesWriter() {
        return propertiesWriter;
    }

    @Required
    public void setPropertiesWriter(PropertiesWriter propertiesWriter) {
        this.propertiesWriter = propertiesWriter;
    }



}
