/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.data;

import au.org.ala.spatial.util.CommonData;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.Legend;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.legend.QueryField;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * TODO NC 2013-08-15 - Remove all the references to the "include null" gesopatially kosher. I have removed from the UI but I didn't want to
 * break the build before the next release
 *
 * @author Adam
 */
//@Configurable(autowire = Autowire.BY_TYPE)
public class BiocacheQuery implements Query, Serializable {
    //    @Autowired
//    FacetCache facetCache;
    static final String SAMPLING_SERVICE_CSV_GZIP = "/webportal/occurrences.gz?";
    static final String SAMPLING_SERVICE = "/webportal/occurrences?";
    static final String SPECIES_LIST_SERVICE = "/webportal/species?";
    static final String SPECIES_LIST_SERVICE_CSV = "/webportal/species.csv?";
    static final String DOWNLOAD_URL = "/occurrences/download?";
    static final String DATA_PROVIDERS_SERVICE = "/webportal/dataProviders?";
    static final String QUERY_TITLE_URL = "/occurrences/search?";
    static final String LEGEND_SERVICE_CSV = "/webportal/legend?";
    static final String BOUNDING_BOX_CSV = "/webportal/bbox?";
    static final String INDEXED_FIELDS_LIST = "/indexed/fields?";
    static final String POST_SERVICE = "/webportal/params?";
    static final String ENDEMIC_COUNT_SERVICE = "/explore/counts/endemic?";
    static final String ENDEMIC_SPECIES_SERVICE_CSV = "/explore/endemic/species.csv?";
    static final String DEFAULT_ROWS = "pageSize=1000000";
    static final String DEFAULT_ROWS_LARGEST = "pageSize=1000000";
    //protected Pattern lsidPattern = Pattern.compile("(^|\\s|\"|\\(|\\[|')lsid:\"?([a-zA-Z0-9\\.:\\-_]*)\"?");
    static final Pattern queryParamsPattern = Pattern.compile("&([a-zA-Z0-9_\\-]+)=");
    /**
     * DEFAULT_VALIDATION must not be null
     */
    //static final String DEFAULT_VALIDATION = "longitude:[-180 TO 180] AND latitude:[-90 TO 90]";
    static final String DEFAULT_VALIDATION = "";
    static final String BIE_SPECIES = "/species/";
    static final String WMS_URL = "/webportal/wms/reflect?";
    private static String[] commonTaxonRanks = new String[]{
            "cultivar",
            "superfamily",
            "subgenus",
            "unranked",
            "infrageneric",
            "subfamily",
            "subspecies",
            "section",
            "infraspecific",
            "hybrid",
            "variety",
            "form",
            "series",
            "tribe"
    };
    //query inputs
    String name;
    String rank;
    String lsids;
    String[] rawNames;
    ArrayList<Facet> facets;
    String wkt;
    String extraParams;
    String paramId;
    String qc;
    String biocacheWebServer;
    String biocacheServer;
    boolean supportsDynamicFacets;
    boolean forMapping;
    //stored query responses.
    String speciesList = null;
    String endemicSpeciesList = null;
    int speciesCount = -1;
    int endemicSpeciesCount = -1;
    int occurrenceCount = -1;
    double[] points = null;
    String solrName = null;
    //static String[][] facetNameExceptions = {{"cl22", "state"}, {"cl959", "places"}, {"cl20", "ibra"}, {"cl21", "imcra"}};
    HashMap<String, LegendObject> legends = new HashMap<String, LegendObject>();
    HashSet<String> flaggedRecords = new HashSet<String>();
    private static Logger logger = Logger.getLogger(BiocacheQuery.class);

    static String translateFieldForSolr(String facetName) {
        if (facetName == null) {
            return facetName;
        }
        for (String[] s : CommonData.facetNameExceptions) {
            if (facetName.equals(s[0])) {
                facetName = s[1];
                break;
            }
        }
        if ("occurrence_year_individual".equals(facetName)) {
            facetName = "occurrence_year";
        }
        if ("occurrence_year_decade".equals(facetName)) {
            facetName = "occurrence_year";
        }
        return facetName;
    }

    static String translateSolrForField(String facetName) {
        for (String[] s : CommonData.facetNameExceptions) {
            if (facetName.equals(s[1])) {
                facetName = s[0];
                break;
            }
        }
        return facetName;
    }

    public BiocacheQuery(String lsids, String wkt, String extraParams, ArrayList<Facet> facets, boolean forMapping, boolean[] geospatialKosher) {
        this(lsids, null, wkt, extraParams, facets, forMapping, geospatialKosher);
    }

    public BiocacheQuery(String lsids, String[] rawNames, String wkt, String extraParams, ArrayList<Facet> facets, boolean forMapping, boolean[] geospatialKosher) {
        this.lsids = lsids;
        this.rawNames = rawNames;
        if (facets != null) {
            this.facets = (ArrayList<Facet>) facets.clone();
        }
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : wkt;
        this.extraParams = extraParams;
        this.forMapping = forMapping;
        this.qc = CommonData.biocacheQc;

        this.biocacheWebServer = CommonData.biocacheWebServer;
        this.biocacheServer = CommonData.biocacheServer;

        if (geospatialKosher != null) {
            addGeospatialKosher(geospatialKosher);
        }

        makeParamId();
    }

    public BiocacheQuery(String lsids, String wkt, String extraParams, ArrayList<Facet> facets, boolean forMapping, boolean[] geospatialKosher, String biocacheServer, String biocacheWebServer, boolean supportsDynamicFacets) {
        this(lsids, null, wkt, extraParams, facets, forMapping, geospatialKosher, biocacheServer, biocacheWebServer, supportsDynamicFacets);
    }

    public BiocacheQuery(String lsids, String[] rawNames, String wkt, String extraParams, ArrayList<Facet> facets, boolean forMapping, boolean[] geospatialKosher, String biocacheServer, String biocacheWebServer, boolean supportsDynamicFacets) {
        this.lsids = lsids;
        this.rawNames = rawNames;
        if (facets != null) {
            this.facets = (ArrayList<Facet>) facets.clone();
        }
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : wkt;
        this.extraParams = extraParams;
        this.forMapping = forMapping;
        this.qc = CommonData.biocacheQc;
        this.supportsDynamicFacets = supportsDynamicFacets;

        if (biocacheServer != null && biocacheWebServer != null) {
            this.biocacheWebServer = biocacheWebServer;
            this.biocacheServer = biocacheServer;
        } else {
            this.biocacheWebServer = CommonData.biocacheWebServer;
            this.biocacheServer = CommonData.biocacheServer;
        }

        if (geospatialKosher != null) {
            addGeospatialKosher(geospatialKosher);
        }

        makeParamId();
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

    static Facet makeFacetGeospatialKosher(boolean includeTrue, boolean includeFalse, boolean includeNull) {
        if (includeTrue && includeFalse && includeNull) {
            return null;    //31192356
        } else if (!includeTrue && !includeFalse && !includeNull) {
            return new Facet("*", "*", false);  //0
        } else if (includeTrue && !includeFalse && !includeNull) {
            return new Facet("geospatial_kosher", "true", true); //28182161
        } else if (!includeTrue && includeFalse && !includeNull) {
            return new Facet("geospatial_kosher", "false", true); //2211703
        } else if (!includeTrue && !includeFalse && includeNull) {
            return new Facet("geospatial_kosher", "*", false); //798492
        } else if (includeTrue && includeFalse && !includeNull) {
            return new Facet("geospatial_kosher", "*", true); //30393864
        } else if (includeTrue && !includeFalse && includeNull) {
            return new Facet("geospatial_kosher", "false", false); //28980653
        } else { //if(!includeTrue && includeFalse && includeNull) {
            return new Facet("geospatial_kosher", "true", false); //3010195
        }
    }

    public static boolean[] parseGeospatialKosher(String facet) {
        boolean[] geospatial_kosher = null;
        if (facet != null) {
            String f = facet.replace("\"", "").replace("(", "").replace(")", "");
            if (f.equals("geospatial_kosher:true")) {
                geospatial_kosher = new boolean[]{true, false, false};
            } else if (f.equals("geospatial_kosher:false")) {
                geospatial_kosher = new boolean[]{false, true, false};
            } else if (f.equals("-geospatial_kosher:*")) {
                geospatial_kosher = new boolean[]{false, false, true};
            } else if (f.equals("geospatial_kosher:*")) {
                geospatial_kosher = new boolean[]{true, true, false};
            } else if (f.equals("-geospatial_kosher:false")) {
                geospatial_kosher = new boolean[]{true, false, true};
            } else if (f.equals("-geospatial_kosher:true")) {
                geospatial_kosher = new boolean[]{false, true, true};
            }
        }
        return geospatial_kosher;
    }

    public boolean[] getGeospatialKosher() {
        if ((lsids + extraParams).contains("geospatial_kosher:")) {
            //must be in a Facet to be compatible
            return null;
        }

        boolean[] geospatial_kosher = new boolean[]{true, true, true};
        if (facets != null) {
            for (int i = 0; i < facets.size(); i++) {
                String f = facets.get(i).toString().replace("\"", "").replace("(", "").replace(")", "");
                if (f.contains("geospatial_kosher:")) {
                    if (f.equals("geospatial_kosher:true")) {
                        geospatial_kosher = new boolean[]{true, false, false};
                    } else if (f.equals("geospatial_kosher:false")) {
                        geospatial_kosher = new boolean[]{false, true, false};
                    } else if (f.equals("-geospatial_kosher:*")) {
                        geospatial_kosher = new boolean[]{false, false, true};
                    } else if (f.equals("geospatial_kosher:*")) {
                        geospatial_kosher = new boolean[]{true, true, false};
                    } else if (f.equals("-geospatial_kosher:false")) {
                        geospatial_kosher = new boolean[]{true, false, true};
                    } else if (f.equals("-geospatial_kosher:true")) {
                        geospatial_kosher = new boolean[]{false, true, true};
                    }
                    break;
                }
            }
        }
        return geospatial_kosher;
    }

    public BiocacheQuery newFacetGeospatialKosher(boolean[] geospatialKosher, boolean forMapping) {
        boolean[] gk = getGeospatialKosher();

        //cannot create the new facet
        if (gk == null) {
            //This should never happen.
            logger.error("Attempted to add a geospatial_kosher facet to an unsupported query: '" + lsids + "', '" + extraParams + "'");
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
        ArrayList<Facet> newFacets = new ArrayList<Facet>();
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

        ArrayList<Facet> newFacets = new ArrayList<Facet>();
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
            logger.error("error getting new WKT from an intersection", e);
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
    public String sample(ArrayList<QueryField> fields) {
        HttpClient client = new HttpClient();
        String url = biocacheServer
                + SAMPLING_SERVICE_CSV_GZIP
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + paramQueryFields(fields)
                + getQc();
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        String sample = null;

        long start = System.currentTimeMillis();
        try {
            int result = client.executeMethod(get);
            sample = decompressGz(get.getResponseBodyAsStream());

            //in the first line do field name replacement
            for (QueryField f : fields) {
                String t = translateFieldForSolr(f.getName());
                if (!f.getName().equals(t)) {
                    sample = sample.replaceFirst(t, f.getName());
                }
            }
        } catch (Exception e) {
            logger.error("error sampling", e);
        }
        logger.debug("get sample in " + (System.currentTimeMillis() - start) + "ms");

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
                        + "&fprefix=" + URLEncoder.encode(value, "UTF-8") + "&pageSize=0&flimit=" + limit;
                GetMethod get = new GetMethod(url);
                get.addRequestHeader("Content-type", "text/plain");
                int result = client.executeMethod(get);
                if (result == 200) {
                    //success
                    String rawJSON = get.getResponseBodyAsString();
                    //parse
                    JSONObject jo = JSONObject.fromObject(rawJSON);

                    JSONArray ja = jo.getJSONArray("facetResults");
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject o = ja.getJSONObject(i);
                        if (o.getString("fieldName").equals(facet)) {
                            //process the values in the list
                            JSONArray values = o.getJSONArray("fieldResult");
                            for (int j = 0; j < values.size(); j++) {
                                JSONObject vo = values.getJSONObject(j);
                                if (slist.length() > 0) {
                                    slist.append("\n");
                                }
                                slist.append(vo.getString("label")).append("//found ").append(Integer.toString(vo.getInt("count")));
                            }
                        }
                    }
                } else {
                    logger.warn("There was an issue performing the autocomplete from the biocache: " + result);
                }

            } catch (Exception e) {
                logger.error("failed to get autocomplete facet=" + facet + ", value=" + value, e);
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
                + DEFAULT_ROWS_LARGEST
                + "&q=" + getQ()
                + getQc();
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            speciesList = get.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("error getting species list from: " + url);
        }

        return speciesList;
    }

    public String endemicSpeciesList() {
        if (endemicSpeciesList != null) {
            return endemicSpeciesList;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + ENDEMIC_SPECIES_SERVICE_CSV
                + "q=" + getQ()
                + getQc();
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            endemicSpeciesList = get.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("error getting endemic species result", e);
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
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            String start = "\"totalRecords\":";
            String end = ",";
            int startPos = response.indexOf(start) + start.length();

            occurrenceCount = Integer.parseInt(response.substring(startPos, response.indexOf(end, startPos)));
        } catch (Exception e) {
            logger.error("error getting records count: " + url, e);
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

        //fill 'speciesList'
        speciesList();

        speciesCount = 0; //first line is header, last line is not \n terminated
        int p = 0;
        while ((p = speciesList.indexOf('\n', p + 1)) > 0) {
            speciesCount++;
        }

        return speciesCount;
    }

    int speciesCountKosher = -1, speciesCountCoordinates = -1, speciesCountAny = -1;
    int occurrenceCountKosher = -1, occurrenceCountCoordinates = -1, occurrenceCountAny = -1;


    @Override
    public int getEndemicSpeciesCount() {
        if (endemicSpeciesCount >= 0 || wkt == null)
            return endemicSpeciesCount;
        //fill endemic species list
        endemicSpeciesList();

        endemicSpeciesCount = 0; //first line is header, last line is not \n terminated
        int p = 0;
        while ((p = endemicSpeciesList.indexOf('\n', p + 1)) > 0) {
            endemicSpeciesCount++;
        }

        return endemicSpeciesCount;

        //othewise we need to determine the endemic species count.
//        HttpClient client = new HttpClient();
//        String url = biocacheServer
//                + ENDEMIC_COUNT_SERVICE                
//                + "q=" + getQ()
//                + getQc() 
//                +"&facets=species_guid";
//        logger.debug(url);
//        GetMethod get = new GetMethod(url);
//
//        try {
//            int result = client.executeMethod(get);
//            String value = get.getResponseBodyAsString();
//            endemicSpeciesCount=Integer.parseInt(value);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        return endemicSpeciesCount;
    }

    public int getSpeciesCountKosher() {
        if (speciesCountKosher >= 0) {
            return speciesCountKosher;
        }
        return speciesCountKosher = newFacetGeospatialKosher(new boolean[]{true, false, false}, false).getSpeciesCount();
    }

    public int getSpeciesCountCoordinates() {
        if (speciesCountCoordinates >= 0) {
            return speciesCountCoordinates;
        }
        return speciesCountCoordinates = newFacetGeospatialKosher(new boolean[]{true, true, false}, false).getSpeciesCount();
    }

    public int getSpeciesCountAny() {
        if (speciesCountAny >= 0) {
            return speciesCountAny;
        }
        return speciesCountAny = newFacetGeospatialKosher(new boolean[]{true, true, true}, false).getSpeciesCount();
    }

    public int getOccurrenceCountKosher() {
        if (occurrenceCountKosher >= 0) {
            return occurrenceCountKosher;
        }
        return occurrenceCountKosher = newFacetGeospatialKosher(new boolean[]{true, false, false}, false).getOccurrenceCount();
    }

    public int getOccurrenceCountCoordinates() {
        if (occurrenceCountCoordinates >= 0) {
            return occurrenceCountCoordinates;
        }
        return occurrenceCountCoordinates = newFacetGeospatialKosher(new boolean[]{true, true, false}, false).getOccurrenceCount();
    }

    public int getOccurrenceCountAny() {
        if (occurrenceCountAny >= 0) {
            return occurrenceCountAny;
        }
        return occurrenceCountAny = newFacetGeospatialKosher(new boolean[]{true, true, true}, false).getOccurrenceCount();
    }



    String paramQueryFields(ArrayList<QueryField> fields) {
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
    public String getQ() {
        if (paramId != null) {
            return "qid:" + paramId;
        }

        return getFullQ(true);
    }

    @Override
    public String getFullQ(boolean encode) {
        StringBuilder sb = new StringBuilder();

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
                if (queryTerms > 0)
                    sb.append(" OR ");
                else
                    sb.append("(");
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

        if (facets != null && facets.size() > 0) {
            for (int i = 0; i < facets.size(); i++) {
                if (queryTerms > 0) {
                    sb.append(" AND ");
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
                sb.append(facet);
                queryTerms++;
            }
        }

        if (DEFAULT_VALIDATION.length() > 0) {
            if (queryTerms > 0) {
                sb.append(" AND ");
            }
            queryTerms++;
            sb.append(DEFAULT_VALIDATION);
        }

        //extra parameters
        if (extraParams != null) {
            if (queryTerms > 0) {
                sb.append(" AND ");
            }
            queryTerms++;
            sb.append(extraParams);
        }

        if (encode) {
            String s = sb.toString();
            sb = new StringBuilder();
            try {
                sb.append(URLEncoder.encode(s, "UTF-8"));
            } catch (Exception e) {
                logger.error("error encoding: " + s, e);
            }
        }

        //wkt term
        if (wkt != null) {
            sb.append("&wkt=").append(wkt.replace(" ", ":"));
        }

        try {
            return sb.toString();
        } catch (Exception e) {
            logger.error("error returning a string", e);
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
        if (wkt == null && !forMapping && getFullQ(true).length() < CommonData.maxQLength) {
            return;
        }

        HttpClient client = new HttpClient();
        String url = biocacheServer
                + POST_SERVICE
                + getQc()
                + "&facet=false";
        PostMethod post = new PostMethod(url);
        try {
            //NQ: query values could contain embedded &'s 
            String[] qs = splitOnParams(getFullQ(false));//getFullQ(false).split("&");
            for (int i = 0; i < qs.length; i++) {
                String q = qs[i];
                int p = q.indexOf('=');
                if (p < 0) {
                    post.addParameter("q", q.substring(p + 1));
                    logger.debug("param: " + "q" + " : " + q.substring(p + 1));
                } else {
                    post.addParameter(q.substring(0, p), q.substring(p + 1));
                    logger.debug("param: " + q.substring(0, p) + " : " + q.substring(p + 1));
                }
                post.addParameter("bbox", forMapping ? "true" : "false");
            }
            int result = client.executeMethod(post);
            String response = post.getResponseBodyAsString();

            if (result == 200) {
                paramId = response;

                logger.debug(url + " > " + paramId);
            } else {
                logger.debug("error with url:" + url + " posting q: " + getQ() + " > response_code:" + result + " response:" + response);
            }
        } catch (Exception e) {
            logger.error("error getting biocache param id from: " + url + " for " + getQ(), e);
        }
    }

    public static void main(String[] args) throws Exception {
        String url = "http://www.example.com/something.html?one=11111&two=22222&three=%2233%20&%20333%22";

        logger.debug(url);
        //url = URLEncoder.encode(url, "UTF-8");
        //logger.debug(url);
        List<org.apache.http.NameValuePair> params = org.apache.http.client.utils.URLEncodedUtils.parse(new java.net.URI(url), "UTF-8");

        for (org.apache.http.NameValuePair param : params) {
            logger.debug(param.getName() + " : " + param.getValue());
        }

        String test1 = "kosher:true &qc=dh1";
        printTest(test1, splitOnParams(test1));
        String test2 = "&q=testong AND bdsf&wkt=dfhjkdghf&qc=dh1";
        printTest(test2, splitOnParams(test2));
    }

    private static void printTest(String original, String[] ts) {
        logger.debug(original);
        for (String t : ts) {
            logger.debug(t);
        }
    }

    /**
     * This method will correctly split on params handling the case where emebedded &'s can exist
     *
     * @param query
     * @return
     */
    private static String[] splitOnParams(String query) {
        String[] totals = query.split(queryParamsPattern.toString());
        //printTest(query, totals);
        //m.
        int i = 1;
        if (totals.length > 1) {
            Matcher m = queryParamsPattern.matcher(query);
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

        if (lsids.split(",").length == 1) {
            rank = getScientificNameRank(lsids).split(",")[1];

            if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
                rank = "taxon";
            }
        } else {
            rank = "scientific name";
        }

        return rank;
    }

    //    /**
//     * test
//     *
//     * @param args
//     */
//    static public void main(String[] args) {
//        BiocacheQuery sq = new BiocacheQuery();
//
//        //count all occurrences and species
//        logger.debug("total number of occurrences: " + sq.getOccurrenceCount());
//        logger.debug("total number of species: " + sq.getSpeciesCount());
//
//        //Repeat in a polygon
//        String wkt = "POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))";
//        sq.addWkt(wkt);
//        logger.debug("wkt: " + wkt);
//        logger.debug("number of occurrences in wkt: " + sq.getOccurrenceCount());
//        logger.debug("number of species in wkt: " + sq.getSpeciesCount());
//
//        //Sample some points
//        ArrayList<QueryField> fields = new ArrayList<QueryField>();
//        fields.add(new QueryField("id", "uuid"));
//        String sample = sq.sample(fields);
//        logger.debug("sample:");
//        logger.debug(sample.substring(0, 500) + ((sample.length() <= 500) ? "" : "..."));
//
//        //Sample some coordinates
//        double[] coordinates = sq.getPoints(null);
//        logger.debug("coordinates:");
//        for (int i = 0; coordinates != null && i < coordinates.length && i < 100; i += 2) {
//            System.out.print(coordinates[0] + "," + coordinates[1] + " ");
//        }
//
//    }
//    public String getSingleLsid() {
//        return (lsids.size() == 1) ? lsids.get(0) : "";
//    }
    static public String getScientificNameRank(String lsid) {

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        logger.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            logger.debug("Arrays.binarySearch(commonTaxonRanks, rank): " + Arrays.binarySearch(commonTaxonRanks, rank));
            if (Arrays.binarySearch(commonTaxonRanks, rank) > -1) {
                rank = "taxon";
            }

            return scientficName + "," + rank;
        } catch (Exception e) {
            logger.error("error getting scientific name:" + snUrl, e);
        }

        return null;
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
            logger.error("error decompressing gz stream", e);
        }
        gzipped.close();

        return s;
    }

    ArrayList<QueryField> facetFieldList = null;

    private Pattern dataResourceUidP = Pattern.compile("data_resource_uid:([\\\"]{0,1}[a-z]{2,3}[0-9]{1,}[\\\"]{0,1})");

    /**
     * Retrieves a list of custom facets for the supplied query.
     *
     * @return
     */
    private List<QueryField> retrieveCustomFacets() {
        List<QueryField> customFacets = new ArrayList<QueryField>();
        //look up facets
        final String jsonUri = biocacheServer + "/upload/dynamicFacets?q=" + getFullQ(true) + "&qc=" + getQc();
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(jsonUri);
            get.addRequestHeader("Content-type", "application/json");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            for (Object arrayElement : ja) {
                JSONObject jsonObject = (JSONObject) arrayElement;
                String facetName = jsonObject.getString("name");
                String facetDisplayName = jsonObject.getString("displayName");

                logger.debug("Adding custom index : " + arrayElement);
                customFacets.add(new QueryField(facetName, facetDisplayName, QueryField.GroupType.CUSTOM, QueryField.FieldType.STRING));
            }
        } catch (Exception e) {
            //System.err.println("Unable to load custom facets for : " + dr);
            //e.printStackTrace();
            logger.error("error loading custom facets for: " + jsonUri, e);
        }
        return customFacets;
    }

    @Override
    public ArrayList<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            if (supportsDynamicFacets) {
                fields.addAll(retrieveCustomFacets());
            }
            //NC: Load all the facets fields from the cache which is populated from the biocache=service
            fields.addAll(FacetCacheImpl.getFacetQueryFieldList());

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
        return "id";
    }

    @Override
    public String getRecordLongitudeFieldName() {
        return "longitude";
    }

    @Override
    public String getRecordLatitudeFieldName() {
        return "latitude";
    }

    @Override
    public String getRecordIdFieldDisplayName() {
        return "id";
    }

    @Override
    public String getRecordLongitudeFieldDisplayName() {
        return "longitude";
    }

    @Override
    public String getRecordLatitudeFieldDisplayName() {
        return "latitude";
    }

    /**
     * Get legend for a facet field.
     *
     * @param colourmode
     * @return
     */
    @Override
    public LegendObject getLegend(String colourmode) {
        if (colourmode.equals("-1") || colourmode.equals("grid")) {
            return null;
        }
        LegendObject lo = legends.get(colourmode);
        if (lo != null && lo.getColourMode() != null && !lo.getColourMode().equals(colourmode)) {
            logger.debug("lo not empty and lo=" + lo);
            lo = legends.get(lo.getColourMode());
        }
        if (lo == null) {
            HttpClient client = new HttpClient();
            String facetToColourBy = colourmode.equals("occurrence_year_decade") ? "occurrence_year" : translateFieldForSolr(colourmode);

            try {
                String url = biocacheServer
                        + LEGEND_SERVICE_CSV
                        + DEFAULT_ROWS
                        + "&q=" + getQ()
                        + "&cm=" + URLEncoder.encode(facetToColourBy, "UTF-8")
                        + getQc();
                logger.debug(url);
                GetMethod get = new GetMethod(url);
                //NQ: Set the header type to JSON so that we can parse JSON instead of CSV (CSV has issue with quoted field where a quote is the escape character)
                get.addRequestHeader("Accept", "application/json");

                //String legend = null;

                int result = client.executeMethod(get);
                String s = get.getResponseBodyAsString();
                //in the first line do field name replacement
                String t = translateFieldForSolr(colourmode);
                if (!colourmode.equals(t)) {
                    s = s.replaceFirst(t, colourmode);
                }

                lo = new BiocacheLegendObject(colourmode, s);

                //test for exceptions
                if (!colourmode.contains(",") && (colourmode.equals("uncertainty") || colourmode.equals("decade") || colourmode.equals("occurrence_year") || colourmode.equals("coordinate_uncertainty"))) {
                    lo = ((BiocacheLegendObject) lo).getAsIntegerLegend();

                    //apply cutpoints to colourMode string
                    Legend l = lo.getNumericLegend();
                    float[] minmax = l.getMinMax();
                    float[] cutpoints = l.getCutoffFloats();
                    float[] cutpointmins = l.getCutoffMinFloats();
                    StringBuilder sb = new StringBuilder();
                    //NQ 20140109: use the translated SOLR field as the colour mode so that "decade" does not cause an issue
                    String newFacet = colourmode.equals("decade") ? "occurrence_year" : colourmode;
                    sb.append(newFacet);
                    int i = 0;
                    int lasti = 0;
                    while (i < cutpoints.length) {
                        if (i == cutpoints.length - 1 || cutpoints[i] != cutpoints[i + 1]) {
                            if (i > 0) {
                                sb.append(",").append(cutpointmins[i]);
                                if (colourmode.equals("occurrence_year") || colourmode.equals("decade"))
                                    sb.append("-01-01T00:00:00Z");
                            } else {
                                sb.append(",*");
                            }
                            sb.append(",").append(cutpoints[i]);
                            if (colourmode.equals("occurrence_year") || colourmode.equals("decade"))
                                sb.append("-12-31T00:00:00Z");
                            lasti = i;
                        }
                        i++;
                    }
                    String newColourMode = sb.toString();
                    if (colourmode.equals("occurrence_year") || colourmode.equals("decade")) {
                        newColourMode = newColourMode.replace(".0", "");
                    }

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (colourmode.equals("month")) {
                    String newColourMode = "month,00,00,01,01,02,02,03,03,04,04,05,05,06,06,07,07,08,08,09,09,10,10,11,11,12,12";

                    lo.setColourMode(newColourMode);
                    legends.put(colourmode, lo);

                    LegendObject newlo = getLegend(newColourMode);
                    newlo.setColourMode(newColourMode);
                    newlo.setNumericLegend(lo.getNumericLegend());
                    legends.put(newColourMode, newlo);

                    lo = newlo;
                } else if (!colourmode.contains(",") && (colourmode.equals("occurrence_year_decade") || colourmode.equals("decade"))) {
                    TreeSet<Integer> decades = new TreeSet<Integer>();
                    for (double d : ((BiocacheLegendObject) lo).categoriesNumeric.keySet()) {
                        decades.add((int) (d / 10));
                    }
                    ArrayList<Integer> d = new ArrayList<Integer>(decades);

                    StringBuilder sb = new StringBuilder();
                    sb.append("occurrence_year");
                    for (int i = (d.size() > 0 && d.get(0) > 0 ? 0 : 1); i < d.size(); i++) {
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
                logger.error("error getting legend for : " + colourmode, e);
            }
        }

        return lo;
    }

    @Override
    public Query newFacets(List<Facet> facets, boolean forMapping) {
        if ((facets == null || facets.isEmpty()) && (this.forMapping || !forMapping)) {
            return this;
        }
        ArrayList<Facet> newFacets = new ArrayList<Facet>();
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

    List<Double> bbox = null;

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
            int result = client.executeMethod(get);
            String[] s = get.getResponseBodyAsString().split(",");
            for (int i = 0; i < 4; i++) {
                bbox.add(Double.parseDouble(s[i]));
            }
        } catch (Exception e) {
            //default to 'world' bb
            SimpleRegion sr = SimpleShapeFile.parseWKT(CommonData.WORLD_WKT);
            bbox.clear();
            bbox.add(Math.max(-180, sr.getBoundingBox()[0][0]));
            bbox.add(Math.max(-90, sr.getBoundingBox()[0][1]));
            bbox.add(Math.min(180, sr.getBoundingBox()[1][0]));
            bbox.add(Math.min(90, sr.getBoundingBox()[1][1]));

            logger.error("error getting species layer bounding box from biocache:" + url, e);
        }

        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        String spname = getSolrName();

        String lastClass = "md_grey-bg";

        String html = "Species layer\n";
        html += "<table class='md_table'>";
        html += "<tr class='" + lastClass + "'><td class='md_th'>Species name: </td><td class='md_spacer'/><td class='md_value'>" + spname + "</td></tr>";
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        boolean[] gk = getGeospatialKosher();
        if (gk == null) {
            html += "<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>" + getSpeciesCount() + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            html += "<tr class='" + lastClass + "'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>" + getOccurrenceCount() + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        } else {
            if (wkt != null && !wkt.equals(CommonData.WORLD_WKT)) {
                html += "<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>" + getSpeciesCountKosher() + " without a flagged spatial issue<br>" + getSpeciesCountCoordinates() + " with any coordinates</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>" + getOccurrenceCountKosher() + " without a flagged spatial issue<br>" + getOccurrenceCountCoordinates() + " with any coordinates</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of endemic species: </td><td class='md_spacer'/><td class='md_value'>" + getEndemicSpeciesCount() + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            } else {
                html += "<tr class='md_grey-bg'><td class='md_th'>Number of species: </td><td class='md_spacer'/><td class='md_value'>" + getSpeciesCountKosher() + " without a flagged spatial issue<br>" + getSpeciesCountCoordinates() + " with any coordinates<br>" + getSpeciesCountAny() + " total including records without coordinates</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of occurrences: </td><td class='md_spacer'/><td class='md_value'>" + getOccurrenceCountKosher() + " without a flagged spatial issue<br>" + getOccurrenceCountCoordinates() + " with any coordinates<br>" + getOccurrenceCountAny() + " total including records without coordinates</td></tr>";
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
                html += "<tr class='" + lastClass + "'><td class='md_th'></td><td class='md_spacer'/><td class='md_value'>" + msg + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
        }
        if (lsids != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Classification: </td><td class='md_spacer'/><td class='md_value'>";
            for (String s : lsids.split(",")) {
                Map<String, String> classification = getSpeciesClassification(s);
                Iterator<String> it = classification.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    String value = classification.get(key);
                    html += "<a href='" + CommonData.bieServer + BIE_SPECIES + value + "' target='_blank'>" + key + "</a> ";
                    if (it.hasNext()) {
                        html += " > ";
                    }
                }

                html += "<br />";
                html += "More information for <a href='" + CommonData.bieServer + BIE_SPECIES + s + "' target='_blank'>" + getScientificNameRank(s).split(",")[0] + "</a>";
                html += "<br />";
                html += "<br />";
            }
            html += "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }

        String dataProviders = StringUtils.trimToNull(getDataProviders());
        if (dataProviders != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Data providers: </td><td class='md_spacer'/><td class='md_value'>" + getDataProviders() + "</td></tr>";
        }
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        if (lsids != null && lsids.length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>List of LSIDs: </td><td class='md_spacer'/><td class='md_value'>" + lsids + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

            String[] wms = CommonData.getSpeciesDistributionWMS(lsids);
            if (wms != null && wms.length > 0) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Expert distributions</td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openDistributions(\"" + lsids + "\")'>" + wms.length + "</a></td><td class='md_spacer'/><td class='md_value'></td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }

            wms = CommonData.getSpeciesChecklistWMS(lsids);
            if (wms != null && wms.length > 0) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Checklist species</td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openChecklists(\"" + lsids + "\")'>" + wms.length + "</a></td><td class='md_spacer'/><td class='md_value'></td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
        }

        html += "<tr class='" + lastClass + "'><td class='md_th' colspan=3><a href='" + biocacheWebServer + "/occurrences/search?q=" + getQ() + "' target='_blank'>Table view of these records</a></td><td class='md_spacer'/><td class='md_value'></td></tr>";
        html += "</table>";

        return html;
    }

    private Map<String, String> getSpeciesClassification(String lsid) {

        String[] classificationList = {"kingdom", "phylum", "class", "order", "family", "genus", "species", "subspecies"};
        Map<String, String> classification = new LinkedHashMap<String, String>();

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        logger.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            //String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            JSONObject joOcc = jo.getJSONObject("classification");
            for (String c : classificationList) {
                if (c.equals(rank)) {
                    break;
                }
                classification.put(joOcc.getString(c.replace("ss", "zz")), joOcc.getString(c.replace("ss", "zz") + "Guid"));
            }

        } catch (Exception e) {
            logger.error("error getting scientific name at: " + snUrl, e);
        }

        return classification;
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
        String url = CommonData.bieServer + "/ws/guid/" + name.replaceAll(" ", "%20");
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String body = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(body);
            if (ja != null && ja.size() > 0) {
                JSONObject jo = ja.getJSONObject(0);
                if (jo != null && jo.has("acceptedIdentifier"))
                    return jo.getString("acceptedIdentifier");
                else
                    return null;
            } else
                return null;
        } catch (Exception e) {
            logger.error("error getting guid at: " + url, e);
            return null;
        }
    }

    /**
     * Retrieves the classification information from the BIE for the supplied GUID.
     *
     * @param lsid
     * @return
     */
    public static Map<String, String> getClassification(String lsid) {

        String[] classificationList = {"kingdom", "phylum", "class", "order", "family", "genus", "species", "subspecies", "scientificName"};
        Map<String, String> classification = new LinkedHashMap<String, String>();

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        logger.debug(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);

            JSONObject joOcc = jo.getJSONObject("classification");
            for (String c : classificationList) {
                //NC stop exception where a rank can't be found
                String value = joOcc.optString(c.replace("ss", "zz"), null);
                if (value != null)
                    classification.put(c.replace("ss", "zz"), value);
            }

        } catch (Exception e) {
            logger.debug("Error getting scientific name for: " + lsid);
            //e.printStackTrace(System.out);
        }

        return classification;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("&extra=").append(CommonData.extraDownloadFields);
        if (extraFields != null && extraFields.length > 0) {
            for (int i = 0; i < extraFields.length; i++) {
                //Solr download has some default fields
                // these include the 'translate' fields
                // remove them from extraFields
                if (translateFieldForSolr(extraFields[i]) != null
                        && !extraFields[i].equals(translateFieldForSolr(extraFields[i]))) {
                    continue;
                }

                if (sb.length() == 0) {
                    //sb.append("&extra=").append(extraFields[i]);
                } else {
                    sb.append(",").append(extraFields[i]);
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
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {

                String html = "";

                JSONArray ja = JSONArray.fromObject(response);
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    html += "<a href='http://collections.ala.org.au/public/showDataProvider/" + jo.getString("id") + "' target='_blank'>" + jo.getString("name") + "</a>: " + jo.getString("count") + " records <br />";
                }
                //return response;

                return html;
            }
        } catch (Exception e) {
            logger.error("error getting query data providers for html:" + url, e);
        }

        return null;
    }

    //LSID match """lsid:"?([a-z0-9\:]*)"?""".r
    Pattern lsidPattern = Pattern.compile("(?:lsid:)\"?([a-z0-9\\:\\.\\-]*)\"?");

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
        logger.debug("Retrieving query metadata: " + url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if (result == 200) {

                JSONObject jo = JSONObject.fromObject(response);

                if (jo.containsKey("queryTitle")) {
                    String title = jo.getString("queryTitle");

                    //clean default parameter
                    title = title.replace(" AND <span>null</span>", "");
                    title = title.replace(" AND null", "");

                    //clean spans
                    int p1 = title.indexOf("<span");
                    while (p1 >= 0) {
                        int p2 = title.indexOf(">", p1);
                        int p3 = title.indexOf("</span>", p2);
                        title = title.substring(0, p1) + title.substring(p2 + 1, p3)
                                + (p3 + 7 < title.length() ? title.substring(p3 + 7, title.length()) : "");

                        p1 = title.indexOf("<span");
                    }

                    solrName = title;
                    logger.debug("solrName12=" + solrName);
                    return solrName;
                }
            }
        } catch (Exception e) {
            logger.error("error getting solr name for a query: " + url, e);
        }

        return null;
    }

    @Override
    public String getQc() {
        return qc;
    }

    @Override
    public void setQc(String qc) {
        this.qc = qc;
    }

    public String getBS() {
        return biocacheServer;
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
        ArrayList<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        newFacets.add(Facet.parseFacet(sb.toString()));
        return new BiocacheQuery(lsids, rawNames, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }
}
