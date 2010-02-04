/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.user;

import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.user.PortalUserImpl;
import au.org.emii.portal.user.UserInfoService;
import au.org.emii.portal.webservice.UserManagementWebService;
import java.util.Map;

/**
 * A mock user info service that always succeeds.  Will return a PortalUser
 * instance for a regular user with all strings set to the value of the
 * TEST_STRING constant
 * @author geoff
 */
public class MockUserInfoService implements UserInfoService {

        public static final String TEST_STRING = "test";

        @Override
        public PortalUser userInfo(String service, String username) {
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

    @Override
    public PortalUser userInfo(String username) {
        return userInfo(null, username);
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
}
