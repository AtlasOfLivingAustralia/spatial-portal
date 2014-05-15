/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.util.*;
import au.org.emii.portal.menu.MapLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Radiogroup;

import java.io.*;
import java.net.URL;
import java.util.List;

/**
 * @author ajay
 */
public class ExportLayerComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(ExportLayerComposer.class);


    Radiogroup exportFormat;
    //Button btnExportCancel;
    //Button btnExportOk;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Export area";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Export Area"));
    }

    //@Override
    public void loadAreaLayersChecks() {
        String selectedLayerName = (String) params.get("polygonLayerName");
        Div areachks = (Div) getFellowIfAny("areachks");

        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayer lyr = layers.get(i);

            Checkbox cAr = new Checkbox(lyr.getDisplayName());
            cAr.setValue(lyr.getWKT());

            areachks.insertBefore(cAr, null);
        }

    }

    public void onCheck$exportFormat(Event event) {
        btnOk.setDisabled(false);
    }

    @Override
    public boolean onFinish() {
        exportAreaAs((String) exportFormat.getSelectedItem().getValue(), rAreaSelected.getLabel(), getSelectedArea());
        detach();
        return true;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                exportFormat.setFocus(true);
                break;
        }
    }

    public void exportAreaAs(String type, String name, SelectedArea sa) {
        String EXPORT_BASE_DIR = CommonData.settings.getProperty("analysis_output_dir") + File.separator + "export" + File.separator;
        try {
            String id = String.valueOf(System.currentTimeMillis());

            File shpDir = new File(EXPORT_BASE_DIR + id + File.separator);
            shpDir.mkdirs();

            File shpfile = null;

            String contentType = LayersUtil.LAYER_TYPE_ZIP;
            //String outfile = ml.getDisplayName().replaceAll(" ", "_")+("shp".equals(type)?"Shapefile":type.toUpperCase())+".zip";
            String outfile = name.replaceAll(" ", "_");
            if ("shp".equals(type)) {
                shpfile = new File(EXPORT_BASE_DIR + id + File.separator + outfile + "_Shapefile.shp");
                ShapefileUtils.saveShapefile(shpfile, sa.getWkt());

                outfile += "_SHP.zip";
            } else if ("kml".equals(type)) {

                StringBuilder sbKml = new StringBuilder();
                sbKml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\r");
                sbKml.append("<kml xmlns=\"http://earth.google.com/kml/2.2\">").append("\r");
                sbKml.append("<Document>").append("\r");
                sbKml.append("  <name>Spatial Portal Active Area</name>").append("\r");
                sbKml.append("  <description><![CDATA[Active area saved from the ALA Spatial Portal: http://spatial.ala.org.au/]]></description>").append("\r");
                sbKml.append("  <Style id=\"style1\">").append("\r");
                sbKml.append("    <LineStyle>").append("\r");
                sbKml.append("      <color>40000000</color>").append("\r");
                sbKml.append("      <width>3</width>").append("\r");
                sbKml.append("    </LineStyle>").append("\r");
                sbKml.append("    <PolyStyle>").append("\r");
                sbKml.append("      <color>73FF0000</color>").append("\r");
                sbKml.append("      <fill>1</fill>").append("\r");
                sbKml.append("      <outline>1</outline>").append("\r");
                sbKml.append("    </PolyStyle>").append("\r");
                sbKml.append("  </Style>").append("\r");
                sbKml.append("  <Placemark>").append("\r");
                sbKml.append("    <name>").append(name).append("</name>").append("\r");
                sbKml.append("    <description><![CDATA[<div dir=\"ltr\">").append(name).append("<br></div>]]></description>").append("\r");
                sbKml.append("    <styleUrl>#style1</styleUrl>").append("\r");

                //Remove first line of kmlGeometry, <?xml...>
                Geometry geom = new WKTReader().read(sa.getWkt());
                Encoder encoder = new Encoder(new KMLConfiguration());
                encoder.setIndenting(true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.encode(geom, KML.Geometry, baos);
                String kmlGeometry = new String(baos.toByteArray());
                sbKml.append(kmlGeometry.substring(kmlGeometry.indexOf('\n')));

                sbKml.append("  </Placemark>").append("\r");
                sbKml.append("</Document>").append("\r");
                sbKml.append("</kml>").append("\r");

                shpfile = new File(EXPORT_BASE_DIR + id + File.separator + outfile + "_KML.kml");
                BufferedWriter wout = new BufferedWriter(new FileWriter(shpfile));
                wout.write(sbKml.toString());
                wout.close();

                outfile += "_KML.zip";
            } else if ("wkt".equals(type)) {
                shpfile = new File(EXPORT_BASE_DIR + id + File.separator + outfile + "_WKT.txt");
                BufferedWriter wout = new BufferedWriter(new FileWriter(shpfile));
                wout.write(sa.getWkt());
                wout.close();

                outfile += "_WKT.zip";
            }

            //zip shpfile
            Zipper.zipDirectory(EXPORT_BASE_DIR + id + File.separator, EXPORT_BASE_DIR + id + ".zip");
            FileInputStream fis = null;
            try {

                byte[] bytes = FileUtils.readFileToByteArray(new File(EXPORT_BASE_DIR + id + ".zip"));
                Filedownload.save(bytes, contentType, outfile);
            } catch (Exception e) {
                logger.error("failed to download file : " + EXPORT_BASE_DIR + id + ".zip", e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        logger.error("failed to close download file : " + EXPORT_BASE_DIR + id + ".zip", e);
                    }
                }
            }


            try {
                remoteLogger.logMapAnalysis(name, "Export - " + StringUtils.capitalize(type) + " Area", sa.getWkt(), "", "", "", outfile, "download");
            } catch (Exception e) {
                logger.error("remote logger error", e);
            }

        } catch (Exception e) {
            logger.error("Unable to export user area", e);
        }
    }
}
