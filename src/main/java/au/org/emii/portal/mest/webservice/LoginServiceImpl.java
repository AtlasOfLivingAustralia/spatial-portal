/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.service.UserInfoService;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.mest.webservice.MestWebService;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.webservice.XmlWebService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;
import javax.ws.rs.core.MultivaluedMap;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public class LoginServiceImpl extends MestWebService implements LoginService {

    /**
     * User info service - looks up real name, email address, etc for a given
     * username
     */
    private UserInfoService userInfoService = null;

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
    public int login(PortalSession session, String username, String password) {

        // make the request
        xmlWebService.makeRequest(parameters.loginServiceUri(), makeQueryParams(username, password));
        int result = processResult();

        if (result == LoginService.SUCCESS) {
            session.setPortalUser(userInfoService.userInfo(username));
        }

        return result;
    }


    private int processResult() {
        int result;
        if (xmlWebService.isResponseXml()) {
            // you only get xml back if the login was successful - if we can't find //ok
            // then we have an unknown error condition happening
            result = (xmlWebService.parseNode("//ok") != null) ? SUCCESS : FAIL_UNKNOWN;

        } else if (xmlWebService.getRawResponse().contains(parameters.getMestConfiguration().getTokenIncorrectLogin())) {
            // wrong username/password

            // tokenIncorrectLogin is the token we receive burried in html from the server to indicate
            // that the wrong username/password was sent.  Typically "UserLoginEx"
            result = FAIL_INVALID;
        } else {
            // definate failure - maybe the server is down?
            result = FAIL_UNKNOWN;
        }
        xmlWebService.close();
        return result;
    }

    @Override
    public int systemLogin(Client client, String username, String password) {

        // make the request
        xmlWebService.makeRequest(client, parameters.loginServiceUri(), makeQueryParams(username, password));
        return processResult();
    }

}

