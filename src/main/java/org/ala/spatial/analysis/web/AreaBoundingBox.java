package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import org.zkoss.zk.ui.Page;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaBoundingBox extends UtilityComposer {

    private String boxGeom;
    private Textbox displayGeom;
    private static final String DEFAULT_AREA = "CURRENTVIEW()";
    String layerName;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnNext(Event event) {
        this.detach();
    }

    public void onClick$btnClear(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        String script = mc.getOpenLayersJavascript().addBoxDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        displayGeom.setValue(DEFAULT_AREA);
    }

    public void onClick$btnCancel(Event event) {
        MapComposer mc = getThisMapComposer();
        if(layerName != null && mc.getMapLayer(layerName) != null) {
            mc.removeLayer(layerName);
        }
        this.detach();
    }

     public void onBoxGeom(Event event) {
        boxGeom = (String) event.getData();
        //setInstructions(null, null);
        try {
	
            if (boxGeom.contains("NaN NaN")) {
                displayGeom.setValue(DEFAULT_AREA);
            } else {
              displayGeom.setValue(boxGeom);
            }
           // updateComboBoxText();
            updateSpeciesList(false); // true


            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            //mc.removeLayer("Area Selection");
            //mc.deactiveLayer(mc.getMapLayer("Area Selection"), true,true);
            layerName = mc.getNextAreaLayerName("My box");
            MapLayer mapLayer = mc.addWKTLayer(boxGeom, layerName);

		//rgAreaSelection.getSelectedItem().setChecked(false);

            //wfsQueryBBox(boxGeom.getValue());


        } catch (Exception e) {//FIXME
        }

    }

       /**
     * updates species list analysis tab with refreshCount
     */
    void updateSpeciesList(boolean populateSpeciesList) {
        try {
            FilteringResultsWCController win =
                    (FilteringResultsWCController) getMapComposer().getFellow("leftMenuAnalysis").getFellow("analysiswindow").getFellow("sf").getFellow("selectionwindow").getFellow("speciesListForm").getFellow("popup_results");
            //if (!populateSpeciesList) {
            win.refreshCount();
            //} else {
            //    win.onClick$refreshButton2();
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
       // updateAreaLabel();
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
