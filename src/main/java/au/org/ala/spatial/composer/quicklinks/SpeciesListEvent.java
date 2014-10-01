/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.StringConstants;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author adam
 */
public class SpeciesListEvent implements EventListener {

    private String polygonLayerName;
    private boolean[] geospatialKosher;
    private boolean chooseEndemic = false;
    private String extraParams;

    public SpeciesListEvent(String polygonLayerName) {
        this.polygonLayerName = polygonLayerName;
        this.geospatialKosher = null;
    }

    public SpeciesListEvent(String polygonLayerName, boolean[] geospatialKosher, boolean chooseEndemic, String extraParams) {
        this.polygonLayerName = polygonLayerName;
        this.geospatialKosher = (geospatialKosher == null) ? null : geospatialKosher.clone();
        this.chooseEndemic = chooseEndemic;
        this.extraParams = extraParams;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        if (polygonLayerName != null) {
            params.put(StringConstants.POLYGON_LAYER_NAME, polygonLayerName);
        } else {
            params.put(StringConstants.POLYGON_LAYER_NAME, StringConstants.NONE);
        }
        if (extraParams != null) {
            params.put(StringConstants.EXTRAPARAMS, extraParams);
        }
        if (geospatialKosher != null) {
            params.put(StringConstants.GEOSPATIAL_KOSHER, geospatialKosher);
        }
        params.put(StringConstants.CHOOSEENDEMIC, chooseEndemic);

        MapComposer mc = (MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE);
        mc.openModal("WEB-INF/zul/tool/SpeciesList.zul", params, StringConstants.ADDTOOLWINDOW);
    }
}
