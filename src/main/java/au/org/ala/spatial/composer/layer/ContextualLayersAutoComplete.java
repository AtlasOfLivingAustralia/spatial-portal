package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.net.URLEncoder;
import java.util.Iterator;

/**
 * Extend a combobox to provide autocomplete facilities for all mappable layers
 *
 * @author angus
 */
public class ContextualLayersAutoComplete extends Combobox {

    private static final Logger LOGGER = Logger.getLogger(ContextualLayersAutoComplete.class);

    public ContextualLayersAutoComplete() {
        refresh("");
    }

    /**
     * Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    private void refresh(String val) {

        //don't do autocomplete when < 3 characters
        if (val.length() < 3) {
            return;
        }

        String baseUrl = CommonData.getLayersServer() + "/fields/";
        try {

            Iterator it = getItems().iterator();

            JSONArray results;
            String lsurl = baseUrl;
            if (val.length() == 0) {
                results = CommonData.getLayerListJSONArray();
            } else {
                lsurl += "search/?q=" + URLEncoder.encode(val, StringConstants.UTF_8);

                LOGGER.debug("nsurl: " + lsurl);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(lsurl);

                get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);

                client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                JSONParser jp = new JSONParser();
                results = (JSONArray) jp.parse(slist);
            }

            LOGGER.debug("got " + results.size() + " layers");

            Sessions.getCurrent().setAttribute("layerlist", results);

            if (!results.isEmpty()) {

                for (int i = 0; i < results.size(); i++) {

                    JSONObject field = (JSONObject) results.get(i);
                    JSONObject layer = (JSONObject) field.get("layer");

                    if (!field.get(StringConstants.ENABLED).toString().equalsIgnoreCase("true")
                            || !field.get(StringConstants.INDB).toString().equalsIgnoreCase("true")
                            || (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(layer.get(StringConstants.TYPE).toString()))) {
                        continue;
                    }

                    String displayName = field.get(StringConstants.NAME).toString();
                    String type = layer.get(StringConstants.TYPE).toString();

                    Comboitem myci;
                    if (it != null && it.hasNext()) {
                        myci = ((Comboitem) it.next());
                        myci.setLabel(displayName);
                    } else {
                        it = null;
                        myci = new Comboitem(displayName);
                        myci.setParent(this);
                    }
                    String c2 = "";
                    if (layer.containsKey(StringConstants.CLASSIFICATION2) && !StringConstants.NULL.equals(layer.get(StringConstants.CLASSIFICATION2))) {
                        c2 = layer.get(StringConstants.CLASSIFICATION2) + ": ";
                    }
                    String c1 = "";
                    if (layer.containsKey(StringConstants.CLASSIFICATION1) && !StringConstants.NULL.equals(layer.get(StringConstants.CLASSIFICATION1))) {
                        c1 = layer.get(StringConstants.CLASSIFICATION1) + ": ";
                    }
                    myci.setDescription(c1 + c2 + type);
                    myci.setDisabled(false);
                    myci.setValue(field);
                }
            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            LOGGER.error("Error searching for layers:", e);
        }
    }
}
