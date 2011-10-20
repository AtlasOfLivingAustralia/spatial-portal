/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
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

    public void init(ArrayList<LayerSelection> layerSelections) {
        Comboitem ci = new Comboitem("paste a layer list");
        ci.setParent(this);
        ci = new Comboitem("upload a layer list");
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
    }
}
