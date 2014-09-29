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
public class ExportAreaEvent implements EventListener {

    private String polygonLayerName;

    public ExportAreaEvent(String polygonLayerName) {
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        if (polygonLayerName != null) {
            params.put(StringConstants.POLYGON_LAYER_NAME, polygonLayerName);
        } else {
            params.put(StringConstants.POLYGON_LAYER_NAME, StringConstants.NONE);
        }
        ((MapComposer) event.getPage().getFellow(StringConstants.MAPPORTALPAGE)).openModal("WEB-INF/zul/output/ExportLayer.zul", params, StringConstants.ADDTOOLWINDOW);
    }
}
