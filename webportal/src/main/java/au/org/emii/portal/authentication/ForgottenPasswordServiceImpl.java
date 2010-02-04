/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.authentication;

import au.org.emii.portal.webservice.UserManagementWebServiceEndpointImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author geoff
 */
public class ForgottenPasswordServiceImpl extends UserManagementWebServiceEndpointImpl implements ForgottenPasswordService {

    /**
     * Email (mest template filename) to send when user requests password reset.  This is the
     * first email that gets sent and includes the changeKey and a link to the reset_password.zul
     * file
     */
    private String resetEmailTemplate = null;

    /**
     * Confirmation email (mest template filename) to send when user has changed password.  This
     * is the final email that gets sent and just says to the effect of "your password has been
     * changed"
     */
    private String confirmEmailTemplate = null;
    
    /**
     * Error message indicating invalid user from MEST - typically UserNotFoundEx
     */
    private String invalidUserErrorMessage = null;

    /**
     * Error message indicating changeKey already used or expired from MEST - typically "Change key invalid"
     */
    private String expiredChangeKeyErrorMessage = null;

    @Override
    public int requestReset(String username) {
        return requestReset(getDefaultUserManagementServiceId(), username);
    }

    @Override
    public int changePassword(String username, String changeKey, String password) {
        return changePassword(getDefaultUserManagementServiceId(), username, changeKey, password);
    }


    public String getConfirmEmailTemplate() {
        return confirmEmailTemplate;
    }

    @Required
    public void setConfirmEmailTemplate(String confirmEmailTemplate) {
        this.confirmEmailTemplate = confirmEmailTemplate;
    }

    public String getResetEmailTemplate() {
        return resetEmailTemplate;
    }

    @Required
    public void setResetEmailTemplate(String resetEmailTemplate) {
        this.resetEmailTemplate = resetEmailTemplate;
    }


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
        } else if (xmlWebService.getRawResponse().contains(invalidUserErrorMessage)) {
            // duplicate user response generates an xml-style error but without
            // setting the correct mime type (uses text/html) so we just look for
            // the duplicate user error message
            result = ForgottenPasswordService.FAIL_INVALID_USER;
        } else if (xmlWebService.getRawResponse().contains(expiredChangeKeyErrorMessage)) {
            // changeKey already used or expired - harmless extra check for email change
            // key result check - will never evaluate true for this phase.
            result = ForgottenPasswordService.FAIL_EXPIRED;
        }
        else {
            result = ForgottenPasswordService.FAIL_UNKNOWN;
        }
        return result;
    }

    @Override
    public int requestReset(String service, String username) {
        serviceUri = getUserManagementWebServiceById(service).resetPasswordServiceUri();

        // make the request
        xmlWebService.makeRequest(serviceUri, makeQueryParamsEmail(username));

        int result = checkResult();
        xmlWebService.close();

        return result;
    }

    @Override
    public int changePassword(String service, String username, String changeKey, String password) {
        serviceUri = getUserManagementWebServiceById(service).changePasswordServiceUri();
        // make the request
        xmlWebService.makeRequest(serviceUri, makeQueryParamsChange(username, password, changeKey));

        int result = checkResult();
        xmlWebService.close();

        return result;
    }

    public String getInvalidUserErrorMessage() {
        return invalidUserErrorMessage;
    }

    @Required
    public void setInvalidUserErrorMessage(String invalidUserErrorMessage) {
        this.invalidUserErrorMessage = invalidUserErrorMessage;
    }


    public String getExpiredChangeKeyErrorMessage() {
        return expiredChangeKeyErrorMessage;
    }

    @Required
    public void setExpiredChangeKeyErrorMessage(String expiredChangeKeyErrorMessage) {
        this.expiredChangeKeyErrorMessage = expiredChangeKeyErrorMessage;
    }
   

    /**
     * Query parameters for requesting the email with the changeKey (first email)
     * @param username
     * @return
     */
    private MultivaluedMap makeQueryParamsEmail(String username) {
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        queryParams.add("template", resetEmailTemplate);
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
        queryParams.add("template", confirmEmailTemplate);
        return queryParams;
    }
}
