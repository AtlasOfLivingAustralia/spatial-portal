package org.ala.spatial.gazetteer;

import org.apache.commons.httpclient.HttpClient;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.util.GeoJSONUtilities;
import org.zkoss.zul.Combobox;
import org.zkoss.zk.ui.event.InputEvent;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zul.Comboitem;
import org.zkoss.zk.ui.Page;

public class AutoComplete extends Combobox {
    private String gazServer = null;

    private GeoJSONUtilities geoJSONUtilities = null;

    public AutoComplete() {
        refresh(""); //init the child comboitems
    }

    public AutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }



    @Override
    public void setValue(String value) {
        super.setValue(value);
    }
    
     /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = this.getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    /** Listens for what a user is entering.
     * @param evt
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {
        String searchString = val.trim().replaceAll("\\s+", "+");

        searchString = (searchString.equals(""))?"a":searchString;

        try {            
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.layersServer + "/search?limit=40&q=" + searchString);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            if(ja == null) {
                return;
            }
            
            Iterator it = getItems().iterator();

            for(int i=0;i<ja.size();i++) {
                JSONObject jo = ja.getJSONObject(i);
                String itemString = jo.getString("name");
                String link = "/shape/geojson/" + jo.getString("pid");
                String description = jo.getString("description") + " (" + jo.getString("fieldname") + ")";

                if (it != null && it.hasNext()) {
                    Comboitem ci = (Comboitem) it.next();
                    ci.setLabel(itemString);
                    ci.setValue(link);
                    ci.setDescription(description);
                } else {
                    it = null;
                    Comboitem ci = new Comboitem();
                    ci.setLabel(itemString);
                    ci.setValue(link);
                    ci.setDescription(description);
                    ci.setParent(this);
                }
            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
