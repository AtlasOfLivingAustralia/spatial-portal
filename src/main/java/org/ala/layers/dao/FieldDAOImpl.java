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
import org.ala.layers.dto.Field;
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
@Service(value = "fieldDAO")
public class FieldDAOImpl extends HibernateDaoSupport implements FieldDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(FieldDAOImpl.class);

    private HibernateTemplate hibernateTemplate;

    @Autowired
    public FieldDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }


    @Override
    public List<Field> getFields() {
        return hibernateTemplate.find("from Field where enabled=true");
    }

    @Override
    public Field getFieldById(String id) {
        //List<Field> fields = hibernateTemplate.find("from Field where enabled=true and id=?", id);


        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("calling getFieldById with DC");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");

        DetachedCriteria dc = DetachedCriteria.forClass(Field.class);
        dc.add(Restrictions.sqlRestriction("id = '" + id + "'"));
        List<Field> fields = hibernateTemplate.findByCriteria(dc);

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Found " + fields.size() + " items");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");




        if (fields.size() > 0) {
            return fields.get(0);
        } else {
            return null; 
        }
    }

    @Override
    public List<Field> getFieldsByDB() {
        return hibernateTemplate.find("from Field where enabled=true and indb=true");
    }
}
