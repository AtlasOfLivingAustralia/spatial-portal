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

package org.ala.layers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Basic page serving Controller
 *
 * @author ajay
 */
@Controller
public class PageController {

    private final String INDEX = "/index";
    private final String WMS = "/wms";
    private final String EXAMPLE = "/examples";
    private final String EXAMPLE_INDEX = "/examples/index";
    private final String EXAMPLE_SPECIES_WMS = "/examples/specieswms";
    private final String EXAMPLE_LAYERS_WMS = "/examples/layerswms";
    private final String EXAMPLE_SPECIES_WMSG = "/examples/specieswmsg";

    @RequestMapping(value = INDEX)
    public String index() {
        return "index";
    }

    @RequestMapping(value = WMS)
    public String wms() {
        return "wms/index";
    }

    @RequestMapping(value = {EXAMPLE, EXAMPLE_INDEX})
    public String exampleIndex() {
        return "examples/index";
    }

    @RequestMapping(value = EXAMPLE_SPECIES_WMS)
    public String exampleSpeciesWms() {
        return "examples/specieswms";
    }

    @RequestMapping(value = EXAMPLE_LAYERS_WMS)
    public String exampleLayersWms() {
        return "examples/layerswms";
    }

    @RequestMapping(value = EXAMPLE_SPECIES_WMSG)
    public String exampleSpeciesWmsg() {
        return "examples/specieswmsg";
    }
}
