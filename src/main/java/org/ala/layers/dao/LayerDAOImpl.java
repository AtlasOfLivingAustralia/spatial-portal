/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.dao;

import java.util.List;
import org.ala.layers.dto.Layer;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service(value = "layerDAO")
public class LayerDAOImpl extends HibernateDaoSupport implements LayerDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(LayerDAOImpl.class);
    private HibernateTemplate hibernateTemplate;

    @Autowired
    public LayerDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    @Override
    public List<Layer> getLayers() {
        return hibernateTemplate.find("from Layer where enabled=true");
    }

    @Override
    public Layer getLayerById(int id) {
        List<Layer> layers = hibernateTemplate.find("from Layer where enabled=true and id=?", id);
        if (layers.size() > 0) {
            return layers.get(0);
        } else {
            return null;
        }

    }

    @Override
    public Layer getLayerByName(String name) {
        //List<Layer> layers = hibernateTemplate.find("from Layer where enabled=true and name=?", name);

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("calling layerbyname with DC");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");

        DetachedCriteria dc = DetachedCriteria.forClass(Layer.class);
        dc.add(Restrictions.sqlRestriction("name = '" + name + "'"));
        List<Layer> layers = hibernateTemplate.findByCriteria(dc);
        
        System.out.println("Found " + layers.size() + " items");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");

        if (layers.size() > 0) {
            return layers.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Layer> getLayersByEnvironment() {
        return hibernateTemplate.find("from Layer where enabled=true and type='Environmental'");
    }

    @Override
    public List<Layer> getLayersByContextual() {
        return hibernateTemplate.find("from Layer where enabled=true and type='Contextual'");
    }
}
