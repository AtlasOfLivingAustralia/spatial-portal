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
     * @return species String should be a list, but lets leave it as a String for now
     */
    public String searchSpecies(String species);

    /**
     * Get all layers, potilitcal/environmental.
     * @return List of layers
     */
    public List listLayers();

    /**
     * Call the map with any params to be passed
     * @param params List of params to be passed
     */
    public void callMap(List params);

    /** 
     * Send to the mapping engine with params to be passed 
     * 
     * @param url URL of the mapping engine
     * @param extra Any extra parameters 
     * @param username Username that may be required 
     * @param password Password that may be required 
     */
    public void sendToMap(String url, String extra, String username, String password);

    /**
     * Call the BIE with any params to be passed
     * @param params List of params to be passed
     */
    public void callBie(List params);
}
