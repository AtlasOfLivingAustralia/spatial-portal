/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer.admin;

import au.org.emii.portal.authentication.LoginService;
import au.org.emii.portal.authentication.LogoutService;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;

import java.util.List;
import javax.annotation.Resource;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author geoff
 */
public class AdminConsoleComposer extends GenericAutowireAutoforwardComposer {

    /**
     * Main display - zk autowired
     */
    private Center display = null;

    /**
     * Textbox for username - zk autowired
     */
    private Textbox username = null;

    /**
     * Textbox for password - zk autowired
     */
    private Textbox password = null;

    /**
     * Error message when incorrect username/password sent - zk autowired
     */
    private Label errorInvalid = null;

    /**
     * Error message when login system encounters unknown error - zk autowired
     */
    private Label errorUnknown = null;

    /**
     * The login screen for logged out/non-admin users - zk autowired
     */
    private Borderlayout loginScreen = null;

    /**
     * The normal menu for logged in admin users - zk autowired
     */
    private Borderlayout welcomeScreen = null;

    /**
     * Spring autowired login service - authenticates against properties file
     * instead of mest.
     *
     * Why?  So that we can login if mest is down (so we can put up notices, etc)
     *
     * Note: name of variable has to match bean id in spring config file and
     * setters/getters have to exist, otherwise the wrong bean can get injected
     */
    private LoginService adminLoginService = null;

    /**
     * Logout service (session destroyer)
     */
    private LogoutService logoutService = null;

    public LoginService getAdminLoginService() {
        return adminLoginService;
    }

    public void setAdminLoginService(LoginService adminLoginService) {
        this.adminLoginService = adminLoginService;
    }

    public LogoutService getLogoutService() {
        return logoutService;
    }

    public void setLogoutService(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    /**
     * Switch to motd
     */
    public void onClick$motd() {
        loadZul("/WEB-INF/zul/admin/MOTD.zul");
    }

    /**
     * Switch to config editor
     */
    public void onClick$config() {
        loadZul("/WEB-INF/zul/admin/Config.zul");
    }

    /**
     * Switch to log viewer
     */
    public void onClick$log() {
        loadZul("/WEB-INF/zul/admin/Log.zul");
    }

    /**
     * Switch to user editor
     */
    public void onClick$user() {
        loadZul("/WEB-INF/zul/admin/User.zul");
    }

    private void loadZul(String filename) {
        // remove any existing children
        List children = display.getChildren();
        if (children != null) {
            while (children.size() > 0) {
                ((AbstractComponent) children.get(0)).detach();
            }
        }

        Window window = (Window) Executions.createComponents(filename, null, null);
        window.setParent(display);
    }

    public void onClick$logout() {
        logoutService.logout(getPortalSession());
        toggleLoginScreen(true);
    }

    private void toggleLoginScreen(boolean showLogin) {
        welcomeScreen.setVisible(! showLogin);
        loginScreen.setVisible(showLogin);

        // remove existing username/password when toggling screens!
        username.setValue(null);
        password.setValue(null);
    }

    public void onClick$login() {
        logger.debug("login");
        hideMessages();
        String usernameValue = username.getValue();
        String passwordValue = password.getValue();

        switch (adminLoginService.login(getPortalSession(), usernameValue, passwordValue)) {
            case LoginService.SUCCESS:
                // hide the login window, show the welcome screen
                toggleLoginScreen(false);
                break;
            case LoginService.FAIL_INVALID:
                // wrong username/password
                logger.debug("invalid username/password");
                errorInvalid.setVisible(true);
                break;
            case LoginService.FAIL_UNKNOWN:
                // dunno - loggin system is having problems
                logger.debug("login error");
                errorUnknown.setVisible(true);
                break;
            default:
                logger.error("unexpected value logging into portal admin console");
        }

    }

    private void hideMessages() {
        errorInvalid.setVisible(false);
        errorUnknown.setVisible(false);
    }

}
