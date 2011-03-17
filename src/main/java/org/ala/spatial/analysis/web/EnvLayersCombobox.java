/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
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

    private static String SAT_SERVER = null;
    SettingsSupplementary settingsSupplementary = null;
    String[] validLayers = null;

    ;

    public EnvLayersCombobox() {
        refresh(""); //init the child comboitems
    }

    public EnvLayersCombobox(String value) throws WrongValueException {
        super(value);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    private void refresh(String val) {
        if (validLayers == null) {
            makeValidLayers();
        }
        if (settingsSupplementary != null) {
            //System.out.println("setting ss.val");
        } else if (this.getParent() != null) {
            settingsSupplementary = settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
            System.out.println("LAC got SS: " + settingsSupplementary);
            SAT_SERVER = settingsSupplementary.getValue(CommonData.SAT_URL);
        } else {
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

                    if (!type.equalsIgnoreCase("environmental")) {
                        continue;
                    }

                    if (!isValidLayer(jo.getString("name"))) {
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
                    String c2 = "";
                    if (!jo.getString("classification2").equals("null")) {
                        c2 = jo.getString("classification2") + ": ";
                    }
                    myci.setDescription(jo.getString("classification1") + ": " + c2 + type);
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

    void makeValidLayers() {
        String[] ctx = CommonData.getContextualLayers();
        String[] env = CommonData.getEnvironmentalLayers();
        validLayers = new String[ctx.length + env.length];
        for (int i = 0; i < env.length; i++) {
            validLayers[i] = env[i].toLowerCase();
        }
        for (int i = 0; i < ctx.length; i++) {
            validLayers[i + env.length] = ctx[i].toLowerCase();
        }
        java.util.Arrays.sort(validLayers);
    }

    boolean isValidLayer(String name) {
        int pos = java.util.Arrays.binarySearch(validLayers, name.toLowerCase());
        return (pos >= 0 && pos < validLayers.length);
    }
}
