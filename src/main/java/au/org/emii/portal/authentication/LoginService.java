/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import au.org.emii.portal.PortalSession;
import au.org.emii.portal.webservice.UserManagementWebServiceEndpoint;
import com.sun.jersey.api.client.Client;

/**
 *
 * @author geoff
 */
public interface LoginService extends UserManagementWebServiceEndpoint {


    /**
     * Attept to login with passed in username and password using a specified service.
     * @param service The key in the Map of UserManagementWebService instances for the desired service
     * @param username
     * @param password
     * @return true on success, otherwise false
     */
    public int login(String service, PortalSession session, String username, String password);

    /**
     * Attept to login with passed in username and password, using the default service
     * @param username
     * @param password
     * @return true on success, otherwise false
     */
    public int login(PortalSession session, String username, String password);


    /**
     * Login using the provided client instance - this is used to create an
     * authenticated client that can then be used for admin operations such as
     * getting info on a user.  Uses the specified service
     */
    public int systemLogin(Client client, String service, String username, String password);

    /**
     * Login using the provided client instance - this is used to create an
     * authenticated client that can then be used for admin operations such as
     * getting info on a user.  Uses the default service
     */
    public int systemLogin(Client client, String username, String password);
}
