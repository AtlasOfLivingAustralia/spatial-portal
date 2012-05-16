/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.ala.spatial.util.UserData;
import org.ala.spatial.util.Zipper;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.helpers.NamespaceSupport;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Imagemap;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;

/**
 *
 * @author ajay
 */
public class AreaUploadShapefileWizardController extends UtilityComposer {

    Imagemap img;
    Media media;
    Listbox lAttributes;
    Button btnRefresh;
    Button btnNext;
    SimpleFeatureSource source;
    SimpleFeatureCollection features;
    String file;
    String layername;

    @Override
    public void afterCompose() {
        super.afterCompose();

        Map args = Executions.getCurrent().getArg();

        layername = (String) args.get("layername");

        media = (Media) args.get("media");
        System.out.println("Got media in wizard");
        System.out.println("m.getName(): " + media.getName());
        System.out.println("getContentType: " + media.getContentType());
        System.out.println("getFormat: " + media.getFormat());


        System.out.println("Layer name: " + layername);

        processMedia();

        img.addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                //imageClicked(event);
            }
        });


//
//        System.out.println("\n\n\nStart. ShapefileRenderer.generateShapeImage\n\n\n");
//        System.out.println(ShapefileRenderer.generateShapeImage("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp", "/Users/ajay/Downloads/MEOW_zip/", "ECOREGION", "Baltic Sea"));
//        System.out.println("\n\n\nEnd  . ShapefileRenderer.generateShapeImage\n\n\n");
//


//        try {
//            File imgFile = new File("/data/ala/runtime/output/layers/meow_ecos_none.jpg");
//            if (imgFile.exists()) {
//                BufferedImage bi;
//                bi = ImageIO.read(imgFile);
//                img.setContent(bi);
//
//
//                loadShape("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp");
//            }
//        } catch (IOException ex) {
//            System.out.println("IO Exception loading the test meow image");
//            ex.printStackTrace(System.out);
//        } catch (Exception ex) {
//            System.out.println("Exception loading the test meow image");
//            ex.printStackTrace(System.out);
//        }

    }

    private void processMedia() {
        Map input = Zipper.unzipFile(media.getName(), media.getStreamData(), getMapComposer().getSettingsSupplementary().getValue("analysis_output_dir") + "layers/");
        String type = "";
        //String file = "";
        if (input.containsKey("type")) {
            type = (String) input.get("type");
        }
        if (input.containsKey("file")) {
            file = (String) input.get("file");
        }
        if (type.equalsIgnoreCase("shp")) {
            loadShape(file);
        } else {
            System.out.println("Unknown file type. ");
            getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, \nKML or Shapefile. ");
        }
    }
    
    private void executeShapeImageRenderer(String shapepath, String column, String filters) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            System.out.println("Generating image via Runtime");
            //String outputdirbase = getMapComposer().getSettingsSupplementary().getValue("analysis_output_dir");

            //String shapepath = "/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp";
            //String shapefilter = "none";
            //String shapeout = "/Users/ajay/Downloads/MEOW_zip/";
            String shapeimageexe = getMapComposer().getSettingsSupplementary().getValue("shapeimagepath");
            //String shapeout = outputdirbase + "/layers/";
            String shapeout = shapepath.substring(0, shapepath.lastIndexOf("/") + 1);

            if (StringUtils.isBlank(column)) {
                column = "none";
            }
            if (StringUtils.isBlank(filters)) {
                filters = "none";
            }

            ////shapeimageexe = "cd /usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/classes && java -classpath .:../lib/* org.ala.spatial.util.ShapefileRenderer ";
            //shapeimageexe = "java -classpath /usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/classes:/usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/lib/* org.ala.spatial.util.ShapefileRenderer ";

            //String command = shapeimageexe + " \"" + shapepath + "\" \"" + shapeout + "\" \"" + column + "\" \"" + filters + "\"";
            String command = shapeimageexe + " " + shapepath + " " + shapeout + " " + column + " " + filters + "";
            System.out.println("Running " + command);

            proc = runtime.exec(command);

            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            String imagepath = "";

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                imagepath = line;
            }

            int exitVal = proc.waitFor();

            if (exitVal != -1) {
                System.out.println("Image generated at: " + imagepath);

                File imgFile = new File(imagepath);
                if (imgFile.exists()) {
                    BufferedImage bi = ImageIO.read(imgFile);
                    img.setContent(bi);
                }
            }


        } catch (Exception e) {
            System.out.println("Unable to generate image for selected shapefile");
            e.printStackTrace(System.out);
        }
    }

    private void loadShape(String filename) {

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        String userFilter = "none";
        try {
            DataStore dataStore = FileDataStoreFinder.getDataStore(new File(filename));
            String[] typenames = dataStore.getTypeNames();
            source = dataStore.getFeatureSource(typenames[0]);
            features = source.getFeatures();

            System.out.println("features.getID(): " + features.getID());

            Listhead lhd = new Listhead();
            SimpleFeatureType schema = features.getSchema();
            for (AttributeType at : schema.getTypes()) {
                if (schema.getDescriptor(at.getName()) == null) {
                    continue;
                }
                Listheader lh = new Listheader(at.getName().toString());
                lh.setParent(lhd);
            }
            lhd.setParent(lAttributes);
            
            SimpleFeatureIterator fi = features.features();
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                Listitem li = new Listitem();
                Listcell lc = null;
                String value = "";

                for (AttributeType at : schema.getTypes()) {
                    if (schema.getDescriptor(at.getName()) == null) {
                        continue;
                    }
                    Object obj = f.getAttribute(at.getName());
                    if (obj == null) {
                        value = f.getID();
                    } else {
                        value = String.valueOf(obj);
                    }
                    lc = new Listcell(value);

                    lc.setParent(li);
                }
                li.setValue(f.getIdentifier());
                //li.setValue(value);
                li.setParent(lAttributes);
            }


            // loadFeatures
            // check if only a single feature,
            // if so, then select it and map it automatically
            System.out.println("features.size(): " + features.size());
            if (features.size() > 1) {
                executeShapeImageRenderer(file, "none", "none");
            } else {
                System.out.println("only a single feature, bypassing wizard...");
                fi = features.features();
                //ArrayList<String> tmpList = new ArrayList<String>();
                //tmpList.add(String.valueOf(fi.next().getAttribute(cbAttributes.getValue())));
                //loadOnMap(tmpList);

                Set<FeatureId> IDs = new HashSet<FeatureId>();
                IDs.add(fi.next().getIdentifier()); 
                loadOnMap(IDs);

                this.detach();
            }

        } catch (IOException e) {
            System.out.println("IO Exception ");
        } catch (Exception e) {
            System.out.println("Generic exception");
            e.printStackTrace(System.out);
        }
    }

    public void onClick$btnRefresh(Event event) {
        String column = "fid";
        String filter = "";

        Iterator<Listitem> it = lAttributes.getSelectedItems().iterator();
        while (it.hasNext()) {
            Listitem li = it.next();
            filter += li.getValue();

            if (it.hasNext()) {
                filter += ",";
            }
        }

        executeShapeImageRenderer(file, column, filter);
    }

    public void onClick$btnNext(Event event) {
        try {
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Iterator<Listitem> it = lAttributes.getSelectedItems().iterator();
            Set<FeatureId> IDs = new HashSet<FeatureId>();
            while (it.hasNext()) {
                Listitem li = it.next();
                IDs.add((FeatureId) li.getValue());
            }
//            Filter filter = ff.id(IDs);
//            SimpleFeatureCollection sff = source.getFeatures(filter);
//            SimpleFeatureIterator fif = sff.features();
//            while (fif.hasNext()) {
//                SimpleFeature f = fif.next();
//                System.out.println("Selected Feature: " + f.getID() + " -> " + f.getAttribute("ECOREGION"));
//            }
            loadOnMap(IDs);
            this.detach();
        } catch (Exception ex) {
            System.out.println("Error iterating thru' features");
            ex.printStackTrace(System.out);
        }
    }

    private void loadOnMap(Set<FeatureId> IDs) {
        try {
            String column = "fid"; 
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            Filter filter = ff.id(IDs);
            
            // set up the math transform used to process the data
            SimpleFeatureType schema = features.getSchema();
            CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
            CoordinateReferenceSystem wgsCRS = DefaultGeographicCRS.WGS84;
            boolean lenient = true; // allow for some error due to different datums
            MathTransform transform = CRS.findMathTransform(dataCRS, wgsCRS, lenient);

            
            SimpleFeatureCollection sff = source.getFeatures(filter);
            SimpleFeatureIterator fif = sff.features();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            ArrayList<Geometry> geoms = new ArrayList<Geometry>();
            while (fif.hasNext()) {
                SimpleFeature f = fif.next();

                System.out.println("Selected Feature: " + f.getID() + " -> " + f.getAttribute("ECOREGION"));


                //geoms.add((Geometry) f.getDefaultGeometry());


                Geometry geom = (Geometry) f.getDefaultGeometry();
                geom = JTS.transform(geom, transform);
                String wktString = geom.toText();
                wktString = wktString.replaceAll(", ", ",");
                boolean valid = true;
                boolean multipolygon = false;
                boolean polygon = false;
                boolean geometrycollection = false;
                if (wktString.startsWith("MULTIPOLYGON ")) {
                    wktString = wktString.substring("MULTIPOLYGON (".length(), wktString.length() - 1);
                    multipolygon = true;
                } else if (wktString.startsWith("POLYGON ")) {
                    wktString = wktString.substring("POLYGON ".length());
                    polygon = true;
                } else if (wktString.startsWith("GEOMETRYCOLLECTION (")) {
                    wktString = wktString.substring("GEOMETRYCOLLECTION (".length(), wktString.length() - 1);
                    geometrycollection = true;
                    isGeometryCollection = true;
                } else {
                    valid = false;
                }
                if (valid) {
                    if (sb.length() > 0) {
                        sb.append(",");
                        sbGeometryCollection.append(",");
                    }
                    sb.append(wktString);

                    if (multipolygon) {
                        sbGeometryCollection.append("MULTIPOLYGON(").append(wktString).append(")");
                    } else if (polygon) {
                        sbGeometryCollection.append("POLYGON").append(wktString);
                    } else if (geometrycollection) {
                        sbGeometryCollection.append(wktString);
                    }
                }
            }
            String wkt = "";
            if (!isGeometryCollection) {
                sb.append(")");
                wkt = "MULTIPOLYGON(" + sb.toString();
            } else {
                sbGeometryCollection.append(")");
                wkt = "GEOMETRYCOLLECTION(" + sbGeometryCollection.toString();
            }

            GeometryFactory gf = new GeometryFactory();
            GeometryCollection gcol = gf.createGeometryCollection(GeometryFactory.toGeometryArray(geoms));


            MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layername, layername);
            mapLayer.setMapLayerMetadata(new MapLayerMetadata());

            UserData ud = new UserData(layername);
            ud.setFilename(media.getName());

            ud.setUploadedTimeInMs(System.currentTimeMillis());
            ud.setType("shapefile");

            String metadata = "";
            metadata += "User uploaded Shapefile \n";
            metadata += "Name: " + ud.getName() + " <br />\n";
            metadata += "Filename: " + ud.getFilename() + " <br />\n";
            metadata += "Date: " + ud.getDisplayTime() + " <br />\n";
            metadata += "Selected polygons (fid): <br />\n";
            metadata += "<ul>";
//            it = items.iterator();
//            while (it.hasNext()) {
//                metadata += "<li>" + it.next() + "</li>";
//            }
            metadata += "</ul>";

            MapLayerMetadata mlmd = mapLayer.getMapLayerMetadata();
            if (mlmd == null) {
                mlmd = new MapLayerMetadata();
            }
            mlmd.setMoreInfo(metadata);
            mapLayer.setMapLayerMetadata(mlmd);

        } catch (IOException e) {
            System.out.println("IO Error retrieving geometry");
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("Generic Error retrieving geometry");
            e.printStackTrace(System.out);
        }

    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void imageClicked(Event event) {
        try {
            System.out.println("*************************");
            System.out.println("Image clicked.");
            System.out.println(event.getClass().getCanonicalName());
            System.out.println(event.getData());
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                System.out.println(me.getX() + ", " + me.getY());
            }
            System.out.println("*************************");
            String wkt4326 = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\"," + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]]," + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]]," + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]]," + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST]," + "  AUTHORITY[\"EPSG\",\"4326\"]]";
            CoordinateReferenceSystem wgsCRS = CRS.parseWKT(wkt4326);
            //RendererUtilities.worldToScreenTransform(null, null, wgsCRS);
            //org.opengis.filter.expression.PropertyName.getNamespaceContext
            //org.opengis.filter.expression.PropertyName pn = new AttributeExpressionImpl(new )
            NamespaceSupport ns = new NamespaceSupport();
            RendererUtilities.worldToScreenTransform(null, null, wgsCRS);
        } catch (TransformException ex) {
            Logger.getLogger(AreaUploadShapefileWizardController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(AreaUploadShapefileWizardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
