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
package org.ala.layers.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.intersect.IntersectConfig;

/**
 * @author adam
 */
public interface LayerIntersectDAO {

    /**
     * Refresh properties and table images.
     * <p/>
     * Returns null if successful, else error as String.
     */
    public String reload();

    /**
     * Sampling one point
     * <p/>
     * Must have local access to layer files and db connection.
     *
     * @param fieldId   one field table id for intersection with the coordinates
     * @param longitude longitude as double
     * @param latitude  latitude as double
     * @return value of the intersection as String or null if no value available.
     */
    public String sampling(String fieldId, double longitude, double latitude);

    /**
     * Sampling one point, multiple fields.  Full result returned
     * <p/>
     * Must have local access to layer files and db connection.
     *
     * @param fieldIds  fields to intersect as field table ids in comma separated String.  e.g. "cl22,cl23"
     * @param longitude longitude as double
     * @param latitude  latitude as double
     * @return full result of sampling returned as Vector containing one Map for each fieldId.
     * Map objects contain, "fieldid", "value", "layername", and in addition for shape results "pid", "description"
     */
    public Vector samplingFull(String fieldIds, double longitude, double latitude);

    /**
     * Sampling
     *
     * @param fieldIds     fields to intersect as field table ids in comma separated String.  e.g. "cl22,cl23"
     * @param pointsString latitude,longitude pairs, comma separated.  e.g. "-20,120,-23,145"
     * @return one string for each fieldId, containing new line separated
     * intersection values of the field and pointsString pairs.  As ArrayList<String>.
     */
    public ArrayList<String> sampling(String fieldIds, String pointsString);

    /**
     * Sampling
     *
     * @param fieldIds fields to intersect as field table ids in String [].
     * @param points   longitude, latitude coordinates as double [][2].
     *                 [][0] is longitude, [][1] is latitude.
     * @return one string for each fieldId, containing new line separated
     * intersection values of the field and pointsString pairs.  As ArrayList<String>.
     */
    public ArrayList<String> sampling(String[] fieldIds, double[][] points);

    /**
     * Sampling
     *
     * @param fieldIds fields to intersect as field table ids in String [].
     * @param points   longitude, latitude coordinates as double [][2].
     *                 [][0] is longitude, [][1] is latitude.
     * @return one string for each fieldId, containing new line separated
     * intersection values of the field and pointsString pairs.  As ArrayList<String>.
     */
    public ArrayList<String> sampling(String[] fieldIds, double[][] points, IntersectCallback callback);

    /**
     * Sampling
     *
     * @param intersectionFiles fields to intersect as IntersectionFile []
     * @param points            longitude, latitude coordinates as double [][2].
     *                          [][0] is longitude, [][1] is latitude.
     * @return one string for each fieldId, containing new line separated
     * intersection values of the field and pointsString pairs.  As ArrayList<String>.
     */
    public ArrayList<String> sampling(IntersectionFile[] intersectionFiles, double[][] points);

    /**
     * Get initialised IntersectConfig.
     *
     * @return IntersectConfig
     */
    public IntersectConfig getConfig();

    /**
     * Sampling one point, all cached fields.
     * <p/>
     * Must have local access to layer files and db connection.
     *
     * @param longitude longitude as double
     * @param latitude  latitude as double
     * @return full result of sampling returned as Map with key as field id and value String or Float
     */
    public HashMap<String, String> sampling(double longitude, double latitude);
}
