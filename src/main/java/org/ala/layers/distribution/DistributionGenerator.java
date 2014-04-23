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

package org.ala.layers.distribution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.ala.layers.util.SpatialUtil;

/**
 * @author Adam
 */
public class DistributionGenerator {
    static int CONCURRENT_THREADS = 6;
    static String db_url = "jdbc:postgresql://localhost:5432/layersdb";
    static String db_usr = "postgres";
    static String db_pwd = "postgres";

    private static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            //String url = "jdbc:postgresql://ala-devmaps-db.vm.csiro.au:5432/layersdb";
            String url = db_url;
            conn = DriverManager.getConnection(url, db_usr, db_pwd);

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return conn;
    }

    static public void main(String[] args) {
        System.out.println("Calculates and fills empty area_km in table distributionshapes.\n\nargs[0] = threadcount, args[1] = db connection string,\n args[2] = db username,\n args[3] = password\n");
        if (args.length >= 4) {
            CONCURRENT_THREADS = Integer.parseInt(args[0]);
            db_url = args[1];
            db_usr = args[2];
            db_pwd = args[3];
        }
        long start = System.currentTimeMillis();
        while (updateArea() > 0) {
            System.out.println("time since start= " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private static int updateArea() {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT id, ST_AsText(the_geom) as wkt FROM distributionshapes WHERE area_km is null"
                    + " limit 100";
            if (conn == null) {
                System.out.println("connection is null");
            } else {
                System.out.println("connection is not null");
            }
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<String[]> data = new LinkedBlockingQueue<String[]>();
            while (rs1.next()) {
                data.put(new String[]{rs1.getString("id"), rs1.getString("wkt")});
            }

            System.out.println("next " + data.size());

            int size = data.size();

            if (size == 0) {
                return 0;
            }

            CountDownLatch cdl = new CountDownLatch(data.size());

            AreaThread[] threads = new AreaThread[CONCURRENT_THREADS];
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j] = new AreaThread(data, cdl, getConnection().createStatement());
                threads[j].start();
            }

            cdl.await();

            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                try {
                    threads[j].s.getConnection().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                threads[j].interrupt();
            }
            return size;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
}

class AreaThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;

    public AreaThread(LinkedBlockingQueue<String[]> lbq, CountDownLatch cdl, Statement s) {
        this.s = s;
        this.cdl = cdl;
        this.lbq = lbq;
    }

    @Override
    public void run() {
        try {
            while (true) {

                try {
                    String[] data = lbq.take();

                    double area = SpatialUtil.calculateArea(data[1]) / 1000.0 / 1000.0;

                    String sql = "UPDATE distributionshapes SET area_km = " + area + " WHERE id='" + data[0] + "';";

                    int update = s.executeUpdate(sql);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cdl.countDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
