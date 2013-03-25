package org.ala.layers.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

@Service("uploadDao")
public class UploadDAOImpl implements UploadDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(UploadDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    
    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }
    
    public int storeGeometryFromWKT(String wkt, String name, String description, String userid) {
        String sql = "INSERT INTO uploaded (pid, name, description, user_id, time_added, the_geom) values (DEFAULT, ?, ?, ?, now(), ST_GeomFromText(?, 4326))";
        jdbcTemplate.update(sql, name, description, userid, wkt);
        
        // get pid of uploaded layer
        String sql2 = "SELECT MAX(pid) from uploaded";
        int pid = jdbcTemplate.queryForInt(sql2);
        
        return pid;
    }
    
    @Override
    public int storeGeometryFromKML(String kml, String name, String description, String userid) {
        String sql = "INSERT INTO uploaded (pid, name, description, user_id, time_added, the_geom) values (DEFAULT, ?, ?, ?, now(), ST_GeomFromKML(?))";
        jdbcTemplate.update(sql, name, description, userid, kml);
        
        
        // get pid of uploaded layer
        String sql2 = "SELECT MAX(pid) from uploaded";
        int pid = jdbcTemplate.queryForInt(sql2);
        
        return pid;
    }

    public String getGeoJson(int pid) {
        String sql = "SELECT ST_AsGeoJSON(the_geom) from uploaded where pid = ?";
        return jdbcTemplate.queryForObject(sql, String.class, Integer.valueOf(pid));
    }
    
    @Override
    public String getKML(int pid) {
        String sql = "SELECT ST_AsKML(the_geom) from uploaded where pid = ?";
        return jdbcTemplate.queryForObject(sql, String.class, Integer.valueOf(pid));
    }

    @Override
    public String getWKT(int pid) {
        String sql = "SELECT ST_AsText(the_geom) from uploaded where pid = ?";
        return jdbcTemplate.queryForObject(sql, String.class, Integer.valueOf(pid));
    }
    
    public List<Integer> pointIntersect(double latitude, double longitude) {
        String sql = MessageFormat.format("SELECT pid from uploaded WHERE ST_Intersects(the_geom, ST_GeomFromText(''POINT({0} {1})'', 4326))", longitude, latitude);
        
        RowMapper<Integer> rowMapper = new RowMapper<Integer>() {

            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt(1);
            }
            
        };
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public boolean deleteGeometry(int pid) {
        String sql = "DELETE from uploaded where pid = ?";
        int rowsDeleted = jdbcTemplate.update(sql, String.class, Integer.valueOf(pid));
        
        if (rowsDeleted == 0) {
            return false;
        } else {
            return true;
        }
    }

}
