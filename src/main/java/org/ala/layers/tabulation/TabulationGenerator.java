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

package org.ala.layers.tabulation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.layers.dao.Records;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;

/**
 *
 * @author Adam
 */
public class TabulationGenerator {
    static int CONCURRENT_THREADS = 6;
    static String db_url = "jdbc:postgresql://localhost:5432/layersdb";
    //static String db_url = "jdbc:postgresql://ala-devmaps-db.vm.csiro.au:5432/layersdb";
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

    static public void main(String[] args) throws IOException {
        System.out.println("args[0] = threadcount,"
                + "\nargs[1] = db connection string,"
                + "\n args[2] = db username,"
                + "\n args[3] = password,"
                + "\n args[4] = (optional) specify one step to run, "
                + "'1' pair objects, '3' delete invalid objects, '4' area, '5' occurrences");
        if(args.length >= 4) {
            CONCURRENT_THREADS = Integer.parseInt(args[0]);
            db_url = args[1];
            db_usr = args[2];
            db_pwd = args[3];
        }

        if(args.length <= 4) {
            updatePairObjects();

//            updateSingleObjects();

            deleteInvalidObjects();

            long start = System.currentTimeMillis();
            while (updateArea() > 0) {
                System.out.println("time since start= " + (System.currentTimeMillis() - start) + "ms");
            }
        } else if(args[4].equals("1")) {
            updatePairObjects();
        } else if(args[4].equals("2")) {
//            updateSingleObjects();
        } else if(args[4].equals("3")) {
            deleteInvalidObjects();
        } else if(args[4].equals("4")) {
            long start = System.currentTimeMillis();
            while (updateArea() > 0) {
                System.out.println("time since start= " + (System.currentTimeMillis() - start) + "ms");
            }
        } else if(args[5].equals("5")) {
            long start = System.currentTimeMillis();
            Records records = new Records("/Users/fan03c/biochache_records/_records.csv");
            while (updateOccurrencesSpecies(records) > 0) {
                System.out.println("time since start= " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private static void updatePairObjects() {
        Connection conn = null;
        try {
            conn = getConnection();
            String allFidPairs = "SELECT (CASE WHEN f1.id < f2.id THEN f1.id ELSE f2.id END) as fid1, (CASE WHEN f1.id < f2.id THEN f2.id ELSE f1.id END) as fid2 FROM fields f1, fields f2 WHERE f1.id != f2.id AND f1.intersect=true AND f2.intersect=true";
            String existingFidPairs = "SELECT fid1, fid2 FROM tabulation WHERE pid1 is null";
            String newFidPairs = "SELECT a.fid1, a.fid2 FROM (" + allFidPairs + ") a LEFT JOIN (" + existingFidPairs + ") b ON a.fid1=b.fid1 AND a.fid2=b.fid2 WHERE b.fid1 is null group by a.fid1, a.fid2;";
            String sql = newFidPairs;
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<String> data = new LinkedBlockingQueue<String>();
            while (rs1.next()) {
                data.put(rs1.getString("fid1") + "," + rs1.getString("fid2"));
            }

            System.out.println("next " + data.size());

            int size = data.size();

            if (size == 0) {
                return;
            }

            CountDownLatch cdl = new CountDownLatch(data.size());

            DistributionThread[] threads = new DistributionThread[CONCURRENT_THREADS];
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j] = new DistributionThread(getConnection().createStatement(), data, cdl);
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

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    private static void updateSingleObjects() {
//        Connection conn = null;
//        try {
//            conn = getConnection();
//            String allFidPairs = "SELECT id as fid1 FROM fields f WHERE f.intersect=true";
//            String existingFidPairs = "SELECT fid1 FROM tabulation WHERE fid2 = '' GROUP BY fid1";
//            String newFidPairs = "SELECT a.fid1 FROM (" + allFidPairs + ") a LEFT JOIN (" + existingFidPairs + ") b "
//                    + "ON a.fid1=b.fid1 WHERE b.fid1 is null group by a.fid1";
//            String sql = newFidPairs;
//            Statement s1 = conn.createStatement();
//            ResultSet rs1 = s1.executeQuery(sql);
//
//            LinkedBlockingQueue<String> data = new LinkedBlockingQueue<String>();
//            while (rs1.next()) {
//                data.put(rs1.getString("fid1"));
//            }
//
//            System.out.println("next " + data.size());
//
//            int size = data.size();
//
//            if (size == 0) {
//                return;
//            }
//
//            CountDownLatch cdl = new CountDownLatch(data.size());
//
//            SingleDistributionThread[] threads = new SingleDistributionThread[CONCURRENT_THREADS];
//            for (int j = 0; j < CONCURRENT_THREADS; j++) {
//                threads[j] = new SingleDistributionThread(getConnection().createStatement(), data, cdl);
//                threads[j].start();
//            }
//
//            cdl.await();
//
//            for (int j = 0; j < CONCURRENT_THREADS; j++) {
//                try {
//                    threads[j].s.getConnection().close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                threads[j].interrupt();
//            }
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            if(conn != null) {
//                try {
//                    conn.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    private static int updateArea() {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT pid1, pid2, ST_AsText(the_geom) as wkt FROM tabulation WHERE pid1 is not null AND area is null "
                    + " limit 100";
            if(conn == null) {
                System.out.println("connection is null");
            } else {
                System.out.println("connection is not null");
            }
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<String[]> data = new LinkedBlockingQueue<String[]>();
            while (rs1.next()) {
                data.put(new String[]{rs1.getString("pid1"), rs1.getString("pid2"), rs1.getString("wkt")});
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
            if(conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    private static int updateOccurrencesSpecies(Records records) {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT pid1, pid2, ST_AsText(the_geom) as wkt FROM tabulation WHERE pid1 is not null AND occurrences is null "
                    + " limit 100";
            if(conn == null) {
                System.out.println("connection is null");
            } else {
                System.out.println("connection is not null");
            }
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);
            
            LinkedBlockingQueue<String[]> data = new LinkedBlockingQueue<String[]>();
            while (rs1.next()) {
                data.put(new String[]{rs1.getString("pid1"), rs1.getString("pid2"), rs1.getString("wkt")});
            }

            System.out.println("next " + data.size());

            int size = data.size();

            if (size == 0) {
                return 0;
            }

            CountDownLatch cdl = new CountDownLatch(data.size());
            
            

            OccurrencesSpeciesThread[] threads = new OccurrencesSpeciesThread[CONCURRENT_THREADS];
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                
                threads[j] = new OccurrencesSpeciesThread(data, cdl, getConnection().createStatement(), records);
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
            if(conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
    
    private static void deleteInvalidObjects() {
        Connection conn = null;
        try {
            String sql = "delete from tabulation where the_geom is not null and st_area(the_geom) = 0;";
            conn = getConnection();
            conn.createStatement().executeQuery(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
}

class DistributionThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String> lbq;
    CountDownLatch cdl;

    public DistributionThread(Statement s, LinkedBlockingQueue<String> lbq, CountDownLatch cdl) {
        this.s = s;
        this.lbq = lbq;
        this.cdl = cdl;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String f = lbq.take();
                String fid1 = f.split(",")[0];
                String fid2 = f.split(",")[1];

                String sql = "INSERT INTO tabulation (fid1, pid1, fid2, pid2, the_geom) "
                        + "SELECT '" + fid1 + "', o1.pid, '" + fid2 + "', o2.pid, "
                        + "ST_INTERSECTION(o1.the_geom, o2.the_geom)"
                        + "FROM (select * from objects where fid='" + fid1 + "') o1 INNER JOIN "
                        + "(select * from objects where fid='" + fid2 + "') o2 ON ST_Intersects(o1.the_geom, o2.the_geom);";

                String placeholder_sql = "INSERT INTO tabulation (fid1, fid2) VALUES ('" + fid1 + "','" + fid2 + "');";
                System.out.println("start: " + fid1 + "," + fid2);
                long start = System.currentTimeMillis();
                int update = s.executeUpdate(sql);
                update = s.executeUpdate(placeholder_sql);
                long end = System.currentTimeMillis();
                System.out.println("processed: " + fid1 + "," + fid2 + " in " + (end - start) / 1000 + "s");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            cdl.countDown();
        }
    }
}

class SingleDistributionThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String> lbq;
    CountDownLatch cdl;

    public SingleDistributionThread(Statement s, LinkedBlockingQueue<String> lbq, CountDownLatch cdl) {
        this.s = s;
        this.lbq = lbq;
        this.cdl = cdl;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String fid = lbq.take();

                String single_column_sql = "INSERT INTO tabulation (fid1, pid1, fid2, pid2, the_geom) "
                        + "SELECT fid, pid, '', '', the_geom FROM objects WHERE fid='" + fid + "'";

                s.executeUpdate(single_column_sql);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            cdl.countDown();
        }
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

                    double area = TabulationUtil.calculateArea(data[2]);

                    String sql = "UPDATE tabulation SET area = " + area + " WHERE pid1='" + data[0] + "' AND pid2='" + data[1] + "';";

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
/*
 * class OccurrencesSpeciesThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;

    public OccurrencesSpeciesThread(LinkedBlockingQueue<String[]> lbq, CountDownLatch cdl, Statement s) {
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
                    Records records = new Records("/Users/fan03c/biochache_records/biochache_records.csv", data[2]);
                    int occurrences = records.getRecordsSize();
                    int species = records.getSpeciesSize();

                    String sqlUpdate = "UPDATE tabulation SET occurrences = " + occurrences + ", species = " + species + " WHERE pid1='" + data[0] + "' AND pid2='" + data[1] + "';";

                    int update = s.executeUpdate(sqlUpdate);
                    
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
*/
class OccurrencesSpeciesThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;
    Records r;

    public OccurrencesSpeciesThread(LinkedBlockingQueue<String[]> lbq, CountDownLatch cdl, Statement s, Records r) {
        this.s = s;
        this.cdl = cdl;
        this.lbq = lbq;
        this.r = r;
    }

    @Override
    public void run() {
        try {
            while (true) {

                try {
                    String[] data = lbq.take();
                    
                    SimpleRegion areaWorking = SimpleShapeFile.parseWKT(data[2]);
                    int recordsLength = r.getRecordsSize();                    
                    int occurrences = 0;
                    
                    /*ArrayList<String> speciesName = new ArrayList<String>();
                    for (int i = 0;i < recordsLength;i++){
                        double longitude = r.getLongitude(i);
                        double latitude = r.getLatitude(i);
                        if (areaWorking.isWithin(longitude, latitude)){
                            occurrences++;
                            if (speciesName.contains(r.getSpecies(i))==false){
                                speciesName.add(r.getSpecies(i));
                            }                            
                        }
                    }
                    int species = speciesName.size();
                    * 
                    */
                    BitSet speciesSet = new BitSet(r.getSpeciesSize());
                    for (int i = 0;i < recordsLength;i++){
                        double longitude = r.getLongitude(i);
                        double latitude = r.getLatitude(i);
                        if (areaWorking.isWithin(longitude, latitude)){
                            occurrences++;
                            speciesSet.set(r.getSpeciesNumber(i));                       
                        }
                    }
                    int species = speciesSet.cardinality();
                                        
                    String sqlUpdate = "UPDATE tabulation SET occurrences = " + occurrences + ", species = " + species + " WHERE pid1='" + data[0] + "' AND pid2='" + data[1] + "';";

                    int update = s.executeUpdate(sqlUpdate);
                    
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
