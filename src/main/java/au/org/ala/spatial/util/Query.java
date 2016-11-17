/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.util;

import au.org.ala.legend.Facet;
import au.org.ala.legend.LegendObject;
import au.org.ala.legend.QueryField;

import java.util.List;

/**
 * @author Adam
 */
public interface Query {

    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in CSV format.
     */
    String sample(List<QueryField> fields);

    /**
     * Get species list for this query.
     *
     * @return species list as String containing CSV.
     */
    String speciesList();

    String endemicSpeciesList();

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    int getOccurrenceCount();

    /**
     * Get number of species in this query.
     *
     * @return number of species as int or -1 on error.
     */
    int getSpeciesCount();

    /**
     * Gets the number of species that are endemic to the WKT area of this query.
     *
     * @return The number of endemic species or -1 on error
     */
    int getEndemicSpeciesCount();

    /**
     * Get unique term.
     *
     * @return
     */
    String getQ();

    /**
     * Get full term.
     *
     * @return
     */
    String getFullQ(boolean encode);

    /**
     * Query data name.
     *
     * @return
     */
    String getName();

    /**
     * Query data rank.
     *
     * @return
     */
    String getRank();

    /**
     * Create new Query after adding wkt.
     *
     * @param wkt
     * @return
     */
    Query newWkt(String wkt, boolean forMapping);

    /**
     * Create new Query after adding facet.
     *
     * @param facet
     * @return
     */
    Query newFacet(Facet facet, boolean forMapping);

    /**
     * Create new Query after adding facets.
     *
     * @param facet
     * @return
     */
    Query newFacets(List<Facet> facet, boolean forMapping);

    /**
     * Get list of available facets.
     *
     * @return
     */
    List<QueryField> getFacetFieldList();

    /**
     * Get the name of the LSID field.
     *
     * @return
     */
    String getSpeciesIdFieldName();

    /**
     * Get the name of the unique id field.
     *
     * @return
     */
    String getRecordIdFieldName();

    /**
     * Get the name of the longitude field.
     *
     * @return
     */
    String getRecordLongitudeFieldName();

    /**
     * Get the name of the latitude field.
     *
     * @return
     */
    String getRecordLatitudeFieldName();

    /**
     * Get the display name of the unique id field
     * as it appears in the output of sample()
     *
     * @return
     */
    String getRecordIdFieldDisplayName();

    /**
     * Get the display name of the longitude field.
     * as it appears in the output of sample()
     *
     * @return
     */
    String getRecordLongitudeFieldDisplayName();

    /**
     * Get the display name of the latitude field.
     * as it appears in the output of sample()
     *
     * @return
     */
    String getRecordLatitudeFieldDisplayName();

    /**
     * Get legend data for a facet.
     *
     * @param colourmode name of field for facet.
     * @return
     */
    LegendObject getLegend(String colourmode);

    String getUrl();

    List<Double> getBBox();

    String getMetadataHtml();

    String getDownloadUrl(String[] extraFields);

    /**
     * Get parameter to add into WMS requests
     */
    String getQc();

    /**
     * Set parameter to add into WMS requests
     */
    void setQc(String qc);

    String getRecordFieldDisplayName(String colourMode);

    /**
     * Add or remove one record to an internal group.
     */
    void flagRecord(String id, boolean set);

    /**
     * Get the number of flagged records.
     */
    int flagRecordCount();

    /**
     * Get the list of flagged records as '\n' separated String.
     */
    String getFlaggedRecords();

    /**
     * Create a new Query including or excluding flagged records
     */
    Query newFlaggedRecords(boolean include);

    /**
     * Retrieves a new autocomplete list based on the supplied query.
     */
    String getAutoComplete(String facet, String value, int limit);

    String getBS();

    String[] getDefaultDownloadFields();

    void setName(String displayName);
}
