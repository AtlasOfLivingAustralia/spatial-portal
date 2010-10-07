/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 * same as LayersAutoComplete with type="environmental" layers only
 *
 * @author ajay
 */
public class EnvLayersCombobox extends Combobox {

    private static String SAT_SERVER = "http://spatial-dev.ala.org.au";
    private static final String SAT_URL = "sat_url";
    SettingsSupplementary settingsSupplementary = null;;

    public EnvLayersCombobox() {
        refresh(""); //init the child comboitems
    }

    public EnvLayersCombobox(String value) throws WrongValueException {
        super(value);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        //refresh(value); //refresh the child comboitems
        //refreshJSON(value);
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
            //refreshJSON(evt.getValue());
        }
    }

    private void refresh(String val) {

        //TODO get this from the config file
        if (settingsSupplementary != null) {
            //System.out.println("setting ss.val");
        } else if(this.getParent() != null){
            settingsSupplementary = settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
            System.out.println("LAC got SS: " + settingsSupplementary);
            SAT_SERVER = settingsSupplementary.getValue(SAT_URL);
        }else{
            return;
        }

        String baseUrl = SAT_SERVER + "/alaspatial/ws/layers/";
        try {
            Iterator it = getItems().iterator();

                String lsurl = baseUrl;
                if (val.length() == 0) {
                    lsurl += "list";
                } else {
                    lsurl += "search/" + URLEncoder.encode(val, "UTF-8");
                }

                System.out.println("nsurl: " + lsurl);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(lsurl);
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();


                JSONArray results = JSONArray.fromObject(slist);
                System.out.println("got " + results.size() + " layers");

                Sessions.getCurrent().setAttribute("layerlist", results);

                if (results.size() > 0) {

                    for (int i = 0; i < results.size(); i++) {

                        JSONObject jo = results.getJSONObject(i);

                        if (!jo.getBoolean("enabled")) {
                            continue;
                        }

                        String displayName = jo.getString("displayname");
                        String type = jo.getString("type");

                        if(!type.equalsIgnoreCase("environmental")){
                            continue;
                        }

                        Comboitem myci = null;
                        if (it != null && it.hasNext()) {
                            myci = ((Comboitem) it.next());
                            myci.setLabel(displayName);
                        } else {
                            it = null;
                            myci = new Comboitem(displayName);
                            myci.setParent(this);
                        }
                        myci.setDescription(type);
                        myci.setDisabled(false);
                        myci.setValue(jo);
                    }
                }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }


        } catch (Exception e) {
            System.out.println("Error searching for layers:");
            e.printStackTrace(System.out);
        }
    }

     private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}
