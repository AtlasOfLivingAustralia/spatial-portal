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

import org.ala.layers.dto.Tabulation;
import org.ala.layers.dto.Ud_header;
import org.ala.layers.legend.QueryField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UserDataDAO {

    public Ud_header put(String user_id, String record_type, String desc, String metadata, String data_path, String analysis_id);

    public Ud_header get(Long ud_header_id);

    public String[] getStringArray(String header_id, String ref);

    public boolean[] getBooleanArray(String header_id, String ref);

    public double[][] getDoublesArray(String header_id, String ref);

    public boolean setStringArray(String header_id, String ref, String[] data);

    public boolean setBooleanArray(String header_id, String ref, boolean[] data);

    public boolean setDoublesArray(String header_id, String ref, double[][] data);

    public List<Ud_header> list(String user_id);

    public boolean setDoubleArray(String ud_header_id, String ref, double[] points);

    public boolean setQueryField(String ud_header_id, String ref, QueryField qf);

    public double[] getDoubleArray(String ud_header_id, String ref);

    public QueryField getQueryField(String ud_header_id, String ref);

    public boolean setMetadata(long ud_header_id, Map metadata);

    public Map getMetadata(long ud_header_id);

    public List<String> listData(String ud_header_id, String data_type);

    public Ud_header facet(String ud_header_id, List<String> new_facets, String new_wkt);

    public String getSampleZip(String id, String fields);
}
