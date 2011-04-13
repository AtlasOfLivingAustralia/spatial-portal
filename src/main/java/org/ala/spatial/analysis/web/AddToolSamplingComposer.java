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
public class AddToolSamplingComposer extends AddToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Sampling";
        this.totalSteps = 4;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true);
        this.updateWindowTitle();
        
    }

}
