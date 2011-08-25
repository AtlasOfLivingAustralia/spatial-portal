/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Adam
 */
public class Field {
    /**
     * Log4j instance
     */
    protected static Logger logger = Logger.getLogger("org.ala.layers.util.Field");
    
    static final long minRefreshTime = 120000; //minimum refresh time in ms
    static HashMap<String, Field> fields;
    static long lastRefresh;
    private final String name;
    private final String id;
    private final String desc;
    private final String type;
    private final String spid;
    private final String sid;
    private final String sname;
    private final String sdesc;
    private final boolean isdb;

    private static HashMap<String, Field> getDBFields() {
        HashMap<String, Field> fs = new HashMap<String, Field>();

        ResultSet rs = DBConnection.query("SELECT * FROM fields WHERE enabled=TRUE;");
        try {
            while (rs != null && rs.next()) {
                fs.put(rs.getString("id"), new Field(
                        rs.getString("name"),
                        rs.getString("id"),
                        rs.getString("desc"),
                        rs.getString("type"),
                        rs.getString("spid"),
                        rs.getString("sid"),
                        rs.getString("sname"),
                        rs.getString("sdesc"),
                        rs.getBoolean("isdb")));
            }
        } catch (SQLException ex) {
            logger.error("An error has occurred retrieving fields");
            logger.error(ExceptionUtils.getFullStackTrace(ex));
        }

        return fs;
    }

    static public Field getField(String id) {
        Field f = null;
        if (fields == null
                || ((f = fields.get(id)) == null
                && minRefreshTime + System.currentTimeMillis() > lastRefresh)) {

            refreshFields();

            f = fields.get(id);
        }

        return f;
    }

    static void refreshFields() {
        //load/reload fields table
        HashMap<String, Field> fs = getDBFields();

        if (fs != null && fs.size() > 0) {
            lastRefresh = System.currentTimeMillis();
            fields = fs;
        }
    }

    private Field(String name, String id, String desc, String type, String spid, String sid, String sname, String sdesc, boolean isdb) {
        this.name = name;
        this.id = id;
        this.desc = desc;
        this.type = type;
        this.spid = spid;
        this.sid = sid;
        this.sname = sname;
        this.sdesc = sdesc;
        this.isdb = isdb;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getSrcName() {
        return sname;
    }

    public String getSrcId() {
        return sid;
    }

    public String getSrcPid() {
        return spid;
    }

    public String getSrcDesc() {
        return sdesc;
    }

    public boolean isDb() {
        return isdb;
    }
}
