/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.authentication;

import au.org.emii.portal.user.UserInfoService;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.webservice.UserManagementWebServiceEndpointImpl;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;
import javax.ws.rs.core.MultivaluedMap;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public class LoginServiceImpl extends UserManagementWebServiceEndpointImpl implements LoginService {

    /**
     * Logger instance
     */
    private Logger logger = Logger.getLogger(getClass());
    /**
     * User info service - looks up real name, email address, etc for a given
     * username
     */
    private UserInfoService userInfoService = null;

    /**
     * This is the token we receive burried in html from the server to indicate
     * that the wrong username/password was sent.  Typically "UserLoginEx"
     */
    private String incorrectPasswordToken = null;

    public UserInfoService getUserInfoService() {
        return userInfoService;
    }

    public void setUserInfoService(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    private MultivaluedMap makeQueryParams(String username, String password) {
        // setup web service parameters
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        queryParams.add("password", password);

        return queryParams;
    }

    @Override
    public int login(String service, PortalSession session, String username, String password) {
        serviceUri = getUserManagementWebServiceById(service).loginServiceUri();
        int result;

        // make the request
        xmlWebService.makeRequest(serviceUri, makeQueryParams(username, password));

        if (xmlWebService.isResponseXml()) {
            // you only get xml back if the login was successful - if we can't find //ok
            // then we have an unknown error condition happening
            result = (xmlWebService.parseNode("//ok") != null) ? SUCCESS : FAIL_UNKNOWN;
            session.setPortalUser(userInfoService.userInfo(service, username));
        } else if (xmlWebService.getRawResponse().contains(incorrectPasswordToken)) {
            // wrong username/password
            result = FAIL_INVALID;
        } else {
            // definate failure - maybe the server is down?
            result = FAIL_UNKNOWN;
        }

        xmlWebService.close();
        return result;
    }

    public String getIncorrectPasswordToken() {
        return incorrectPasswordToken;
    }

    @Required
    public void setIncorrectPasswordToken(String incorrectPasswordToken) {
        this.incorrectPasswordToken = incorrectPasswordToken;
    }



    @Override
    public int login(PortalSession session, String username, String password) {
        return login(getDefaultUserManagementServiceId(), session, username, password);
    }

    @Override
    public int systemLogin(Client client, String service, String username, String password) {
        serviceUri = getUserManagementWebServiceById(service).loginServiceUri();
        int result;

        // make the request
        xmlWebService.makeRequest(client, serviceUri, makeQueryParams(username, password));

        if (xmlWebService.isResponseXml()) {
            result = (xmlWebService.parseNode("//ok") != null) ? SUCCESS : FAIL_INVALID;
        } else {
            // definate failure - maybe the server is down?
            result = FAIL_UNKNOWN;
        }
        // else definate failure...
        xmlWebService.close();
        return result;
    }

    @Override
    public int systemLogin(Client client, String username, String password) {
         return systemLogin(client, getDefaultUserManagementServiceId(), username, password);
    }
}

