package org.ala.spatial.analysis.web;

import au.org.emii.portal.settings.SettingsSupplementary;
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

    //TODO get these from the config file
    private static final String COMMON_NAME_URL = "common_name_url";
    private static final String SAT_URL = "sat_url";
    //private static final String cnUrl = "data.ala.org.au";
    //private static final String commonSearch = "/search/commonNames/";
    private String cnUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/returnType/commonName/view/ajaxTaxonName?query=_query_";
    private String satServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"; // http://localhost:8080
    private boolean bSearchCommon = false;
    private SettingsSupplementary settingsSupplementary = null;

    public boolean isSearchCommon() {
        return bSearchCommon;
    }

    public void setSearchCommon(boolean searchCommon) {
        this.bSearchCommon = searchCommon;
    }

    public SpeciesAutoComplete() {
        refresh(""); //init the child comboitems
        System.out.println("setting cnurl in sac()");
        //cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
        System.out.println("setting satserver in sac()");
        //satServer = settingsSupplementary.getValue(SAT_URL);
    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
        System.out.println("setting cnurl in sac(val)");
        //cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
        System.out.println("setting satserver in sac(val)");
        //satServer = settingsSupplementary.getValue(SAT_URL);
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

    private void refreshBIE(String val) {
        //String cnUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/returnType/commonName/view/ajaxTaxonName?query=";
        try {
            //TODO get this from the config file
            if (settingsSupplementary != null) {
                System.out.println("setting ss.bie.val");
                cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
            } else {
                System.out.println("NOT setting ss.bie.val");
            }

            String nsurl = cnUrl.replaceAll("_query_", URLEncoder.encode(val, "UTF-8"));
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nsurl);

            System.out.println(nsurl);

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
                    //myci.setDescription("description goes here... ");
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
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshBIE");
            e.printStackTrace(System.out);

            new Comboitem("No species found. error.").setParent(this);
        }
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {

        //TODO get this from the config file
        if (settingsSupplementary != null) {
            System.out.println("setting ss.val");
            satServer = settingsSupplementary.getValue(SAT_URL);
        } else {
            System.out.println("NOT setting ss.val");
        }

        String snUrl = satServer + "/alaspatial/species/taxon/";
        //String snUrl = "http://localhost:8080/alaspatial/species/taxon/";


        try {

            System.out.println("Looking for common name: " + isSearchCommon());

            if (isSearchCommon()) {
                if (val.trim().equalsIgnoreCase("")) {
                    if (val.length() == 0) {
                        setDroppable("false");
                    } else {
                        setDroppable("true");
                    }
                } else {
                    refreshBIE(val);
                }
                return;
            }

            /*
            if (val.length() == 0) {
            setDroppable("false");
            } else {
            setDroppable("true");
            }
             *
             */


            System.out.println("Looking up scientific name for '" + val + "' at " + snUrl);

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
