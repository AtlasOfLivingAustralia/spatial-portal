package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;

import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.Command;
import org.zkoss.zk.au.ComponentCommand;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

/**
 *
 * @author Angus
 */
public class SelectionController extends UtilityComposer {

    @Override
	public void afterCompose() {
		super.afterCompose();
    }

    public void onClick$btnPolygonSelection(Event event) {
        MapComposer mc = getThisMapComposer();
       
        mc.getOpenLayersJavascript().addPolygonDrawingTool();
    }

     /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    /**
     *
     * @param cmdId
     * @return
     */
    public Command getCommand(String cmdId) {
        if (cmdId.equals("onNotifyServer")) {
            return new CustomCommand(cmdId);
        }
        return super.getCommand(cmdId);
    }

    private class CustomCommand extends ComponentCommand {

        public CustomCommand(String command) {
            super(command,
                    Command.SKIP_IF_EVER_ERROR | Command.CTRL_GROUP);
        }

        protected void process(AuRequest request) {
            Events.postEvent(new Event(request.getCommand().getId(), request.getComponent(), request.getData()));
        }
    }
}