package org.ala.spatial.web;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class SpeciesAutoComplete extends Combobox {

    public SpeciesAutoComplete() {
        refresh(""); //init the child comboitems
    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        refresh(value); //refresh the child comboitems
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }



    private void refreshJSON(String val) {
        String snUrl = "";
        String cnUrl = "";
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {

        String snUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/view/ajaxTaxonName?query=";
        String cnUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/returnType/commonName/view/ajaxTaxonName?query=";

        try {

            String nsurl = snUrl + URLEncoder.encode(val, "UTF-8");

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nsurl);

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Response status code: " + result);
            System.out.println("Response: \n" + slist);

            String[] aslist = slist.split("\n");
            System.out.println("Got " + aslist.length + " records.");

            Iterator it = getItems().iterator();
            if (aslist.length > 0) {

                for (int i = 0; i < aslist.length; i++) {
                    if (it != null && it.hasNext()) {
                        ((Comboitem) it.next()).setLabel(aslist[i]);
                    } else {
                        it = null;
                        new Comboitem(aslist[i]).setParent(this);
                    }
                }
            } else {
                if (it != null && it.hasNext()) {
                    ((Comboitem) it.next()).setLabel("No species found.");
                } else {
                        it = null;
                        new Comboitem("No species found.").setParent(this);
                    }
                
            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshRemote");
            e.printStackTrace(System.out);

            new Comboitem("No species found. error.").setParent(this);
        }
    }
    
}
