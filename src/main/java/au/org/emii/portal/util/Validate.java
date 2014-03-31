package au.org.emii.portal.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.validator.EmailValidator;
import org.apache.commons.validator.UrlValidator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Quick and easy wrapper to do common escaping and validation routines
 *
 * @author geoff
 */
public class Validate {

    /**
     * Check if a passed in string is empty
     *
     * @param string
     * @return false if String is not null, empty or just whitespace, otherwise
     * return true
     */
    public static boolean empty(String string) {
        return (!((string != null) && (!string.matches("\\s*"))));
    }

    /**
     * Escape HTML using StringEscapeUtils and then trim any whitespace
     * from start and end using String class
     */
    public static String escapeHtmlAndTrim(String string) {
        return StringEscapeUtils.escapeHtml(string).trim();
    }

    /**
     * Check a passed in string is NOT a valid http[s] uri.  Remove
     * any whitespace for the test
     *
     * @param uri to be checked
     * @return true if the uri is invalid, otherwise false
     */
    public static boolean invalidHttpUri(String uri) {
        UrlValidator urlValidator = new UrlValidator(
                new String[]{"http", "https"}
        );

        return (!urlValidator.isValid(uri.trim()));

    }

    /**
     * Check if a uri stars with http[s]://, if it doesn't, prepend
     * it and return the new string
     *
     * @param string
     * @return
     */
    public static String prefixUri(String uri) {
        /* idiot proofing: prefix with "http://" if we dont' start
         * with either http:// or https://
		 */
        String fullUri;
        if (!uri.matches("^[Hh][Tt][Tt][Pp][Ss]?://.*")) {
            fullUri = "http://" + uri;
        } else {
            fullUri = uri;
        }

        return fullUri;
    }

    /**
     * Return a DateFormat instance configured for long ISO 8601
     * dates (eg 2004-05-04T23:50:03.045Z)
     *
     * @return
     */
    public static DateFormat getIsoDateFormatter() {
        SimpleDateFormat sd = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        );
        return sd;
    }

    /**
     * Return a DateFormat instance configured for short ISO 8601
     * dates (eg 2004-05-04)
     *
     * @return
     */
    public static SimpleDateFormat getShortIsoDateFormatter() {
        SimpleDateFormat sd = new SimpleDateFormat(
                "yyyy-MM-dd"
        );
        return sd;
    }

    /**
     * Validate an email address - return true for valid, false for invalid.
     * Wraps commons Emailvalidator
     *
     * @param email
     * @return
     */
    public static boolean email(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

}
