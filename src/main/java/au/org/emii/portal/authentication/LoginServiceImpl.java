/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.authentication;

import au.org.emii.portal.user.UserInfoService;
import au.org.emii.portal.webservice.XmlWebService;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.webservice.UserManagementWebServiceEndpointImpl;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.log4j.Logger;
import javax.ws.rs.core.MultivaluedMap;

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
    public boolean login(String service, PortalSession session, String username, String password) {
        serviceUri = getUserManagementWebServiceById(service).loginServiceUri();
        boolean loginOk = false;

        // make the request
        xmlWebService.makeRequest(serviceUri, makeQueryParams(username, password));

        if (xmlWebService.isResponseXml()) {
            loginOk = (xmlWebService.parseNode("//ok") != null);
            session.setPortalUser(userInfoService.userInfo(service, username));
        }
        // else definate failure...
        xmlWebService.close();
        return loginOk;
    }

    @Override
    public boolean login(PortalSession session, String username, String password) {
        return login(getDefaultUserManagementServiceId(), session, username, password);
    }

    @Override
    public boolean systemLogin(Client client, String service, String username, String password) {
        serviceUri = getUserManagementWebServiceById(service).loginServiceUri();
        boolean loginOk = false;

        // make the request
        xmlWebService.makeRequest(client, serviceUri, makeQueryParams(username, password));

        if (xmlWebService.isResponseXml()) {
            loginOk = (xmlWebService.parseNode("//ok") != null);
        }
        // else definate failure...
        xmlWebService.close();
        return loginOk;
    }

    @Override
    public boolean systemLogin(Client client, String username, String password) {
         return systemLogin(client, getDefaultUserManagementServiceId(), username, password);
    }
}

