/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.ShapefileUtils;
import au.org.ala.spatial.util.Zipper;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * @author ajay
 */
public class ExportLayerComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(ExportLayerComposer.class);


    private Radiogroup exportFormat;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Export area";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Export Area"));

        if (rgArea.getItemCount() == 0) {
            getMapComposer().showMessage("No areas mapped. Create an area using Add to Map | Area");
            this.detach();
        }
    }

    //@Override
    public void loadAreaLayersChecks() {
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
        if (currentStep == 1) {
            rgArea.setFocus(true);
        } else if (currentStep == 2) {
            exportFormat.setFocus(true);
        }
    }

    public void exportAreaAs(String type, String name, SelectedArea sa) {
        String exportBaseDir = CommonData.getSettings().getProperty(StringConstants.ANALYSIS_OUTPUT_DIR) + File.separator + "export" + File.separator;
        try {
            String id = String.valueOf(System.currentTimeMillis());

            File shpDir = new File(exportBaseDir + id + File.separator);
            shpDir.mkdirs();

            File shpfile;

            String contentType = LayersUtil.LAYER_TYPE_ZIP;

            String outfile = name.replaceAll(" ", "_");
            if ("shp".equals(type)) {
                shpfile = new File(exportBaseDir + id + File.separator + outfile + "_Shapefile.shp");
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

                shpfile = new File(exportBaseDir + id + File.separator + outfile + "_KML.kml");
                BufferedWriter wout = new BufferedWriter(new FileWriter(shpfile));
                wout.write(sbKml.toString());
                wout.close();

                outfile += "_KML.zip";
            } else if (StringConstants.WKT.equals(type)) {
                shpfile = new File(exportBaseDir + id + File.separator + outfile + "_WKT.txt");
                BufferedWriter wout = new BufferedWriter(new FileWriter(shpfile));
                wout.write(sa.getWkt());
                wout.close();

                outfile += "_WKT.zip";
            }

            //zip shpfile
            Zipper.zipDirectory(exportBaseDir + id + File.separator, exportBaseDir + id + ".zip");
            try {
                byte[] bytes = FileUtils.readFileToByteArray(new File(exportBaseDir + id + ".zip"));
                Filedownload.save(bytes, contentType, outfile);
            } catch (Exception e) {
                LOGGER.error("failed to download file : " + exportBaseDir + id + ".zip", e);
            }

            try {
                remoteLogger.logMapAnalysis(name, "Export - " + StringUtils.capitalize(type) + " Area", sa.getWkt(), "", "", "", outfile, "download");
            } catch (Exception e) {
                LOGGER.error("remote logger error", e);
            }

        } catch (Exception e) {
            LOGGER.error("Unable to export user area", e);
        }
    }
}
