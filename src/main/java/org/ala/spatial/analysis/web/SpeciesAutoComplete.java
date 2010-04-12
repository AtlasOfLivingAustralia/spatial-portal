package org.ala.spatial.analysis.web;

import java.net.URLEncoder;
import java.util.Iterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {

        String snUrl = "http://ec2-184-73-34-104.compute-1.amazonaws.com/alaspatial/species/taxon/";

        try {

            /*
            if (val.length() == 0) {
                setDroppable("false");
            } else {
                setDroppable("true");
            }
             *
             */

            Iterator it = getItems().iterator();
            if (val.length() == 0) {
                Comboitem myci = null;
                if (it != null && it.hasNext()) {
                    myci = ((Comboitem) it.next());
                    myci.setLabel("Please start by typing in a species name...");
                } else {
                    it = null;
                    myci = new Comboitem("Please start by typing in a species name...");
                    myci.setParent(this);
                }
                myci.setDescription("");
                myci.setDisabled(true);
            } else {
                String nsurl = snUrl + URLEncoder.encode(val, "UTF-8");

                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(nsurl);
                get.addRequestHeader("Content-type", "text/plain");

                int result = client.executeMethod(get);
                String slist = get.getResponseBodyAsString();

                System.out.println("Response status code: " + result);
                System.out.println("Response: \n" + slist);

                String[] aslist = slist.split("\n");
                System.out.println("Got " + aslist.length + " records.");

                if (aslist.length > 0) {

                    for (int i = 0; i < aslist.length; i++) {
                        String[] spVal = aslist[i].split("/");

                        Comboitem myci = null;
                        if (it != null && it.hasNext()) {
                            myci = ((Comboitem) it.next());
                            myci.setLabel(spVal[0].trim());
                        } else {
                            it = null;
                            myci = new Comboitem(spVal[0].trim());
                            myci.setParent(this);
                        }
                        myci.setDescription(spVal[1].trim() + " - " + spVal[2].trim());
                        myci.setDisabled(false);
                    }
                }
                /*else {
                    if (it != null && it.hasNext()) {
                        ((Comboitem) it.next()).setLabel("No species found.");
                    } else {
                        it = null;
                        new Comboitem("No species found.").setParent(this);
                    }

                }*/

            }
            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshRemote");
            e.printStackTrace(System.out);

            //new Comboitem("No species found. error.").setParent(this);
        }

    }
}
