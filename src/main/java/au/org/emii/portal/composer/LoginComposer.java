/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.util.Validate;
import au.org.emii.portal.service.ForgottenPasswordService;
import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.util.IsoCountries;
import au.org.emii.portal.databinding.CountryListModel;
import au.org.emii.portal.service.RegistrationService;

import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

/**
 * Composer for Login.zul - handles logins and registrations
 * @author geoff
 */
public class LoginComposer extends UtilityComposer {

    /**
     * Username for existing login - zk autowired
     */
    private Textbox loginEmailUsername = null;
    /**
     * Password for existing login - zk autowired
     */
    private Textbox loginPassword = null;
    /**
     * Email address - new user - zk autowired
     */
    private Textbox createEmailUsername = null;
    /**
     * First name - new user - zk autowired
     */
    private Textbox createFirstName = null;
    /**
     * Last name - new user - zk autowired
     */
    private Textbox createLastName = null;
    /**
     * Address - new user - zk autowired
     */
    private Textbox createAddress = null;
    /**
     * State - new user - zk autowired
     */
    private Textbox createState = null;
    /**
     * Zip code - new user - zk autowired
     */
    private Textbox createPostZip = null;
    /**
     * Country - new user - zk autowired.
     * Will create an autocomplete combox box
     */
    private Combobox createCountry = null;
    /**
     * Organisation - new user - zk autowired
     */
    private Textbox createOrgDept = null;
    
    /**
     * Kind (== affiliation) - new user - zk autowired
     */
    private Listbox createKind = null;

    /**
     * Login service - spring autowired
     */
    private LoginService portalUserLoginService = null;
    /**
     * Registration service - spring autowired
     */
    private RegistrationService registrationService = null;

    /**
     * Message to show when the user logged in with an invalid email/password.
     * zk autowired
     */
    private Div loginInvalidMessage = null;

    /**
     * Message to show new users after successful registration - zk autowired
     */
    private Div loginBoxNewUserSuccess = null;

    /**
     * Message to show new users when email address being registered is invalid
     * zk autowired
     */
    private Label createEmailUsernameInvalid = null;

    /**
     * Message to show new users when first name is invalid - zk autowired
     */
    private Label createFirstNameInvalid = null;

    /**
     * Message to show users when last name is invalid - zk autowrired
     */
    private Label createLastNameInvalid = null;

    /**
     * Message to show users when the requested user is already registered
     */
    private Label duplicateUserError = null;

    /**
     * Message to show users when an error occurred while registering, eg MEST
     * server down, unknown error, etc
     */
    private Label registrationError = null;

    /**
     * Tab container for login/register tabs - zk autowired
     */
    private Tabbox loginBoxMenu = null;

    /**
     * Login tab - zk autowired
     */
    private Tab loginTab = null;

    /**
     * Registration tab - zk autowired
     */
    private Tab registerTab = null;

    /**
     * Link to get password emailed out.  Gets disabled immediately after a
     * successful registration - zk autowired.
     */
    private Toolbarbutton emailPasswordButton = null;

    /**
     * Regular instructions for login system.  Gets disabled immediately after a
     * successful registration and replaced with registration specific instructions
     * (loginBoxNewUserSuccess) - zk autowired
     */
    private Label loginInstructions = null;


    /**
     * Forgotten password service (MEST) - spring autowrired
     */
    private ForgottenPasswordService forgottenPasswordService = null;

    /**
     * Error message to show when login service is down - zk autowired
     */
    private Div loginErrorMessage = null;

    /**
     * Instructions to show after a successful password reset request - zk autowired
     */
    private Label forgottenPasswordInstructions = null;

    /**
     * Error message - internal error - forgotten password - zk autowired
     */
    private Div forgottenPasswordErrorMessage = null;

    /**
     * Error message - user doesn't exist - forotten password - zk autowired
     */
    private Div forgottenPasswordInvalidUserErrorMessage = null;
    
    /**
     * Error message - user not specified - forgotten password - zk autowired
     */
    private Div forgottenPasswordMissingUsername = null;
    
    

    private IsoCountries isoCountries = null;

    /**
     * Initialise the form
     */
    private void load() {

        // load the countries list
        createCountry.setModel(new CountryListModel(isoCountries.getCountries()));

    }

    //-- AfterCompose --//
    @Override
    public void afterCompose() {
        super.afterCompose();

        // showtime!
        load();
    }

    /**
     * FIXME! Write actions to perform on login here - eg load portalsession
     * from hibernate
     * @param password 
     * @param username 
     */

    private void loginActions(String username, String password) {
                
        // hide any error message before removing window
        loginInvalidMessage.setVisible(false);
        
        getMapComposer().loginActions();  // login actions for all the site        

    }

    /**
     * Actions to perform on a successful registration, currently:
     * 1) switch to login tab
     * 2) hide "email my password" button/link
     * 3) display message "check your email..."
     * 4) populate username field
     * 5) Hide the normal login instructions
     */
    private void registrationActions() {
        loginBoxMenu.setSelectedTab(loginTab);
        emailPasswordButton.setVisible(false);
        loginBoxNewUserSuccess.setVisible(true);
        loginEmailUsername.setValue(createEmailUsername.getValue());
        loginInstructions.setVisible(false);

    }
    
    public void onClick$emailPasswordButton() {
        String username = loginEmailUsername.getValue();
        hideRegistrationMessages();
        hideLoginErrors();
        hideForgottenPasswordErrorsAndInstructions();
        loginInstructions.setVisible(true);
        if (Validate.empty(username)) {
            logger.debug("username empty - doing nothing");
            forgottenPasswordMissingUsername.setVisible(true);
        } else {
            logger.debug("sending reset request");
            switch (forgottenPasswordService.requestReset(username)) {
                case ForgottenPasswordService.SUCCESS:
                    logger.debug("reset request sent");
                    forgottenPasswordInstructions.setVisible(true);
                    loginInstructions.setVisible(false);
                    break;
                case ForgottenPasswordService.FAIL_INVALID:
                    forgottenPasswordInvalidUserErrorMessage.setVisible(true);
                    break;
                case ForgottenPasswordService.FAIL_UNKNOWN:
                    forgottenPasswordErrorMessage.setVisible(true);
                    break;
                default:
                    forgottenPasswordErrorMessage.setVisible(true);
                    logger.error("forgotten password service requestReset() returned invalid status");
            }
        }
    }


    /**
     * Login - button click
     */
    public void onClick$loginSubmitButton() {
        logger.debug("login requested");

        hideForgottenPasswordErrorsAndInstructions();
        hideLoginErrors();
        hideRegistrationMessages();
        loginInstructions.setVisible(true);

        String username = loginEmailUsername.getValue();
        String password = loginPassword.getValue();

        if ((! Validate.empty(username)) && (! Validate.empty(password))) {
            logger.debug("loging as: " + username + ":" + password);
            switch (portalUserLoginService.login(getPortalSession(), username, password)) {
                case LoginService.SUCCESS:
                    // login ok - close window
                    logger.debug("login finished, login ok");
                    loginActions(username, password);
                    detach();
                    break;
                case LoginService.FAIL_INVALID:
                    // leave window open, let user try again - hide any messages from
                    // the registration system
                    loginInvalidMessage.setVisible(true);
                    logger.debug("login finished, wrong username/password");
                    break;
                case LoginService.FAIL_UNKNOWN:
                    logger.error("User login failed with unknown status - is MEST down?");
                    loginErrorMessage.setVisible(true);
                    break;
                default:
                    logger.error("loginService.login() is returning an invalid status");

            }

        } else {
            // no username or password was entered - service will return an error
            // if this happens so just dont send the request and tell user to sort it out
            loginInvalidMessage.setVisible(true);
        }

    }


    /**
     * Validate all fields are correct for new user registrations
     */
    private boolean validateNewUser(String lastName,
                                    String firstName,
                                    String email,
                                    String address,
                                    String state,
                                    String zip,
                                    String country,
                                    String organisation,
                                    String affiliation) {
        boolean valid = true;

        // required fields are email, first name and last name - any empties trigger error

        // last name
        if (Validate.empty(lastName)) {
            valid = false;
            createLastNameInvalid.setVisible(true);
        } else {
            createLastNameInvalid.setVisible(false);
        }

        // first name
        if (Validate.empty(firstName)) {
            valid = false;
            createFirstNameInvalid.setVisible(true);
        } else {
            createFirstNameInvalid.setVisible(false);
        }

        // email
        if (Validate.empty(email)) {
            valid = false;
            createEmailUsernameInvalid.setVisible(true);
        } else if (! Validate.email(email)) {
            // extra check for email address - conform to RFC2822
            valid = false;
            createEmailUsernameInvalid.setVisible(true);
        } else {
            createEmailUsernameInvalid.setVisible(false);
        }

        // rest of fields - not really bothered since it's all optional

        return valid;

    }

    /**
     * Registration - button click
     */
    public void onClick$registrationSubmitButton() {
        logger.debug("start register new user");
        String lastName = createLastName.getValue();
        String firstName = createFirstName.getValue();
        String email = createEmailUsername.getValue();
        String address = createAddress.getValue();
        String state = createState.getValue();
        String zip = createPostZip.getValue();
        String country = (createCountry.getSelectedItem() == null) ?
            null : (String) createCountry.getSelectedItem().getValue();
        String organisation = createOrgDept.getValue();
        String affiliation =
                (createKind.getSelectedItem() == null) ? "" : (String) createKind.getSelectedItem().getValue();

        if (validateNewUser(lastName, firstName, email, address, state, zip, country, organisation, affiliation)) {
            logger.debug("user registration parameters valid, attempting to self register user");

            // hide any existing error messages
            duplicateUserError.setVisible(false);
            registrationError.setVisible(false);

            switch (registrationService.register(lastName, firstName, email, address, state, zip, country, organisation, affiliation)) {
                case RegistrationService.FAIL_DUPLICATE:
                    logger.info("refusing to register duplicate user: " + email);
                    duplicateUserError.setVisible(true);
                    break;
                case RegistrationService.FAIL_INCOMPLETE:
                    logRegistrationError("incomplete registration - should never happen.", lastName, firstName, email, address, state, zip, country, organisation, affiliation);
                    registrationError.setVisible(true);
                    break;
                case RegistrationService.SUCCESS:
                    logger.debug("register new user ok: " + email);
                    registrationActions();
                    break;
                case RegistrationService.FAIL_UNKNOWN:
                    logRegistrationError("unknown error from registration system - check mest up?", lastName, firstName, email, address, state, zip, country, organisation, affiliation);
                    registrationError.setVisible(true);
                    break;
                default:
                    logRegistrationError("invalid return state reported for registrationService.register()", lastName, firstName, email, address, state, zip, country, organisation, affiliation);
                    registrationError.setVisible(true);
            }
        } else {
            logger.debug("not processing - new user is invalid");
        }
        logger.debug("leaving add a user");
    }

    /**
     * Log full details of registration error
     */
    private void logRegistrationError(  String errorMessage,
                                        String lastName,
                                        String firstName,
                                        String email,
                                        String address,
                                        String state,
                                        String zip,
                                        String country,
                                        String organisation,
                                        String affiliation) {
        logger.error(
            String.format(
                "%s " + 
                "Registration data was: " +
                "lastName='%s', " +
                "firstname='%s', " +
                "email='%s', " +
                "address='%s', " +
                "state='%s', " +
                "zip='%s', " +
                "country='%s', " +
                "organisation='%s', " +
                "affiliation='%s'",
                errorMessage, lastName, firstName, email, address, state, zip, country, organisation, affiliation
            )
        );
    }

    private void hideForgottenPasswordErrorsAndInstructions() {
        forgottenPasswordInstructions.setVisible(false);
        forgottenPasswordErrorMessage.setVisible(false);
        forgottenPasswordInvalidUserErrorMessage.setVisible(false);
        forgottenPasswordMissingUsername.setVisible(false);
    }

    private void hideLoginErrors() {
        loginErrorMessage.setVisible(false);
        loginInvalidMessage.setVisible(false);
    }

    private void hideLoginErrorsAndInstructions() {
        hideLoginErrors();
        loginInstructions.setVisible(false);
    }

    private void hideRegistrationMessages() {
        emailPasswordButton.setVisible(true);
        loginBoxNewUserSuccess.setVisible(false);
    }

    public IsoCountries getIsoCountries() {
        return isoCountries;
    }

    public void setIsoCountries(IsoCountries isoCountries) {
        this.isoCountries = isoCountries;
    }


}
