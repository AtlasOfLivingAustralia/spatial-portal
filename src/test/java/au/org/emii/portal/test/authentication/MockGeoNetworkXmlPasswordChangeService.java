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

import java.io.PrintStream;

/**
 * Password change mock service - used for restting forgotten passwords with a
 * change key
 *
 * @author geoff
 */
public class MockGeoNetworkXmlPasswordChangeService implements Container {

    private Logger logger = Logger.getLogger(getClass());
    /**
     * Flag to treat change key as expired
     */
    private boolean changeKeyExpired = false;
    /**
     * Name of user that exists
     */
    public final static String GOOD_USER = "test";
    /**
     * Name of user that doesn't exist
     */
    public final static String BAD_USER = "nothere";
    /**
     * Change key to use for testing - value doesn't matter
     */
    public final static String CHANGE_KEY = "aaaaaa";
    /**
     * File contents to return on success
     */
    public final static String SUCCEED_OUTPUT = "test_data/xml.password.change-succeed";
    /**
     * File contents to return on expired changekey (by calling invalidate())
     */
    public final static String FAIL_EXPIRED_OUTPUT = "test_data/xml.password.change-fail_expired";
    /**
     * File contents to return on invalid user requested
     */
    public final static String FAIL_INVALID_USER_OUTPUT = "test_data/xml.password.change-fail_invalid_user";

    @Override
    public void handle(Request request, Response response) {
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String changeKey = request.getParameter("changeKey");
            PrintStream out = response.getPrintStream();
            String outputFile;
            if (username.equals(GOOD_USER)) {
                if (changeKeyExpired) {
                    outputFile = FAIL_EXPIRED_OUTPUT;
                } else {
                    response.set("Content-Type", "application/xml");
                    outputFile = SUCCEED_OUTPUT;
                }
            } else {
                outputFile = FAIL_INVALID_USER_OUTPUT;
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

        } catch (Exception ex) {
            logger.error("Exception - should never happen", ex);
        }
    }

    public void expireChangeKey() {
        changeKeyExpired = true;
    }
}
