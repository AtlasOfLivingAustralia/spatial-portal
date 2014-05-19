/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.quicklinks;

import au.org.ala.spatial.composer.tool.ExportLayerComposer;
import au.org.emii.portal.composer.MapComposer;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import java.util.Hashtable;

/**
 * @author adam
 */
public class ExportAreaEvent implements EventListener {

    String polygonLayerName;
    MapComposer mc;

    public ExportAreaEvent(MapComposer mc, String polygonLayerName) {
        this.mc = mc;
        this.polygonLayerName = polygonLayerName;
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        if (polygonLayerName != null) {
            params.put("polygonLayerName", polygonLayerName);
        } else {
            params.put("polygonLayerName", "none");
        }
        ExportLayerComposer window = (ExportLayerComposer) mc.openModal("WEB-INF/zul/output/ExportLayer.zul", params, "addtoolwindow");

//        MapLayer ml = mc.getMapLayer(polygonLayerName);
//        Window w = (Window) mc.getPage().getFellowIfAny("popup_results");
//        if (w != null) {
//            w.detach();
//        }
//        double[] bbox = null;
//        if (ml != null && ml.getMapLayerMetadata() != null
//                && ml.getMapLayerMetadata().getBbox() != null
//                && ml.getMapLayerMetadata().getBbox().size() == 4) {
//            bbox = new double[4];
//            bbox[0] = ml.getMapLayerMetadata().getBbox().get(0);
//            bbox[1] = ml.getMapLayerMetadata().getBbox().get(1);
//            bbox[2] = ml.getMapLayerMetadata().getBbox().get(2);
//            bbox[3] = ml.getMapLayerMetadata().getBbox().get(3);
//        }
//        FilteringResultsWCController.open(ml.getWKT(), ml.getName(), ml.getDisplayName(), (String) ml.getData("area"), bbox);
    }
}
