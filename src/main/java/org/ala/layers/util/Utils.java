package org.ala.layers.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Adam
 */
public class Utils {

    /**
     * Log4j instance
     */
    static protected Logger logger = Logger.getLogger("org.ala.layers.util.Utils");

    /**
     * Converts an sql result set into a JSON string
     * @param r Result set to convert
     * @return JSON formatted representation of result set
     */
    static public String resultSetToJSON(ResultSet r) {
        StringBuilder sb = new StringBuilder();
        try {
            ResultSetMetaData md = r.getMetaData();
            String[] headers = new String[md.getColumnCount()+1];
            int[] types = new int[md.getColumnCount()+1];
            for (int i = 1; i < headers.length; i++) {
                headers[i] = md.getColumnLabel(i);
                types[i] = md.getColumnType(i);
            }
            int count = 0;
            sb.append("[");
            while (r != null && r.next()) {
                if (count > 0) {
                    sb.append(",");
                }

                sb.append("{");

                for (int i = 1; i < headers.length; i++) {
                    if (i > 1) {
                        sb.append(",");
                    }
                    sb.append("\"").append(headers[i]).append("\":");

                    if(types[i] == java.sql.Types.BOOLEAN) {
                        sb.append(r.getBoolean(i));
                    } else {
                        //string/default
                        String rsValue = r.getString(i);
                        //perform json character escapes
                        if (rsValue != null){
                            rsValue = rsValue.replace("\"", "\\\"").replace("\\", "\\\\").replace("/", "\\/").replace("\n", "\\n").replace("\t", "\\t");
                        }
                        sb.append("\"").append(rsValue).append("\"");
                    }
                }

                sb.append("}");

                count++;
                logger.debug("Processed json for result set item " + count);
            }
            sb.append("]");
        } catch (Exception e) {
            logger.error("An error occurred rendering result set");
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }

        return sb.toString();
    }
}
