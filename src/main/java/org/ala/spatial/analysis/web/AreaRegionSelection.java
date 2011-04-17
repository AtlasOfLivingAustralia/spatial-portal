/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import org.zkoss.zk.ui.event.Event;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.AutoComplete;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Textbox;

/**
 *
 * @author angus
 */
public class AreaRegionSelection extends UtilityComposer {

    Button btnOk;
    private AutoComplete gazetteerAuto;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
        Comboitem ci = gazetteerAuto.getSelectedItem();

        //exit if no match found
        if (ci == null) {
            return;
        }

        String link = (String) ci.getValue();
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer = getMapComposer().addGeoJSON(label, CommonData.geoServer + link);

        if (mapLayer != null) {  //might be a duplicate layer making mapLayer == null
            JSONObject jo = JSONObject.fromObject(mapLayer.getGeoJSON());
            String metadatalink = jo.getJSONObject("properties").getString("Layer_Metadata");

            mapLayer.setMapLayerMetadata(new MapLayerMetadata());
            mapLayer.getMapLayerMetadata().setMoreInfo(metadatalink);

            getMapComposer().updateUserLogMapLayer("gaz", label + "|" + CommonData.geoServer + link);

        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

     /**
     * Adds the currently selected gazetteer feature to the map
     */
    public void onChange$gazetteerAuto() {

        Comboitem ci = gazetteerAuto.getSelectedItem();

        //when no item selected find an exact match from listed items
        if (ci == null) {
            String txt = gazetteerAuto.getText();
            for (Object o : gazetteerAuto.getItems()) {
                Comboitem c = (Comboitem) o;
                if (c.getLabel().equalsIgnoreCase(txt)) {
                    gazetteerAuto.setSelectedItem(c);
                    ci = c;
                    break;
                }
            }
        }

        if(ci == null) {
            btnOk.setDisabled(true);
        } else {
            btnOk.setDisabled(false);
        }
    }   
}


