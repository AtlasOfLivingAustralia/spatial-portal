/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerSelection;
import au.org.emii.portal.util.LayerUtilities;
import org.apache.log4j.Logger;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.util.ArrayList;

/**
 * same as LayersAutoComplete with type="environmental" layers only
 *
 * @author ajay
 */
public class SelectedLayersCombobox extends Combobox {

    private static Logger logger = Logger.getLogger(SelectedLayersCombobox.class);

    boolean includeAnalysisLayers = false;

    public void init(ArrayList<LayerSelection> layerSelections, MapComposer mc, boolean includeAnalysisLayers) {
        this.includeAnalysisLayers = includeAnalysisLayers;

        while (getItemCount() > 0) {
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
        for (MapLayer ml : mc.getGridLayers()) {
            //get layer name
            String name = null;
            String url = ml.getUri();
            int p1 = url.indexOf("ALA:") + 4;
            int p2 = url.indexOf("&", p1);
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
                if (ml.getSubType() == LayerUtilities.ALOC) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilities.MAXENT) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilities.GDM) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilities.ODENSITY) {
                    name = ml.getPid();
                } else if (ml.getSubType() == LayerUtilities.SRICHNESS) {
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
