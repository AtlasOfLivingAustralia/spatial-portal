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
 * Service to generate and email a change key as used for resetting forgotten
 * passwords
 *
 * @author geoff
 */
public class MockGeoNetworkXmlPasswordEmaillinkService implements Container {

    public final static String SUCCEED_OUTPUT = "test_data/xml.password.emaillink-succeed";

    public final static String FAIL_INVALID_USER_OUTPUT = "test_data/xml.password.emaillink-fail_invalid_user";

    private Logger logger = Logger.getLogger(getClass());

    /**
     * Name of user that exists
     */
    public final static String GOOD_USER = "test";

    /**
     * Name of user that doesn't exist
     */
    public final static String BAD_USER = "nothere";

    @Override
    public void handle(Request request, Response response) {
        try {
            String username = request.getParameter("username");
            PrintStream out = response.getPrintStream();
            String outputFile;

            if (username.equals(GOOD_USER)) {
                response.set("Content-Type", "application/xml");
                outputFile = SUCCEED_OUTPUT;
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

}
