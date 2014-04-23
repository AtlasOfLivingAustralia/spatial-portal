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
package org.ala.spatial.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Use to consume streams when exec'ing analysis functions.
 * <p/>
 * E.g.
 * <code>
 * //init and start
 * StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR", job);
 * errorGobbler.start();
 * <p/>
 * //do stuff
 * <p/>
 * //end the stream
 * errorGobbler.interrupt();
 * </code>
 * <p/>
 * </code>
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
            while ((line = br.readLine()) != null) {
                if (job != null) {
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
