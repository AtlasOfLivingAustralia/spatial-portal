/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.test.registration;

import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.registration.RegistrationService;
import au.org.emii.portal.test.AbstractTester;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Resource;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.*;

/**
 *
 * @author geoff
 */
public class RegistrationTests extends AbstractTester {

        private Connection connection = null;

        private final static String USER_SURNAME = "test_surname";

        /**
         * (first)name - not to be confused with username!
         */
        private final static String USER_NAME = "test_name";

        private final static String USER_ADDRESS = "test_address";

        private final static String USER_STATE = "test_state";

        private final static String USER_ZIP = "test_zip";

        private final static String USER_COUNTRY = "test_country";

        private final static String USER_EMAIL = "test_email";

        private final static String USER_ORGANISATION = "test_organisation";

        private final static String USER_KIND = "test_kind";

        private final static String USER_PROFILE = "test_profile";

        @Autowired
        private MestWebServiceParameters parameters = null;

        @Resource(name="registrationService")
        private RegistrationService registrationService = null;



        /**
         * keep a reference to the fake servlet so we can check what gets created
         */
        private Container container = null;

        @Before
        public void setup() {
                container = new MockGeoNetworkXmlSelfRegisterService();
                try {
                        connection = new SocketConnection(container);
                        SocketAddress address = new InetSocketAddress(
                                parameters.servicePort(parameters.selfRegistrationServiceUri()));
                        connection.connect(address);
                } catch (IOException ex) {
                        logger.error("unable to setup test http server", ex);
                }
        }

        @After
        public void cleanup() {
                try {
                        connection.close();
                } catch (IOException ex) {
                }
        }

        /**
         * Check PortalUser instance matches the constants in this class
         * @param p
         * @return
         */
        private boolean checkPortalUser() {
                PortalUser p = ((MockGeoNetworkXmlSelfRegisterService) container).getLastUserCreated();
                return
                        p != null &&
                        p.getAddress() != null && p.getAddress().equals(USER_ADDRESS) &&
                        p.getCountry() != null && p.getCountry().equals(USER_COUNTRY) &&
                        p.getEmail() != null && p.getEmail().equals(USER_EMAIL) &&
                        p.getFirstName() != null && p.getFirstName().equals(USER_NAME) &&
                        p.getLastName() != null && p.getLastName().equals(USER_SURNAME) &&
                        p.getOrganisation() != null && p.getOrganisation().equals(USER_ORGANISATION) &&
                        p.getState() != null && p.getState().equals(USER_STATE) &&
                        p.getUsername() != null && p.getUsername().equals(USER_EMAIL) &&
                        p.getZip() != null && p.getZip().equals(USER_ZIP);
        }

        /**
         * Add the same user twice - must fail
         */
        @Test
        public void duplicateUser() {

                // step one - create good user - must succeed
                assertTrue(
                        registrationService.register(
                                USER_SURNAME,
                                USER_NAME,
                                USER_EMAIL,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.SUCCESS
                );
                assertTrue(checkPortalUser());

                // step two - do the same again - must fail with duplicate error
                assertTrue(
                        registrationService.register(
                                USER_SURNAME,
                                USER_NAME,
                                USER_EMAIL,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.FAIL_DUPLICATE
                );
                assertFalse(checkPortalUser());

        }

        /**
         * Add a new regular user - must succeed
         */
        @Test
        public void newUser() {
                assertTrue(
                        registrationService.register(
                                USER_SURNAME,
                                USER_NAME,
                                USER_EMAIL,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.SUCCESS
                );

                assertTrue(checkPortalUser());
        }

        /**
         * Add a new user with incomplete data - must all fail as incompletes
         */
        @Test
        public void incompleteUser() {

                // must fail - null surname
                assertTrue(
                        registrationService.register(
                                null,
                                USER_NAME,
                                USER_EMAIL,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.FAIL_INCOMPLETE
                );

                assertFalse(checkPortalUser());

                // must fail - null name
                assertTrue(
                        registrationService.register(
                                USER_SURNAME,
                                null,
                                USER_EMAIL,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.FAIL_INCOMPLETE
                );

                assertFalse(checkPortalUser());

                // must fail - nulll email
                assertTrue(
                        registrationService.register(
                                USER_SURNAME,
                                USER_NAME,
                                null,
                                USER_ADDRESS,
                                USER_STATE,
                                USER_ZIP,
                                USER_COUNTRY,
                                USER_ORGANISATION,
                                USER_KIND
                        ) == RegistrationService.FAIL_INCOMPLETE
                );

                assertFalse(checkPortalUser());
        }

    public MestWebServiceParameters getParameters() {
        return parameters;
    }

    public void setParameters(MestWebServiceParameters parameters) {
        this.parameters = parameters;
    }

    public RegistrationService getRegistrationService() {
        return registrationService;
    }

    public void setRegistrationService(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

        
}
