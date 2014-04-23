/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.data;

import au.org.ala.spatial.util.CommonData;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.LegendObject;
import org.ala.layers.legend.QueryField;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * TODO NC 2013-08-15 - Remove all the references to the "include null" gesopatially kosher. I have removed from the UI but I didn't want to
 * break the build before the next release
 *
 * @author Adam
 */
//@Configurable(autowire = Autowire.BY_TYPE)
public class UserDataQuery implements Query {

    static final String SAMPLING_SERVICE_CSV_GZIP = "/userdata/occurrences.gz?";
    static final String SAMPLING_SERVICE = "/userdata/occurrences?";
    static final String DOWNLOAD_URL = "/userdata/download?";
    static final String LEGEND_SERVICE_CSV = "/userdata/legend?";

    static final Pattern queryParamsPattern = Pattern.compile("&([a-zA-Z0-9_\\-]+)=");
    /**
     * DEFAULT_VALIDATION must not be null
     */

    static final String DEFAULT_VALIDATION = "";
    static final String WMS_URL = "/userdata/wms/reflect?";

    String name;
    String ud_header_id;

    ArrayList<Facet> facets;
    String wkt;

    String qc;
    String layersServiceServer;

    boolean forMapping;

    int occurrenceCount = -1;
    List<Double> bbox = null;
    String metadata;

    HashMap<String, LegendObject> legends = new HashMap<String, LegendObject>();
    HashSet<String> flaggedRecords = new HashSet<String>();

    private static Logger logger = Logger.getLogger(UserDataQuery.class);

    public UserDataQuery(String ud_header_id) {

        this.ud_header_id = ud_header_id;

        layersServiceServer = CommonData.layersServer;

        //make new facet from layersServiceService
        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.layersServer + "/userdata/facet");

        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader("Accept", "application/json");

        post.addParameter("id", ud_header_id);

        try {
            logger.debug("calling facet user data ws: " + sbProcessUrl.toString());
            int result = client.executeMethod(post);

            org.json.simple.JSONObject jo = (org.json.simple.JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

            if (!jo.containsKey("error")) {

            }

            String facet_id = jo.containsKey("facet_id") ? ":" + String.valueOf(jo.get("facet_id")) : "";

            this.ud_header_id = ud_header_id.split(":")[0] + facet_id;

            if (jo.containsKey("metadata")) {
                org.json.simple.JSONObject j = (org.json.simple.JSONObject) new JSONParser().parse((String) jo.get("metadata"));
                if (j.containsKey("bbox")) {
                    //get bbox
                    String[] b = ((String) j.get("bbox")).split(",");
                    bbox = new ArrayList<Double>();
                    bbox.add(Double.parseDouble(b[0]));
                    bbox.add(Double.parseDouble(b[1]));
                    bbox.add(Double.parseDouble(b[2]));
                    bbox.add(Double.parseDouble(b[3]));
                }
                if (j.containsKey("number_of_records")) {
                    occurrenceCount = ((Long) j.get("number_of_records")).intValue();
                }
            }

        } catch (Exception e) {
            logger.error("failed to create the facet", e);
        }
    }

    public UserDataQuery(String ud_header_id, String wkt, ArrayList<Facet> facets, boolean forMapping) {
        if (facets != null) {
            this.facets = (ArrayList<Facet>) facets.clone();
        }
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : wkt;

        this.ud_header_id = ud_header_id;

        layersServiceServer = CommonData.layersServer;

        //make new facet from layersServiceService
        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.layersServer + "/userdata/facet");

        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader("Accept", "application/json");

        if (wkt != null) {
            post.addParameter("wkt", wkt);
        }
        if (facets != null) {
            int i = 0;
            for (Facet f : facets) {
                post.addParameter("facet" + i, f.toString());
                i++;
            }
        }
        post.addParameter("id", ud_header_id);

        try {
            logger.debug("calling facet user data ws: " + sbProcessUrl.toString());
            int result = client.executeMethod(post);

            org.json.simple.JSONObject jo = (org.json.simple.JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

            if (!jo.containsKey("error")) {

            }

            String facet_id = jo.containsKey("facet_id") ? ":" + String.valueOf(jo.get("facet_id")) : "";

            this.ud_header_id = ud_header_id.split(":")[0] + facet_id;

            if (jo.containsKey("metadata")) {
                org.json.simple.JSONObject j = (org.json.simple.JSONObject) new JSONParser().parse((String) jo.get("metadata"));
                if (j.containsKey("bbox")) {
                    //get bbox
                    String[] b = ((String) j.get("bbox")).split(",");
                    bbox = new ArrayList<Double>();
                    bbox.add(Double.parseDouble(b[0]));
                    bbox.add(Double.parseDouble(b[1]));
                    bbox.add(Double.parseDouble(b[2]));
                    bbox.add(Double.parseDouble(b[3]));
                }
                if (j.containsKey("number_of_records")) {
                    occurrenceCount = ((Long) j.get("number_of_records")).intValue();
                }
            }

        } catch (Exception e) {
            logger.error("failed to create the facet", e);
        }
    }

    /**
     * Further restrict records by field values.
     */
    @Override
    public UserDataQuery newFacet(Facet facet, boolean forMapping) {
        if (facet == null) {
            return this;
        }

        ArrayList<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        if (facet != null) {
            newFacets.add(facet);
        }

        return new UserDataQuery(ud_header_id, wkt, newFacets, forMapping);
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
    public UserDataQuery newWkt(String wkt, boolean forMapping) {
        if (wkt == null || wkt.equals(CommonData.WORLD_WKT) || wkt.equals(this.wkt)) {
            return this;
        }

        UserDataQuery sq = null;
        try {
            String newWkt = wkt;
            if (this.wkt != null) {
                Geometry newGeom = new WKTReader().read(wkt);
                Geometry thisGeom = new WKTReader().read(this.wkt);
                Geometry intersectionGeom = thisGeom.intersection(newGeom);
                newWkt = (new WKTWriter()).write(intersectionGeom).replace(" (", "(").replace(", ", ",").replace(") ", ")");
            }

            sq = new UserDataQuery(ud_header_id, newWkt, facets, forMapping);
        } catch (Exception e) {
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
        String url = layersServiceServer
                + SAMPLING_SERVICE_CSV_GZIP
                + "&q=" + getQ()
                + paramQueryFields(fields);
        logger.debug(url);
        GetMethod get = new GetMethod(url);

        String sample = null;

        long start = System.currentTimeMillis();
        try {
            int result = client.executeMethod(get);
            sample = decompressGz(get.getResponseBodyAsStream());
        } catch (Exception e) {
        }
        logger.debug("get sample in " + (System.currentTimeMillis() - start) + "ms");

        return sample;
    }


    /**
     * Get species list for this query.
     *
     * @return species list as String containing CSV.
     */
    @Override
    public String speciesList() {
        return null;
    }

    @Override
    public String endemicSpeciesList() {
        return null;
    }

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    @Override
    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Get number of species in this query.
     *
     * @return number of species as int or -1 on error.
     */
    @Override
    public int getSpeciesCount() {
        return 1; //user points ahs no species idenifiers
    }

    @Override
    public int getEndemicSpeciesCount() {
        return 1;
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
        return ud_header_id;
    }

    @Override
    public String getFullQ(boolean encode) {
        StringBuilder sb = new StringBuilder();
        sb.append(metadata).append("\n");

        if (wkt.length() > 0) {
            sb.append(wkt).append("\n");
        }

        for (int i = 0; i < facets.size(); i++) {
            sb.append(facets.get(i).toString()).append("\n");
        }

        return sb.toString();

    }

    @Override
    public String getName() {
        if (name == null) {
            name = "User uploaded points";
        }

        return name;
    }

    @Override
    public String getRank() {
        return "species";
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
            logger.error("error decompressing gz", e);
        }
        gzipped.close();

        return s;
    }

    ArrayList<QueryField> facetFieldList = null;

    @Override
    public ArrayList<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            ArrayList<QueryField> fields = new ArrayList<QueryField>();

            //NC: Load all the facets fields from the cache which is populated from the biocache=service
            //fields.addAll(FacetCacheImpl.getFacetQueryFieldList());

            //1. add facet list from layersServiceServer
            //2. add default facets (lga, states and territories, etc)
            fields.addAll(getQueryFields());

            facetFieldList = fields;
        }

        return facetFieldList;
    }

    private List<QueryField> getQueryFields() {

        //make new facet from layersServiceService

        ArrayList<QueryField> fields = new ArrayList<QueryField>();

        try {
            ObjectMapper om = new ObjectMapper();
            List ja = om.readValue(new URL(CommonData.layersServer + "/userdata/list?id=" + ud_header_id), List.class);

            for (int i = 0; i < ja.size(); i++) {
                fields.add(getQueryField((String) ja.get(i)));
            }

            //also include defaults
            for (QueryField f : CommonData.getDefaultUploadSamplingFields()) {
                fields.add(getQueryField(f.getName()));
            }
        } catch (Exception e) {
            logger.error("failed to create the facet", e);
        }

        return fields;
    }

    private QueryField getQueryField(String field) {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(new URL(CommonData.layersServer + "/userdata/getqf?id=" + ud_header_id + "&field=" + URLEncoder.encode(field, "UTF-8")), QueryField.class);
        } catch (Exception e) {
            logger.error("error getting query field header: " + ud_header_id + " field: " + field, e);
        }

        return null;
    }

    @Override
    public String getSpeciesIdFieldName() {
        return "null";
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

        if (lo == null) {
            //is it in fields?
            for (int i = 0; i < facetFieldList.size(); i++) {
                if (facetFieldList.get(i).getName().equals(colourmode)) {
                    return facetFieldList.get(i).getLegend();
                }
            }

            //get it from layersServiceServer
            QueryField qf = getQueryField(colourmode);
            if (qf != null) {
                facetFieldList.add(qf);
                return qf.getLegend();
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

        return new UserDataQuery(ud_header_id, wkt, newFacets, forMapping);
    }

    @Override
    public String getUrl() {
        return CommonData.layersServer + WMS_URL;
    }

    @Override
    public List<Double> getBBox() {
        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        // user data
        String html = "user uploaded points";

        return html;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("&extra=").append(CommonData.extraDownloadFields);
        if (extraFields != null && extraFields.length > 0) {
            for (int i = 0; i < extraFields.length; i++) {
                sb.append(",").append(extraFields[i]);
            }
        }
        return layersServiceServer + DOWNLOAD_URL + "q=" + getQ() + sb.toString();
    }

    @Override
    public String getQc() {
        return "";
    }

    @Override
    public void setQc(String qc) {
        // n/a
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
        return new UserDataQuery(ud_header_id, wkt, newFacets, forMapping);
    }

    @Override
    public String getAutoComplete(String facet, String value, int limit) {
        return null;
    }

    @Override
    public String getBS() {
        return CommonData.layersServer;
    }
}
