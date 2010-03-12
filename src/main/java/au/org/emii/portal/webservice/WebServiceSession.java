/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.webservice;

import com.sun.jersey.client.apache.ApacheHttpClient;

/**
 *
 * @author geoff
 */
public interface WebServiceSession {
    /**
     * (Re)login to the web service
     */
    public void login();

    /**
     * Get a http client instance - should already be logged in but if it looks
     * like it isn't, you can call login() to solve this
     * @return
     */
    public ApacheHttpClient getClient();
}
