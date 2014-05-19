package au.org.ala.spatial.dto;

/**
 * Created by a on 6/05/2014.
 */
public class ShapeObjectDTO {
    String wkt;
    String name;
    String description;
    String user_id;
    String api_key;

    public ShapeObjectDTO(String wkt, String name, String description, String user_id, String api_key) {
        this.wkt = wkt;
        this.name = name;
        this.description = description;
        this.user_id = user_id;
        this.api_key = api_key;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }
}
