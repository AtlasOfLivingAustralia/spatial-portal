package org.ala.spatial.dao;

import java.util.List;
import org.ala.spatial.model.LayerInfo;

/**
 * DAO class for the LayerInfo class / 'layers' list
 * 
 * @author ajay
 */
public interface LayersDAO {
    public List<LayerInfo> getLayers();
    public LayerInfo getLayerById(String id);
    public List<LayerInfo> getLayersByName(String name);
    public List<LayerInfo> getLayersByDisplayName(String name);
    public List<LayerInfo> getLayersByType(String type);
    public List<LayerInfo> getLayersBySource(String source);
    public List<LayerInfo> getLayersByExtent(String extent);
    public List<LayerInfo> getLayersByEnabled(boolean enabled);
    public List<LayerInfo> getLayersByNotes(String notes);
    public List<LayerInfo> getLayersByCriteria(String keywords);

    public void addLayer(LayerInfo layer);
}
