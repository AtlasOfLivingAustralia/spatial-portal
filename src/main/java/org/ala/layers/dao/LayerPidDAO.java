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

import java.util.List;

import org.ala.layers.dto.LayerPid;

/**
 * DAO for the LayerPid object
 *
 * @author ajay
 */
public interface LayerPidDAO {
    public List<LayerPid> getLayers();

    public LayerPid getLayerById(String id);

    public LayerPid getLayerByPid(String pid);

}
