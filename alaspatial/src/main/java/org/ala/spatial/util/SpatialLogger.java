package org.ala.spatial.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Helper class to log functions/methods to the database
 * for various user-management analysis
 *
 * @author ajayr
 */
public class SpatialLogger {

    private final String DATE_FORMAT_NOW = "dd-MM-yyyy HH:mm:ss";

    private String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());

    }

    public void log(String msg) {
        System.out.println(now() + "> " + msg);
    }

    public void log(String title, String desc) {
        System.out.println(now() + "> " + title + ": " + desc);
    }

    
}
