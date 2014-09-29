package au.org.ala.spatial.composer.input;

import au.org.emii.portal.composer.UtilityComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.Textbox;

/**
 * @author ajay
 */
public class PasteLayerListController extends UtilityComposer {

    private static final Logger LOGGER = Logger.getLogger(PasteLayerListController.class);

    private Textbox layerList;
    private EventListener callback;

    public void onClick$btnOk(Event event) {
        if (callback != null) {
            try {
                callback.onEvent(new ForwardEvent("", this, event, layerList.getText()));
            } catch (Exception e) {
                LOGGER.error("failed when calling ToolComposer callback", e);
            }
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void setCallback(EventListener callback) {
        this.callback = callback;
    }
}
