

package au.org.emii.portal.servlet;

import org.hibernate.SessionFactory;

/**
 *
 * @author brendon
 */
public interface WorldDepthDao {

    public void setSessionFactory(SessionFactory sessionFactory);

    public String fetchDepth(String sLon, String sLat);

}
