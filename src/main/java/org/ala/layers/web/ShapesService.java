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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.layers.dao.ObjectDAO;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.geojson.geom.GeometryJSON;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vividsolutions.jts.geom.Geometry;

/**
 *
 * @author Adam
 */
@Controller
public class ShapesService {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    @Resource(name = "objectDao")
    private ObjectDAO objectDao;

    /*
     * return a shape as kml
     */
    @RequestMapping(value = "/shape/{type}/{id}", method = RequestMethod.GET)
    public void findShape(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse resp) {
        OutputStream os = null;
        try {
            os = resp.getOutputStream();
            //validate object id
            id = cleanObjectId(id);

//            List<Objects> objects = objectDao.getObjectsById(id);
//            if (objects.size() > 0) {
//                Geometry geom = objects.get(0).getGeometry();
//                if (type.equalsIgnoreCase("wkt")) {
//                    WKTWriter wkt = new WKTWriter();
//                    return wkt.write(geom);
//                } else if (type.equalsIgnoreCase("kml")) {
//                    Encoder e = new Encoder(new KMLConfiguration());
//                    e.setIndenting(true);
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    e.encode(geom, KML.Geometry, baos);
//                    String kmlGeometry = new String(baos.toByteArray());
//                    return kmlGeometry.substring(kmlGeometry.indexOf('\n'));
//                } else if (type.equalsIgnoreCase("geojson")) {
//                    return "Not supported yet.";
//                }
//
//            } else {
//                return "";
//            }

            if (type.equalsIgnoreCase("wkt")) {
                resp.setContentType("application/wkt");
                objectDao.streamObjectsGeometryById(os, id, type);
            } else if (type.equalsIgnoreCase("kml")) {
                resp.setContentType("application/vnd.google-earth.kml+xml");
                objectDao.streamObjectsGeometryById(os, id, type);
            } else if (type.equalsIgnoreCase("geojson")) {
                resp.setContentType("application/json; subtype=geojson");
                objectDao.streamObjectsGeometryById(os, id, type);
            } else if (type.equalsIgnoreCase("shp")) {
                resp.setContentType("application/zip");
                resp.setHeader("Content-Disposition", "attachment;filename=" + id + ".zip");
                objectDao.streamObjectsGeometryById(os, id, type);

                
            } else {
                os.write(("'" + type + "' type not supported yet.").getBytes());
            }
        } catch (Exception e) {
            logger.error("An error has occurred retrieving '" + type + "' for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(e));
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    logger.error("Error closing http request stream", e);
                }
            }
        }
    }

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
    
    
    // Create from geoJSON
    @RequestMapping(value = "/shape/upload/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadGeoJSON(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\" and \"user_id\"");
                return retMap;
            }

            String geojson = (String) parsedJSON.get("geojson");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");

            GeometryJSON gJson = new GeometryJSON();
            Geometry geometry = gJson.read(new StringReader(geojson));
            String wkt = geometry.toText();

            String pid = objectDao.createUserUploadedObject(wkt, name, description, userid);

            retMap.put("id", Integer.parseInt(pid));
        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (IOException ex) {
            logger.error("Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.", ex);
            retMap.put("error", "Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.");
        } catch (Exception ex) {
            logger.error("Error uploading geojson", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }
    
    // Create from WKT
    @RequestMapping(value = "/shape/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("wkt") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"wkt\", \"name\", \"description\" and \"user_id\"");
                return retMap;
            }

            String wkt = (String) parsedJSON.get("wkt");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");

            String pid = objectDao.createUserUploadedObject(wkt, name, description, userid);

            retMap.put("id", Integer.parseInt(pid));
        } catch (DataAccessException ex) {
            logger.error("Malformed WKT.", ex);
            retMap.put("error", "Malformed WKT.");
        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (Exception ex) {
            logger.error("Error uploading WKT", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }
}
