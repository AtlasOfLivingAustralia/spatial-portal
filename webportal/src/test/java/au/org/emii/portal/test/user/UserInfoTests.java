/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.test.user;

import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.session.PortalUser;
import au.org.emii.portal.service.UserInfoService;
import au.org.emii.portal.test.AbstractTester;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Resource;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.After;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the user info subsystem
 * @author geoff
 */
public class UserInfoTests extends AbstractTester {

        /**
         * HTTP server instance - reference allows use to stop gracefully after testing
         */
        private Connection connection = null;

        /**
         * Reference to mock server implementation
         */
        private Container container = null;

        @Resource(name="userInfoService")
        private UserInfoService userInfoService = null;

        @Autowired
        private MestWebServiceParameters parameters = null;

        @Test
        public void adminUser() {

                // login to mest before looking up users
                login();

                PortalUser p = userInfoService.userInfo("portal");

                // Test portal user
                assertNotNull(p);

                // username
                assertNotNull(p.getUsername());
                assertTrue(p.getUsername().equals("portal"));

                // surname
                assertNotNull(p.getLastName());
                assertTrue(p.getLastName().equals("portal"));

                // first name
                assertNotNull(p.getFirstName());
                assertTrue(p.getFirstName().equals("imos"));

                // profile (type)
                assertEquals(p.getType(), PortalUser.USER_ADMIN);

                // address, state, zip, country, email and organisation
                // are not set for this user
                assertNull(p.getAddress());
                assertNull(p.getState());
                assertNull(p.getZip());
                assertNull(p.getCountry());
                assertNull(p.getEmail());
                assertNull(p.getOrganisation());
        }

        @Test
        public void regularUser() {

                // login to mest before looking up users
                login();
                
                PortalUser p = userInfoService.userInfo("HopeJ");

                // test valid response
                assertNotNull(p);

                // username
                assertNotNull(p.getUsername());
                assertTrue(p.getUsername().equals("HopeJ"));

                // surname
                assertNotNull(p.getLastName());
                assertTrue(p.getLastName().equals("Hope"));
                
                // name
                assertNotNull(p.getFirstName());
                assertTrue(p.getFirstName().equals("Jacqui"));

                // profile (type)
                assertEquals(p.getType(), PortalUser.USER_REGULAR);

                // address
                assertNotNull(p.getAddress());
                assertTrue(p.getAddress().equals("UTAS"));

                // state
                assertNotNull(p.getState());
                assertTrue(p.getState().equals("TAS"));

                // zip
                assertNotNull(p.getZip());
                assertTrue(p.getZip().equals("7000"));

                // country
                assertNotNull(p.getCountry());
                assertTrue(p.getCountry().equals("Australia"));

                // email
                assertNotNull(p.getEmail());
                assertTrue(p.getEmail().equals("Jacqui.Hope@utas.edu.au"));

                // organisation
                assertNotNull(p.getOrganisation());
                assertTrue(p.getOrganisation().equals("emii"));
        }


        @Test
        public void invalidUser() {

                // login to mest before looking up users
                login();
                
                PortalUser p = userInfoService.userInfo("nothere");
                assertNull(p);
        }

        @Test
        public void lookupWhileLoggedOut() {
                PortalUser p = userInfoService.userInfo("portal");
                // if not logged into mest as admin, should get a null back here
                assertNull(p);
        }

        @After
        public void shutdown() {
                try {
                        connection.close();
                } catch (IOException ex) {
                }
        }

        @Before
        public void setup() {
                container = new MockGeoNetworkXmlUserListService();
                try {
                        connection = new SocketConnection(container);
                        SocketAddress address = new InetSocketAddress(
                                parameters.servicePort(parameters.userInfoServiceUri()));
                        connection.connect(address);
                } catch (IOException ex) {
                        logger.error("unable to setup test http server", ex);
                }
        }

        private void login() {
                ((MockGeoNetworkXmlUserListService) container).setLoggedIn(true);
        }


        public UserInfoService getUserInfoService() {
                return userInfoService;
        }

        public void setUserInfoService(UserInfoService userInfoService) {
                this.userInfoService = userInfoService;
        }

    public MestWebServiceParameters getParameters() {
        return parameters;
    }

    public void setParameters(MestWebServiceParameters parameters) {
        this.parameters = parameters;
    }
}
