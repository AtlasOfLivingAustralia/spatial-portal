package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author Adam
 */
public class AreaPolygon extends UtilityComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        MapComposer mc = getMapComposer();
        String script = mc.getOpenLayersJavascript().addPolygonDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
