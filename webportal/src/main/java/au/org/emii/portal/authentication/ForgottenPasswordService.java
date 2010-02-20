/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.authentication;

import au.org.emii.portal.service.ServiceStatusCodes;

/**
 *
 * @author geoff
 */
public interface ForgottenPasswordService extends ServiceStatusCodes {

    /**
     * Request that a password is reset - this generates a one-time change key
     * which is emailed to the user to verfify identity.  The change key will
     * be required to actually change the password.  Request is made to the
     * default service
     * @param username username to reset password for
     * @return integer constant, as defined in ForgottenPasswordService
     */
    public int requestReset(String username);

    /**
     * Change the password for a given user.  For this to work, they must have a
     * valid change key, generated by a previous call to requestReset().  Request
     * is made to the default service
     * @param username username to change password for
     * @param changeKey one time change key - found in the email sent to the user
     * @param password new password
     * @return integer constant, as defined in ForgottenPasswordService
     */
    public int changePassword(String username, String changeKey, String password);

}
