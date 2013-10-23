/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.web.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.client.Client;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.Layer;
import org.ala.spatial.analysis.index.LayerDistanceIndex;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * inter layer association distances webservices.
 *
 * @author ajay
 */
@Controller
public class LayerDistancesWSController {

    @RequestMapping(value = "/ws/layerdistances", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Double> aloc(HttpServletRequest req) {
        Map<String, Double> map = new HashMap<String, Double>();
        return LayerDistanceIndex.loadDistances();
    }

    @RequestMapping(value = "/layers/analysis/inter_layer_association_rawnames.csv", method = RequestMethod.GET)
    public ResponseEntity<String> CSVrawnames(HttpServletRequest req) {
        String csv =  makeCSV("name");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType("text/csv"));
        return new ResponseEntity<String>(csv, responseHeaders, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/layers/analysis/inter_layer_association.csv", method = RequestMethod.GET)
    public ResponseEntity<String> CSV(HttpServletRequest req) {
        String csv = makeCSV("displayname");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType("text/csv"));
        return new ResponseEntity<String>(csv, responseHeaders, HttpStatus.CREATED);        
    }

    private String makeCSV(String type) {
        Map<String, Double> map = LayerDistanceIndex.loadDistances();

        TreeSet<String> layerSet = new TreeSet<String>();
        for (String k : map.keySet()) {
            String[] ks = k.split(" ");
            layerSet.add(ks[0]);
            layerSet.add(ks[1]);
        }
        ArrayList<String> layerList = new ArrayList<String>(layerSet);

        //match against layers
        ArrayList<Layer> layerMatch = new ArrayList<Layer>(layerList.size());
        for (int i = 0; i < layerList.size(); i++) {
            layerMatch.add(null);
        }
        List<Field> fields = Client.getFieldDao().getFields();
        List<Layer> layers = Client.getLayerDao().getLayers();
        for (int i = 0; i < layerList.size(); i++) {
            for (int j = 0; j < fields.size(); j++) {
                if (fields.get(j).getId().equals(layerList.get(i))) {
                    for (int k = 0; k < layers.size(); k++) {
                        if ((fields.get(j).getSpid() + "").equals(layers.get(k).getId() + "")) {
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
        for (int i = 0; i < layerList.size(); i++) {
            if (i > 0) {
                if (type.equals("name")) {
                    sb.append(layerMatch.get(i).getName());
                } else if (type.equals("displayname")) {
                    sb.append(layerMatch.get(i).getDisplayname());
                }
            }
            sb.append(",");
            int size = (i == 0) ? layerList.size() - 1 : i;
            for (int j = 0; j < size; j++) {
                if (i == 0) {
                    if (type.equals("name")) {
                        sb.append(layerMatch.get(j).getName());
                    } else if (type.equals("displayname")) {
                        sb.append(layerMatch.get(j).getDisplayname());
                    }
                } else {
                    String key = (layerList.get(i).compareTo(layerList.get(j)) < 0) ? layerList.get(i) + " " + layerList.get(j) : layerList.get(j) + " " + layerList.get(i);
                    if (key != null && map.get(key) != null) {
                        sb.append(map.get(key));
                    }
                }

                if (j < size - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
