package org.ala.layers.dao;



public interface UploadDAO {

    public int uploadWKT(String wkt, String name, String description, String userid);
    
    public String getGeoJson(int pid);
}
