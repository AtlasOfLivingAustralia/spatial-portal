/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.layer.ContextualLayersAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import net.sf.json.JSONObject;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * @author angus
 */
public class ContextualLayerSelection extends ToolComposer {

    private Button btnNext;
    private ContextualLayersAutoComplete autoCompleteLayers;
    private String treeName, treePath, treeMetadata;
    private int treeSubType;

    @Override
    public void afterCompose() {
        super.afterCompose();

        btnNext.setDisabled(true);

    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onClick$btnNext(Event event) {

        Map<String, Object> winProps = new HashMap<String, Object>();
        winProps.put(StringConstants.PARENT, this);
        winProps.put(StringConstants.PARENTNAME, "Tool");
        winProps.put(StringConstants.SELECTEDMETHOD, selectedMethod);

        if (treeName != null) {
            getMapComposer().addWMSLayer(getMapComposer().getNextAreaLayerName(treeName), treeName,
                    treePath.replace("&layers=", "&layer="),
                    (float) 0.75, treeMetadata, null, treeSubType, null, null);

            String activeLayerName = treePath.replaceAll("^.*ALA:", "").replaceAll("&.*", "");

            String lyrSubType = "";
            if (treeSubType == LayerUtilitiesImpl.CONTEXTUAL) {
                lyrSubType = "Contextual";
            } else if (treeSubType == LayerUtilitiesImpl.GRID) {
                lyrSubType = StringConstants.ENVIRONMENTAL;
            }

            remoteLogger.logMapArea(treeName, "Layer - " + lyrSubType, treePath, activeLayerName, treeMetadata);
        }

        Window window = (Window) Executions.createComponents("WEB-INF/zul/add/area/AreaMapPolygon.zul", this.getParent(), winProps);
        window.doOverlapped();
        String script = getMapComposer().getOpenLayersJavascript().addFeatureSelectionTool();
        getMapComposer().getOpenLayersJavascript().execute(getMapComposer().getOpenLayersJavascript().getIFrameReferences() + script);

        this.detach();
    }

    public void onChange$autoCompleteLayers(Event event) {
        treeName = null;

        ContextualLayerListComposer llc = (ContextualLayerListComposer) getFellow("layerTree").getFellow("contextuallayerlistwindow");

        if (autoCompleteLayers.getItemCount() > 0 && autoCompleteLayers.getSelectedItem() != null) {
            JSONObject jo = autoCompleteLayers.getSelectedItem().getValue();
            String metadata;

            metadata = CommonData.getLayersServer() + "/layers/view/more/" + jo.getString(StringConstants.ID);

            setLayer(jo.getString(StringConstants.DISPLAYNAME), jo.getString("displaypath"), metadata,
                    StringConstants.ENVIRONMENTAL.equalsIgnoreCase(jo.getString(StringConstants.TYPE)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
        } else {

            // if the autocomplete has been type, but before selecting an option,
            // the focus is lost (eg, clicking on the next button or on tree)
            // it generates an error. This should fix it. 
            if (llc.tree.getSelectedItem() == null) {
                return;
            }

            JSONObject joLayer = JSONObject.fromObject(llc.tree.getSelectedItem().getTreerow().getAttribute("lyr"));
            if (!StringConstants.CLASS.equals(joLayer.getString(StringConstants.TYPE))) {

                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.getString(StringConstants.ID);

                setLayer(joLayer.getString(StringConstants.DISPLAYNAME), joLayer.getString("displaypath"), metadata,
                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.getString(StringConstants.TYPE)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
            } else {
                String classValue = joLayer.getString(StringConstants.DISPLAYNAME);
                String layer = joLayer.getString(StringConstants.LAYERNAME);
                String displaypath = CommonData.getGeoServer()
                        + "/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:Objects&format=image/png&viewparams=s:"
                        + joLayer.getString("displaypath");

                displaypath = displaypath.replace("gwc/service/", "");

                String metadata = CommonData.getLayersServer() + "/layers/view/more/" + joLayer.getString(StringConstants.ID);

                setLayer(layer + " - " + classValue, displaypath, metadata,
                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(joLayer.getString(StringConstants.TYPE)) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL);
            }

            //close parent if it is 'addlayerwindow'
            if (getRoot().hasFellow("addlayerwindow")) {
                getRoot().getFellow("addlayerwindow").detach();
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
        ContextualLayerListComposer llc = (ContextualLayerListComposer) getFellow("layerTree").getFellow("contextuallayerlistwindow");
        llc.tree.clearSelection();

        btnNext.setDisabled(false);
    }
}
