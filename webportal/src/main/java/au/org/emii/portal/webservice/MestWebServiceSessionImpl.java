/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.webservice;

import au.org.emii.portal.authentication.LoginService;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author geoff
 */
public class MestWebServiceSessionImpl implements WebServiceSession {

    private ApacheHttpClient client;
    private String username = null;
    private String password = null;
    private LoginService loginService = null;

    @Override
    public void login() {
        loginService.systemLogin(getClient(), username, password);
    }

    @Override
    public ApacheHttpClient getClient() {
        if (client == null) {
            makeClient();
        }
        return client;
    }

    private void makeClient() {
        // must enable cookies in config prior to making client or they get ignored
        DefaultApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
        config.getProperties().put(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, new Boolean(true));
        client = ApacheHttpClient.create(config);

        login();
    }

    public LoginService getLoginService() {
        return loginService;
    }

    public void setLoginService(LoginService loginService) {
        this.loginService = loginService;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    
}
