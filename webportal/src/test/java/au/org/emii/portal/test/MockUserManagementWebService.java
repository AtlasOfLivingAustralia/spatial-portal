/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.test;

import au.org.emii.portal.SearchCatalogue;
import au.org.emii.portal.webservice.UserManagementWebService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author geoff
 */
public class MockUserManagementWebService implements UserManagementWebService {

    private String serviceUri = "http://localhost:8888/";

    @Override
    public String loginServiceUri() {
        return serviceUri;
    }

    @Override
    public String userInfoServiceUri() {
        return serviceUri;
    }

    @Override
    public String selfRegistrationServiceUri() {
        return serviceUri;

    }

    @Override
    public String newUserProfile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String administratorProfile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String resetPasswordServiceUri() {
        return serviceUri;
    }

    @Override
    public String changePasswordServiceUri() {
        return serviceUri;
    }

    @Override
    public int servicePort(String uri) {
        return SearchCatalogue.resolveServicePort(uri);
    }
}
