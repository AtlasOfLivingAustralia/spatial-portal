package org.ala.spatial.web.zk;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;

/**
 * Sample page for GDM
 * 
 * @author ajay
 */
public class GdmZK extends GenericForwardComposer {

    private Combobox quantile;
    private Button runGdm;
    private String satServer = null;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
    }

    public void onClick$runGdm(Event event) {
        System.out.println("runGdm clicked");
        startGDM();
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
