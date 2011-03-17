package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;

/**
 *
 * @author ajay
 */
public class SamplingResultsWCController extends UtilityComposer {
    public SamplingWCController parent = null;
    Label samplingresultslabel;
    Rows results_rows;

    public void onClick$btnDownload(Event event) {
        Events.echoEvent("download",parent,null);
        detach();
    }    
}
