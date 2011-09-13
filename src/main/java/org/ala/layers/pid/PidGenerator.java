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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ajay
 */
public class PidGenerator {

    private static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://ala-devmaps-db.vm.csiro.au:5432/layersdb";
            //String url = "jdbc:postgresql://localhost:5432/layersdb";
            conn = DriverManager.getConnection(url, "postgres", "postgres");

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return conn;
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

            //System.out.println("handle creation status: " + mintHandleFormattedResponse.isSuccess());
            //System.out.println(mintHandleFormattedResponse.getXmlResponse());

            return mintHandleFormattedResponse.getHandle();

        } catch (Exception e) {
            System.out.println("Unable to generate PID");
            e.printStackTrace(System.out);
        }

        return null;

    }

    public static void main(String[] args) {
        System.out.println("starting PID generation...");

        try {
        Connection conn = getConnection();
        String sql = "SELECT id FROM layerpids WHERE pid IS NULL";
        Statement s1 = conn.createStatement();
        ResultSet rs1 = s1.executeQuery(sql);

        LinkedBlockingQueue<Statement> statements = new LinkedBlockingQueue<Statement>();
        int CONCURRENT_THREADS = 50;
        for(int j=0;j<CONCURRENT_THREADS;j++) {
            statements.add(conn.createStatement());
        }
        long start = System.currentTimeMillis();

            int i=0;
            while (rs1.next()) {
                Statement s2 = statements.take();

                new PidThread(rs1.getString("id"), s2, statements).start();

                if (++i==100) break;
                i++;

                if(i % 100 == 0) {
                    System.out.println("processed: " + i + " at " + (100 / ((System.currentTimeMillis() - start) / 1000.0)) + " records/s");
                    start = System.currentTimeMillis();
                }
            }

            while(statements.size() > 0) {
                statements.take().close();
            }
        } catch (Exception ex) {
            Logger.getLogger(PidGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }



        System.out.println("Completed PID threading");
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
            String sql = "UPDATE layerpids SET pid = '"+handle+"' WHERE id = '"+id+"'";
            int update = s.executeUpdate(sql);

            lbq.put(s);
        } catch (Exception ex) {
            Logger.getLogger(PidThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
