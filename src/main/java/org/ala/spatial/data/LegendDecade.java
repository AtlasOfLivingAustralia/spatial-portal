package org.ala.spatial.data;
/**
 * 
 * Generates a legend based on decades
 * 
 * @author Natasha Quimby (natsha.quimby@csiro.au)
 *
 */
public class LegendDecade extends Legend {

    @Override
    public void generate(double[] d, int divisions) {
        //all the values for the decades MUST be included
        if (Double.isNaN(max)) {
            return;
        }
        cutoffMins = removeNaN(d);
        cutoffs = new double[cutoffMins.length];
        for(int i=0; i<cutoffs.length; i++){
            cutoffs[i] = cutoffMins[i]+9;
        }
        
    }
    private double[] removeNaN(double[] ds){
        int nanCount=0;
        for(double d:ds){
            if(Double.isNaN(d)){
                nanCount++;
            }                
        }
        double[] clean = new double[ds.length-nanCount];
        int i =0;
        for(double d :ds){
            if(!Double.isNaN(d)){
                clean[i++] = d;
            }
        }
        return clean;
    }

    @Override
    public String getTypeName() {
        
        return "Decade Legend";
    }

}
