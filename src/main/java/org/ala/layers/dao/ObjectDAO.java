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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.ala.layers.dto.Objects;
import org.ala.layers.util.LayerFilter;

/**
 * DAO for the Object object
 *
 * @author ajay
 */
public interface ObjectDAO {
    public List<Objects> getObjects();
    public List<Objects> getObjectsById(String id);
    public String getObjectsGeometryById(String id, String geomtype);
    public Objects getObjectByPid(String pid);
    public Objects getObjectByIdAndLocation(String fid, Double lng, Double lat);
    public List<Objects> getNearestObjectByIdAndLocation(String fid, int limit, Double lng, Double lat);
    public void streamObjectsGeometryById(OutputStream os, String id, String geomtype) throws IOException;
    public List<Objects> getObjectByFidAndName(String fid, String name);

    public List<Objects> getObjectsByIdAndArea(String id, Integer limit, String wkt);
    public List<Objects> getObjectsByIdAndIntersection(String id, Integer limit, LayerFilter layerFilter);
    public List<Objects> getObjectsByIdAndIntersection(String id, Integer limit, String intersectingPid);
    public String createUserUploadedObject(String wkt, String name, String description, String userid);
    public void updateUserUploadedObject(int pid, String wkt, String name, String description, String userid);
}
