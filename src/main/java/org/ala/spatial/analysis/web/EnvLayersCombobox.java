/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class EnvLayersCombobox extends Combobox {

    private static final String SAT_URL = "sat_url";

    private String[] envLayers;
    private SettingsSupplementary settingsSupplementary = null;
    private String satServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"; // http://localhost:8080

    public EnvLayersCombobox(String value) throws WrongValueException {
        super(value);
        envLayers = setupEnvironmentalLayers();
    }

    public EnvLayersCombobox() {
        //refresh("");
        envLayers = setupEnvironmentalLayers();
    }

    @Override
    public void setValue(String value) throws WrongValueException {
        super.setValue(value);
        //refresh(value);
    }

    public void onChanging(InputEvent event) {
        if (!event.isChangingBySelectBack()) {
            //refresh(event.getValue());
        }
    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private String[] setupEnvironmentalLayers() {
        String[] aslist = null;
        try {

            if (settingsSupplementary != null) {
                System.out.println("setting sat_url for EnvLyr combo");
                satServer = settingsSupplementary.getValue(SAT_URL);
            } else {
                System.out.println("NOT setting sat_url for EnvLyr combo");
            }

            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";
  
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            aslist = slist.split("\n");

            System.out.println("Loading " + aslist.length + " env.layers for ALOC... ");

            Iterator it = getItems().iterator();
            for (int i = 0; i < aslist.length; i++) {
                if (aslist[i] == null) {
                    System.out.println("env.layer at " + i + " is null.");
                } else {
                System.out.println(">> " + aslist[i]);
                if (it != null && it.hasNext()) {
                    ((Comboitem) it.next()).setLabel(aslist[i] + " (Terrestrial)");
                } else {
                    it = null;
                    new Comboitem(aslist[i] + " (Terrestrial)").setParent(this);
                }
                }
            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }

        return aslist;
    }

    private void refresh(String val) {
        int j = Arrays.binarySearch(envLayers, val);
        if (j < 0) {
            j = -j - 1;
        }

        Iterator it = getItems().iterator();
        for (int cnt = 10; --cnt >= 0 && j < envLayers.length && envLayers[j].startsWith(val); ++j) {
            if (it != null && it.hasNext()) {
                ((Comboitem) it.next()).setLabel(envLayers[j]);
            } else {
                it = null;
                new Comboitem(envLayers[j]).setParent(this);
            }
        }

        while (it != null && it.hasNext()) {
            it.next();
            it.remove();
        }

    }
}
