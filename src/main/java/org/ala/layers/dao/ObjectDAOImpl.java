/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.dao;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.ala.layers.dto.GridClass;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Objects;
import org.ala.layers.intersect.Grid;
import org.ala.layers.tabulation.TabulationUtil;
import org.apache.log4j.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.jdbc.core.simple.ParameterizedBeanPropertyRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author ajay
 */
@Service("objectDao")
public class ObjectDAOImpl implements ObjectDAO {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(ObjectDAOImpl.class);
    private SimpleJdbcTemplate jdbcTemplate;
    @Resource(name = "layerIntersectDao")
    private LayerIntersectDAO layerIntersectDao;

    @Resource(name = "dataSource")
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @Override
    public List<Objects> getObjects() {
        //return hibernateTemplate.find("from Objects");
        logger.info("Getting a list of all objects");
        String sql = "select o.pid as pid, o.id as id, o.name as name, o.desc as description, o.fid as fid, f.name as fieldname from objects o, fields f where o.fid = f.id";
        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class));
    }

    @Override
    public List<Objects> getObjectsById(String id) {
        //return hibernateTemplate.find("from Objects where id = ?", id);
        logger.info("Getting object info for fid = " + id);
        //String sql = "select * from objects where fid = ?";
        String sql = "select o.pid as pid, o.id as id, o.name as name, o.desc as description, o.fid as fid, f.name as fieldname, o.bbox, o.area_km from objects o, fields f where o.fid = ? and o.fid = f.id";
        List<Objects> objects = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), id);

        //get grid classes
        if (objects == null || objects.isEmpty()) {
            objects = new ArrayList<Objects>();
            IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(id);
            if (f != null && f.getClasses() != null) {
                for (Entry<Integer, GridClass> c : f.getClasses().entrySet()) {
                    if (f.getType().equals("a")) {           //class pid
                        Objects o = new Objects();
                        o.setPid(f.getLayerPid() + ":" + c.getKey());
                        o.setId(f.getLayerPid() + ":" + c.getKey());
                        o.setName(c.getValue().getName());
                        o.setFid(f.getFieldId());
                        o.setFieldname(f.getFieldName());
                        o.setBbox(c.getValue().getBbox());
                        o.setArea_km(c.getValue().getArea_km());
                        objects.add(o);
                    } else {                                //polygon pid
                        try {
//                            BufferedReader br = new BufferedReader(new FileReader(f.getFilePath() + File.separator + c.getKey() + ".wkt.index"));
//                            String line;
//                            while((line = br.readLine()) != null) {
//                                if(line.length() > 0) {
//                                    String [] cells = line.split(",");
//                                    Objects o = new Objects();
//                                    o.setPid(f.getLayerPid() + ":" + c.getKey() + ":" + cells[0]);
//                                    o.setId(f.getLayerPid() + ":" + c.getKey() + ":" + cells[0]);
//                                    o.setName(c.getValue().getName());
//                                    o.setFid(f.getFieldId());
//                                    o.setFieldname(f.getFieldName());
//
//                                    //Too costly to calculate on the fly, and not pre-calculated.
////                                    o.setBbox(c.getValue().getBbox());
////                                    o.setArea_km(c.getValue().getArea_km());
//                                    objects.add(o);
//                                }
//                            }
//                            br.close();
                            RandomAccessFile raf = new RandomAccessFile(f.getFilePath() + File.separator + c.getKey() + ".wkt.index.dat", "r");
                            long len = raf.length() / (4 + 4 + 4 * 4 + 4); //group number, character offset, minx, miny, maxx, maxy, area sq km
                            for (int i = 0; i < len; i++) {
                                int n = raf.readInt();
                                /*int charoffset = */ raf.readInt();
                                float minx = raf.readFloat();
                                float miny = raf.readFloat();
                                float maxx = raf.readFloat();
                                float maxy = raf.readFloat();
                                float area = raf.readFloat();

                                Objects o = new Objects();
                                o.setPid(f.getLayerPid() + ":" + c.getKey() + ":" + n);
                                o.setId(f.getLayerPid() + ":" + c.getKey() + ":" + n);
                                o.setName(c.getValue().getName());
                                o.setFid(f.getFieldId());
                                o.setFieldname(f.getFieldName());

                                o.setBbox("POLYGON((" + minx + " " + miny + ","
                                        + minx + " " + maxy + ","
                                        + +maxx + " " + maxy + ","
                                        + +maxx + " " + miny + ","
                                        + +minx + " " + miny + "))");
                                o.setArea_km(1.0 * area);
                                objects.add(o);
                            }
                            raf.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return objects;
    }

    @Override
    public String getObjectsGeometryById(String id, String geomtype) {
        logger.info("Getting object info for id = " + id + " and geometry as " + geomtype);
        String sql = "";
        if ("kml".equals(geomtype)) {
            sql = "SELECT ST_AsKml(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("wkt".equals(geomtype)) {
            sql = "SELECT ST_AsText(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("geojson".equals(geomtype)) {
            sql = "SELECT ST_AsGeoJSON(the_geom) as geometry FROM objects WHERE pid=?;";
        }

        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), id);

        String geometry = null;
        if (l.size() > 0) {
            geometry = l.get(0).getGeometry();
        }

        //get grid classes
        if (geometry == null && id.length() > 0) {
            //grid class pids are, 'layerPid:gridClassNumber'
            try {
                String[] s = id.split(":");
                if (s.length >= 2) {
                    int n = Integer.parseInt(s[1]);
                    IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(s[0]);
                    if (f != null && f.getClasses() != null) {
                        GridClass gc = f.getClasses().get(n);
                        if (gc != null
                                && ("kml".equals(geomtype)
                                || "wkt".equals(geomtype)
                                || "geojson".equals(geomtype))) {
                            if (f.getType().equals("a") || s.length == 2) {           //class
                                File file = new File(f.getFilePath() + File.separator + s[1] + "." + geomtype + ".zip");
                                if (file.exists()) {
                                    ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
                                    zis.getNextEntry();
                                    BufferedReader br = new BufferedReader(new InputStreamReader(zis));
                                    String line;
                                    StringBuilder sb = new StringBuilder();
                                    while ((line = br.readLine()) != null) {
                                        if (sb.length() > 0) {
                                            sb.append("\n");
                                        }
                                        sb.append(line);
                                    }
                                    br.close();
                                    zis.close();
                                    geometry = sb.toString();
                                }
                            } else {                                //polygon
                                BufferedReader br = null;
                                RandomAccessFile raf = null;
                                try {
                                    String[] cells = null;

//                                    br = new BufferedReader(new FileReader(f.getFilePath() + File.separator + s[1] + ".wkt.index"));
//                                    String line;
//                                    while((line = br.readLine()) != null) {
//                                        if(line.length() > 0) {
//                                            cells = line.split(",");
//                                            if(cells[0].equals(s[2])) {
//                                                break;
//                                            }
//                                        }
//                                    }
//                                    br.close();

                                    raf = new RandomAccessFile(f.getFilePath() + File.separator + s[1] + ".wkt.index.dat", "r");
                                    long len = raf.length() / (4 + 4 + 4 * 4 + 4); //group number, character offset, minx, miny, maxx, maxy, area sq km
                                    int s2 = Integer.parseInt(s[2]);
                                    for (int i = 0; i < len; i++) {
                                        int gn = raf.readInt();
                                        int charoffset = raf.readInt();
                                        /*float minx =*/ raf.readFloat();
                                        /*float miny =*/ raf.readFloat();
                                        /*float maxx =*/ raf.readFloat();
                                        /*float maxy =*/ raf.readFloat();
                                        /*float area =*/ raf.readFloat();
                                        if (gn == s2) {
                                            cells = new String[]{String.valueOf(gn), String.valueOf(charoffset)};
                                        }
                                    }
                                    raf.close();

                                    if (cells != null) {
                                        //get polygon wkt string
                                        File file = new File(f.getFilePath() + File.separator + s[1] + ".wkt.zip");
                                        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
                                        zis.getNextEntry();
                                        InputStreamReader isr = new InputStreamReader(zis);
                                        isr.skip(Long.parseLong(cells[1]));
                                        char[] buffer = new char[1024];
                                        int size;
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("POLYGON");
                                        int end = -1;
                                        while (end < 0 && (size = isr.read(buffer)) > 0) {
                                            sb.append(buffer, 0, size);
                                            end = sb.toString().indexOf("))");
                                        }
                                        end += 2;

                                        String wkt = sb.toString().substring(0, end);

                                        if (geomtype.equals("wkt")) {
                                            geometry = wkt;
                                        } else {
                                            WKTReader r = new WKTReader();
                                            Geometry g = r.read(wkt);

                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            if (geomtype.equals("kml")) {
                                                Encoder encoder = new Encoder(new KMLConfiguration());
                                                encoder.setIndenting(true);
                                                encoder.encode(geometry, KML.Geometry, baos);
                                            } else if (geomtype.equals("geojson")) {
                                                FeatureJSON fjson = new FeatureJSON();
                                                final SimpleFeatureType TYPE = DataUtilities.createType("class", "the_geom:MultiPolygon,name:String");
                                                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                                                featureBuilder.add(geometry);
                                                featureBuilder.add(gc.getName());
                                                fjson.writeFeature(featureBuilder.buildFeature(null), baos);
                                            }
                                            return new String(baos.toByteArray());
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    if (br != null) {
                                        br.close();
                                    }
                                    if (raf != null) {
                                        raf.close();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return geometry;
    }

    @Override
    public void streamObjectsGeometryById(OutputStream os, String id, String geomtype) throws IOException {
        logger.info("Getting object info for id = " + id + " and geometry as " + geomtype);
        String sql = "";
        if ("kml".equals(geomtype)) {
            sql = "SELECT ST_AsKml(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("wkt".equals(geomtype)) {
            sql = "SELECT ST_AsText(the_geom) as geometry FROM objects WHERE pid=?;";
        } else if ("geojson".equals(geomtype)) {
            sql = "SELECT ST_AsGeoJSON(the_geom) as geometry FROM objects WHERE pid=?;";
        }

        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), id);
        
        if (l.size() > 0) {
            os.write(l.get(0).getGeometry().getBytes());
        } else {
            //get grid classes
            if (id.length() > 0) {
                //grid class pids are, 'layerPid:gridClassNumber'
                try {
                    String[] s = id.split(":");
                    if (s.length >= 2) {
                        int n = Integer.parseInt(s[1]);
                        IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(s[0]);
                        if (f != null && f.getClasses() != null) {
                            GridClass gc = f.getClasses().get(n);
                            if (gc != null
                                    && ("kml".equals(geomtype)
                                    || "wkt".equals(geomtype)
                                    || "geojson".equals(geomtype))) {
                                if (f.getType().equals("a") || s.length == 2) {           //class
                                    File file = new File(f.getFilePath() + File.separator + s[1] + "." + geomtype + ".zip");
                                    if (file.exists()) {
                                        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
                                        zis.getNextEntry();
                                        byte[] buffer = new byte[1024];
                                        int size;
                                        while ((size = zis.read(buffer)) > 0) {
                                            os.write(buffer, 0, size);
                                        }
                                        zis.close();
                                    }
                                } else {                                //polygon
                                    BufferedReader br = null;
                                    RandomAccessFile raf = null;
                                    try {
                                        String[] cells = null;

//                                    br = new BufferedReader(new FileReader(f.getFilePath() + File.separator + s[1] + ".wkt.index"));
//                                    String line;
//                                    while((line = br.readLine()) != null) {
//                                        if(line.length() > 0) {
//                                            cells = line.split(",");
//                                            if(cells[0].equals(s[2])) {
//                                                break;
//                                            }
//                                        }
//                                    }

                                        raf = new RandomAccessFile(f.getFilePath() + File.separator + s[1] + ".wkt.index.dat", "r");
                                        long len = raf.length() / (4 + 4 + 4 * 4 + 4); //group number, character offset, minx, miny, maxx, maxy, area sq km
                                        int s2 = Integer.parseInt(s[2]);
                                        for (int i = 0; i < len; i++) {
                                            int gn = raf.readInt();
                                            int charoffset = raf.readInt();
                                            /*float minx =*/ raf.readFloat();
                                            /*float miny =*/ raf.readFloat();
                                            /*float maxx =*/ raf.readFloat();
                                            /*float maxy =*/ raf.readFloat();
                                            /*float area =*/ raf.readFloat();
                                            if (gn == s2) {
                                                cells = new String[]{String.valueOf(gn), String.valueOf(charoffset)};
                                            }
                                        }
                                        raf.close();

                                        if (cells != null) {
                                            //get polygon wkt string
                                            File file = new File(f.getFilePath() + File.separator + s[1] + ".wkt.zip");
                                            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
                                            zis.getNextEntry();
                                            InputStreamReader isr = new InputStreamReader(zis);
                                            isr.skip(Long.parseLong(cells[1]));
                                            char[] buffer = new char[1024];
                                            int size;
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("POLYGON");
                                            int end = -1;
                                            while (end < 0 && (size = isr.read(buffer)) > 0) {
                                                sb.append(buffer, 0, size);
                                                end = sb.toString().indexOf("))");
                                            }
                                            end += 2;

                                            String wkt = sb.toString().substring(0, end);

                                            if (geomtype.equals("wkt")) {
                                                os.write(wkt.getBytes());
                                            } else {
                                                WKTReader r = new WKTReader();
                                                Geometry g = r.read(wkt);

                                                if (geomtype.equals("kml")) {
                                                    Encoder encoder = new Encoder(new KMLConfiguration());
                                                    encoder.setIndenting(true);
                                                    encoder.encode(g, KML.Geometry, os);
                                                } else if (geomtype.equals("geojson")) {
                                                    FeatureJSON fjson = new FeatureJSON();
                                                    final SimpleFeatureType TYPE = DataUtilities.createType("class", "the_geom:MultiPolygon,name:String");
                                                    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                                                    featureBuilder.add(g);
                                                    featureBuilder.add(gc.getName());
                                                    fjson.writeFeature(featureBuilder.buildFeature(null), os);
                                                }
                                            }
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        if (br != null) {
                                            br.close();
                                        }
                                        if (raf != null) {
                                            raf.close();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Objects getObjectByPid(String pid) {
        //List<Objects> l = hibernateTemplate.find("from Objects where pid = ?", pid);
        logger.info("Getting object info for pid = " + pid);
        String sql = "select o.pid, o.id, o.name, o.desc as description, o.fid as fid, f.name as fieldname, o.bbox, o.area_km from objects o, fields f where o.pid = ? and o.fid = f.id";
        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), pid);

        //get grid classes
        if ((l == null || l.isEmpty()) && pid.length() > 0) {
            //grid class pids are, 'layerPid:gridClassNumber'
            try {
                String[] s = pid.split(":");
                if (s.length >= 2) {
                    int n = Integer.parseInt(s[1]);
                    IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(s[0]);
                    if (f != null && f.getClasses() != null) {
                        GridClass gc = f.getClasses().get(n);
                        if (gc != null) {
                            Objects o = new Objects();
                            o.setPid(pid);
                            o.setId(pid);
                            o.setName(gc.getName());
                            o.setFid(f.getFieldId());
                            o.setFieldname(f.getFieldName());

                            if (f.getType().equals("a") || s.length == 2) {
                                o.setBbox(gc.getBbox());
                                o.setArea_km(gc.getArea_km());
                            } else {
                                String wkt = getObjectsGeometryById(pid, "wkt");
                                if (wkt != null) {
                                    WKTReader r = new WKTReader();
                                    o.setBbox(r.read(wkt).getEnvelope().toText().replace(" (", "(").replace(", ", ","));

                                    o.setArea_km(TabulationUtil.calculateArea(wkt) / 1000.0 / 1000.0);
                                }
                            }

                            l.add(o);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Objects getObjectByIdAndLocation(String fid, Double lng, Double lat) {
        logger.info("Getting object info for fid = " + fid + " at loc: (" + lng + ", " + lat + ") ");
        String sql = "select o.pid, o.id, o.name, o.desc as description, o.fid as fid, f.name as fieldname, o.bbox, o.area_km from objects o, fields f where o.fid = ? and ST_Within(ST_SETSRID(ST_Point(?,?),4326), o.the_geom) and o.fid = f.id";
        List<Objects> l = jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), new Object[]{fid, lng, lat});
        if (l == null || l.isEmpty()) {
            //get grid classes intersection
            l = new ArrayList<Objects>();
            IntersectionFile f = layerIntersectDao.getConfig().getIntersectionFile(fid);
            if (f != null && f.getClasses() != null) {
                Vector v = layerIntersectDao.samplingFull(fid, lng, lat);
                if (v != null && v.size() > 0 && v.get(0) != null) {
                    Map m = (Map) v.get(0);
                    int key = (int) Double.parseDouble(((String) m.get("pid")).split(":")[1]);
                    GridClass gc = f.getClasses().get(key);
                    if (f.getType().equals("a")) {           //class pid
                        Objects o = new Objects();
                        o.setName(gc.getName());
                        o.setFid(f.getFieldId());
                        o.setFieldname(f.getFieldName());
                        o.setPid(f.getLayerPid() + ":" + gc.getId());
                        o.setId(f.getLayerPid() + ":" + gc.getId());
                        o.setBbox(gc.getBbox());
                        o.setArea_km(gc.getArea_km());
                        l.add(o);
                    } else { // if(f.getType().equals("b")) {//polygon pid
                        Grid g = new Grid(f.getFilePath() + File.separator + "polygons");
                        if (g != null) {
                            float[] vs = g.getValues(new double[][]{{lng, lat}});
                            String pid = f.getLayerPid() + ":" + gc.getId() + ":" + ((int) vs[0]);
                            l.add(getObjectByPid(pid));
                        }
                    }
                }
            }
        }
        if (l.size() > 0) {
            return l.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Objects> getNearestObjectByIdAndLocation(String fid, int limit, Double lng, Double lat) {
        logger.info("Getting " + limit + " nearest objects in field fid = " + fid + " to loc: (" + lng + ", " + lat + ") ");

        String sql = "select fid, name, \"desc\", pid, id, ST_AsText(the_geom) as geometry, "
                + "st_Distance_Sphere(ST_SETSRID(ST_Point( ? , ? ),4326), the_geom) as distance, "
                + "degrees(Azimuth( ST_SETSRID(ST_Point( ? , ? ),4326), the_geom)) as degrees "
                + "from objects where fid= ? order by distance limit ? ";

        return jdbcTemplate.query(sql, ParameterizedBeanPropertyRowMapper.newInstance(Objects.class), lng, lat, lng, lat, fid, new Integer(limit));
    }
}
