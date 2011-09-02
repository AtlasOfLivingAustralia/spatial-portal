/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.pid;

import au.csiro.pidclient.AndsPidClient;
import au.csiro.pidclient.AndsPidClient.HandleType;
import au.csiro.pidclient.AndsPidResponse;
import au.csiro.pidclient.business.AndsPidIdentity;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import org.postgis.PGgeometry;

/**
 *
 * @author ajay
 */
public class PGBridge {

    public static void loadLayersPid() {
    }

    public static void getLayer() {
        try {
            String sql = "SELECT * FROM layers ";
            ResultSet rs = query(sql);
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                System.out.println(id + ") " + name + " (" + type + ")");

                loadObject("cl" + id, type, "", rs.getString("path"), "", null);

                if (type.equals("Contextual")) {
                    //List<Hashtable<String, Object>> records = getRecord(name, "id");
                    String uniqueFields = getUniqueFields(id);
                    getObjects(id, uniqueFields);
                }
            }

        } catch (Exception e) {
            System.out.println("error getting layers");
            e.printStackTrace(System.out);
        }
    }

    private static void getObjects(String tableId, String uniqueFields) {
        try {
            String sql = "SELECT " + uniqueFields + ",the_geom FROM \"" + tableId + "\"";
            System.out.println(sql);
            String[] fields = uniqueFields.split(",");
            ResultSet rs = query(sql);
            int c = 1;
            while (rs.next()) {
                //String id = rs.getString("id");
                //for (int i=0;i<fields.length;i++) {
                //    System.out.print(rs.getString(i+1) + ",,, ");
                //}

                PGgeometry geom = (PGgeometry) rs.getObject("the_geom");
                System.out.println("Loading with _____" + uniqueFields + "____");
                //System.out.println("Loading " + id + " with " + geom.toString());

                loadObject("cl" + tableId + "_" + c, "Contextual", "", null, "", (PGgeometry) rs.getObject("the_geom"));
                c++;
            }
        } catch (Exception e) {
            System.out.println("error getting objects");
            e.printStackTrace(System.out);
        }
    }

    private static String getUniqueFields(String tableId) {
        try {
            String sql = "SELECT sid FROM fields WHERE spid = '" + tableId + "' ";
            ResultSet rs = query(sql);
            while (rs.next()) {
                String id = rs.getString("sid");
                return id;
            }
        } catch (Exception e) {
            System.out.println("Unable to get ID field");
            e.printStackTrace(System.out);
        }

        return null;
    }

    private static void loadObject(String id, String type, String unique, String path, String metadata, PGgeometry geom) {
        try {
            String sql = "";
            int typeInt = 1;

            if (type.equals("Contextual")) {
                typeInt = 1;
            } else if (type.equals("Environmental")) {
                typeInt = 2;
            } else if (type.equals("Checklist")) {
                typeInt = 3;
            } else if (type.equals("Shapefile")) {
                typeInt = 4;
            } else if (type.equals("Distribution")) {
                typeInt = 5;
            }

            if (geom != null) {
                sql = "INSERT INTO layerpids (id, type, the_geom) values (";
                sql += "'" + id + "',";
                sql += typeInt + ",";
                //sql += "'"+unique+"',";
                sql += "'" + geom + "'";
                sql += ")";
            } else if (path != null) {
                sql = "INSERT INTO layerpids (id, type, path) values ( ";
                sql += "'" + id + "',";
                sql += typeInt + ",";
                //sql += "'"+unique+"',";
                sql += "'" + path + "'";
                sql += ")";
            }

            Connection conn = getConnection();
            Statement s = conn.createStatement();

            s.executeUpdate(sql);

        } catch (Exception e) {
            System.out.println("Unable to load objects into table");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Gets PostGIS data given the 'name' attribute value
     *
     * @param name The 'name' attribute value to lookup
     * 
     */
    public static List<Hashtable<String, Object>> getRecord(String name, String idattr) {
        Connection conn;
        List<Hashtable<String, Object>> records = null;
        try {
            /*
             * Load the JDBC driver and establish a connection.
             */
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/layersdb";
            conn = DriverManager.getConnection(url, "postgres", "P0stgres");
            /*
             * Add the geometry types to the connection. Note that you
             * must cast the connection to the pgsql-specific connection
             * implementation before calling the addDataType() method.
             */
            ((org.postgresql.PGConnection) conn).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
            ((org.postgresql.PGConnection) conn).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));

            records = new Vector<Hashtable<String, Object>>();
            /*
             * Create a statement and execute a select query.
             */
            Statement s = conn.createStatement();
            ResultSet r = s.executeQuery("select " + idattr + ", astext(the_geom), the_geom from " + name);
            while (r.next()) {
                Hashtable<String, Object> record = new Hashtable<String, Object>();
                /*
                 * Retrieve the geometry as an object then cast it to the geometry type.
                 * Print things out.
                 */
                String id = r.getString(1);
                String geomtext = r.getString(2);
                PGgeometry geom = (PGgeometry) r.getObject(3);
                //System.out.println("Row " + id + ":");
                //System.out.println(geom.toString());
                record.put("id", id);
                record.put("geomtext", geomtext);
                record.put("geom", geom.toString());

                records.add(record);

            }
            s.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Unable to load geometry record");
            e.printStackTrace(System.out);
        }

        return records;
    }

    private static Connection getConnection() {
        Connection conn = null;
        try {
            /*
             * Load the JDBC driver and establish a connection.
             */
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/layersdb";
            conn = DriverManager.getConnection(url, "postgres", "P0stgres");
            /*
             * Add the geometry types to the connection. Note that you
             * must cast the connection to the pgsql-specific connection
             * implementation before calling the addDataType() method.
             */
            ((org.postgresql.PGConnection) conn).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
            ((org.postgresql.PGConnection) conn).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return conn;
    }

    private static ResultSet query(String sql) {
        try {
            /*
             * Load the JDBC driver and establish a connection.
             */
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/layersdb";
            Connection conn = DriverManager.getConnection(url, "postgres", "P0stgres");
            /*
             * Add the geometry types to the connection. Note that you
             * must cast the connection to the pgsql-specific connection
             * implementation before calling the addDataType() method.
             */
            ((org.postgresql.PGConnection) conn).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
            ((org.postgresql.PGConnection) conn).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));

            /*
             * Create a statement and execute a select query.
             */
            Statement s = conn.createStatement();
            //ResultSet r = s.executeQuery("select " + idattr + ", astext(the_geom), the_geom from " + name);

            return s.executeQuery(sql);

            //return DBConnection.query(sql);

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return null;
    }

    public static void generateLayerPids() {
        try {
            Connection conn = getConnection();
            String sql = "SELECT id FROM layerpids WHERE pid IS NULL";
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);
            while (rs1.next()) {
                String id = rs1.getString("id");
                System.out.println("Generating handle for '" + id + "': ");
                System.out.println("****************************");
                String handle = mintLayerPid(HandleType.DESC, id);
                System.out.println("****************************");
                System.out.println(handle);

                Statement s2 = conn.createStatement();
                int update = s2.executeUpdate("UPDATE layerpids SET pid = '"+handle+"' WHERE id = '"+id+"'");

                System.out.println("Updated: " + update);
                System.out.println("****************************");

                s2.close(); 
            }
            rs1.close();
            s1.close();
            conn.close(); 
        } catch (Exception e) {
            System.out.println("\nError generating handle...");
            e.printStackTrace(System.out);
        }
    }

    public static String mintLayerPid(HandleType handleType, String value) {
        try {
            AndsPidIdentity andsid = new AndsPidIdentity();
            andsid.setAppId("2c6ed180e966774eee8409f7152b0cc885d07f71");
            andsid.setAuthDomain("csiro.au");
            andsid.setIdentifier("ALA");

            AndsPidClient ands = new AndsPidClient();
            ands.setPidServiceHost("test.ands.org.au");
            ands.setPidServicePath("/pids");
            ands.setPidServicePort(8443);
            ands.setRequestorIdentity(andsid);
            AndsPidResponse mintHandleFormattedResponse = ands.mintHandleFormattedResponse(handleType, value);

            return mintHandleFormattedResponse.getHandle();

            //System.out.println("handle creation status: " + mintHandleFormattedResponse.isSuccess());
            //System.out.println(mintHandleFormattedResponse.getXmlResponse());

        } catch (Exception e) {
            System.out.println("Unable to generate PID");
            e.printStackTrace(System.out);
        }

        return null;

    }

    private static void testPidGeneration() {
        try {
            AndsPidIdentity andsid = new AndsPidIdentity();
            andsid.setAppId("2c6ed180e966774eee8409f7152b0cc885d07f71");
            andsid.setAuthDomain("csiro.au");
            andsid.setIdentifier("ALA");

            AndsPidClient ands = new AndsPidClient();
            ands.setPidServiceHost("test.ands.org.au");
            ands.setPidServicePath("/pids");
            ands.setPidServicePort(8443);
            ands.setRequestorIdentity(andsid);
            AndsPidResponse mintHandleFormattedResponse = ands.mintHandleFormattedResponse(AndsPidClient.HandleType.DESC, "test");

            System.out.println("handle creation status: " + mintHandleFormattedResponse.isSuccess());
            System.out.println(mintHandleFormattedResponse.getXmlResponse());

        } catch (Exception e) {
            System.out.println("Unable to generate PID");
            e.printStackTrace(System.out);
        }

    }

    //static ConnectionPool source = null;
    public static void main(String[] args) {
//        source = new ConnectionPool();
//        source.setDataSourceName("A Data Source");
//        source.setServerName("localhost");
//        source.setDatabaseName("test");
//        source.setUser("testuser");
//        source.setPassword("testpassword");
//        source.setMaxConnections(10);
        //getLayer();

        generateLayerPids();
    }
}
