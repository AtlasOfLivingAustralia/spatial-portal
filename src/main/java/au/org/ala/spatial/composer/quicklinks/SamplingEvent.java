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
public class SamplingEvent implements EventListener {

    private boolean[] geospatialKosher;
    private String speciesLayerName;
    private String polygonLayerName;
    private String environmentalLayerName;

    public SamplingEvent(String speciesLayerName, String polygonLayerName, String environmentalLayerName) {
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.geospatialKosher = null;
    }

    public SamplingEvent(String speciesLayerName, String polygonLayerName, String environmentalLayerName, boolean[] geospatialKosher) {
        this.speciesLayerName = speciesLayerName;
        this.polygonLayerName = polygonLayerName;
        this.environmentalLayerName = environmentalLayerName;
        this.geospatialKosher = geospatialKosher.clone();
    }

    @Override
    public void onEvent(Event event) throws Exception {
        MapComposer mc = (MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE);

        Map<String, Object> params = new HashMap<String, Object>();
        if (speciesLayerName != null) {
            params.put(StringConstants.SPECIES_LAYER_NAME, speciesLayerName);
        } else {
            params.put(StringConstants.SPECIES_LAYER_NAME, StringConstants.NONE);
        }
        if (polygonLayerName != null) {
            params.put(StringConstants.POLYGON_LAYER_NAME, polygonLayerName);
        } else {
            params.put(StringConstants.POLYGON_LAYER_NAME, StringConstants.NONE);
        }
        if (environmentalLayerName != null) {
            params.put(StringConstants.ENVIRONMENTALLAYERNAME, environmentalLayerName);
        } else {
            params.put(StringConstants.ENVIRONMENTALLAYERNAME, StringConstants.NONE);
        }
        if (geospatialKosher != null) {
            params.put(StringConstants.GEOSPATIAL_KOSHER, geospatialKosher);
        }
        mc.openModal("WEB-INF/zul/tool/Sampling.zul", params, StringConstants.ADDTOOLWINDOW);
    }
}
