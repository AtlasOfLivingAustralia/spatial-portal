/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.authentication;

import au.org.emii.portal.authentication.ForgottenPasswordService;
import au.org.emii.portal.test.AbstractTester;
import au.org.emii.portal.webservice.UserManagementWebService;
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
 *
 * Tests for part 1 of handling a forgotten password - generating and emailing a changeKey
 * @author geoff
 */
public class ForgottenPasswordEmailTests extends AbstractTester {

    /**
     * Password to change to - value doesn't matter
     */
    public final static String PASSWORD = "bbbbbb";

    @Resource(name="mockUserManagementWebService")
    private UserManagementWebService service = null;

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
                forgottenPasswordService.requestReset(
                    MockGeoNetworkXmlPasswordEmaillinkService.GOOD_USER
                ),
                ForgottenPasswordService.SUCCESS
        );
    }

    /**
     * Test restting password for user that doesn't exist - must fail
     */
    @Test
    public void resetUserInvalid() {
        assertEquals(
                forgottenPasswordService.requestReset(
                    MockGeoNetworkXmlPasswordEmaillinkService.BAD_USER
                ),
                ForgottenPasswordService.FAIL_INVALID_USER
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
            SocketAddress address = new InetSocketAddress(service.servicePort(service.resetPasswordServiceUri()));
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
}
