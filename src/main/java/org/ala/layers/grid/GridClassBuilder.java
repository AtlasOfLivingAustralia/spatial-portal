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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.ala.layers.dto.GridClass;
import org.ala.layers.intersect.Grid;
import org.ala.layers.tabulation.TabulationUtil;
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
 *
 * @author Adam
 */
public class GridClassBuilder {

    public static HashMap<Integer, GridClass> buildFromGrid(String filePath) throws IOException {
        File wktDir = new File(filePath);
        wktDir.mkdirs();

        int[] wktMap = null;

        HashMap<Integer, GridClass> classes = new HashMap<Integer, GridClass>();
        Properties p = new Properties();
        p.load(new FileReader(filePath + ".txt"));
        for (String key : p.stringPropertyNames()) {
            try {
                int k = Integer.parseInt(key);
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
                System.out.println("wkt written to file");
                gc.setArea_km(TabulationUtil.calculateArea((String) wktIndexed.get("wkt")) / 1000.0 / 1000.0);

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
                for (int i = 0; i < index.length; i++) {
                    if(index[i].length() > 1) {
                        String[] cells = index[i].split(",");
                        raf.writeInt(Integer.parseInt(cells[0]));   //polygon number
                        int polygonStart = Integer.parseInt(cells[1]);
                        raf.writeInt(polygonStart);   //character offset
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
                        raf.writeFloat((float) (TabulationUtil.calculateArea(polygonWkt) / 1000.0 / 1000.0));
                    }
                }
                raf.close();
                wktIndexed = null;

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
            } catch (NumberFormatException e) {
                System.out.println("Excluding shape key '" + key + "'");
            } catch (Exception e) {
                //logger.warn("Cannot parse integer key '" + key + "' in file " + filePath + ".txt");
                e.printStackTrace();
            }
        }

        //write polygon mapping
        Grid g = new Grid(filePath);
        g.writeGrid(filePath + File.separator + "polygons", wktMap, g.xmin, g.ymin, g.xmax, g.ymax, g.xres, g.yres, g.nrows, g.ncols);

        return classes;
    }
}
