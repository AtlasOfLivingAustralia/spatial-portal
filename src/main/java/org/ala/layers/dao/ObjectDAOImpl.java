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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.util.List;
import org.ala.layers.dto.Objects;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernatespatial.criterion.SpatialRestrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service(value = "objectDAO")
public class ObjectDAOImpl extends HibernateDaoSupport implements ObjectDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(FieldDAOImpl.class);
    private HibernateTemplate hibernateTemplate;

    @Autowired
    public ObjectDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }

    @Override
    public List<Objects> getObjects() {
        return hibernateTemplate.find("from Objects");
    }

    @Override
    public List<Objects> getObjectsById(String id) {
        return hibernateTemplate.find("from Objects where id = ?", id);
    }

    @Override
    public Objects getObjectByPid(String pid) {
        List<Objects> l = hibernateTemplate.find("from Objects where pid = ?", pid);
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Objects getObjectByIdAndLocation(String id, Double lng, Double lat) {
//        id = "cl"+id;
//        Object[] values = {id, lng, lat};
//        List<Objects> l = hibernateTemplate.find("from Objects where fid=? AND ST_Within(ST_SETSRID(ST_Point(?,?),4326), geometry)", values );
//
//        if (l.size() > 0) {
//            return l.get(0);
//        } else {
//            return null;
//        }

        System.out.println("===================================================");

//        String wktFilter = "POINT("+lng+" "+lat+")";
//
//        WKTReader fromText = new WKTReader();
//        Geometry filter = null;
//        try {
//            filter = fromText.read(wktFilter);
//        } catch (ParseException e) {
//            throw new RuntimeException("Not a WKT String:" + wktFilter);
//        }
//
//        DetachedCriteria dc = DetachedCriteria.forClass(Objects.class);
//        dc.add(Restrictions.eq("fid", id));
//        dc.add(SpatialRestrictions.within("geometry", filter));
//
//        System.out.println("Query params: \n\t-" + id + "\n\t-" + filter);
//
//        List<Objects> l = hibernateTemplate.findByCriteria(dc);



        DetachedCriteria dc = DetachedCriteria.forClass(Objects.class);
        dc.add(Restrictions.sqlRestriction("fid = '"+id+"'"));
        dc.add(Restrictions.sqlRestriction("ST_Within(ST_SETSRID(ST_Point("+lng+", "+lat+"), 4326), the_geom)"));
        List<Objects> l = hibernateTemplate.findByCriteria(dc);


        System.out.println("******************* Found " + l.size() + " items. ");
        System.out.println("===================================================");

        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }

    }

}
