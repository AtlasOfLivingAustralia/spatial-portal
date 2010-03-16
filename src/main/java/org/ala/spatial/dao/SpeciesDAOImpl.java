package org.ala.spatial.dao;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.ala.spatial.model.Species;
import org.ala.spatial.model.TaxonNames;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

/**
 * Implementation class for any of the species search.
 * Currently, all species seach is combined into a single class
 * for easier management
 *
 * @author ajayr
 * @version 1.0
 * 
 */
@Service(value="personDAO")
public class SpeciesDAOImpl extends HibernateDaoSupport implements SpeciesDAO {

    /*
    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    }
     */
    /**
    private HibernateTemplate hibernateTemplate;

    public void setSessionFactory(SessionFactory sessionFactory) {
        //System.out.println("Setting session factory!!");
        this.hibernateTemplate = new HibernateTemplate(sessionFactory);
    }
     **/

    private HibernateTemplate hibernateTemplate;

    @Autowired
    public SpeciesDAOImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
        this.setSessionFactory(sessionFactory);
        this.hibernateTemplate = new HibernateTemplate(sessionFactory); 
    }

    /*
     * Test method to get some species quickly
     *
     * @return     List of species
     */
    // TODO maybe remove this class eventually?
    @Override
    public List<Species> getSpecies() {
        Vector tmpSpecies = new Vector();

        Species s = new Species();
        s.setScientificname("Test name 1");
        tmpSpecies.add(s);

        Species s2 = new Species();
        s2.setScientificname("Test name 2");
        tmpSpecies.add(s2);

        return tmpSpecies;
    }

    /*
     * Get all records for a given taxon name.
     *
     * @param name Taxon name
     * @return     List of species
     */
    @Override
    public List<Species> getRecordsByNameLevel(String name, String level) { //  throws DataAccessException
        //return this.sessionFactory.getCurrentSession().createQuery("from Species where scientificname = ? ").setParameter(0, name).list();
        if (level.equalsIgnoreCase("g")) {
            level = "genus";
        } else if (level.equalsIgnoreCase("sn")) {
            level = "scientificname";
        } else if (level.equalsIgnoreCase("f")) {
            level = "family";
        } else if (level.equalsIgnoreCase("cn")) {
            level = "commonname";
        }

        //Object[] params = {level, name};
        //return hibernateTemplate.find("from Species where ? = ? ", params);
        /*
        Session session = getSession(false);
        try {
        Query query = session.createQuery("from Species where scientificname = ?");
        query.setString(0, name);
        List result = query.list();
        if (result == null) {
        //throw new MyException("No search results.");
        }
        return result;
        } catch (HibernateException ex) {
        throw convertHibernateAccessException(ex);
        }
         */


        
        final String theName = name.toLowerCase();
        final String theLevel = level.toLowerCase();

        return (List) this.hibernateTemplate.execute(new HibernateCallback() {

            @Override
            public List<Species> doInHibernate(Session session) {
                Query query = session.createQuery("from Species where lower("+theLevel+") = :name "); // (:level)
                //query.setParameter(0, theLevel);
                //query.setParameter(1, theName);
                //query.setString("level", theLevel);
                query.setString("name", theName); 
                return query.list();
            }
        });
    }

    /*
     * Get all records for a given taxon id
     *
     * @param name Taxon id
     */
    @Override
    public List<Species> getRecordsById(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*
     * Test methond to get taxon names as Master Names List.
     *
     * @return     List of names
     */
    @Override
    public List<TaxonNames> getNames() {
        Vector tmpSpecies = new Vector();

        TaxonNames s = new TaxonNames();
        s.setTname("Test name 1");
        s.setTlevel("scientificname");
        tmpSpecies.add(s);

        TaxonNames s2 = new TaxonNames();
        s2.setTname("Test name 2");
        s2.setTlevel("scientificname");
        tmpSpecies.add(s2);

        TaxonNames s3 = new TaxonNames();
        s3.setTname("Test name 3");
        s3.setTlevel("genus");
        tmpSpecies.add(s3);

        TaxonNames s4 = new TaxonNames();
        s4.setTname("Test name 4");
        s4.setTlevel("phylum");
        tmpSpecies.add(s4);

        return tmpSpecies;

    }

    /*
     * Get all taxon names list.
     *
     * @param name Taxon name
     * @return     List of names
     */
    @Override
    public List<TaxonNames> findByName(String name) {
        return hibernateTemplate.find("from TaxonNames where lower(tname) like ? ", (name.toLowerCase() + "%"));
    }

    /*
     * Get all taxon names list, compatible with paging. 
     *
     * @param name Taxon name
     * @param limit limit factor for paging
     * @param offset offset factor for paging 
     * @return     Map of names and the total count available
     */
    @Override
    public Map findByName(final String name, final int limit, final int offset) {  // throws DataAccessException
        //Object[] mnldetails = {(name.toLowerCase() + "%"), limit, offset};
        //return hibernateTemplate.find("from TaxonNames where lower(tname) like ? LIMIT ? OFFSET ? ", mnldetails);

        int namesCount = ((Long) hibernateTemplate.find("select count(*) from TaxonNames where lower(tname) like ? ", (name.toLowerCase() + "%")).get(0)).intValue();

        List namesList = (List) this.hibernateTemplate.execute(new HibernateCallback() {

            @Override
            public List<TaxonNames> doInHibernate(Session session) {
                Query query = session.createQuery("from TaxonNames where lower(tname) LIKE ? ");
                query.setParameter(0, (name.toLowerCase() + "%"));
                query.setFirstResult(offset);
                query.setMaxResults(limit);
                return query.list();
            }
        });

        Hashtable<String, Object> htNames = new Hashtable();
        htNames.put("totalCount", namesCount);
        htNames.put("taxanames", namesList);

        return htNames;
    }
}
