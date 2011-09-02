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

import java.sql.SQLException;
import java.util.List;
import org.ala.layers.dto.SearchObject;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service(value = "searchDAO")
public class SearchDAOImpl extends HibernateTemplate implements SearchDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(SearchDAOImpl.class);
    private HibernateTemplate hibernateTemplate;

    @Autowired
    public SearchDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    @Override
    public List<SearchObject> findByCriteria(final String criteria) {
        //return hibernateTemplate.find("from SearchObject where ", this)

        return (List<SearchObject>) hibernateTemplate.execute(new HibernateCallback() {
            public Object doInHibernate(final Session session) throws HibernateException, SQLException {
                return session.getNamedQuery("searchobjects") //
                        .setParameter("q", criteria) //
                        .setParameter("lim", 20) 
                        .list();
            }
        });
    }
}
