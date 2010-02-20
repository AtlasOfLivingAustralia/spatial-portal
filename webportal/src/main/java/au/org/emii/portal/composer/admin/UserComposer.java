/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer.admin;

import au.org.emii.portal.Validate;
import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.authentication.LoginServicePropertiesImpl;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Textbox;

/**
 *
 * @author geoff
 */
public class UserComposer extends GenericAutowireAutoforwardComposer {

    /**
     * Minium allowed length for password
     */
    private final static int PASSWORD_MIN_LENGTH = 6;

    /**
     * Required by zk to do the databinding.  This replaces the init-class directive
     * in the .zul file and allows the page to be requested more than once
     */
    private AnnotateDataBinder binder;

    /**
     * Handle to the list of User instances contained within the model, to allow
     * easy serialisation to the properties file when its time to save
     */
    private List<User> users = null;

    /**
     * Listbox of all users - zk autowired and annotation databound :-D
     */
    private Listbox userListbox = null;

    /**
     * Model of data to show in the listbox.  This swallows the users variable
     * and any modifications to the users list variable must be made through
     * model.
     */
    private ListModelList model = null;

    /**
     * Spring autowired login service - authenticates against properties file
     * instead of mest.
     *
     * Why?  So that we can login if mest is down (so we can put up notices, etc)
     *
     * Note: name of variable has to match bean id in spring config file and
     * setters/getters have to exist, otherwise the wrong bean can get injected
     */
    private LoginServicePropertiesImpl adminLoginService = null;

    /**
     * username control for CRUDing - zk autowired
     */
    private Textbox username = null;
    
    /**
     * password control for CRUDing - zk autowired
     */
    private Textbox password = null;

    /**
     * confirm password control for CRUDing - zk autowired
     */
    private Textbox confirmPassword = null;

    /**
     * Error message for empty username - zk autowired
     */
    private Label errorUsername = null;

    /**
     * Error message for empty password(s) - zk autowired
     */
    private Label errorPassword = null;

    /**
     * Error message for passwords not matching - zk autowired
     */
    private Label errorPasswordMatch = null;

    /**
     * Error saving passwords to properties file - zk autowired
     */
    private Label errorSaving = null;

    /**
     * Success message for properties file saved - zk autowired
     */
    private Label successMessage = null;

    /**
     * Username/password changed for user message - zk autowired.  This is to give the
     * user visual feedback that something has happened when they clicked update
     */
    private Label successUserUpdated = null;

    /**
     * Error message to display when no user has been selected - zk autowired
     */
    private Label errorNoSelection = null;

    /**
     * Error message for when password is too short - zk autowired
     */
    private Label errorPasswordTooShort = null;
    
    /**
     * Accessor for .zul page to read the model
     * @return
     */
    public ListModelList getModel() {
        return model;
    }

    public LoginService getAdminLoginService() {
        return adminLoginService;
    }

    public void setAdminLoginService(LoginServicePropertiesImpl adminLoginService) {
        this.adminLoginService = adminLoginService;
    }

    /**
     * Remove the selected user from listbox, then redraw list and empty form controls
     */
    public void onClick$delete() {
        User user = getSelectedUser();
        if (user == null) {
            errorNoSelection.setVisible(true);
        } else {
            model.remove(user);
            userListbox.setModel(model);
        }
    }

    /**
     * Update either the username/password.  The password field can be left blank
     * to just change the username
     */
    public void onClick$update() {
        User user = getSelectedUser();
        if (user == null) {
            errorNoSelection.setVisible(true);
        } else {
            String usernameValue = username.getValue();
            String passwordValue = password.getValue();
            hideMessages();
            // allow empty passwords but if set, they must match
            if (validate(true)) {
                // update the username
                user.setUsername(usernameValue);

                // check if password was changed too
                if (! Validate.empty(passwordValue)) {
                    user.setPassword(
                        adminLoginService.formatForProperties(
                            adminLoginService.hashWithSalt(passwordValue)
                    ));
                }

                successUserUpdated.setVisible(true);

                // update the listbox incase the username was changed
                userListbox.setModel(model);
            }
        }
    }

    /**
     * Create a new user with given username and password
     */
    public void onClick$create() {
        String usernameValue = username.getValue();
        String passwordValue = password.getValue();
        hideMessages();
        if (validate(false)) {
            User user = new User(
                    usernameValue,
                    adminLoginService.formatForProperties(adminLoginService.hashWithSalt(passwordValue)));
            model.add(user);

            // tell listbox to re-render
            userListbox.setModel(model);

        }

    }

    /**
     * Get the user currently selected in the userListbox.  Warning: can be null!
     * @return
     */
    private User getSelectedUser() {
        return (userListbox.getSelectedItem() == null) ?
            null : (User) userListbox.getSelectedItem().getValue();
    }

    /**
     * When a user is selected in the list, grab the value object represented at
     * this location (User instance) and use it to populate the control panel where
     * the username and password can be updated or the user deleted
     */
    public void onSelect$userListbox() {
        logger.debug("user selected");
        User selected = getSelectedUser();

        // put the selected user into the edit user panel.  Empty the password
        // fields since there's no point giving the hash to the user
        username.setValue(selected.getUsername());
        password.setValue(null);
        confirmPassword.setValue(null);


    }

    /**
     * Hide any existing status messages
     */
    private void hideMessages() {
        successMessage.setVisible(false);
        successUserUpdated.setVisible(false);
        errorNoSelection.setVisible(false);
        errorPassword.setVisible(false);
        errorPasswordMatch.setVisible(false);
        errorSaving.setVisible(false);
        errorUsername.setVisible(false);
        errorPasswordTooShort.setVisible(false);
    }

    /**
     * Validate required fields have been set and that passwords match
     * @param allowEmptyPassword when true, allow passwords to be empty (=no change) but if
     * password has been set, it must still match the confirmPassword field
     * @return true if the form request is valid otherwise false
     */
    private boolean validate(boolean allowEmptyPassword) {
        String usernameValue = username.getValue();
        String passwordValue = password.getValue();
        String confirmPasswordValue = confirmPassword.getValue();
        boolean valid = true;
        if (Validate.empty(usernameValue)) {
            valid = false;
            errorUsername.setVisible(true);
        }

        if ((! allowEmptyPassword) && Validate.empty(passwordValue) && Validate.empty(confirmPasswordValue)) {
            valid = false;
            errorPassword.setVisible(true);
        }

        if (! passwordValue.equals(confirmPasswordValue)) {
            valid = false;
            errorPasswordMatch.setVisible(true);
        }

        if (passwordValue.length() > 0 && passwordValue.length() < PASSWORD_MIN_LENGTH) {
            valid = false;
            errorPasswordTooShort.setVisible(true);
        }

        return valid;
    }

    /**
     * Persist the Model to the correct properties file
     */
    private void save() {
        hideMessages();
        Properties properties = new Properties();
        for (User user : users) {
            String usernameValue = user.getUsername();
            String passwordValue = user.getPassword();
            logger.debug(String.format("user %s password %s", usernameValue, passwordValue));
            properties.put(usernameValue, passwordValue);
        }

        if (adminLoginService.updateProperties(properties, getPortalSession().getPortalUser().getUsername())) {
            successMessage.setVisible(true);
        } else {
            errorSaving.setVisible(true);
        }
    }


    @Override
    public void afterCompose() {
        super.afterCompose();

        // init the model
        makeModel();

        // init the binder
        binder = new AnnotateDataBinder(this);
        binder.loadAll();
    }



    public void onClick$save() {
        save();
    }

    private void makeModel() {
        users = new ArrayList<User>();
        Properties userProperties =  adminLoginService.getUsers();

        Enumeration keys = userProperties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            users.add(new User(key,userProperties.getProperty(key)));
        }

        model = new ListModelList(users, true);
    }

    public class User {
        private String username = null;
        private String password = null;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
        
    }

    /**
     * Accessor for password minimum length - required so that .zul page can
     * access the variable to build an error
     * @return
     */
    public int getPasswordMinLength() {
        return PASSWORD_MIN_LENGTH;
    }

}
