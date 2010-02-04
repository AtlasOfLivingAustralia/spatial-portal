package au.org.emii.portal.userdata;

import java.io.Serializable;

/**
 *
 * @author brendon
 */
public class User implements Serializable {
    private short userId;
    private String userName;

    public short getUserId() {
        return userId;
    }

    public void setUserId(short userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}