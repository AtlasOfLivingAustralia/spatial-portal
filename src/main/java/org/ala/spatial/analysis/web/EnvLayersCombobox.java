/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Executions;
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

    String[] validLayers = null;
    boolean includeAnalysisLayers = false;
    String includeLayers;

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

    public void refresh(String val) {
        if (validLayers == null) {
            makeValidLayers();
        }

        String baseUrl = CommonData.satServer + "/ws/layers/";
        try {
            Iterator it = getItems().iterator();

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
                get.addRequestHeader("Accept", "application/json, text/javascript, */*");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();


                results = JSONArray.fromObject(slist);
            }
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
                    String name = jo.getString("name");

                    //if (!type.equalsIgnoreCase("environmental")) {
                    //    continue;
                    //}

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
                    
                    if (!type.equalsIgnoreCase("environmental") && this.getIncludeLayers() == "EnvironmentalLayers") {
                        myci.setDisabled(true);
                    } else if (!type.equalsIgnoreCase("environmental") && this.getIncludeLayers() == "MixLayers" 
                                    && !name.equalsIgnoreCase("landcover")
                                    && !name.equalsIgnoreCase("landuse")
                                    && !name.equalsIgnoreCase("vast")
                                    && !name.equalsIgnoreCase("native_veg")
                                    && !name.equalsIgnoreCase("present_veg")) {
                                myci.setDisabled(true);
                    } else {
                        myci.setDisabled(false);
                    }
                    //myci.setDisabled(false);
                    myci.setValue(jo);
                }
            }

            if(includeAnalysisLayers) {
                for(MapLayer ml : getMapComposer().getAnalysisLayers()) {
                    String displayName = ml.getDisplayName();
                    String type = null;
                    String name = null;
                    String classification1 = null;
                    String classification2 = null;
                    if(ml.getSubType() == LayerUtilities.ALOC) {
                        type = "contextual";
                        name = (String)ml.getData("pid");
                        classification1 = "Analysis";
                        classification2 = "Classification";
                        continue;   //no contextuals
                    } else if(ml.getSubType() == LayerUtilities.MAXENT) {
                        type = "environmental";
                        classification1 = "Analysis";
                        classification2 = "Prediction";
                        name = (String)ml.getData("pid");
                    } else if(ml.getSubType() == LayerUtilities.ODENSITY) {
                        type = "environmental";
                        classification1 = "Analysis";
                        classification2 = "Occurrence Density";
                        name = (String)ml.getData("pid");
                    } else if(ml.getSubType() == LayerUtilities.SRICHNESS) {
                        type = "environmental";
                        classification1 = "Analysis";
                        classification2 = "Species Richness";
                        name = (String)ml.getData("pid");
                    } else {
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

                    myci.setDescription(classification1 + ": " + classification2 + " " + type);
                    myci.setDisabled(false);

                    JSONObject jo = new JSONObject();
                    jo.put("name", name);
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
        System.out.println("this.includeLayers="+this.includeLayers);
    }

    public MapComposer getMapComposer() {
            return (MapComposer) Executions.getCurrent()
                            .getDesktop()
                                    .getPage("MapZul")
                                            .getFellow("mapPortalPage");
    }
}
