package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
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

    private static Logger logger = Logger.getLogger(ContextualLayersAutoComplete.class);

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

        //TODO get this from the config file
        if (settingsSupplementary != null) {
        } else if (this.getParent() != null) {
            settingsSupplementary = settingsSupplementary = ((MapComposer) getPage().getFellow("mapPortalPage")).getSettingsSupplementary();
            logger.debug("LAC got SS: " + settingsSupplementary);
        } else {
            return;
        }

        String baseUrl = CommonData.layersServer + "/layers/";
        try {

            Iterator it = getItems().iterator();

            JSONArray results = null;
            String lsurl = baseUrl;
            if (val.length() == 0) {
                results = CommonData.getLayerListJSONArray();
            } else {
                lsurl += "search/?q=" + URLEncoder.encode(val, "UTF-8");

                logger.debug("nsurl: " + lsurl);

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(lsurl);

                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                results = JSONArray.fromObject(slist);
            }

            logger.debug("got " + results.size() + " layers");

            Sessions.getCurrent().setAttribute("layerlist", results);

            if (results.size() > 0) {

                for (int i = 0; i < results.size(); i++) {

                    JSONObject jo = results.getJSONObject(i);

                    if (!jo.getBoolean("enabled") || (jo.getString("type").equalsIgnoreCase("environmental"))) {
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
                    if (jo.containsKey("classification2") && !jo.getString("classification2").equals("null")) {
                        c2 = jo.getString("classification2") + ": ";
                    }
                    String c1 = "";
                    if (jo.containsKey("classification1") && !jo.getString("classification1").equals("null")) {
                        c1 = jo.getString("classification1") + ": ";
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
            logger.error("Error searching for layers:", e);
        }
    }
}
