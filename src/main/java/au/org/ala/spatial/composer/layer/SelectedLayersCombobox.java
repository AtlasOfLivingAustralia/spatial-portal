/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayerSelection;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.util.List;

/**
 * same as LayersAutoComplete with type=Constants.ENVIRONMENTAL layers only
 *
 * @author ajay
 */
public class SelectedLayersCombobox extends Combobox {

    private boolean includeAnalysisLayers = false;

    public void init(List<LayerSelection> layerSelections, MapComposer mc, boolean includeAnalysisLayers) {
        this.includeAnalysisLayers = includeAnalysisLayers;

        while (getItemCount() > 0) {
            removeItemAt(0);
        }

        Comboitem ci = new Comboitem("Paste a layer set");
        ci.setParent(this);
        ci = new Comboitem("Import a layer set");
        ci.setParent(this);
        for (int i = 0; i < CommonData.getAnalysisLayerSets().size(); i++) {
            ci = new Comboitem(CommonData.getAnalysisLayerSets().get(i).toString());
            ci.setValue(CommonData.getAnalysisLayerSets().get(i));
            ci.setParent(this);
        }
        for (int i = 0; i < layerSelections.size(); i++) {
            ci = new Comboitem(layerSelections.get(i).toString());
            ci.setValue(layerSelections.get(i));
            ci.setParent(this);

        }
        //add on map layers, active and inactive
        for (MapLayer ml : mc.getGridLayers()) {
            //get layer name
            String name = null;
            String url = ml.getUri();
            int p1 = url.indexOf("&style=") + 7;
            int p2 = url.indexOf("_style", p1);
            if (p1 > 4) {
                if (p2 < 0) {
                    p2 = url.length();
                }
                name = url.substring(p1, p2);
            }
            if (name != null) {
                ci = new Comboitem(ml.getDisplayName());
                ci.setValue(new LayerSelection(ml.getDisplayName(), name));
                ci.setParent(this);
            }
        }

        if (includeAnalysisLayers) {
            //add on map layers, active and inactive
            for (MapLayer ml : mc.getAnalysisLayers()) {
                String name = null;
                if (ml.getSubType() == LayerUtilitiesImpl.ALOC) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilitiesImpl.MAXENT) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilitiesImpl.GDM) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilitiesImpl.ODENSITY) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilitiesImpl.SRICHNESS) {
                    name = ml.getPid();
                }
                if (name != null) {
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
