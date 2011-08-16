/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 * @author Adam
 */
public class SolrQuery {

    static public final String BIOCACHE_URL = "http://localhost:8083/biocache-service";
    static public final String SAMPLING_SERVICE = "/webportal/occurrences?";
    static public final String SPECIES_LIST_SERVICE = "/webportal/species?";
    static public final String POST_SERVICE = "/webportal/params?";
    static public final String DEFAULT_ROWS = "pageSize=10000";
    static public final String DEFAULT_VALIDATION = "";
    static public final String BIE_SPECIES = "http://bie.ala.org.au/species/";

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
    ArrayList<String> lsids;
    ArrayList<Facet> facets;
    String wkt;
    String extraParams;

    //query param id
    String paramId;

    public SolrQuery() {
        lsids = new ArrayList<String>();
        facets = new ArrayList<Facet>();
        wkt = null;
        extraParams = null;

        paramId = null;
    }

    public SolrQuery(String lsid, String wkt, String extraParams) {
        lsids = new ArrayList<String>();
        if(lsid != null) {
            lsids.add(lsid);
        }

        facets = new ArrayList<Facet>();

        if(wkt != null) {
            this.wkt = wkt.replace(" ", ":");
        }

        this.extraParams = extraParams;

        paramId = null;
    }

    void resetOutput() {
        paramId = null;
    }

    /**
     * Restrict records by a list of species.
     *
     * @param lsids
     */
    public void addLsid(String lsid) {
        this.lsids.add(lsid);

        resetOutput();
    }

    /**
     * Restrict records by field values.
     *
     * @param field
     * @param values
     */
    public void addFacet(Facet facet) {
        facets.add(facet);

        resetOutput();
    }

    /**
     * Restrict to an area.
     *
     * @param wkt
     */
    public void addWkt(String wkt) {
        this.wkt = wkt;

        resetOutput();
    }

    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in JSON format.
     */
    public String sample(ArrayList<QueryField> fields) {
        HttpClient client = new HttpClient();
        String url = BIOCACHE_URL
                + SAMPLING_SERVICE
                + "&" + DEFAULT_ROWS
                + DEFAULT_VALIDATION
                + "&" + toString()
                + paramQueryFields(fields)
                + "&facets=";
        System.out.println(url);
        GetMethod get = new GetMethod(url);

        String sample = null;

        try {
            int result = client.executeMethod(get);
            sample = get.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sample;
    }

    /**
     * Get species list for this query.
     *
     * @return species list as String containing JSON array.
     */
    public String speciesList() {
        HttpClient client = new HttpClient();
        String url = BIOCACHE_URL
                + SPECIES_LIST_SERVICE
                + "&" + DEFAULT_ROWS
                + DEFAULT_VALIDATION
                + "&" + toString();
        System.out.println(url);
        GetMethod get = new GetMethod(url);

        String speciesList = null;

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
    public int getOccurrenceCount() {
        int occurrenceCount = -1;

        HttpClient client = new HttpClient();
        String url = BIOCACHE_URL
                + SAMPLING_SERVICE
                + "pageSize=0"
                + DEFAULT_VALIDATION
                + "&" + toString()
                + "&facets=";
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
    public int getSpeciesCount() {
        int speciesCount = -1;

        HttpClient client = new HttpClient();
        String url = BIOCACHE_URL
                + SPECIES_LIST_SERVICE
                + DEFAULT_ROWS
                + DEFAULT_VALIDATION
                + "&" + toString();
        System.out.println(url);
        GetMethod get = new GetMethod(url);

        try {
            int result = client.executeMethod(get);
            String response = get.getResponseBodyAsString();

            if(result == 200) {
                speciesCount = 0;
                int pos = 0;
                while ((pos = response.indexOf('{', pos + 1)) >= 0) {
                    speciesCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return speciesCount;
    }

    /**
     * Convert JSON array to CSV.
     *
     * Use for converting output of sample and species list to csv.
     *
     * @param json JSON array as String.
     * @return JSON array in CSV format as String.
     */
    static public String convertJSONArrayObjectstoCSV(String json) {
        JSONArray ja = null;

        //parse for species list
        try {
            ja = JSONArray.fromObject(json);
        } catch (Exception e) {

        }

        //parse for sampling (if not species list)
        if(ja == null) {
            try {
                ja = JSONObject.fromObject(json).getJSONArray("occurrences");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        StringBuilder sb = null;
        String[] columns = getJSONColumns(ja);

        if (columns != null) {
            sb = new StringBuilder();

            for (int j = 0; j < columns.length; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(columns[j].replace("\"", "\"\"")).append("\"");
            }

            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = ja.getJSONObject(i);

                sb.append("\n");

                for (int j = 0; j < columns.length; j++) {
                    if (j > 0) {
                        sb.append(",");
                    }
                    if(jo.has(columns[j]) && jo.getString(columns[j]) != null) {
                        sb.append("\"").append(jo.getString(columns[j]).replace("\"", "\"\"")).append("\"");
                    } else {
                        sb.append("\"\"");
                    }
                }
            }
        }

        return sb.toString();
    }

    static String[] getJSONColumns(JSONArray ja) {
        String[] keys = null;
        if (ja != null && ja.size() > 0) {
            HashSet<String> hs = new HashSet<String>();

            for (int i = 0; i < ja.size(); i++) {
                hs.addAll(ja.getJSONObject(i).keySet());
            }

            keys = new String[hs.size()];
            hs.toArray(keys);
        }

        return keys;
    }

    /**
     * Get parsed coordinates and optional fields for this query.
     *
     * @param fields QueryFields to return in the sample as ArrayList<QueryField>.
     * If a QueryField isStored() it will be populated with the field data.
     * @return coordinates as double [] like [lng, lat, lng, lat, ...].
     */
    public double[] getPoints(ArrayList<QueryField> fields) {
        if (fields == null) {
            fields = new ArrayList<QueryField>();
        }
        fields.add(new QueryField("longitude", "decimalLongitude"));
        fields.add(new QueryField("latitude","decimalLatitude"));

        String sample = sample(fields);
        System.out.println("sample.substring(0,500):" + ((sample.length() < 500)?sample:sample.substring(0,500) + "..."));

        long start = System.currentTimeMillis();
        JSONArray ja = JSONObject.fromObject(sample).getJSONArray("occurrences");
        System.out.println("parse sample.length=" + sample.length() + " in " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        double[] points = null;

        int errCount = 0;
        if (ja != null) {
            points = new double[ja.size() * 2];

            int pos = 0;
            boolean ok = false;
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                ok = false;
                try {                    
                    points[pos] = Double.parseDouble(jo.getString("decimalLongitude"));
                    points[pos + 1] = Double.parseDouble(jo.getString("decimalLatitude"));                    

                    pos += 2;
                    ok = true;
                } catch (Exception e) {
                    if(errCount < 10) {
                        System.out.print("err(" + jo.getString("decimalLongitude") + "," + jo.getString("decimalLatitude") + ")");
                    }
                    if(errCount < 1) {
                        e.printStackTrace();
                    }
                    errCount++;
                }
                if(ok) {
                    for (int e = 0; e < fields.size(); e++) {
                        if (fields.get(e).isStored()) {
                            try {
                                fields.get(e).add(jo.getString(fields.get(e).getDisplayName()));
                            } catch (Exception ex) {
                                fields.get(e).add("");
                            }
                        }
                    }
                }
            }

            if (points.length != pos) {
                double[] pointsCopy = new double[pos];
                System.arraycopy(points, 0, pointsCopy, 0, pos);
                points = pointsCopy;
                System.out.println("pointsCopy, errCount=" + errCount);
            }
        }
        System.out.println("fill getPoints in " + (System.currentTimeMillis() - start) + "ms");

        return points;
    }

    String paramQueryFields(ArrayList<QueryField> fields) {
        StringBuilder sb = new StringBuilder();

        sb.append("&fl=");

        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(fields.get(i).getName());
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int queryTerms = 0;
        for (int i = 0; i < lsids.size(); i++) {
            if(lsids.get(i).indexOf("paramId") < 0) {
                if (queryTerms > 0) {
                    sb.append(" OR ");
                } else {
                    sb.append("q=( ");
                }
                //sb.append("lsid:").append(lsids.get(i));
                sb.append("kingdom:Animalia");
                queryTerms++;
            }
        }
        if(queryTerms > 0) {
            sb.append(" )");
        } else if (facets.size() == 0){
            sb.append("q=*:*");
            queryTerms++;
        }

        if (facets.size() > 0) {
            for (int i = 0; i < facets.size(); i++) {
                if(queryTerms > 0) {
                    sb.append(" AND ");
                }
                sb.append(facets.get(i).toString());
                queryTerms++;
            }
        }

        //wkt term
        if (wkt != null) {
            sb.append("&wkt=" + wkt);
        }

        //paramId term, there should never be more than one of these.
        int count = 0;
        for (int i = 0; i < lsids.size(); i++) {
            if(lsids.get(i).indexOf("paramId") >= 0) {
                if (count > 0 || wkt != null || queryTerms > 0) {
                    sb.append("&");
                }
                sb.append(lsids.get(i).replace(":","="));
                count++;
            }
        }

        //extra
        if(extraParams != null) {
            sb.append("&").append(extraParams);
        }

        return sb.toString();
    }


    /**
     * Get a short query string.
     *
     * @return a short query term as String, or null on error.
     */
    public String getShortQuery() {
        if (paramId == null) {
            HttpClient client = new HttpClient();
            String url = BIOCACHE_URL
                    + POST_SERVICE
                    + DEFAULT_VALIDATION
                    + "&" + toString();
            PostMethod post = new PostMethod(url);

            try {
                int result = client.executeMethod(post);
                String response = post.getResponseBodyAsString();

                if(result == 200) {
                    paramId = "paramId:" + response;

                    System.out.println(url + ", " + paramId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return paramId;
    }

    public String getName() {
        String name = null;

        if(lsids.size() == 0) {
            name = "All species";
        } else if(lsids.size() == 1){
            name = getScientificNameRank(lsids.get(0)).split(",")[0];
        } else {
            name = "Multiple species";
        }

        return name;
    }

    public String getRank() {
        String rank = null;

        if(lsids.size() == 1){
            rank = getScientificNameRank(lsids.get(0)).split(",")[1];

            if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
                rank = "taxon";
            }
        } else {
            rank = "species";
        }

        return rank;
    }

    /**
     * test
     *
     * @param args
     */
    static public void main(String [] args) {
        SolrQuery sq = new SolrQuery();

        //count all occurrences and species
        System.out.println("total number of occurrences: " + sq.getOccurrenceCount());
        System.out.println("total number of species: " +  sq.getSpeciesCount());

        //Repeat in a polygon
        String wkt = "POLYGON((140:-37,151:-37,151:-26,140.1310:-26,140:-37))";
        sq.addWkt(wkt);
        System.out.println("wkt: " + wkt);
        System.out.println("number of occurrences in wkt: " + sq.getOccurrenceCount());
        System.out.println("number of species in wkt: " + sq.getSpeciesCount());

        //Sample some points
        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        fields.add(new QueryField("id", "uuid"));
        String sample = sq.sample(fields);
        System.out.println("sample:");
        System.out.println(sample.substring(0,500) + ((sample.length() <= 500) ? "": "..."));

        //Sample some coordinates
        double [] coordinates = sq.getPoints(null);
        System.out.println("coordinates:");
        for(int i=0;coordinates != null && i<coordinates.length && i < 100;i+=2) {
            System.out.print(coordinates[0] + "," + coordinates[1] + " ");
        }

    }

    public String getSingleLsid() {
        return (lsids.size() == 1)?lsids.get(0):"";
    }

    static public String getScientificNameRank(String lsid) {

        String snUrl = BIE_SPECIES + lsid + ".json";
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
}

class Facet {

    String field;
    String value;

    public Facet(String field, String value) {
        this.field = field;
        this.value = "\"" + value + "\"";
    }

    public Facet(String field, double min, double max, boolean includeRange) {
        if (includeRange) {
            this.field = field;
        } else {
            this.field = "-" + field;
        }

        this.value = "["
                + String.valueOf(min)
                + " TO "
                + String.valueOf(max)
                + "]";
    }

    @Override
    public String toString() {
        return field + ":" + value;
    }
}