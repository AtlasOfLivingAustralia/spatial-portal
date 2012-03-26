package org.ala.spatial.web.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.client.Client;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Layer;
import org.ala.spatial.analysis.index.LayerDistanceIndex;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author ajay
 */
@Controller
public class LayerDistancesWSController {

    @RequestMapping(value = "/ws/layerdistances", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Double> aloc(HttpServletRequest req) {
        Map<String, Double> map = new HashMap<String, Double>();
        return LayerDistanceIndex.loadDistances();
    }

    @RequestMapping(value = "/layers/analysis/inter_layer_association_rawnames.csv", method = RequestMethod.GET)
    public
    @ResponseBody
    String CSVrawnames(HttpServletRequest req) {
        return makeCSV("name");
    }

    @RequestMapping(value = "/layers/analysis/inter_layer_association.csv", method = RequestMethod.GET)
    public
    @ResponseBody
    String CSV(HttpServletRequest req) {
        return makeCSV("displayname");
    }

    private String makeCSV(String type) {
        Map<String, Double> map = LayerDistanceIndex.loadDistances();

        TreeSet<String> layerSet = new TreeSet<String>();
        for(String k : map.keySet()) {
            String [] ks = k.split(" ");
            layerSet.add(ks[0]);
            layerSet.add(ks[1]);
        }
        ArrayList<String> layerList = new ArrayList<String>(layerSet);

        //match against layers
        ArrayList<Layer> layerMatch = new ArrayList<Layer>(layerList.size());
        for(int i=0;i<layerList.size();i++) {
            layerMatch.add(null);
        }
        List<Field> fields = Client.getFieldDao().getFields();
        List<Layer> layers = Client.getLayerDao().getLayers();
        for(int i=0;i<layerList.size();i++) {
            for(int j=0;j<fields.size();j++) {
                if(fields.get(j).getId().equals(layerList.get(i))) {
                    for(int k=0;k<layers.size();k++) {
                        if((fields.get(j).getSpid() + "").equals(layers.get(k).getId() + "")) {
                            layerMatch.set(i, layers.get(k));
                            break;
                        }
                    }
                    break;
                }
            }
        }

        //output lower association matrix
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<layerList.size();i++) {
            if(i > 0) {
                if(type.equals("name")) {
                    sb.append(layerMatch.get(i).getName());
                } else if(type.equals("displayname")) {
                    sb.append(layerMatch.get(i).getDisplayname());
                }
            }
            sb.append(",");
            int size = (i == 0)?layerList.size()-1 : i + 1;
            for(int j=0;j<size;j++) {
                if(i == 0) {
                    if(type.equals("name")) {
                    sb.append(layerMatch.get(j).getName());
                } else if(type.equals("displayname")) {
                    sb.append(layerMatch.get(j).getDisplayname());
                }
                } else {
                    String key = (layerList.get(i).compareTo(layerList.get(j)) < 0) ? layerList.get(i) + " " + layerList.get(j) : layerList.get(j) + " " + layerList.get(i);
                    if(key != null && !key.equals("null")) {
                        sb.append(map.get(key));
                    }
                }

                if(j < size - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
