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
     * MEST template file used to generate the email that get's sent to the user
     */
    private String templateFile = null;
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
        xmlWebService.makeRequest(serviceUri, makeQueryParamsChange(username, password, changeKey, templateFile));

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

    public String getTemplateFile() {
        return templateFile;
    }

    @Required
    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public String getExpiredChangeKeyErrorMessage() {
        return expiredChangeKeyErrorMessage;
    }

    @Required
    public void setExpiredChangeKeyErrorMessage(String expiredChangeKeyErrorMessage) {
        this.expiredChangeKeyErrorMessage = expiredChangeKeyErrorMessage;
    }
   

    /**
     * Query parameters for sending the email
     * @param username
     * @return
     */
    private MultivaluedMap makeQueryParamsEmail(String username) {
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        return queryParams;
    }

    /**
     * Query parameters for performing the password change
     * @param username
     * @param password
     * @param changeKey
     * @return
     */
    private MultivaluedMap makeQueryParamsChange(String username, String password, String changeKey, String templateFile) {
        MultivaluedMap queryParams = new MultivaluedMapImpl();
        queryParams.add("username", username);
        queryParams.add("password", password);
        queryParams.add("changeKey", changeKey);
        queryParams.add("template", templateFile);
        return queryParams;
    }
}
