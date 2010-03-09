/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.session;

import java.io.Serializable;

/**
 *
 * @author geoff
 */
public interface PortalUser extends Serializable {

    /**
     * Not logged in
     */
    public final static int USER_NOT_LOGGED_IN = -1;

    /**
     * Constant indicating a normal user
     */
    public final static int USER_REGULAR = 0;

    /**
     * Constant indicating an admin user.
     */
    public final static int USER_ADMIN = 1;


    /**
     * single line address field
     * @return
     */
    public String getAddress();
    public void setAddress(String address);

    /**
     * State
     * @return
     */
    public String getState();
    public void setState(String state);

    /**
     * Country
     * @return
     */
    public String getCountry();
    public void setCountry(String country);

    /**
     * Email
     * @return
     */
    public String getEmail();
    public void setEmail(String email);

    /**
     * First name
     * @return
     */
    public String getFirstName();
    public void setFirstName(String firstName);

    /**
     * Type of user - relates to the USER_ constants which are used to determine
     * access levels.
     * @return
     */
    public int getType();
    public void setType(int type);

    /**
     * Conveience method - is user logged in?
     * @return
     */
    public boolean isLoggedIn();

    /**
     * Conveience method - is user an administraotr
     * @return
     */
    public boolean isAdmin();
   

    /**
     * Last name
     * @return
     */
    public String getLastName();
    public void setLastName(String lastName);

    /**
     * Organisation / department
     * @return
     */
    public String getOrganisation();
    public void setOrganisation(String organisation);

    /**
     * Username - users registered through the portal will default to using the
     * email address for this but will be given the option of setting their own
     * @return
     */
    public String getUsername();
    public void setUsername(String username);

    /**
     * ZIP code
     * @return
     */
    public String getZip();
    public void setZip(String zip);

}
