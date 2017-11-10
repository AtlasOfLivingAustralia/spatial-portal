/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.legend.Legend;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * TODO NC 2013-08-15 - Remove all the references to the "include null" gesopatially kosher. I have removed from the UI but I didn't want to
 * break the build before the next release
 *
 * @author Adam
 */
public class BiocacheQuery implements Query, Serializable {
    static final String SAMPLING_SERVICE_CSV_GZIP = "/occurrences/index/download?facet=false&reasonTypeId=10&qa=none";
    static final String SAMPLING_SERVICE = "/webportal/occurrences?";
    static final String SPECIES_LIST_SERVICE_CSV = "/occurrences/facets/download?facets=names_and_lsid&lookup=true&count=true&";
    static final String SPECIES_COUNT_SERVICE = "/occurrence/facets?facets=names_and_lsid";
    static final String DOWNLOAD_URL = "/occurrences/download?";
    static final String DATA_PROVIDERS_SERVICE = "/webportal/dataProviders?";
    static final String QUERY_TITLE_URL = "/occurrences/search?";
    static final String LEGEND_SERVICE_CSV = "/webportal/legend?";
    static final String BOUNDING_BOX_CSV = "/webportal/bbox?";
    static final String INDEXED_FIELDS_LIST = "/indexed/fields?";
    static final String POST_SERVICE = "/webportal/params?";
    static final String QID_DETAILS = "/webportal/params/details/";
    static final String ENDEMIC_COUNT_SERVICE = "/explore/counts/endemic?";
    static final String ENDEMIC_SPECIES_SERVICE_CSV = "/explore/endemic/species.csv?";
    static final String ENDEMIC_LIST = "/explore/endemic/species/";
    static final String DEFAULT_ROWS = "&pageSize=1000000";
    static final String DEFAULT_ROWS_LARGEST = "pageSize=1000000";
    static final Pattern QUERY_PARAMS_PATTERN = Pattern.compile("&([a-zA-Z0-9_\\-]+)=");
    /**
     * DEFAULT_VALIDATION must not be null
     */
    static final String DEFAULT_VALIDATION = "";
    static final String BIE_SPECIES = "/species/";
    static final String BIE_SPECIES_WS = "/ws/species/";
    static final String WMS_URL = "/webportal/wms/reflect?";
    private static final Logger LOGGER = Logger.getLogger(BiocacheQuery.class);
    private static final String[] COMMON_TAXON_RANKS = new String[]{
            "cultivar",
            "superfamily",
            "subgenus",
            "unranked",
            "infrageneric",
            "subfamily",
            StringConstants.SUB_SPECIES,
            "section",
            "infraspecific",
            "hybrid",
            "variety",
            "form",
            "series",
            "tribe"
    };
    //query inputs
    private String name;
    private String rank;
    private String lsids;
    private String[] rawNames;
    private List<Facet> facets;
    private String wkt;
    private String extraParams;
    private String paramId;
    private String qc;
    private String biocacheWebServer;
    private String biocacheServer;
    private boolean supportsDynamicFacets;
    private boolean forMapping;
    //stored query responses.
    private String speciesList = null;
    private String endemicSpeciesList = null;
    private int speciesCount = -1;
    private int endemicSpeciesCount = -1;
    private int occurrenceCount = -1;
    private String solrName = null;
    private Map<String, LegendObject> legends = new HashMap<String, LegendObject>();
    private Set<String> flaggedRecords = new HashSet<String>();
    private int speciesCountKosher = -1, speciesCountCoordinates = -1, speciesCountAny = -1;
    private int occurrenceCountKosher = -1, occurrenceCountCoordinates = -1, occurrenceCountAny = -1;
    private List<QueryField> facetFieldList = null;
    private List<Double> bbox = null;
    private Pattern lsidPattern = Pattern.compile("(?:lsid:)\"?([a-z0-9\\:\\.\\-]*)\"?");

    public BiocacheQuery(String lsids, String wkt, String extraParams, List<Facet> facets, boolean forMapping, boolean[] geospatialKosher) {
        this(lsids, null, wkt, extraParams, facets, forMapping, geospatialKosher);
    }

    public BiocacheQuery(String lsids, String[] rawNames, String wkt, String extraParams, List<Facet> facets, boolean forMapping, boolean[] geospatialKosher) {
        this(lsids, rawNames, wkt, extraParams, facets, forMapping, geospatialKosher, null, null, false);
    }

    public BiocacheQuery(String lsids, String wkt, String extraParams, List<Facet> facets, boolean forMapping, boolean[] geospatialKosher, String biocacheServer, String biocacheWebServer, boolean supportsDynamicFacets) {
        this(lsids, null, wkt, extraParams, facets, forMapping, geospatialKosher, biocacheServer, biocacheWebServer, supportsDynamicFacets);
    }

    public BiocacheQuery(String lsids, String[] rawNames, String wkt, String extraParams, List<Facet> facets, boolean forMapping, boolean[] geospatialKosher, String biocacheServer, String biocacheWebServer, boolean supportsDynamicFacets) {

        if (biocacheServer != null && biocacheWebServer != null) {
            this.biocacheWebServer = biocacheWebServer;
            this.biocacheServer = biocacheServer;
        } else {
            this.biocacheWebServer = CommonData.getBiocacheWebServer();
            this.biocacheServer = CommonData.getBiocacheServer();
        }

        //identify and extract qids from fqs or single term extraParams
        //q and fqs get added to extraParams
        //wkt is unioned
        if (facets != null || extraParams != null) {
            String newExtraParams = null;
            for (int i = facets == null ? -1 : facets.size() - 1; i >= -1; i--) {
                //check extraParams for qid terms
                String term;
                if (i >= 0) term = facets.get(i).toString();
                else term = extraParams;
                if (term != null) {
                    for (String termPart : term.split("&")) {
                        int qidPos = termPart.indexOf("qid:");
                        //match "qid:...", "q=qid:...", "fq=qid:..."
                        if (qidPos == 0 || qidPos == 2 || qidPos == 3) {
                            if (i == -1) extraParams = null;

                            JSONObject jo = getQidDetails(termPart.replace("fq=", "").replace("q=", ""));

                            try {
                                StringBuilder sb = new StringBuilder();
                                if (jo.containsKey("q")) {
                                    if (jo.get("q").toString().length() > 0 && !jo.get("q").toString().equals("*:*")) {
                                        //unescape q
                                        sb.append("&fq=").append(jo.get("q").toString());
                                    }
                                }
                                if (jo.containsKey("fqs")) {
                                    JSONArray ja = (JSONArray) jo.get("fqs");
                                    for (int j = 0; j < ja.size(); j++) {
                                        if (ja.get(j).toString().length() > 0 && !ja.get(j).toString().equals("*:*")) {
                                            //no need to unescape fq
                                            sb.append("&fq=").append(ja.get(j));
                                        }
                                    }
                                }
                                if (newExtraParams == null) {
                                    newExtraParams = sb.toString().replace("\\", "");
                                } else {
                                    newExtraParams += sb.toString().replace("\\", "");
                                }
                                if (jo.containsKey("wkt") && jo.get("wkt").toString().length() > 0) {
                                    String qidWkt = jo.get("wkt").toString();

                                    if (wkt == null) {
                                        wkt = qidWkt;
                                    } else {
                                        try {
                                            WKTReader wktReader = new WKTReader();
                                            Geometry g1 = wktReader.read(wkt);
                                            Geometry g2 = wktReader.read(qidWkt);
                                            wkt = g1.union(g2).toText().replace(", ", ",").replace(" (", "(");
                                        } catch (Exception e) {
                                            LOGGER.error("failed to union wkt: " + wkt + " and " + qidWkt);
                                        }
                                    }
                                }

                                //set name
                                if (jo.containsKey("displayString")) {
                                    name = jo.get("displayString").toString();
                                    solrName = jo.get("displayString").toString();
                                }

                                if (i >= 0) facets.remove(i);

                            } catch (Exception e) {
                                LOGGER.error("failed to merge " + facets.get(i).toString(), e);
                            }
                        } else {
                            //append non-qid part if updating 'extraParams'
                            if (i < 0) {
                                if (newExtraParams == null) {
                                    newExtraParams = termPart;
                                } else {
                                    newExtraParams += "&" + termPart;
                                }
                            }
                        }
                    }
                }
            }
            if (newExtraParams != null) {
                extraParams = newExtraParams;
            }
        }
        
        this.lsids = lsids;
        this.rawNames = rawNames == null ? null : rawNames.clone();
        if (facets != null) {
            this.facets = new ArrayList<Facet>(facets.size());
            this.facets.addAll(facets);
        }
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : Util.fixWkt(wkt);
        this.extraParams = extraParams;
        this.forMapping = forMapping;
        this.qc = CommonData.getBiocacheQc();
        this.supportsDynamicFacets = supportsDynamicFacets;

        if (geospatialKosher != null) {
            addGeospatialKosher(geospatialKosher);
        }

        makeParamId();
    }

    /**
     * qid q values are escaped for SOLR. Unescape them to prevent double escaping.
     *
     * @param s
     * @return
     */
    private String unescape(String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); ++i) {
            char c1 = s.charAt(i);
            if (c1 == '\\') {
                char c = s.charAt(i + 1);
                if (c == 92 || c == 43 || c == 45 || c == 33 || c == 40 || c == 41 || c == 58 || c == 94 || c == 91 || c == 93 || c == 34 || c == 123 || c == 125 || c == 126 || c == 42 || c == 63 || c == 124 || c == 38 || c == 59 || c == 47 || Character.isWhitespace(c)) {
                    sb.append(c);
                    i++;
                } else {
                    sb.append(c1);
                }
            } else {
                sb.append(c1);
            }
        }

        return sb.toString();
    }

    static final String translateFieldForSolr(String facetName) {
        String newFacetName = facetName;
        if (newFacetName == null) {
            return null;
        }
        for (String[] s : CommonData.getFacetNameExceptions()) {
            if (newFacetName.equals(s[0])) {
                newFacetName = s[1];
                break;
            }
        }
        if ("occurrence_year_individual".equals(newFacetName)) {
            newFacetName = StringConstants.OCCURRENCE_YEAR;
        }
        if (StringConstants.OCCURRENCE_YEAR_DECADE.equals(newFacetName)) {
            newFacetName = StringConstants.OCCURRENCE_YEAR;
        }
        return newFacetName;
    }

    static Facet makeFacetGeospatialKosher(boolean includeTrue, boolean includeFalse, boolean includeNull) {
        if (includeTrue && includeFalse && includeNull) {
            return null;
        } else if (!includeTrue && !includeFalse && !includeNull) {
            return new Facet("*", "*", false);
        } else if (includeTrue && !includeFalse && !includeNull) {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, StringConstants.TRUE, true);
        } else if (!includeTrue && includeFalse && !includeNull) {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, StringConstants.FALSE, true);
        } else if (!includeTrue && !includeFalse && includeNull) {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, "*", false);
        } else if (includeTrue && includeFalse && !includeNull) {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, "*", true);
        } else if (includeTrue && !includeFalse && includeNull) {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, StringConstants.FALSE, false);
        } else {
            return new Facet(StringConstants.GEOSPATIAL_KOSHER, StringConstants.TRUE, false);
        }
    }

    public static boolean[] parseGeospatialKosher(String facet) {
        boolean[] geospatialKosher = null;
        if (facet != null) {
            String f = facet.replace("\"", "").replace("(", "").replace(")", "");
            if ("geospatial_kosher:true".equals(f)) {
                geospatialKosher = new boolean[]{true, false, false};
            } else if ("geospatial_kosher:false".equals(f)) {
                geospatialKosher = new boolean[]{false, true, false};
            } else if ("-geospatial_kosher:*".equals(f)) {
                geospatialKosher = new boolean[]{false, false, true};
            } else if ("geospatial_kosher:*".equals(f)) {
                geospatialKosher = new boolean[]{true, true, false};
            } else if ("-geospatial_kosher:false".equals(f)) {
                geospatialKosher = new boolean[]{true, false, true};
            } else if ("-geospatial_kosher:true".equals(f)) {
                geospatialKosher = new boolean[]{false, true, true};
            }
        }
        return geospatialKosher;
    }

    /**
     * This method will correctly split on params handling the case where emebedded &'s can exist
     *
     * @param query
     * @return
     */
    private static String[] splitOnParams(String query) {
        String[] totals = query.split(QUERY_PARAMS_PATTERN.toString());

        int i = 1;
        if (totals.length > 1) {
            Matcher m = QUERY_PARAMS_PATTERN.matcher(query);
            while (m.find()) {
                totals[i] = m.group(1) + "=" + totals[i];
                i++;
            }

        }
        if (totals.length > 1 && StringUtils.isEmpty(totals[0])) {
            totals = Arrays.copyOfRange(totals, 1, totals.length);
        }
        return totals;
    }

    public static String getScientificNameRank(String lsid) {

        String snUrl = "true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie")) ?
                CommonData.getBieServer() + BIE_SPECIES_WS + lsid + ".json" :
                CommonData.getBieServer() + BIE_SPECIES + lsid + ".json";

        LOGGER.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(slist);
            String scientficName = ((JSONObject) jo.get("taxonConcept")).get("nameString").toString();
            String r = ((JSONObject) jo.get("taxonConcept")).get("rankString").toString();

            LOGGER.debug("Arrays.binarySearch(COMMON_TAXON_RANKS, rank): " + Arrays.binarySearch(COMMON_TAXON_RANKS, r));
            if (Arrays.binarySearch(COMMON_TAXON_RANKS, r) > -1) {
                r = StringConstants.TAXON;
            }

            return scientficName + "," + r;
        } catch (Exception e) {
            LOGGER.error("error getting scientific name:" + snUrl, e);
        }

        return StringConstants.OCCURRENCES;
    }

    /**
     * Performs a scientific name or common name lookup and returns the guid if it exists in the BIE
     * <p/>
     * TODO Move getGuid and getClassification to BIE Utilities...
     *
     * @param name
     * @return
     */
    public static String getGuid(String name) {
        if ("true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie"))) {
            String url = CommonData.getBieServer() + "/ws/species/lookup/bulk";
            try {
                HttpClient client = new HttpClient();
                PostMethod get = new PostMethod(url);
                get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);
                get.setRequestEntity(new StringRequestEntity("{\"names\":[\"" + name.replace("\"","\\\"") + "\"]}"));

                client.executeMethod(get);
                String body = get.getResponseBodyAsString();

                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(body);
                if (ja != null && !ja.isEmpty()) {
                    JSONObject jo = (JSONObject) ja.get(0);
                    if (jo != null && jo.containsKey("acceptedIdentifier") && jo.get("acceptedIdentifier") != null) {
                        return jo.get("acceptedIdentifier").toString();
                    } else if (jo != null && jo.containsKey("acceptedIdentifierGuid") && jo.get("acceptedIdentifierGuid") != null) {
                        return jo.get("acceptedIdentifierGuid").toString();
                    } else if (jo != null && jo.containsKey("acceptedConceptID") && jo.get("acceptedConceptID") != null) {
                        return jo.get("acceptedConceptID").toString();
                    } else if (jo != null && jo.containsKey("guid") && jo.get("guid") != null) {
                        return jo.get("guid").toString();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("error getting guid at: " + url, e);
                return null;
            }
        } else {
            String url = CommonData.getBieServer() + "/ws/guid/" + name.replaceAll(" ", "%20");
            try {
                HttpClient client = new HttpClient();
                GetMethod get = new GetMethod(url);
                get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);

                client.executeMethod(get);
                String body = get.getResponseBodyAsString();

                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(body);
                if (ja != null && !ja.isEmpty()) {
                    JSONObject jo = (JSONObject) ja.get(0);
                    if (jo != null && jo.containsKey("acceptedIdentifier") && jo.get("acceptedIdentifier") != null) {
                        return jo.get("acceptedIdentifier").toString();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("error getting guid at: " + url, e);
                return null;
            }
        }
    }

    /**
     * Retrieves the classification information from the BIE for the supplied GUID.
     *
     * @param lsid
     * @return
     */
    public static Map<String, String> getClassification(String lsid) {

        String[] classificationList = {StringConstants.KINGDOM, StringConstants.PHYLUM, StringConstants.CLASS, StringConstants.ORDER, StringConstants.FAMILY, StringConstants.GENUS, StringConstants.SPECIES, StringConstants.SUB_SPECIES, StringConstants.SCIENTIFIC_NAME};
        Map<String, String> classification = new LinkedHashMap<String, String>();

        String snUrl = "true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie")) ?
                CommonData.getBieServer() + BIE_SPECIES_WS + lsid + ".json" :
                CommonData.getBieServer() + BIE_SPECIES + lsid + ".json";
        LOGGER.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(slist);

            JSONObject joOcc = (JSONObject) jo.get("classification");
            for (String c : classificationList) {
                //NC stop exception where a rank can't be found
                String s = "true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie")) ? c : c.replace("ss", "zz");
                if (joOcc.containsKey(s)) {
                    String value = joOcc.get(s).toString();
                    if (value != null) {
                        classification.put(s, value);
                    }
                }
            }
            if ("true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie"))) {
                joOcc = (JSONObject) jo.get("taxonConcept");
                if (joOcc != null) {
                    if (ArrayUtils.contains(classificationList, joOcc.get("rankString"))) {
                        classification.put(joOcc.get("rankString").toString(), joOcc.get("nameString").toString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting scientific name for: " + lsid);

        }

        return classification;
    }

    //call makeParamId() after this function
    void addGeospatialKosher(boolean[] geospatialKosher) {
        if (getGeospatialKosher() == null) {
            //Do not add/replace facet for geospatial_kosher when it is present
            //outside a facet.
            return;
        }

        Facet f = BiocacheQuery.makeFacetGeospatialKosher(geospatialKosher[0], geospatialKosher[1], geospatialKosher[2]);

        if (facets == null) {
            facets = new ArrayList<Facet>();
        } else {
            for (int i = 0; i < facets.size(); i++) {
                if (facets.get(i).toString().contains("geospatial_kosher:")) {
                    facets.remove(i);
                    break;
                }
            }
        }

        if (f != null) {
            facets.add(f);
        }
    }

    public boolean[] getGeospatialKosher() {
        boolean[] geospatialKosher = new boolean[]{true, true, true};

        if ((lsids + extraParams).contains("geospatial_kosher:")) {
            //must be in a Facet to be compatible, so include everything
            return geospatialKosher;
        }

        if (facets != null) {
            for (int i = 0; i < facets.size(); i++) {
                String f = facets.get(i).toString().replace("\"", "").replace("(", "").replace(")", "");
                if (f.contains("geospatial_kosher:")) {
                    if ("geospatial_kosher:true".equals(f)) {
                        geospatialKosher = new boolean[]{true, false, false};
                    } else if ("geospatial_kosher:false".equals(f)) {
                        geospatialKosher = new boolean[]{false, true, false};
                    } else if ("-geospatial_kosher:*".equals(f)) {
                        geospatialKosher = new boolean[]{false, false, true};
                    } else if ("geospatial_kosher:*".equals(f)) {
                        geospatialKosher = new boolean[]{true, true, false};
                    } else if ("-geospatial_kosher:false".equals(f)) {
                        geospatialKosher = new boolean[]{true, false, true};
                    } else if ("-geospatial_kosher:true".equals(f)) {
                        geospatialKosher = new boolean[]{false, true, true};
                    }
                    break;
                }
            }
        }
        return geospatialKosher;
    }

    public BiocacheQuery newFacetGeospatialKosher(boolean[] geospatialKosher, boolean forMapping) {
        boolean[] gk = getGeospatialKosher();

        //cannot create the new facet
        if (gk == null) {
            //This should never happen.
            LOGGER.error("Attempted to add a geospatial_kosher facet to an unsupported query: '" + lsids + "', '" + extraParams + "'");
            return null;
        }

        int sum = 0;
        for (int i = 0; i < gk.length; i++) {
            if (gk[i] == geospatialKosher[i]) {
                sum++;
            }
        }
        if (sum == gk.length) {
            return newFacet(null, forMapping);
        }
        List<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            for (int i = 0; i < facets.size(); i++) {
                if (!facets.get(i).toString().contains("geospatial_kosher:")) {
                    newFacets.add(facets.get(i));
                }
            }
        }

        return new BiocacheQuery(lsids, rawNames, wkt, extraParams, newFacets, forMapping, geospatialKosher, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }

    /**
     * Further restrict records by field values.
     */
    @Override
    public BiocacheQuery newFacet(Facet facet, boolean forMapping) {
        if (facet == null && (this.forMapping || !forMapping)) {
            return this;
        }

        List<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        if (facet != null) {
            newFacets.add(facet);
        }

        return new BiocacheQuery(lsids, rawNames, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }

    /**
     * Restrict to an area.
     * <p/>
     * If an area already exists the additional area is applied.
     *
     * @param wkt
     * @return new BiocacheQuery with the additional wkt area applied.
     */
    @Override
    public BiocacheQuery newWkt(String wkt, boolean forMapping) {
        if (wkt == null || wkt.equals(CommonData.WORLD_WKT) || wkt.equals(this.wkt)) {
            if (this.forMapping || !forMapping) {
                return this;
            } else {
                return new BiocacheQuery(lsids, rawNames, wkt, extraParams, facets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
            }
        }

        BiocacheQuery sq = null;
        try {
            String newWkt = wkt;
            if (this.wkt != null) {
                Geometry newGeom = new WKTReader().read(wkt);
                Geometry thisGeom = new WKTReader().read(this.wkt);
                Geometry intersectionGeom = thisGeom.intersection(newGeom);
                newWkt = (new WKTWriter()).write(intersectionGeom).replace(" (", "(").replace(", ", ",").replace(") ", ")");
            }

            sq = new BiocacheQuery(lsids, rawNames, newWkt, extraParams, facets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
        } catch (Exception e) {
            LOGGER.error("error getting new WKT from an intersection", e);
        }

        return sq;
    }

    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in CSV format.
     */
    @Override
    public String sample(List<QueryField> fields) {
        HttpClient client = new HttpClient();
        String url = biocacheServer
                + SAMPLING_SERVICE_CSV_GZIP
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + paramQueryFields(fields).replace("&fl=", "&fields=")
                + getQc();
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        String sample = null;

        long start = System.currentTimeMillis();
        try {
            client.executeMethod(get);
            sample = decompressZip(get.getResponseBodyAsStream());

            //in the first line do field name replacement
            for (QueryField f : fields) {
                String t = translateFieldForSolr(f.getName());
                if (!f.getName().equals(t)) {
                    sample = sample.replaceFirst(t, f.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("error sampling", e);
        }
        LOGGER.debug("get sample in " + (System.currentTimeMillis() - start) + "ms");

        return sample;
    }

    /**
     * Returns the "autocomplete" values for the query based on the supplied facet field. Extensible so that we can add autocomplete
     * based on queries in other areas.
     * <p/>
     * NC 20131126 - added to support an autocomplete of raw taxon name for a fix associated with Fungi
     *
     * @param facet The facet to autocomplete on
     * @param value The prefix for the autocomplete
     * @param limit The maximum number of values to return
     * @return
     */
    public String getAutoComplete(String facet, String value, int limit) {
        HttpClient client = new HttpClient();
        StringBuilder slist = new StringBuilder();
        if (value.length() >= 3 && StringUtils.isNotBlank(facet)) {

            try {
                String url = biocacheServer
                        + QUERY_TITLE_URL + "q=" + getQ() + getQc() + "&facets=" + facet
                        + "&fprefix=" + URLEncoder.encode(value, StringConstants.UTF_8) + "&pageSize=0&flimit=" + limit;
                GetMethod get = new GetMethod(url);
                get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.TEXT_PLAIN);
                int result = client.executeMethod(get);
                if (result == 200) {
                    //success
                    String rawJSON = get.getResponseBodyAsString();
                    //parse
                    JSONParser jp = new JSONParser();
                    JSONObject jo = (JSONObject) jp.parse(rawJSON);

                    JSONArray ja = (JSONArray) jo.get("facetResults");
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject o = (JSONObject) ja.get(i);
                        if (o.get("fieldName").equals(facet)) {
                            //process the values in the list
                            JSONArray values = (JSONArray) o.get("fieldResult");
                            for (int j = 0; j < values.size(); j++) {
                                JSONObject vo = (JSONObject) values.get(j);
                                if (slist.length() > 0) {
                                    slist.append("\n");
                                }
                                slist.append(vo.get("label")).append("//found ")
                                        .append(vo.get(StringConstants.COUNT).toString());
                            }
                        }
                    }
                } else {
                    LOGGER.warn("There was an issue performing the autocomplete from the biocache: " + result);
                }

            } catch (Exception e) {
                LOGGER.error("failed to get autocomplete facet=" + facet + ", value=" + value, e);
            }
        }
        return slist.toString();
    }

    /**
     * Get species list for this query.
     *
     * @return species list as String containing CSV.
     */
    @Override
    public String speciesList() {
        if (speciesList != null) {
            return speciesList;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + SPECIES_LIST_SERVICE_CSV
                + "&q=" + getQ()
                + getQc();
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            client.executeMethod(get);
            speciesList = get.getResponseBodyAsString();

            //add 'Other' correction and add additional columns
            List<String> header = CommonData.getSpeciesListAdditionalColumnsHeader();
            StringBuilder newlist = new StringBuilder();
            int total = getOccurrenceCount();
            CSVReader csv = new CSVReader(new StringReader(speciesList));
            String[] line;
            int count = 0;
            int lastpos = 0;
            while ((line = csv.readNext()) != null) {

                int nextpos = speciesList.indexOf('\n', lastpos + 1);
                if (nextpos < 0) nextpos = speciesList.length();
                newlist.append(speciesList.substring(lastpos, nextpos));

                List<String> list = header;
                if (lastpos != 0) {
                    list = CommonData.getSpeciesListAdditionalColumns(header, line[0]);
                }
                for (int i = 0; i < list.size(); i++) {
                    newlist.append(",\"").append(list.get(i).replace("\"", "\"\"").replace("\\", "\\\\")).append("\"");
                }
                lastpos = nextpos;

                try {
                    count += Integer.parseInt(line[line.length - 1]);
                } catch (Exception e) {
                }
            }
            if (total - count > 0) {
                String correction = "\n,,,,,,,,,,Other (not species rank)," + (total - count);
                newlist.append(correction);
            }
            speciesList = newlist.toString();
        } catch (Exception e) {
            LOGGER.error("error getting species list from: " + url);
        }

        return speciesList;
    }

    public String endemicSpeciesList() {
        if (endemicSpeciesList != null) {
            return endemicSpeciesList;
        }

        if (CommonData.getSettings().containsKey("endemic.sp.method")
                && CommonData.getSettings().getProperty("endemic.sp.method").equals("true")) {
            String speciesList = speciesList();

            //can get species list counts as "kosher:true" or "kosher:*" only
            Map speciesCounts;
            if (getGeospatialKosher()[1]) {     //[1] is 'include kosher:false'
                speciesCounts = CommonData.getSpeciesListCounts(false);
            } else {
                speciesCounts = CommonData.getSpeciesListCountsKosher(false);
            }

            StringBuilder sb = new StringBuilder();
            int speciesCol = 0;
            int countCol = 11;
            try {
                CSVReader csv = new CSVReader(new StringReader(speciesList));
                String[] row;
                int currentPos = 0;
                int nextPos = speciesList.indexOf('\n', currentPos + 1);
                sb.append(speciesList.substring(currentPos, nextPos));  //header
                csv.readNext(); //header
                while ((row = csv.readNext()) != null) {
                    //add if species is not present elsewhere
                    Long c = (Long) speciesCounts.get(row[speciesCol]);
                    if (c != null && c <= Long.parseLong(row[countCol])) {
                        if (nextPos > speciesList.length()) {
                            nextPos = speciesList.length();
                        }
                        sb.append(speciesList.substring(currentPos, nextPos));
                    } else if (c == null) {
                        LOGGER.error("failed to find species_guid: " + row[speciesCol] + " in CommonData.getSpeciesListCounts()");
                    }

                    currentPos = nextPos;
                    nextPos = speciesList.indexOf('\n', currentPos + 1);
                }
            } catch (Exception e) {
                LOGGER.error("failed generating endemic species list", e);
            }

            endemicSpeciesList = sb.toString();
        } else {

            forMapping = true;
            if (paramId == null) makeParamId();

            HttpClient client = new HttpClient();
            String url = biocacheServer
                    + ENDEMIC_LIST
                    + paramId
                    + "?facets=names_and_lsid";

            LOGGER.debug(url);
            GetMethod get = new GetMethod(url);

            try {
                client.executeMethod(get);

                JSONParser jp = new JSONParser();

                JSONArray ja = (JSONArray) jp.parse(get.getResponseBodyAsString());

                //extract endemic matches from the species list
                String speciesList = speciesList();
                StringBuilder sb = new StringBuilder();

                int idx = speciesList.indexOf('\n');
                if (idx > 0) {
                    sb.append(speciesList.substring(0, idx));
                }
                for (int j = 0; j < ja.size(); j++) {
                    JSONObject jo = (JSONObject) ja.get(j);
                    if (jo.containsKey("label")) {
                        idx = speciesList.indexOf("\n" + jo.get("label") + ",");
                        if (idx > 0) {
                            int lineEnd = speciesList.indexOf('\n', idx + 1);
                            if (lineEnd < 0) lineEnd = speciesList.length();
                            sb.append(speciesList.substring(idx, lineEnd));
                        }
                    }
                }

                endemicSpeciesList = sb.toString();
            } catch (Exception e) {
                LOGGER.error("error getting endemic species result", e);
            }
        }

        return endemicSpeciesList;
    }

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    @Override
    public int getOccurrenceCount() {
        if (occurrenceCount >= 0) {
            return occurrenceCount;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + SAMPLING_SERVICE
                + "pageSize=0&facet=false"
                + "&q=" + getQ()
                + getQc();
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            String start = "\"totalRecords\":";
            String end = ",";
            int startPos = response.indexOf(start) + start.length();

            occurrenceCount = Integer.parseInt(response.substring(startPos, response.indexOf(end, startPos)));
        } catch (Exception e) {
            LOGGER.error("error getting records count: " + url, e);
        }

        return occurrenceCount;
    }

    /**
     * Get number of species in this query.
     *
     * @return number of species as int or -1 on error.
     */
    @Override
    public int getSpeciesCount() {
        if (speciesCount >= 0) {
            return speciesCount;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + SPECIES_COUNT_SERVICE
                /* TODO: fix biocache to use fqs here */
                + "&q=" + getQ().replace("&fq=", "%20AND%20")
                + getQc();
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONArray ja = (JSONArray) jp.parse(response);
            if (!ja.isEmpty()) {
                JSONObject jo = (JSONObject) ja.get(0);
                if (jo.containsKey(StringConstants.COUNT)) {
                    speciesCount = Integer.parseInt(jo.get(StringConstants.COUNT).toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting records count: " + url, e);
        }

        return speciesCount;
    }

    @Override
    public int getEndemicSpeciesCount() {
        if (endemicSpeciesCount >= 0) {
            return endemicSpeciesCount;
        }

        //fill endemic species list
        endemicSpeciesList();

        //first line is header, last line is not \n terminated
        endemicSpeciesCount = 0;
        int p = 0;
        while ((p = endemicSpeciesList.indexOf('\n', p + 1)) > 0) {
            endemicSpeciesCount++;
        }

        return endemicSpeciesCount;
    }

    public int getSpeciesCountKosher() {
        if (speciesCountKosher < 0) {
            speciesCountKosher = newFacetGeospatialKosher(new boolean[]{true, false, false}, false).getSpeciesCount();
        }
        return speciesCountKosher;
    }

    public int getSpeciesCountCoordinates() {
        if (speciesCountCoordinates < 0) {
            speciesCountCoordinates = newFacetGeospatialKosher(new boolean[]{true, true, false}, false).getSpeciesCount();
        }
        return speciesCountCoordinates;
    }

    public int getSpeciesCountAny() {
        if (speciesCountAny < 0) {
            speciesCountAny = newFacetGeospatialKosher(new boolean[]{true, true, true}, false).getSpeciesCount();
        }
        return speciesCountAny;
    }

    public int getOccurrenceCountKosher() {
        if (occurrenceCountKosher < 0) {
            occurrenceCountKosher = newFacetGeospatialKosher(new boolean[]{true, false, false}, false).getOccurrenceCount();
        }
        return occurrenceCountKosher;
    }

    public int getOccurrenceCountCoordinates() {
        if (occurrenceCountCoordinates < 0) {
            occurrenceCountCoordinates = newFacetGeospatialKosher(new boolean[]{true, true, false}, false).getOccurrenceCount();
        }
        return occurrenceCountCoordinates;
    }

    public int getOccurrenceCountAny() {
        if (occurrenceCountAny < 0) {
            occurrenceCountAny = newFacetGeospatialKosher(new boolean[]{true, true, true}, false).getOccurrenceCount();
        }
        return occurrenceCountAny;
    }

    String paramQueryFields(List<QueryField> fields) {
        StringBuilder sb = new StringBuilder();

        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(fields.get(i).getName());
            }
        }

        return (sb.length() > 0) ? "&fl=" + sb.toString() : "";
    }

    @Override
    public final String getQ() {
        if (paramId != null) {
            return "qid:" + paramId;
        }

        return getFullQ(true);
    }

    private JSONObject getQidDetails(String qidTerm) {
        HttpClient client = new HttpClient();
        String url = biocacheServer
                + QID_DETAILS + qidTerm.replace("qid:", "");
        GetMethod get = new GetMethod(url);
        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(response);
                return jo;
            } else {
                LOGGER.debug("error with url:" + url + " getting qid details for " + qidTerm + " > response_code:" + result + " response:" + response);
            }
        } catch (Exception e) {
            LOGGER.error("error getting biocache param details from " + url, e);
        }
        return null;
    }

    @Override
    public final String getFullQ(boolean encode) {
        StringBuilder sb = new StringBuilder();

        if (wkt != null && "POLYGON EMPTY".equals(wkt)) {
            wkt = null;
        }

        int queryTerms = 0;
        if (lsids != null) {
            for (String s : lsids.split(",")) {
                if (queryTerms > 0) {
                    sb.append(" OR ");
                } else {
                    sb.append("(");
                }
                sb.append("lsid:").append(s);
                queryTerms++;
            }
        }
        if (rawNames != null && rawNames.length > 0) {
            for (String rawName : rawNames) {
                if (queryTerms > 0) {
                    sb.append(" OR ");
                } else {
                    sb.append("(");
                }
                sb.append("raw_name:\"").append(rawName).append("\"");
                queryTerms++;
            }
        }
        if (queryTerms > 0) {
            sb.append(")");
        } else if ((facets == null || facets.isEmpty())
                && DEFAULT_VALIDATION.length() == 0
                && extraParams == null) {
            sb.append("*:*");
            queryTerms++;
        }

        if (encode) {
            try {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(URLEncoder.encode(sb.toString(), StringConstants.UTF_8));
                sb = sb2;
            } catch (Exception e) {
                LOGGER.error("error encoding: " + sb.toString(), e);
            }
        }

        if (facets != null && !facets.isEmpty()) {
            for (int i = 0; i < facets.size(); i++) {
                if (queryTerms > 0) {
                    sb.append("&fq=");
                }
                String facet = facets.get(i).toString();
                int p = facet.indexOf(':');
                if (p > 0) {
                    String f = facet.substring(0, p);
                    String t = translateFieldForSolr(f);
                    if (!f.equals(t)) {
                        facet = t + facet.substring(p);
                    }
                }
                //can't put extra brackets around "not" operators
                if (facet.contains(" OR ") && !facet.startsWith("-")) {
                    facet = "(" + facet + ")";
                }
                if (encode) {
                    try {
                        sb.append(URLEncoder.encode(facet, StringConstants.UTF_8));
                    } catch (Exception e) {
                        LOGGER.error("error encoding: " + facet, e);
                    }
                } else {
                    sb.append(facet);
                }
                queryTerms++;
            }
        }

        if (DEFAULT_VALIDATION.length() > 0) {
            if (queryTerms > 0) {
                sb.append("&fq=");
            }
            queryTerms++;
            if (encode) {
                try {
                    sb.append(URLEncoder.encode(DEFAULT_VALIDATION, StringConstants.UTF_8));
                } catch (Exception e) {
                    LOGGER.error("error encoding: " + DEFAULT_VALIDATION, e);
                }
            } else {
                sb.append(DEFAULT_VALIDATION);
            }
        }

        //extra parameters
        if (extraParams != null) {
            if (queryTerms > 0) {
                sb.append("&fq=");
            }

            if (encode) {
                try {
                    //split
                    String[] split = extraParams.split("&");
                    for (int i = 0; i < split.length; i++) {
                        String key = "";
                        String value = split[i];
                        if (i > 0) {
                            int e = split[i].indexOf("=");
                            if (e > 0) {
                                key = split[i].substring(0, e);
                                value = split[i].substring(e + 1);
                            }
                        }
                        sb.append(key).append(URLEncoder.encode(value, StringConstants.UTF_8));
                    }
                } catch (Exception e) {
                    LOGGER.error("error encoding: " + extraParams, e);
                }
            } else {
                sb.append(extraParams);
            }
        }

        //wkt term
        if (wkt != null) {
            sb.append("&wkt=").append(wkt.replace(" ", ":"));
        }

        try {
            String q = sb.toString();
            if (q.startsWith("&fq=")) {
                q = q.substring(4);
            } else if (q.startsWith("fq%28")) {
                q = q.substring(5);
            }
            return q;
        } catch (Exception e) {
            LOGGER.error("error returning a string", e);
        }

        return "";
    }

    /**
     * Get a query string under a registered id.
     *
     * @return a short query term as String, or null on error.
     */
    final void makeParamId() {
        paramId = null;

        //do not create paramId for short queries
        if (!forMapping && getFullQ(true).length() < CommonData.getMaxQLength()) {
            return;
        }

        //reduce wkt (may also need to validate WKT)
        wkt = Util.reduceWKT(wkt).getReducedWKT();

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + POST_SERVICE
                + getQc()
                + "&facet=false";
        PostMethod post = new PostMethod(url);
        try {
            //NQ: query values could contain embedded &'s
            String[] qs = splitOnParams(getFullQ(false));
            for (int i = 0; i < qs.length; i++) {
                String q = qs[i];
                int p = q.indexOf('=');
                if (p < 0) {
                    post.addParameter("q", q.substring(p + 1));
                    LOGGER.debug("param: " + "q" + " : " + q.substring(p + 1));
                } else {
                    post.addParameter(q.substring(0, p), q.substring(p + 1));
                    LOGGER.debug("param: " + q.substring(0, p) + " : " + q.substring(p + 1));
                }
            }
            post.addParameter(StringConstants.BBOX, forMapping ? StringConstants.TRUE : StringConstants.FALSE);
            int result = client.executeMethod(post);
            String response = post.getResponseBodyAsString();

            if (result == 200) {
                paramId = response;

                LOGGER.debug(url + " > " + paramId);
            } else {
                LOGGER.debug("error with url:" + url + " posting q: " + getQ() + " > response_code:" + result + " response:" + response);
            }
        } catch (Exception e) {
            LOGGER.error("error getting biocache param id from: " + url + " for " + getQ(), e);
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        if (lsids == null) {
            name = "All species";
        } else if (lsids.split(",").length == 1) {
            name = getScientificNameRank(lsids).split(",")[0];
        } else {
            name = "Selected species";
        }

        return name;
    }

    @Override
    public String getRank() {
        if (rank != null) {
            return rank;
        }

        if (lsids != null && lsids.split(",").length == 1) {
            rank = getScientificNameRank(lsids).split(",")[1];

            if (StringConstants.SCIENTIFICNAME.equalsIgnoreCase(rank) || StringConstants.SCIENTIFIC.equalsIgnoreCase(rank)) {
                rank = StringConstants.TAXON;
            }
        } else {
            rank = StringConstants.SCIENTIFICNAME;
        }

        return rank;
    }

    private String decompressGz(InputStream gzipped) throws IOException {
        String s = null;
        try {
            GZIPInputStream gzip = new GZIPInputStream(gzipped);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1048576);
            byte[] buffer = new byte[1048576];
            int size;
            while ((size = gzip.read(buffer)) >= 0) {
                baos.write(buffer, 0, size);
            }
            s = new String(baos.toByteArray());
        } catch (Exception e) {
            LOGGER.error("error decompressing gz stream", e);
        }
        gzipped.close();

        return s;
    }

    private String decompressZip(InputStream zipped) throws IOException {
        String s = null;
        try {
            ZipInputStream gzip = new ZipInputStream(zipped);

            //data.csv
            gzip.getNextEntry();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(1048576);
            byte[] buffer = new byte[1048576];
            int size;
            while ((size = gzip.read(buffer)) >= 0) {
                baos.write(buffer, 0, size);
            }
            s = new String(baos.toByteArray());
        } catch (Exception e) {
            LOGGER.error("error decompressing gz stream", e);
        }
        zipped.close();

        return s;
    }

    /**
     * Retrieves a list of custom facets for the supplied query.
     *
     * @return
     */
    private List<QueryField> retrieveCustomFacets() {
        List<QueryField> customFacets = new ArrayList<QueryField>();

        //custom fields can only be retrieved with specific query types
        String full = getFullQ(false);

        Matcher match = Pattern.compile("data_resource_uid:\"??dr[t][0-9]+\"??").matcher(full);
        if (!match.find()) {
            return customFacets;
        }

        //look up facets
        try {
            final String jsonUri = biocacheServer + "/upload/dynamicFacets?q=" + URLEncoder.encode(match.group(), "UTF-8");
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(jsonUri);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);
            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONArray ja = (JSONArray) jp.parse(slist);

            for (Object arrayElement : ja) {
                JSONObject jsonObject = (JSONObject) arrayElement;
                String facetName = jsonObject.get(StringConstants.NAME).toString();
                String facetDisplayName = jsonObject.get("displayName").toString();

                //TODO: remove this when _RNG fields work in legend &cm= parameter
                if (!(facetDisplayName.contains("(Range)") && facetName.endsWith("_RNG"))) {
                    LOGGER.debug("Adding custom index : " + arrayElement);
                    customFacets.add(new QueryField(facetName, facetDisplayName, QueryField.GroupType.CUSTOM, QueryField.FieldType.STRING));
                }
            }
        } catch (Exception e) {
            LOGGER.error("error loading custom facets for: " + getFullQ(false), e);
        }
        return customFacets;
    }

    private List<String> retrieveCustomFields() {
        List<String> customFields = new ArrayList<String>();
        //look up facets
        final String jsonUri = biocacheServer + "/upload/dynamicFacets?q=" + getFullQ(true) + "&qc=" + getQc();
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(jsonUri);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);
            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONArray ja = (JSONArray) jp.parse(slist);

            for (Object arrayElement : ja) {
                JSONObject jsonObject = (JSONObject) arrayElement;
                String facetName = jsonObject.get(StringConstants.NAME).toString();

                if (!facetName.endsWith("_RNG")) {
                    customFields.add(facetName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("error loading custom facets for: " + jsonUri, e);
        }
        return customFields;
    }

    @Override
    public List<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            List<QueryField> fields = new ArrayList<QueryField>();
            if (true || supportsDynamicFacets) {
                fields.addAll(retrieveCustomFacets());
            }
            //NC: Load all the facets fields from the cache which is populated from the biocache=service
            fields.addAll(CommonData.getFacetQueryFieldList());

            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setStored(true);
            }

            facetFieldList = fields;
        }

        return facetFieldList;
    }

    @Override
    public String getSpeciesIdFieldName() {
        return "taxon_concept_lsid";
    }

    @Override
    public String getRecordIdFieldName() {
        return StringConstants.ID;
    }

    @Override
    public String getRecordLongitudeFieldName() {
        return StringConstants.LONGITUDE;
    }

    @Override
    public String getRecordLatitudeFieldName() {
        return StringConstants.LATITUDE;
    }

    @Override
    public String getRecordIdFieldDisplayName() {
        return StringConstants.ID;
    }

    @Override
    public String getRecordLongitudeFieldDisplayName() {
        return StringConstants.LONGITUDE;
    }

    @Override
    public String getRecordLatitudeFieldDisplayName() {
        return StringConstants.LATITUDE;
    }

    /**
     * Get legend for a facet field.
     *
     * @param colourmode
     * @return
     */
    @Override
    public LegendObject getLegend(String colourmode) {
        if ("-1".equals(colourmode) || StringConstants.GRID.equals(colourmode)) {
            return null;
        }
        LegendObject lo = legends.get(colourmode);
        if (lo != null && lo.getColourMode() != null && !lo.getColourMode().equals(colourmode)) {
            LOGGER.debug("lo not empty and lo=" + lo);
            lo = legends.get(lo.getColourMode());
        }
        if (lo == null && getOccurrenceCount() > 0) {
            HttpClient client = new HttpClient();
            String facetToColourBy = StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) ? StringConstants.OCCURRENCE_YEAR : translateFieldForSolr(colourmode);

            try {
                String url = biocacheServer
                        + LEGEND_SERVICE_CSV
                        + DEFAULT_ROWS
                        + "&q=" + getQ()
                        + "&cm=" + URLEncoder.encode(facetToColourBy, StringConstants.UTF_8)
                        + getQc();
                LOGGER.debug(url);
                GetMethod get = new GetMethod(url);
                //NQ: Set the header type to JSON so that we can parse JSON instead of CSV (CSV has issue with quoted field where a quote is the escape character)
                get.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

                client.executeMethod(get);
                String s = get.getResponseBodyAsString();
                //in the first line do field name replacement
                String t = translateFieldForSolr(colourmode);
                if (!colourmode.equals(t)) {
                    s = s.replaceFirst(t, colourmode);
                }

                lo = new BiocacheLegendObject(colourmode, s);

                //test for exceptions
                if (!colourmode.contains(",")
                        && (StringConstants.UNCERTAINTY.equals(colourmode)
                        || StringConstants.DECADE.equals(colourmode)
                        || StringConstants.OCCURRENCE_YEAR.equals(colourmode)
                        || StringConstants.COORDINATE_UNCERTAINTY.equals(colourmode))) {
                    lo = ((BiocacheLegendObject) lo).getAsIntegerLegend();

                    //apply cutpoints to colourMode string
                    Legend l = lo.getNumericLegend();
                    if (l == null || l.getCutoffFloats() == null) {
                        return null;
                    }

                    float[] cutpoints = l.getCutoffFloats();
                    float[] cutpointmins = l.getCutoffMinFloats();
                    StringBuilder sb = new StringBuilder();
                    //NQ 20140109: use the translated SOLR field as the colour mode so that Constants.DECADE does not cause an issue
                    String newFacet = StringConstants.DECADE.equals(colourmode) ? StringConstants.OCCURRENCE_YEAR : colourmode;
                    sb.append(newFacet);
                    int i = 0;
                    while (i < cutpoints.length) {
                        if (i == cutpoints.length - 1 || cutpoints[i] != cutpoints[i + 1]) {
                            if (i > 0) {
                                sb.append(",").append(cutpointmins[i]);
                                if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
                                    sb.append(StringConstants.DATE_TIME_BEGINNING_OF_YEAR);
                                }
                            } else {
                                sb.append(",*");
                            }
                            sb.append(",").append(cutpoints[i]);
                            if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
                                sb.append(StringConstants.DATE_TIME_END_OF_YEAR);
                            }
                        } else if (i < cutpoints.length - 1 && cutpoints[i] == cutpoints[i + 1]) {
                            cutpointmins[i + 1] = cutpointmins[i];
                        }
                        i++;
                    }
                    String newColourMode = sb.toString();
                    if (StringConstants.OCCURRENCE_YEAR.equals(colourmode) || StringConstants.DECADE.equals(colourmode)) {
                        newColourMode = newColourMode.replace(".0", "");
                    }

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (StringConstants.MONTH.equals(colourmode)) {
                    String newColourMode = "month,00,00,01,01,02,02,03,03,04,04,05,05,06,06,07,07,08,08,09,09,10,10,11,11,12,12";

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (!colourmode.contains(",") && (StringConstants.OCCURRENCE_YEAR_DECADE.equals(colourmode) || StringConstants.DECADE.equals(colourmode))) {
                    Set<Integer> decades = new TreeSet<Integer>();
                    for (double d : ((BiocacheLegendObject) lo).categoriesNumeric.keySet()) {
                        decades.add((int) (d / 10));
                    }
                    List<Integer> d = new ArrayList<Integer>(decades);

                    StringBuilder sb = new StringBuilder();
                    sb.append(StringConstants.OCCURRENCE_YEAR);
                    for (int i = !d.isEmpty() && d.get(0) > 0 ? 0 : 1; i < d.size(); i++) {
                        if (i > 0) {
                            sb.append(",").append(d.get(i));
                            sb.append("0-01-01T00:00:00Z");
                        } else {
                            sb.append(",*");
                        }
                        sb.append(",").append(d.get(i));
                        sb.append("9-12-31T00:00:00Z");
                    }
                    String newColourMode = sb.toString();

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else {
                    legends.put(colourmode, lo);
                }
            } catch (Exception e) {
                LOGGER.error("error getting legend for : " + colourmode, e);
            }
        }

        return lo;
    }

    @Override
    public Query newFacets(List<Facet> facets, boolean forMapping) {
        if ((facets == null || facets.isEmpty()) && (this.forMapping || !forMapping)) {
            return this;
        }
        List<Facet> newFacets = new ArrayList<Facet>();
        if (this.facets != null) {
            newFacets.addAll(this.facets);
        }
        newFacets.addAll(facets);

        return new BiocacheQuery(lsids, rawNames, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }

    @Override
    public String getUrl() {
        return biocacheServer + WMS_URL;
    }

    @Override
    public List<Double> getBBox() {
        if (bbox != null) {
            return bbox;
        }

        bbox = new ArrayList<Double>();

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + BOUNDING_BOX_CSV
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + getQc();

        GetMethod get = new GetMethod(url);
        try {
            client.executeMethod(get);
            String[] s = get.getResponseBodyAsString().split(",");
            for (int i = 0; i < 4; i++) {
                bbox.add(Double.parseDouble(s[i]));
            }
        } catch (Exception e) {
            //default to 'world' bb
            bbox = Util.getBoundingBox(CommonData.WORLD_WKT);

            LOGGER.error("error getting species layer bounding box from biocache:" + url, e);
        }

        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        String spname = getSolrName();

        String lastClass = "md_grey-bg";

        StringBuilder html = new StringBuilder();
        html.append("Species layer\n");
        html.append("<table class='md_table'>");
        html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Species name: </td><td class='md_spacer'/><td class='md_value'>").append(spname).append("</td></tr>");
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        boolean[] gk = getGeospatialKosher();
        if (gk == null) {
            html.append("<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>").append(getSpeciesCount()).append("</td></tr>");
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>").append(getOccurrenceCount()).append("</td></tr>");
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        } else {
            if (wkt != null && !wkt.equals(CommonData.WORLD_WKT)) {
                html.append("<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>").append(getSpeciesCountKosher()).append(" without a flagged spatial issue<br>").append(getSpeciesCountCoordinates()).append(" with any coordinates</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>").append(getOccurrenceCountKosher()).append(" without a flagged spatial issue<br>").append(getOccurrenceCountCoordinates()).append(" with any coordinates</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Number of endemic species: </td><td class='md_spacer'/><td class='md_value'>").append(getEndemicSpeciesCount()).append("</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            } else {
                html.append("<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>").append(getSpeciesCountKosher()).append(" without a flagged spatial issue<br>").append(getSpeciesCountCoordinates()).append(" with any coordinates<br>").append(getSpeciesCountAny()).append(" total including records without coordinates</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>").append(getOccurrenceCountKosher()).append(" without a flagged spatial issue<br>").append(getOccurrenceCountCoordinates()).append(" with any coordinates<br>").append(getOccurrenceCountAny()).append(" total including records without coordinates</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
            String areamsg = "";
            if (wkt != null && !wkt.equals(CommonData.WORLD_WKT)) {
                areamsg = " in the area selected";
            }
            String msg = null;
            if (gk[0] && gk[1]) {
                msg = "Map layer displays records with and without geospatial issues " + areamsg;
            } else if (gk[0]) {
                msg = "Map layer only displays records without geospatial issues " + areamsg;
            } else if (gk[1]) {
                msg = "Map layer only displays records with geospatial issues " + areamsg;
            }
            if (msg != null) {
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'></td><td class='md_spacer'/><td class='md_value'>").append(msg).append("</td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
        }
        if (lsids != null) {
            html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Classification: </td><td class='md_spacer'/><td class='md_value'>");
            for (String s : lsids.split(",")) {
                Map<String, String> classification = getSpeciesClassification(s);
                boolean first = true;
                for (Map.Entry<String, String> o : classification.entrySet()) {
                    if (!first) {
                        html.append(" > ");
                    } else {
                        first = false;
                    }

                    html.append("<a href='").append(CommonData.getBieServer()).append(BIE_SPECIES).append(o.getValue()).append("' target='_blank'>").append(o.getKey()).append("</a> ");
                }

                html.append("<br />");
                html.append("More information for <a href='").append(CommonData.getBieServer()).append(BIE_SPECIES).append(s).append("' target='_blank'>").append(getScientificNameRank(s).split(",")[0]).append("</a>");
                html.append("<br />");
                html.append("<br />");
            }
            html.append("</td></tr>");
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }

        String dataProviders = StringUtils.trimToNull(getDataProviders());
        if (dataProviders != null) {
            html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Data providers: </td><td class='md_spacer'/><td class='md_value'>").append(getDataProviders()).append("</td></tr>");
        }
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        if (lsids != null && lsids.length() > 0) {
            html.append("<tr class='").append(lastClass).append("'><td class='md_th'>List of LSIDs: </td><td class='md_spacer'/><td class='md_value'>").append(lsids).append("</td></tr>");
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

            String[] wms = CommonData.getSpeciesDistributionWMS(lsids);
            if (wms.length > 0) {
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Expert distributions</td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openDistributions(\"").append(lsids).append("\")'>").append(wms.length).append("</a></td><td class='md_spacer'/><td class='md_value'></td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }

            wms = CommonData.getSpeciesChecklistWMS(lsids);
            if (wms.length > 0) {
                html.append("<tr class='").append(lastClass).append("'><td class='md_th'>Checklist species</td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openChecklists(\"").append(lsids).append("\")'>").append(wms.length).append("</a></td><td class='md_spacer'/><td class='md_value'></td></tr>");
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
        }

        //ensure qid exists
        if (paramId == null) {
            forMapping = true;
            makeParamId();
        }

        html.append("<tr class='").append(lastClass).append("'><td class='md_th'>QID</td><td class='md_spacer'/><td class='md_value'>").append(paramId).append(" (" + biocacheServer + ")").append("</td></tr>");
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";


        html.append("<tr class='").append(lastClass).append("'><td class='md_th' colspan=3><a href='").append(biocacheWebServer).append("/occurrences/search?q=").append(getQ()).append("' target='_blank'>Table view of these records</a></td></tr>");

        //link to new download page in ala-hub
        try {
            html.append("<tr class='").append(lastClass).append("'><td class='md_th' colspan=3><a href='").append(biocacheWebServer).append("/download?searchParams=%3Fq%3D").append(URLEncoder.encode(getQ(), "UTF-8")).append("&targetUri=/occurrences/search' target='_blank'>Standard download of these records</a></td></tr>");
        } catch (Exception e) {
        }

        html.append("</table>");

        return html.toString();
    }

    public String getQid() {
        //ensure qid exists
        if (paramId == null) {
            forMapping = true;
            makeParamId();
        }
        return paramId;
    }

    private Map<String, String> getSpeciesClassification(String lsid) {

        String[] classificationList = {StringConstants.KINGDOM, StringConstants.PHYLUM, StringConstants.CLASS, StringConstants.ORDER, StringConstants.FAMILY, StringConstants.GENUS, StringConstants.SPECIES, StringConstants.SUB_SPECIES};
        Map<String, String> classification = new LinkedHashMap<String, String>();

        String snUrl = "true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie")) ?
                CommonData.getBieServer() + BIE_SPECIES_WS + lsid + ".json" :
                CommonData.getBieServer() + BIE_SPECIES + lsid + ".json";

        LOGGER.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader(StringConstants.CONTENT_TYPE, StringConstants.APPLICATION_JSON);

            client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(slist);
            String r = ((JSONObject) jo.get("taxonConcept")).get("rankString").toString();

            JSONObject joOcc = (JSONObject) jo.get("classification");
            for (String c : classificationList) {
                if (c.equals(r)) {
                    break;
                }
                if (joOcc.containsKey(c.replace("ss", "zz"))) {
                    classification.put(joOcc.get(c.replace("ss", "zz")).toString(), joOcc.get(c.replace("ss", "zz") + "Guid").toString());
                }
            }

        } catch (Exception e) {
            LOGGER.error("error getting scientific name at: " + snUrl, e);
        }

        return classification;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        //this default behaviour of excluding default fields from the download URL may change
        List<String> fieldsAlreadyIncluded = new ArrayList<String>();
        String[] defaultFields = getDefaultDownloadFields();
        if (defaultFields.length > 0) {
            Collections.addAll(fieldsAlreadyIncluded, defaultFields);
        }

        StringBuilder sb = new StringBuilder();
        try {
            sb.append("&fields=").append(URLEncoder.encode(CommonData.getSettings().getProperty("biocache_download_fields"), StringConstants.UTF_8));
            List<String> customFields = retrieveCustomFields();
            for (int i = 0; i < customFields.size(); i++) {
                sb.append(",").append(customFields.get(i));
            }
        } catch (Exception e) {
            LOGGER.error("webportal-config.properties biocache_download_fields error while encoding to UTF-8", e);
        }
        sb.append("&extra=").append(CommonData.getExtraDownloadFields());
        if (extraFields != null && extraFields.length > 0) {
            for (int i = 0; i < extraFields.length; i++) {
                if (!fieldsAlreadyIncluded.contains(extraFields[i])) {
                    //Solr download has some default fields
                    // these include the 'translate' fields
                    // remove them from extraFields
                    if (translateFieldForSolr(extraFields[i]) != null
                            && !extraFields[i].equals(translateFieldForSolr(extraFields[i]))) {
                        continue;
                    }

                    if (sb.length() > 0) {
                        sb.append(",").append(extraFields[i]);
                    }
                }
            }
        }
        return biocacheServer + DOWNLOAD_URL + "q=" + getQ() + sb.toString() + getQc();
    }

    private String getDataProviders() {
        HttpClient client = new HttpClient();
        String url = biocacheServer
                + DATA_PROVIDERS_SERVICE
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + getQc();
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {

                StringBuilder html = new StringBuilder();

                JSONParser jp = new JSONParser();
                JSONArray ja = (JSONArray) jp.parse(response);
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = (JSONObject) ja.get(i);
                    html.append("<a href='https://collections.ala.org.au/public/showDataProvider/")
                            .append(jo.get(StringConstants.ID).toString())
                            .append("' target='_blank'>").append(jo.get(StringConstants.NAME))
                            .append("</a>: ").append(jo.get(StringConstants.COUNT)).append(" records <br />");
                }

                return html.toString();
            }
        } catch (Exception e) {
            LOGGER.error("error getting query data providers for html:" + url, e);
        }

        return null;
    }

    public String getLsids() {
        return lsids;
    }


    public List<String> getLsidFromExtraParams() {
        List<String> extraLsids = new ArrayList<String>();
        if (extraParams != null) {
            //extraLsids
            Matcher matcher = lsidPattern.matcher(extraParams);
            while (matcher.find()) {
                extraLsids.add(matcher.group().replaceFirst("lsid:", "").replaceAll("\"", ""));
            }
        }
        return extraLsids;
    }

    public String getSolrName() {
        if (solrName != null) {
            return solrName;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + QUERY_TITLE_URL
                + "&q=" + getQ()
                + getQc()
                + "&pageSize=0&facet=false";
        LOGGER.debug("Retrieving query metadata: " + url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {

                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(response);

                if (jo.containsKey("queryTitle")) {
                    String title = jo.get("queryTitle").toString();

                    //clean default parameter
                    title = title.replace(" AND <span>null</span>", "");
                    title = title.replace(" AND null", "");

                    //clean spans
                    int p1 = title.indexOf("<span");
                    while (p1 >= 0) {
                        int p2 = title.indexOf('>', p1);
                        int p3 = title.indexOf("</span>", p2);
                        title = title.substring(0, p1) + title.substring(p2 + 1, p3)
                                + (p3 + 7 < title.length() ? title.substring(p3 + 7, title.length()) : "");

                        p1 = title.indexOf("<span");
                    }

                    solrName = title;
                    LOGGER.debug("solrName12=" + solrName);
                    return solrName;
                }
            }
        } catch (Exception e) {
            LOGGER.error("error getting solr name for a query: " + url, e);
        }

        return null;
    }

    @Override
    public final String getQc() {
        return qc;
    }

    @Override
    public void setQc(String qc) {
        this.qc = qc;
    }

    public String getBS() {
        return biocacheServer;
    }

    @Override
    public String[] getDefaultDownloadFields() {
        String s = CommonData.getSettings().getProperty("default_biocache_download_layer_fields");

        if (s != null && s.length() > 0) {
            return s.split(",");
        } else {
            return new String[0];
        }
    }

    public String getWS() {
        return biocacheWebServer;
    }

    /**
     * Get the column header name for a column in the output of sampling.
     *
     * @param colourMode
     * @return
     */
    @Override
    public String getRecordFieldDisplayName(String colourMode) {
        //sampling column name is the same as colourMode name.
        return colourMode;
    }

    /**
     * Add or remove one record to an internal group.
     */
    @Override
    public void flagRecord(String id, boolean set) {
        if (set) {
            flaggedRecords.add(id);
        } else {
            flaggedRecords.remove(id);
        }
    }

    /**
     * Get the number of flagged records.
     */
    @Override
    public int flagRecordCount() {
        return flaggedRecords.size();
    }

    /**
     * Get the list of flagged records as '\n' separated String.
     */
    @Override
    public String getFlaggedRecords() {
        StringBuilder sb = new StringBuilder();
        for (String s : flaggedRecords) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Create a new Query including or excluding flagged records
     */
    @Override
    public Query newFlaggedRecords(boolean include) {
        if (flagRecordCount() == 0) {
            return this;
        }

        StringBuilder sb = new StringBuilder();
        if (include) {
            for (String s : flaggedRecords) {
                if (sb.length() > 0) {
                    sb.append(" OR ");
                }
                sb.append("id:").append(s);
            }
        } else {
            for (String s : flaggedRecords) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                sb.append("-id:").append(s);
            }
        }
        List<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        newFacets.add(Facet.parseFacet(sb.toString()));
        return new BiocacheQuery(lsids, rawNames, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }
}
