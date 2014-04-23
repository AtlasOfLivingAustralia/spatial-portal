/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.analysis.maxent;

import org.ala.spatial.util.StreamGobbler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.ala.spatial.util.AnalysisJob;
import org.ala.spatial.util.AnalysisJobMaxent;

/**
 * Gets the submitted parameters and runs a maxent model
 *
 * @author ajayr
 */
public class MaxentServiceImpl implements MaxentService {

    MaxentSettings cmdMaxent = null;

    public MaxentServiceImpl() {
        cmdMaxent = new MaxentSettings();
    }

    public MaxentSettings getMaxentSettings() {
        return cmdMaxent;
    }

    public void setMaxentSettings(MaxentSettings cmdMaxent) {
        this.cmdMaxent = cmdMaxent;
    }

    /**
     * The generateSessionDirectory allows creating a session directory
     *
     * @param thePath
     * @return
     */
    private File generateSessionDirectory(String thePath) {
        File fDir = null;

        try {
            //fDir = new File(cmdPath + sessionId);
            fDir = new File(thePath);
            fDir.mkdir();
        } catch (Exception e) {
        }

        return fDir;
    }

    /**
     * The process method sets up the parameters and runs the maxent process
     *
     * @return success int value if the process was successful
     */
    @Override
    public int process(AnalysisJob job) {
        return runCommand(cmdMaxent.toString(), job);
    }

    /**
     * The runCommand method does the fork'ing
     *
     * @param command The command to be run
     * @return success int value if the process was successful
     */
    private int runCommand(String command, AnalysisJob job) {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] acmd = new String[3];
            acmd[0] = "cmd.exe";
            acmd[1] = "/C";
            acmd[2] = command;

            System.out.println("Exec'ing " + command);
            Process proc = runtime.exec(command);

            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", job);

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", job);

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            System.out.printf("Output of running %s is:", command);

            int exitVal = proc.waitFor();

            errorGobbler.interrupt();
            outputGobbler.interrupt();

            // any error???
            return exitVal;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return 1;
    }

    public int process(AnalysisJobMaxent job) {
        MaxentThread mt = new MaxentThread(cmdMaxent.toString(), job);
        mt.start();

        while (mt.isAlive() && (job == null || !job.isCancelled())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                //wake up
            }
        }

        try {
            mt.kill(); //in case it is still running, MaxentThread will end now
        } catch (Exception e) {
        }

        return mt.exitValue;
    }
}

class MaxentThread extends Thread {

    public int exitValue = -1;
    String command;
    Process proc;
    AnalysisJob job;

    public MaxentThread(String command_, AnalysisJob job) {
        command = command_;
        this.job = job;
        setPriority(Thread.MIN_PRIORITY);
    }

    public void kill() {
        proc.destroy();
    }

    /**
     * The runCommand method does the fork'ing
     *
     * @param command The command to be run
     * @return success int value if the process was successful
     */
    public void run() {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] acmd = new String[3];
            acmd[0] = "cmd.exe";
            acmd[1] = "/C";
            acmd[2] = command;

            System.out.println("Exec'ing " + command);
            proc = runtime.exec(command);

            System.out.println("Setting up output stream readers");
            InputStreamReader isre = new InputStreamReader(proc.getErrorStream());
            BufferedReader bre = new BufferedReader(isre);
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            System.out.printf("Output of running %s is:", command);

            while ((line = bre.readLine()) != null) {
                if (job != null) {
                    job.log(line);
                }
                System.out.println(line);
            }

            while ((line = br.readLine()) != null) {
                if (job != null) {
                    job.log(line);
                }
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            // any error???
            exitValue = exitVal;
            return;
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        exitValue = 1;

    }
}
