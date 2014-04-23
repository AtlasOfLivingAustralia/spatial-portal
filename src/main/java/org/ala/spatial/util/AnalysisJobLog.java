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

import java.io.FileWriter;

/**
 * Use for offline analysis requests.
 * <p/>
 * Does the log printing to System.out the optional specified log file.
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
