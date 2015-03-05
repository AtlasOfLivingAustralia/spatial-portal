/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.composer;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.QueryField;
import au.org.ala.spatial.sampling.Sampling;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import org.apache.log4j.Logger;
import org.zkoss.zul.Filedownload;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author a
 */
public final class SamplingDownloadUtil {
    private static final Logger LOGGER = Logger.getLogger(SamplingDownloadUtil.class);

    private SamplingDownloadUtil() {
        //to hide public constructor
    }

    public static void downloadSecond(MapComposer mc, Query downloadSecondQuery, String[] downloadSecondLayers) {
        LOGGER.debug("attempting to sample biocache records with analysis layers: " + downloadSecondQuery);
        if (downloadSecondQuery != null) {
            try {
                List<QueryField> fields = new ArrayList<QueryField>();
                fields.add(new QueryField(downloadSecondQuery.getRecordIdFieldName()));
                fields.add(new QueryField(downloadSecondQuery.getRecordLongitudeFieldName()));
                fields.add(new QueryField(downloadSecondQuery.getRecordLatitudeFieldName()));

                String results = downloadSecondQuery.sample(fields);

                if (results != null) {
                    CSVReader csvreader = new CSVReader(new StringReader(results));
                    List<String[]> csv = csvreader.readAll();
                    csvreader.close();

                    int longitudeColumn = Util.findInArray(downloadSecondQuery.getRecordLongitudeFieldDisplayName(), csv.get(0));
                    int latitudeColumn = Util.findInArray(downloadSecondQuery.getRecordLatitudeFieldDisplayName(), csv.get(0));
                    int idColumn = Util.findInArray(downloadSecondQuery.getRecordIdFieldDisplayName(), csv.get(0));

                    double[] points = new double[(csv.size() - 1) * 2];
                    String[] ids = new String[csv.size() - 1];
                    int pos = 0;
                    for (int i = 1; i < csv.size(); i++) {
                        try {
                            points[pos] = Double.parseDouble(csv.get(i)[longitudeColumn]);
                            points[pos + 1] = Double.parseDouble(csv.get(i)[latitudeColumn]);
                        } catch (Exception e) {
                            points[pos] = Double.NaN;
                            points[pos + 1] = Double.NaN;
                        }
                        ids[pos / 2] = csv.get(i)[idColumn];
                        pos += 2;
                    }

                    double[][] p = new double[points.length / 2][2];
                    for (int i = 0; i < points.length; i += 2) {
                        p[i / 2][0] = points[i];
                        p[i / 2][1] = points[i + 1];
                    }

                    List<String> layers = new ArrayList<String>();
                    StringBuilder sb = new StringBuilder();
                    sb.append("id,longitude,latitude");
                    for (String layer : downloadSecondLayers) {
                        sb.append(",");
                        String name = CommonData.getLayerDisplayName(layer);
                        if (name == null) {
                            name = CommonData.getFacetLayerDisplayNameDefault(layer);
                            if (name == null) {
                                MapLayer ml = mc.getMapLayer(layer);
                                if (ml == null) {
                                    name = layer;
                                } else {
                                    name = ml.getDisplayName();
                                }
                            }
                        }
                        sb.append(name);

                        layers.add(CommonData.getLayerFacetName(layer));
                    }
                    List<String[]> sample = Sampling.sampling(layers, p);

                    if (!sample.isEmpty()) {
                        for (int j = 0; j < sample.get(0).length; j++) {
                            sb.append("\n");
                            sb.append(ids[j]).append(",").append(p[j][0]).append(",").append(p[j][1]);
                            for (int i = 0; i < sample.size(); i++) {
                                sb.append(",").append(sample.get(i)[j]);
                            }
                        }
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(baos);

                    ZipEntry anEntry = new ZipEntry("analysis_output_intersect.csv");
                    zos.putNextEntry(anEntry);
                    zos.write(sb.toString().getBytes());
                    zos.close();

                    Filedownload.save(baos.toByteArray(), "application/zip", "analysis_output_intersect.zip");


                }
            } catch (Exception e) {
                LOGGER.error("error downloading samping records", e);
            }
        }
    }
}
