package org.ala.layers.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.ala.layers.dao.UploadDAO;
import org.ala.layers.util.SpatialConversionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.geojson.GeoJSON;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

@Controller
public class UploadService {

    @Resource(name = "uploadDao")
    private UploadDAO uploadDao;

    // Create from WKT
    @RequestMapping(value = "/upload/wkt", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadWKT(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("wkt") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                throw new IllegalArgumentException("JSON body must be an object with key value pairs for \"wkt\", \"name\", \"description\" and \"userid\"");
            }

            String wkt = (String) parsedJSON.get("wkt");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");

            int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);

            retMap.put("pid", pid);
        } catch (JsonParseException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (JsonMappingException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Create from KML
    @RequestMapping(value = "/upload/kml", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadKML(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("kml") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                throw new IllegalArgumentException("JSON body must be an object with key value pairs for \"kml\", \"name\", \"description\" and \"userid\"");
            }

            String kml = (String) parsedJSON.get("kml");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");

            int pid = uploadDao.storeGeometryFromKML(kml, name, description, userid);

            retMap.put("pid", pid);
        } catch (JsonParseException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (JsonMappingException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Create from Shapefile
    @RequestMapping(value = "/upload/shp", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadShapefile(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();

        try {
            ObjectMapper mapper = new ObjectMapper();

            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("zippedShpFileUrl") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                throw new IllegalArgumentException("JSON body must be an object with key value pairs for \"zippedShpFileUrl\", \"name\", \"description\" and \"userid\"");
            }

            String zippedShpFileUrl = (String) parsedJSON.get("zippedShpFileUrl");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");

            // Download the zipped shape file from the supplied URL

            File downloadedZippedShpFile = File.createTempFile("downloadedZippedShp", "zip");

            IOUtils.copy(new URL(zippedShpFileUrl).openStream(), new FileOutputStream(downloadedZippedShpFile));

            String wkt = convertShapeFileToWKT(new ZipFile(downloadedZippedShpFile));

            int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);

            retMap.put("pid", pid);
        } catch (JsonParseException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (JsonMappingException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }

        return retMap;
    }

    private String convertShapeFileToWKT(ZipFile zippedShp) {
        File tempDir = Files.createTempDir();
        File shpFile = null;

        ZipEntry entry;
        Enumeration<? extends ZipEntry> zipEntries = zippedShp.entries();
        while (zipEntries.hasMoreElements()) {
            entry = zipEntries.nextElement();
            File unzippedFile = new File(tempDir, entry.getName());
            try {
                IOUtils.copy(zippedShp.getInputStream(entry), new FileOutputStream(unzippedFile));
            } catch (IOException ex) {
                throw new RuntimeException("Error extracting shape file to disk");
            }

            if (entry.getName().endsWith(".shp")) {
                shpFile = unzippedFile;
            }
        }

        if (shpFile == null) {
            throw new IllegalArgumentException(".shp not included in zip");
        }

        String shpAsWkt = SpatialConversionUtils.shapefileToWKT(shpFile);
        return shpAsWkt;
    }

    // Create from geoJSON
    @RequestMapping(value = "/upload/geojson", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> uploadGeoJSON(@RequestBody String json) throws Exception {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map parsedJSON = mapper.readValue(json, Map.class);

            if (!(parsedJSON.containsKey("geojson") && parsedJSON.containsKey("name") && parsedJSON.containsKey("description") && parsedJSON.containsKey("userid"))) {
                throw new IllegalArgumentException("JSON body must be an object with key value pairs for \"geojson\", \"name\", \"description\" and \"userid\"");
            }

            String geojson = (String) parsedJSON.get("geojson");
            String name = (String) parsedJSON.get("name");
            String description = (String) parsedJSON.get("description");
            String userid = (String) parsedJSON.get("userid");
            
            
            GeometryJSON gJson = new GeometryJSON();
            Geometry geometry = gJson.read(new StringReader(geojson));
            String wkt = geometry.toText();

            int pid = uploadDao.storeGeometryFromWKT(wkt, name, description, userid);

            retMap.put("pid", pid);
        } catch (JsonParseException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (JsonMappingException ex) {
            retMap.put("error", "Invalid JSON in request");
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Retrieve GeoJson
    @RequestMapping(value = "/upload/{id}/geojson", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getGeoJson(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            retMap.put("geojson", uploadDao.getGeoJson(id));

        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Retrieve KML
    @RequestMapping(value = "/upload/{id}/kml", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getKML(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            retMap.put("kml", uploadDao.getGeoJson(id));

        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Retrieve shapefile
    @RequestMapping(value = "/upload/{id}/shp", method = RequestMethod.GET)
    @ResponseBody
    public void getShapeFile(@PathVariable("id") int id, HttpServletResponse response) throws Exception {
        String wkt = uploadDao.getWKT(id);
        final File shpFile = File.createTempFile("uploaded", ".shp");
        final String shpFileBaseName = shpFile.getName().replaceAll("\\.shp", "");
        SpatialConversionUtils.saveShapefile(shpFile, wkt);
        
        // build a zip file containing the shp file and also any associated files.
        
        final File temporaryZipFile = File.createTempFile("shapeFileDownload", "zip");
        ZipOutputStream zipOS = new ZipOutputStream(new FileOutputStream(temporaryZipFile));
        Iterator<File> iterFile = FileUtils.iterateFiles(shpFile.getParentFile(), new IOFileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getParentFile().equals(temporaryZipFile.getParentFile()) && file.getName().startsWith(shpFileBaseName);
            }

            @Override
            public boolean accept(File dir, String name) {
                return dir.equals(shpFile.getParent()) && name.startsWith(shpFileBaseName);
            }
            
        }, null);
        
        while(iterFile.hasNext()) {
            File nextFile = iterFile.next();
            ZipEntry zipEntry = new ZipEntry(nextFile.getName());
            zipOS.putNextEntry(zipEntry);            
            zipOS.write(FileUtils.readFileToByteArray(nextFile));
        }
        

        InputStream is = new FileInputStream(shpFile);
        IOUtils.copy(is, response.getOutputStream());
        response.setContentType("application/octet-stream");
        //response.set
        response.flushBuffer();
    }

    // Retrieve wkt
    @RequestMapping(value = "/upload/{id}/wkt", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getWKT(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            retMap.put("wkt", uploadDao.getWKT(id));

        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

    // Delete
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    public Map<String, Object> deleteLayer(@PathVariable("id") int id) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            boolean deleteSuccessful = uploadDao.deleteGeometry(id);
            retMap.put("deleted", deleteSuccessful);
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }

        return retMap;
    }

    // Point intersection
    @RequestMapping(value = "/upload/intersect", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> pointIntersect(@RequestParam(value = "latitude", required = true, defaultValue = "") double latitude,
            @RequestParam(value = "longitude", required = true, defaultValue = "") double longitude) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        try {
            List<Integer> intersectingPids = uploadDao.pointIntersect(latitude, longitude);
            retMap.put("pids", intersectingPids);
        } catch (Exception ex) {
            retMap.put("error", ex.getMessage());
        }
        return retMap;
    }

}
