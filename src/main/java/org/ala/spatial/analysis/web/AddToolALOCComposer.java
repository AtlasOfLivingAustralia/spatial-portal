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
public class AddToolALOCComposer extends AddToolComposer {

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Classification";
        this.totalSteps = 4;

        this.loadGridLayers(true);
        this.updateWindowTitle();
    }

}
