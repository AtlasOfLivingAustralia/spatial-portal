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
import org.ala.layers.dto.LayerPid;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service(value = "layerPidDAO")
public class LayerPidDAOImpl extends HibernateDaoSupport implements LayerPidDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(LayerPidDAOImpl.class);
    private HibernateTemplate hibernateTemplate;

    @Autowired
    public LayerPidDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    @Override
    public List<LayerPid> getLayers() {
        return hibernateTemplate.find("from LayerPid");
    }

    @Override
    public LayerPid getLayerById(String id) {
        List<LayerPid> layers = hibernateTemplate.find("from LayerPid id=?", id);
        if (layers.size() > 0) {
            return layers.get(0);
        } else {
            return null;
        }

    }

    @Override
    public LayerPid getLayerByPid(String pid) {
        List<LayerPid> layers = hibernateTemplate.find("from LayerPid pid=?", pid);
        if (layers.size() > 0) {
            return layers.get(0);
        } else {
            return null;
        }

    }

}
