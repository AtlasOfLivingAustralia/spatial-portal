/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import au.org.emii.portal.PortalSession;
import au.org.emii.portal.service.ServiceStatusCodes;
import com.sun.jersey.api.client.Client;

/**
 *
 * @author geoff
 */
public interface LoginService extends ServiceStatusCodes {


    /**
     * Attept to login with passed in username and password
     * @param username
     * @param password
     * @return true on success, otherwise false
     */
    public int login(PortalSession session, String username, String password);


    /**
     * Login using the provided client instance - this is used to create an
     * authenticated client that can then be used for admin operations such as
     * getting info on a user.  Uses the default service
     */
    public int systemLogin(Client client, String username, String password);
}
