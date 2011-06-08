package org.ala.spatial.dao;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.ala.spatial.model.LayerInfo;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

/**
 * Implementation class for LayersDAO
 * 
 * @author ajay
 * @version 1.0
 */
@Service(value = "layersDAO")
public class LayersDAOImpl extends HibernateDaoSupport implements LayersDAO {

    private HibernateTemplate hibernateTemplate;

    @Autowired
    public LayersDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    /**
     * Get a list of all layers
     * 
     * @return List of layers
     */
    @Override
    public List<LayerInfo> getLayers() {
        return hibernateTemplate.find("from LayerInfo where enabled=true order by  classification1, classification2, displayname ");
    }

    /**
     * Get layer by id
     * 
     * @param id
     * @return LayerInfo
     */
    @Override
    public LayerInfo getLayerById(String id) {
        List<LayerInfo> layers = hibernateTemplate.find("from LayerInfo where uid = ? ", id);
        if (layers.size() > 0) {
            return layers.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get layer by name
     *
     * @param name
     * @return LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByName(String name) {
        return hibernateTemplate.find("from LayerInfo where lower(name) = ? order by  classification1, classification2, displayname ", name.toLowerCase());
    }

    /**
     * Get layer by display name
     * 
     * @param name
     * @return LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByDisplayName(String name) {
        return hibernateTemplate.find("from LayerInfo where lower(displayname) like ? order by  classification1, classification2, displayname ", ("%" + name.toLowerCase() + "%"));
    }

    /**
     * Get layers by type
     * 
     * @param type
     * @return List LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByType(String type) {
        return hibernateTemplate.find("from LayerInfo where lower(type) = ? order by  classification1, classification2, displayname ", type.toLowerCase());
    }

    /**
     * Get layers by source
     * 
     * @param source
     * @return List LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersBySource(String source) {
        return hibernateTemplate.find("from LayerInfo where lower(source) = ? order by  classification1, classification2, displayname ", source.toLowerCase());
    }

    /**
     * Get layers by extent
     * 
     * @param extent
     * @return List LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByExtent(String extent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Get layers by enabled
     * 
     * @param enabled
     * @return List LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByEnabled(boolean enabled) {
        return hibernateTemplate.find("from LayerInfo where enabled = ? order by  classification1, classification2, displayname ", enabled);
    }

    /**
     * Get layers by notes/keywords
     *
     * @param notes
     * @return List LayerInfo
     */
    @Override
    public List<LayerInfo> getLayersByNotes(String notes) {
        return hibernateTemplate.find("from LayerInfo where lower(notes) like ? order by  classification1, classification2, displayname ", ("%" + notes.toLowerCase() + "%"));
        // AND enabled=true 
    }

    @Override
    public void addLayer(LayerInfo layer) {
        hibernateTemplate.saveOrUpdate(layer);
    }

    @Override
    public List<LayerInfo> getLayersByCriteria(String keywords) {
        String sql = "";
        sql += "from LayerInfo where ";
        sql += "lower(notes) like ? ";
        sql += " or lower(displayname) like ? ";
        //sql += " or lower(type) like ? ";
        sql += " or lower(name) like ? ";
        sql += "  AND enabled=true ";
        sql += " order by displayname ";

        keywords = "%" + keywords.toLowerCase() + "%";

        List list = hibernateTemplate.find(sql, new String[]{keywords, keywords, keywords}); // keywords,

        //remove duplicates if any
        Set setItems = new LinkedHashSet(list);
        list.clear();
        list.addAll(setItems);

        return list;//no duplicates now

    }
}
