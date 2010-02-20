package au.org.emii.portal.service;

import au.org.emii.portal.config.Settings;
import java.util.Map;

/**
 * Define constants used to record service exit status
 * @author geoff
 */
public interface ServiceStatusCodes {

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



}
