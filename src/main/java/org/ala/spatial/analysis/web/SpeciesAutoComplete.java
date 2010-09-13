package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.PortalSessionUtilities;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchResult;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.ala.spatial.search.TaxaScientificSearchSummary;
import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;

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
    private String satServer = "http://spatial-dev.ala.org.au"; // http://localhost:8080
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
        //refreshJSON("");
        //System.out.println("setting cnurl in sac()");
        //cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
        //System.out.println("setting satserver in sac()");
        //satServer = settingsSupplementary.getValue(SAT_URL);


    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
        //System.out.println("setting cnurl in sac(val)");
        //cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
        //System.out.println("setting satserver in sac(val)");
        //satServer = settingsSupplementary.getValue(SAT_URL);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        //refresh(value); //refresh the child comboitems
        //refreshJSON(value);
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
            //refreshJSON(evt.getValue());
        }
    }
/*
    private void refreshBIE(String val) {
        //String cnUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/returnType/commonName/view/ajaxTaxonName?query=";
        try {
            //TODO get this from the config file
            if (settingsSupplementary != null) {
                //System.out.println("setting ss.bie.val");
                cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
            } else {
                //System.out.println("NOT setting ss.bie.val");
            }

            String nsurl = cnUrl.replaceAll("_query_", URLEncoder.encode(val, "UTF-8"));
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nsurl);

            //System.out.println(nsurl);

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //System.out.println("Response status code: " + result);
            //System.out.println("Response: \n" + slist);

            String[] aslist = slist.split("\n");
            //System.out.println("Got " + aslist.length + " records.");

            Iterator it = getItems().iterator();
            if (aslist.length > 0) {

                for (int i = 0; i < aslist.length; i++) {
                    String taxon = aslist[i];
                    //taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1).toLowerCase();

                    Comboitem myci = null;
                    if (it != null && it.hasNext()) {
                        myci = ((Comboitem) it.next());
                        myci.setLabel(taxon);
                    } else {
                        it = null;
                        myci = new Comboitem(taxon);
                        myci.setParent(this);
                    }
                    myci.setDescription("scientific name: " + getScientificName(taxon));
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
*/
    /** Refresh comboitem based on the specified value.
     */
    public void refresh(String val) {

        // Start by constraining the search to a min 3-chars
        if (val.length() < 3) {
            //getItems().clear();
            return;
        }

        //TODO get this from the config file
        if (settingsSupplementary != null) {
            //System.out.println("setting ss.val");
            //satServer = settingsSupplementary.getValue(SAT_URL);
        } else if(this.getParent() != null){
            settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
            System.out.println("SAC got SS: " + settingsSupplementary);
            satServer = settingsSupplementary.getValue(SAT_URL);
            cnUrl = settingsSupplementary.getValue(COMMON_NAME_URL);
            //System.out.println("NOT setting ss.val");
        }else{
            return;
        }

        String snUrl = satServer + "/alaspatial/species/taxon/";
        //String snUrl = "http://localhost:8080/alaspatial/species/taxon/";


        try {

            System.out.println("Looking for common name: " + isSearchCommon());

            /*
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
             *
             */

            /*
            if (val.length() == 0) {
            setDroppable("false");
            } else {
            setDroppable("true");
            }
             *
             */


            //System.out.println("Looking up scientific name for '" + val + "' at " + snUrl);

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

                //System.out.println("Response status code: " + result);
                //System.out.println("Response: \n" + slist);

                //System.out.println("adding common names to this");
                //slist += refreshCommonNames(val);

                String[] aslist = slist.split("\n");
                //System.out.println("Got " + aslist.length + " records.");

                if (aslist.length > 0) {

                    Arrays.sort(aslist);

                    for (int i = 0; i < aslist.length; i++) {
                        String[] spVal = aslist[i].split("/");

                        String taxon = spVal[0].trim();
                        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1).toLowerCase();


                        //Label hiddenVal = new Label();
                        //hiddenVal.setValue(spVal[1].trim());
                        //hiddenVal.setVisible(false);

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
                        
                        //hiddenVal.setParent(myci);

                        //if (spVal[1].trim().startsWith("Scientific name")) {
                        if (spVal[2].trim().contains(":")) {
                            //myci.setValue(spVal[1].trim().substring(spVal[1].trim().indexOf(":")).trim());
                            myci.setValue(spVal[2].trim().substring(spVal[2].trim().indexOf(":") + 1).trim());
                        } else {
                            myci.setValue(taxon);
                        }

                        // since we are sorting and interleaving all names
                        // the common names ('contains') might be on the top
                        // of the scientific names ('starts with'), so we
                        // want to select the first 'starts with' option
                        //if (taxon.startsWith(val)) {
                        //    this.setSelectedItem(myci);
                        //}
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
/*
    private String refreshCommonNames(String val) {
        StringBuffer cnListString = new StringBuffer();
        cnListString.append("");
        String baseUrl = "http://data.ala.org.au/search/";
        String cnUrl = "commonNames/_val_/json";

        String callUrl = baseUrl + cnUrl;

        val = val.trim();

        // Start by constraining the search to a min 3-chars
        if (val.length() < 3) {
            return "";
        }

        try {

            //System.out.println("Looking for common name: " + val);

            String searchValue = val.replaceAll(" ", " AND ");
            callUrl = callUrl.replaceAll("_val_", URLEncoder.encode(searchValue, "UTF-8"));


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(callUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();



            JSONArray results = new JSONArray();
            results = searchCommon(slist);

            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    cnListString.append(results.getJSONObject(i).get("commonName"));
                    cnListString.append(" / ");
                    cnListString.append("Scientific name: " + results.getJSONObject(i).get("scientificName"));
                    cnListString.append(" / ");
                    cnListString.append(" found " + results.getJSONObject(i).get("occurrenceCoordinateCount"));
                    cnListString.append("\n");
                }
            }

        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshRemote");
            e.printStackTrace(System.out);

            //new Comboitem("No species found. error.").setParent(this);
        }

        return cnListString.toString();
    }
*/

    /*
    private void refreshJSON(String val) {

        String baseUrl = "http://data.ala.org.au/search/";
        String cnUrl = "commonNames/_val_/json";
        String snUrl = "scientificNames/_val_/json";

        String callUrl = baseUrl;

        val = val.trim();

        // Start by constraining the search to a min 3-chars
        if (val.length() < 3) {
            return;
        }

        //TODO get this from the config file
        if (settingsSupplementary != null) {
            //System.out.println("setting ss.val");
            satServer = settingsSupplementary.getValue(SAT_URL);
        } else {
            //System.out.println("NOT setting ss.val");
        }

        //String snUrl = satServer + "/alaspatial/species/taxon/";
        //String snUrl = "http://localhost:8080/alaspatial/species/taxon/";


        try {

            //System.out.println("Looking for common name: " + isSearchCommon());

            if (isSearchCommon()) {
                callUrl += cnUrl;
            } else {
                callUrl += snUrl;
            }

            String searchValue = val.replaceAll(" ", " AND ");
            callUrl = callUrl.replaceAll("_val_", URLEncoder.encode(searchValue, "UTF-8"));


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(callUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();



            JSONArray results = new JSONArray();
            results = searchCommon(slist);

            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    String itemString = "";
                    String descString = "";
                    itemString = (String) results.getJSONObject(i).get("commonName");
                    descString = "Scientific name: " + (String) results.getJSONObject(i).get("scientificName");
                    descString += " - " + ((Integer) results.getJSONObject(i).get("occurrenceCoordinateCount")).toString() + " records";
                }
            }

        } catch (Exception e) {
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshRemote");
            e.printStackTrace(System.out);

            //new Comboitem("No species found. error.").setParent(this);
        }
    }*/

    public JSONArray searchScientific(String json) {

        try {

            JSONArray joResult = new JSONArray();

            TaxaScientificSearchSummary tss = new TaxaScientificSearchSummary();
            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(TaxaScientificSearchSummary.class);
            jsonConfig.setJavaPropertyFilter(
                    new PropertyFilter() {

                        @Override
                        public boolean apply(Object source, String name,
                                Object value) {
                            if ("result".equals(name)) {
                                return true;
                            }
                            return false;
                        }
                    });
            JSONObject jo = JSONObject.fromObject(json);

            tss = (TaxaScientificSearchSummary) JSONSerializer.toJava(jo,
                    jsonConfig);

            if (tss.getRecordsReturned() > 1) {
                joResult = jo.getJSONArray("result");
            }

            return joResult;
        } catch (Exception e) {
            System.out.println("Something didnt work looking for scientific name");
            e.printStackTrace(System.out);
        }

        return null;
    }

    public JSONArray searchCommon(String json) {
        try {

            // first check if this is a valid json
            // we do this by looking at the first character
            // being the '{'
            if (!json.startsWith("{")) {
                System.out.println("invalid common names response");
                return null; 
            }

            JSONArray joResult = new JSONArray();
            TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
            jsonConfig.setJavaPropertyFilter(
                    new PropertyFilter() {

                        @Override
                        public boolean apply(Object source, String name,
                                Object value) {
                            if ("result".equals(name)) {
                                return true;
                            }
                            return false;
                        }
                    });
            JSONObject jo = JSONObject.fromObject(json);

            tss = (TaxaCommonSearchSummary) JSONSerializer.toJava(jo, jsonConfig);

            if (tss.getRecordsReturned() > 1) {

                joResult = jo.getJSONArray("result");

            }

            return joResult;
        } catch (Exception e) {
            System.out.println("Something didnt work looking for common name");
            e.printStackTrace(System.out);
        }

        return null;
    }
/*
    private String getScientificName(String cname) {
        String taxon = "";


        try {

            String nuri = "http://data.ala.org.au/search/commonNames/" + URLEncoder.encode(cname, "UTF-8") + "/json";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nuri);
            get.addRequestHeader("Content-type", "application/json");



            int result = client.executeMethod(get);
            String snlist = get.getResponseBodyAsString();

            TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
            jsonConfig.setJavaPropertyFilter(
                    new PropertyFilter() {

                        @Override
                        public boolean apply(Object source, String name,
                                Object value) {
                            if ("result".equals(name)) {
                                return true;
                            }
                            return false;
                        }
                    });
            JSONObject jo = JSONObject.fromObject(snlist);

            tss = (TaxaCommonSearchSummary) JSONSerializer.toJava(jo, jsonConfig);




            if (tss.getRecordsReturned() > 1) {

                JSONArray joResult = jo.getJSONArray("result");

                JsonConfig jsonConfigResult = new JsonConfig();
                jsonConfigResult.setRootClass(TaxaCommonSearchResult.class);

                for (int i = 0; i < joResult.size(); i++) {
                    TaxaCommonSearchResult tr = (TaxaCommonSearchResult) JSONSerializer.toJava(joResult.getJSONObject(i), jsonConfigResult);
                    tss.addResult(tr);
                }
            }
            //taxon = tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")";
            taxon = tss.getResultList().get(0).getScientificName();
            //status.setValue("Got: " + tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")");
        } catch (Exception e) {
            System.out.println("Oopps, error getting scientific name from common name");
            e.printStackTrace(System.out);


        }

        return taxon;

    }*/

    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }
}
