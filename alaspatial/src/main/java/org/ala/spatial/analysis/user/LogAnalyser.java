package org.ala.spatial.analysis.user;

/**
 * Analyse the log4j (useractions.log) file and send generate reports
 * 
 * @author ajay
 */
public class LogAnalyser {


    private void startAnalysis() {
        try {

            // start by loading the up useractions.log
            

        } catch (Exception e) {
            System.out.println("Unable to analyse useractions");
            e.printStackTrace(System.out); 
        }
    }

}
