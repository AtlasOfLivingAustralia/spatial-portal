/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.composer.tool.SpeciesListComposer;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import java.util.Hashtable;

/**
 * @author adam
 */
public class SpeciesListEvent implements EventListener {

    String polygonLayerName;
    MapComposer mc;
    int steps_to_skip;
    boolean[] geospatialKosher;
    boolean chooseEndemic = false;
    String extraParams;

    public SpeciesListEvent(MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.steps_to_skip = 0;
        this.geospatialKosher = null;
    }

    public SpeciesListEvent(MapComposer mc, String polygonLayerName, int steps_to_skip, boolean[] geospatialKosher) {
        this(mc, polygonLayerName, steps_to_skip, geospatialKosher, false);
    }

    public SpeciesListEvent(MapComposer mc, String polygonLayerName, int steps_to_skip, boolean[] geospatialKosher, boolean chooseEndemic) {
        this(mc, polygonLayerName, steps_to_skip, geospatialKosher, chooseEndemic, null);
    }

    public SpeciesListEvent(MapComposer mc, String polygonLayerName, int steps_to_skip, boolean[] geospatialKosher, boolean chooseEndemic, String extraParams) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
        this.steps_to_skip = steps_to_skip;
        this.geospatialKosher = geospatialKosher;
        this.chooseEndemic = chooseEndemic;
        this.extraParams = extraParams;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        if (extraParams != null) {
            params.put("extraParams", extraParams);
        }
        SpeciesListComposer window = (SpeciesListComposer) mc.openModal("WEB-INF/zul/tool/SpeciesList.zul", params, "addtoolwindow");
        window.setGeospatialKosherCheckboxes(geospatialKosher);
        window.setChooseEndemic(chooseEndemic);

        int skip = steps_to_skip;
        while (skip > 0) {
            window.onClick$btnOk(event);
            skip--;
        }
    }
}
