package au.org.emii.portal.composer;

import org.zkoss.zul.Label;

/**
 * Composer for the simple popup error message - allows easy access to the
 * message field
 *
 * @author geoff
 */
public class ErrorMessageComposer extends UtilityComposer {

    private static final long serialVersionUID = 1L;
    private Label message;

    public Label getMessage() {
        return message;
    }

    public void setMessage(Label message) {
        this.message = message;
    }

    public void setMessage(String message) {
        this.message.setValue(message);
    }

    public void onClick$ok() {
        detach();
    }
}
