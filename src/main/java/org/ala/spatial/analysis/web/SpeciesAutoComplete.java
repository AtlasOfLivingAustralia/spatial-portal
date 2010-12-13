package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class SpeciesAutoComplete extends Combobox {

    private static final String COMMON_NAME_URL = "common_name_url";
    private static final String SAT_URL = "sat_url";
    private String cnUrl = null;
    private String satServer = null; 
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
    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
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
    public void refresh(String val) {

        // Start by constraining the search to a min 3-chars
        if (val.length() < 3) {
            return;
        }

        if (settingsSupplementary != null) {
        } else if(this.getParent() != null){
            settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
            System.out.println("SAC got SS: " + settingsSupplementary);
            satServer = settingsSupplementary.getValue(SAT_URL);
            cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
        }else{
            return;
        }

        String snUrl = satServer + "/alaspatial/species/taxon/";

        try {

            System.out.println("Looking for common name: " + isSearchCommon());

            getItems().clear();
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

                String[] aslist = slist.split("\n");

                if (aslist.length > 1) {

                    Arrays.sort(aslist);

                    for (int i = 0; i < aslist.length; i++) {
                        String[] spVal = aslist[i].split("/");

                        String taxon = spVal[0].trim();
                        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1).toLowerCase();

                        Comboitem myci = null;
                        if (it != null && it.hasNext()) {
                            myci = ((Comboitem) it.next());
                            myci.setLabel(taxon);
                        } else {
                            it = null;
                            myci = new Comboitem(taxon);
                            myci.setParent(this);
                        }
                        myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records");
                        myci.setDisabled(false);
                        myci.addAnnotation(spVal[1].trim(),"LSID", null);

                        if (spVal[2].trim().contains(":")) {
                            myci.setValue(spVal[2].trim().substring(spVal[2].trim().indexOf(":") + 1).trim());
                        } else {
                            myci.setValue(taxon);
                        }
                    }
                }

            }
            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refresh");
            e.printStackTrace(System.out);
        }

    }

    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}
