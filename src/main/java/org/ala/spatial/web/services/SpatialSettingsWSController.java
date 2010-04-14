package org.ala.spatial.web.services;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.ala.spatial.analysis.tabulation.SamplingService;
import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SpatialSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping(value = "/layer/{layer}/extents", method = RequestMethod.GET)
    public @ResponseBody String getLayerExtents(@PathVariable  String layer) {

        String layer_filename = SamplingService.layerDisplayNameToName(layer.replaceAll("_", " "));

        String extents = SpeciesListIndex.getLayerExtents(layer_filename);

        if (extents == null) extents = "Layer extents not available.";

        return extents;
    }

    @RequestMapping(value = "/layer/{layer}/metadata", method = RequestMethod.GET)
    public @ResponseBody String getLayerMetadata(@PathVariable  String layer) {

        String layer_filename = SamplingService.layerDisplayNameToName(layer.replaceAll("_", " "));

        String metadata= SamplingService.getLayerMetaData(layer_filename);

        if (metadata == null) metadata = "Layer metadata not available.";

        return metadata;
    }

}
