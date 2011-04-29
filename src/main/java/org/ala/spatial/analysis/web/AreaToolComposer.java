/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import java.util.Map;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class AreaToolComposer extends UtilityComposer {

//    public String boxGeom;
//    public Textbox displayGeom;
//    String layerName;
//    Textbox txtLayerName;
//    Button btnNext;
//    Button btnOk;
//    Button btnClear;

    String layerName;

    boolean isAnalysisChild = false;
    AddToolComposer analysisParent = null;
    Map winProps = null;

    @Override
    public void afterCompose() {
        super.afterCompose();
        //txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));

        Component parent = this.getParent();
        System.out.println("Parent: " + parent.getId() + " - " + parent.getWidgetClass());

        if (parent.getId().equals("addtoolwindow")) {
            analysisParent = (AddToolComposer) this.getParent();
            isAnalysisChild = true; 
        } else {
            isAnalysisChild = false; 
        }
    }

    @Override
    public void detach() {
        super.detach();
        if (isAnalysisChild) {
            analysisParent.hasCustomArea = true;
            analysisParent.resetWindow(layerName);
        }
    }

}
