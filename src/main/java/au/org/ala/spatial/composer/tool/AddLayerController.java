/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;

/**
 * @author YUAN
 */
public class AddLayerController extends ToolComposer {
    private static Logger logger = Logger.getLogger(AddLayerController.class);

    @Override
    public void afterCompose() {
        super.afterCompose();
        btnOk.setDisabled(true);

        this.selectedMethod = "Add layers";
        this.totalSteps = 1;

        this.setIncludeAnalysisLayersForUploadQuery(true);
        //this.loadAreaLayers();
        this.loadGridLayers(false, true, false);
        this.updateWindowTitle();
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        //this.updateName("My Prediction model for " + rgSpecies.getSelectedItem().getLabel());
        //this.updateName(getMapComposer().getNextAreaLayerName("My layer"));

    }

    @Override
    void fixFocus() {
        logger.debug(currentStep);
        switch (currentStep) {

            case 1:
                selectedLayersCombobox.setFocus(true);
                break;

        }
    }

    @Override
    public void onClick$btnOk(Event event) {
        //super.onClick$btnOk(event);
        if (currentStep == 1) {
            loadMap(event);
        }
    }

    @Override
    public void loadMap(Event event) {
        if (lbListLayers.getSelectedLayers().length > 0) {
            String[] sellayers = lbListLayers.getSelectedLayers();
            int i = 0;
            for (String s : sellayers) {
                i++;
                String uid = "";
                String type = "";
                String treeName = "";
                String treePath = "";
                String legendurl = "";
                String metadata = "";
                JSONArray layerlist = CommonData.getLayerListJSONArray();
                for (int j = 0; j < layerlist.size(); j++) {
                    JSONObject jo = layerlist.getJSONObject(j);
                    String name = jo.getString("name");
                    if (name.equals(s)) {
                        uid = jo.getString("id");
                        type = jo.getString("type");
                        treeName = StringUtils.capitalize(jo.getString("displayname"));
                        treePath = jo.getString("displaypath");
                        legendurl = CommonData.geoServer + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + s;
                        metadata = CommonData.layersServer + "/layers/view/more/" + uid;
                        break;
                    } else {
                        continue;
                    }
                }

                getMapComposer().addWMSLayer(s, treeName, treePath, (float) 0.75, metadata, legendurl, type.equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL, null, null, null);

                try {
                    remoteLogger.logMapArea(treeName, "Layer - " + type, treePath, s, metadata);
                } catch (Exception e) {
                    //logger
                }
            }
        }
        this.detach();
    }

    public String getUid(String name) {
        String uid = "";
        try {
            JSONArray layerlist = CommonData.getLayerListJSONArray();
            for (int j = 0; j < layerlist.size(); j++) {
                JSONObject jo = layerlist.getJSONObject(j);
                String n = jo.getString("name");
                if (name.equals(n)) {
                    uid = jo.getString("id");
                    logger.debug("id=" + uid);
                    break;
                } else {
                    continue;
                }
            }
            return uid;

        } catch (Exception e) {
            logger.error("error setting up env list", e);
            return null;
        }
    }
}
