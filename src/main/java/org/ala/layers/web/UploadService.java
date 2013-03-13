package org.ala.layers.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.ala.layers.dao.UploadDAO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UploadService {
    
    @Resource(name="uploadDao")
    private UploadDAO uploadDao;

    @RequestMapping(value = "/uploadwkt", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Map uploadWKT(@RequestParam(value = "wkt", required = true, defaultValue = "") String wkt,
            @RequestParam(value = "name", required = true, defaultValue = "") String name,
            @RequestParam(value = "description", required = true, defaultValue = "") String description,
            @RequestParam(value = "userid", required = true, defaultValue = "") String userid) {
        
        int pid = uploadDao.uploadWKT(wkt, name, description, userid);
        
        Map retMap = new HashMap();
        retMap.put("pid", pid);
        return retMap;
    }
    
    public void uploadShapeFile() {
        
    }
    
    public void uploadKML() {
        
    }
    
    @RequestMapping(value = "/getgeojson", method = RequestMethod.GET)
    @ResponseBody
    public String getGeoJson(@RequestParam("id") int id) {
        return uploadDao.getGeoJson(id);
    }
    
}
