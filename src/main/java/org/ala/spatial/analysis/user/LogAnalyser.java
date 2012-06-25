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
