package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.geotools.geometry.GeometryBuilder;



import org.geotools.referencing.crs.DefaultGeographicCRS;

import org.zkoss.zk.ui.Page;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaWKT extends AreaToolComposer {

   
    private Textbox displayGeom;
    //String layerName;
    Textbox txtLayerName;
    Label invalidWKT;
    Button btnOk;
    Button btnClear;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
    }

    public void onClick$btnOk(Event event) { 
        if (validWKT(displayGeom.getText())) {

            MapComposer mc = getThisMapComposer();

            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null)?txtLayerName.getValue():mc.getNextAreaLayerName(txtLayerName.getValue());
            MapLayer mapLayer = mc.addWKTLayer(displayGeom.getText(), layerName, txtLayerName.getValue());

            //reapply layer name
            getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
            getMapComposer().redrawLayersList();

            this.detach();
        }
        else {
          //  invalidWKT.setValue("WKT is Invalid");
            invalidWKT.setVisible(true);
        }
    }

    public boolean validWKT(String wkt) {
        if (wkt.replaceAll(" ","").isEmpty()){
            invalidWKT.setValue("WKT is Invalid");
            return false;
        }
        try {
            WKTReader wktReader = new WKTReader();
            wktReader.read(wkt);
            return true;
        }
        catch(ParseException parseException) {
            invalidWKT.setValue("WKT is Invalid");
            return false;
        }
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        displayGeom.setValue("");
//        btnOk.setDisabled(true);
//        btnClear.setDisabled(true);
        invalidWKT.setVisible(false);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
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
