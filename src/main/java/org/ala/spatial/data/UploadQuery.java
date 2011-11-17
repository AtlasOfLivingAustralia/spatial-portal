/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.ala.spatial.sampling.Sampling;
import org.ala.spatial.sampling.SimpleRegion;
import org.ala.spatial.sampling.SimpleShapeFile;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.wms.RecordsLookup;

/**
 *
 * @author Adam
 */
public class UploadQuery implements Query, Serializable {

    ArrayList<QueryField> data;
    double[] points;
    String name;
    String uniqueId;
    String metadata;
    int originalFieldCount;

    //for history
    String wkt = "";
    ArrayList<Facet> facets = new ArrayList<Facet>();

    /**
     *
     * @param uniqueId
     * @param name
     * @param points
     * @param fields uploaded column data in ArrayList<QueryField>.  First
     * three fields must contain record id, longitude, latitude.
     */
    public UploadQuery(String uniqueId, String name, double[] points, ArrayList<QueryField> fields, String metadata) {
        this.points = points;
        this.data = fields;
        this.name = name;
        this.uniqueId = uniqueId;
        this.metadata = metadata;
        this.originalFieldCount = fields.size();
    }

    public void resetOriginalFieldCount(int count) {
        if(count == -1) {
            this.originalFieldCount = data.size();
        } else {
            this.originalFieldCount = count;
        }
    }

    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in JSON format.
     */
    @Override
    public String sample(ArrayList<QueryField> fields) {
        StringBuilder sb = new StringBuilder();

        //do sampling
        layerSampling(fields);

        //identify fields to data for return
        int[] validFields = null;
        if (fields == null) {
            validFields = new int[data.size()];
            for (int i = 0; i < data.size(); i++) {
                validFields[i] = i;
            }
        } else {
            validFields = new int[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                validFields[i] = -1;
                for (int j = 0; j < data.size(); j++) {
                    if (fields.get(i) != null
                            && fields.get(i).getName() != null
                            && data.get(j).getName().equals(fields.get(i).getName())) {
                        validFields[i] = j;
                        break;
                    }
                }
            }
        }

        //header
        for (int i = 0; i < validFields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            if (validFields[i] >= 0) {
                sb.append(data.get(validFields[i]).getDisplayName());
            } else {
                sb.append(fields.get(i).getDisplayName());
            }
        }

        //data
        int recordCount = points.length / 2;
        for (int j = 0; j < recordCount; j++) {
            sb.append("\n");
            for (int i = 0; i < validFields.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                if (validFields[i] >= 0 && data.get(validFields[i]).getAsString(j) != null) {
                    sb.append("\"");
                    sb.append(String.valueOf(data.get(validFields[i]).getAsString(j)).replace("\"", "\"\""));
                    sb.append("\"");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Get species list for this query.
     *
     * @return species list as String containing JSON array.
     */
    @Override
    public String speciesList() {
        return null;
    }

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    @Override
    public int getOccurrenceCount() {
        return points.length / 2;
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

    /**
     * Get parsed coordinates and optional fields for this query.
     *
     * @param fields QueryFields to return in the sample as ArrayList<QueryField>.
     * If a QueryField isStored() it will be populated with the field data.
     * @return coordinates as double [] like [lng, lat, lng, lat, ...].
     */
    @Override
    public double[] getPoints(ArrayList<QueryField> fields) {
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                for (int j = 0; j < data.size(); j++) {
                    if (fields.get(i).getName().equals(data.get(j).getName())) {
                        fields.get(i).copyData(data.get(j));
                        break;
                    }
                }
            }
        }

        return points;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRank() {
        return "species";
    }

    @Override
    public String getQ() {
        return uniqueId;
    }

    @Override
    public String getFullQ(boolean encode) {
        StringBuilder sb = new StringBuilder();        
        sb.append(metadata).append("\n");
        
        if(wkt.length()>0) {
            sb.append(wkt).append("\n");
        }

        for(int i=0;i<facets.size();i++) {
            sb.append(facets.get(i).toString()).append("\n");
        }

        return sb.toString();
    }

    @Override
    public Query newWkt(String wkt, boolean forMapping) {
        if (wkt == null || wkt.equals(CommonData.WORLD_WKT)) {
            return this;
        }

        SimpleRegion sr = SimpleShapeFile.parseWKT(wkt);

        //per record test
        boolean[] valid = new boolean[points.length / 2];
        int count = 0;
        for (int i = 0; i < valid.length; i++) {
            valid[i] = sr.isWithin(points[i * 2], points[i * 2 + 1]);
            if (valid[i]) {
                count++;
            }
        }

        UploadQuery q = newFromValidMapping(valid, count);
        
        //maintain wkt history
        if(this.wkt.length() > 0) {
            q.wkt = this.wkt + " AND " + wkt;
        } else {
            q.wkt = wkt;
        }

        return q;
    }

    UploadQuery newFromValidMapping(boolean[] valid, int count) {
        //copy original data
        ArrayList<QueryField> facetData = new ArrayList<QueryField>(originalFieldCount);
        for (int i = 0; i < originalFieldCount; i++) {
            QueryField qf = new QueryField(data.get(i).getName(), data.get(i).getDisplayName(), data.get(i).getFieldType());
            qf.ensureCapacity(count);
            for (int j = 0; j < valid.length; j++) {
                if (valid[j]) {
                    qf.add(data.get(i).getAsString(j));
                }
            }
            qf.store();
            facetData.add(qf);
        }

        //copy points
        double[] facetPoints = new double[count * 2];
        int pos = 0;
        for (int j = 0; j < valid.length; j++) {
            if (valid[j]) {
                facetPoints[pos * 2] = points[j * 2];
                facetPoints[pos * 2 + 1] = points[j * 2 + 1];
                pos++;
            }
        }


        String uid = String.valueOf(System.currentTimeMillis());

        RecordsLookup.putData(uid, facetPoints, facetData, metadata);

        return new UploadQuery(uid, name, facetPoints, facetData, metadata);
    }

    @Override
    public Query newFacet(Facet facet, boolean forMapping) {
        ArrayList<Facet> facets = new ArrayList<Facet>();
        facets.add(facet);
        return newFacets(facets, forMapping);
    }

    @Override
    public String getWMSpath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ArrayList<QueryField> getFacetFieldList() {
        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        for(int i=1;i<data.size();i++) {
            fields.add(data.get(i));
        }
        return fields;
    }

    /**
     * Do sampling on layers that are in fields but not in data.
     * 
     * @param fields
     */
    private void layerSampling(ArrayList<QueryField> fields) {
        if (fields == null) {
            return;
        }
        ArrayList<QueryField> forSampling = new ArrayList<QueryField>();
        for (int i = 0; i < fields.size(); i++) {
            boolean found = false;
            for (int j = 0; j < data.size(); j++) {
                if (data.get(j).getName().equals(fields.get(i).getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                forSampling.add(fields.get(i));
            }
        }

        //do sampling
        double[][] coordinates = new double[points.length / 2][2];
        for (int i = 0; i < points.length; i += 2) {
            coordinates[i / 2][0] = points[i];
            coordinates[i / 2][1] = points[i + 1];
        }
        ArrayList<String> facetIds = new ArrayList<String>();
        for (int i = 0; i < forSampling.size(); i++) {
            facetIds.add(forSampling.get(i).getName());
        }
        ArrayList<String[]> output = Sampling.sampling(facetIds, coordinates);

        for (int i = 0; i < forSampling.size(); i++) {
            String[] s = output.get(i);
            QueryField qf = forSampling.get(i);
            if (qf.getName() != null) {
                qf.ensureCapacity(s.length);
                for (int j = 0; j < s.length; j++) {
                    qf.add(s[j]);
                }
                qf.store();

                data.add(qf);
            }
        }

        //update RecordsLookup for new fields, if it is already in RecordsLookup
        Object[] recordData = (Object[]) RecordsLookup.getData(getQ());
        if (recordData != null) {
            double[] points = (double[]) recordData[0];
            RecordsLookup.putData(getQ(), points, data, metadata);
        }
    }

    @Override
    public String getSpeciesIdFieldName() {
        return null;
    }

    @Override
    public String getRecordIdFieldName() {
        return data.get(0).getName();
    }

    @Override
    public String getRecordLongitudeFieldName() {
        return data.get(1).getName();
    }

    @Override
    public String getRecordLatitudeFieldName() {
        return data.get(2).getName();
    }

    @Override
    public String getRecordIdFieldDisplayName() {
        return data.get(0).getDisplayName();
    }

    @Override
    public String getRecordLongitudeFieldDisplayName() {
        return data.get(1).getDisplayName();
    }

    @Override
    public String getRecordLatitudeFieldDisplayName() {
        return data.get(2).getDisplayName();
    }

    @Override
    public LegendObject getLegend(String colourmode) {
        //get existing
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getName().equals(colourmode)) {
                return data.get(i).getLegend();
            }
        }

        //create
        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        QueryField qf = new QueryField(colourmode, CommonData.getFacetLayerDisplayName(colourmode), QueryField.FieldType.AUTO);
        qf.setStored(true);
        fields.add(qf);
        layerSampling(fields);

        //get existing
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getName().equals(colourmode)) {
                return data.get(i).getLegend();
            }
        }

        //fail
        return null;
    }

    @Override
    public Query newFacets(List<Facet> facet, boolean forMapping) {
        //copy all the data through the list of facets (AND)

        //setup
        ArrayList<List<QueryField>> facetFields = new ArrayList<List<QueryField>>();
        for (int k = 0; k < facet.size(); k++) {
            Facet f = facet.get(k);
            String[] fields = f.getFields();
            List<QueryField> qf = new ArrayList<QueryField>();
            for (int j = 0; j < fields.length; j++) {
                int i = 0;
                for (i = 0; i < data.size(); i++) {
                    if (fields[j].equals(data.get(i).getName())) {
                        qf.add(data.get(i));
                        break;
                    }
                }
            }
            facetFields.add(qf);
        }

        //per record test
        boolean[] valid = new boolean[points.length / 2];
        int count = 0;
        for (int i = 0; i < valid.length; i++) {
            int sum = 0;
            for (int j = 0; j < facet.size(); j++) {
                if (facet.get(j).isValid(facetFields.get(j), i)) {
                    sum++;
                }
            }

            valid[i] = sum == facet.size();
            if (valid[i]) {
                count++;
            }
        }

        UploadQuery uq = newFromValidMapping(valid, count);

        //maintain facet history
        uq.facets.addAll(this.facets);
        uq.facets.addAll(facet);

        return uq;
    }

    @Override
    public String getUrl() {
        return CommonData.webportalServer + "/ws/wms/reflect?";
    }
    List<Double> bbox = null;

    @Override
    public List<Double> getBBox() {
        if (bbox == null) {
            bbox = new ArrayList<Double>();

            double minx, miny, maxx, maxy;
            minx = points[0];
            maxx = points[0];
            miny = points[1];
            maxy = points[1];
            for (int i = 0; i < points.length; i += 2) {
                if (minx > points[i]) {
                    minx = points[i];
                }
                if (maxx < points[i]) {
                    maxx = points[i];
                }
                if (miny > points[i + 1]) {
                    miny = points[i + 1];
                }
                if (maxy < points[i + 1]) {
                    maxy = points[i + 1];
                }
            }

            bbox.add(minx);
            bbox.add(miny);
            bbox.add(maxx);
            bbox.add(maxy);
        }

        return bbox;
    }

    @Override
    public String getMetadataHtml() {
        String [] m = metadata.replace("<br />","").split("\n");
        String name = m[1].substring(m[1].indexOf(':') + 1).trim();
        String description = m[2].substring(m[2].indexOf(':') + 1).trim();
        String date = m[3].substring(m[3].indexOf(':') + 1).trim();

        StringBuilder fieldsList = new StringBuilder();
        for(int i=0;i<data.size();i++) {
            if(i > 0) {
                fieldsList.append(", ");
            }
            fieldsList.append(data.get(i).getDisplayName());
        }

        String html = "User uploaded coordinates\n";
        html += "<table class='md_table'>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Name: </td><td class='md_spacer'/><td class='md_value'>" + name + "</td></tr>";
        html += "<tr><td class='md_th'>Description: </td><td class='md_spacer'/><td class='md_value'>" + description + "</td></tr>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Date: </td><td class='md_spacer'/><td class='md_value'>" + date + "</td></tr>";
        html += "<tr><td class='md_th'>Number of coordinates: </td><td class='md_spacer'/><td class='md_value'>" + getOccurrenceCount() + "</td></tr>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Number of fields: </td><td class='md_spacer'/><td class='md_spacer'>" + data.size() + "</td></tr>";
        html += "<tr><td class='md_th'>List of fields: </td><td class='md_spacer'/><td class='md_spacer'>" + fieldsList.toString() + "</td></tr>";
        html += "</table>";

        return html;
    }

    @Override
    public String getDownloadUrl(String[] extraFields) {
        return null;
    }

    @Override
    public byte[] getDownloadBytes(String[] extraFields, String [] displayNames) {
        ArrayList<QueryField> fields = new ArrayList<QueryField>();
        if (getFacetFieldList() != null) {
            fields.add(data.get(0));    //id column (1st) is not in getFacetFieldList()
            fields.addAll(getFacetFieldList());
        }
        if (extraFields != null && extraFields.length > 0) {
            for (int i=0;i<extraFields.length;i++) {
                String s = extraFields[i];
                if(displayNames == null) {
                    fields.add(new QueryField(s, CommonData.getFacetLayerDisplayName(s), QueryField.FieldType.AUTO));
                } else {
                    fields.add(new QueryField(s, displayNames[i], QueryField.FieldType.AUTO));
                }
            }
        }
        try {
            //zip it
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(bos);
            ZipEntry ze = new ZipEntry(getName() + ".csv");
            zos.putNextEntry(ze);
            zos.write(sample(fields).getBytes("UTF-8"));
            zos.close();

            return bos.toByteArray();
        } catch (Exception ex) {
            Logger.getLogger(UploadQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String getQc() {
        return null;
    }

    @Override
    public void setQc(String qc) {

    }

    /**
     * Get the column header name for a column in the output of sampling.
     *
     * @param colourMode
     * @return
     */
    @Override
    public String getRecordFieldDisplayName(String colourMode) {
        for(int i=0;i<data.size();i++) {
            if(data.get(i).getName().equals(colourMode)) {
                return data.get(i).getDisplayName();
            }
        }
        return colourMode;
    }
}
