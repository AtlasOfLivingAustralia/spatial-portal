package org.ala.spatial.web;

import java.net.URLEncoder;
import java.util.Iterator;
import org.ala.spatial.analysis.index.OccurrencesCollection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
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
        String snUrl = "http://data.ala.org.au/search/scientificNames/_name_*/json";
        String cnUrl = "http://data.ala.org.au/search/commonNames/_name_*/json";

        String urlOptionSort = "sort";
        String urlOptionDir = "dir";
        String urlOptionStart = "startIndex";
        String urlOptionPageSize = "results";

        try {

            if (val.length() < 4) {
                return;
            }

            String nsurl = snUrl.replace("_name_", URLEncoder.encode(val, "UTF-8"));

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nsurl);

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got JSON.slist for " + nsurl + ": \n" + slist);


            JsonFactory f = new JsonFactory();
            JsonParser jp = f.createJsonParser(slist);

            System.out.println("jp.init: " + jp.getCurrentName());

            jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jp.getCurrentName();
                JsonToken jt = jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                if ("result".equals(fieldname)) { // contains an object
                    /*
                    while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String namefield = jp.getCurrentName();
                    jp.nextToken(); // move to value
                    if ("scientificName".equals(namefield)) {
                    name.setFirst(jp.getText());
                    } else if ("scientificNameUrl".equals(namefield)) {
                    name.setLast(jp.getText());
                    } else if ("occurrenceCoordinateCount".equals(namefield)) {
                    name.setLast(jp.getText());
                    }

                    //else {
                    //    throw new IllegalStateException("Unrecognized field '" + fieldname + "'!");
                    //}


                    }
                     *
                     */
                    //user.setName(name);
                    System.out.println("Got result: " + jt.name());
                } else if ("recordsReturned".equals(fieldname)) {
                    //user.setGender(Gender.valueOf(jp.getText()));
                    System.out.println("pagesize: " + jp.getText());
                } else if ("totalRecords".equals(fieldname)) {
                    //user.setVerified(jp.getCurrentToken() == JsonToken.VALUE_TRUE);
                    System.out.println("total: " + jp.getText());
                } else if ("startIndex".equals(fieldname)) {
                    //user.setUserImage(jp.getBinaryValue());
                    System.out.println("startIndex: " + jp.getText());
                }
                //else {
                //    throw new IllegalStateException("Unrecognized field '" + fieldname + "'!");
                //}
            }
            jp.close(); // ensure resources get cleaned up timely and properly







        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshJSON");
            e.printStackTrace(System.out);

            //new Comboitem("No species found. error.").setParent(this);
        }
    }

    public void refresh(String val) {
        try {

            if (val.length() == 0) {
                setDroppable("false");
            } else {
                setDroppable("true");
            }

            String[] aslist = OccurrencesCollection.findSpecies(val, 40);
            if (aslist == null) {
                aslist = new String[1];
                aslist[0] = "";
            }
            System.out.print("#" + aslist.length);

            Iterator it = getItems().iterator();

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
            System.out.println("Species.AutoComplete error: ");
            e.printStackTrace(System.out);
        }
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refreshBIE(String val) {

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
                    Comboitem myci = null;
                    if (it != null && it.hasNext()) {
                        myci = ((Comboitem) it.next());
                        myci.setLabel(aslist[i]);
                    } else {
                        it = null;
                        myci = new Comboitem(aslist[i]);
                        myci.setParent(this);
                    }
                    myci.setDescription("description goes here... ");
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
