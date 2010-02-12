package org.ala.spatial.analysis;

import java.util.List;

/**
 * Interface class for the base Spatial Analysis functions/methods
 *
 * @author ajayr
 */
public interface SpatialAnalysis {

    /**
     * Search for a specific species.
     * @param species To be searched for
     * @return String should be a list, but lets leave it as a String for now
     */
    public String searchSpecies(String species);

    /**
     * Get all layers, potilitcal/environmental.
     * @return List of layers
     */
    public List listLayers();

    /**
     * Call the map with any params to be passed
     * @param List of params to be passed
     */
    public void callMap(List params);

    /**
     * Call the BIE with any params to be passed
     * @param List of params to be passed
     */
    public void callBie(List params);
}
