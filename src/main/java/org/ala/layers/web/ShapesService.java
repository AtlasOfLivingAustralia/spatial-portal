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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.util.SpatialConversionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
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
            // validate object id
            id = cleanObjectId(id);

            // List<Objects> objects = objectDao.getObjectsById(id);
            // if (objects.size() > 0) {
            // Geometry geom = objects.get(0).getGeometry();
            // if (type.equalsIgnoreCase("wkt")) {
            // WKTWriter wkt = new WKTWriter();
            // return wkt.write(geom);
            // } else if (type.equalsIgnoreCase("kml")) {
            // Encoder e = new Encoder(new KMLConfiguration());
            // e.setIndenting(true);
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // e.encode(geom, KML.Geometry, baos);
            // String kmlGeometry = new String(baos.toByteArray());
            // return kmlGeometry.substring(kmlGeometry.indexOf('\n'));
            // } else if (type.equalsIgnoreCase("geojson")) {
            // return "Not supported yet.";
            // }
            //
            // } else {
            // return "";
            // }

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
    
    
    private Map<String, Object> processGeoJSONRequest(String json, Integer pid) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        Map parsedJSON;
        try {
            ObjectMapper mapper = new ObjectMapper();
            parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\" and \"user_id\"");
                return retMap;
            }

            Object geojsonObj = parsedJSON.get("geojson");
            String geojsonStr = mapper.writeValueAsString(geojsonObj);
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");

            GeometryJSON gJson = new GeometryJSON();
            Geometry geometry = gJson.read(new StringReader(geojsonStr));
            String wkt = geometry.toText();

            if (pid != null) {
                objectDao.updateUserUploadedObject(pid, wkt, name, description, userid);
                retMap.put("updated", true);
            } else {
                String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, userid);
                retMap.put("id", Integer.parseInt(generatedPid));
            }

        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
            return retMap;
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
            return retMap;
        } catch (IOException ex) {
            logger.error("Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.", ex);
            retMap.put("error", "Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.");
        } catch (Exception ex) {
            logger.error("Error uploading geojson", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;        
    }

    // Create from geoJSON
    @RequestMapping(value = "/shape/upload/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadGeoJSON(@RequestBody String json) throws Exception {
        return processGeoJSONRequest(json, null);
    }
    
    // Create from geoJSON
    @RequestMapping(value = "/shape/upload/geojson/{pid}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateWithGeoJSON(@RequestBody String json, @PathVariable("pid") int pid) throws Exception {
        return processGeoJSONRequest(json, pid);
    }
    
    private Map<String, Object> processWKTRequest(String json, Integer pid) {
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

            if (pid != null) {
                objectDao.updateUserUploadedObject(pid, wkt, name, description, userid);
                retMap.put("updated", true);
            } else {
                String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, userid);
                retMap.put("id", Integer.parseInt(generatedPid));
            }

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

    // Create from WKT
    @RequestMapping(value = "/shape/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json) throws Exception {
        return processWKTRequest(json, null);
    }
    
    // Create from WKT
    @RequestMapping(value = "/shape/upload/wkt/{pid}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateWithWKT(@RequestBody String json, @PathVariable("pid") int pid) throws Exception {
        return processWKTRequest(json, pid);
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/shp", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadShapeFile(@RequestBody String json, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Configure a repository (to ensure a secure temp location is used)

        File repository = new File(System.getProperty("java.io.tmpdir"));
        factory.setRepository(repository);

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List<FileItem> items = upload.parseRequest(req);

        if (items.size() != 1) {

        } else {
            FileItem fileItem = items.get(0);
            File tmpZipFile = File.createTempFile("shpUpload", ".zip");
            IOUtils.copy(fileItem.getInputStream(), new FileOutputStream(tmpZipFile));

            File shpFile = SpatialConversionUtils.extractZippedShapeFile(tmpZipFile);

            SpatialConversionUtils.getShapeFileManifest(shpFile);

        }

        return null;
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/shp", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveFeatureFromShapeFile(@RequestBody String json) throws Exception {

        return null;
    }
    
    
}
