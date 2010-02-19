package au.org.emii.portal.userdata;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import au.org.emii.portal.SearchQuery;

public class UserSearches extends UserDataManager{

	private List<UserSearch> userSearches = new ArrayList<UserSearch>();
	private Logger logger = Logger.getLogger(this.getClass());
	

	
}
