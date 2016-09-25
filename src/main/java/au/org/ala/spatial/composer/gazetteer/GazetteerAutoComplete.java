package au.org.ala.spatial.composer.gazetteer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.util.Iterator;

public class GazetteerAutoComplete extends Combobox {

    private static final Logger LOGGER = Logger.getLogger(GazetteerAutoComplete.class);

    public GazetteerAutoComplete() {
        refresh("");
    }

    /**
     * Listens for what a user is entering.
     *
     * @param evt
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    /**
     * Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {
        String searchString = val.trim().replaceAll("\\s+", "+");

        searchString = (searchString.isEmpty()) ? "a" : searchString;

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getLayersServer() + "/search?limit=40&q=" + searchString + "&userObjects=false");
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONArray ja = (JSONArray) jp.parse(slist);

            if (ja == null) {
                return;
            }

            Iterator it = getItems().iterator();

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                String itemString = jo.get(StringConstants.NAME).toString();
                String description = (jo.containsKey(StringConstants.DESCRIPTION) ? jo.get(StringConstants.DESCRIPTION).toString() : "")
                        + " (" + jo.get("fieldname") + ")";

                if (it != null && it.hasNext()) {
                    Comboitem ci = (Comboitem) it.next();
                    ci.setLabel(itemString);
                    ci.setValue(jo);
                    ci.setDescription(description);
                } else {
                    it = null;
                    Comboitem ci = new Comboitem();
                    ci.setLabel(itemString);
                    ci.setValue(jo);
                    ci.setDescription(description);
                    ci.setParent(this);
                }
            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            LOGGER.error("error selecting gaz autocomplete item", e);
        }
    }

}
