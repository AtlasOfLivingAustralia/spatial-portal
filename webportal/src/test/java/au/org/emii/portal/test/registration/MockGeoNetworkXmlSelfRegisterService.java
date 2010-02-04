/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.test.registration;

import au.org.emii.portal.test.user.MockGeoNetworkXmlUserListService;
import au.org.emii.portal.user.PortalUser;
import au.org.emii.portal.user.PortalUserImpl;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

/**
 *
 * @author geoff
 */
public class MockGeoNetworkXmlSelfRegisterService implements Container {
        private final static String SUCCEED_OUTPUT = "test_data/xml.self.register-succeed";
        private final static String FAIL_DUPLICATE_OUTPUT = "test_data/xml.self.register-fail_duplicate";
        private final static String FAIL_INCOMPLETE_OUTPUT = "test_data/xml.self.register-fail_incomplete";
        private final static String XML_MIME_TYPE = "application/xml";

        private List<String> accountsCreated = new ArrayList<String>();
        private PortalUser lastUserCreated = null;

        private boolean testParams(String surname, String name, String email, String profile) {
                return surname != null &&
                        name != null &&
                        email != null &&
                        profile != null;
        }

        private boolean checkForDuplicates(String email) {
                boolean duplicate = false;
                for (String string : accountsCreated) {
                        if (string.equals(email)) {
                                duplicate = true;
                        }
                }
                return duplicate;
        }

        @Override
        public void handle(Request request, Response response) {
                PrintStream out = null;
                String responseFile = null;
                String responseText = null;
                
                // erase any previous status
                lastUserCreated = null;
                try {
                        String surname = request.getParameter("surname");
                        String name = request.getParameter("name");
                        String email = request.getParameter("email");
                        String profile = request.getParameter("profile");
                        String country = request.getParameter("country");
                        String address = request.getParameter("address");
                        String organisation = request.getParameter("organisation");
                        String state = request.getParameter("state");
                        String zip = request.getParameter("zip");
                        
                        if (testParams(surname, name, email, profile)) {
                                if (checkForDuplicates(email)) {
                                        // its a dup -error
                                        response.set("Content-Type", XML_MIME_TYPE);
                                        responseFile = FAIL_DUPLICATE_OUTPUT;
                                } else {
                                        // all good...
                                        response.set("Content-Type", XML_MIME_TYPE);
                                        responseFile = SUCCEED_OUTPUT;

                                        responseText  =
                                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                "<response surname=\"" + surname + "\" " +
                                                "name=\"" + name + "\" " +
                                                "email=\"" + email + "\" " +
                                                "username=\"" + email +"\" />\n";



                                        // store the account in the list so we can
                                        // have tests check for duplicates
                                        accountsCreated.add(email);

                                        // create the last user information
                                        lastUserCreated = new PortalUserImpl();
                                        lastUserCreated.setLastName(surname);
                                        lastUserCreated.setAddress(address);
                                        lastUserCreated.setCountry(country);
                                        lastUserCreated.setEmail(email);
                                        lastUserCreated.setFirstName(name);
                                        lastUserCreated.setOrganisation(organisation);
                                        lastUserCreated.setState(state);
                                        lastUserCreated.setUsername(email);
                                        lastUserCreated.setZip(zip);

                                }
                        } else {
                                // its incomplete -
                                responseFile = FAIL_INCOMPLETE_OUTPUT;
                        }

                        // flush output to client
                        out = response.getPrintStream();
                        if (responseText != null) {
                                // response from string concatenation
                                out.print(responseText);
                        } else {
                                // canned error message
                                out.print(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(responseFile)));
                        }
                } catch (IOException ex) {
                        Logger.getLogger(MockGeoNetworkXmlUserListService.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                        out.close();
                }
        }

        public PortalUser getLastUserCreated() {
                return lastUserCreated;
        }

        public void setLastUserCreated(PortalUser lastUserCreated) {
                this.lastUserCreated = lastUserCreated;
        }
        

}
