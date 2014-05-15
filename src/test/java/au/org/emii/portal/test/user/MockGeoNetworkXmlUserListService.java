/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.user;

import org.apache.commons.io.IOUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author geoff
 */
public class MockGeoNetworkXmlUserListService implements Container {

    /**
     * File to output on success - regular user
     */
    private final static String SUCCEED_REGULAR_OUTPUT = "test_data/xml.user.get-succeed_regular";

    /**
     * File to output on success - admin user
     */
    private final static String SUCCEED_ADMIN_OUTPUT = "test_data/xml.user.get-succeed_admin";

    /**
     * File to output on invalid user requested
     */
    private final static String FAIL_INVALID_OUTPUT = "test_data/xml.user.get-fail_invalid_user";

    /**
     * File to output when not already logged in
     */
    private final static String FAIL_UNAUTHORISED_OUTPUT = "test_data/xml.user.fail_unauthorised";

    /**
     * Give the username that is associated with the xml response in SUCCEED_REGULAR_OUTPUT
     */
    private final static String REGULAR_USERNAME = "HopeJ";

    /**
     * Give the username that is associated with the xml response in SUCCEED_ADMIN_OUPUT
     */
    private final static String ADMIN_USERNAME = "portal";

    /**
     * Flag to indicate whether we have authenticated to mest or not
     */
    public boolean loggedIn = false;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }


    @Override
    public void handle(Request request, Response response) {
        PrintStream out = null;
        try {
            String username = request.getParameter("username");
            String responseFile;

            if (loggedIn) {
                // once logged in, you always get XML back
                response.set("Content-Type", "application/xml");

                if (username.equals(REGULAR_USERNAME)) {
                    responseFile = SUCCEED_REGULAR_OUTPUT;
                } else if (username.equals(ADMIN_USERNAME)) {
                    responseFile = SUCCEED_ADMIN_OUTPUT;
                } else {
                    responseFile = FAIL_INVALID_OUTPUT;
                }

            } else {
                responseFile = FAIL_UNAUTHORISED_OUTPUT;
            }

            out = response.getPrintStream();
            out.print(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(responseFile)));
        } catch (IOException ex) {
            Logger.getLogger(MockGeoNetworkXmlUserListService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

}
