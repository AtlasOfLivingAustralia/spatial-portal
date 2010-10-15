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

    private static final String DATE_FORMAT_NOW = "dd-MM-yyyy HH:mm:ss";

    private static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());

    }

    public static void log(String msg) {
        System.out.println(now() + "> " + msg);
    }

    public static void log(String title, String desc) {
        System.out.println(now() + "> " + title + ": " + desc);
    }

    public static void info(String msg) {
        log(msg);
    }

    public static void info(String msg, String desc) {
        log(msg, desc);
    }
}
