/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.authentication.PropertiesAuthenticationService;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.test.AbstractTester;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the admin login service (uses properties files)
 * @author geoff
 */
public class AdminLoginTests extends AbstractTester {

    private Properties users = new Properties();

    private final static String USERS_FILE = "/users.properties";
    private final static URL USERS_URL = AdminLoginTests.class.getResource(USERS_FILE);

    /**
     * Name of known good user
     */
    private String GOOD_USER = "GOODUSER";

    /**
     * Password for known good user - will be looked up from extra entry
     * in properties file
     */
    private String goodPassword = null;
    private final static String BAD_USER = "BAD_USER";
    private final static String BAD_PASSWORD = "BAD_PASSWORD";
    private final static String NEW_USER = "newuser";
    private final static String NEW_PASSWORD = "newpassword";

    @Resource(name="adminConsoleLoginService")
    private PropertiesAuthenticationService loginService = null;

    public PropertiesAuthenticationService getLoginService() {
        return loginService;
    }

    public AdminLoginTests() {
        try {
            users.load(new FileInputStream(new File(USERS_URL.getFile())));
        } catch (IOException ex) {
            logger.error("cannot load poperties: " + ex.getMessage());
        }
    }

    public void setLoginService(PropertiesAuthenticationService loginService) {
        this.loginService = loginService;
    }

    @Before
    public void setup() {
        // password is mapped to username_Password for testing...
        goodPassword = users.getProperty(GOOD_USER + "_PASSWORD");

        logger.info("type=" + loginService.getClass().getName());


        logger.info(String.format("using usernamee='%s', password='%s'", GOOD_USER, goodPassword));
    }

    @Test
    public void testLoginOk() {
        PortalSession session = new PortalSession();
        assertEquals(
                LoginService.SUCCESS,
                loginService.login(
                    session,
                    GOOD_USER, goodPassword
                )
        );
        assertTrue(session.isLoggedIn());

        // make sure we are promoted to admin...
        assertEquals(session.getPortalUser().getType(), PortalUser.USER_ADMIN);

        // ensure username set inside session
        assertTrue(session.getPortalUser().getUsername().equals(GOOD_USER));


    }

    @Test
    public void gracefulFail() {
        // fail nicely when null username and password passed in
        assertEquals(
            LoginService.FAIL_INVALID,
            loginService.login(
                null,
                null,
                null
            )
        );

    }

    @Test
    public void testLoginFail() {
        PortalSession session;

        session = new PortalSession();
        assertEquals(
            LoginService.FAIL_INVALID,
            loginService.login(
                    session,
                    GOOD_USER,
                    BAD_PASSWORD
            )
        );
        assertFalse(session.isLoggedIn());
        // the portalUser should always be set within a session to avoid having
        // to constantly be checking it for null or isLoggedIn()
        assertNotNull(session.getPortalUser());

        session = new PortalSession();
        assertEquals(
                LoginService.FAIL_INVALID,
                loginService.login(
                    session,
                    BAD_USER,
                    BAD_PASSWORD
                )
        );
        assertFalse(session.isLoggedIn());

        session = new PortalSession();
        assertEquals(
                LoginService.FAIL_INVALID,
                loginService.login(
                    session,
                    BAD_USER,
                    goodPassword
                )
        );
        assertFalse(session.isLoggedIn());
    }

    @Test
    public void createUserAndLogin() {
        // create user
        String[] hashAndSalt = loginService.hashWithSalt(NEW_PASSWORD);
        String passwordEntry = loginService.formatForProperties(hashAndSalt);

        // put in properties -  must save as well.  Maven will do this in /target
        // so it doesn't affect our test data
        Properties properties = loginService.getUsers();
        assertNotNull("properties are null - was users.properties loaded?", properties);
        properties.put(NEW_USER, passwordEntry);
        assertTrue(loginService.updateProperties(properties,"JUNIT"));


        // see if we can login...
        PortalSession session = new PortalSession();
        assertEquals(
                LoginService.SUCCESS,
                loginService.login(session, NEW_USER, NEW_PASSWORD));

        assertTrue(session.isAdmin());

        // ensure username set inside session
        assertTrue(session.getPortalUser().getUsername().equals(NEW_USER));
    }

}

