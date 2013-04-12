package org.ala.layers.dao;

import java.util.List;
import java.util.Map;



public interface GeometryDAO {

    public int storeGeometryFromWKT(String wkt, String name, String description, String userid) throws InvalidGeometryException;
    
    // Returns map containing "geojson" - geometry in geoJSON format (String), "id" - geometry id (int), "name" - geometry name (String), "description" - geometry description (String), "user_id" - id of user who
    // uploaded the geometry (String), time_added - time at which the geometry was uploaded (Date)
    public Map<String, Object> getGeoJSONAndMetadata(int pid) throws InvalidIdException;
    
    // Returns map containing "wkt" - geometry in wkt format (String), "id" - geometry id (int), "name" - geometry name (String), "description" - geometry description (String), "user_id" - id of user who
    // uploaded the geometry (String), time_added - time at which the geometry was uploaded (Date)
    public Map<String, Object> getWKTAndMetadata(int pid) throws InvalidIdException;
    
    public List<Integer> pointRadiusIntersect(double latitude, double longitude, double radiusKilometres);
    
    public List<Integer> areaIntersect(String wkt) throws InvalidGeometryException;
    
    public boolean deleteGeometry(int pid) throws InvalidIdException;
    
    public static class InvalidGeometryException extends Exception {}
    public static class InvalidIdException extends Exception {}
}
