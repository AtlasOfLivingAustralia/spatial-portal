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
 * Extend a combobox to provide autocomplete facilities for
 * all mappable layers
 * 
 * @author angus
 */
public class ContextualLayersAutoComplete extends Combobox {

    SettingsSupplementary settingsSupplementary = null;

    ;

    public ContextualLayersAutoComplete() {
        refresh(""); //init the child comboitems
    }

    public ContextualLayersAutoComplete(String value) throws WrongValueException {
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
        } else if (this.getParent() != null) {
            settingsSupplementary = settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
            System.out.println("LAC got SS: " + settingsSupplementary);
        } else {
            return;
        }

        String baseUrl = CommonData.satServer + "/ws/layers/";
        try {

            //System.out.println("bringing in layers:");
            //System.out.println(layersUtil.getLayerList());

            Iterator it = getItems().iterator();
            /*
            if (val.length() == 0) {
            Comboitem myci = null;
            if (it != null && it.hasNext()) {
            myci = ((Comboitem) it.next());
            myci.setLabel("Please start by typing in a layer name or keyword...");
            } else {
            it = null;
            myci = new Comboitem("Please start by typing in a layer name or keyword...");
            myci.setParent(this);
            }
            myci.setDescription("");
            myci.setDisabled(true);
            } else {
             *
             */
            JSONArray results = null;
            String lsurl = baseUrl;
            if (val.length() == 0) {
                lsurl += "list";
                results = CommonData.getLayerListJSONArray();
            } else {
                lsurl += "search/" + URLEncoder.encode(val, "UTF-8");            

                System.out.println("nsurl: " + lsurl);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(lsurl);
                //get.addRequestHeader("Content-type", "application/json");
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                //System.out.println("Response status code: " + result);
                //System.out.println("Response: \n" + slist);

                results = JSONArray.fromObject(slist);
            }

            System.out.println("got " + results.size() + " layers");

            Sessions.getCurrent().setAttribute("layerlist", results);

            if (results.size() > 0) {

                for (int i = 0; i < results.size(); i++) {

                    JSONObject jo = results.getJSONObject(i);

                    if (!jo.getBoolean("enabled")||(jo.getString("type").equalsIgnoreCase("environmental"))) {
                        continue;
                    }

                    String displayName = jo.getString("displayname");
                    String type = jo.getString("type");

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
            /*else {
            if (it != null && it.hasNext()) {
            ((Comboitem) it.next()).setLabel("No species found.");
            } else {
            it = null;
            new Comboitem("No species found.").setParent(this);
            }

            }*/

            ///}
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
