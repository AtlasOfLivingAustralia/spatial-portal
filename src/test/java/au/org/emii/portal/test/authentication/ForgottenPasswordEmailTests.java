/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;

import au.org.emii.portal.authentication.ForgottenPasswordService;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.test.AbstractTester;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.After;
import org.junit.Before;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import java.net.SocketAddress;
import static org.junit.Assert.*;

/**
 *
 * Tests for part 1 of handling a forgotten password - generating and emailing a changeKey
 * @author geoff
 */
public class ForgottenPasswordEmailTests extends AbstractTester {

    @Autowired
    private MestWebServiceParameters parameters = null;

    private Connection connection = null;

    /**
     * Service under test
     */
    @Autowired
    private ForgottenPasswordService forgottenPasswordService = null;

    /**
     * test resetting password for a user that exists - must succeed
     */
    @Test
    public void resetUserExists() {
        assertEquals(
                ForgottenPasswordService.SUCCESS,
                forgottenPasswordService.requestReset(
                    MockGeoNetworkXmlPasswordEmaillinkService.GOOD_USER
                )
        );
    }

    /**
     * Test restting password for user that doesn't exist - must fail
     */
    @Test
    public void resetUserInvalid() {
        assertEquals(
                ForgottenPasswordService.FAIL_INVALID,
                forgottenPasswordService.requestReset(
                    MockGeoNetworkXmlPasswordEmaillinkService.BAD_USER
                )
        );
    }
    
    public ForgottenPasswordService getForgottenPasswordService() {
        return forgottenPasswordService;
    }

    public void setForgottenPasswordService(ForgottenPasswordService forgottenPasswordService) {
        this.forgottenPasswordService = forgottenPasswordService;
    }

    /**
     * Create fake http server to service requests
     */
    @Before
    public void setup() {
        Container container = new MockGeoNetworkXmlPasswordEmaillinkService();
        try {
            connection = new SocketConnection(container);
            SocketAddress address = new InetSocketAddress(
                    parameters.servicePort(parameters.resetPasswordServiceUri()));
            connection.connect(address);
        } catch (IOException ex) {
            logger.error("unable to setup test http server", ex);
        }
    }

    @After
    public void shutdown() {
        try {
            connection.close();
        } catch (IOException ex) {}
    }

    public MestWebServiceParameters getParameters() {
        return parameters;
    }

    public void setParameters(MestWebServiceParameters parameters) {
        this.parameters = parameters;
    }


}
