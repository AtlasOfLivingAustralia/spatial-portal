/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.output;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;

/**
 * @author ajay
 */
public class SamplingAnalysisDownloadController extends UtilityComposer {

    public void onClick$btnDownload(Event event) {
        getMapComposer().downloadSecond(event);
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
