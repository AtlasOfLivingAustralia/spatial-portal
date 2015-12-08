/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zkoss.zk.ui.event.Event;

/**
 * @author YUAN
 */
public class AddLayerController extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(AddLayerController.class);

    @Override
    public void afterCompose() {
        super.afterCompose();
        btnOk.setDisabled(true);

        this.selectedMethod = "Add layers";
        this.totalSteps = 1;

        this.setIncludeAnalysisLayersForUploadQuery(true);

        this.loadGridLayers(false, true, false);
        this.updateWindowTitle();
    }

    @Override
    void fixFocus() {
        LOGGER.debug(currentStep);
        if (currentStep == 1) {
            selectedLayersCombobox.setFocus(true);
        }
    }

    @Override
    public void onClick$btnOk(Event event) {

        if (currentStep == 1) {
            loadMap(event);
        }
    }

    public void loadMap(Event event) {
        if (lbListLayers.getSelectedLayers().length > 0) {
            String[] sellayers = lbListLayers.getSelectedLayers();

            for (String s : sellayers) {
                String uid;
                String type = "";
                String treeName = "";
                String treePath = "";
                String legendurl = "";
                String metadata = "";
                JSONArray layerlist = CommonData.getLayerListJSONArray();
                for (int j = 0; j < layerlist.size(); j++) {
                    JSONObject field = (JSONObject) layerlist.get(j);
                    JSONObject layer = (JSONObject) field.get("layer");
                    String name = field.get(StringConstants.ID).toString();
                    if (name.equals(s)) {
                        uid = layer.get(StringConstants.ID).toString();
                        type = layer.get(StringConstants.TYPE).toString();
                        treeName = StringUtils.capitalize(field.get(StringConstants.NAME).toString());
                        treePath = layer.get("displaypath").toString();
                        legendurl = CommonData.getGeoServer() + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" +
                                layer.get(StringConstants.NAME).toString() + "&styles=" + name + "_style";
                        metadata = CommonData.getLayersServer() + "/layers/view/more/" + uid;
                        break;
                    }
                }

                getMapComposer().addWMSLayer(s, treeName, treePath, (float) 0.75, metadata, legendurl,
                        StringConstants.ENVIRONMENTAL.equalsIgnoreCase(type) ? LayerUtilitiesImpl.GRID : LayerUtilitiesImpl.CONTEXTUAL, null, null, null);

                remoteLogger.logMapArea(treeName, "Layer - " + type, treePath, s, metadata);
            }
        }
        this.detach();
    }
}
