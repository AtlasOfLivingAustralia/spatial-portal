/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

/**
 *
 * @author ajay
 */
public class ALOCWCController extends UtilityComposer {

    private Listbox lbenvlayers;
    private Combobox cbEnvLayers;
    private Label lblNoLayersSelected;
    private Listbox lbSelLayers;
    private Button btnDeleteSelected;
    private Textbox groupCount;
    private Button btnGenerate;
    private List<String> selectedLayers;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  // http://localhost:8080
    private String satServer = geoServer;

    @Override
    public void afterCompose() {
        super.afterCompose();

        setupEnvironmentalLayers(); 
        
        selectedLayers = new Vector<String>();

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private void setupEnvironmentalLayers() {
        try {

            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";

            //Messagebox.show("Loading env data from: " + envurl);


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //Messagebox.show("done loading data: " + slist);

            String[] aslist = slist.split("\n");

            if (aslist.length > 0) {

                lbenvlayers.setItemRenderer(new ListitemRenderer() {

                    public void render(Listitem li, Object data) {
                        li.setWidth(null);
                        new Listcell((String) data).setParent(li);
                    }
                });

                lbenvlayers.setModel(new SimpleListModel(aslist));
            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    public void onChange$cbEnvLayers(Event event) {
        String new_value = "";

        new_value = cbEnvLayers.getValue();
        if (new_value.equals("")) {
            return;
        }
        new_value = (new_value.equals("")) ? "" : new_value.substring(0, new_value.indexOf("(")).trim();
        System.out.println("new value: " + new_value);

        if (selectedLayers.contains(new_value)) {
            System.out.println("is not new");
            return;
        } else {
            System.out.println("is new");
            selectedLayers.add(new_value);
        }

        //lbSelLayers.appendItem(new_value, new_value);

        lbSelLayers.setModel(new SimpleListModel(selectedLayers));

        toggleVisibleComponents();

    }

    public void onClick$btnDeleteSelected(Event event) {
        Iterator it = lbSelLayers.getSelectedItems().iterator();
        while (it.hasNext()) {
            Listitem li = (Listitem) it.next();
            selectedLayers.remove(li.getLabel());
        }

        lbSelLayers.setModel(new SimpleListModel(selectedLayers));

        toggleVisibleComponents();

    }

    private void toggleVisibleComponents() {
        if (selectedLayers.size() == 0) {
            lbSelLayers.setVisible(false);
            lblNoLayersSelected.setVisible(true);
        } else {
            lbSelLayers.setVisible(true);
            lblNoLayersSelected.setVisible(false);
        }

    }

    public void onClick$btnGenerate(Event event) {
        try {
            StringBuffer sbenvsel = new StringBuffer();

            /*
            if (selectedLayers.size() > 0) {
                Iterator it = selectedLayers.iterator();
                while (it.hasNext()) {
                    sbenvsel.append(it.next());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }
                }
            } else {
                Messagebox.show("Please select some environmental layers","ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }
            *
            */
            if (lbenvlayers.getSelectedCount() > 0) {
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }

                }
            } else {
                Messagebox.show("Please select some environmental layers","ALA Spatial Toolkit", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/aloc/process?");
            sbProcessUrl.append("gc=" + URLEncoder.encode(groupCount.getValue(), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString()); // testurl
            //get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from ALOCWSController: \n" + slist);

            String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:aloc_class_" + slist + "&styles=&srs=EPSG:4326&TRANSPARENT=true&FORMAT=image%2Fpng";

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            mc.addWMSLayer("ALOC " + slist, mapurl, (float) 0.5);

        } catch (Exception ex) {
            //Logger.getLogger(ALOCWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Opps!: ");
            ex.printStackTrace(System.out);
        }

    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        //Page page = maxentWindow.getPage();
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}