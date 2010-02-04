package au.org.emii.portal.webservice;

import java.util.Map;

/**
 * Common interface for all user management web services.  The purpose of this
 * interface is to ensure that the implementing class gets a UserManagementWebService
 * instance set automatically by Spring.  The UserManagementWebService class is
 * just a bunch of String URLs to request data from (the http endpoints)
 * @author geoff
 */
public interface UserManagementWebServiceEndpoint {

    // constants to indicate processing result of web service requests

    /**
     * Operation completed successfully
     */
    public final static int SUCCESS             = 0;

    /**
     * Operation failed - invalid username or password
     */
    public final static int FAIL_INVALID        = 1;

    /**
     * Operation failed - error talking to web service
     */
    public final static int FAIL_UNKNOWN        = 2;

    /**
     * Operation failed - some data is missing
     */
    public final static int FAIL_INCOMPLETE     = 3;

    /**
     * Operation failed - duplicate request
     */
    public final static int FAIL_DUPLICATE      = 4;

     /**
     * operation failed - request used or expired
     */
    public final static int FAIL_EXPIRED        = 5;

    /**
     * Return the map of all available UserManagementServices
     * @return
     */
    public Map<String, UserManagementWebService> getUserManagementServices();

    /**
     * Set the supported UserManagementWebService instances - these contain
     * Strings showing where to make http web service requests to
     * @param services
     */
    public void setUserManagementServices(Map<String, UserManagementWebService> services);

    /**
     * Set the default user management service by identifying a key in the Map
     * of user management web services, as set with setUserManagementServices(...)
     * @param service
     */
    public void setDefaultUserManagementService(UserManagementWebService service);

    /**
     * Identify the default user management web service in use by identifying the
     * key used in the Map.
     * @return
     */
    public UserManagementWebService getDefaultUserManagementService();

        /**
     * Set the default user management service by identifying a key in the Map
     * of user management web services, as set with setUserManagementServices(...)
     * @param service
     */
    public void setDefaultUserManagementServiceId(String service);

    /**
     * Identify the default user management web service in use by identifying the
     * key used in the Map of UserManagementWebService.
     * @return
     */
    public String getDefaultUserManagementServiceId();

    /**
     * Return a UserManagementWebService instance by its key in the Map
     * @param id
     * @return
     */
    public UserManagementWebService getUserManagementWebServiceById(String id);
}
