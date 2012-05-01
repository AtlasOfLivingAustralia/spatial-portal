/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author Adam
 */
public class StreamGobbler extends Thread {
    BufferedReader br;
    String name;
    AnalysisJob job;

    public StreamGobbler(InputStream is, String name, AnalysisJob job) {
        br = new BufferedReader(new InputStreamReader(is));
        this.name = name;
        this.job = job;
    }

    @Override
    public void run() {
        try {
            String line;
            while((line = br.readLine()) != null) {
                if(job != null) {
                    job.log(name + ": " + line);
                }
                System.out.println(name + ": " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("exiting StreamGobbler: " + name);
    }

}
