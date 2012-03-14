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
import org.zkoss.zul.Image;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

/**
 *
 * @author ajay
 */
public class AreaUploadShapefileWizardController extends UtilityComposer {

    Image img;
    Media media;
    Combobox cbAttributes;
    Listbox lbData;
    Button btnRefresh;
    Button btnNext;
    SimpleFeatureSource source;
    SimpleFeatureCollection features;
    String file;
    String layername;

    @Override
    public void afterCompose() {
        super.afterCompose();
        //loadShape();
        //ShapefileRenderer.generateShapeImage("");
        //loadRemoteShape();
        //img.setSrc("http://localhost/~ajay/pages/meow_.jpg");

        //System.out.println(ShapefileRenderer.generateShapeImage("/Users/ajay/Downloads/MEOW_zip/MEOW2/meow_ecos.shp", "Arctic", "/Users/ajay/Downloads/MEOW_zip/"));

        //executeShapeImageRenderer();

//        System.out.println("Via args");
//        Map args = Executions.getCurrent().getArg();
//        System.out.println("args.length: " + args.size());
//        System.out.println("mblah: " + args.get("mblah"));
//
//        System.out.println("via args.attr");
//        Map args2 = Executions.getCurrent().getAttributes();
//        System.out.println("args.attr.length: " + args2.size());
//
//        System.out.println("Via setAttributes");
//
//        String blah = (String)getAttribute("blah");
//        System.out.println("Blah = " + blah);
//

        Map args = Executions.getCurrent().getArg();

        layername = (String) args.get("layername");

        media = (Media) args.get("media");
        System.out.println("Got media in wizard");
        System.out.println("m.getName(): " + media.getName());
        System.out.println("getContentType: " + media.getContentType());
        System.out.println("getFormat: " + media.getFormat());


        System.out.println("Layer name: " + layername);

        processMedia();

    }

    private void processMedia() {
        Map input = Zipper.unzipFile(media.getName(), media.getStreamData(), getMapComposer().getSettingsSupplementary().getValue("analysis_output_dir")+"layers/");
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
            String shapeout = shapepath.substring(0, shapepath.lastIndexOf("/")+1);

            if (StringUtils.isBlank(column)) {
                column = "none";
            }
            if (StringUtils.isBlank(filters)) {
                filters = "none";
            }

            //shapeimageexe = "cd /usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/classes && java -classpath .:../lib/* org.ala.spatial.util.ShapefileRenderer ";
            shapeimageexe = "java -classpath /usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/classes:/usr/local/tomcat/instance_03_webportal/webapps/webportal/WEB-INF/lib/* org.ala.spatial.util.ShapefileRenderer ";

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

            SimpleFeatureType schema = features.getSchema();
            for (AttributeType at : schema.getTypes()) {
                if (schema.getDescriptor(at.getName()) == null) {
                    continue;
                }
                Comboitem ci = new Comboitem(at.getName().toString());
                ci.setValue(at.getName().toString());
                ci.setParent(cbAttributes);
            }
            cbAttributes.setSelectedIndex(0);

            // loadFeatures
            // check if only a single feature,
            // if so, then select it and map it automatically
            System.out.println("features.size(): " + features.size());
            if (features.size() > 1) {
                System.out.println("Loading all features...");
                loadFeatures((String) cbAttributes.getSelectedItem().getValue());
                System.out.println("Generating image...");
                executeShapeImageRenderer(file, "none", "none");
            } else {
                System.out.println("only a single feature, bypassing wizard...");
                ArrayList<String> tmpList = new ArrayList<String>();
                SimpleFeatureIterator fi = features.features();
                tmpList.add(String.valueOf(fi.next().getAttribute(cbAttributes.getValue())));
                loadOnMap(cbAttributes.getValue(), tmpList);
                this.detach();
            }

        } catch (IOException e) {
            System.out.println("IO Exception ");
        } catch (Exception e) {
            System.out.println("Generic exception");
            e.printStackTrace(System.out);
        }
    }

    private void loadFeatures(String attribute) {
        try {
            lbData.getItems().clear();

            System.out.println("Loading values for '" + attribute + "'");

            ArrayList<String> tmpList = new ArrayList<String>();

            SimpleFeatureIterator fi = features.features();
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                //System.out.println(" > " + f.getID() + " - " + f.getAttribute("ECOREGION") + " - " + f.getAttribute("PROVINCE"));
                Listitem li = new Listitem();
                Listcell lc = null;
                String value = "";
                if (StringUtils.isBlank(attribute)) {
                    lc = new Listcell(f.getID());
                } else {
                    Object obj = f.getAttribute(attribute);
                    if (obj == null) {
                        value = f.getID();
                    } else {
                        value = String.valueOf(obj);
                    }
                    lc = new Listcell(value);
                }
                // check if the value is already added,
                // if so, let's ignore it.
                if (!tmpList.contains(value)) {
                    lc.setParent(li);
                    //li.setValue(f.getID());
                    li.setValue(value);
                    li.setParent(lbData);
                    tmpList.add(value);
                }
            }

//            // check if only a single feature,
//            // if so, then select it and map it automatically
//            if (tmpList.size() == 1) {
//                loadOnMap(cbAttributes.getValue(), tmpList);
//                return;
//            }

        } catch (Exception e) {
            System.out.println("Unable to load features");
            e.printStackTrace(System.out);
        }
    }

    public void onSelect$cbAttributes(Event event) {
        loadFeatures(cbAttributes.getValue());
    }

    public void onClick$btnRefresh(Event event) {
        String column = cbAttributes.getValue();
        String filter = "";

        Iterator<Listitem> it = lbData.getSelectedItems().iterator();
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

        Iterator<Listitem> it = lbData.getSelectedItems().iterator();
        ArrayList<String> items = new ArrayList<String>();
        while (it.hasNext()) {
            Listitem li = it.next();
            items.add((String) li.getValue());
        }

        loadOnMap(cbAttributes.getValue(), items);

        this.detach();
    }

    private void loadOnMap(String column, ArrayList<String> items) {
        try {
            if (items.size() == 0 || StringUtils.isBlank(column)) {
                return;
            }
            
            final FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
            ArrayList<Filter> filters = new ArrayList<Filter>();
            Iterator<String> it = items.iterator();
            while (it.hasNext()) {
                filters.add(ff.like(ff.property(column), it.next()));
            }
            Filter filter = ff.or(filters);
            SimpleFeatureCollection sff = source.getFeatures(filter);
            SimpleFeatureIterator fif = sff.features();
            StringBuilder sb = new StringBuilder();
            StringBuilder sbGeometryCollection = new StringBuilder();
            boolean isGeometryCollection = false;
            ArrayList<Geometry> geoms = new ArrayList<Geometry>();
            while (fif.hasNext()) {
                SimpleFeature f = fif.next();
                //geoms.add((Geometry) f.getDefaultGeometry());


                Geometry geom = (Geometry) f.getDefaultGeometry();
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
            metadata += "Selected polygons (" + cbAttributes.getValue() + "): <br />\n";
            metadata += "<ul>";
            it = items.iterator();
            while (it.hasNext()) {
                metadata += "<li>"+it.next()+"</li>";
            }
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
}
