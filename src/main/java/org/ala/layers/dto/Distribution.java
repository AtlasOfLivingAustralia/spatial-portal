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

package org.ala.layers.dto;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This class serves as a model object for the "distributions" table
 *
 * @author ajay
 */

//@XmlRootElement(name="distribution")
//@XStreamAlias("distribution")
@JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
public class Distribution {
    private String id;
    private double depthMinimum;
    private double depthMaximum;
    private String path;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getDepthMaximum() {
        return depthMaximum;
    }

    public void setDepthMaximum(double depthMaximum) {
        this.depthMaximum = depthMaximum;
    }

    public double getDepthMinimum() {
        return depthMinimum;
    }

    public void setDepthMinimum(double depthMinimum) {
        this.depthMinimum = depthMinimum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
