/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import au.com.bytecode.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.sf.json.JSONArray;
import org.ala.spatial.exception.NoSpeciesFoundException;
import org.ala.spatial.sampling.SimpleRegion;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Adam
 */
public class BiocacheQuery implements Query, Serializable {

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
    static final String DEFAULT_ROWS = "pageSize=1000000";
    static final String DEFAULT_ROWS_LARGEST = "pageSize=100000000";
    /** DEFAULT_VALIDATION must not be null */
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
    int speciesCount = -1;
    int occurrenceCount = -1;
    double[] points = null;
    String solrName = null;
    //static String[][] facetNameExceptions = {{"cl22", "state"}, {"cl959", "places"}, {"cl20", "ibra"}, {"cl21", "imcra"}};
    HashMap<String, LegendObject> legends = new HashMap<String, LegendObject>();
    HashSet<String> flaggedRecords = new HashSet<String>();

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
        if("occurrence_year_individual".equals(facetName)) {
            facetName = "occurrence_year";
        }
        if("occurrence_year_decade".equals(facetName)) {
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
        this.lsids = lsids;
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
        this.lsids = lsids;
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
            new Exception("Attempted to add a geospatial_kosher facet to an unsupported query: '" + lsids + "', '" + extraParams + "'").printStackTrace();
            return newFacet(null, forMapping);
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

        return new BiocacheQuery(lsids, wkt, extraParams, newFacets, forMapping, geospatialKosher, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
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

        return new BiocacheQuery(lsids, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }

    /**
     * Restrict to an area.
     *
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
                return new BiocacheQuery(lsids, wkt, extraParams, facets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
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

            sq = new BiocacheQuery(lsids, newWkt, extraParams, facets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
        } catch (Exception e) {
            e.printStackTrace();
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
        System.out.println(url);
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
            e.printStackTrace();
        }
        System.out.println("get sample in " + (System.currentTimeMillis() - start) + "ms");

        return sample;
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
        System.out.println(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            speciesList = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return speciesList;
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
        System.out.println(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            String start = "\"totalRecords\":";
            String end = ",";
            int startPos = response.indexOf(start) + start.length();

            occurrenceCount = Integer.parseInt(response.substring(startPos, response.indexOf(end, startPos)));
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Get parsed coordinates and optional fields for this query.
     *
     * @param fields QueryFields to return in the sample as ArrayList<QueryField>.
     * If a QueryField isStored() it will be populated with the field data.
     * @return coordinates as double [] like [lng, lat, lng, lat, ...].
     */
    @Override
    public double[] getPoints(ArrayList<QueryField> fields) throws NoSpeciesFoundException {
        //if no additional fields requested, return points only
        if (fields == null && points != null) {
            return points;
        }

        if (fields == null) {
            fields = new ArrayList<QueryField>();
        }
        fields.add(new QueryField("longitude"));
        fields.add(new QueryField("latitude"));

        String sample = sample(fields);

        long start = System.currentTimeMillis();

        int lineCount = -1; //header count offset
        int pos = -1;
        while ((pos = sample.indexOf('\n', pos + 1)) >= 0) {
            lineCount++;
        }
        System.out.println("sampled records count: " + lineCount);

        if (lineCount == -1) {
            throw new NoSpeciesFoundException();
        }


        CSVReader csv = new CSVReader(new StringReader(sample));

        //process header
        int[] fieldsToCsv = new int[fields.size()];
        for (int i = 0; i < fieldsToCsv.length; i++) {
            fieldsToCsv[i] = -1;
        }
        String[] line = {};
        try {
            line = csv.readNext();
        } catch (IOException ex) {
            Logger.getLogger(BiocacheQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 0; i < line.length; i++) {
            if (line[i] != null && i < fieldsToCsv.length) {
                for (int j = 0; j < fields.size(); j++) {
                    if (fields.get(j).getName().equals(line[i])) {
                        fieldsToCsv[j] = i;
                        break;
                    }
                }
            }
        }
        int longitudePos = fieldsToCsv[fieldsToCsv.length - 2];
        int latitudePos = fieldsToCsv[fieldsToCsv.length - 1];

        //process records
        points = new double[lineCount * 2];
        int errCount = 0;
        pos = 0;
        try {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isStored()) {
                    fields.get(i).ensureCapacity(lineCount);
                }
            }

            while ((line = csv.readNext()) != null) {
                boolean ok = false;
                try {
                    points[pos] = Double.parseDouble(line[longitudePos]);
                    points[pos + 1] = Double.parseDouble(line[latitudePos]);
                    pos += 2;
                    ok = true;
                } catch (Exception e) {
                    errCount++;
                }
                if (ok) {
                    for (int i = 0; i < fields.size(); i++) {
                        if (fields.get(i).isStored()) {
                            try {
                                fields.get(i).add(line[fieldsToCsv[i]]);
                            } catch (Exception ex) {
                                fields.get(i).add("");
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isStored()) {
                    long st = System.currentTimeMillis();
                    fields.get(i).store();
                    System.out.println(fields.get(i).getDisplayName() + " stored in " + (System.currentTimeMillis() - st) + "ms");
                }
            }

            csv.close();
        } catch (IOException ex) {
            Logger.getLogger(BiocacheQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (points.length != pos) {
            double[] pointsCopy = new double[pos];
            System.arraycopy(points, 0, pointsCopy, 0, pos);
            points = pointsCopy;
            System.out.println("pointsCopy, errCount=" + errCount);
        }

        System.out.println("filled getPoints in " + (System.currentTimeMillis() - start) + "ms");

        return points;
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
                if (facet.contains(" OR ")) {
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
                e.printStackTrace();
            }
        }

        //wkt term
        if (wkt != null) {
            sb.append("&wkt=" + wkt.replace(" ", ":"));
        }

        try {
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
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
            String[] qs = getFullQ(false).split("&");
            for (int i = 0; i < qs.length; i++) {
                String q = qs[i];
                int p = q.indexOf('=');
                if (p < 0) {
                    post.addParameter("q", q.substring(p + 1));
                    System.out.println("param: " + "q" + " : " + q.substring(p + 1));
                } else {
                    post.addParameter(q.substring(0, p), q.substring(p + 1));
                    System.out.println("param: " + q.substring(0, p) + " : " + q.substring(p + 1));
                }
                post.addParameter("bbox", forMapping ? "true" : "false");
            }
            int result = client.executeMethod(post);
            String response = post.getResponseBodyAsString();

            if (result == 200) {
                paramId = response;

                System.out.println(url + " > " + paramId);
            } else {
                System.out.println("error with url:" + url + " posting q: " + getQ() + " > response_code:" + result + " response:" + response);
            }
        } catch (Exception e) {
            System.out.println("error with url:" + url + " posting q: " + getQ());
            e.printStackTrace();
        }
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
//        System.out.println("total number of occurrences: " + sq.getOccurrenceCount());
//        System.out.println("total number of species: " + sq.getSpeciesCount());
//
//        //Repeat in a polygon
//        String wkt = "POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))";
//        sq.addWkt(wkt);
//        System.out.println("wkt: " + wkt);
//        System.out.println("number of occurrences in wkt: " + sq.getOccurrenceCount());
//        System.out.println("number of species in wkt: " + sq.getSpeciesCount());
//
//        //Sample some points
//        ArrayList<QueryField> fields = new ArrayList<QueryField>();
//        fields.add(new QueryField("id", "uuid"));
//        String sample = sq.sample(fields);
//        System.out.println("sample:");
//        System.out.println(sample.substring(0, 500) + ((sample.length() <= 500) ? "" : "..."));
//
//        //Sample some coordinates
//        double[] coordinates = sq.getPoints(null);
//        System.out.println("coordinates:");
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
        System.out.println(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);
            String scientficName = jo.getJSONObject("taxonConcept").getString("nameString");
            String rank = jo.getJSONObject("taxonConcept").getString("rankString");

            System.out.println("Arrays.binarySearch(commonTaxonRanks, rank): " + Arrays.binarySearch(commonTaxonRanks, rank));
            if (Arrays.binarySearch(commonTaxonRanks, rank) > -1) {
                rank = "taxon";
            }

            return scientficName + "," + rank;
        } catch (Exception e) {
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
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
            e.printStackTrace();
        }
        gzipped.close();

        return s;
    }

    @Override
    public String getWMSpath() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        try {
            final String jsonUri = biocacheServer + "/upload/dynamicFacets?q=" +getFullQ(true) + "&qc="+getQc();
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(jsonUri);
            get.addRequestHeader("Content-type", "application/json");
            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONArray ja = JSONArray.fromObject(slist);

            for(Object arrayElement: ja){
                JSONObject jsonObject = (JSONObject) arrayElement;
                String facetName = jsonObject.getString("name");
                String facetDisplayName = jsonObject.getString("displayName");

                System.out.println("Adding custom index : " + arrayElement);
                customFacets.add(new QueryField(facetName, facetDisplayName, QueryField.FieldType.STRING));
            }
        } catch (Exception e){
            //System.err.println("Unable to load custom facets for : " + dr);
            e.printStackTrace();
        }
        return customFacets;
    }

    @Override
    public ArrayList<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            if(supportsDynamicFacets){
                fields.addAll(retrieveCustomFacets());
            }
            // Taxonomic
            fields.add(new QueryField("taxon_name", "Scientific name", QueryField.FieldType.STRING));
            fields.add(new QueryField("raw_taxon_name", "Scientific name (unprocessed)", QueryField.FieldType.STRING));
            fields.add(new QueryField("subspecies_name", "Subspecies", QueryField.FieldType.STRING));
            //fields.add(new QueryField("species", "Species", QueryField.FieldType.STRING));
            fields.add(new QueryField("genus", "Genus", QueryField.FieldType.STRING));
            fields.add(new QueryField("family", "Family", QueryField.FieldType.STRING));
            fields.add(new QueryField("order", "Order", QueryField.FieldType.STRING));
            fields.add(new QueryField("class", "Class", QueryField.FieldType.STRING));
            fields.add(new QueryField("phylum", "Phylum", QueryField.FieldType.STRING));
            fields.add(new QueryField("kingdom", "Kingdom", QueryField.FieldType.STRING));
            fields.add(new QueryField("species_group", "Lifeform", QueryField.FieldType.STRING));
            fields.add(new QueryField("rank", "Identified to rank", QueryField.FieldType.STRING));
            fields.add(new QueryField("interaction", "Species interaction", QueryField.FieldType.INT));
            // Geospatial
            fields.add(new QueryField("coordinate_uncertainty", "Spatial uncertainty (metres)", QueryField.FieldType.INT));
            fields.add(new QueryField("sensitive", "Sensitive", QueryField.FieldType.STRING));
            fields.add(new QueryField("state_conservation", "State conservation status", QueryField.FieldType.STRING));
            fields.add(new QueryField("raw_state_conservation", "State conservation (unprocessed)", QueryField.FieldType.STRING));
            fields.add(new QueryField("places", "LGA boundaries", QueryField.FieldType.STRING));
            fields.add(new QueryField("state", "Australian States and Territories", QueryField.FieldType.STRING));
            fields.add(new QueryField("country", "Country boundaries", QueryField.FieldType.STRING));
            fields.add(new QueryField("ibra", "IBRA regions", QueryField.FieldType.STRING));
            fields.add(new QueryField("imcra", "IMCRA regions", QueryField.FieldType.STRING));
            fields.add(new QueryField("cl918", "Dynamic land cover", QueryField.FieldType.STRING));
            fields.add(new QueryField("cl617", "Vegetation types - native", QueryField.FieldType.STRING));
            fields.add(new QueryField("cl620", "Vegetation types - present", QueryField.FieldType.STRING));
            //fields.add(new QueryField("geospatial_kosher", "Location Quality", QueryField.FieldType.STRING));
            // Temporal
            fields.add(new QueryField("month", "Month", QueryField.FieldType.STRING));
            fields.add(new QueryField("occurrence_year", "Period (by equal counts)", QueryField.FieldType.INT));
            fields.add(new QueryField("year", "Year (by highest counts)", QueryField.FieldType.STRING));
            fields.add(new QueryField("occurrence_year_decade", "Decade", QueryField.FieldType.STRING));
            // Record details
            fields.add(new QueryField("basis_of_record", "Record type", QueryField.FieldType.STRING));
            fields.add(new QueryField("type_status", "Specimen type", QueryField.FieldType.STRING));
            fields.add(new QueryField("multimedia", "Multimedia", QueryField.FieldType.STRING));
            fields.add(new QueryField("collector", "Collector", QueryField.FieldType.STRING));
            // Attribution
            fields.add(new QueryField("data_resource", "Dataset", QueryField.FieldType.STRING));
            fields.add(new QueryField("data_provider", "Data provider", QueryField.FieldType.STRING));
            fields.add(new QueryField("collection_name", "Collection", QueryField.FieldType.STRING));
            //fields.add(new QueryField("collection_uid", "Collection", QueryField.FieldType.STRING));
            fields.add(new QueryField("institution_name", "Institution", QueryField.FieldType.STRING));
            //fields.add(new QueryField("institution_code_name", "Institution", QueryField.FieldType.STRING));
            //fields.add(new QueryField("institution_uid", "Institution", QueryField.FieldType.STRING));
            // Record Assertions
            fields.add(new QueryField("assertions", "Record Issues", QueryField.FieldType.STRING));
            fields.add(new QueryField("outlier_layer", "Outlier for layer", QueryField.FieldType.STRING));
            fields.add(new QueryField("outlier_layer_count", "Outlier layer count", QueryField.FieldType.STRING));
            fields.add(new QueryField("taxonomic_issue", "Taxon identification issue", QueryField.FieldType.STRING));
            fields.add(new QueryField("establishment_means", "Cultivation status", QueryField.FieldType.STRING));            
            fields.add(new QueryField("duplicate_status", "Duplicate status", QueryField.FieldType.STRING));            

            //fields.add(new QueryField("biogeographic_region", "Biogeographic Region", QueryField.FieldType.STRING));
            //fields.add(new QueryField("species_guid", "Species", QueryField.FieldType.STRING));
            //fields.add(new QueryField("interaction", "Species Interaction", QueryField.FieldType.INT));

            //fields.add(new QueryField("cl678", "Land use", QueryField.FieldType.STRING));
            //fields.add(new QueryField("cl619", "Vegetation - condition", QueryField.FieldType.STRING));
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
            System.out.println("lo not empty and lo=" + lo);
            lo = legends.get(lo.getColourMode());
        }
        if (lo == null) {
            HttpClient client = new HttpClient();
            String facetToColourBy = colourmode.equals("occurrence_year_decade")?"occurrence_year":translateFieldForSolr(colourmode);

            try {
                String url = biocacheServer
                        + LEGEND_SERVICE_CSV
                        + DEFAULT_ROWS
                        + "&q=" + getQ()
                        + "&cm=" + URLEncoder.encode(facetToColourBy, "UTF-8")
                        + getQc();
                System.out.println(url);
                GetMethod get = new GetMethod(url);

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
                if (!colourmode.contains(",") && (colourmode.equals("occurrence_year") || colourmode.equals("coordinate_uncertainty"))) {
                    lo = ((BiocacheLegendObject) lo).getAsIntegerLegend();

                    //apply cutpoints to colourMode string
                    Legend l = lo.getNumericLegend();
                    double[] minmax = l.getMinMax();
                    double[] cutpoints = l.getCutoffdoubles();
                    double[] cutpointmins = l.getCutoffMindoubles();
                    StringBuilder sb = new StringBuilder();
                    sb.append(colourmode);
                    int i = 0;
                    int lasti = 0;
                    while (i < cutpoints.length) {
                        if (i == cutpoints.length - 1 || cutpoints[i] != cutpoints[i + 1]) {
                            if (i > 0) {
                                sb.append(",").append(cutpointmins[i]);
                                if(colourmode.equals("occurrence_year"))
                                    sb.append("-01-01T00:00:00Z");
                            } else {
                                sb.append(",*");
                            }
                            sb.append(",").append(cutpoints[i]);
                            if(colourmode.equals("occurrence_year"))
                                    sb.append("-12-31T00:00:00Z");
                            lasti = i;
                        }
                        i++;
                    }
                    String newColourMode = sb.toString();
                    if(colourmode.equals("occurrence_year")) {
                        newColourMode = newColourMode.replace(".0","");
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
                } else if (!colourmode.contains(",") && colourmode.equals("occurrence_year_decade")) {
                    TreeSet<Integer> decades = new TreeSet<Integer>();
                    for(double d : ((BiocacheLegendObject) lo).categoriesNumeric.keySet()) {
                        decades.add((int)(d/10));
                    }
                    ArrayList<Integer> d = new ArrayList<Integer>(decades);

                    StringBuilder sb = new StringBuilder();
                    sb.append("occurrence_year");
                    for(int i=(d.size()>0&&d.get(0)>0?0:1);i<d.size();i++) {
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
                e.printStackTrace();
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

        return new BiocacheQuery(lsids, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
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

        System.out.println(url);

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

            e.printStackTrace();
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
        if(dataProviders != null){
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
        System.out.println(snUrl);

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
            System.out.println("Error getting scientific name");
            e.printStackTrace(System.out);
        }

        return classification;
    }

    public static Map<String, String> getClassification(String lsid) {

        String[] classificationList = {"kingdom", "phylum", "class", "order", "family", "genus", "species", "subspecies", "scientificName"};
        Map<String, String> classification = new LinkedHashMap<String, String>();

        String snUrl = CommonData.bieServer + BIE_SPECIES + lsid + ".json";
        System.out.println(snUrl);

        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(snUrl);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            JSONObject jo = JSONObject.fromObject(slist);

            JSONObject joOcc = jo.getJSONObject("classification");
            for (String c : classificationList) {
                classification.put(c.replace("ss", "zz"), joOcc.getString(c.replace("ss", "zz")));
            }

        } catch (Exception e) {
            System.out.println("Error getting scientific name for: " + lsid);
            //e.printStackTrace(System.out);
        }

        return classification;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("&extra=").append("coordinateUncertaintyInMeters");
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

    @Override
    public byte[] getDownloadBytes(String[] extraFields, String[] displayNames) {
        return null;
    }

    private String getDataProviders() {
        HttpClient client = new HttpClient();
        String url = biocacheServer
                + DATA_PROVIDERS_SERVICE
                + DEFAULT_ROWS
                + "&q=" + getQ()
                + getQc();
        System.out.println(url);
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
            e.printStackTrace();
        }

        return null;
    }

    public String getLsids() {
        return lsids;
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
        System.out.println("Retrieving query metadata: " + url);
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
                    System.out.println("solrName12=" + solrName);
                    return solrName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        return new BiocacheQuery(lsids, wkt, extraParams, newFacets, forMapping, null, biocacheServer, biocacheWebServer, this.supportsDynamicFacets);
    }
}
