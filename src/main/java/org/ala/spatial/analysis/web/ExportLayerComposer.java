/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radiogroup;

/**
 *
 * @author ajay
 */
public class ExportLayerComposer extends UtilityComposer {

    Radiogroup exportFormat;
    Button btnExportCancel;
    Button btnExportOk;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onCheck$exportFormat(Event event) {
        btnExportOk.setDisabled(false);
    }

    public void onClick$btnExportCancel(Event event) {
        this.detach();
    }

    public void onClick$btnExportOk(Event event) {
        getMapComposer().exportAreaAs(exportFormat.getSelectedItem().getValue());
        detach(); 
    }


    
}
