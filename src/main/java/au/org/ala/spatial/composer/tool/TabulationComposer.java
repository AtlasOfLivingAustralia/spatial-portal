/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.FieldDTO;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.util.*;

/**
 * @author ajay
 */
public class TabulationComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(TabulationComposer.class);
    private Combobox cbTabLayers1;
    private Combobox cbTabLayers2;
    private Combobox cbTabType;
    private Map<FieldDTO, List<FieldDTO>> tabLayers;
    private Map<String, List<String>> tabLayerDisplayNames;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = StringConstants.TABULATION;
        this.totalSteps = 1;

        this.updateWindowTitle();
        cbTabLayers1.setFocus(true);

        tabLayers = new HashMap<FieldDTO, List<FieldDTO>>();
        tabLayerDisplayNames = new HashMap<String, List<String>>();

        try {
            int i;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(CommonData.getLayersServer() + "/tabulations.json");
            get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);
            String tlayers = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONObject joTop = (JSONObject) jp.parse(tlayers);
            JSONArray joarr = (JSONArray) joTop.get("tabulations");
            for (i = 0; i < joarr.size(); i++) {
                JSONObject jo = (JSONObject) joarr.get(i);
                FieldDTO f1 = new FieldDTO(jo.get("fid1").toString(), jo.get("name1").toString(), "");
                FieldDTO f2 = new FieldDTO(jo.get("fid2").toString(), jo.get("name2").toString(), "");
                load(f1, f2);
                load(f2, f1);
            }
            Set keySet = (Set) tabLayerDisplayNames.keySet();
            List keyList = new ArrayList(keySet);
            Collections.sort(keyList);
            LOGGER.debug("keyList1=" + keyList);
            for (int j = 0; j < keyList.size(); j++) {
                String temp = (String) keyList.get(j);
                LOGGER.debug("temp=" + temp);
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
            LOGGER.debug("Unable to call tabulation service for a list of layers", e);
        }
    }

    private void load(FieldDTO f1, FieldDTO f2) {
        List<FieldDTO> f2s = getField2(f1);
        List<String> f2DisplayNames = getFieldDisplayName2(f1.getDisplayName());
        if (f2s == null) {
            f2s = new ArrayList<FieldDTO>();
            f2s.add(f2);
            tabLayers.put(f1, f2s);
            f2DisplayNames = new ArrayList<String>();
            f2DisplayNames.add(f2.getDisplayName());
            tabLayerDisplayNames.put(f1.getDisplayName(), f2DisplayNames);
        } else {
            f2s.add(f2);
            f2DisplayNames.add(f2.getDisplayName());
        }
    }

    private List<FieldDTO> getField2(FieldDTO f1) {
        List<FieldDTO> f2s = null;

        Iterator<FieldDTO> it = tabLayers.keySet().iterator();
        while (it.hasNext()) {
            FieldDTO fi = it.next();
            if (f1.getName().equals(fi.getName())) {
                f2s = tabLayers.get(fi);
                break;
            }
        }

        return f2s;
    }

    private List<String> getFieldDisplayName2(String f1DisplayName) {
        List<String> f2DisplayNames = null;
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
        List<String> f2DisplayNamess = getFieldDisplayName2(f1DisplayNames);

        if (!cbTabLayers2.getChildren().isEmpty()) {
            cbTabLayers2.getChildren().clear();
        }

        if (f2DisplayNamess != null) {
            for (int i = 0; i < f2DisplayNamess.size(); i++) {
                String f2 = f2DisplayNamess.get(i);
                Comboitem ci = new Comboitem(f2);
                ci.setValue(f2);
                ci.setParent(cbTabLayers2);
            }
            cbTabLayers2.setSelectedIndex(0);
        } else {
            LOGGER.debug("f2DisplayNames == null");
        }
    }

    @Override
    public boolean onFinish() {
        String f1DisplayName = cbTabLayers1.getSelectedItem().getValue();
        String f2DisplayName = cbTabLayers2.getSelectedItem().getValue();
        String f1Name = "", f2Name = "";
        Iterator<FieldDTO> it = tabLayers.keySet().iterator();
        while (it.hasNext()) {
            FieldDTO fi = it.next();
            if (f1DisplayName.equals(fi.getDisplayName())) {
                f1Name = fi.getName();
            } else if (f2DisplayName.equals(fi.getDisplayName())) {
                f2Name = fi.getName();
            }
        }

        Event e = new Event("tabulation", this, CommonData.getLayersServer() + "/tabulation/" + cbTabType.getSelectedItem().getValue() + "/" + f1Name + "/" + f2Name + "/tabulation.html" + "\n" + StringConstants.TABULATION + "\n" + CommonData.getLayersServer() + "/tabulation/" + cbTabType.getSelectedItem().getValue() + "/" + f1Name + "/" + f2Name + "/tabulation.csv");
        getMapComposer().openUrl(e);

        remoteLogger.logMapAnalysis(StringConstants.TABULATION, "Tool - Tabulation", "", "", f1Name + ":" + f2Name, "", (String) cbTabType.getSelectedItem().getValue(), StringConstants.STARTED);

        this.detach();

        return true;
    }

}
