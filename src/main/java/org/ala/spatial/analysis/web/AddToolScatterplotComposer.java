/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import org.zkoss.zk.ui.event.Event;

/**
 *
 * @author ajay
 */
public class AddToolScatterplotComposer extends AddToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 5;

        this.loadSpeciesLayers();
        this.updateWindowTitle();
        
    }

}
