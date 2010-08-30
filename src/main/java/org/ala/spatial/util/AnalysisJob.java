/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author Adam
 */
public class AnalysisJob extends Thread implements Serializable {
    private final String DATE_FORMAT_NOW = "dd-MM-yyyy HH:mm:ss";
    public final String WAITING = "WAITING";
    public final String RUNNING = "RUNNING";
    public final String SUCCESSFUL = "SUCCESSFUL";
    public final String FAILED = "FAILED";

    double progress;    //progress 0 to 1
    long progressTime;  //time of last set progress
    int stage;          //for use by extended classes
    String status;      //status of thread
    StringBuffer log;         //log of job events
    long runTime;       //total run time in ms
    String currentState;//state of job; WAITING, RUNNING, SUCCESSFUL, FAILED

    public AnalysisJob(String pid){
        this.setName(pid);
        status = "";
        log = new StringBuffer();
        stage = -1;
        setCurrentState(WAITING);
    }

    public long getEstimate(){
        return 0;
    }

    public boolean cancel(){
        return true;
    }

    public String getLog(){
        return log.toString();
    }

    public void log(String s){
        log.append("\r\n").append(now()).append("> ").append(s);
    }

    public long getRunTime(){
        return runTime;
    }

    public void setRunTime(long l){
        runTime = l;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String s){
        status = s;
    }

    public void setProgress(double d, String s){
        setProgress(d);
        log(s);
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double d){
        progress = d;
        progressTime = System.currentTimeMillis();
    }

     public int getStage() {
        return stage;
    }

    public void setStage(int i){
        stage = i;
    }

    private String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String state){
        currentState = state;
    }
}
