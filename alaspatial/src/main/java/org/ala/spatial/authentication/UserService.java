package org.ala.spatial.authentication;

/**
 * Helper class for getting/setting user management
 * and session management data
 *
 * @author ajayr
 */
public interface UserService {

    /**
     * Get a particular session data.
     * @param sid Key for the session param id
     * @return Object value for the requested session param id
     */
    public Object getSessionInfo(String sid);

    /**
     * Set a particular session data.
     * @param sid Key for the session param id
     * @param obj Value for the session param id
     */
    public void setSessionInfo(String sid, Object obj);
}
