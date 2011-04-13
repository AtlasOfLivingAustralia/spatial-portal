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
public class AddToolMaxentComposer extends AddToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Prediction";
        this.totalSteps = 5;

        this.loadAreaLayers();
        this.loadSpeciesLayers();
        this.loadGridLayers(true);
        this.updateWindowTitle();
        
    }

}
