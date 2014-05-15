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
 * Fake password change service used for changing passwords when the old
 * password is known
 *
 * @author geoff
 */
public class MockGeoNetworkXmlPwUpdateService implements Container {

    private Logger logger = Logger.getLogger(getClass());
    public static final String BAD_PASSWORD = "aaaaaa";
    public static final String GOOD_PASSWORD = "bbbbbb";
    public static final String NEW_PASSWORD = "nnnnnn";

    private static final String SUCCEED_OUTPUT = "test_data/xml.user.pwupdate-succeed";
    private static final String WRONG_PASSWORD_OUTPUT = "test_data/xml.user.pwupdate-fail_wrong_password";
    private static final String UNAUTHORISED_OUTPUT = "test_data/xml.user.pwupdate-fail-unauthorised";
    private boolean loggedIn = true;

    @Override
    public void handle(Request request, Response response) {
        String outputFile = null;
        try {
            String password = request.getParameter("password");
            String newPassword = request.getParameter("newPassword");
            PrintStream out = response.getPrintStream();

            if (loggedIn) {
                if (password.equals(GOOD_PASSWORD)) {
                    // ok change password
                    response.set("Content-Type", "application/xml");
                    outputFile = SUCCEED_OUTPUT;
                    logger.info("password changed to " + newPassword);
                } else {
                    outputFile = WRONG_PASSWORD_OUTPUT;
                    logger.error("password incorrect - not changing");
                }
            } else {
                outputFile = UNAUTHORISED_OUTPUT;
                logger.error("not logged in");
            }


            // XML mime type is only for a successful response - all other cases
            // send html mime type back, even though output is valid xml.  This
            // is to mimic the MEST server
            if (!outputFile.equals(SUCCEED_OUTPUT)) {
                response.set("Content-Type", "text/html");
            }

            String output = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(outputFile));
            out.print(output);
            out.close();

        } catch (IOException ex) {
            logger.error("mock server io error", ex);
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }


}
