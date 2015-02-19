/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.layer;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.net.URLEncoder;
import java.util.Iterator;

/**
 * same as LayersAutoComplete with type=Constants.ENVIRONMENTAL layers only
 *
 * @author ajay
 */
public class EnvLayersCombobox extends Combobox {

    private static final Logger LOGGER = Logger.getLogger(EnvLayersCombobox.class);
    private boolean includeAnalysisLayers = false;
    private String includeLayers;

    public EnvLayersCombobox() {
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

    public void refresh(String val) {
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

                JSONParser jp = new JSONParser();
                results = (JSONArray) jp.parse(slist);
            }
            LOGGER.debug("got " + results.size() + " layers");

            Sessions.getCurrent().setAttribute("layerlist", results);

            if (!results.isEmpty()) {

                for (int i = 0; i < results.size(); i++) {

                    JSONObject jo = (JSONObject) results.get(i);

                    if (!jo.get(StringConstants.ENABLED).toString().equalsIgnoreCase("true") || !jo.containsKey(StringConstants.DISPLAYNAME) || !jo.containsKey(StringConstants.TYPE)
                            || (!StringConstants.ENVIRONMENTAL.equalsIgnoreCase(jo.get(StringConstants.TYPE).toString())
                            && (includeLayers != null && !"AllLayers".equalsIgnoreCase(includeLayers)
                            && !"MixLayers".equalsIgnoreCase(includeLayers)))) {
                        continue;
                    }

                    String displayName = jo.get(StringConstants.DISPLAYNAME).toString();
                    String type = jo.get(StringConstants.TYPE).toString();
                    String name = jo.get(StringConstants.NAME).toString();

                    JSONObject layer = CommonData.getLayer(name);
                    boolean skip = false;
                    if (layer != null && layer.containsKey(StringConstants.FIELDS)) {
                        JSONArray ja = (JSONArray) layer.get(StringConstants.FIELDS);
                        for (int j = 0; j < ja.size() && !skip; j++) {
                            if (((JSONObject) ja.get(j)).containsKey(StringConstants.ADD_TO_MAP) && !((JSONObject) ja.get(j)).get(StringConstants.ADD_TO_MAP).toString().equalsIgnoreCase("true")) {
                                skip = true;
                            }
                        }
                    }

                    if (!skip) {
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
                        if (jo.containsKey(StringConstants.CLASSIFICATION2) && !StringConstants.NULL.equals(jo.get(StringConstants.CLASSIFICATION2))) {
                            c2 = jo.get(StringConstants.CLASSIFICATION2) + ": ";
                        }
                        String c1 = "";
                        if (jo.containsKey(StringConstants.CLASSIFICATION1) && !StringConstants.NULL.equals(jo.get(StringConstants.CLASSIFICATION1))) {
                            c1 = jo.get(StringConstants.CLASSIFICATION1) + ": ";
                        }
                        myci.setDescription(c1 + c2 + type);
                        myci.setValue(jo);
                    }
                }
            }

            if (includeAnalysisLayers) {
                for (MapLayer ml : getMapComposer().getAnalysisLayers()) {
                    String displayName = ml.getDisplayName();
                    String type;
                    String name;
                    String classification1;
                    String classification2;
                    if (ml.getSubType() == LayerUtilitiesImpl.MAXENT) {
                        type = StringConstants.ENVIRONMENTAL;
                        classification1 = StringConstants.ANALYSIS;
                        classification2 = StringConstants.PREDICTION;
                        name = ml.getName();
                    } else if (ml.getSubType() == LayerUtilitiesImpl.GDM) {
                        type = StringConstants.ENVIRONMENTAL;
                        classification1 = StringConstants.ANALYSIS;
                        classification2 = StringConstants.GDM;
                        name = ml.getName();
                    } else if (ml.getSubType() == LayerUtilitiesImpl.ODENSITY) {
                        type = StringConstants.ENVIRONMENTAL;
                        classification1 = StringConstants.ANALYSIS;
                        classification2 = StringConstants.OCCURRENCE_DENSITY;
                        name = ml.getName();
                    } else if (ml.getSubType() == LayerUtilitiesImpl.SRICHNESS) {
                        type = StringConstants.ENVIRONMENTAL;
                        classification1 = StringConstants.ANALYSIS;
                        classification2 = StringConstants.SPECIES_RICHNESS;
                        name = ml.getName();
                    } else {
                        continue;
                    }

                    Comboitem myci;
                    if (it != null && it.hasNext()) {
                        myci = ((Comboitem) it.next());
                        myci.setLabel(displayName);
                    } else {
                        it = null;
                        myci = new Comboitem(displayName);
                        myci.setParent(this);
                    }

                    myci.setDescription(classification1 + ": " + classification2 + " " + type);
                    myci.setDisabled(false);

                    JSONObject jo = new JSONObject();
                    jo.put(StringConstants.NAME, name);
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

    public boolean getIncludeAnalysisLayers() {
        return includeAnalysisLayers;
    }

    public void setIncludeAnalysisLayers(boolean includeAnalysisLayers) {
        this.includeAnalysisLayers = includeAnalysisLayers;
    }

    public String getIncludeLayers() {
        return includeLayers;
    }

    public void setIncludeLayers(String includeLayers) {
        this.includeLayers = includeLayers;
        LOGGER.debug("this.includeLayers=" + this.includeLayers);
    }

    public MapComposer getMapComposer() {
        return (MapComposer) Executions.getCurrent().getDesktop().getPage("MapZul").getFellow(StringConstants.MAPPORTALPAGE);
    }
}
