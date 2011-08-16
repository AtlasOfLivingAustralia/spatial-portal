package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import net.sf.json.JSONObject;
import org.ala.spatial.analysis.web.LayersAutoComplete;
import org.ala.spatial.analysis.web.SpeciesAutoComplete;
import org.ala.spatial.gazetteer.AutoComplete;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class AddPlaceController extends UtilityComposer {
    
    private AutoComplete gazetteerAuto;

    @Override
    public void afterCompose() {
        super.afterCompose();
    }

    public void onClick$btnOk(Event event) {
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

        //exit if no match found
        if (ci == null) {
            return;
        }

        String link = (String) ci.getValue();
        String label = ci.getLabel();

        //add feature to the map as a new layer
        MapLayer mapLayer = getMapComposer().addGeoJSON(label, CommonData.geoServer + link);

        if (mapLayer != null && mapLayer.getGeoJSON() != null) {  //might be a duplicate layer making mapLayer == null
            //parsing is taking too long
            //JSONObject jo = JSONObject.fromObject(mapLayer.getGeoJSON());
            //String metadatalink = jo.getJSONObject("properties").getString("Layer_Metadata");
            String typeStart = "\"Layer_Metadata\":\"";
            String typeEnd = "\"";
            int start = mapLayer.getGeoJSON().indexOf(typeStart) + typeStart.length();
            int end = mapLayer.getGeoJSON().indexOf('\"', start);
            String metadatalink = mapLayer.getGeoJSON().substring(start, end);

            mapLayer.setMapLayerMetadata(new MapLayerMetadata());
            mapLayer.getMapLayerMetadata().setMoreInfo(metadatalink);

            getMapComposer().updateUserLogMapLayer("gaz", label + "|" + CommonData.geoServer + link);

        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }
}
