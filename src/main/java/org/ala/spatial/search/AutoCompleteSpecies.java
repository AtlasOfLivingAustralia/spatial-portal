/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.search;

/**
 *
 * @author brendon --pinched code from Angus's class
 *
 */
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.zkoss.zul.Combobox;
import org.zkoss.zk.ui.event.InputEvent;
import java.util.Iterator;
import org.zkoss.zul.Comboitem;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class AutoCompleteSpecies extends Combobox {

    private static final String Common = "common";
    private static final String Scientific = "scientific";
    // TODO these need to be sourced from the config file
    private static final String URL = "data.ala.org.au";
    private static final String commonSearch = "/search/commonNames/";
    private static final String scientificSearch = "/search/scientificNames/";
    private String searchType = Common;

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public AutoCompleteSpecies() {
        refresh(""); //init the child comboitems
    }

    public AutoCompleteSpecies(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }

    public void setValue(String value) {
        super.setValue(value);
        refresh(value); //refresh the child comboitems
    }

    /** Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        refresh(evt.getValue());
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {

        //first check the length

        if (val.length() > 2) {
            //update the dictionary
            //TODO: remove hardcoded host, credentials
            HttpHost targetHost = new HttpHost(URL, 80, "http");

            DefaultHttpClient httpclient = new DefaultHttpClient();
            // Add AuthCache to the execution context
            BasicHttpContext localcontext = new BasicHttpContext();
            //localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
            String searchString = forURL(val);

            if (searchType.equals(Common)) {
                searchString = commonSearch + searchString + "/json";
            } else {
                searchString = scientificSearch + searchString + "/json";
            }


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

                if (searchType.equals(Common)) {
                    results = searchCommon(responseText);
                } else {
                    results = searchScientific(responseText);
                }


                //_dict = new String[results.size()];
                Iterator it = getItems().iterator();

                for (int i = 0; i < results.size(); i++) {
                    String itemString;
                    if (searchType.equals(Common)) {
                        itemString = (String) results.getJSONObject(i).get("commonName");
                    } else {
                        itemString = (String) results.getJSONObject(i).get("scientificName");
                    }


                    if (it != null && it.hasNext()) {
                        ((Comboitem) it.next()).setLabel(itemString);
                    } else {
                        it = null;
                        new Comboitem(itemString).setParent(this);
                    }

                }
                while (it != null && it.hasNext()) {
                    it.next();
                    it.remove();
                }

            } catch (Exception e) {
            }

        }
    }

    public JSONArray searchScientific(String json) {

        JSONArray joResult = new JSONArray();

        TaxaScientificSearchSummary tss = new TaxaScientificSearchSummary();
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.setRootClass(TaxaScientificSearchSummary.class);
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

        tss = (TaxaScientificSearchSummary) JSONSerializer.toJava(jo,
                jsonConfig);

        if (tss.getRecordsReturned() > 1) {
            joResult = jo.getJSONArray("result");

        }

        return joResult;

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
