/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.authentication;

import au.org.emii.portal.mest.webservice.MestWebService;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author geoff
 */
public class ForgottenPasswordServiceImpl extends  MestWebService implements ForgottenPasswordService {


    /**
     * The logic to check both the email generation and password change request is
     * identical so can be shared.
     * @return
     */
    private int checkResult() {
        int result;

        if (xmlWebService.isResponseXml()) {
            if (xmlWebService.parseNode("//response") != null) {
                result = ForgottenPasswordService.SUCCESS;
            } else {
                result = ForgottenPasswordService.FAIL_UNKNOWN;
            }
        } else if (xmlWebService.getRawResponse() == null) {
            // must check the raw response is set to avoid npe when checking its
            // contents later
            result = ForgottenPasswordService.FAIL_UNKNOWN;
        } else if (xmlWebService.getRawResponse().contains(
                parameters.getMestConfiguration().getTokenInvalidUser())) {
            // duplicate user response generates an xml-style error but without
            // setting the correct mime type (uses text/html) so we just look for
            // the duplicate user error message
            result = ForgottenPasswordService.FAIL_INVALID;
        } else if (xmlWebService.getRawResponse().contains(
                parameters.getMestConfiguration().getTokenExpiredChangeKey())) {
            // changeKey already used or expired - harmless extra check for email change
            // key result check - will never evaluate true for this phase.
            result = ForgottenPasswordService.FAIL_EXPIRED;
        }
        else {
            result = ForgottenPasswordService.FAIL_UNKNOWN;
        }
        
        xmlWebService.close();
        return result;
    }

    @Override
    public int requestReset(String username) {

        // make the request
        xmlWebService.makeRequest(
                parameters.resetPasswordServiceUri(),
                makeQueryParamsEmail(username));

        return checkResult();
    }

    @Override
    public int changePassword(String username, String changeKey, String password) {
      
        // make the request
        xmlWebService.makeRequest(
                parameters.changePasswordServiceUri(),
                makeQueryParamsChange(username, password, changeKey));

        return checkResult();
    }


    /**
     * Query parameters for requesting the email with the changeKey (first email)
     * @param username
     * @return
     */
    private MultivaluedMap makeQueryParamsEmail(String username) {
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        queryParams.add("template", parameters.getEmailTemplateResetPassword());
        return queryParams;
    }

    /**
     * Query parameters for performing the password change.  Also sends the user
     * an email, output of which is controlled by the template parameter
     * @param username
     * @param password
     * @param changeKey
     * @return
     */
    private MultivaluedMap makeQueryParamsChange(String username, String password, String changeKey) {
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        queryParams.add("password", password);
        queryParams.add("changeKey", changeKey);
        queryParams.add("template", parameters.getEmailTemplatePasswordChanged());
        return queryParams;
    }
}
