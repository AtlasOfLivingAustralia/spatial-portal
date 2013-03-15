package org.ala.layers.web;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.ala.layers.dao.UploadDAO;
import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

@Controller
public class UploadService {
    
    @Resource(name="uploadDao")
    private UploadDAO uploadDao;

    // Create from WKT
    @RequestMapping(value = "/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        Map parsedJSON = mapper.readValue(json, Map.class);
        
        if (!(parsedJSON.containsKey("wkt") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
            throw new IllegalArgumentException("JSON body must contain wkt, name, description and userid");
        }
        
        String wkt = (String) parsedJSON.get("wkt");
        String name = (String) parsedJSON.get("name");
        String description = (String) parsedJSON.get("description");
        String userid = (String) parsedJSON.get("userid");
        
        int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);
        
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("pid", pid);
        return retMap;
    }
    
    // Create from KML
    @RequestMapping(value = "/upload/kml", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadKML(@RequestBody String json) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        Map parsedJSON = mapper.readValue(json, Map.class);
        
        if (!(parsedJSON.containsKey("kml") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
            throw new IllegalArgumentException("JSON body must contain kml, name, description and userid");
        }
        
        String kml = (String) parsedJSON.get("kml");
        String name = (String) parsedJSON.get("name");
        String description = (String) parsedJSON.get("description");
        String userid = (String) parsedJSON.get("userid");
        
        int pid = uploadDao.storeGeometryFromKML(kml, name, description, userid);
        
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("pid", pid);
        return retMap;
    }
    
    // Create from Shapefile
    //@RequestMapping(value = "/upload/shp", method = RequestMethod.POST)
    
    // Create from geoJSON
    @RequestMapping(value = "/upload/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadGeoJSON(@RequestBody String json) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        Map parsedJSON = mapper.readValue(json, Map.class);
        
        if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
            throw new IllegalArgumentException("JSON body must contain geojson, name, description and userid");
        }
        
        String geojson = (String) parsedJSON.get("geojson");
        String name = (String) parsedJSON.get("name");
        String description = (String) parsedJSON.get("description");
        String userid = (String) parsedJSON.get("userid");
        
        int pid = uploadDao.storeGeometryFromGeoJSON(geojson, name, description, userid);
        
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("pid", pid);
        return retMap;
    }
    
    // Retrieve GeoJson
    @RequestMapping(value = "/upload/{id}/geojson", method = RequestMethod.GET)
    @ResponseBody
    public String getGeoJson(@PathVariable ("id") int id) {
        return uploadDao.getGeoJson(id);
    }
    
    // Retrieve KML
    @RequestMapping(value = "/upload/{id}/kml", method = RequestMethod.GET)
    @ResponseBody
    public String getKML(@PathVariable ("id") int id) {
        return uploadDao.getKML(id);
    }
    
    // Retrieve shapefile
    @RequestMapping(value = "/upload/{id}/shp", method = RequestMethod.GET)
    @ResponseBody
    public String getShapeFile(@PathVariable ("id") int id) {
        return uploadDao.getKML(id);
    }
    
    // Retrieve wkt
    @RequestMapping(value = "/upload/{id}/wkt", method = RequestMethod.GET)
    @ResponseBody
    public String getWKT(@PathVariable("id") int id) {
        return uploadDao.getWKT(id);
    }
    
    // Delete
    //@RequestMapping(value = "/upload/{id}", method = RequestMethod.DELETE)

    // Point intersection
    @RequestMapping(value = "/upload/intersect", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> pointIntersect(@RequestParam(value = "latitude", required = true, defaultValue = "") double latitude,
            @RequestParam(value = "longitude", required = true, defaultValue = "") double longitude) {
        
        Map<String, Object> retMap = new HashMap<String, Object>();
        
        List<Integer> intersectingPids = uploadDao.pointIntersect(latitude, longitude);
        retMap.put("pids", intersectingPids);
        
        return retMap;
    }
    
}
