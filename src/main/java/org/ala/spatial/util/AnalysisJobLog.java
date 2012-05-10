/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.util;

import java.io.FileWriter;

/**
 *
 * @author Adam
 */
public class AnalysisJobLog extends AnalysisJob {

    FileWriter fw;
    String filename;

    public AnalysisJobLog(String filename) {
        super(filename);

        this.filename = filename;
        try {
            fw = new FileWriter(filename, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void log(String s) {
        super.log(s);
        output(s);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        output(message);
    }

    @Override
    public void setCurrentState(String state) {
        super.setCurrentState(state);
        output(state);
    }

    @Override
    public void setProgress(double d, String s) {
        super.setProgress(d, s);
        output(s);
    }

    void output(String s) {
        System.out.println(s);
        if (fw != null) {
            try {
                fw.write(s);
                fw.write("\n");
                fw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (fw != null) {
            try {
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
