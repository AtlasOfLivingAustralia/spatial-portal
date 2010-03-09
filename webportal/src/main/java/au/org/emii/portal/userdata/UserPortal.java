package au.org.emii.portal.userdata;

import java.io.Serializable;



/**
 *
 * @author brendon
 */
public class UserPortal implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long userId;
    private String userName;
    private UserMap userMap;

    /**
	 * @return the userMap
	 */
	public UserMap getUserMap() {
		return userMap;
	}

	/**
	 * @param userMap the userMap to set
	 */
	public void setUserMap(UserMap userMap) {
		this.userMap = userMap;
	}

	public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    

}