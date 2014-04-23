/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.layers.tabulation;

import java.io.IOException;

import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.spatial.analysis.layers.Records;

/**
 * @author Adam
 */
public class TabulationUtil {
    static public int calculateOccurrences(String pathToRecords, String wkt) throws IOException {
        SimpleRegion region = SimpleShapeFile.parseWKT(wkt);
        Records records = new Records(pathToRecords, region);
        int result = records.getRecordsSize();
        return result;
    }

    static public int calculateSpecies(String pathToRecords, String wkt) throws IOException {
        SimpleRegion region = SimpleShapeFile.parseWKT(wkt);
        Records records = new Records(pathToRecords, region);
        int result = records.getSpeciesSize();
        return result;
    }
}
