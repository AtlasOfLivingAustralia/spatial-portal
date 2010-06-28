package org.ala.spatial.analysis.web;

import java.net.URLEncoder;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 * Extend a combobox to provide autocomplete facilities for
 * all mappable layers
 * 
 * @author ajay
 */
public class LayersAutoComplete extends Combobox {

    private static String SAT_SERVER = "http://localhost:8080";

    public LayersAutoComplete() {
        refresh(""); //init the child comboitems
    }

    public LayersAutoComplete(String value) throws WrongValueException {
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
        String baseUrl = SAT_SERVER + "/alaspatial/ws/layers/";
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
                String lsurl = baseUrl;
                if (val.length() == 0) {
                    lsurl += "list";
                } else {
                    lsurl += "search/" + URLEncoder.encode(val, "UTF-8");
                }

                System.out.println("nsurl: " + lsurl);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(lsurl);
                //get.addRequestHeader("Content-type", "application/json");
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                System.out.println("Response status code: " + result);
                System.out.println("Response: \n" + slist);

                JSONArray results = JSONArray.fromObject(slist);
                System.out.println("got " + results.size() + " layers");

                if (results.size() > 0) {

                    for (int i = 0; i < results.size(); i++) {

                        JSONObject jo = results.getJSONObject(i);

                        if (!jo.getBoolean("enabled")) {
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
                        myci.setDescription(type);
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
}
