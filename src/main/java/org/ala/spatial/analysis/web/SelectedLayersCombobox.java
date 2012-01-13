/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerSelection;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import org.ala.spatial.util.CommonData;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 * same as LayersAutoComplete with type="environmental" layers only
 *
 * @author ajay
 */
public class SelectedLayersCombobox extends Combobox {
    boolean includeAnalysisLayers = false;

    public void init(ArrayList<LayerSelection> layerSelections, MapComposer mc, boolean includeAnalysisLayers) {
        this.includeAnalysisLayers = includeAnalysisLayers;

        while(getItemCount() > 0) {
            removeItemAt(0);
        }

        Comboitem ci = new Comboitem("Paste a layer set");
        ci.setParent(this);
        ci = new Comboitem("Import a layer set");
        ci.setParent(this);
        for (int i = 0; i < CommonData.analysisLayerSets.size(); i++) {
            ci = new Comboitem(CommonData.analysisLayerSets.get(i).toString());
            ci.setValue(CommonData.analysisLayerSets.get(i));
            ci.setParent(this);
        }
        for (int i = 0; i < layerSelections.size(); i++) {
            ci = new Comboitem(layerSelections.get(i).toString());
            ci.setValue(layerSelections.get(i));
            ci.setParent(this);

        }
        //add on map layers, active and inactive
        for(MapLayer ml : mc.getGridLayers()) {
            //get layer name
            String name = null;
            String url = ml.getUri();
            int p1 = url.indexOf("ALA:") + 4;
            int p2 = url.indexOf("&",p1);
            if(p1 > 4) {
                if(p2 < 0) p2 = url.length();
                name = url.substring(p1,p2);
            }
            if(name != null) {
                ci = new Comboitem(ml.getDisplayName());
                ci.setValue(new LayerSelection(ml.getDisplayName(), name));
                ci.setParent(this);
            }
        }

        if(includeAnalysisLayers) {
            //add on map layers, active and inactive
            for(MapLayer ml : mc.getAnalysisLayers()) {
                String name = null;
                if(ml.getSubType() == LayerUtilities.ALOC) {
                    name = (String)ml.getData("pid");
                } else if(ml.getSubType() == LayerUtilities.MAXENT) {
                    name = (String)ml.getData("pid");
                } else if(ml.getSubType() == LayerUtilities.ODENSITY) {
                    name = (String)ml.getData("pid");
                } else if(ml.getSubType() == LayerUtilities.SRICHNESS) {
                    name = (String)ml.getData("pid");
                }
                if(name != null) {
                    ci = new Comboitem(ml.getDisplayName());
                    ci.setValue(new LayerSelection(ml.getDisplayName(), name));
                    ci.setParent(this);
                }
            }
        }
    }

    public boolean getIncludeAnalysisLayers() {
        return includeAnalysisLayers;
    }
}
