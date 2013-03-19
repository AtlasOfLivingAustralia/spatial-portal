package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;




import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
        String wkt = displayGeom.getText();
        if (validWKT(wkt)) {
            
            MapComposer mc = getThisMapComposer();

            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null)?txtLayerName.getValue():mc.getNextAreaLayerName(txtLayerName.getValue());
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            String metadata = "";
            metadata += "User pasted WKT \n";
            metadata += "Name: " + layerName + " <br />\n";
            metadata += "Date: " + formatter.format(calendar.getTime()) + " <br />\n";
            
            MapLayerMetadata mlmd = mapLayer.getMapLayerMetadata();
            if (mlmd == null) {
                mlmd = new MapLayerMetadata();
            }
            mlmd.setMoreInfo(metadata);
            mapLayer.setMapLayerMetadata(mlmd);


            //reapply layer name
            getMapComposer().getMapLayer(layerName).setDisplayName(txtLayerName.getValue());
            getMapComposer().redrawLayersList();

            ok = true;

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
            com.vividsolutions.jts.geom.Geometry g =wktReader.read(wkt);
            //NC 20130319: Ensure that the WKT is valid according to the WKT standards.
            //logger.debug("GEOMETRY TYPE: " + g.getGeometryType());
            IsValidOp op = new IsValidOp(g);
            if(!op.isValid()){
                invalidWKT.setValue("WKT is invalid. "+op.getValidationError().getMessage());
                logger.warn("WKT is invalid."  + op.getValidationError().getMessage());
                //TODO Fix invalid WKT text using https://github.com/tudelft-gist/prepair maybe???
            }
            else if(g.isRectangle()){
                //NC 20130319: When the shape is a rectangle ensure that the points a specified in the correct order.
                //get the new WKT for the rectangle will possibly need to change the order.
                //com.vividsolutions.jts.geom.Geometry g2 = g.getBoundary();
                //com.vividsolutions.jts.io.WKTWriter wktWriter = new com.vividsolutions.jts.io.WKTWriter();
                //String wkt2 = wktWriter.write(g2);
                com.vividsolutions.jts.geom.Envelope envelope =g.getEnvelopeInternal();
                String wkt2 = "POLYGON(("
                    + envelope.getMinX() + " " + envelope.getMinY() + ","
                    + envelope.getMaxX() + " " + envelope.getMinY() + ","
                    + envelope.getMaxX() + " " + envelope.getMaxY() + ","
                    + envelope.getMinX() + " " + envelope.getMaxY() + ","               
                    + envelope.getMinX() + " " + envelope.getMinY() + "))";
                if(!wkt.equals(wkt2)){
                    logger.debug("NEW WKT for Rectangle: " + wkt);
                    invalidWKT.setValue("WKT for Rectangle is in incorrect order. We have automatically fixed this. Press next to accept this value.");
                    displayGeom.setValue(wkt2);
                    return false;
                }
                
            }
            return op.isValid();
        }
        catch(ParseException parseException) {
            invalidWKT.setValue("WKT is Invalid. " +parseException.getMessage());
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
