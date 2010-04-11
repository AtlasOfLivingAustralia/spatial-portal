package org.ala.spatial.web.services;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
@RequestMapping("/ws/spatial/settings")
public class SpatialSettingsWSController {

    private SpatialSettings ssets;

    private List<Layer> _layers;

    @RequestMapping(value = "/layers/environmental", method = RequestMethod.GET)
    public @ResponseBody Layer[] envListAsLayers(HttpServletRequest req) {

        ssets = new SpatialSettings();

        _layers = new ArrayList();
        Layer[] _layerlist = ssets.getEnvironmentalLayers();

        for (int i = 0; i < _layerlist.length; i++) {
            _layers.add(_layerlist[i]);
            //System.out.println("Layer: " + _layerlist[i].name + " - " + _layerlist[i].display_name);

        }

        return _layerlist;
    }

    @RequestMapping(value = "/layers/environmental/string", method = RequestMethod.GET)
    public @ResponseBody String environmentalLayersAsString(HttpServletRequest req) {

        ssets = new SpatialSettings();

        StringBuffer sbEnvList = new StringBuffer();

        _layers = new ArrayList();
        Layer[] _layerlist = ssets.getEnvironmentalLayers();

        for (int i = 0; i < _layerlist.length; i++) {
            _layers.add(_layerlist[i]);
            //System.out.println("Layer: " + _layerlist[i].name + " - " + _layerlist[i].display_name);

            sbEnvList.append(_layerlist[i].display_name + "\n");

        }

        return sbEnvList.toString();
    }

    @RequestMapping(value = "/layers/contextual/string", method = RequestMethod.GET)
    public @ResponseBody String contextualLayersAsString(HttpServletRequest req) {

        ssets = new SpatialSettings();

        StringBuffer sbEnvList = new StringBuffer();

        _layers = new ArrayList();
        Layer[] _layerlist = ssets.getContextualLayers();

        for (int i = 0; i < _layerlist.length; i++) {
            _layers.add(_layerlist[i]);
            //System.out.println("Layer: " + _layerlist[i].name + " - " + _layerlist[i].display_name);

            sbEnvList.append(_layerlist[i].display_name + "\n");

        }

        return sbEnvList.toString();
    }

}
