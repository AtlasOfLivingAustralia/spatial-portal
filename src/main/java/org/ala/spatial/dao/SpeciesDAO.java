package org.ala.spatial.dao;

import java.util.List;
import java.util.Map;
import org.ala.spatial.domain.Species;
import org.ala.spatial.domain.TaxonNames;

/**
 *
 * @author ajayr
 */
public interface SpeciesDAO {
    public List<Species> getSpecies();
    public List<Species> getRecordsByNameLevel(String name, String level);
    public List<Species> getRecordsById(String id);

    public List<TaxonNames> getNames();
    public List<TaxonNames> findByName(String name);
    public Map findByName(String name, int limit, int offset);
}
