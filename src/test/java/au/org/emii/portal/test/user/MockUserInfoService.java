/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.user;

import au.org.emii.portal.session.PortalUser;
import au.org.emii.portal.session.PortalUserImpl;
import au.org.emii.portal.service.UserInfoService;

/**
 * A mock user info service that always succeeds.  Will return a PortalUser
 * instance for a regular user with all strings set to the value of the
 * TEST_STRING constant
 * @author geoff
 */
public class MockUserInfoService implements UserInfoService {

        public static final String TEST_STRING = "test";

        @Override
        public PortalUser userInfo(String username) {
                PortalUser u = new PortalUserImpl();
                u.setUsername(username);
                u.setAddress(TEST_STRING);
                u.setCountry(TEST_STRING);
                u.setEmail(TEST_STRING);
                u.setState(TEST_STRING);
                u.setFirstName(TEST_STRING);
                u.setLastName(TEST_STRING);
                u.setOrganisation(TEST_STRING);
                u.setType(PortalUser.USER_REGULAR);
                u.setZip(TEST_STRING);

                return u;
        }


}
