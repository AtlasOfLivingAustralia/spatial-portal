package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
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

        String baseUrl = CommonData.getLayersServer() + "/layers/";
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

                results = JSONArray.fromObject(slist);
            }

            LOGGER.debug("got " + results.size() + " layers");

            Sessions.getCurrent().setAttribute("layerlist", results);

            if (!results.isEmpty()) {

                for (int i = 0; i < results.size(); i++) {

                    JSONObject jo = results.getJSONObject(i);

                    if (!jo.getBoolean(StringConstants.ENABLED) || (StringConstants.ENVIRONMENTAL.equalsIgnoreCase(jo.getString(StringConstants.TYPE)))) {
                        continue;
                    }

                    String displayName = jo.getString(StringConstants.DISPLAYNAME);
                    String type = jo.getString(StringConstants.TYPE);

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
                    if (jo.containsKey(StringConstants.CLASSIFICATION2) && !StringConstants.NULL.equals(jo.getString(StringConstants.CLASSIFICATION2))) {
                        c2 = jo.getString(StringConstants.CLASSIFICATION2) + ": ";
                    }
                    String c1 = "";
                    if (jo.containsKey(StringConstants.CLASSIFICATION1) && !StringConstants.NULL.equals(jo.getString(StringConstants.CLASSIFICATION1))) {
                        c1 = jo.getString(StringConstants.CLASSIFICATION1) + ": ";
                    }
                    myci.setDescription(c1 + c2 + type);
                    myci.setDisabled(false);
                    myci.setValue(jo);
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
