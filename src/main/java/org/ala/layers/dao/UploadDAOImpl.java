package org.ala.layers.dao;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

@Service("uploadDao")
public class UploadDAOImpl implements UploadDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(TabulationDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    
    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }
    
    public int uploadWKT(String wkt, String name, String description, String userid) {
        String sql = "INSERT INTO uploaded (pid, name, description, user_id, the_geom) values (DEFAULT, ?, ?, ?, ST_GeomFromText(?, 4326))";
        jdbcTemplate.update(sql, name, description, userid, wkt);
        
        // get pid of uploaded layers
        String sql2 = "SELECT MAX(pid) from uploaded";
        int pid = jdbcTemplate.queryForInt(sql2);
        
        return pid;
    }
    
    public String getGeoJson(int pid) {
        String sql = "SELECT ST_AsGeoJSON(the_geom) from uploaded where pid = ?";
        return jdbcTemplate.queryForObject(sql, String.class, Integer.valueOf(pid));
    }
}
