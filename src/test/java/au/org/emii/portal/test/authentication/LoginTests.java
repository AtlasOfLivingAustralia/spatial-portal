/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.test.AbstractTester;
import au.org.emii.portal.webservice.UserManagementWebService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.*;
/**
 *
 * @author geoff
 */
public class LoginTests extends AbstractTester {

    @Autowired
    private LoginService loginService = null;

    @Resource(name="mockUserManagementWebService")
    private UserManagementWebService service = null;
    private Connection connection = null;

    public LoginService getLoginService() {
        return loginService;
    }

    public void setLoginService(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * Create fake http server to service requests
     */
    @Before
    public void setup() {   
        Container container = new MockGeoNetworkXmlUserLoginService();
        try {
            connection = new SocketConnection(container);
            SocketAddress address = new InetSocketAddress(service.servicePort(service.loginServiceUri()));
            connection.connect(address);
        } catch (IOException ex) {
            logger.error("unable to setup test http server", ex);
        }
    }

    @After
    public void shutdown() {
        try {
            connection.close();
        } catch (IOException ex) {}
    }

    @Test
    public void testLoginOk() {
        PortalSession session = new PortalSession();
        assertEquals(
                loginService.login(
                    session,
                    MockGeoNetworkXmlUserLoginService.GOOD_USER,
                    MockGeoNetworkXmlUserLoginService.GOOD_PASSWORD
                ),
                LoginService.SUCCESS
        );
        assertTrue(session.isLoggedIn());

        // the user we get back will be from the MockUserInfoService
        assertNotNull(session.getPortalUser());
        assertNotNull(session.getPortalUser().getAddress());
        assertNotNull(session.getPortalUser().getCountry());
        assertNotNull(session.getPortalUser().getEmail());
        assertNotNull(session.getPortalUser().getFirstName());
        assertNotNull(session.getPortalUser().getLastName());
        assertNotNull(session.getPortalUser().getOrganisation());
        assertNotNull(session.getPortalUser().getState());
        assertTrue(session.getPortalUser().getUsername().equals(MockGeoNetworkXmlUserLoginService.GOOD_USER));

        // although our admin user has admin type in the xml, our mock service always
        // sets everything to type USER_REGULAR
        assertEquals(session.getPortalUser().getType(), PortalUser.USER_REGULAR);

        assertNotNull(session.getPortalUser().getZip());

    }

    @Test
    public void testLoginFail() {
        PortalSession session;

        session = new PortalSession();
        assertEquals(
            LoginService.FAIL_INVALID,
            loginService.login(
                    session,
                    MockGeoNetworkXmlUserLoginService.GOOD_USER,
                    MockGeoNetworkXmlUserLoginService.BAD_PASSWORD
            )
        );
        assertFalse(session.isLoggedIn());
        // the portalUser should always be set within a session to avoid having
        // to constantly be checking it for null or isLoggedIn()
        assertNotNull(session.getPortalUser());
        
        session = new PortalSession();
        assertEquals(
                loginService.login(
                    session,
                    MockGeoNetworkXmlUserLoginService.BAD_USER,
                    MockGeoNetworkXmlUserLoginService.BAD_PASSWORD
                ),
                LoginService.FAIL_INVALID
        );
        assertFalse(session.isLoggedIn());

        session = new PortalSession();
        assertEquals(
                loginService.login(
                    session, MockGeoNetworkXmlUserLoginService.BAD_USER,
                    MockGeoNetworkXmlUserLoginService.GOOD_PASSWORD
                ),
                LoginService.FAIL_INVALID
        );
        assertFalse(session.isLoggedIn());
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

    public UserManagementWebService getService() {
            return service;
    }

    public void setService(UserManagementWebService service) {
            this.service = service;
    }



}
