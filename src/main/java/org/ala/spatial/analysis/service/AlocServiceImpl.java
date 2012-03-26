package org.ala.spatial.analysis.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.ala.spatial.analysis.maxent.MaxentService;
import org.ala.spatial.util.AnalysisJobAloc;

/**
 * Gets the submitted parameters and runs a Aloc model
 * 
 * @author ajayr
 */
public class AlocServiceImpl implements MaxentService {

    AlocSettings cmdAloc = null;

    public AlocServiceImpl() {
        cmdAloc = new AlocSettings();
    }

    public AlocSettings getAlocSettings() {
        return cmdAloc;
    }

    public void setAlocSettings(AlocSettings cmdAloc) {
        this.cmdAloc = cmdAloc;
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
     * The process method sets up the parameters and runs the Aloc process
     *
     * @return success int value if the process was successful
     */
    @Override
    public int process() {
        return runCommand(cmdAloc.toString());
    }

    /**
     * The runCommand method does the fork'ing
     * 
     * @param command The command to be run
     * @return success int value if the process was successful
     */
    private int runCommand(String command) {
        Runtime runtime = Runtime.getRuntime();

        try {
            String[] acmd = new String[3];
            acmd[0] = "cmd.exe";
            acmd[1] = "/C";
            acmd[2] = command;

            //System.out.println("Execing " + acmd[0] + " " + acmd[1] + " " + acmd[2]);
            System.out.println("Exec'ing " + command);
            Process proc = runtime.exec(command);

            /*
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", false);

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT", false);

            // kick them off
            errorGobbler.start();
            outputGobbler.start();
             */

            System.out.println("Setting up output stream readers");
            InputStreamReader isre = new InputStreamReader(proc.getErrorStream());
            BufferedReader bre = new BufferedReader(isre);
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            System.out.printf("Output of running %s is:", command);
            // Arrays.toString(acmd)

            while ((line = bre.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            /*
            // if good, add it to geoserver
            if (exitVal == 0) {
            String cmd2 =
            Process proc2 = runtime.exec()
            }
             */

            // any error???
            return exitVal;
        } catch (Exception e) {
            System.out.println("OOOOPPPSSS: " + e.toString());
            System.out.println("{success: false , responseText: 'Error occurred' + " + e.toString() + "}");
            e.printStackTrace(System.out);
        }

        return 1;
    }

    public int process(AnalysisJobAloc job) {
        AlocThread mt = new AlocThread(cmdAloc.toString());
        mt.start();

        while (mt.isAlive() && (job == null || !job.isCancelled())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                //wake up
            }
        }

        try {
            mt.kill(); //in case it is still running, AlocThread will end now
        } catch (Exception e) {
        }

        return mt.exitValue;
    }
}

class AlocThread extends Thread {

    public int exitValue = -1;
    String command;
    Process proc;

    public AlocThread(String command_) {
        command = command_;
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

            //System.out.println("Execing " + acmd[0] + " " + acmd[1] + " " + acmd[2]);
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
                System.out.println(line);
            }

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            int exitVal = proc.waitFor();

            // any error???
            exitValue = exitVal;
            return;
        } catch (Exception e) {
            System.out.println("OOOOPPPSSS: " + e.toString());
            System.out.println("{success: false , responseText: 'Error occurred' + " + e.toString() + "}");
            e.printStackTrace(System.out);
        }

        exitValue = 1;

    }
}
