/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.analysis.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialLogger;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
public class LayerDistanceIndex  implements AnalysisIndexService {

    final public static String LAYER_DISTANCE_FILE = "layerDistances.dat";
    final public static String LAYER_DISTANCE_FILE_CSV = "layerDistances.csv";
    final public static String LAYER_DISTANCE_FILE_CSV_RAWNAMES = "layerDistancesRawNames.csv";
    
    double [][] min;
    double [][] max;
    double [][] range;
    Grid [] grids;

    static double [][] all_measures;

    @Override
    public void occurancesUpdate() {
        //env grid vs env grid
        Layer [] layers = TabulationSettings.environmental_data_files;

        double [][] measures = new double[layers.length][layers.length];

        min = new double[TabulationSettings.grd_nrows][TabulationSettings.grd_ncols];
        max = new double[TabulationSettings.grd_nrows][TabulationSettings.grd_ncols];
        range = new double[TabulationSettings.grd_nrows][TabulationSettings.grd_ncols];
        for(int i=0;i<min.length;i++) {
            for(int j=0;j<min[i].length;j++) {
                min[i][j] = Double.MAX_VALUE;
                max[i][j] = Double.MAX_VALUE*-1;
                range[i][j] = Double.NaN;
            }
        }

        for(int i=0;i<measures.length;i++) {
            for(int j=0;j<measures[i].length;j++) {
                measures[i][j] = Double.NaN;
            }
        }

        System.out.println("updating min/max");
        for(int i=0;i<layers.length;i++) {
            updateMinMax(layers[i].name);
        }
        for(int i=0;i<min.length;i++){
            for(int j=0;j<min[i].length;j++){
                if(max[i][j] != Double.MAX_VALUE*-1) {
                    range[i][j] = max[i][j] - min[i][j];
                } else {
                    range[i][j] = 0;
                }
            }
        }

        //do distance calculations in blocks to reduce grids being loaded
        (new SpatialLogger()).log("calculating distances");
        ArrayList<int[]> comparisons = new ArrayList<int[]>();
        int inc = TabulationSettings.max_grids_load - TabulationSettings.analysis_threads;
        for (int span = 0; span < layers.length; span += inc){
            for (int j = span; j < layers.length; j++) {
                for (int i = span; i < span + inc && i < layers.length; i++) {
                    if(i < j){
                        int[] c = new int[2];
                        c[0] = i;
                        c[1] = j;
                        comparisons.add(c);
                    }
                }
            }
        }
        
        LinkedBlockingQueue<int[]> lbq = new LinkedBlockingQueue(comparisons);
        
        int threadcount = TabulationSettings.analysis_threads;
        CalculateDistanceThread [] cdts = new CalculateDistanceThread[threadcount];
        for(int i = 0; i<cdts.length;i++){
            cdts[i] = new CalculateDistanceThread(lbq, range, min, measures, layers);
        }
        
        try{
            boolean finished = false;
            while(!finished){
                finished = true;
                for(int i=0;i<cdts.length;i++){
                    if(cdts[i].isAlive()){
                        finished = false;
                        break;
                    }
                }
            }            
        }catch (Exception e){
            e.printStackTrace();
        }

        (new SpatialLogger()).log("finsihed calculating distances");

        //object output
        try{
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + LAYER_DISTANCE_FILE);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(measures);
            oos.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        //csv output, lower asymetric, layer display names
        try{
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path
                    + LAYER_DISTANCE_FILE_CSV);

            fw.append(",");
            for(int i=0;i<measures.length-1;i++){
                fw.append(layers[i].display_name).append(",");
            }
            for(int i=1;i<measures.length;i++){
                fw.append("\r\n");
                fw.append(layers[i].display_name);
                for(int j=0;j<i;j++){
                    fw.append(",").append(String.valueOf(measures[i][j]));
                }
            }
            fw.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        //csv output, lower asymetric, layer names
        try{
            FileWriter fw = new FileWriter(
                    TabulationSettings.index_path
                    + LAYER_DISTANCE_FILE_CSV_RAWNAMES);

            fw.append(",");
            for(int i=0;i<measures.length-1;i++){
                fw.append(layers[i].name).append(",");
            }
            for(int i=1;i<measures.length;i++){
                fw.append("\r\n");
                fw.append(layers[i].name);
                for(int j=0;j<i;j++){
                    fw.append(",").append(String.valueOf(measures[i][j]));
                }
            }
            fw.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static public void load() {
        try{
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + LAYER_DISTANCE_FILE);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            all_measures = (double [][])ois.readObject();
            ois.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static public double getMeasure(String layer1, String layer2) {
        Layer [] layers = TabulationSettings.environmental_data_files;
        int i,j;
        for(i=0;i<layers.length;i++){
            if(layer1.equals(layers[i].name)){
                break;
            }
        }
        for(j=0;j<layers.length;j++){
            if(layer2.equals(layers[j].name)){
                break;
            }
        }
        if(j<layers.length && i < layers.length) {
            return all_measures[i][j];
        }

        return Double.NaN;
    }

    @Override
    public void layersUpdate(String layername) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isUpdated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    void updateMinMax(String layer) {
        Grid g = Grid.getGrid(TabulationSettings.getPath(layer));

        float [] d = g.getGrid();

        double longitude;
        double latitude;
        int p;
        double v;

        for(int i=0;i<TabulationSettings.grd_nrows;i++){
            for(int j=0;j<TabulationSettings.grd_ncols;j++){
                longitude = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv * j;
                latitude = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv * i;

                p = g.getcellnumber(longitude,latitude);

                if(p >= 0 && p < d.length){
                    v = (d[p]-g.minval) / (g.maxval - g.minval);
                } else {
                    continue;
                }

                if(max[i][j] < v) max[i][j] = v;
                if(min[i][j] > v) min[i][j] = v;
            }
        }
    }

    static public void main(String [] args){
        TabulationSettings.load();

        LayerDistanceIndex ldi = new LayerDistanceIndex();
        ldi.occurancesUpdate();
    }
}


class CalculateDistanceThread implements Runnable {
    Thread t;
    double [][] range;
    double [][] min;
    LinkedBlockingQueue<int[]> lbq;
    double [][] measures;
    int [] next;
    Layer [] layers;

    CalculateDistanceThread(LinkedBlockingQueue<int[]> lbq_, double[][] range_, double[][] min_, double[][] measures_, Layer[] layers_) {
        lbq = lbq_;
        range = range_;
        min = min_;
        measures = measures_;
        layers = layers_;

        t = new Thread(this);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public void run() {
        try{
            next = new int[1];
            while(next != null){
                synchronized(lbq){
                    if(lbq.size() > 0){
                        next = lbq.take();                        
                    } else {
                        next = null;
                    }
                }
                if(next != null){
                    measures[next[0]][next[1]] = getDistance(layers[next[0]].name,layers[next[1]].name);
                    measures[next[1]][next[0]] = measures[next[0]][next[1]];
                    System.out.print(".");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    double getDistance(String layer1, String layer2) {
        Grid g1 = Grid.getGridStandardized(TabulationSettings.getPath(layer1));
        Grid g2 = Grid.getGridStandardized(TabulationSettings.getPath(layer2));

        float [] d1 = g1.getGrid();
        float[] d2 = g2.getGrid();

        int count = 0;
        double sum = 0;

        double longitude;
        double latitude;
        int p1,p2;
        double v1,v2;

        for(int i=0;i<TabulationSettings.grd_nrows;i++){
            for(int j=0;j<TabulationSettings.grd_ncols;j++){
                longitude = TabulationSettings.grd_xmin + TabulationSettings.grd_xdiv * j;
                latitude = TabulationSettings.grd_ymin + TabulationSettings.grd_ydiv * i;

                p1 = g1.getcellnumber(longitude,latitude);
                p2 = g2.getcellnumber(longitude, latitude);

                if(p1 >= 0 && p1 < d1.length && range[i][j] > 0){
                    v1 = d1[p1];//(d1[p1]-g1.minval) / (g1.maxval - g1.minval);
                } else {
                    continue;
                }

                if(p2 >= 0 && p2 < d2.length){
                    v2 = d2[p2];//(d2[p2]-g2.minval) / (g2.maxval - g2.minval);
                } else {
                    continue;
                }

                if(!Double.isNaN(v1) && !Double.isNaN(v2)){
                    count++;
                    sum += java.lang.Math.abs(v1 - v2) / range[i][j];
               }
            }
        }

        return sum/count;
    }

    boolean isAlive() {
        return t.isAlive();
    }

}