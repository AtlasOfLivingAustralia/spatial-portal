/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.userdata;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.AnimationSelection;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.mest.SearchQuery;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.wms.WMSStyle;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.zkoss.zk.ui.Executions;

/**
 *
 * @author brendon
 */
public class UserDataDaoImpl implements UserDataDao{

    private SessionFactory sessionFactory;
    private UserPortal currentUser = new UserPortal();
    private List<UserMap> userMaps = new ArrayList<UserMap>();
    private List<UserSearch> userSearches = new ArrayList<UserSearch>();
    private Logger logger = Logger.getLogger(this.getClass());

     /**
     * @return the currentUser
     */
    public UserPortal getCurrentUser() {
        return currentUser;
    }

    /**
     * @param currentUser
     *            the currentUser to set
     */
    public void setCurrentUser(UserPortal currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * @return the userSearches
     */
    public void fetchUser(String userName) {
        try {
            // Begin unit of work
            Session sess = this.sessionFactory.getCurrentSession();
            sess.beginTransaction();
            currentUser = (UserPortal) sess.createQuery(
                    "from UserPortal where username=?").setString(0, userName).uniqueResult();

            if (currentUser == null) {
                // then we need to create one
                UserPortal us = new UserPortal();
                us.setUserName(userName);
                sess.save(us);
                sess.getTransaction().commit();
                currentUser = us;
            }

        } catch (HibernateException hex) {

            logger.debug(hex.getMessage());
        }

    }

    public UserMap getUserMapByName(String mapName) {
        UserMap uMap = new UserMap();

        try {
            // Begin unit of work
            Session sess = this.sessionFactory.getCurrentSession();
            sess.beginTransaction();

            Query q = sess.createQuery("from UserMap where userid=:userid and mapname=:mapname");
            q.setLong("userid", currentUser.getUserId());
            q.setString("mapname", mapName);
            uMap = (UserMap) q.uniqueResult();


        } catch (HibernateException hex) {

            logger.debug(hex.getMessage());
        }

        return uMap;

    }

    public UserMap fetchUserMapByName(String mapName) {
        UserMap uMap = new UserMap();
        uMap = getUserMapByName(mapName);

        //now we have the map we need to reconstitute it
        //from its constituent parts
        //get the active layers

        uMap.setActiveLayers(getMapLayersDetails(getActiveLayers(uMap.getId())));
        uMap.setUserDefinedLayers(getMapLayersDetails(getUserDefinedLayers(uMap.getId())));
        //send it back


        return uMap;
    }



    @SuppressWarnings("unchecked")
    @Override
    public List<UserMap> getUserMaps() {

        try {
            // Begin unit of work
            Session sess = this.sessionFactory.getCurrentSession();
            sess.beginTransaction();
            userMaps = (List<UserMap>) sess.createQuery(
                    "from UserMap where userid=?").setLong(0,
                    currentUser.getUserId()).list();

        } catch (HibernateException hex) {

            logger.debug(hex.getMessage());
        }

        return userMaps;
    }

    @SuppressWarnings("unchecked")
    public List<UserSearch> getUserSearches() {

        try {
            // Begin unit of work
            Session sess = sessionFactory.getCurrentSession();
            sess.beginTransaction();
            userSearches = (List<UserSearch>) sess.createQuery(
                    "from UserSearch where userid=?").setLong(0,
                    currentUser.getUserId()).list();

        } catch (HibernateException hex) {

            logger.debug(hex.getMessage());
        }

        return userSearches;
    }

    public UserSearch getUserSearchByName(String searchname) {
        UserSearch us = new UserSearch();

        try {
            // Begin unit of work
            Session sess = sessionFactory.getCurrentSession();
            sess.beginTransaction();
            us = (UserSearch) sess.createQuery(
                    "from UserSearch where searchname=?").setString(0,
                    searchname).uniqueResult();

        } catch (HibernateException hex) {

            logger.debug(hex.getMessage());
        }

        return us;

    }

   public List<MapLayer> getMapLayersDetails(List<MapLayer> lm) {
       List<MapLayer> layers = new ArrayList<MapLayer>();
       
       try {            
            // Begin unit of work
            Session sess = this.sessionFactory.getCurrentSession();
            sess.beginTransaction();

            //get the bits of the layer that it needs
            for (MapLayer m: lm) {
                //get the layer WMSStyles
                List<WMSStyle> ws = sess.createQuery(
                        "from WMSStyle where maplayerid=?").setLong(0,
                        m.getMapLayerId()).list();
                m.setStyles(ws);


                if (m.isCurrentlyAnimated()) {
                    AnimationSelection as = (AnimationSelection) sess.createQuery(
                            "from AnimationSelection where maplayerid=?").setLong(0,
                            m.getMapLayerId()).uniqueResult();
                    m.setAnimationSelection(as);
                    
                    MapLayerMetadata mm = (MapLayerMetadata) sess.createQuery(
                            "from MapLayerMetadata where maplayerid=?").setLong(0,
                            m.getMapLayerId()).uniqueResult();
                    
                    m.setMapLayerMetadata(mm);
                }
                
               layers.add(m);
            }


        } catch (HibernateException ex) {

        }






       return layers;

   }

    public List<MapLayer> getActiveLayers(long mapId) {
        return getMapLayers(mapId, false);
    }

    private List<MapLayer> getMapLayers(long mapId, boolean userDefined) {
        List<MapLayer> layers = new ArrayList<MapLayer>();

        try {
            // Begin unit of work
            Session sess = sessionFactory.getCurrentSession();
            sess.beginTransaction();
            Query query = sess.createQuery("from MapLayer where baselayer=false and userdefinedlayer=? and usermapid=?");
            query.setBoolean(0, userDefined);
            query.setLong(1, mapId);

            layers = (List<MapLayer>) query.list();
        } catch (HibernateException hex) {
            logger.error("error retrieving map layers from db", hex);
        }

        return layers;
    }

    public List<MapLayer> getUserDefinedLayers(long mapId) {
        return getMapLayers(mapId, true);
    }

    public boolean checkMapName(String mapname) {
        boolean bcheck = true;
        // test if the user already has a map of the same name
        for (UserMap u : userMaps) {
            if (u.getMapname().equalsIgnoreCase(mapname)) {
                bcheck = false;
                break;
            }
        }

        return bcheck;
    }


    public boolean checkSearchName(String searchname) {
        boolean bcheck = true;
        // test if the user already has a map of the same name
        for (UserSearch u : userSearches) {
            if (u.getSearchName().equalsIgnoreCase(searchname)) {
                bcheck = false;
                break;
            }
        }

        return bcheck;
    }

    public void saveMap(String mapname) {

        UserMap umap = new UserMap();
        PortalSession ps = getMapComposer().getPortalSession();

        // bit of a nasty hack - obtain a second hand reference to portalsessionutils throug
        // the composer.  Ideally we would become a spring bean and inject it into ourself
        // on initialisation - todo!
        PortalSessionUtilities psUtils = getMapComposer().getPortalSessionUtilities();

        BoundingBox bBox = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();

        // need to save the bounding box before we can
        // add it to the UserMap object
        bBox.setUserMap(umap);
        saveObjectToDB(bBox);
        umap.setMapname(mapname);
        umap.setbBox(bBox);
        umap.setUserid(currentUser);
        saveObjectToDB(umap);

        //get the active layer details and add the list to the object
        umap.setActiveLayers(addMapLayersDetails(ps.getActiveLayers(), umap.getId(), false));

        //get the user defined layer details and add the list to the object
        umap.setUserDefinedLayers(addMapLayersDetails(ps.getUserDefinedLayers(), umap.getId(), true));

        //get the current base layer
        MapLayer ml = ps.getCurrentBaseLayer();
        ml.setUserMapId(umap.getId());
        saveObjectToDB(ml);
        umap.setCurrentBaseLayer(ml);
        updateObjectToDB(umap);

    }


    public List<MapLayer> addMapLayersDetails(List<MapLayer> mapLayers, long umapid, boolean userdef) {
        List<MapLayer> mL = new ArrayList<MapLayer>();

        for (MapLayer mapLayer: mapLayers) {
            mapLayer.setUserMapId(umapid);
            mapLayer.setUserDefinedLayer(userdef);
            saveObjectToDB(mapLayer);
            List<WMSStyle> styles = mapLayer.getStyles();


            //get the styles for this layer and add them

            for (WMSStyle s : styles) {
                s.setMaplayerid(mapLayer.getMapLayerId());
                saveObjectToDB(s);
            }


            //check the metadata of the layer
            //for animated layers
            if (mapLayer.getMapLayerMetadata() != null) {
                MapLayerMetadata mm = mapLayer.getMapLayerMetadata();
                mm.setMaplayerid(mapLayer.getMapLayerId());
                saveObjectToDB(mm);
                mapLayer.setMapLayerMetadata(mm);
            }

            //does this layer have AnimationSelection
            //if so save that too

            if (mapLayer.isCurrentlyAnimated()) {
                AnimationSelection as = mapLayer.getAnimationSelection();
                as.setMaplayerid(mapLayer.getMapLayerId());
                saveObjectToDB(as);
                mapLayer.setAnimationSelection(as);
            }


            //Does this layer have any children
            if (mapLayer.getChildren() != null) {
                for (MapLayer child : mapLayer.getChildren()) {
                    child.setUserMapId(umapid);
                    child.setParentmaplayerid(mapLayer.getMapLayerId());
                }
            }

            updateObjectToDB(mapLayer);
            mL.add(mapLayer);
        }

        return mapLayers;


    }

    public void saveObjectToDB(Object obj) {
        Session sess = this.sessionFactory.getCurrentSession();
        sess.beginTransaction();
        sess.save(obj);
        sess.getTransaction().commit();
    }

    public void deleteObjectFromDb(Object obj) {
        Session sess = this.sessionFactory.getCurrentSession();
        sess.beginTransaction();
        sess.delete(obj);
        sess.getTransaction().commit();
    }

    public void updateObjectToDB(Object obj) {
        Session sess = this.sessionFactory.getCurrentSession();
        sess.beginTransaction();
        sess.update(obj);
        sess.getTransaction().commit();
    }

    public UserMap parsePortalSession() {
        UserMap userMap = new UserMap();
        return userMap;
    }

    public void updateMap(String mapname) {
    }

    public void deleteMap(String mapname) {
        UserMap umap = getUserMapByName(mapname);
        deleteObjectFromDb(umap);
        cleanupMapLayers(umap.getId());

    }

    public void deleteSearch(String searchName) {
        UserSearch search = getUserSearchByName(searchName);
        deleteObjectFromDb(search);

    }


    public void cleanupMapLayers(long mapid) {
        List<MapLayer> mL = new ArrayList<MapLayer>();

        try {
             Session sess = this.sessionFactory.getCurrentSession();
             sess.beginTransaction();
             String s = ("delete from maplayer where usermapid=" + mapid);
             SQLQuery sql = sess.createSQLQuery(s);
             sql.executeUpdate();
             sess.getTransaction().commit();
             logger.debug("Cleaned up the orphaned layers -- thanks SLAYER LAGER");

        } catch (HibernateException hex) {
            logger.error("error retrieving map layers from db", hex);
        }

    }

    public void saveSearch(String searchname, SearchQuery sq) {
        UserSearch u = new UserSearch();
        u.setKeyword(sq.getSearchTerm());
        u.setSearchName(searchname);
        u.setUserId(currentUser.getUserId());

        if (sq.isUseDate()) {
            u.setStartDate(sq.getStartDate());
            u.setEndDate(sq.getEndDate());
        }

        if (sq.isUseBBOX()) {
            u.setEast(sq.getRight());
            u.setWest(sq.getLeft());
            u.setNorth(sq.getTop());
            u.setSouth(sq.getBottom());
        } else {
            u.setEast(-999.0);
            u.setWest(-999.0);
            u.setNorth(-999.0);
            u.setSouth(-999.0);
        }

        saveObjectToDB(u);

    }

    public MapComposer getMapComposer() {
        return (MapComposer) Executions.getCurrent().getDesktop().getPage(
                "MapZul").getFellow("mapPortalPage");
    }

    @Override
    public void setSessionFactory(SessionFactory usersSessionFactory) {
        this.sessionFactory = usersSessionFactory;
    }

}
