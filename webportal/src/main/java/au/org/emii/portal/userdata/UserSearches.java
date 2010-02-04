package au.org.emii.portal.userdata;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import au.org.emii.portal.SearchQuery;

public class UserSearches {

	private List<UserSearch> userSearches = new ArrayList<UserSearch>();
	private Logger logger = Logger.getLogger(this.getClass());
	private User user = new User();

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<UserSearch> getUserSearches() {
		return userSearches;
	}

	public void setUserSearches(List<UserSearch> userSearches) {
		this.userSearches = userSearches;
	}

	public User fetchUser(String userName) {

		try {
			// Begin unit of work
			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();
			user = (User) sess.createQuery("from users where name=?")
					.setString(0, userName).uniqueResult();

		} catch (HibernateException hex) {

			logger.debug(hex.getMessage());
		}

		return user;

	}

	@SuppressWarnings("unchecked")
	public List<UserSearch> fetchUserSearches(String userName) {

		try {
			// get the userId
			int userId = fetchUser(userName).getUserId();

			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();

			userSearches = (List<UserSearch>) sess.createQuery(
					"from usersearches where=?").setInteger(0, userId);

			// End unit of work
		} catch (Exception ex) {
			// cleanup the hibernate object
			HibernateUtil.getSessionFactory().getCurrentSession()
					.getTransaction().rollback();

		}

		return userSearches;

	}
	
	public void saveUserSearch(String userName, SearchQuery sq) {
		
		try {
			
			UserSearch userSearch = new UserSearch();
			
			// get the userId
			short userId = fetchUser(userName).getUserId();

			Session sess = HibernateUtil.getSessionFactory()
					.getCurrentSession();
			
			userSearch.setUserId(userId);
			
			
		} catch (Exception ex) {
			
		}
		
		
	}

}
