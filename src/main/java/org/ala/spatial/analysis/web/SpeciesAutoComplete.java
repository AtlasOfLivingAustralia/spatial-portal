package org.ala.spatial.analysis.web;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author ajay
 */
public class SpeciesAutoComplete extends Combobox {

    //TODO get these from the config file
    private static final String cnUrl = "data.ala.org.au";
    private static final String commonSearch = "/search/commonNames/";
    private static final String scientificSearch = "/search/scientificNames/";
    private boolean bSearchCommon = true;

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
        refresh(value); //refresh the child comboitems
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refreshBIE(evt.getValue());
        }
    }

     private void refreshBIE(String val) {

    String snUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/view/ajaxTaxonName?query=";
    String cnUrl = "http://data.ala.org.au/taxonomy/taxonName/ajax/returnType/commonName/view/ajaxTaxonName?query=";

        try {

            String nsurl;

            if (isSearchCommon()) {
                nsurl = cnUrl + URLEncoder.encode(val, "UTF-8");
            } else {
               nsurl = snUrl + URLEncoder.encode(val, "UTF-8");
            }



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
            System.out.println("Oopss! something went wrong in SpeciesAutoComplete.refreshRemote");
            e.printStackTrace(System.out);

            new Comboitem("No species found. error.").setParent(this);
        }

     }
    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {

        //TODO get this from the config file
        String snUrl = "http://ec2-184-73-34-104.compute-1.amazonaws.com/alaspatial/species/taxon/";


        try {

            String TipMessage;


            if (isSearchCommon()) {
                TipMessage = "Please start by typing in a species name...";
            } else {
                TipMessage = "Please start by typing in a common name...";
            }

            Iterator it = getItems().iterator();
            if (val.length() == 0) {
                Comboitem myci = null;
                if (it != null && it.hasNext()) {
                    myci = ((Comboitem) it.next());
                    myci.setLabel(TipMessage);
                } else {
                    it = null;
                    myci = new Comboitem(TipMessage);
                    myci.setParent(this);
                }
                myci.setDescription("");
                myci.setDisabled(true);
            } else {


                if (isSearchCommon()) {


                    HttpHost targetHost = new HttpHost(cnUrl, 80, "http");

                    DefaultHttpClient httpclient = new DefaultHttpClient();
                    // Add AuthCache to the execution context
                    BasicHttpContext localcontext = new BasicHttpContext();
                    //localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
                    String searchString = forURL(val);

                    searchString = commonSearch + searchString + "*/json";
                    HttpGet httpget = new HttpGet(searchString);


                    try {
                        HttpResponse response = httpclient.execute(targetHost, httpget, localcontext);
                        HttpEntity entity = response.getEntity();
                        String responseText = "";
                        if (entity != null) {
                            responseText = new String(EntityUtils.toByteArray(entity));
                        } else {
                            responseText = "Fail";
                        }


                        JSONArray results = new JSONArray();
                        results = searchCommon(responseText);

                        //_dict = new String[results.size()];
                        Iterator its = getItems().iterator();

                        for (int i = 0; i < results.size(); i++) {
                            String itemString;
                            itemString = (String) results.getJSONObject(i).get("commonName");
                            if (its != null && its.hasNext()) {
                                ((Comboitem) its.next()).setLabel(itemString);
                            } else {
                                its = null;
                                new Comboitem(itemString).setParent(this);
                            }

                        }
                        while (its != null && its.hasNext()) {
                            its.next();
                            its.remove();
                        }

                    } catch (Exception e) {
                    }


                } else {
                    String nsurl = snUrl + URLEncoder.encode(val + "*", "UTF-8");

                    HttpClient client = new HttpClient();
                    GetMethod get = new GetMethod(nsurl);
                    get.addRequestHeader("Content-type", "text/plain");

                    int result = client.executeMethod(get);
                    String slist = get.getResponseBodyAsString();
                    String[] aslist = slist.split("\n");

                    if (aslist.length > 0) {

                        for (int i = 0; i < aslist.length; i++) {
                            String[] spVal = aslist[i].split("/");

                            Comboitem myci = null;
                            if (it != null && it.hasNext()) {
                                myci = ((Comboitem) it.next());
                                myci.setLabel(spVal[0].trim());
                                myci.setTooltip("Species name");
                            } else {
                                it = null;
                                myci = new Comboitem(spVal[0].trim());
                                myci.setParent(this);
                            }
                            myci.setDescription(spVal[1].trim() + " - " + spVal[2].trim());
                            myci.setDisabled(false);
                        }
                    }
                }
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

    public JSONArray searchCommon(String json) {
        JSONArray joResult = new JSONArray();
        TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
        jsonConfig.setJavaPropertyFilter(new PropertyFilter() {

            @Override
            public boolean apply(Object source, String name, Object value) {
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

    }

    /**
     * string checking code, for spaces and special characters
     * @param aURLFragment
     * @return String
     */
    public static String forURL(String aURLFragment) {
        String result = null;
        try {
            result = URLEncoder.encode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }
}
