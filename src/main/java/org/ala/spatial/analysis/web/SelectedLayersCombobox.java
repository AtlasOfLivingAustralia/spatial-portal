/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerSelection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Window;

/**
 * same as LayersAutoComplete with type="environmental" layers only
 *
 * @author ajay
 */
public class SelectedLayersCombobox extends Combobox {

    public void init(ArrayList<LayerSelection> layerSelections, MapComposer mc) {
        Comboitem ci = new Comboitem("paste a layer set");
        ci.setParent(this);
//        ci = new Comboitem("upload a layer list");
//        ci.setParent(this);
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
    }
}
