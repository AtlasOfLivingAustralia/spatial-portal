package au.org.ala.spatial.composer.species;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author ajay
 */
public class SpeciesAutoComplete extends Combobox {
    private static final Logger LOGGER = Logger.getLogger(SpeciesAutoComplete.class);
    private boolean biocacheOnly = false;
    private boolean bSearchCommon = false;
    private BiocacheQuery biocacheQuery;
    /**
     * The biocache field to used in the autocomplete.  Defaults to the raw_taxon_name if none is provided.
     */
    private String facetField = "raw_taxon_name";

    public SpeciesAutoComplete() {
        super();
        refresh("");
    }

    public SpeciesAutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }

    /**
     * Creates an autocomplete based on a query - autocomplete will be populated from a facet generated from the query
     *
     * @param query
     */
    public SpeciesAutoComplete(BiocacheQuery query) {
        this.biocacheQuery = query;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    public boolean isSearchCommon() {
        return bSearchCommon;
    }

    public void setSearchCommon(boolean searchCommon) {
        this.bSearchCommon = searchCommon;
    }

    /**
     * Listens what an user is entering.
     */
    public void onChanging(InputEvent evt) {
        if (!evt.isChangingBySelectBack()) {
            refresh(evt.getValue());
        }
    }

    /**
     * Refresh comboitem based on the specified value.
     */
    public void refresh(String val) {

        // Start by constraining the search to a min 3-chars
        if (val.length() < 3) {
            return;
        }

        //Do something about geocounts.
        //High limit because geoOnly=true cuts out valid matches, e.g. "Macropus"
        try {

            LOGGER.debug("Looking for common name: " + isSearchCommon());

            getItems().clear();
            Iterator it = getItems().iterator();
            if (val.length() == 0) {
                Comboitem myci;
                if (it.hasNext()) {
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
                StringBuilder sb = new StringBuilder();

                if (biocacheQuery != null) {
                    sb.append(biocacheQuery.getAutoComplete("raw_taxon_name", val, 50));
                } else {
                    //sb.append(autoService(val));
                    sb.append(searchService(val));
                }
                if (!biocacheOnly) {
                    sb.append(loadUserPoints(val));
                }

                String sslist = sb.toString();
                LOGGER.debug("SpeciesAutoComplete: \n" + sslist);

                String[] aslist = sslist.split("\n");

                if (aslist.length > 0 && aslist[0].length() > 0) {

                    Arrays.sort(aslist);

                    for (int i = 0; i < aslist.length; i++) {
                        String[] spVal = aslist[i].split("/");

                        String taxon = spVal[0].trim();

                        Comboitem myci;
                        if (it != null && it.hasNext()) {
                            myci = ((Comboitem) it.next());
                            myci.setLabel(taxon);
                        } else {
                            it = null;
                            myci = new Comboitem(taxon);
                            myci.setParent(this);
                        }

                        String desc;
                        if (spVal.length >= 4) {
                            desc = spVal[2] + " - " + spVal[3];
                        } else {
                            desc = spVal[2];
                        }
                        String[] wmsDistributions = CommonData.getSpeciesDistributionWMS(spVal[1]);
                        if (wmsDistributions.length > 0) {
                            if (wmsDistributions.length == 1) {
                                desc += " +1 expert distribution";
                            } else {
                                desc += " +" + wmsDistributions.length + " expert distributions";
                            }
                        }
                        String[] wmsChecklists = CommonData.getSpeciesChecklistWMS(spVal[1]);
                        if (wmsChecklists.length > 0) {
                            if (wmsChecklists.length == 1) {
                                desc += " +1 checklist area";
                            } else {
                                desc += " +" + wmsChecklists.length + " checklist areas";
                            }
                        }
                        myci.setDescription(desc);


                        myci.setDisabled(false);

                        myci.setValue(taxon);

                        myci.setDisabled(false);
                        if (myci.getAnnotations() != null) {
                            myci.getAnnotations().clear();
                        }
                        if (org.apache.commons.lang.StringUtils.isNotBlank(spVal[1])) {
                            myci.addAnnotation(spVal[1], "LSID", null);
                        } else {
                            //add the scientific name as the annotation
                            myci.addAnnotation(spVal[0], StringConstants.VALUE, null);
                        }

                    }
                }
            }
            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {
            LOGGER.error("Oopss! something went wrong in SpeciesAutoComplete.refresh", e);
        }
    }

    private String loadUserPoints(String val) {
        Map<String, UserDataDTO> htUserSpecies = (Map<String, UserDataDTO>) Sessions.getCurrent().getAttribute(StringConstants.USERPOINTS);
        String userPoints = "";
        String lVal = val.toLowerCase();

        try {
            if (htUserSpecies != null && !htUserSpecies.isEmpty()) {

                StringBuilder sbup = new StringBuilder();
                for (Map.Entry<String, UserDataDTO> e : htUserSpecies.entrySet()) {
                    String k = e.getKey();
                    UserDataDTO ud = e.getValue();

                    if ("user".contains(lVal)
                            || ud.getName().toLowerCase().contains(lVal)
                            || ud.getDescription().toLowerCase().contains(lVal)) {
                        sbup.append(ud.getName());
                        sbup.append(" /");
                        sbup.append(k);
                        sbup.append("/");
                        sbup.append("user");
                        sbup.append("/");
                        sbup.append(ud.getFeatureCount());
                        sbup.append("\n");
                    }
                }
                userPoints = sbup.toString();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load user points into Species Auto Complete", e);
        }

        return userPoints;
    }

    String searchService(String val) throws Exception {
        String nsurl = CommonData.getBieServer() + "/search.json?pageSize=100&q=" + URLEncoder.encode(val, StringConstants.UTF_8) + "&fq=idxtype:TAXON";

        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(nsurl);
        get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

        client.executeMethod(get);
        String rawJSON = get.getResponseBodyAsString();

        //parse
        JSONParser jp = new JSONParser();
        JSONObject jo = (JSONObject) jp.parse(rawJSON);
        jo = (JSONObject) jo.get("searchResults");

        StringBuilder slist = new StringBuilder();
        JSONArray ja = (JSONArray) jo.get("results");
        for (int i = 0; i < ja.size(); i++) {
            JSONObject o = (JSONObject) ja.get(i);

            //count for guid
            try {
                if (o.containsKey(StringConstants.NAME) && o.containsKey("guid") && o.containsKey(StringConstants.RANK)) {
                    Integer count = CommonData.getLsidCounts().getCount(o.get("guid").toString());

                    if (slist.length() > 0) {
                        slist.append("\n");
                    }

                    String commonName = null;
                    boolean commonNameMatch = false;
                    if (o.containsKey("commonName") && !StringConstants.NONE.equals(o.get("commonName"))
                            && !StringConstants.NULL.equals(o.get("commonName"))) {
                        commonName = o.get("commonName").toString();
                        commonName = commonName.trim().replace("/", ",");
                        String[] cns = commonName.split(",");
                        for (int j = 0; j < cns.length; j++) {
                            if (cns[j].toLowerCase().contains(val)) {
                                commonName = cns[j];
                                commonNameMatch = true;
                                break;
                            }
                        }
                        if (commonName.indexOf(',') > 1) {
                            commonName = commonName.substring(0, commonName.indexOf(','));
                        }
                    }

                    //macaca / urn:lsid:catalogueoflife.org:taxon:d84852d0-29c1-102b-9a4a-00304854f820:ac2010 / genus / found 17
                    //swap name and common name if it is a common name match
                    if (commonNameMatch) {
                        slist.append(commonName).append(" /");
                        slist.append(o.get("guid")).append("/");
                        slist.append(o.get(StringConstants.RANK));
                        slist.append(", ").append(o.get(StringConstants.NAME).toString().replace("/", ","));
                        slist.append("/");
                        if (count != null) {
                            slist.append("found ");
                            slist.append(count);
                        } else {
                            slist.append("counting");
                        }
                    } else {
                        slist.append(o.get(StringConstants.NAME).toString().replace("/", ",")).append(" /");
                        slist.append(o.get("guid")).append("/");
                        slist.append(o.get(StringConstants.RANK));
                        if (commonName != null) {
                            slist.append(", ").append(commonName);
                        }
                        slist.append("/");
                        if (count != null) {
                            slist.append("found ");
                            slist.append(count);
                        } else {
                            slist.append("counting");
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("failed to add species autocomplete item: " + o, e);
            }
        }

        return slist.toString();
    }

    String autoService(String val) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

        StringBuilder slist = new StringBuilder();

        long start = System.currentTimeMillis();
        //while there is inappropriate sorting use limit=50
        String nsurl = "";
        try {
            nsurl = CommonData.getBieServer() + "/search/auto.json?limit=50&q=" + URLEncoder.encode(val, StringConstants.UTF_8);

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nsurl);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

            int result = client.executeMethod(get);

            if (result != 200) {
                LOGGER.debug("SPECIES AUTOCOMPLETE ERROR|" + sdf.format(new Date()) + "|" + (System.currentTimeMillis() - start) + "ms|" + nsurl + "|response code " + result);
            } else {
                String rawJSON = get.getResponseBodyAsString();

                LOGGER.debug("SPECIES AUTOCOMPLETE SUCCESSFUL|" + sdf.format(new Date()) + "|" + (System.currentTimeMillis() - start) + "ms|" + nsurl);

                //parse
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(rawJSON);

                JSONArray ja = (JSONArray) jo.get("autoCompleteList");

                Set<String> lsids = new HashSet<String>();
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject o = (JSONObject) ja.get(i);

                    //count for guid
                    try {
                        if (o.containsKey("left") && o.containsKey("right")) {
                            if (o.containsKey(StringConstants.NAME)
                                    && o.containsKey("guid")
                                    && o.containsKey("rankString")
                                    && !lsids.contains(o.get("guid"))) {

                                Integer count = CommonData.getLsidCounts().getCount(o.get("guid").toString());

                                lsids.add(o.get("guid").toString());
                                if (slist.length() > 0) {
                                    slist.append("\n");
                                }

                                String matchedName = o.get(StringConstants.NAME).toString().replace("/", ",");
                                if (o.containsKey("matchedNames") && !StringConstants.NULL.equals(o.get("matchedNames"))
                                        && !"[]".equals(o.get("matchedNames"))) {
                                    matchedName = ((JSONArray) o.get("matchedNames")).get(0).toString();
                                }
                                String commonName = null;
                                if (o.containsKey("commonName") && !StringConstants.NONE.equals(o.get("commonName"))
                                        && !StringConstants.NULL.equals(o.get("commonName"))) {
                                    commonName = o.get("commonName").toString().replaceAll("\n", "");
                                    commonName = commonName.trim().replace("/", ",");
                                    String[] cns = commonName.split(",");
                                    for (int j = 0; j < cns.length; j++) {
                                        if (cns[j].toLowerCase().contains(val)) {
                                            commonName = cns[j];
                                            break;
                                        }
                                    }
                                    if (commonName.indexOf(',') > 1) {
                                        commonName = commonName.substring(0, commonName.indexOf(','));
                                    }
                                }

                                //macaca / urn:lsid:catalogueoflife.org:taxon:d84852d0-29c1-102b-9a4a-00304854f820:ac2010 / genus / found 17
                                //swap name and common name if it is a common name match
                                if (o.containsKey("commonNameMatches") && !StringConstants.NULL.equals(o.get("commonNameMatches"))
                                        && !"[]".equals(o.get("commonNameMatches"))) {
                                    slist.append(matchedName).append(" /");
                                    slist.append(o.get("guid")).append("/");
                                    slist.append(o.get("rankString"));
                                    slist.append(", ").append(o.get(StringConstants.NAME).toString().replace("/", ","));
                                    slist.append("/");
                                    if (count != null) {
                                        slist.append("found ");
                                        slist.append(count);
                                    } else {
                                        slist.append("counting");
                                    }
                                } else {
                                    slist.append(matchedName).append(" /");
                                    slist.append(o.get("guid")).append("/");
                                    slist.append(o.get("rankString"));
                                    if (commonName != null) {
                                        slist.append(", ").append(commonName);
                                    }
                                    slist.append("/");
                                    if (count != null) {
                                        slist.append("found ");
                                        slist.append(count);
                                    } else {
                                        slist.append("counting");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("SPECIES AUTOCOMPLETE ERROR|" + sdf.format(new Date()) + "|" + (System.currentTimeMillis() - start) + "ms|" + nsurl, e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("SPECIES AUTOCOMPLETE ERROR|" + sdf.format(new Date()) + "|" + (System.currentTimeMillis() - start) + "ms|" + nsurl, e);
        }

        return slist.toString();
    }

    public void setBiocacheOnly(boolean biocacheOnly) {
        this.biocacheOnly = biocacheOnly;
    }

    /**
     * @return the biocacheQuery
     */
    public BiocacheQuery getBiocacheQuery() {
        return biocacheQuery;
    }

    /**
     * @param biocacheQuery the biocacheQuery to set
     */
    public void setBiocacheQuery(BiocacheQuery biocacheQuery) {
        this.biocacheQuery = biocacheQuery;
    }

    /**
     * @return the facetField
     */
    public String getFacetField() {
        return facetField;
    }

    /**
     * @param facetField the facetField to set
     */
    public void setFacetField(String facetField) {
        this.facetField = facetField;
    }

}
