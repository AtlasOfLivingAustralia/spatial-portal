/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer;

import au.org.emii.portal.service.ForgottenPasswordService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author geoff
 */
public class ResetPasswordComposer extends UtilityComposer {

    /**
     * changeKey - form parameter, get set by afterCompose()
     */
    private String changeKey = null;

    /**
     * username - form parameter, gets set by afterCompose()
     */
    private String username = null;

    /**
     * Password - zk autowired
     */
    private Textbox password = null;

    /**
     * Confirm password - zk autowired
     */
    private Textbox confirmPassword = null;

    /**
     * MEST forgotten password service - spring autowired
     */
    private ForgottenPasswordService forgottenPasswordService = null;

    /**
     * Error message when passwords dont match - zk autowired
     */
    private Label errorPasswordsDontMatch = null;

    /**
     * Error message when password is too short - zk autowired
     */
    private Label errorPasswordTooShort = null;

    /**
     * Error message for expired or used change key - zk autowired
     */
    private Label errorChangeKeyExpired = null;

    /**
     * Error message for invalid user (perhaps user deleted between changekey
     * being granted and used?) - zk autowired
     */
    private Label errorInvalidUser = null;

    /**
     * Error message for unknown error while doing reset (eg mest down) - zk
     * autowired
     */
    private Label errorUnknown = null;

    /**
     * block containing the password input controls
     */
    private Div inputControls = null;

    /**
     * block containing the message to show when password change is complete
     */
    private Div completedMessage = null;

    /**
     * Minimum password length in characters
     */
    private final static int passwordMinLength = 6;

    /**
     * User clicked password change submit button
     */
    public void onClick$submit() {
        String passwordString = password.getValue();
        String confirmPasswordString = confirmPassword.getValue();

        // hide any current error messages
        errorChangeKeyExpired.setVisible(false);
        errorInvalidUser.setVisible(false);
        errorPasswordTooShort.setVisible(false);
        errorPasswordsDontMatch.setVisible(false);
        errorUnknown.setVisible(false);

        if (passwordString != null && passwordString.length() >= passwordMinLength) {
            if (passwordString.equals(confirmPasswordString)) {
                // all good- change the passwords

                switch (forgottenPasswordService.changePassword(username, changeKey, passwordString)) {
                    case ForgottenPasswordService.FAIL_EXPIRED:
                        errorChangeKeyExpired.setVisible(true);
                        break;
                    case ForgottenPasswordService.FAIL_INVALID:
                        errorInvalidUser.setVisible(true);
                        break;
                    case ForgottenPasswordService.FAIL_UNKNOWN:
                        logger.error(
                            String.format(
                                "Unknown error while changing password - is mest down?  Parameters are username=%s" +
                                "changeKey=%s password=%s",
                                username, changeKey, passwordString
                            )
                        );
                        errorUnknown.setVisible(true);
                        break;
                    case ForgottenPasswordService.SUCCESS:
                        completedMessage.setVisible(true);
                        inputControls.setVisible(false);
                        break;
                    default:
                        logger.error("unhandled status returned from password change");
                }
            } else {
                errorPasswordsDontMatch.setVisible(true);
            }
        } else {
            errorPasswordTooShort.setVisible(true);
        }
    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        // grab the form parameters - they will be gone when the AJAX updates
        // start getting sent
        username = Executions.getCurrent().getParameter("username");
        changeKey = Executions.getCurrent().getParameter("changeKey");
    }


    /**
     * Accessor for password minimum length - required so that .zul page can
     * access the variable to build an error
     * @return
     */
    public int getPasswordMinLength() {
        return passwordMinLength;
    }
  
}
