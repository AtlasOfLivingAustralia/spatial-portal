/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.composer.layer.ContextualLayersAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.ContextualLayerListComposer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

/**
 * @author angus
 */
public class ContextualLayerSelection extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(ContextualLayerSelection.class);
    Button btnNext;
    ContextualLayersAutoComplete autoCompleteLayers;
    String treeName, treePath, treeMetadata;
    int treeSubType;

    @Override
    public void afterCompose() {
        super.afterCompose();

        btnNext.setDisabled(true);

    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onClick$btnNext(Event event) {
        if (treeName != null) {
            getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(treeName), treeName,
                    treePath,
                    (float) 0.75, treeMetadata, null, treeSubType, null, null);

            String activeLayerName = treePath.replaceAll("^.*ALA:", "").replaceAll("&.*", "");

            String lyrSubType = "";
            if (treeSubType == LayerUtilities.CONTEXTUAL) {
                lyrSubType = "Contextual";
            } else if (treeSubType == LayerUtilities.GRID) {
                lyrSubType = "Environmental";
            }

            remoteLogger.logMapArea(treeName, "Layer - " + lyrSubType, treePath, activeLayerName, treeMetadata);
        }

        Window window = (Window) Executions.createComponents("WEB-INF/zul/add/area/AreaMapPolygon.zul", this.getParent(), winProps);
        window.doOverlapped();
        String script = getMapComposer().getOpenLayersJavascript().addFeatureSelectionTool();
        getMapComposer().getOpenLayersJavascript().execute(OpenLayersJavascript.iFrameReferences + script);

        this.detach();
    }

    public void onChange$autoCompleteLayers(Event event) {
        treeName = null;
        //btnOk.setDisabled(true);

        ContextualLayerListComposer llc = (ContextualLayerListComposer) getFellow("layerTree").getFellow("contextuallayerswindow");

        if (autoCompleteLayers.getItemCount() > 0 && autoCompleteLayers.getSelectedItem() != null) {
            JSONObject jo = autoCompleteLayers.getSelectedItem().getValue();
            String metadata = "";

            metadata = CommonData.layersServer + "/layers/view/more/" + jo.getString("id");

            setLayer(jo.getString("displayname"), jo.getString("displaypath"), metadata,
                    jo.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);
        } else {

            // if the autocomplete has been type, but before selecting an option,
            // the focus is lost (eg, clicking on the next button or on tree)
            // it generates an error. This should fix it. 
            if (llc.tree.getSelectedItem() == null) {
                return;
            }

            JSONObject joLayer = JSONObject.fromObject(llc.tree.getSelectedItem().getTreerow().getAttribute("lyr"));
            if (!joLayer.getString("type").contentEquals("class")) {

                String metadata = CommonData.layersServer + "/layers/view/more/" + joLayer.getString("id");

                setLayer(joLayer.getString("displayname"), joLayer.getString("displaypath"), metadata,
                        joLayer.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);
            } else {
                String classAttribute = joLayer.getString("classname");
                String classValue = joLayer.getString("displayname");
                String layer = joLayer.getString("layername");
                //String displaypath = joLayer.getString("displaypath") + "&cql_filter=(" + classAttribute + "='" + classValue + "');include";
                String displaypath = CommonData.geoServer
                        + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
                        + joLayer.getString("displaypath");
                //Filtered requests don't work on
                displaypath = displaypath.replace("gwc/service/", "");
                // Messagebox.show(displaypath);
                String metadata = CommonData.layersServer + "/layers/view/more/" + joLayer.getString("id");

                setLayer(layer + " - " + classValue, displaypath, metadata,
                        joLayer.getString("type").equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL);
            }

            //close parent if it is 'addlayerwindow'
            try {
                getRoot().getFellow("addlayerwindow").detach();
            } catch (Exception e) {
            }
        }
    }

    public void setLayer(String name, String displaypath, String metadata, int subType) {
        treeName = name;
        treePath = displaypath;
        treeMetadata = metadata;
        treeSubType = subType;

        //fill autocomplete text
        autoCompleteLayers.setText(name);

        //clear selection on tree
        ContextualLayerListComposer llc = (ContextualLayerListComposer) getFellow("layerTree").getFellow("contextuallayerswindow");
        llc.tree.clearSelection();

        // btnOk.setDisabled(false);
        btnNext.setDisabled(false);
    }
}
