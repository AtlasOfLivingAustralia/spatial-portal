/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.service.RegistrationService;
import au.org.emii.portal.mest.webservice.MestWebService;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author geoff
 */
public class RegistrationServiceImpl extends MestWebService implements RegistrationService {


    @Override
    public int register(String lastName, String firstName, String email, String address, String state, String zip, String country, String organisation, String affiliation) {
 
        String serviceUri = parameters.selfRegistrationServiceUri();
        int status = FAIL_UNKNOWN;

        // setup web service parameters
        MultivaluedMap queryParams = new MultivaluedMapImpl();

        // mandatory parameters
        queryParams.add("surname", lastName);
        queryParams.add("name", firstName);
        queryParams.add("email", email);

        // optional parameters
        queryParams.add("address", address);
        queryParams.add("state", state);
        queryParams.add("zip", zip);
        queryParams.add("country", country);
        queryParams.add("org", organisation);
        queryParams.add("kind", affiliation);

        // profile parameter - manditory but should always be 'RegisteredUser'
        queryParams.add("profile", parameters.getNewUserProfile());

        // Template to ask the MEST to use when emailing the user to let them
        // know their password
        queryParams.add("template", parameters.getEmailTemplateRegistration());

        xmlWebService.makeRequest(serviceUri, queryParams);

        if (xmlWebService.isResponseXml()) {
            String errorMessage = xmlWebService.parseString("//response/result/text()");
            if (errorMessage != null && errorMessage.equals(parameters.getMestConfiguration().getTokenDuplicateUser())) {
                // tokenDuplicateUser matches the the contents of the
                // /response/result/text() element to indicate a duplicate user
                // has been added.  Normally 'errorEmailAddressAlreadyRegistered'
                status = FAIL_DUPLICATE;
            } else {
                String replySurname = xmlWebService.parseString("response/@surname");
                String replyName = xmlWebService.parseString("response/@name");
                String replyEmail = xmlWebService.parseString("response/@email");
                String replyUsername = xmlWebService.parseString("response/@username");

                // check the response matches what we sent
                if (replySurname != null && replySurname.equals(lastName)
                        && replyName != null && replyName.equals(firstName)
                        && replyEmail != null && replyEmail.equals(email)
                        && replyUsername != null && replyUsername.equals(email)) {

                    // all good
                    status = SUCCESS;
                } else {
                    // hmm registration looks like it succeeded but doesn't match the criteria
                    // for success - unknown error (default status) and log the result
                    logger.error(
                            "Unknown response from registration system at '" + serviceUri + "': '"
                            + xmlWebService.getRawResponse() + "'");
                }
            }
        } else {
            // non-xml error - normally indicates missing data
            status = FAIL_INCOMPLETE;
        }
        return status;
    }
}
