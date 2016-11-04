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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
                        String[] spVal = aslist[i].split("\\|");

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
                        sbup.append(" |");
                        sbup.append(k);
                        sbup.append("|");
                        sbup.append("user");
                        sbup.append("|");
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
        //search URL for new BIE
        String v = val;

        StringBuilder slist = new StringBuilder();

        String size = CommonData.getSettings().getProperty("autocomplete.limit", "20");

        String type = CommonData.getSettings().getProperty("autocomplete.type", "search");
        if (type.equals("search")) {
            String nsurl = CommonData.getBieServer() + "/ws/search.json?pageSize=" + size + "&q=" + URLEncoder.encode(v, StringConstants.UTF_8) + "&fq=idxtype:TAXON";

            //for repeating with wildcard for patial matches
            String nsurl2 = CommonData.getBieServer() + "/ws/search.json?pageSize=" + size + "&q=" + URLEncoder.encode(v + "*", StringConstants.UTF_8) + "&fq=idxtype:TAXON";

            slist.append(buildList(getResults(nsurl), val));

            //no need to search with wildcard when there is > 1 term in the search
            if (!val.contains(" ")) slist.append(buildList(getResults(nsurl2), val));
        } else if (type.equals("auto")) {
            String nsurl = CommonData.getBieServer() + "/ws/search/auto.json?pageSize=" + size + "&q=" + URLEncoder.encode(v, StringConstants.UTF_8);

            slist.append(buildList(getResults(nsurl), val));
        } else if (type.equals("biocache")) {
            String nsurl = CommonData.getBiocacheServer() + "/autocomplete/search?pageSize=" + size + "&q=" + URLEncoder.encode(v, StringConstants.UTF_8);

            slist.append(buildList(getResults(nsurl), val));
        }

        return slist.toString();
    }

    private JSONArray getResults(String nsurl) throws Exception {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(nsurl);
        get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);

        client.executeMethod(get);
        String rawJSON = get.getResponseBodyAsString();

        //parse
        JSONParser jp = new JSONParser();
        JSONObject jo = (JSONObject) jp.parse(rawJSON);

        //support search and auto bie webservices
        if (jo.containsKey("searchResults")) {
            return (JSONArray) ((JSONObject) jo.get("searchResults")).get("results");
        } else {
            return (JSONArray) jo.get("autoCompleteList");
        }
    }

    private String buildList(JSONArray ja, String val) {
        StringBuilder slist = new StringBuilder();

        for (int i = 0; i < ja.size(); i++) {
            JSONObject o = (JSONObject) ja.get(i);

            //count for guid
            try {
                String rank = StringConstants.RANK;
                if (o.containsKey("rankString")) rank = "rankString";

                if (o.containsKey(StringConstants.NAME) && o.containsKey("guid") && o.containsKey(rank)) {
                    Integer count = 0;
                    if (o.containsKey("occCount") && !o.get("occCount").equals("0")) {
                        try {
                            count = Integer.parseInt(o.get("occCount").toString());
                        } catch (Exception e) {
                            count = CommonData.getLsidCounts().getCount(o.get("guid").toString());
                        }
                    } else {
                        count = CommonData.getLsidCounts().getCount(o.get("guid").toString());
                    }

                    if (slist.length() > 0) {
                        slist.append("\n");
                    }

                    String commonName = null;
                    boolean commonNameMatch = false;

                    if (o.containsKey("commonName") && !StringConstants.NONE.equals(o.get("commonName")) &&
                            o.get("commonName") != null && !StringConstants.NULL.equals(o.get("commonName"))) {
                        commonName = o.get("commonName").toString();
                        String[] cns = commonName.split(",");
                        if (o.get("commonName") instanceof JSONArray) {
                            cns = (String[]) ((JSONArray) o.get("commonName")).toArray(cns);
                            if (cns.length > 0 && cns[0] != null) commonName = cns[0];
                        }

                        if (o.containsKey("commonNameMatches")) {
                            cns = (String[]) ((JSONArray) o.get("commonNameMatches")).toArray(cns);
                        }

                        for (int j = 0; j < cns.length; j++) {
                            if (cns[j] != null && cns[j].toLowerCase().contains(val)) {
                                commonName = cns[j].replace("<b>", "").replace("</b>", "");
                                commonNameMatch = true;
                                break;
                            }
                        }
                        if (commonName.indexOf(',') > 1) {
                            commonName = commonName.substring(0, commonName.indexOf(','));
                        }
                    }

                    String name = o.get(StringConstants.NAME).toString();
                    //misapplied identification
                    if (o.containsKey("taxonomicStatus") && o.containsKey("acceptedConceptName") && "misapplied".equalsIgnoreCase(o.get("taxonomicStatus").toString())) {
                        name += " (misapplied to " + o.get("acceptedConceptName").toString() + ")";
                    }

                    //macaca / urn:lsid:catalogueoflife.org:taxon:d84852d0-29c1-102b-9a4a-00304854f820:ac2010 / genus / found 17
                    //swap name and common name if it is a common name match
                    if (commonNameMatch) {
                        slist.append(commonName).append(" |");
                        slist.append(o.get("guid")).append("|");
                        slist.append(o.get(rank));
                        slist.append(", ").append(name.replace("|", ","));
                        slist.append("|");
                        if (count != null) {
                            slist.append("found ");
                            slist.append(count);
                        } else {
                            slist.append("counting");
                        }
                    } else {
                        slist.append(name.replace("|", ",")).append(" |");
                        slist.append(o.get("guid")).append("|");
                        slist.append(o.get(rank));
                        if (commonName != null) {
                            slist.append(", ").append(commonName);
                        }
                        slist.append("|");
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
