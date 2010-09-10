package org.ala.spatial.analysis.service;

import java.io.File;
import java.io.FileWriter;
import org.ala.spatial.analysis.index.IndexedRecord;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.analysis.service.SamplingService;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 * facilitates access into occurrences index, 
 * 
 * records, species, points
 * 
 * @author adam
 *
 */
public class OccurrencesService {

    /**
     * returns a list of (species names / type / count) for valid 
     * .beginsWith matches
     * 
     * @param filter begins with text to search for
     * @param limit limit on output
     * @return formatted species matches as String[]
     */
    static public String[] filterSpecies(String filter, int limit) {
        return OccurrencesIndex.filterIndex(filter, limit);
    }

    /**
     * gets the number of records for an exact match species (genus, etc) name
     * @param scientificname
     * @return number of records as int
     */
    public static int getSpeciesCount(String scientificname) {
        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(scientificname);

        int count = 0;
        if (ir != null) {
            for (int i = 0; i < ir.length; i++) {
                count += ir[i].record_end - ir[i].record_start + 1;
            }
        }
        return count;
    }
}
