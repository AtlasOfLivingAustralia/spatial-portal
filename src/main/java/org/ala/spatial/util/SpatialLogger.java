/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Helper class to log functions/methods to the database for various
 * user-management analysis
 *
 * @author ajayr
 */
public class SpatialLogger {

    private static final String DATE_FORMAT_NOW = "dd-MM-yyyy HH:mm:ss:SSS";

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
