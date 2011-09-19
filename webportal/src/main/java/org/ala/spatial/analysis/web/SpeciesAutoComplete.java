package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
        } else if (this.getParent() != null) {
            settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
        } else {
            return;
        }

        //Do something about geocounts.
        //High limit because geoOnly=true cuts out valid matches, e.g. "Macropus"
        String snUrl = CommonData.bieServer + "/search/auto.json?limit=100&q=";

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
                String rawJSON = get.getResponseBodyAsString();

                //parse
                JSONObject jo = JSONObject.fromObject(rawJSON);

                StringBuilder slist = new StringBuilder();
                JSONArray ja = jo.getJSONArray("autoCompleteList");
                int found = 0;
                for(int i=0;i<ja.size() && found < 20;i++){
                    JSONObject o = ja.getJSONObject(i);

                    //count for guid
                    try {
                        String q = "lft:%5B" + o.getLong("left") + "%20TO%20" + o.getLong("right") + "%5D%20AND%20geospatial_kosher:true";
                        long count = getCount(q);
                        System.out.println("count=" + count + " for " +o.getString("name") + ":" + o.getString("guid") );

                        if(count > 0) {
                            if(slist.length() > 0) {
                                slist.append("\n");
                            }

                            //macaca / urn:lsid:catalogueoflife.org:taxon:d84852d0-29c1-102b-9a4a-00304854f820:ac2010 / genus / found 17
                            slist.append(o.getString("name")).append(" / ");
                            slist.append(o.getString("guid")).append(" / ");
                            slist.append(o.getString("rankString")).append(" / found ");
                            slist.append(count);

                            found++;
                        }
                    } catch (Exception e) {

                    }
                }

                slist.append(loadUserPoints(val));

//                slist += loadOccurrencesInActiveArea(val);

                String sslist = slist.toString();
                System.out.println("SpeciesAutoComplete: \n" + sslist);

                String[] aslist = sslist.split("\n");

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

                        String[] wmsNames = CommonData.getSpeciesDistributionWMS(spVal[1].trim());
                        if (wmsNames != null && wmsNames.length > 0) {
                            if (wmsNames.length == 1) {
                                myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records + map");
                            } else {
                                myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records + " + wmsNames.length + " maps");
                            }
                        } else {
                            myci.setDescription(spVal[2].trim() + " - " + spVal[3].trim() + " records");
                        }

                        myci.setDisabled(false);
                        myci.addAnnotation(spVal[1].trim(), "LSID", null);

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

                        if ("user".contains(val)
                                || ud.getName().toLowerCase().contains(val)
                                || ud.getDescription().toLowerCase().contains(val)) {
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

    //keep query geocounts as [0] count and [1] last requested
    HashMap<String, long []> queryGeoCounts = new HashMap<String, long[]>();
    long max_age = 60*60*1000; //max age is 1hr
    private long getCount(String key) {
        long [] data = queryGeoCounts.get(key);
        if(data == null || System.currentTimeMillis() - data[1] > max_age) {
            data = new long[2];
            data[0] = queryCount(key);
            //ok if >= 0
            if(data[0] >= 0) {
                data[1] = System.currentTimeMillis();
                queryGeoCounts.put(key, data);
            }
            
        }
        if(data != null) {
            return data[0];
        } else {
            return 0;
        }
    }

    long queryCount(String key) {
        int count = -1;

        HttpClient client = new HttpClient();
        String url = CommonData.biocacheServer
                + "/webportal/occurrences?"
                + "pageSize=0"
                + "&q=" + key;
        System.out.println("getting count for autocomplete > " + url);
        GetMethod get = new GetMethod(url.replace("[", "%5B").replace("]", "%5D"));

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            String start = "\"totalRecords\":";
            String end = ",";
            int startPos = response.indexOf(start) + start.length();

            count = Integer.parseInt(response.substring(startPos, response.indexOf(end, startPos)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

//    private String loadOccurrencesInActiveArea(String val) {
//        String layerPrefix = "Occurrences in Active area ";
//        String userPoints = "";
//
//        for(MapLayer ml : getThisMapComposer().getActiveAreaLayers()) {
//            if(ml.getName().contains(layerPrefix) && ml.getDisplayName().toLowerCase().contains(val.toLowerCase())) {
//                try {
//                    userPoints = ml.getDisplayName()
//                            + " / "
//                            + ml.getMapLayerMetadata().getSpeciesLsid()
//                            + " / Active Area / "
//                            + ml.getMapLayerMetadata().getOccurrencesCount()
//                            + "\n";
//                } catch (Exception e) {
//                    System.out.println("Unable to load Active Area points into Species Auto Complete");
//                    e.printStackTrace(System.out);
//                }
//            }
//        }
//
//        return userPoints;
//    }
}
