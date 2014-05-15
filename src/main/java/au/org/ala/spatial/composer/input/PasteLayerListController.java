package au.org.ala.spatial.composer.input;

import au.org.ala.spatial.composer.tool.ToolComposer;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Textbox;

/**
 * @author ajay
 */
public class PasteLayerListController extends UtilityComposer {

    Textbox layerList;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        if (getParent() instanceof ToolComposer) {
            ((ToolComposer) getParent()).selectLayerFromList(layerList.getText());
            ((ToolComposer) getParent()).updateLayerSelectionCount();
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
