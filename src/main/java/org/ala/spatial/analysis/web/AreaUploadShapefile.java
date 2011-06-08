package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.ShapefileUtils;
import org.ala.spatial.util.Zipper;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Adam
 */
public class AreaUploadShapefile extends AreaToolComposer {

    Fileupload fileUpload;
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
        //UploadEvent ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        UploadEvent ue = null;
        if (event.getName().equals("onUpload")) {
            ue = (UploadEvent) event;
        } else if (event.getName().equals("onForward")) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            System.out.println("unable to upload file");
            return;
        } else {
            System.out.println("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            System.out.println("m.getName(): " + m.getName());
            System.out.println("getContentType: " + m.getContentType());
            System.out.println("getFormat: " + m.getFormat());

            // check the content-type
//            if (m.getContentType().equalsIgnoreCase("text/plain") || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV) || m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_CSV_EXCEL)) {
//                loadUserPoints(m.getName(), m.getReaderData());
//            } else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_EXCEL)) {
//                byte[] csvdata = m.getByteData();
//                loadUserPoints(m.getName(), new StringReader(new String(csvdata)));
//            } else
            if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_KML)) {
                System.out.println("isBin: " + m.isBinary());
                System.out.println("inMem: " + m.inMemory());
                if (m.inMemory()) {
                    loadUserLayerKML(m.getName(), m.getByteData());
                } else {
                    loadUserLayerKML(m.getName(), m.getStreamData());
                }
            } else if (m.getFormat().equalsIgnoreCase("zip")) { //else if (m.getContentType().equalsIgnoreCase(LayersUtil.LAYER_TYPE_ZIP)) {
                // "/data/ala/runtime/output/layers/"
                // "/Users/ajay/projects/tmp/useruploads/"
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
                    System.out.println("Uploaded file is a shapefile. Loading...");
                    Map shape = ShapefileUtils.loadShapefile(new File(file));

                    if (shape == null) {
                        return;
                    } else {
                        String wkt = (String) shape.get("wkt");
                        wkt = wkt.replace("MULTIPOLYGON (((", "POLYGON((").replaceAll(", ", ",").replace(")))", "))");
                        System.out.println("Got shapefile wkt...");
                        //String layerName = getMapComposer().getNextAreaLayerName(txtLayerName.getValue());
                        layerName = txtLayerName.getValue();
                        MapLayer mapLayer = getMapComposer().addWKTLayer(wkt, layerName, layerName);
                        mapLayer.setMapLayerMetadata(new MapLayerMetadata());
                        mapLayer.getMapLayerMetadata().setMoreInfo("User uploaded shapefile. \n Used polygon: " + shape.get("id"));

                        ok = true;
                    }
                } else {
                    System.out.println("Unknown file type. ");
                    getMapComposer().showMessage("Unknown file type. Please upload a valid CSV, KML or Shapefile. ");
                }
            }
        } catch (Exception ex) {
            getMapComposer().showMessage("Unable to load file. Please try again. ");
            ex.printStackTrace();
        }
    }

    public void loadUserLayerKML(String name, InputStream data) {
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

            loadUserLayerKML(name, kmlData.getBytes());

        } catch (Exception e) {
            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);
        }
    }

    public void loadUserLayerKML(String name, byte[] kmldata) {
        try {

            String id = String.valueOf(System.currentTimeMillis());
            String kmlpath = "/data/ala/runtime/output/layers/" + id + "/";
            File kmlfilepath = new File(kmlpath);
            kmlfilepath.mkdirs();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(kmlfilepath.getAbsolutePath() + "/" + name)));
            String kmlstr = new String(kmldata);
            out.write(kmlstr);
            out.close();

            String kmlurl = CommonData.satServer + "/output/layers/" + id + "/" + name;

            //MapLayer mapLayer = getMapComposer().getGenericServiceAndBaseLayerSupport().createMapLayer("User-defined kml layer", txtLayerName.getValue(), "KML", kmlurl);
            MapLayer mapLayer = getMapComposer().addKMLLayer(txtLayerName.getValue(), txtLayerName.getValue(), kmlurl);

            if (mapLayer == null) {
                logger.debug("The layer " + name + " couldnt be created");
                getMapComposer().showMessage(getMapComposer().getLanguagePack().getLang("ext_layer_creation_failure"));
            } else {
                this.layerName = mapLayer.getName();
                ok = true;
                String kmlwkt = getKMLPolygonAsWKT(kmlstr);
                mapLayer.setWKT(kmlwkt);
                getMapComposer().addUserDefinedLayerToMenu(mapLayer, true);
            }
        } catch (Exception e) {

            getMapComposer().showMessage("Unable to load your file. Please try again.");

            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);
        }
    }

    private static String getKMLPolygonAsWKT(String kmldata) {
        try {

            String[] kml = kmldata.toLowerCase().split("polygon");
            int trueLength = (kml.length-1)/2;

            StringBuilder sbKml = new StringBuilder();
            if (trueLength > 1) {
                sbKml.append("GEOMETRYCOLLECTION(");
            }
            for (int j=1; j<kml.length; j+=2) {
                String k = kml[j];
                if (k.trim().equals("")) continue;
                int pos1 = k.indexOf("coordinates") + 12;
                int pos2 = k.indexOf("/coordinates") - 1;
                String kcoords = k.substring(pos1, pos2);

                String[] coords = kcoords.split(" ");
                if (coords.length == 1) {
                    coords = kcoords.split("\n");
                }

                if (j>1) {
                    sbKml.append(",\n\n");
                }

                sbKml.append("POLYGON((");
                for (int i = 0; i < coords.length; i++) {
                    String c = coords[i];
                    String[] cs = c.split(",");
                    if (cs.length > 1) {
                        if (i > 0) {
                            sbKml.append(",");
                        }
                        sbKml.append(cs[0]).append(" ").append(cs[1]);
                    }
                }
                sbKml.append("))");
            }

            if (trueLength > 1) {
                sbKml.append(")");
            }

            return sbKml.toString(); 

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return null;
    }
}
