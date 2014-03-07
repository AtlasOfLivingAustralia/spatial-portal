/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.composer.tool.SamplingComposer;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import java.util.Hashtable;

/**
 * @author adam
 */
public class SamplingEvent implements EventListener {

    String speciesLayerName;
    String polygonLayerName;
    String environmentalLayerName;
    MapComposer mc;
    int steps_to_skip;
    boolean[] geospatialKosher;

    public SamplingEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.steps_to_skip = 0;
        this.geospatialKosher = null;
    }

    public SamplingEvent(MapComposer mc, String speciesLayerName, String polygonLayerName, String environmentalLayerName, int steps_to_skip, boolean[] geospatialKosher) {
        this.mc = mc;
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.steps_to_skip = steps_to_skip;
        this.geospatialKosher = geospatialKosher;
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
        SamplingComposer window = (SamplingComposer) mc.openModal("WEB-INF/zul/tool/Sampling.zul", params, "addtoolwindow");

        window.setGeospatialKosherCheckboxes(geospatialKosher);

        int skip = steps_to_skip;
        while (skip > 0) {
            window.onClick$btnOk(event);
            skip--;
        }
        //window.onClick$btnOk(event);
        //window.onClick$btnOk(event);
    }
}
