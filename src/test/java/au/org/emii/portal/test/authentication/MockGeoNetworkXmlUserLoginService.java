/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Use simple framework embedded http server to create simulated http responses
 *
 * @author geoff
 */
public class MockGeoNetworkXmlUserLoginService implements Container {

    private final static String SUCCEED_OUTPUT = "test_data/xml.user.login-succeed";
    private final static String FAIL_OUTPUT = "test_data/xml.user.login-fail";
    public static final String GOOD_USER = "admin";
    public static final String GOOD_PASSWORD = "admin";
    public static final String BAD_USER = "nothere";
    public static final String BAD_PASSWORD = "nothere";
    private Logger logger = Logger.getLogger(getClass());
    /**
     * True if login requests will succeed otherwise false
     */
    private boolean succeed = true;

    public boolean isSucceed() {
        return succeed;
    }

    public void setSucceed(boolean succeed) {
        this.succeed = succeed;
    }


    /**
     * Fake long function
     *
     * @param username
     * @param password
     * @return
     */
    private boolean login(String username, String password) {
        return username != null && username.equals(GOOD_USER) &&
                password != null && password.equals(GOOD_PASSWORD);
    }

    @Override
    public void handle(Request request, Response response) {
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            PrintStream out = response.getPrintStream();
            boolean loggedIn = login(username, password);
            logger.debug("fake sever says logged in = " + loggedIn);
            if (loggedIn) {
                response.set("Content-Type", "application/xml");
                out.print(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(SUCCEED_OUTPUT)));
            } else {
                response.set("Content-Type", "text/html");
                out.print(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(FAIL_OUTPUT)));
            }
            out.close();
        } catch (IOException e) {
            logger.error("IO exception - should never happen", e);
        }

    }


}
