/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.servlet;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 *
 * @author brendon
 */
public class WorldDepthDaoImpl implements WorldDepthDao {

     private SessionFactory sessionFactory;

    @Override
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public String fetchDepth(String sLon, String sLat) {
        String sDepth = "null";
        Session session = this.sessionFactory.getCurrentSession();
        String sQuery = null;

        try {
            // Begin unit of work

            session.beginTransaction();
            sQuery = "SELECT depth FROM bathy.world_depth WHERE ST_DWithin(geom, GeomFromText('POINT(" + sLon + " " + sLat +  ")', 4326), 0.1) ORDER BY distance(geom, GeomFromText('POINT(" + sLon + " " + sLat +  ")', 4326)) LIMIT 1;";

            SQLQuery qryDepth = session.createSQLQuery(sQuery);
            sDepth = String.valueOf(qryDepth.uniqueResult());

            session.close();
            // End unit of work

        }
        catch (Exception ex) {
           //just ignore it
        }

        return sDepth;

    }

}
