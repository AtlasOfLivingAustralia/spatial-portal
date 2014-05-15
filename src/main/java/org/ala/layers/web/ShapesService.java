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
import org.ala.layers.util.JSONRequestBodyParser;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.geojson.geom.GeometryJSON;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
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

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("geojson", Map.class, false);
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("description", String.class, false);
        reqBodyParser.addParameter("user_id", String.class, false);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String wkt = null;
            Map<String, Object> geojsonAsMap = (Map<String, Object>) reqBodyParser.getParsedValue("geojson");
            try {
                String geojsonString = new ObjectMapper().writeValueAsString(geojsonAsMap);
                GeometryJSON gJson = new GeometryJSON();
                Geometry geometry = gJson.read(new StringReader(geojsonString));

                if (!geometry.isValid()) {
                    retMap.put("error", "Invalid geometry");
                }

                wkt = geometry.toText();
            } catch (Exception ex) {
                logger.error("Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.", ex);
                retMap.put("error", "Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.");
                return retMap;
            }

            String name = (String) reqBodyParser.getParsedValue("name");
            String description = (String) reqBodyParser.getParsedValue("description");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }

            try {
                if (pid != null) {
                    objectDao.updateUserUploadedObject(pid, wkt, name, description, user_id);
                    objectDao.updateObjectNames();
                    retMap.put("updated", true);
                } else {
                    String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, user_id);
                    objectDao.updateObjectNames();
                    retMap.put("id", Integer.parseInt(generatedPid));
                }

            } catch (Exception ex) {
                logger.error("Error uploading geojson", ex);
                retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
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

    private Map<String, Object> processWKTRequest(String json, Integer pid, boolean namesearch) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("wkt", String.class, false);
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("description", String.class, false);
        reqBodyParser.addParameter("user_id", String.class, false);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String wkt = (String) reqBodyParser.getParsedValue("wkt");
            String name = (String) reqBodyParser.getParsedValue("name");
            String description = (String) reqBodyParser.getParsedValue("description");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }

            if (!isWKTValid(wkt)) {
                retMap.put("error", "Invalid WKT");
            }

            try {
                if (pid != null) {
                    objectDao.updateUserUploadedObject(pid, wkt, name, description, user_id);
                    objectDao.updateObjectNames();
                    retMap.put("updated", true);
                } else {
                    String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, user_id, namesearch);
                    objectDao.updateObjectNames();
                    retMap.put("id", Integer.parseInt(generatedPid));
                }

            } catch (DataAccessException ex) {
                logger.error("Malformed WKT.", ex);
                retMap.put("error", "Malformed WKT.");
            } catch (Exception ex) {
                logger.error("Error uploading WKT", ex);
                retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
        }
        return retMap;
    }

    // Create from WKT
    @RequestMapping(value = "/shape/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json
            , @RequestParam(value = "namesearch", required = false, defaultValue = "true") Boolean namesearch
            ) throws Exception {
        return processWKTRequest(json, null, namesearch);
    }

    // Create from WKT
    @RequestMapping(value = "/shape/upload/wkt/{pid}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateWithWKT(@RequestBody String json, @PathVariable("pid") int pid
            , @RequestParam(value = "namesearch", required = false, defaultValue = "true") Boolean namesearch
            ) throws Exception {
        return processWKTRequest(json, pid, namesearch);
    }

    // UploadShapeFile
    @RequestMapping(value = "/shape/upload/shp", method = RequestMethod.POST)
    @ResponseBody
    public Map<Object, Object> uploadShapeFile(HttpServletRequest req, HttpServletResponse resp, @RequestParam(value = "user_id", required = false) String userId,
                                               @RequestParam(value = "api_key", required = false) String apiKey) throws Exception {
        // Use linked hash map to maintain key ordering
        Map<Object, Object> retMap = new LinkedHashMap<Object, Object>();

        File tmpZipFile = File.createTempFile("shpUpload", ".zip");

        if (!ServletFileUpload.isMultipartContent(req)) {
            String jsonRequestBody = IOUtils.toString(req.getReader());

            JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
            reqBodyParser.addParameter("user_id", String.class, false);
            reqBodyParser.addParameter("shp_file_url", String.class, false);
            reqBodyParser.addParameter("api_key", String.class, false);

            if (reqBodyParser.parseJSON(jsonRequestBody)) {

                String shpFileUrl = (String) reqBodyParser.getParsedValue("shp_file_url");
                userId = (String) reqBodyParser.getParsedValue("user_id");
                apiKey = (String) reqBodyParser.getParsedValue("api_key");

                if (!checkAPIKey(apiKey, userId)) {
                    retMap.put("error", "Invalid user ID or API key");
                    return retMap;
                }

                // Use shape file url from json body
                IOUtils.copy(new URL(shpFileUrl).openStream(), new FileOutputStream(tmpZipFile));
                retMap.putAll(handleZippedShapeFile(tmpZipFile));
            } else {
                retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
            }

        } else {
            if (!checkAPIKey(apiKey, userId)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }

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

        Pair<String, File> idFilePair = SpatialConversionUtils.extractZippedShapeFile(zippedShp);
        String uploadedShpId = idFilePair.getLeft();
        File shpFile = idFilePair.getRight();

        retMap.put("shp_id", uploadedShpId);

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

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("description", String.class, false);
        reqBodyParser.addParameter("user_id", String.class, false);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String name = (String) reqBodyParser.getParsedValue("name");
            String description = (String) reqBodyParser.getParsedValue("description");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }

            try {
                File shpFileDir = new File(System.getProperty("java.io.tmpdir"), shapeFileId);

                String wkt = SpatialConversionUtils.getShapeFileFeatureAsWKT(shpFileDir, featureIndex);

                if (!isWKTValid(wkt)) {
                    retMap.put("error", "Invalid geometry");
                }

                if (pid != null) {
                    objectDao.updateUserUploadedObject(pid, wkt, name, description, user_id);
                    retMap.put("updated", true);
                } else {
                    String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, user_id);
                    retMap.put("id", Integer.parseInt(generatedPid));
                }

            } catch (Exception ex) {
                logger.error("Error processsing shapefile feature request", ex);
                retMap.put("error", ex.getMessage());
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
        }

        return retMap;
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

    @RequestMapping(value = "/shape/upload/pointradius/{latitude}/{longitude}/{radius}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> createPointRadius(@RequestBody String json, @PathVariable("latitude") double latitude, @PathVariable("longitude") double longitude, @PathVariable("radius") double radius)
            throws Exception {
        return processPointRadiusRequest(json, null, latitude, longitude, radius);
    }

    @RequestMapping(value = "/shape/upload/pointradius/{objectPid}/{latitude}/{longitude}/{radius}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updateWithPointRadius(@RequestBody String json, @PathVariable("latitude") double latitude, @PathVariable("longitude") double longitude, @PathVariable("radius") double radius, @PathVariable("objectPid") int objectPid)
            throws Exception {
        return processPointRadiusRequest(json, objectPid, latitude, longitude, radius);
    }

    private Map<String, Object> processPointRadiusRequest(String json, Integer pid, double latitude, double longitude, double radiusKm) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("description", String.class, false);
        reqBodyParser.addParameter("user_id", String.class, false);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String name = (String) reqBodyParser.getParsedValue("name");
            String description = (String) reqBodyParser.getParsedValue("description");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }

            try {
                String wkt = SpatialConversionUtils.createCircleJs(longitude, latitude, radiusKm * 1000);
                if (pid == null) {
                    String generatedPid = objectDao.createUserUploadedObject(wkt, name, description, user_id);
                    retMap.put("id", Integer.parseInt(generatedPid));
                } else {
                    objectDao.updateUserUploadedObject(pid, wkt, name, description, user_id);
                    retMap.put("updated", true);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
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

            return (Boolean) parsedJSON.get("valid");
/*
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
*/
        } catch (Exception ex) {
            throw new RuntimeException("Error checking API key");
        }
    }

    @RequestMapping(value = "/shape/upload/{pid}", method = RequestMethod.DELETE)
    @ResponseBody
    public Map<String, Object> deleteShape(@PathVariable("pid") int pid) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            boolean success = objectDao.deleteUserUploadedObject(pid);
            retMap.put("success", success);
        } catch (Exception ex) {
            logger.error("Error deleting shape " + pid, ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    @RequestMapping(value = "/poi", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> createPointOfInterest(@RequestBody String json) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("object_id", String.class, true);
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("type", String.class, false);
        reqBodyParser.addParameter("latitude", Double.class, false);
        reqBodyParser.addParameter("longitude", Double.class, false);
        reqBodyParser.addParameter("bearing", Double.class, true);
        reqBodyParser.addParameter("user_id", String.class, false);
        reqBodyParser.addParameter("description", String.class, true);
        reqBodyParser.addParameter("focal_length", Double.class, true);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String object_id = (String) reqBodyParser.getParsedValue("object_id");
            String name = (String) reqBodyParser.getParsedValue("name");
            String type = (String) reqBodyParser.getParsedValue("type");
            Double latitude = (Double) reqBodyParser.getParsedValue("latitude");
            Double longitude = (Double) reqBodyParser.getParsedValue("longitude");
            Double bearing = (Double) reqBodyParser.getParsedValue("bearing");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String description = (String) reqBodyParser.getParsedValue("description");
            Double focal_length = (Double) reqBodyParser.getParsedValue("focal_length");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }
            try {
                int id = objectDao.createPointOfInterest(object_id, name, type, latitude, longitude, bearing, user_id, description, focal_length);
                retMap.put("id", id);
            } catch (Exception ex) {
                logger.error("Error creating point of interest", ex);
                retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
        }

        return retMap;
    }

    @RequestMapping(value = "/poi/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Map<String, Object> deletePointOfInterest(@PathVariable("id") int id, @RequestParam(value = "user_id", required = true, defaultValue = "") String userId,
                                                     @RequestParam(value = "api_key", required = true, defaultValue = "") String apiKey) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        if (!checkAPIKey(apiKey, userId)) {
            retMap.put("error", "Invalid user ID or API key");
            return retMap;
        }

        try {
            boolean success = objectDao.deletePointOfInterest(id);
            retMap.put("deleted", success);
        } catch (Exception ex) {
            logger.error("Error uploading point of interest " + id, ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    @RequestMapping(value = "/poi/{id}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> updatePointOfInterest(@RequestBody String json, @PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        JSONRequestBodyParser reqBodyParser = new JSONRequestBodyParser();
        reqBodyParser.addParameter("object_id", String.class, true);
        reqBodyParser.addParameter("name", String.class, false);
        reqBodyParser.addParameter("type", String.class, false);
        reqBodyParser.addParameter("latitude", Double.class, false);
        reqBodyParser.addParameter("longitude", Double.class, false);
        reqBodyParser.addParameter("bearing", Double.class, true);
        reqBodyParser.addParameter("user_id", String.class, true);
        reqBodyParser.addParameter("description", String.class, true);
        reqBodyParser.addParameter("focal_length", Double.class, true);
        reqBodyParser.addParameter("api_key", String.class, false);

        if (reqBodyParser.parseJSON(json)) {

            String object_id = (String) reqBodyParser.getParsedValue("object_id");
            String name = (String) reqBodyParser.getParsedValue("name");
            String type = (String) reqBodyParser.getParsedValue("type");
            Double latitude = (Double) reqBodyParser.getParsedValue("latitude");
            Double longitude = (Double) reqBodyParser.getParsedValue("longitude");
            Double bearing = (Double) reqBodyParser.getParsedValue("bearing");
            String user_id = (String) reqBodyParser.getParsedValue("user_id");
            String description = (String) reqBodyParser.getParsedValue("description");
            Double focal_length = (Double) reqBodyParser.getParsedValue("focal_length");
            String api_key = (String) reqBodyParser.getParsedValue("api_key");

            if (!checkAPIKey(api_key, user_id)) {
                retMap.put("error", "Invalid user ID or API key");
                return retMap;
            }
            try {
                boolean updateSuccessful = objectDao.updatePointOfInterest(id, object_id, name, type, latitude, longitude, bearing, user_id, description, focal_length);
                retMap.put("updated", updateSuccessful);
            } catch (Exception ex) {
                logger.error("Error updating point of interest " + id, ex);
                retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
            }
        } else {
            retMap.put("error", StringUtils.join(reqBodyParser.getErrorMessages(), ","));
        }

        return retMap;
    }

    @RequestMapping(value = "/poi/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getPointOfInterestDetails(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            return objectDao.getPointOfInterestDetails(id);
        } catch (IllegalArgumentException ex) {
            retMap.put("error", "Invalid point of interest id " + id);
        } catch (Exception ex) {
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    private boolean isWKTValid(String wkt) {
        WKTReader wktReader = new WKTReader();
        try {
            Geometry geom = wktReader.read(wkt);
            return geom.isValid();
        } catch (ParseException ex) {
            return false;
        }
    }

}
