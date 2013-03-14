package org.ala.layers.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import net.sf.json.JSON;

import org.ala.layers.dao.UploadDAO;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UploadService {
    
    @Resource(name="uploadDao")
    private UploadDAO uploadDao;

    // Create from WKT
    @RequestMapping(value = "/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map uploadWKT(@RequestBody String json) {
        Object obj = JSONValue.parse(json);
        JSONObject jsonObj = (JSONObject) obj;
        
        if (!(jsonObj.containsKey("wkt") && jsonObj.containsKey("name") && jsonObj.containsKey("description") && jsonObj.containsKey("userid"))) {
            throw new IllegalArgumentException("JSON object must contain wkt, name, description and userid");
        }
        
        String wkt = (String) jsonObj.get("wkt");
        String name = (String) jsonObj.get("name");
        String description = (String) jsonObj.get("description");
        String userid = (String) jsonObj.get("userid");
        
        int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);
        
        Map retMap = new HashMap();
        retMap.put("pid", pid);
        return retMap;
    }
    
    // Create from KML
//    @RequestMapping(value = "/upload/kml", method = RequestMethod.POST)
//    @ResponseBody
//    public Map uploadKML(@RequestParam(value = "wkt", required = true, defaultValue = "") String wkt,
//            @RequestParam(value = "name", required = true, defaultValue = "") String name,
//            @RequestParam(value = "description", required = true, defaultValue = "") String description,
//            @RequestParam(value = "userid", required = true, defaultValue = "") String userid) {
//        
//        int pid = uploadDao.storeGeometryFromKML(wkt, name, description, userid);
//        
//        Map retMap = new HashMap();
//        retMap.put("pid", pid);
//        return retMap;
//    }
    
    // Create from Shapefile
    //@RequestMapping(value = "/upload/shp", method = RequestMethod.POST)
    
    // Create from geoJSON
//    @RequestMapping(value = "/upload/geojson", method = RequestMethod.POST)
//    @ResponseBody
//    public Map uploadGeoJSON(@RequestParam(value = "wkt", required = true, defaultValue = "") String wkt,
//            @RequestParam(value = "name", required = true, defaultValue = "") String name,
//            @RequestParam(value = "description", required = true, defaultValue = "") String description,
//            @RequestParam(value = "userid", required = true, defaultValue = "") String userid) {
//        
//        int pid = uploadDao.storeGeometryFromGeoJSON(wkt, name, description, userid);
//        
//        Map retMap = new HashMap();
//        retMap.put("pid", pid);
//        return retMap;
//    }
    
    // Retrieve GeoJson
    @RequestMapping(value = "/upload/{id}/geojson", method = RequestMethod.GET)
    @ResponseBody
    public String getGeoJson(@RequestParam("id") int id) {
        return uploadDao.getGeoJson(id);
    }
    
    // Retrieve KML
    //@RequestMapping(value = "/upload/{id}/kml", method = RequestMethod.GET)
    @ResponseBody
    public String getKML(@RequestParam("id") int id) {
        return uploadDao.getKML(id);
    }
    
    // Retrieve shapefile
    //@RequestMapping(value = "/upload/{id}/shp", method = RequestMethod.GET)
    
    // Retrieve wkt
    //@RequestMapping(value = "/upload/{id}/wkt", method = RequestMethod.GET)
    @ResponseBody
    public String getWKT(@RequestParam("id") int id) {
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
