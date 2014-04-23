/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.pid;

import au.csiro.pidclient.AndsPidClient;
import au.csiro.pidclient.AndsPidClient.HandleType;
import au.csiro.pidclient.AndsPidResponse;
import au.csiro.pidclient.business.AndsPidIdentity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ajay
 */
public class PidGenerator {

    private String DB_DRIVER_DEV = "org.postgresql.Driver";
    private String DB_URL_DEV = "jdbc:postgresql://ala-devmaps-db.vm.csiro.au:5432/layersdb";
    private String DB_USERNAME_DEV = "postgres";
    private String DB_PASSWORD_DEV = "postgres";

    private String DB_DRIVER_PROD = "org.postgresql.Driver";
    private String DB_URL_PROD = "jdbc:postgresql://ala-maps-db.vic.csiro.au:5432/layersdb";
    private String DB_USERNAME_PROD = "postgres";
    private String DB_PASSWORD_PROD = "postgres";

    private static String ANDS_APPID_TEST = "2c6ed180e966774eee8409f7152b0cc885d07f71";
    private static String ANDS_AUTH_DOMAIN_TEST = "csiro.au";
    private static String ANDS_IDENTIFIER_TEST = "ALA";
    private static String ANDS_HOST_TEST = "test.ands.org.au";

    private static String ANDS_APPID_PROD = "2c6ed180e966774eee8409f7152b0cc885d07f71";
    private static String ANDS_AUTH_DOMAIN_PROD = "csiro.au";
    private static String ANDS_IDENTIFIER_PROD = "ALA";
    private static String ANDS_HOST_PROD = "services.ands.org.au";

    private static boolean isProduction = false;

    private Connection getConnection() {
        Connection conn = null;
        String db_driver = DB_DRIVER_DEV;
        String db_url = DB_URL_DEV;
        String db_user = DB_USERNAME_DEV;
        String db_pass = DB_PASSWORD_DEV;
        if (isProduction) {
            db_driver = DB_DRIVER_PROD;
            db_url = DB_URL_PROD;
            db_user = DB_USERNAME_PROD;
            db_pass = DB_PASSWORD_PROD;
        }

        try {
            Class.forName(db_driver);
            String url = db_url;
            //String url = "jdbc:postgresql://localhost:5432/layersdb";
            conn = DriverManager.getConnection(url, db_user, db_pass);

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return conn;
    }

    private void testPidGeneration() {
        try {
            AndsPidIdentity andsid = new AndsPidIdentity();
            andsid.setAppId(ANDS_APPID_TEST);
            andsid.setAuthDomain(ANDS_AUTH_DOMAIN_TEST);
            andsid.setIdentifier(ANDS_IDENTIFIER_TEST);

            AndsPidClient ands = new AndsPidClient();
            ands.setPidServiceHost(ANDS_HOST_TEST);
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

    public static String mintLayerPid(HandleType handleType, String value) {
        try {
            String ands_appid = ANDS_APPID_TEST;
            String ands_auth = ANDS_AUTH_DOMAIN_TEST;
            String ands_ident = ANDS_IDENTIFIER_TEST;
            String ands_host = ANDS_HOST_TEST;
            if (isProduction) {
                ands_appid = ANDS_APPID_PROD;
                ands_auth = ANDS_AUTH_DOMAIN_PROD;
                ands_ident = ANDS_IDENTIFIER_PROD;
                ands_host = ANDS_HOST_PROD;
            }

            AndsPidIdentity andsid = new AndsPidIdentity();
            andsid.setAppId(ands_appid);
            andsid.setAuthDomain(ands_auth);
            andsid.setIdentifier(ands_ident);

            AndsPidClient ands = new AndsPidClient();
            ands.setPidServiceHost(ands_host);
            ands.setPidServicePath("/pids");
            ands.setPidServicePort(8443);
            ands.setRequestorIdentity(andsid);
            AndsPidResponse mintHandleFormattedResponse = ands.mintHandleFormattedResponse(handleType, value);

            //System.out.println("handle creation status: " + mintHandleFormattedResponse.isSuccess());
            //System.out.println(mintHandleFormattedResponse.getXmlResponse());

            return mintHandleFormattedResponse.getHandle();

        } catch (Exception e) {
            System.out.println("Unable to generate PID");
            e.printStackTrace(System.out);
        }

        return null;

    }

    private void startGeneration() {
        System.out.println("starting PID generation...");

        try {
            Connection conn = getConnection();
            String sql = "SELECT id FROM layerpids WHERE pid IS NULL";
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<Statement> statements = new LinkedBlockingQueue<Statement>();
            int CONCURRENT_THREADS = 50;
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                statements.add(conn.createStatement());
            }
            long start = System.currentTimeMillis();

            int i = 0;
            while (rs1.next()) {
                Statement s2 = statements.take();

                new PidThread(rs1.getString("id"), s2, statements).start();

                if (++i == 100) {
                    break;
                }
                i++;

                if (i % 100 == 0) {
                    System.out.println("processed: " + i + " at " + (100 / ((System.currentTimeMillis() - start) / 1000.0)) + " records/s");
                    start = System.currentTimeMillis();
                }
            }

            while (statements.size() > 0) {
                statements.take().close();
            }
        } catch (Exception ex) {
            Logger.getLogger(PidGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }


        System.out.println("Completed PID threading");
    }

    public static void main(String[] args) {

        if (args.length > 0) {
            if (args[0].trim().toLowerCase().equals("production")) {
                isProduction = true;
            }
        }

        PidGenerator pg = new PidGenerator();
        pg.startGeneration();
    }
}

class PidThread extends Thread {

    String id = "";
    Statement s;
    LinkedBlockingQueue<Statement> lbq;

    public PidThread(String id, Statement s, LinkedBlockingQueue<Statement> lbq) {
        this.id = id;
        this.s = s;
        this.lbq = lbq;
    }

    public void run() {
        try {
            String handle = PidGenerator.mintLayerPid(HandleType.DESC, id);
            String sql = "UPDATE layerpids SET pid = '" + handle + "' WHERE id = '" + id + "'";
            int update = s.executeUpdate(sql);

            lbq.put(s);
        } catch (Exception ex) {
            Logger.getLogger(PidThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
