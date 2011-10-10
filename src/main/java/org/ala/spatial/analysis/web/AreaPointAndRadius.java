package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaPointAndRadius extends AreaToolComposer {

    private Textbox displayGeom;
    //String layerName;
    Textbox txtLayerName;
    Button btnNext;
    Button btnClear;
    
    @Override
    public void afterCompose() {
        super.afterCompose();

        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
    }

    public void onClick$btnNext(Event event) {
        //reapply layer name
        getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
        getMapComposer().redrawLayersList();

        ok = true;
        
        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addRadiusDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        displayGeom.setText("");
        btnNext.setDisabled(true);
        btnClear.setDisabled(true);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
    }

    /**
     *
     * @param event
     */
    public void onSelectionGeom(Event event) {
        String selectionGeom = (String) event.getData();

        try {

            String wkt = "";
            if (selectionGeom.contains("NaN NaN")) {
                displayGeom.setValue("");
              //  lastTool = null;
            } else if (selectionGeom.startsWith("LAYER(")) {
                //reset stored size
               // storedSize = null;
                //get WKT from this feature
                String v = selectionGeom.replace("LAYER(", "");
                //FEATURE(table name if known, class name)
                v = v.substring(0, v.length() - 1);
                
                //for display
                wkt = Util.wktFromJSON(getMapComposer().getMapLayer(v).getGeoJSON());
                displayGeom.setValue(wkt);

                //calculate area is not populated
//                if (storedSize == null) {
//                    storedSize = getAreaOfWKT(wkt);
//                }
            } else {
                wkt = selectionGeom;
                displayGeom.setValue(wkt);
            }

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            if (wkt.length() > 0) {
                 layerName = (mc.getMapLayer(txtLayerName.getValue()) == null)?txtLayerName.getValue():mc.getNextAreaLayerName(txtLayerName.getValue());
                MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

                MapLayerMetadata md = mapLayer.getMapLayerMetadata();
                if(md == null) {
                    md = new MapLayerMetadata();
                    mapLayer.setMapLayerMetadata(md);
                }
                md.setMoreInfo(LayersUtil.getMetadataForWKT("User drawn point and radius", wkt));
            }
         //   rgAreaSelection.getSelectedItem().setChecked(false);

            btnNext.setDisabled(false);
            btnClear.setDisabled(false);

        } catch (Exception e) {//FIXME
        }
    }

    /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }    
}
