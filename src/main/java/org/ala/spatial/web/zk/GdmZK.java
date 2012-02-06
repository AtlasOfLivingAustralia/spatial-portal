package org.ala.spatial.web.zk;

import java.net.URLEncoder;
import java.util.Iterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;

/**
 * Sample page for GDM
 * 
 * @author ajay
 */
public class GdmZK extends GenericForwardComposer {

    private Combobox quantile;
    private Button runGdm;
    private String satServer = null;
    private Listbox lbenvlayers;
    private Listbox lbsplist;
    private Combobox sac;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
    }

    public void onSelect$lbenvlayers(Event event) {
        System.out.println("Selected layers: ");
        Iterator<Listitem> it = lbenvlayers.getSelectedItems().iterator();
        while (it.hasNext()) {
            Listitem li = it.next();
            System.out.println(li.getLabel() + " => " + li.getValue());
        }

    }

    public void onClick$addToMap(Event event) {
        String[] spVal = sac.getSelectedItem().getDescription().split(" ");
        lbsplist.appendItem(sac.getText() + "("+spVal[0]+")", spVal[0]);
    }

    public void onClick$runGdm(Event event) {
        System.out.println("runGdm clicked");
        startGDM2();
    }

    private void startGDM2() {
        try {
            System.out.println("Starting GDM2");

            String taxons = "";
            String envlayers = "";

            // load the species
            Iterator<Listitem> it = lbsplist.getItems().iterator(); 
            while (it.hasNext()) {
                Listitem li = it.next();
                System.out.println(li.getLabel() + " => " + li.getValue());
                String sp = (String)li.getValue();
                taxons+=URLEncoder.encode(sp.replace(".", "__"), "UTF-8") + ";";
            }

            // load the env layers
            it = lbenvlayers.getSelectedItems().iterator();
            while (it.hasNext()) {
                Listitem li = it.next();
                System.out.println(li.getLabel() + " => " + li.getValue());
                envlayers+=li.getValue() + ";";
            }

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("http://spatial-dev.ala.org.au/alaspatial/ws/gdm/process2?");
            sbProcessUrl.append("taxons=" + taxons);
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(envlayers, "UTF-8"));
            sbProcessUrl.append("&quantile=" + quantile.getValue());
            sbProcessUrl.append("&useDistance=1");
            sbProcessUrl.append("&useSubSample=0");

            System.out.println("gdm.url: " + sbProcessUrl.toString());


            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            //get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String output = get.getResponseBodyAsString();

            System.out.println("output: \n" + output);

        } catch (Exception e) {
            System.out.println("Unable to run GDM2:");
            e.printStackTrace(System.out);
        }
    }

    private void startGDM() {
        try {
            System.out.println("Starting GDM");

            String taxon = "urn:lsid:biodiversity.org.au:apni.taxon:295864";
            String envlayers = "evapm:evap_mean";
            String area = "POLYGON((89.264 -50.06,89.264 6.322,178.736 6.322,178.736 -50.06,89.264 -50.06))";

            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("http://spatial-dev.ala.org.au/alaspatial/ws/gdm/process?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon.replace(".", "__"), "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(envlayers, "UTF-8"));
            sbProcessUrl.append("&quantile=" + quantile.getValue());
            sbProcessUrl.append("&useDistance=1");
            sbProcessUrl.append("&useSubSample=0");


            HttpClient client = new HttpClient();
            PostMethod get = new PostMethod(sbProcessUrl.toString());
            get.addParameter("area", area);
            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String output = get.getResponseBodyAsString();

            System.out.println("output: \n" + output);

        } catch (Exception ex) {
            //Logger.getLogger(GdmZK.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error with startGDM");
            ex.printStackTrace(System.out);
        }

    }
}
