/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer.admin;

import au.org.emii.portal.motd.MOTD;
import au.org.emii.portal.Validate;
import au.org.emii.portal.composer.GenericAutowireAutoforwardComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author geoff
 */
public class MOTDEditComposer extends GenericAutowireAutoforwardComposer {

    /**
     * Enabled/disabled checkbox - zk autowired
     */
    private Checkbox enableMotd = null;

    /**
     * Title input - zk autowired
     */
    private Textbox title = null;

    /**
     * Message input - zk autowired
     */
    private Textbox message = null;

    /**
     * Error message - title empty - zk autowired
     */
    private Label errorTitle = null;

    /**
     * Error message - message content empty - zk autowired
     */
    private Label errorMessage = null;

    /**
     * Error message for when there was an error saving properties - zk autowired
     */
    private Label errorUnknown = null;

    /**
     * Message for when properties were saved OK
     */
    private Label successMessage = null;

    private MOTD motd = null;

    /**
     * Hide all messages
     * @return
     */
    private void hideMessages() {
        errorTitle.setVisible(false);
        errorMessage.setVisible(false);
        errorUnknown.setVisible(false);
        successMessage.setVisible(false);
    }

    /**
     * Validate title and message - display error messages if required
     * @param title
     * @param message
     * @return
     */
    private boolean validate(String title, String message) {
        boolean valid = true;
        if (Validate.empty(title)) {
            errorTitle.setVisible(true);
            valid = false;
        }

        if (Validate.empty(message)) {
            errorMessage.setVisible(true);
            valid = false;
        }

        return valid;
    }

    /**
     * Save button clicked
     */
    public void onClick$save() {
        boolean enabledValue = enableMotd.isChecked();
        String titleValue = title.getValue();
        String messageValue = message.getValue();

        hideMessages();
        if (validate(titleValue, messageValue)) {
            if (motd.updateMotd(enabledValue, titleValue, messageValue, getPortalSession().getPortalUser().getUsername())) {
                successMessage.setVisible(true);
            } else {
                errorUnknown.setVisible(true);
            }
        }
    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        // set the initial values to be those from the loaded MOTD
        enableMotd.setChecked(motd.isMotdEnabled());
        title.setValue(motd.getMotd("title"));
        message.setValue(motd.getMotd("message"));
    }

    public MOTD getMotd() {
        return motd;
    }

    public void setMotd(MOTD motd) {
        this.motd = motd;
    }



}
