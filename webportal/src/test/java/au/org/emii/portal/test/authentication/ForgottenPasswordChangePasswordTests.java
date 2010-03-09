/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.test.authentication;

import au.org.emii.portal.service.ForgottenPasswordService;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.test.AbstractTester;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.Resource;
import org.junit.After;
import org.junit.Before;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import java.net.SocketAddress;
import static org.junit.Assert.*;

/**
 * Tests for part 2 of handling a forgotten password - changing the password
 * @author geoff
 */
public class ForgottenPasswordChangePasswordTests extends AbstractTester {

    /**
     * Password to change to - value doesn't matter
     */
    public final static String PASSWORD = "bbbbbb";

    @Autowired
    private MestWebServiceParameters parameters = null;

    private Connection connection = null;

    /**
     * Reference to mock server implementation
     */
    private Container container = null;

    /**
     * Service under test
     */
    @Autowired
    private ForgottenPasswordService forgottenPasswordService = null;

    /**
     * Change password - must succeed
     */
    @Test
    public void changePassword() {
        assertEquals(
                forgottenPasswordService.changePassword(
                MockGeoNetworkXmlPasswordChangeService.GOOD_USER,
                MockGeoNetworkXmlPasswordChangeService.CHANGE_KEY,
                PASSWORD),
                ForgottenPasswordService.SUCCESS);
    }

    /**
     * Change password for invalid user - must fail
     */
    @Test
    public void changePasswordInvalidUser() {
        assertEquals(
                forgottenPasswordService.changePassword(
                MockGeoNetworkXmlPasswordChangeService.BAD_USER,
                MockGeoNetworkXmlPasswordChangeService.CHANGE_KEY,
                PASSWORD),
                ForgottenPasswordService.FAIL_INVALID);

    }

    /**
     * Make a request with an expired change key - must fail with expired value
     */
    @Test
    public void changePasswordExpiredChangeKey() {
        // force key expire
        ((MockGeoNetworkXmlPasswordChangeService) container).expireChangeKey();

        assertEquals(
                forgottenPasswordService.changePassword(
                MockGeoNetworkXmlPasswordChangeService.GOOD_USER,
                MockGeoNetworkXmlPasswordChangeService.CHANGE_KEY,
                PASSWORD),
                ForgottenPasswordService.FAIL_EXPIRED);
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
        container = new MockGeoNetworkXmlPasswordChangeService();
        try {
            connection = new SocketConnection(container);
            SocketAddress address = new InetSocketAddress(
                    parameters.servicePort(parameters.changePasswordServiceUri()));
            connection.connect(address);
        } catch (IOException ex) {
            logger.error("unable to setup test http server", ex);
        }
    }

    @After
    public void shutdown() {
        try {
            connection.close();
        } catch (IOException ex) {
        }
    }

    public MestWebServiceParameters getParameters() {
        return parameters;
    }

    public void setParameters(MestWebServiceParameters parameters) {
        this.parameters = parameters;
    }

    
}
