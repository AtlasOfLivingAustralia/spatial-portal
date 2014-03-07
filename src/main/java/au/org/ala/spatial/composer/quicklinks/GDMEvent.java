/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.composer.tool.GDMComposer;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import java.util.Hashtable;

/**
 * @author adam
 */
public class GDMEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;

    public GDMEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (speciesLayerName != null) {
            params.put("speciesLayerName", speciesLayerName);
        } else {
            params.put("speciesLayerName", "none");
        }
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (environmentalLayerName != null) {
            params.put("environmentalLayerName", environmentalLayerName);
        } else {
            params.put("environmentalLayerName", "none");
        }
        GDMComposer window = (GDMComposer) mc.openModal("WEB-INF/zul/tool/GDM.zul", params, "addtoolwindow");
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}
