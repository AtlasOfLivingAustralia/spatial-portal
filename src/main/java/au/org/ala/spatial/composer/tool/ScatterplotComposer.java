/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.data.*;
import au.org.ala.spatial.userpoints.RecordsLookup;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.ScatterplotData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.UserData;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zul.Checkbox;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * @author ajay
 */
public class ScatterplotComposer extends ToolComposer {

    private static Logger logger = Logger.getLogger(ScatterplotComposer.class);
    int generation_count = 1;
    ScatterplotData data;
    Checkbox chkShowEnvIntersection;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Scatterplot";
        this.totalSteps = 6;

        this.setIncludeAnalysisLayersForAnyQuery(true);
        //this.setIncludeAnalysisLayersForUploadQuery(true);

        this.loadAreaLayers("World");
        this.loadSpeciesLayers();
        this.loadAreaLayersHighlight();
        this.loadSpeciesLayersBk();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
        this.updateName(getMapComposer().getNextAreaLayerName("My Scatterplot"));
    }

    @Override
    public boolean onFinish() {
        logger.debug("Area: " + getSelectedArea());
        logger.debug("Species: " + getSelectedSpecies());

        Query query = getSelectedSpecies();
        if (query == null) {
            getMapComposer().showMessage("There was a problem selecting the species.  Try to select the species again", this);
            return false;
        }

        Query lsid = getSelectedSpecies();
        String name = getSelectedSpeciesName();

        JSONObject jo = cbLayer1.getSelectedItem().getValue();
        String lyr1name = cbLayer1.getText();
        String lyr1value = jo.getString("name");

        jo = cbLayer2.getSelectedItem().getValue();
        String lyr2name = cbLayer2.getText();
        String lyr2value = jo.getString("name");

        String pid = "";
        Rectangle2D.Double selection = null;
        boolean enabled = true;

        Query backgroundLsid = getSelectedSpeciesBk();
        if (bgSearchSpeciesACComp.hasValidAnnotatedItemSelected()) {
            backgroundLsid = bgSearchSpeciesACComp.getQuery(getMapComposer(), false, getGeospatialKosher());//QueryUtil.get((String) bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), false, getGeospatialKosher());
        }

        SelectedArea filterSa = getSelectedArea();
        SelectedArea highlightSa = getSelectedAreaHighlight();

        boolean envGrid = chkShowEnvIntersection.isChecked();

        Query lsidQuery = QueryUtil.queryFromSelectedArea(lsid, filterSa, false, getGeospatialKosher());

        Query backgroundLsidQuery = QueryUtil.queryFromSelectedArea(backgroundLsid, filterSa, false, getGeospatialKosherBk());

        //split layers into 'in biocache' and 'out of biocache'
        Set<String> biocacheLayers = CommonData.biocacheLayerList;
        logger.debug("biocachelayers:" + biocacheLayers);
        if (biocacheLayers.contains(lyr2value) || biocacheLayers.contains(lyr1value)) {
            lsidQuery = convertToLocal(lsidQuery);
            backgroundLsidQuery = convertToLocal(backgroundLsidQuery);
        }

        ScatterplotData data = new ScatterplotData(lsidQuery, name, lyr1value,
                lyr1name, lyr2value, lyr2name, pid, selection, enabled,
                (backgroundLsid != null) ? backgroundLsidQuery : null,
                filterSa, highlightSa, envGrid);

        getMapComposer().loadScatterplot(data, tToolName.getValue());

        this.detach();

        try {
            String extras = "";
            if (highlightSa != null) {
                extras += "highlight=" + highlightSa.getWkt();
            }
            if (backgroundLsid != null && backgroundLsid instanceof BiocacheQuery) {
                extras += "background=" + ((BiocacheQuery) backgroundLsid).getLsids();
            } else if (backgroundLsid != null && backgroundLsid instanceof UploadQuery) {
                extras += "background=" + backgroundLsid.getQ();
            } else {
                extras += "background=none";
            }

            if (lsidQuery instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) lsidQuery;
                extras = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false) + "|" + extras;
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), bq.getLsids(), lyr1value + ":" + lyr2value, pid, extras, "SUCCESSFUL");
            } else if (lsidQuery instanceof UploadQuery) {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), lsidQuery.getQ(), "", pid, extras, "SUCCESSFUL");
            } else {
                remoteLogger.logMapAnalysis(tToolName.getValue(), "Tool - Scatterplot", filterSa.getWkt(), "", "", pid, extras, "SUCCESSFUL");
            }
        } catch (Exception e) {
            logger.error("logging error", e);
        }

        return true;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                rgArea.setFocus(true);
                break;
            case 2:
                if (rSpeciesSearch.isChecked()) {
                    searchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpecies.setFocus(true);
                }
                break;
            case 3:
                rgAreaHighlight.setFocus(true);
                break;
            case 4:
                cbLayer2.setFocus(true);
                break;
            case 5:
                if (rSpeciesSearchBk.isChecked()) {
                    bgSearchSpeciesACComp.getAutoComplete().setFocus(true);
                } else {
                    rgSpeciesBk.setFocus(true);
                }
                break;
            case 6:
                tToolName.setFocus(true);
                break;
        }
    }

    private Query convertToLocal(Query lsid) {
        if (lsid instanceof BiocacheQuery) {
            ArrayList<QueryField> f = new ArrayList<QueryField>();
            f.add(new QueryField(lsid.getRecordIdFieldName()));
            f.add(new QueryField(lsid.getRecordLongitudeFieldName()));
            f.add(new QueryField(lsid.getRecordLatitudeFieldName()));

            String results = lsid.sample(f);

            // Read a line in to check if it's a valid file
            // if it throw's an error, then it's not a valid csv file
            CSVReader reader = new CSVReader(new StringReader(results));

            List userPoints = null;
            try {
                userPoints = reader.readAll();
            } catch (IOException e) {
                logger.error("error reading scatterplot results", e);
            }

            logger.debug("userPoints.size(): " + userPoints.size());
            //if only one column treat it as a list of LSID's
            if (userPoints.size() == 0) {
                throw (new RuntimeException("no data in csv"));
            }

            boolean hasHeader = false;

            // check if it has a header
            String[] upHeader = (String[]) userPoints.get(0);
            try {
                Double d1 = new Double(upHeader[1]);
                Double d2 = new Double(upHeader[2]);
            } catch (Exception e) {
                hasHeader = true;
            }

            logger.debug("hasHeader: " + hasHeader);

            UserData ud = new UserData("scatterplot");

            // check if the count of points goes over the threshold.
            int sizeToCheck = (hasHeader) ? userPoints.size() - 1 : userPoints.size();
            logger.debug("Checking user points size: " + sizeToCheck + " -> " + settingsSupplementary.getValueAsInt("max_record_count_upload"));

            ArrayList<QueryField> fields = new ArrayList<QueryField>();
            if (upHeader.length == 2) {
                //only points upload, add 'id' column at the start
                fields.add(new QueryField("id"));
                fields.get(0).ensureCapacity(sizeToCheck);
            }
            String[] defaultHeader = {"id", "longitude", "latitude"};
            for (int i = 0; i < upHeader.length; i++) {
                String n = upHeader[i];
                if (upHeader.length == 2 && i < 2) {
                    n = defaultHeader[i + 1];
                } else if (upHeader.length > 2 && i < 3) {
                    n = defaultHeader[i];
                }
                fields.add(new QueryField("f" + String.valueOf(i), n, QueryField.FieldType.AUTO));
                fields.get(fields.size() - 1).ensureCapacity(sizeToCheck);
            }

            double[] points = new double[sizeToCheck * 2];
            int counter = 1;
            int hSize = hasHeader ? 1 : 0;
            for (int i = 0; i < userPoints.size() - hSize; i++) {
                String[] up = (String[]) userPoints.get(i + hSize);
                if (up.length > 2) {
                    for (int j = 0; j < up.length && j < fields.size(); j++) {
                        //replace anything that may interfere with webportal facet parsing
                        String s = up[j].replace("\"", "'").replace(" AND ", " and ").replace(" OR ", " or ");
                        if (s.length() > 0 && s.charAt(0) == '*') {
                            s = "_" + s;
                        }
                        fields.get(j).add(s);
                    }
                    try {
                        points[i * 2] = Double.parseDouble(up[1]);
                        points[i * 2 + 1] = Double.parseDouble(up[2]);
                    } catch (Exception e) {
                    }
                } else if (up.length > 1) {
                    fields.get(0).add(ud.getName() + "-" + counter);
                    for (int j = 0; j < up.length && j < fields.size(); j++) {
                        fields.get(j + 1).add(up[j]);
                    }
                    try {
                        points[i * 2] = Double.parseDouble(up[0]);
                        points[i * 2 + 1] = Double.parseDouble(up[1]);
                    } catch (Exception e) {
                    }
                    counter++;
                }
            }

            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).store();
            }

            String pid = String.valueOf(System.currentTimeMillis());

            ud.setFeatureCount(userPoints.size() - hSize);

            String metadata = "";
            metadata += "User uploaded points \n";
            metadata += "Name: " + ud.getName() + " <br />\n";
            metadata += "Description: " + ud.getDescription() + " <br />\n";
            metadata += "Date: " + ud.getDisplayTime() + " <br />\n";
            metadata += "Number of Points: " + ud.getFeatureCount() + " <br />\n";

            ud.setMetadata(metadata);
            ud.setSubType(LayerUtilities.SPECIES_UPLOAD);
            ud.setLsid(pid);

            lsid = new UploadQuery(pid, ud.getName(), points, fields, metadata);
            ud.setQuery(lsid);
            RecordsLookup.putData(pid, points, fields, metadata);
            try {
                // close the reader and data streams
                reader.close();
            } catch (IOException e) {
                logger.error("error closing user uploaded points file", e);
            }

            // add it to the user session
            Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
            if (htUserSpecies == null) {
                htUserSpecies = new Hashtable<String, UserData>();
            }
            htUserSpecies.put(pid, ud);
            getMapComposer().getSession().setAttribute("userpoints", htUserSpecies);

        }
        return lsid;
    }
}
