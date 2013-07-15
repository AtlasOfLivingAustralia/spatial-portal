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
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.ObjectDAO;
import org.ala.layers.intersect.IntersectConfig;
import org.ala.layers.util.SpatialConversionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.springframework.web.bind.annotation.RequestParam;
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

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id") && parsedJSON
                    .containsKey("api_key"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\", \"user_id\" and \"api_key\"");
                return retMap;
            }

            Object geojsonObj = parsedJSON.get("geojson");
            String geojsonStr = mapper.writeValueAsString(geojsonObj);
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");
            String apiKey = (String) parsedJSON.get("api_key");

            if (!checkAPIKey(apiKey, userid)) {
                retMap.put("error", "Invalid API key");
                return retMap;
            }

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

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id") && parsedJSON
                    .containsKey("api_key"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\", \"user_id\" and \"api_key\"");
                return retMap;
            }

            String wkt = (String) parsedJSON.get("wkt");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");

            String apiKey = (String) parsedJSON.get("api_key");

            if (!checkAPIKey(apiKey, userid)) {
                retMap.put("error", "Invalid API key");
                return retMap;
            }

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
    public Map<Object, Object> uploadShapeFile(HttpServletRequest req, HttpServletResponse resp, @RequestParam(value = "user_id", required = true) String userId,
            @RequestParam(value = "api_key", required = true) String apiKey, @RequestParam(value = "shp_file_url", required = false) String shpFileUrl) throws Exception {
        // Use linked hash map to maintain key ordering
        Map<Object, Object> retMap = new LinkedHashMap<Object, Object>();

        File tmpZipFile = File.createTempFile("shpUpload", ".zip");

        if (!ServletFileUpload.isMultipartContent(req)) {
            if (shpFileUrl == null) {
                retMap.put("error", "shpFileUrl not supplied");
            }

            if (!checkAPIKey(apiKey, userId)) {
                retMap.put("error", "Invalid API key");
                return retMap;
            }

            // Use shape file url from json body
            IOUtils.copy(new URL(shpFileUrl).openStream(), new FileOutputStream(tmpZipFile));
            retMap.putAll(handleZippedShapeFile(tmpZipFile));
        } else {
            // Create a factory for disk-based file items. File size limit is
            // 50MB
            // Configure a repository (to ensure a secure temp location is used)
            File repository = new File(System.getProperty("java.io.tmpdir"));
            DiskFileItemFactory factory = new DiskFileItemFactory(1024 * 1024 * 50, repository);

            factory.setRepository(repository);

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

            // Parse the request
            List<FileItem> items = upload.parseRequest(req);

            if (items.size() == 1) {
                FileItem fileItem = items.get(0);
                IOUtils.copy(fileItem.getInputStream(), new FileOutputStream(tmpZipFile));
                retMap.putAll(handleZippedShapeFile(tmpZipFile));
            } else {
                retMap.put("error", "Multiple files sent in request. A single zipped shape file should be supplied.");
            }
        }

        return retMap;
    }

    private Map<Object, Object> handleZippedShapeFile(File zippedShp) throws IOException {
        // Use linked hash map to maintain key ordering
        Map<Object, Object> retMap = new LinkedHashMap<Object, Object>();

        File shpFile = SpatialConversionUtils.extractZippedShapeFile(zippedShp);

        List<List<Pair<String, Object>>> manifestData = SpatialConversionUtils.getShapeFileManifest(shpFile);

        int featureIndex = 0;
        for (List<Pair<String, Object>> featureData : manifestData) {
            // Use linked hash map to maintain key ordering
            Map<String, Object> featureDataMap = new LinkedHashMap<String, Object>();

            for (Pair<String, Object> fieldData : featureData) {
                featureDataMap.put(fieldData.getLeft(), fieldData.getRight());
            }

            retMap.put(featureIndex, featureDataMap);

            featureIndex++;
        }

        return retMap;
    }

    private Map<String, Object> processShapeFileFeatureRequest(String json, Integer pid, String shapeFileId, int featureIndex) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        Map parsedJSON;
        try {
            ObjectMapper mapper = new ObjectMapper();
            parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id") && parsedJSON.containsKey("api_key"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"name\", \"description\", \"user_id\" and \"api_key\"");
                return retMap;
            }

            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");
            String apiKey = (String) parsedJSON.get("api_key");

            if (!checkAPIKey(apiKey, userid)) {
                retMap.put("error", "Invalid API key");
                return retMap;
            }

            File shpFileDir = new File(System.getProperty("java.io.tmpdir"), shapeFileId);

            String wkt = SpatialConversionUtils.getShapeFileFeatureAsWKT(shpFileDir, featureIndex);

            String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, userid);
            retMap.put("id", Integer.parseInt(generatedPid));

        } catch (Exception ex) {

        }

        return null;
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/shp/{shapeId}/{featureIndex}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveFeatureFromShapeFile(@RequestBody String json, @PathVariable("shapeId") String shapeId, @PathVariable("featureIndex") int featureIndex) throws Exception {
        return processShapeFileFeatureRequest(json, null, shapeId, featureIndex);
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/shp/{objectPid}/{shapeId}/{featureIndex}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateFromShapeFileFeature(@RequestBody String json, @PathVariable("objectPid") int objectPid, @PathVariable("shapeId") String shapeId,
            @PathVariable("featureIndex") int featureIndex) throws Exception {
        return processShapeFileFeatureRequest(json, objectPid, shapeId, featureIndex);
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/pointradius/{latitude}/{longitude}/{radius}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> createPointRadius(@RequestBody String json, @PathVariable("latitude") double latitude, @PathVariable("longitude") double longitude, @PathVariable("radius") double radius)
            throws Exception {
        return processPointRadiusRequest(json, null, latitude, longitude, radius);
    }

    private Map<String, Object> processPointRadiusRequest(String json, Integer pid, double latitude, double longitude, double radiusKm) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        Map parsedJSON;
        try {
            ObjectMapper mapper = new ObjectMapper();
            parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("user_id") && parsedJSON.containsKey("api_key"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"name\", \"description\", \"user_id\" and \"api_key\"");
                return retMap;
            }

            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("user_id");
            String apiKey = (String) parsedJSON.get("api_key");

            if (!checkAPIKey(apiKey, userid)) {
                retMap.put("error", "Invalid API key");
                return retMap;
            }

            if (pid == null) {
                String wkt = SpatialConversionUtils.createCircleJs(longitude, latitude, radiusKm * 1000);
                String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, userid);
                retMap.put("id", Integer.parseInt(generatedPid));
            } else {
                return null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return retMap;
    }

    private boolean checkAPIKey(String apiKey, String userId) {
        try {
            HttpClient httpClient = new HttpClient();
            GetMethod get = new GetMethod(MessageFormat.format(IntersectConfig.getApiKeyCheckUrlTemplate(), apiKey));

            int returnCode = httpClient.executeMethod(get);
            if (returnCode != 200) {
                throw new RuntimeException("Error occurred checking api key");
            }

            String responseText = get.getResponseBodyAsString();

            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(responseText, Map.class);

            boolean valid = (Boolean) parsedJSON.get("valid");

            if (valid) {
                String keyUserId = (String) parsedJSON.get("userId");
                String app = (String) parsedJSON.get("app");

                if (!keyUserId.equals(userId)) {
                    return false;
                }

                if (!app.equals(IntersectConfig.getSpatialPortalAppName())) {
                    return false;
                }

                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error checking API key");
        }
    }

}
