package org.ala.layers.web;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.ala.layers.dao.GeometryDAO;
import org.ala.layers.dao.GeometryDAO.InvalidGeometryException;
import org.ala.layers.dao.GeometryDAO.InvalidIdException;
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

@Controller
public class GeometryService {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(GeometryService.class);

    @Resource(name = "geometryDao")
    private GeometryDAO geometryDao;

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

    // Create from WKT
    @RequestMapping(value = "/geometry/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("wkt") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"wkt\", \"name\", \"description\" and \"userid\"");
                return retMap;
            }

            String wkt = (String) parsedJSON.get("wkt");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");

            int pid = geometryDao.storeGeometryFromWKT(wkt, name, description, userid);

            retMap.put("id", pid);
        } catch (DataAccessException ex) {
            logger.error("Malformed WKT.", ex);
            retMap.put("error", "Malformed WKT.");
        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (GeometryDAO.InvalidGeometryException ex) {
            logger.error("Invalid geometry.", ex);
            retMap.put("error", "Invalid geometry. See web service documentation for a discussion of valid geometries.");
        } catch (Exception ex) {
            logger.error("Error uploading WKT", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // Retrieve wkt
    @RequestMapping(value = { "/geometry/{id}/wkt" }, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getWKT(@PathVariable("id") int id) {
        // Use linked hash map to maintain ordering
        Map<String, Object> retMap = new LinkedHashMap<String, Object>();
        try {
            Map<String, Object> wktAndMetadata = geometryDao.getWKTAndMetadata(id);
            retMap.put("wkt", wktAndMetadata.get("wkt"));
            retMap.put("id", wktAndMetadata.get("id"));
            retMap.put("name", wktAndMetadata.get("name"));
            retMap.put("description", wktAndMetadata.get("description"));
            retMap.put("user_id", wktAndMetadata.get("user_id"));
            retMap.put("time_added", df.format(wktAndMetadata.get("time_added")));
        } catch (InvalidIdException ex) {
            logger.error("Invalid geometry ID: " + id, ex);
            retMap.put("error", "Invalid geometry ID: " + id);
        } catch (Exception ex) {
            logger.error("Error getting WKT", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // Create from geoJSON
    @RequestMapping(value = "/geometry/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadGeoJSON(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                retMap.put("error", "JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\" and \"userid\"");
                return retMap;
            }

            String geojson = (String) parsedJSON.get("geojson");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");

            GeometryJSON gJson = new GeometryJSON();
            Geometry geometry = gJson.read(new StringReader(geojson));
            String wkt = geometry.toText();

            int pid = geometryDao.storeGeometryFromWKT(wkt, name, description, userid);

            retMap.put("id", pid);
        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (IOException ex) {
            logger.error("Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.", ex);
            retMap.put("error", "Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.");
        } catch (InvalidGeometryException ex) {
            logger.error("Invalid geometry.", ex);
            retMap.put("error", "Invalid geometry. See web service documentation for a discussion of valid geometries.");
        } catch (Exception ex) {
            logger.error("Error uploading geojson", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // Retrieve GeoJson
    @RequestMapping(value = "/geometry/{id}/geojson", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getGeoJson(@PathVariable("id") int id) {
        // Use linked hash map to maintain ordering
        Map<String, Object> retMap = new LinkedHashMap<String, Object>();
        try {
            Map<String, Object> geoJsonAndMetadata = geometryDao.getGeoJSONAndMetadata(id);
            retMap.put("geojson", geoJsonAndMetadata.get("geojson"));
            retMap.put("id", geoJsonAndMetadata.get("id"));
            retMap.put("name", geoJsonAndMetadata.get("name"));
            retMap.put("description", geoJsonAndMetadata.get("description"));
            retMap.put("user_id", geoJsonAndMetadata.get("user_id"));
            retMap.put("time_added", df.format(geoJsonAndMetadata.get("time_added")));
        } catch (InvalidIdException ex) {
            logger.error("Invalid geometry ID: " + id, ex);
            retMap.put("error", "Invalid geometry ID: " + id);
        } catch (Exception ex) {
            logger.error("Error getting geojson", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // // Create from Shapefile
    // @RequestMapping(value = "/upload/shp", method = RequestMethod.POST)
    // @ResponseBody
    // public Map<String, Object> uploadShapefile(@RequestBody String json)
    // throws Exception {
    // Map<String, Object> retMap = new HashMap<String, Object>();
    //
    // try {
    // ObjectMapper mapper = new ObjectMapper();
    //
    // Map parsedJSON = mapper.readValue(json, Map.class);
    //
    // if (!(parsedJSON.containsKey("zippedShpFileUrl") &&
    // parsedJSON.containsKey("name") && parsedJSON.containsKey("description")
    // && parsedJSON.containsKey("userid"))) {
    // throw new
    // IllegalArgumentException("JSON body must be an object with key value pairs for \"zippedShpFileUrl\", \"name\", \"description\" and \"userid\"");
    // }
    //
    // String zippedShpFileUrl = (String) parsedJSON.get("zippedShpFileUrl");
    // String name = (String) parsedJSON.get("name");
    // String description = (String) parsedJSON.get("description");
    // String userid = (String) parsedJSON.get("userid");
    //
    // // Download the zipped shape file from the supplied URL
    // File downloadedZippedShpFile = File.createTempFile("downloadedZippedShp",
    // "zip");
    //
    // //TODO - what if shape file is too big????
    //
    // IOUtils.copy(new URL(zippedShpFileUrl).openStream(), new
    // FileOutputStream(downloadedZippedShpFile));
    //
    // String wkt = convertShapeFileToWKT(new ZipFile(downloadedZippedShpFile));
    //
    // int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);
    //
    // downloadedZippedShpFile.delete();
    //
    // retMap.put("pid", pid);
    // } catch (JsonParseException ex) {
    // retMap.put("error", "Invalid JSON in request");
    // } catch (JsonMappingException ex) {
    // retMap.put("error", "Invalid JSON in request");
    // } catch (Exception ex) {
    // retMap.put("error", ex.getMessage());
    // }
    //
    // return retMap;
    // }
    //
    // private String convertShapeFileToWKT(ZipFile zippedShp) {
    // File tempDir = Files.createTempDir();
    // File shpFile = null;
    //
    // ZipEntry entry;
    // Enumeration<? extends ZipEntry> zipEntries = zippedShp.entries();
    // while (zipEntries.hasMoreElements()) {
    // entry = zipEntries.nextElement();
    // File unzippedFile = new File(tempDir, entry.getName());
    // try {
    // IOUtils.copy(zippedShp.getInputStream(entry), new
    // FileOutputStream(unzippedFile));
    // } catch (IOException ex) {
    // throw new RuntimeException("Error extracting shape file to disk");
    // }
    //
    // if (entry.getName().endsWith(".shp")) {
    // shpFile = unzippedFile;
    // }
    // }
    //
    // if (shpFile == null) {
    // throw new IllegalArgumentException(".shp not included in zip");
    // }
    //
    // String shpAsWkt = SpatialConversionUtils.shapefileToWKT(shpFile);
    // return shpAsWkt;
    // }
    //
    //
    // // Retrieve shapefile
    // @RequestMapping(value = "/uploaded/{id}/shp", method = RequestMethod.GET)
    // @ResponseBody
    // public void getShapeFile(@PathVariable("id") int id, HttpServletResponse
    // response) throws Exception {
    // String wkt = uploadDao.getWKT(id);
    // final File shpFile = File.createTempFile("uploaded", ".shp");
    // final String shpFileBaseName = shpFile.getName().replaceAll("\\.shp",
    // "");
    // SpatialConversionUtils.saveShapefile(shpFile, wkt);
    //
    // response.setContentType("application/zip");
    // response.setHeader("Content-Disposition", "attachment;filename=" +
    // shpFileBaseName + ".zip");
    //
    // // build a zip file containing the shp file and also any associated
    // files.
    //
    // ZipOutputStream zipOS = new ZipOutputStream(response.getOutputStream());
    // Iterator<File> iterFile = FileUtils.iterateFiles(shpFile.getParentFile(),
    // new IOFileFilter() {
    //
    // @Override
    // public boolean accept(File file) {
    // return file.getParentFile().equals(shpFile.getParentFile()) &&
    // file.getName().startsWith(shpFileBaseName);
    // }
    //
    // @Override
    // public boolean accept(File dir, String name) {
    // return dir.equals(shpFile.getParent()) &&
    // name.startsWith(shpFileBaseName);
    // }
    //
    // }, null);
    //
    // while(iterFile.hasNext()) {
    // File nextFile = iterFile.next();
    // ZipEntry zipEntry = new ZipEntry(nextFile.getName());
    // zipOS.putNextEntry(zipEntry);
    // zipOS.write(FileUtils.readFileToByteArray(nextFile));
    // zipOS.closeEntry();
    // }
    //
    // zipOS.close();
    //
    // //response.set
    // response.flushBuffer();
    // }
    //
    // // Retrieve KML
    // @RequestMapping(value = "/uploaded/{id}/kml", method = RequestMethod.GET)
    // @ResponseBody
    // public Map<String, Object> getKML(@PathVariable("id") int id) {
    // // Need to add wrapper document around this
    //
    // Map<String, Object> retMap = new HashMap<String, Object>();
    // try {
    // retMap.put("kml", uploadDao.getKML(id));
    //
    // } catch (Exception ex) {
    // retMap.put("error", ex.getMessage());
    // }
    // return retMap;
    // }

    // Delete
    @RequestMapping(value = "/geometry/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Map<String, Object> deleteLayer(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            boolean deleteSuccessful = geometryDao.deleteGeometry(id);
            retMap.put("deleted", deleteSuccessful);
        } catch (Exception ex) {
            logger.error("error", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }

        return retMap;
    }

    // Point/radius intersection
    @RequestMapping(value = "/geometry/intersect_point_radius", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> pointRadiusIntersect(@RequestParam(value = "latitude", required = true, defaultValue = "") double latitude,
            @RequestParam(value = "longitude", required = true, defaultValue = "") double longitude, @RequestParam(value = "radius", required = true, defaultValue = "") double radiusKilometres) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if (latitude < -90 || latitude > 90) {
            retMap.put("error", "Invalid latitude and or longitude. Expecting latitude -90 to 90 and longitude -180 to 180.");
        } else if (longitude < -180 || longitude > 180) {
            retMap.put("error", "Invalid latitude and or longitude. Expecting latitude -90 to 90 and longitude -180 to 180.");
        } else if (radiusKilometres < 0) {
            retMap.put("error", "Radius must be a positive value in kilometres.");
        }

        try {
            List<Integer> intersectingIds = geometryDao.pointRadiusIntersect(latitude, longitude, radiusKilometres);
            retMap.put("intersecting_ids", intersectingIds);
        } catch (Exception ex) {
            logger.error("error", ex);
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // Area intersection
    @RequestMapping(value = "/geometry/intersect_area/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> areaIntersectWkt(@RequestBody String json) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!parsedJSON.containsKey("wkt")) {
                retMap.put("error", "JSON body must be an object with key value pair for \"wkt\"");
                return retMap;
            }

            String wkt = (String) parsedJSON.get("wkt");
            List<Integer> intersectingIds = geometryDao.areaIntersect(wkt);

            retMap.put("intersecting_ids", intersectingIds);
        } catch (JsonParseException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (JsonMappingException ex) {
            logger.error("Malformed request. Expecting a JSON object.", ex);
            retMap.put("error", "Malformed request. Expecting a JSON object.");
        } catch (IOException ex) {
            logger.error("Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.", ex);
            retMap.put("error", "Malformed GeoJSON geometry. Note that only GeoJSON geometries can be supplied here. Features and FeatureCollections cannot.");
        } catch (DataAccessException ex) {
            logger.error("Malformed WKT.", ex);
            retMap.put("error", "Malformed WKT.");
        } catch (Exception ex) {
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

    // Area intersection
    @RequestMapping(value = "/geometry/intersect_area/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> areaIntersectGeoJSON(@RequestBody String json) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("geojson"))) {
                retMap.put("error", "JSON body must be an object with key value pair for \"geojson\"");
                return retMap;
            }

            String geojson = (String) parsedJSON.get("geojson");

            // Convert geojson to WKT then use that
            GeometryJSON gJson = new GeometryJSON();
            Geometry geometry = gJson.read(new StringReader(geojson));
            String geoJsonAsWkt = geometry.toText();
            List<Integer> intersectingIds = geometryDao.areaIntersect(geoJsonAsWkt);

            retMap.put("intersecting_ids", intersectingIds);
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
            retMap.put("error", "Unexpected error. Please notify support@ala.org.au.");
        }
        return retMap;
    }

}
