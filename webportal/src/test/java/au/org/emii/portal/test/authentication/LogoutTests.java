/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;

import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.service.LogoutService;
import au.org.emii.portal.session.PortalUser;
import au.org.emii.portal.test.AbstractTester;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.*;

/**
 *
 * @author geoff
 */
public class LogoutTests extends AbstractTester {
        public static String TEST_STRING = "test";

        /**
         * Logout service to test
         */
        @Autowired
        private LogoutService logoutService = null;

        public LogoutService getLogoutService() {
                return logoutService;
        }

        public void setLogoutService(LogoutService logoutService) {
                this.logoutService = logoutService;
        }

        /**
         * Check a user's session is properly logged out when logout is called
         */
        @Test
        public void logout() {
                // first login a user by manually creating a PortalUser instance
                PortalSession session = new PortalSession();
                PortalUser user;
                user = session.getPortalUser();

                user.setType(PortalUser.USER_REGULAR);
                user.setAddress(TEST_STRING);
                user.setCountry(TEST_STRING);
                user.setEmail(TEST_STRING);
                user.setFirstName(TEST_STRING);
                user.setLastName(TEST_STRING);
                user.setOrganisation(TEST_STRING);
                user.setState(TEST_STRING);
                user.setUsername(TEST_STRING);
                user.setZip(TEST_STRING);

                // check user is really logged in...
                assertTrue(session.isLoggedIn());
                assertFalse(session.isAdmin());

                // then log them out
                logoutService.logout(session);

                // then check logout has been done properly
                assertFalse(session.isLoggedIn());
                assertFalse(session.isAdmin());
                user = session.getPortalUser();
                assertEquals(user.getType(), PortalUser.USER_NOT_LOGGED_IN);
                assertNull(user.getAddress());
                assertNull(user.getCountry());
                assertNull(user.getEmail());
                assertNull(user.getFirstName());
                assertNull(user.getLastName());
                assertNull(user.getOrganisation());
                assertNull(user.getState());
                assertNull(user.getUsername());
                assertNull(user.getZip());



        }
}

