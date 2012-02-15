/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.util.Field;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Messagebox;
import org.ala.spatial.util.CommonData;

/**
 *
 * @author ajay
 */
public class AddToolTabulationComposer extends AddToolComposer {

    Combobox cbTabLayers1;
    Combobox cbTabLayers2;
    Combobox cbTabType;
    HashMap<Field, ArrayList<Field>> tabLayers;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Tabulation";
        this.totalSteps = 1;

        this.updateWindowTitle();
        cbTabLayers1.setFocus(true);

        tabLayers = new HashMap<Field, ArrayList<Field>>();

        try {
            int i = 0;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.layersServer + "/tabulations.json");
            get.addRequestHeader("Accept", "application/json");
            int result = client.executeMethod(get);
            String tlayers = get.getResponseBodyAsString();

            JSONObject joTop = JSONObject.fromObject(tlayers);
            JSONArray joarr = joTop.getJSONArray("tabulations");
            for (i = 0; i < joarr.size(); i++) {
                JSONObject jo = joarr.getJSONObject(i);
                Field f1 = new Field(jo.getString("fid1"), jo.getString("name1"), "");
                Field f2 = new Field(jo.getString("fid2"), jo.getString("name2"), "");
                load(f1, f2);
            }

            Iterator<Field> it = tabLayers.keySet().iterator();
            while (it.hasNext()) {
                Field f1 = it.next();

                Comboitem ci = new Comboitem(f1.display_name);
                ci.setValue(f1);
                ci.setParent(cbTabLayers1);
            }

            cbTabLayers1.addEventListener("onChange", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    onChange$cbTabLayer1(event);
                }
            });

            cbTabLayers1.setSelectedIndex(0);
            onChange$cbTabLayer1(null);
            cbTabLayers2.setSelectedIndex(0);
            cbTabType.setSelectedIndex(0);

        } catch (Exception e) {
            System.out.println("Unable to call tabulation service for a list of layers");
            e.printStackTrace(System.out);
        }
    }

    private void load(Field f1, Field f2) {
        ArrayList<Field> f2s = getField2(f1);

        if (f2s == null) {
            f2s = new ArrayList<Field>();
            f2s.add(f2);
            tabLayers.put(f1, f2s);
        } else {
            f2s.add(f2);
        }
    }

    public void onChange$cbTabType(Event event) {
        System.out.println("Selected type: " + cbTabType.getSelectedItem().getValue());
        System.out.println("type: " + (String)event.getData());
    }
    public void onChange$cbTabLayer1(Event event) {

        Field f1 = (Field) cbTabLayers1.getSelectedItem().getValue();
        ArrayList<Field> f2s = getField2(f1);

        if (cbTabLayers2.getChildren().size() > 0) {
            cbTabLayers2.getChildren().clear();
        }

        if (f2s != null) {
            for (int i = 0; i < f2s.size(); i++) {
                Field f2 = f2s.get(i);
                Comboitem ci = new Comboitem(f2.display_name);
                ci.setValue(f2);
                ci.setParent(cbTabLayers2);
            }
            cbTabLayers2.setSelectedIndex(0);
        }
    }

    @Override
    public boolean onFinish() {
        Field f1 = (Field) cbTabLayers1.getSelectedItem().getValue();
        Field f2 = (Field) cbTabLayers2.getSelectedItem().getValue();

        System.out.println("2.Selected type: " + cbTabType.getSelectedItem().getValue());

        String tabfor = "area";
        if (cbTabType.getSelectedItem().getValue().equals("area")) {
            tabfor = "area";
        } else if (cbTabType.getSelectedItem().getValue().equals("arearp")) {
            tabfor = "area/row";
        } else if (cbTabType.getSelectedItem().getValue().equals("areacp")) {
            tabfor = "area/column";
        } else if (cbTabType.getSelectedItem().getValue().equals("species")) {
            tabfor = "species";
        } else if (cbTabType.getSelectedItem().getValue().equals("speciesrp")) {
            tabfor = "species/row";
        } else if (cbTabType.getSelectedItem().getValue().equals("speciescp")) {
            tabfor = "species/column";
        } else if (cbTabType.getSelectedItem().getValue().equals("occurrence")) {
            tabfor = "occurrences";
        } else if (cbTabType.getSelectedItem().getValue().equals("occurrencerp")) {
            tabfor = "occurrences/row";
        } else if (cbTabType.getSelectedItem().getValue().equals("occurrencecp")) {
            tabfor = "occurrences/column";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CommonData.layersServer + "/tabulation/" + tabfor + "/" + f1.name + "/" + f2.name + "/html");
        sb.append("\n").append("Tabulation").append("\n");
        sb.append(CommonData.layersServer + "/tabulation/" + tabfor + "/" + f1.name + "/" + f2.name + "/csv");
        //sb.append("Tabulation for " + f1.display_name + " and " + f2.display_name);


        Event e = new Event("tabulation", this, sb.toString());
        getMapComposer().openUrl(e);

        //this.detach();

        return true;
    }

    private ArrayList<Field> getField2(Field f1) {
        ArrayList<Field> f2s = null;

        Iterator<Field> it = tabLayers.keySet().iterator();
        while (it.hasNext()) {
            Field fi = it.next();
            if (f1.name.equals(fi.name)) {
                f2s = tabLayers.get(fi);
                break;
            }
        }

        return f2s;
    }
}
