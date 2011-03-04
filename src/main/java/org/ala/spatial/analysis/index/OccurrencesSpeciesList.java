/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.index;

/**
 *
 * @author Adam
 */
public class OccurrencesSpeciesList {

    String datasetId;
    int[] speciesCounts;
    int[] occurrenceRecords;
    int occurrencesCount;
    int speciesCount;
    OccurrencesFilter occurrencesFilter;

    /**
     *
     * @param id dataset uniqueId() as String
     * @param filter OccurrencesFilter of species list to produce
     */
    OccurrencesSpeciesList(String id, OccurrencesFilter filter) {
        this.datasetId = id;
        occurrencesFilter = filter;

        OccurrencesIndex oi = OccurrencesCollection.getOccurrencesIndex(id);
        if (oi != null) {
            this.speciesCounts = oi.getSpeciesCounts(filter);
        } else {
            this.speciesCounts = null;
        }

        calcTotals();
    }

    /**
     * update occurrences and species totals
     */
    private void calcTotals() {
        occurrencesCount = 0;
        speciesCount = 0;

        if (speciesCounts != null) {
            for (int c : speciesCounts) {                
                if (c > 0) {
                    occurrencesCount += c;
                    speciesCount++;
                }
            }
        }
    }

    /**
     *
     * @return occurrences count as int
     */
    public int getOccurrencesCount() {
        return occurrencesCount;
    }

    /**
     *
     * @return species count as int
     */
    public int getSpeciesCount() {
        return speciesCount;
    }

    /**
     *
     * @return species list as comma delimited records containing
     *         <code>family, scientific name, common names, taxon rank, occurrences count</code>
     *         as String []
     *
     */
    public String[] getSpeciesList() {
        Dataset d = OccurrencesCollection.getDataset(datasetId);

        if (speciesCount == 0 || d == null) {
            return null;
        }

        OccurrencesIndex oi = d.getOccurrencesIndex();

        if (oi == null) {
            return null;
        }

        String[] sa = new String[speciesCount];
        int p = 0;
        for (int i = 0; i < speciesCounts.length; i++) {
            if (speciesCounts[i] > 0) {                
                sa[p] = (new StringBuilder())
                        .append(SpeciesIndex.getFamilyName(i)).append("*")
                        .append(SpeciesIndex.getScientificName(i)).append("*")
                        .append(SpeciesIndex.getCommonNames(i)).append("*")
                        .append(SpeciesIndex.getTaxonRank(i)).append("*")
                        .append(SpeciesIndex.getLSID(i)).append("*")
                        .append(speciesCounts[i]).toString();
                p++;
            }
        }
        return sa;
    }

    /**
     * get's a species list entry with an LSID input
     */
    static public String getSpeciesListEntryFromADistribution(String lsid) {
        int i = SpeciesIndex.findLSID(lsid);
        return (new StringBuilder())
                        .append(SpeciesIndex.getFamilyName(i)).append("*")
                        .append(SpeciesIndex.getScientificName(i)).append("*")
                        .append(SpeciesIndex.getCommonNames(i)).append("*")
                        .append(SpeciesIndex.getTaxonRank(i)).append("*")
                        .append(SpeciesIndex.getLSID(i)).append("*")
                        .append("-1").toString();
    }

    /**
     *
     * @return occurrence record id's for the OccurrencesFilter supplied in
     * construction.  Otherwise null.
     */
    public OccurrenceRecordNumbers getRecordNumbers() {
        OccurrencesIndex oi = OccurrencesCollection.getOccurrencesIndex(datasetId);
        if (oi != null) {
            this.occurrenceRecords = oi.getRecordNumbers(occurrencesFilter);
            return new OccurrenceRecordNumbers(datasetId, occurrenceRecords);
        } else {
            this.occurrenceRecords = null;
            return null;
        }
    }
}
