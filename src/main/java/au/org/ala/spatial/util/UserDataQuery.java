/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.legend.Facet;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.StringConstants;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
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
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * TODO NC 2013-08-15 - Remove all the references to the "include null" gesopatially kosher. I have removed from the UI but I didn't want to
 * break the build before the next release
 *
 * @author Adam
 */
public class UserDataQuery implements Query {

    static final String SAMPLING_SERVICE_CSV_GZIP = "/userdata/occurrences.gz?";
    static final String SAMPLING_SERVICE = "/userdata/occurrences?";
    static final String DOWNLOAD_URL = "/userdata/sample?";
    static final String LEGEND_SERVICE_CSV = "/userdata/legend?";

    static final Pattern QUERY_PARAMS_PATTERN = Pattern.compile("&([a-zA-Z0-9_\\-]+)=");
    /**
     * DEFAULT_VALIDATION must not be null
     */

    static final String DEFAULT_VALIDATION = "";
    static final String WMS_URL = "/userdata/wms/reflect?";
    private static final Logger LOGGER = Logger.getLogger(UserDataQuery.class);
    private String name;
    private String udHeaderId;
    private List<Facet> facets;
    private String wkt;
    private String qc;
    private String layersServiceServer;
    private boolean forMapping;
    private int occurrenceCount = -1;
    private List<Double> bbox = null;
    private String metadata;
    private Map<String, LegendObject> legends = new HashMap<String, LegendObject>();
    private Set<String> flaggedRecords = new HashSet<String>();
    private List<QueryField> facetFieldList = null;

    public UserDataQuery(String udHeaderId) {

        this.udHeaderId = udHeaderId;

        layersServiceServer = CommonData.getLayersServer();

        //make new facet from layersServiceService
        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.getLayersServer()).append("/userdata/facet");

        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

        post.addParameter(StringConstants.ID, udHeaderId);

        try {
            LOGGER.debug("calling facet user data ws: " + sbProcessUrl.toString());
            client.executeMethod(post);

            org.json.simple.JSONObject jo = (org.json.simple.JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

            String facetId = jo.containsKey("facet_id") ? ":" + jo.get("facet_id") : "";

            this.udHeaderId = udHeaderId.split(":")[0] + facetId;

            if (jo.containsKey("metadata")) {
                org.json.simple.JSONObject j = (org.json.simple.JSONObject) new JSONParser().parse((String) jo.get("metadata"));
                if (j.containsKey(StringConstants.BBOX)) {
                    //get bbox
                    String[] b = ((String) j.get(StringConstants.BBOX)).split(",");
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
            LOGGER.error("failed to create the facet", e);
        }
    }

    public UserDataQuery(String udHeaderId, String wkt, List<Facet> facets) {
        if (facets != null) {
            this.facets = new ArrayList<Facet>(facets.size());
            for (int i = 0; i < facets.size(); i++) {
                this.facets.add(facets.get(i));
            }
        }
        this.wkt = (wkt != null && wkt.equals(CommonData.WORLD_WKT)) ? null : wkt;

        this.udHeaderId = udHeaderId;

        layersServiceServer = CommonData.getLayersServer();

        //make new facet from layersServiceService
        StringBuilder sbProcessUrl = new StringBuilder();
        sbProcessUrl.append(CommonData.getLayersServer()).append("/userdata/facet");

        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(sbProcessUrl.toString());

        post.addRequestHeader(StringConstants.ACCEPT, StringConstants.APPLICATION_JSON);

        if (wkt != null) {
            post.addParameter(StringConstants.WKT, wkt);
        }
        if (facets != null) {
            int i = 0;
            for (Facet f : facets) {
                post.addParameter(StringConstants.FACET + i, f.toString());
                i++;
            }
        }
        post.addParameter(StringConstants.ID, udHeaderId);

        try {
            LOGGER.debug("calling facet user data ws: " + sbProcessUrl.toString());
            client.executeMethod(post);

            org.json.simple.JSONObject jo = (org.json.simple.JSONObject) new JSONParser().parse(post.getResponseBodyAsString());

            String facetId = jo.containsKey("facet_id") ? ":" + jo.get("facet_id") : "";

            this.udHeaderId = udHeaderId.split(":")[0] + facetId;

            if (jo.containsKey("metadata")) {
                org.json.simple.JSONObject j = (org.json.simple.JSONObject) new JSONParser().parse((String) jo.get("metadata"));
                if (j.containsKey(StringConstants.BBOX)) {
                    //get bbox
                    String[] b = ((String) j.get(StringConstants.BBOX)).split(",");
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
            LOGGER.error("failed to create the facet", e);
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

        List<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }

        newFacets.add(facet);

        return new UserDataQuery(udHeaderId, wkt, newFacets);
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

            sq = new UserDataQuery(udHeaderId, newWkt, facets);
        } catch (Exception e) {
            LOGGER.error("failed to filter user uploaded points with WKT: " + udHeaderId + ", " + wkt);
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
        String url = layersServiceServer
                + SAMPLING_SERVICE_CSV_GZIP
                + "&q=" + getQ()
                + paramQueryFields(fields);
        LOGGER.debug(url);
        GetMethod get = new GetMethod(url);

        String sample = null;

        long start = System.currentTimeMillis();
        try {
            client.executeMethod(get);
            sample = decompressGz(get.getResponseBodyAsStream());
        } catch (Exception e) {
            LOGGER.error("failed to sample for: " + url, e);
        }
        LOGGER.debug("get sample in " + (System.currentTimeMillis() - start) + "ms");

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
        return 1;
    }

    @Override
    public int getEndemicSpeciesCount() {
        return 1;
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
    public String getQ() {
        return udHeaderId;
    }

    @Override
    public String getFullQ(boolean encode) {
        StringBuilder sb = new StringBuilder();
        sb.append(metadata).append("\n");

        if (wkt != null && wkt.length() > 0) {
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
        return StringConstants.SPECIES;
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
            LOGGER.error("error decompressing gz", e);
        }
        gzipped.close();

        return s;
    }

    @Override
    public List<QueryField> getFacetFieldList() {
        if (facetFieldList == null) {
            List<QueryField> fields = new ArrayList<QueryField>();

            //NC: Load all the facets fields from the cache which is populated from the biocache=service

            //1. add facet list from layersServiceServer
            //2. add default facets (lga, states and territories, etc)
            fields.addAll(getQueryFields());

            facetFieldList = fields;
        }

        return facetFieldList;
    }

    private List<QueryField> getQueryFields() {

        //make new facet from layersServiceService

        List<QueryField> fields = new ArrayList<QueryField>();

        try {
            ObjectMapper om = new ObjectMapper();
            List ja = om.readValue(new URL(CommonData.getLayersServer() + "/userdata/list?id=" + udHeaderId), List.class);

            for (int i = 0; i < ja.size(); i++) {
                fields.add(getQueryField((String) ja.get(i)));
            }

            //also include defaults
            for (QueryField f : CommonData.getDefaultUploadSamplingFields()) {
                fields.add(getQueryField(f.getName()));
            }
        } catch (Exception e) {
            LOGGER.error("failed to create the facet", e);
        }

        return fields;
    }

    private QueryField getQueryField(String field) {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(new URL(CommonData.getLayersServer() + "/userdata/getqf?id=" + udHeaderId + "&field=" + URLEncoder.encode(field, StringConstants.UTF_8)), QueryField.class);
        } catch (Exception e) {
            LOGGER.error("error getting query field header: " + udHeaderId + " field: " + field, e);
        }

        return null;
    }

    @Override
    public String getSpeciesIdFieldName() {
        return StringConstants.NULL;
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
        List<Facet> newFacets = new ArrayList<Facet>();
        if (this.facets != null) {
            newFacets.addAll(this.facets);
        }
        newFacets.addAll(facets);

        return new UserDataQuery(udHeaderId, wkt, newFacets);
    }

    @Override
    public String getUrl() {
        return CommonData.getLayersServer() + WMS_URL;
    }

    @Override
    public List<Double> getBBox() {
        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        // user data
        return "user uploaded points";
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        //this default behaviour of excluding default fields from the download URL may change
        List<String> fieldsAlreadyIncluded = new ArrayList<String>();

        StringBuilder sb = new StringBuilder();
        if (extraFields != null && extraFields.length > 0) {
            for (int i = 0; i < extraFields.length; i++) {
                if (!fieldsAlreadyIncluded.contains(extraFields[i])) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    } else {
                        sb.append("&fl=");
                    }
                    sb.append(extraFields[i]);
                }
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
        List<Facet> newFacets = new ArrayList<Facet>();
        if (facets != null) {
            newFacets.addAll(facets);
        }
        newFacets.add(Facet.parseFacet(sb.toString()));
        return new UserDataQuery(udHeaderId, wkt, newFacets);
    }

    @Override
    public String getAutoComplete(String facet, String value, int limit) {
        return null;
    }

    @Override
    public String getBS() {
        return CommonData.getLayersServer();
    }

    @Override
    public String[] getDefaultDownloadFields() {
        return new String[0];

    }
}
