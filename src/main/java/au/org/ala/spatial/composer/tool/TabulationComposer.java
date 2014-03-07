/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Field;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.util.*;

/**
 * @author ajay
 */
public class TabulationComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(TabulationComposer.class);
    Combobox cbTabLayers1;
    Combobox cbTabLayers2;
    Combobox cbTabType;
    HashMap<Field, ArrayList<Field>> tabLayers;
    HashMap<String, ArrayList<String>> tabLayerDisplayNames;
    ArrayList keyList;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Tabulation";
        this.totalSteps = 1;

        this.updateWindowTitle();
        cbTabLayers1.setFocus(true);

        tabLayers = new HashMap<Field, ArrayList<Field>>();
        tabLayerDisplayNames = new HashMap<String, ArrayList<String>>();

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
                load(f2, f1);
            }
            Set keySet = (Set) tabLayerDisplayNames.keySet();
            keyList = new ArrayList(keySet);
            Collections.sort(keyList);
            logger.debug("keyList1=" + keyList);
            for (int j = 0; j < keyList.size(); j++) {
                String temp = (String) keyList.get(j);
                logger.debug("temp=" + temp);
                Comboitem ci = new Comboitem(temp);
                ci.setValue(temp);
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
            logger.debug("Unable to call tabulation service for a list of layers", e);
        }
    }

    private void load(Field f1, Field f2) {
        ArrayList<Field> f2s = getField2(f1);
        ArrayList<String> f2DisplayNames = getFieldDisplayName2(f1.display_name);
        if (f2s == null) {
            f2s = new ArrayList<Field>();
            f2s.add(f2);
            tabLayers.put(f1, f2s);
            f2DisplayNames = new ArrayList<String>();
            f2DisplayNames.add(f2.display_name);
            tabLayerDisplayNames.put(f1.display_name, f2DisplayNames);
        } else {
            f2s.add(f2);
            f2DisplayNames.add(f2.display_name);
        }
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

    private ArrayList<String> getFieldDisplayName2(String f1DisplayName) {
        ArrayList<String> f2DisplayNames = null;
        Iterator<String> it = tabLayerDisplayNames.keySet().iterator();
        while (it.hasNext()) {
            String fiDisplayName = it.next();
            if (f1DisplayName.equalsIgnoreCase(fiDisplayName)) {
                f2DisplayNames = tabLayerDisplayNames.get(fiDisplayName);
                Collections.sort(f2DisplayNames);
                break;
            }
        }
        return f2DisplayNames;
    }

    public void onChange$cbTabLayer1(Event event) {

        String f1DisplayNames = cbTabLayers1.getSelectedItem().getValue();
        ArrayList<String> f2DisplayNamess = getFieldDisplayName2(f1DisplayNames);

        if (cbTabLayers2.getChildren().size() > 0) {
            cbTabLayers2.getChildren().clear();
        }

        if (f2DisplayNamess != null) {
            //sortStringBubble(f2s);
            for (int i = 0; i < f2DisplayNamess.size(); i++) {
                String f2 = f2DisplayNamess.get(i);
                Comboitem ci = new Comboitem(f2);
                ci.setValue(f2);
                ci.setParent(cbTabLayers2);
            }
            cbTabLayers2.setSelectedIndex(0);
        } else {
            logger.debug("f2DisplayNames == null");
        }
    }

    @Override
    public boolean onFinish() {
        String f1DisplayName = cbTabLayers1.getSelectedItem().getValue();
        String f2DisplayName = cbTabLayers2.getSelectedItem().getValue();
        String f1Name = "", f2Name = "";
        Iterator<Field> it = tabLayers.keySet().iterator();
        while (it.hasNext()) {
            Field fi = it.next();
            if (f1DisplayName.equals(fi.display_name)) {
                f1Name = fi.name;
            } else if (f2DisplayName.equals(fi.display_name)) {
                f2Name = fi.name;
            } else {
                continue;
            }
        }

        //sb.append("Tabulation for " + f1.display_name + " and " + f2.display_name);


        Event e = new Event("tabulation", this, CommonData.layersServer + "/tabulation/" + cbTabType.getSelectedItem().getValue() + "/" + f1Name + "/" + f2Name + "/tabulation.html" + "\n" + "Tabulation" + "\n" + CommonData.layersServer + "/tabulation/" + cbTabType.getSelectedItem().getValue() + "/" + f1Name + "/" + f2Name + "/tabulation.csv");
        getMapComposer().openUrl(e);

        remoteLogger.logMapAnalysis("Tabulation", "Tool - Tabulation", "", "", f1Name + ":" + f2Name, "", ((String) cbTabType.getSelectedItem().getValue()), "STARTED");

        this.detach();

        return true;
    }

    public static void sortStringBubble(ArrayList<Field> fs) {
        boolean flag = true;  // will determine when the sort is finished
        while (flag) {
            for (int i = 0; i < fs.size() - 1; i++) {
                Field element1 = fs.get(i);
                Field element2 = fs.get(i + 1);
                Field temp;
                if (element1.display_name.compareToIgnoreCase(element2.display_name) > 0) {
                    temp = element1;
                    element1 = element2;
                    element2 = temp;
                    flag = true;

                }
            }
        }
    }

}
