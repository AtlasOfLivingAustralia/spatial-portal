package au.org.ala.spatial.composer.add.area;

import au.org.ala.spatial.util.ShapefileUtils;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Zipper;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.apache.log4j.Logger;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam
 */
public class AreaUploadShapefile extends AreaToolComposer {

    private static Logger logger = Logger.getLogger(AreaUploadShapefile.class);
    //Fileupload fileUpload;
    Button fileUpload;
    Textbox txtLayerName;

    @Override
    public void afterCompose() {
        super.afterCompose();
        txtLayerName.setValue(getMapComposer().getNextAreaLayerName("My Area"));
        fileUpload.addEventListener("onUpload", new EventListener() {

            public void onEvent(Event event) throws Exception {
                onClick$btnOk(event);
            }
        });
    }

    public void onClick$btnOk(Event event) {
        onUpload$btnFileUpload(event);
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onUpload$btnFileUpload(Event event) {

        UploadEvent ue = null;
        if (event.getName().equals("onUpload")) {
            ue = (UploadEvent) event;
        } else if (event.getName().equals("onForward")) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            logger.debug("unable to upload file");
            return;
        } else {
            logger.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            logger.debug("m.getName(): " + m.getName());
            logger.debug("getContentType: " + m.getContentType());
            logger.debug("getFormat: " + m.getFormat());

            UserData ud = new UserData(txtLayerName.getValue());
            ud.setFilename(m.getName());

            byte[] kmldata = getKml(m);
            if (kmldata != null) {
                loadUserLayerKML(m.getName(), kmldata, ud);

            } else if (m.getName().toLowerCase().endsWith("zip")) {
                Map args = new HashMap();
                args.put("layername", txtLayerName.getValue());
                args.put("media", m);
                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/area/AreaUploadShapefileWizard.zul", this.getParent(), args);
                try {
                    window.doModal();
                } catch (SuspendNotAllowedException e) {
                    // we are really closing the window without opening/displaying to the user                
                }
            } else if (m.getName().toLowerCase().endsWith("zip_removeme")) {

                Map input = Zipper.unzipFile(m.getName(), m.getStreamData(), "/data/ala/runtime/output/layers/");
                String type = "";
                String file = "";
                if (input.containsKey("type")) {
                    type = (String) input.get("type");
                }
                if (input.containsKey("file")) {
                    file = (String) input.get("file");
                }
                if (type.equalsIgnoreCase("shp")) {
                    logger.debug("Uploaded file is a shapefile. Loading...");
                    Map shape = ShapefileUtils.loadShapefile(new File(file));

                    if (shape == null) {
                        return;
                    } else {
                        String wkt = (String) shape.get("wkt");
                        logger.debug("Got shapefile wkt...");
                        layerName = txtLayerName.getValue();
                        MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layerName, layerName);

                        ud.setUploadedTimeInMs(System.currentTimeMillis());
                        ud.setType("shapefile");

                        String metadata = "";
                        metadata += "User uploaded Shapefile \n";
                        metadata += "Name: " + ud.getName() + " <br />\n";
                        metadata += "Filename: " + ud.getFilename() + " <br />\n";
                        metadata += "Date: " + ud.getDisplayTime() + " <br />\n";

                        mapLayer.getMapLayerMetadata().setMoreInfo(metadata);
                        ok = true;
                    }
                } else {
                    logger.debug("Unknown file type. ");
                    getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, \nKML or Shapefile. ");
                }
            } else {
                logger.debug("Unknown file type. ");
                getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, \nKML or Shapefile. ");
            }
        } catch (Exception ex) {
            getMapComposer().showMessage("Unable to load file. Please try again. ");
            ex.printStackTrace();
        }
    }

    private byte[] getKml(Media m) {
        try {
            String kmlData = "";

            if (m.inMemory()) {
                kmlData = new String(m.getByteData());
            } else if (m.isBinary()) {
                InputStream data = m.getStreamData();
                if (data != null) {
                    Writer writer = new StringWriter();

                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(
                                new InputStreamReader(data));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, n);
                        }
                    } finally {
                        data.close();
                    }
                    kmlData = writer.toString();
                }
            } else if ("txt".equals(m.getFormat())) {
                Writer writer = new StringWriter();
                char[] buffer = new char[1024];
                try {
                    Reader reader = m.getReaderData();
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                }
                kmlData = writer.toString();
            }

            if (kmlData.contains("xml") && kmlData.contains("Document")) { // kmlData.contains("kml") && 
                return kmlData.getBytes();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.debug("Exception checking if kml file", e);
        }
        return null;
    }

    public void loadUserLayerKML(String name, InputStream data, UserData ud) {
        try {
            String kmlData = "";

            if (data != null) {
                Writer writer = new StringWriter();

                char[] buffer = new char[1024];
                try {
                    Reader reader = new BufferedReader(
                            new InputStreamReader(data));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } finally {
                    data.close();
                }
                kmlData = writer.toString();
            }

            loadUserLayerKML(name, kmlData.getBytes(), ud);

        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            logger.debug("unable to load user kml: ", e);
        }
    }

    public void loadUserLayerKML(String name, byte[] kmldata, UserData ud) {
        try {

            String id = String.valueOf(System.currentTimeMillis());
            String kmlpath = "/data/ala/runtime/output/layers/" + id + "/";
            File kmlfilepath = new File(kmlpath);
            kmlfilepath.mkdirs();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(kmlfilepath.getAbsolutePath() + "/" + name)));
            String kmlstr = new String(kmldata);
            out.write(kmlstr);
            out.close();

            MapComposer mc = getMapComposer();
            layerName = (mc.getMapLayer(txtLayerName.getValue()) == null) ? txtLayerName.getValue() : mc.getNextAreaLayerName(txtLayerName.getValue());
            String wkt = getKMLPolygonAsWKT(kmlstr);
            MapLayer mapLayer = mc.addWKTLayer(wkt, layerName, txtLayerName.getValue());

            ud.setUploadedTimeInMs(Long.parseLong(id));
            ud.setType("kml");

            String metadata = "";
            metadata += "User uploaded KML area \n";
            metadata += "Name: " + ud.getName() + " <br />\n";
            metadata += "Filename: " + ud.getFilename() + " <br />\n";
            metadata += "Date: " + ud.getDisplayTime() + " <br />\n";

            mapLayer.getMapLayerMetadata().setMoreInfo(metadata);

            if (mapLayer == null) {
                logger.debug("The layer " + name + " couldnt be created");
                mc.showMessage(mc.getLanguagePack().getLang("ext_layer_creation_failure"));
            } else {
                ok = true;
                mc.addUserDefinedLayerToMenu(mapLayer, true);
            }
        } catch (Exception e) {

            getMapComposer().showMessage("Unable to load your file. Please try again.");

            logger.debug("unable to load user kml: ", e);
        }
    }

    private static String getKMLPolygonAsWKT(String kmldata) {
        try {
            Parser parser = new Parser(new KMLConfiguration());
            SimpleFeature f = (SimpleFeature) parser.parse(new StringReader(kmldata));
            Collection placemarks = (Collection) f.getAttribute("Feature");

            Geometry g = null;
            SimpleFeature sf = null;

            //for <Placemark>
            if (placemarks.size() > 0 && placemarks.size() > 0) {
                sf = (SimpleFeature) placemarks.iterator().next();
                g = (Geometry) sf.getAttribute("Geometry");
            }

            //for <Folder><Placemark>
            if (g == null && sf != null) {
                placemarks = (Collection) sf.getAttribute("Feature");
                if (placemarks != null && placemarks.size() > 0) {
                    g = (Geometry) ((SimpleFeature) placemarks.iterator().next()).getAttribute("Geometry");
                }
            }

            if (g != null) {
                WKTWriter wr = new WKTWriter();
                String wkt = wr.write(g);
                return wkt.replace(" (", "(").replace(", ", ",").replace(") ", ")");
            }

        } catch (Exception e) {
            logger.error("erro converting KML to WKT: " + kmldata.substring(0, 200));
        }

        return null;
    }
}
