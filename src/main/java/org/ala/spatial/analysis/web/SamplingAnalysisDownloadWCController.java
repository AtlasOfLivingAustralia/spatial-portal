/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author ajay
 */
public class SamplingAnalysisDownloadWCController extends UtilityComposer {

    public void onClick$btnDownload(Event event) {
        getMapComposer().downloadSecond(event);
        this.detach();
    }
}
