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
package org.ala.layers.grid;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.WKTReader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.ala.layers.dto.GridClass;
import org.ala.layers.intersect.Grid;
import org.ala.layers.util.SpatialUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author Adam
 */
public class GridClassBuilder {

    final public static int[] colours = {0x003366CC, 0x00DC3912, 0x00FF9900, 0x00109618, 0x00990099, 0x000099C6, 0x00DD4477, 0x0066AA00, 0x00B82E2E, 0x00316395, 0x00994499, 0x0022AA99, 0x00AAAA11, 0x006633CC, 0x00E67300, 0x008B0707, 0x00651067, 0x00329262, 0x005574A6, 0x003B3EAC, 0x00B77322, 0x0016D620, 0x00B91383, 0x00F4359E, 0x009C5935, 0x00A9C413, 0x002A778D, 0x00668D1C, 0x00BEA413, 0x000C5922, 0x00743411};

    public static void main(String[] args) {
        //args = new String[]{"e:\\layers\\ready\\shape_diva\\fci"};

        System.out.println("args[0]=diva grid input file (do not include .grd or .gri)\n\n");

        if (args.length > 0) {
            //remove existing
            try {
                File f;

                if ((f = new File(args[0] + ".classes.json")).exists()) {
                    f.delete();
                }

                if ((f = new File(args[0])).exists() && f.isDirectory()) {
                    File[] fs = f.listFiles();
                    for (int i = 0; i < fs.length; i++) {
                        if (fs[i].isFile()) {
                            fs[i].delete();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            //build new
            try {
                buildFromGrid(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static HashMap<Integer, GridClass> buildFromGrid(String filePath) throws IOException {
        File wktDir = new File(filePath);
        wktDir.mkdirs();

        int[] wktMap = null;

        //track values for the SLD
        ArrayList<Integer> maxValues = new ArrayList<Integer>();
        ArrayList<String> labels = new ArrayList<String>();

        HashMap<Integer, GridClass> classes = new HashMap<Integer, GridClass>();
        Properties p = new Properties();
        p.load(new FileReader(filePath + ".txt"));

        ArrayList<Integer> keys = new ArrayList<Integer>();
        for (String key : p.stringPropertyNames()) {
            try {
                int k = Integer.parseInt(key);
                keys.add(k);
            } catch (NumberFormatException e) {
                System.out.println("Excluding shape key '" + key + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        java.util.Collections.sort(keys);
        for (int j = 0; j < keys.size(); j++) {
            int k = keys.get(j);
            String key = String.valueOf(k);
            try {
                String name = p.getProperty(key);

                GridClass gc = new GridClass();
                gc.setName(name);
                gc.setId(k);

                System.out.println("getting wkt for " + filePath + " > " + key);

                //write class wkt
                File zipFile = new File(filePath + File.separator + key + ".wkt.zip");
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                zos.putNextEntry(new ZipEntry(key + ".wkt"));
                Map wktIndexed = Envelope.getGridSingleLayerEnvelopeAsWktIndexed(filePath + "," + key + "," + key, wktMap);
                zos.write(((String) wktIndexed.get("wkt")).getBytes());
                zos.close();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath + File.separator + key + ".wkt"));
                bos.write(((String) wktIndexed.get("wkt")).getBytes());
                bos.close();
                System.out.println("wkt written to file");
                gc.setArea_km(SpatialUtil.calculateArea((String) wktIndexed.get("wkt")) / 1000.0 / 1000.0);

                //store map
                wktMap = (int[]) wktIndexed.get("map");

                //write wkt index
                FileWriter fw = new FileWriter(filePath + File.separator + key + ".wkt.index");
                fw.append((String) wktIndexed.get("index"));
                fw.close();
                //write wkt index a binary, include extents (minx, miny, maxx, maxy) and area (sq km)
                RandomAccessFile raf = new RandomAccessFile(filePath + File.separator + key + ".wkt.index.dat", "rw");
                String[] index = ((String) wktIndexed.get("index")).split("\n");
                int len = ((String) wktIndexed.get("wkt")).length();
                WKTReader r = new WKTReader();

                int minPolygonNumber = 0;
                int maxPolygonNumber = 0;

                for (int i = 0; i < index.length; i++) {
                    if (index[i].length() > 1) {
                        String[] cells = index[i].split(",");
                        int polygonNumber = Integer.parseInt(cells[0]);
                        raf.writeInt(polygonNumber);   //polygon number
                        int polygonStart = Integer.parseInt(cells[1]);
                        raf.writeInt(polygonStart);   //character offset

                        if (i == 0) {
                            minPolygonNumber = polygonNumber;
                        } else if (i == index.length - 1) {
                            maxPolygonNumber = polygonNumber;
                        }

                        int polygonEnd = len;
                        if (i + 1 < index.length) {
                            polygonEnd = Integer.parseInt(index[i + 1].split(",")[1]) - 1; //-1 for comma
                        }
                        String polygonWkt = ((String) wktIndexed.get("wkt")).substring(polygonStart, polygonEnd);
                        Geometry g = r.read("POLYGON" + polygonWkt);
                        raf.writeFloat((float) g.getEnvelopeInternal().getMinX());
                        raf.writeFloat((float) g.getEnvelopeInternal().getMinY());
                        raf.writeFloat((float) g.getEnvelopeInternal().getMaxX());
                        raf.writeFloat((float) g.getEnvelopeInternal().getMaxY());
                        raf.writeFloat((float) (SpatialUtil.calculateArea(polygonWkt) / 1000.0 / 1000.0));
                    }
                }
                raf.close();
                wktIndexed = null;

                //for SLD
                maxValues.add(gc.getMaxShapeIdx());
                labels.add(name.replace("\"", "'"));
                gc.setMinShapeIdx(minPolygonNumber);
                gc.setMaxShapeIdx(maxPolygonNumber);

                System.out.println("getting multipolygon for " + filePath + " > " + key);
                MultiPolygon mp = Envelope.getGridEnvelopeAsMultiPolygon(filePath + "," + key + "," + key);
                gc.setBbox(mp.getEnvelope().toText().replace(" (", "(").replace(", ", ","));

                classes.put(k, gc);

                try {
//                    if(zipFile.length() < 1024*1024/5) {
                    //write class kml
                    zos = new ZipOutputStream(new FileOutputStream(filePath + File.separator + key + ".kml.zip"));
                    zos.putNextEntry(new ZipEntry(key + ".kml"));
                    Encoder encoder = new Encoder(new KMLConfiguration());
                    encoder.setIndenting(true);
                    encoder.encode(mp, KML.Geometry, zos);
                    zos.close();
                    System.out.println("kml written to file");

                    //write class geojson
                    zos = new ZipOutputStream(new FileOutputStream(filePath + File.separator + key + ".geojson.zip"));
                    zos.putNextEntry(new ZipEntry(key + ".geojson"));
                    FeatureJSON fjson = new FeatureJSON();
                    final SimpleFeatureType TYPE = DataUtilities.createType("class", "the_geom:MultiPolygon,id:Integer,name:String");
                    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                    featureBuilder.add(mp);
                    featureBuilder.add(k);
                    featureBuilder.add(name);
                    SimpleFeature sf = featureBuilder.buildFeature(null);
                    fjson.writeFeature(sf, zos);
                    zos.close();
                    System.out.println("geojson written to file");

                    //write class shape file
                    File newFile = new File(filePath + File.separator + key + ".shp");
                    ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
                    Map<String, Serializable> params = new HashMap<String, Serializable>();
                    params.put("url", newFile.toURI().toURL());
                    params.put("create spatial index", Boolean.FALSE);
                    ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
                    newDataStore.createSchema(TYPE);
                    newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
                    Transaction transaction = new DefaultTransaction("create");
                    String typeName = newDataStore.getTypeNames()[0];
                    SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
                    SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                    featureStore.setTransaction(transaction);
                    SimpleFeatureCollection collection = FeatureCollections.newCollection();
                    collection.add(sf);
                    featureStore.addFeatures(collection);
                    transaction.commit();
                    transaction.close();

                    zos = new ZipOutputStream(new FileOutputStream(filePath + File.separator + key + ".shp.zip"));
                    //add .dbf .shp .shx .prj
                    String[] exts = {".dbf", ".shp", ".shx", ".prj"};
                    for (String ext : exts) {
                        zos.putNextEntry(new ZipEntry(key + ext));
                        FileInputStream fis = new FileInputStream(filePath + File.separator + key + ext);
                        byte[] buffer = new byte[1024];
                        int size;
                        while ((size = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, size);
                        }
                        fis.close();
                        //remove unzipped files
                        new File(filePath + File.separator + key + ext).delete();
                    }
                    zos.close();
                    System.out.println("shape file written to zip");
//                    } else {
//                        System.out.println("polygon too large");
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                //logger.warn("Cannot parse integer key '" + key + "' in file " + filePath + ".txt");
                e.printStackTrace();
            }
        }

        //write polygon mapping
        Grid g = new Grid(filePath);
        g.writeGrid(filePath + File.separator + "polygons", wktMap, g.xmin, g.ymin, g.xmax, g.ymax, g.xres, g.yres, g.nrows, g.ncols);

        //copy the header file to get it exactly the same, but change the data type
        copyHeaderAsInt(filePath + ".grd", filePath + File.separator + "polygons.grd");

        //write sld
        exportSLD(filePath + File.separator + "polygons.sld", new File(filePath + ".txt").getName(), maxValues, labels);

        writeProjectionFile(filePath + File.separator + "polygons.prj");

        //write .classes.json
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(filePath + ".classes.json"), classes);

        return classes;
    }

    static void exportSLD(String filename, String name, ArrayList<Integer> maxValues, ArrayList<String> labels) {
        try {
            StringBuffer sld = new StringBuffer();
            /* header */
            sld.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sld.append("<sld:StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.0.0\">");
            sld.append("<sld:NamedLayer>");
            sld.append("<sld:Name>raster</sld:Name>");
            sld.append(" <sld:UserStyle>");
            sld.append("<sld:Name>raster</sld:Name>");
            sld.append("<sld:Title>A very simple color map</sld:Title>");
            sld.append("<sld:Abstract>A very basic color map</sld:Abstract>");
            sld.append("<sld:FeatureTypeStyle>");
            sld.append(" <sld:Name>name</sld:Name>");
            sld.append("<sld:FeatureTypeName>Feature</sld:FeatureTypeName>");
            sld.append(" <sld:Rule>");
            sld.append("   <sld:RasterSymbolizer>");
            sld.append(" <sld:Geometry>");
            sld.append(" <ogc:PropertyName>geom</ogc:PropertyName>");
            sld.append(" </sld:Geometry>");
            sld.append(" <sld:ChannelSelection>");
            sld.append(" <sld:GrayChannel>");
            sld.append("   <sld:SourceChannelName>1</sld:SourceChannelName>");
            sld.append(" </sld:GrayChannel>");
            sld.append(" </sld:ChannelSelection>");
            sld.append(" <sld:ColorMap type=\"intervals\">");

            /* outputs */
            sld.append("\n<sld:ColorMapEntry color=\"#ffffff\" opacity=\"0\" quantity=\"1\"/>\n");
            for (int i = 0; i < labels.size(); i++) {
                sld.append("<sld:ColorMapEntry color=\"#" + getHexColour(colours[i % colours.length]) + "\" quantity=\"" + (maxValues.get(i) + 1) + ".0\" label=\"" + labels.get(i) + "\" opacity=\"1\"/>\r\n");
            }

            /* footer */
            sld.append("</sld:ColorMap></sld:RasterSymbolizer></sld:Rule></sld:FeatureTypeStyle></sld:UserStyle></sld:NamedLayer></sld:StyledLayerDescriptor>");

            /* write */
            FileWriter fw = new FileWriter(filename);
            fw.append(sld.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getHexColour(int colour) {
        String s = Integer.toHexString(colour);
        while (s.length() > 6) {
            s = s.substring(1);
        }
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    private static void writeProjectionFile(String filename) {
        try {
            FileWriter spWriter = new FileWriter(filename);

            StringBuffer sbProjection = new StringBuffer();
            sbProjection.append("GEOGCS[\"WGS 84\", ").append("\n");
            sbProjection.append("    DATUM[\"WGS_1984\", ").append("\n");
            sbProjection.append("        SPHEROID[\"WGS 84\",6378137,298.257223563, ").append("\n");
            sbProjection.append("            AUTHORITY[\"EPSG\",\"7030\"]], ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"6326\"]], ").append("\n");
            sbProjection.append("    PRIMEM[\"Greenwich\",0, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"8901\"]], ").append("\n");
            sbProjection.append("    UNIT[\"degree\",0.01745329251994328, ").append("\n");
            sbProjection.append("        AUTHORITY[\"EPSG\",\"9122\"]], ").append("\n");
            sbProjection.append("    AUTHORITY[\"EPSG\",\"4326\"]] ").append("\n");

            //spWriter.write("spname, longitude, latitude \n");
            spWriter.append(sbProjection.toString());
            spWriter.close();

        } catch (IOException ex) {
            //Logger.getLogger(MaxentServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("error writing species file:");
            ex.printStackTrace(System.out);
        }
    }

    private static void copyHeaderAsInt(String src, String dst) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(src));
            FileWriter fw = new FileWriter(dst);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("DataType=")) {
                    fw.write("DataType=INT\n");
                } else {
                    fw.write(line);
                    fw.write("\n");
                }
            }
            fw.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
