package org.ala.layers.dao;

import java.util.List;



public interface UploadDAO {

    public int storeGeometryFromWKT(String wkt, String name, String description, String userid);
    
    public int storeGeometryFromKML(String kml, String name, String description, String userid);
    
    public int storeGeometryFromGeoJSON(String geojson, String name, String description, String userid);
    
    public String getGeoJson(int pid);
    
    public String getKML(int pid);
    
    public String getWKT(int pid);
    
    public List<Integer> pointIntersect(double latitude, double longitude);
    
    public void deleteGeometry(int pid);
    
}
