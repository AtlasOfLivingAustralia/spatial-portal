package org.ala.layers.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

@Service("geometryDao")
public class GeometryDAOImpl implements GeometryDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(GeometryDAOImpl.class);
    private SimpleJdbcTemplate _jdbcTemplate;
    
    // Date formatter for formatting date strings
    
    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this._jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }
    
    @Override
    public int storeGeometryFromWKT(String wkt, String name, String description, String userid) throws GeometryDAO.InvalidGeometryException {
        if (wkt.toUpperCase().contains("GEOMETRYCOLLECTION")) {
            throw new GeometryDAO.InvalidGeometryException();
        }
        
        if (!isGeometryValid(wkt)) {
            throw new GeometryDAO.InvalidGeometryException();
        }
        
        String sql = "INSERT INTO uploaded (pid, name, description, user_id, time_added, the_geom, geog) values (DEFAULT, ?, ?, ?, now(), ST_GeomFromText(?, 4326), Geography(ST_GeomFromText(?, 4326)))";
        _jdbcTemplate.update(sql, name, description, userid, wkt, wkt);
        
        // get id of uploaded layer
        String sql2 = "SELECT MAX(pid) from uploaded";
        int id = _jdbcTemplate.queryForInt(sql2);
        
        return id;
    }
    
    // Returns geometry geojson (String), id (Integer), name (String), description (String), user_id (String), time_added (Date)
    @Override
    public Map<String, Object> getGeoJSONAndMetadata(int id) throws GeometryDAO.InvalidIdException {
        if (!isLayerIdValid(id)) {
            throw new InvalidIdException();
        }
        
        String sql = "SELECT ST_AsGeoJSON(the_geom), pid, name, description, user_id, time_added from uploaded where pid = ?";

        RowMapper<Map<String, Object>> rm = new RowMapper<Map<String, Object>>() {

            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> rowMap = new HashMap<String, Object>();
                
                rowMap.put("geojson", rs.getString(1));
                rowMap.put("id",rs.getInt(2));
                rowMap.put("name",rs.getString(3));
                rowMap.put("description",rs.getString(4));
                rowMap.put("user_id", rs.getString(5));
                rowMap.put("time_added",rs.getTime(6));
                
                return rowMap;
            }
            
        };
        
        return _jdbcTemplate.query(sql, rm, Integer.valueOf(id)).get(0);
    }

    // Returns geometry wkt (String), id (Integer), name (String), description (String), user_id (String), time_added (Date)
    @Override
    public Map<String, Object> getWKTAndMetadata(int id) throws GeometryDAO.InvalidIdException {
        if (!isLayerIdValid(id)) {
            throw new InvalidIdException();
        }
        
        String sql = "SELECT ST_AsText(the_geom), pid, name, description, user_id, time_added from uploaded where pid = ?";
        
        RowMapper<Map<String, Object>> rm = new RowMapper<Map<String, Object>>() {

            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> rowMap = new HashMap<String, Object>();
                
                rowMap.put("wkt", rs.getString(1));
                rowMap.put("id",rs.getInt(2));
                rowMap.put("name",rs.getString(3));
                rowMap.put("description",rs.getString(4));
                rowMap.put("user_id", rs.getString(5));
                rowMap.put("time_added",rs.getTime(6));
                
                return rowMap;
            }
            
        };
        
        return _jdbcTemplate.query(sql, rm, Integer.valueOf(id)).get(0);
    }
    
    public List<Integer> pointRadiusIntersect(double latitude, double longitude, double radiusKilometres) {
        double radiusMetres = radiusKilometres * 1000;
        String sql = MessageFormat.format("SELECT pid from uploaded WHERE ST_DWithin(geog, Geography(ST_GeomFromText(''POINT({0} {1})'', 4326)), {2}) ORDER BY pid", Double.toString(longitude), Double.toString(latitude), Double.toString(radiusMetres));
        
        RowMapper<Integer> rowMapper = new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }
            
        };
        
        return _jdbcTemplate.query(sql, rowMapper);
    }
    
    public List<Integer> areaIntersect(String wkt) throws GeometryDAO.InvalidGeometryException {
        if (wkt.toUpperCase().contains("GEOMETRYCOLLECTION")) {
            throw new GeometryDAO.InvalidGeometryException();
        }
        
        if (!isGeometryValid(wkt)) {
            throw new GeometryDAO.InvalidGeometryException();
        }
        
        String sql = "SELECT pid from uploaded WHERE ST_Intersects(the_geom, ST_GeomFromText(?, 4326)) ORDER BY pid";
        
        RowMapper<Integer> rowMapper = new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }
            
        };
        
        return _jdbcTemplate.query(sql, rowMapper,  wkt);
    }

    @Override
    public boolean deleteGeometry(int id) throws GeometryDAO.InvalidIdException {
        if (!isLayerIdValid(id)) {
            throw new InvalidIdException();
        }
        
        String sql = "DELETE from uploaded where pid = ?";
        int rowsDeleted = _jdbcTemplate.update(sql, Integer.valueOf(id));
        
        if (rowsDeleted == 0) {
            return false;
        } else {
            return true;
        }
    }
    
    private boolean isLayerIdValid(int id) {
        String sql = "SELECT COUNT(pid) from uploaded where pid = ?";
        int count = _jdbcTemplate.queryForInt(sql, id);
        return count != 0;
    }
    
    private boolean isGeometryValid(String wkt) {
        String sql = "SELECT ST_IsValid(ST_GeomFromText(?, 4326));";
        return _jdbcTemplate.queryForObject(sql, Boolean.class, wkt);
    }

}
