package org.ala.layers.stats;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.layers.tabulation.TabulationUtil;
/**
 * This class generates bbox and area_km attributes for entries in the objects table.
 * 
 * @author jac24n
 */
public class StatsGenerator {
    static int CONCURRENT_THREADS = 10;
    static String db_url = "jdbc:postgresql://localhost:5432/layersdb";
    static String db_usr = "postgres";
    static String db_pwd = "postgres";

    private static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = db_url;
            conn = DriverManager.getConnection(url, db_usr, db_pwd);

        } catch (Exception e) {
            System.out.println("Unable to create Connection");
            e.printStackTrace(System.out);
        }

        return conn;
    }
    
    public static void main (String[] args){
        System.out.println("args[0] = threadcount, args[1] = db connection string, args[2] = db username, args[3] = password");
        if(args.length >= 4) {
            CONCURRENT_THREADS = Integer.parseInt(args[0]);
            db_url = args[1];
            db_usr = args[2];
            db_pwd = args[3];
        }
        
        Connection c = getConnection();
        String count_sql = "select count(*) as cnt from objects where bbox is null;";
        int count = 0;
        try{
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery(count_sql);
            while (rs.next()){
                count = rs.getInt("cnt");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        int iter = count/200000;
        
        System.out.println("Breaking into " + iter + " iterations");
        for (int i = 0; i <= iter; i++){
            updateBbox();
            updateArea();
        }
    }

        private static void updateBbox() {

        try {
            Connection conn = getConnection();
            String sql = "SELECT pid from objects where bbox is null limit 250000;";
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<String[]> data = new LinkedBlockingQueue<String[]>();
            while (rs1.next()) {
                data.put(new String[]{rs1.getString("pid")});
            }

            CountDownLatch cdl = new CountDownLatch(data.size());
            
            BboxThread[] threads = new BboxThread[CONCURRENT_THREADS];
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j] = new BboxThread(data, cdl, getConnection().createStatement());
                threads[j].start();
            }

            cdl.await();

            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j].interrupt();
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }
    
        private static void updateArea() {

        try {
            Connection conn = getConnection();
            String sql = "SELECT pid from objects where area_km is null limit 200000;";
            Statement s1 = conn.createStatement();
            ResultSet rs1 = s1.executeQuery(sql);

            LinkedBlockingQueue<String[]> data = new LinkedBlockingQueue<String[]>();
            while (rs1.next()) {
                data.put(new String[]{rs1.getString("pid")});
            }

            CountDownLatch cdl = new CountDownLatch(data.size());

            AreaThread[] threads = new AreaThread[CONCURRENT_THREADS];
            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j] = new AreaThread(data, cdl, getConnection().createStatement());
                threads[j].start();
            }

            cdl.await();

            for (int j = 0; j < CONCURRENT_THREADS; j++) {
                threads[j].interrupt();
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }
}

class BboxThread extends Thread {

    Statement s;
    LinkedBlockingQueue<String[]> lbq;
    CountDownLatch cdl;

    public BboxThread(LinkedBlockingQueue<String[]> lbq, CountDownLatch cdl, Statement s) {
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
                    String sql = "SELECT ST_AsText(ST_EXTENT(the_geom)) as bbox from objects where pid = '" + data[0] + "';";
                    System.out.println("Running " + sql);
                    ResultSet rs = s.executeQuery(sql);                
                    String bbox = "";
                    while (rs.next()) {
                        bbox = rs.getString("bbox");
                    }                    
                    sql = "UPDATE objects set bbox = '" + bbox + "' where pid = '" + data[0] + "';";
                    System.out.println("Running " + sql);
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
                    String sql = "SELECT ST_AsText(the_geom) as wkt from objects where pid = '" + data[0] + "';";
                    System.out.println("Running " + sql);
                    ResultSet rs = s.executeQuery(sql);                
                    String wkt = "";
                    while (rs.next()) {
                        wkt = rs.getString("wkt");
                    }            
                    double area = TabulationUtil.calculateArea(wkt)/1000;

                    sql = "UPDATE objects SET area_km = " + area + " WHERE pid='" + data[0] + "'";
                    System.out.println("Running " + sql);
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
