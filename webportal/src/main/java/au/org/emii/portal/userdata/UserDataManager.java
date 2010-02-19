/**
 * 
 */
package au.org.emii.portal.userdata;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.zkoss.zk.ui.Executions;

import au.org.emii.portal.BoundingBox;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.PortalSession;
import au.org.emii.portal.WMSStyle;
import au.org.emii.portal.composer.MapComposer;

/**
 * @author brendon
 * 
 */
public class UserDataManager implements Serializable {

	private UserPortal currentUser = new UserPortal();
	private List<UserMap> userMaps = new ArrayList<UserMap>();
	private List<UserSearch> userSearches = new ArrayList<UserSearch>();
	// private MapComposer mapComposer = new MapComposer();

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
			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();
			sess.beginTransaction();
			currentUser = (UserPortal) sess.createQuery(
					"from UserPortal where username=?").setString(0, userName)
					.uniqueResult();

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

	@SuppressWarnings("unchecked")
	public List<UserMap> getUserMaps() {

		try {
			// Begin unit of work
			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();
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
			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();
			sess.beginTransaction();
			userSearches = (List<UserSearch>) sess.createQuery(
					"from UserSearch where userid=?").setLong(0,
					currentUser.getUserId()).list();

		} catch (HibernateException hex) {

			logger.debug(hex.getMessage());
		}

		return userSearches;
	}

	public void saveMap(String mapname) {
		boolean bcheck = true;
		// test if the user already has a map of the same name

		for (UserMap u : userMaps) {
			if (u.getMapname().equalsIgnoreCase(mapname)) {
				getMapComposer().showMessage("Map exists \n " +
						"You currently have a map with that name \n" +
						"Choose another name or Update");
				
				bcheck = false;
				break;

			}
		}

		if (bcheck) {
			UserMap umap = new UserMap();
			PortalSession ps = getMapComposer().getPortalSession();
			BoundingBox bBox = ps.getCurrentBoundingBox();

			// need to save the buonding box before we can
			// add it to the UserMap object
			saveObjectToDB(bBox);
						
			List<MapLayer> activeLayers = ps.getActiveLayers();			
			for (MapLayer mapLayer : activeLayers) {
				saveObjectToDB(mapLayer);
			}
			
			umap.setActiveLayers(activeLayers);

			umap.setMapname(mapname);
			umap.setbBox(ps.getCurrentBoundingBox());
			umap.setUserid(currentUser);

			saveObjectToDB(umap);
		}

	}

	public void saveObjectToDB(Object obj) {
		Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
		sess.beginTransaction();
		sess.save(obj);
		sess.getTransaction().commit();
	}

	public UserMap parsePortalSession() {
		UserMap userMap = new UserMap();

		return userMap;

	}

	public void updateMap(String mapname) {

	}

	public void deleteMap(String mapname) {

	}
	
	public long getNextVal() {
		String sQuery = "SELECT nextval('userdata.userdata_id_seq')";
		Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
		sess.beginTransaction();
		 
		BigInteger i = (BigInteger)sess.createSQLQuery(sQuery).uniqueResult();
		
		return i.longValue();
	}

	public MapComposer getMapComposer() {
		return (MapComposer) Executions.getCurrent().getDesktop().getPage(
				"MapZul").getFellow("mapPortalPage");
	}

}
