/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ala.spatial.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam
 */
public class AnalysisQueue {
    static LinkedBlockingQueue<AnalysisJob> jobs = new LinkedBlockingQueue<AnalysisJob>();
    static ConcurrentHashMap<String, AnalysisJob> runningJobs = new ConcurrentHashMap<String, AnalysisJob>();
    static ConcurrentHashMap<String, AnalysisJob> finishedJobs = new ConcurrentHashMap<String, AnalysisJob>();
    static AnalysisJobConsumer analysisJobConsumer = null;

    public static void addJob(AnalysisJob job){
        try {
            jobs.put(job);
        } catch (InterruptedException ex) {
            Logger.getLogger(AnalysisQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(analysisJobConsumer == null){
            analysisJobConsumer = new AnalysisJobConsumer(jobs, runningJobs, finishedJobs);
            analysisJobConsumer.start();
        }
        saveJob(job);
    }

    public static boolean removeJob(String pid){
        AnalysisJob j = getJob(pid);
        if(j != null){
            if(!jobs.remove(j)){
                j.cancel();
                runningJobs.remove(j);
            }
            return true;
        }
        return false;
    }

    public static long getEstimate(String pid){
        AnalysisJob j = getJob(pid);
        if(j != null){
            return j.getEstimate();
        }
        return -1;
    }

    public static String getStatus(String pid){
        String msg = "";

        long queuePosition = getQueuePosition(pid);
        if(queuePosition >= 0){
            msg += "In Queue: " + (queuePosition+1) + " of " + jobs.size() + "\r\n";

            long queueEstimate = getQueueEstimate(pid);
            if(queueEstimate > 0){
                msg += "Est to start: " + (queueEstimate/1000) + " s\r\n";
            }
        }

        String jobMsg = getJobStatus(pid);
        if(jobMsg != null){
            msg += jobMsg;
        }

        return msg;
    }

    static AnalysisJob getJob(String pid){
        //check in waiting jobs
        for(AnalysisJob j : jobs){
            if(j.getName().equals(pid)){
                return j;
            }
        }
        //check in running jobs
        for(Entry<String,AnalysisJob> e : runningJobs.entrySet()){
            if(e.getValue().getName().equals(pid)){
                return e.getValue();
            }
        }
        //check in finished jobs
        for(Entry<String,AnalysisJob> e : finishedJobs.entrySet()){
            if(e.getValue().getName().equals(pid)){
                return e.getValue();
            }
        }
        //check in saved jobs, move to finished jobs here
        AnalysisJob j = getSavedJob(pid);
        if(j != null){
            finishedJobs.put(j.getName(),j);
        }
        return j;
    }

    public static String getJobStatus(String pid){
        AnalysisJob j = getJob(pid);
        if(j != null){
            String msg = "";
            int qp = getQueuePosition(pid);
            if(qp >= 0) {
                msg = "in queue: " + (qp+1) + " of " + jobs.size() + ", ";
            }
            return msg + j.getStatus();
        }
        return null;
    }

    public static int getQueuePosition(String pid){
        int position = 0;
        for(AnalysisJob j : jobs){
            if(j.getName().equals(pid)){
                return position;
            }
            position++;
        }
        return -1;
    }

    public static String getLog(String pid) {
        AnalysisJob j = getJob(pid);
        if(j != null){
            return j.getLog();
        }
        return null;
    }

    public static String getProgress(String pid) {
        AnalysisJob j = getJob(pid);
        if(j != null){
            return String.valueOf(j.getProgress());
        }
        return null;
    }

    public static String getState(String pid) {
        AnalysisJob j = getJob(pid);
        if(j != null){
            return j.getCurrentState();
        }
        return null;
    }

    private static long getQueueEstimate(String pid) {
        long estimate = 0;
        for(AnalysisJob j : jobs){
            if(j.getName().equals(pid)){
                break;
            }
            estimate += j.getEstimate();
        }
        for(Entry<String,AnalysisJob> e : runningJobs.entrySet()){
            estimate += e.getValue().getEstimate();
        }
        return estimate;
    }

    public static void saveJob(AnalysisJob j){
        try{
            FileOutputStream fos = new FileOutputStream(
                    TabulationSettings.index_path
                    + "JOB" + j.getName());
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(j);
            oos.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static AnalysisJob getSavedJob(String pid){
        AnalysisJob j = null;
        try{
            FileInputStream fis = new FileInputStream(
                    TabulationSettings.index_path
                    + "JOB" + pid);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);
            j = (AnalysisJob) ois.readObject();
            ois.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return j;
    }
}

class AnalysisJobConsumer extends Thread {
    LinkedBlockingQueue<AnalysisJob> jobs;
    ConcurrentHashMap<String, AnalysisJob> runningJobs;
    ConcurrentHashMap<String, AnalysisJob> finishedJobs;

    public AnalysisJobConsumer(LinkedBlockingQueue<AnalysisJob> jobs_, ConcurrentHashMap<String, AnalysisJob> runningJobs_,ConcurrentHashMap<String, AnalysisJob> finishedJobs_){
        jobs = jobs_;
        runningJobs = runningJobs_;
        finishedJobs = finishedJobs_;
    }

    @Override
    public void run() {
        while(true){
            try {
                if(runningJobs.size() >= TabulationSettings.jobs_maximum){
                    removeFinishedJobs();
                    Thread.sleep(100);
                }
                AnalysisJob j = jobs.take();
                runningJobs.put(j.getName(),j);
                j.start();
            } catch (InterruptedException ex) {
                Logger.getLogger(AnalysisJobConsumer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void removeFinishedJobs(){
        for(Entry<String,AnalysisJob> e : runningJobs.entrySet()){
            if(!e.getValue().isAlive()){
                runningJobs.remove(e.getValue());
                finishedJobs.put(e.getValue().getName(),e.getValue());

                //save finished job
                AnalysisQueue.saveJob(e.getValue());
            }
        }
    }
}
