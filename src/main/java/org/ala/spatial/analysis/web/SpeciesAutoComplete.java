package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.UserData;
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

                slist += loadUserPoints(val);

	                System.out.println("SpeciesAutoComplete: \n" + slist);

                String[] aslist = slist.split("\n");

                if (aslist.length > 0 && aslist[0].length() > 0) {

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

                        String [] wmsNames = CommonData.getSpeciesDistributionWMS(spVal[1].trim());
                        if(wmsNames != null && wmsNames.length > 0) {
                            if(wmsNames.length == 1) {
                                myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records + map");
                            } else {
                                myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records + " + wmsNames.length + " maps");
                            }
                        } else {
                            myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records");
                        }
                        
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

    private String loadUserPoints(String val) {
	        String userPoints = "";
	        Hashtable<String, UserData> htUserSpecies = (Hashtable) getThisMapComposer().getSession().getAttribute("userpoints");
                val = val.toLowerCase();

	        try {
	            if (htUserSpecies != null) {
	                if (htUserSpecies.size() > 0) {
	                    Enumeration e = htUserSpecies.keys();
	                    StringBuilder sbup = new StringBuilder();
	                    while (e.hasMoreElements()) {
	                        String k = (String) e.nextElement();
	                        UserData ud = htUserSpecies.get(k);

	                        if ("user".contains(val) ||
	                                ud.getName().toLowerCase().contains(val) ||
	                                ud.getDescription().toLowerCase().contains(val)) {
	                            sbup.append(ud.getName());
	                            sbup.append(" / ");
                            sbup.append(k);
	                            sbup.append(" / ");
	                            sbup.append("user");
	                            sbup.append(" / ");
	                            sbup.append(ud.getFeatureCount());
	                            sbup.append("\n");
	                        }
	                    }
                    userPoints = sbup.toString();
	                }
            }
	        } catch (Exception e) {
	            System.out.println("Unable to load user points into Species Auto Complete");
	            e.printStackTrace(System.out);
	        }

	        return userPoints;
	    }

}
