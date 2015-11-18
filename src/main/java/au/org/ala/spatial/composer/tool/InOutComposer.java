/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.layer.ContextualLayersAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Filedownload;

/**
 * @author ajay
 */
public class InOutComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(InOutComposer.class);
    private int generationCount = 1;

    private ContextualLayersAutoComplete autoCompleteLayers;
    private String layerName;
    private String layerDisplayName;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "In/Out Comparison";
        this.totalSteps = 2;

        this.updateWindowTitle();

    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
    }

    @Override
    public boolean onFinish() {
        Query query = getSelectedSpecies();
        if (query == null) {
            return false;
        }

        try {
            StringBuilder results = new StringBuilder();

            //get area of the layer
            String field = CommonData.getLayerFacetNameDefault(layerName);
            JSONParser jp = new JSONParser();
            JSONObject jo = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/field/" + field));
            JSONArray objects = (JSONArray) jo.get("objects");
            double totalArea = 0;
            for (int i = 0; i < objects.size(); i++) {
                if (((JSONObject) objects.get(i)).containsKey("area_km")) {
                    totalArea += (Double) ((JSONObject) objects.get(i)).get("area_km");
                }
            }

            Query in = query.newFacet(new Facet(field, "*", true), false);
            Query out = query.newFacet(new Facet(field, "*", false), false);

            results.append("Species,Area name,Sq km,Occurrences,Species\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append(layerDisplayName).append(",").
                    append(totalArea).append(",").
                    append(in.getOccurrenceCount()).append(",").
                    append(in.getSpeciesCount()).append("\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append("Not in: ").append(layerDisplayName).append(",").
                    append(510000000.0 - totalArea).append(",").
                    append(out.getOccurrenceCount()).append(",").
                    append(out.getSpeciesCount()).append("\n");

            //show results
            String metadata = "<html><body>" +
                    "<div class='aooeoo'>" +
                    "<div>In (" + layerDisplayName + ") Out (rest of world) Report for: " + getSelectedSpeciesName() + "</div><br />" +
                    "<table >" +
                    "<tr><td>Area name</td><td>Sq km</td><td>Occurrences</td><td>Species</td></tr>" +
                    "<tr><td>" + layerDisplayName + "</td><td>" + totalArea + "</td><td>" + in.getOccurrenceCount() + "</td><td>" + in.getSpeciesCount() + "</td></tr>" +
                    "<tr><td>Not in: " + layerDisplayName + "</td><td>" + (510000000.0 - totalArea) + "</td><td>" + out.getOccurrenceCount() + "</td><td>" + out.getSpeciesCount() + "</td></tr>" +
                    "</table></div>";

            Event ev = new Event(StringConstants.ONCLICK, null, "In Out Report\n" + metadata);
            getMapComposer().openHTML(ev);

            //download metadata as text
            Filedownload.save(results.toString(), "text/plain", "In Out Report.csv");

            this.detach();

            return true;
        } catch (Exception e) {
            LOGGER.error("failed In Out finish", e);
        }
        return false;
    }

    public boolean download(Event event) {
        return false;
    }

    @Override
    void fixFocus() {
    }

    public void onChange$autoCompleteLayers(Event event) {
        if (autoCompleteLayers.getItemCount() > 0 && autoCompleteLayers.getSelectedItem() != null) {
            JSONObject jo = autoCompleteLayers.getSelectedItem().getValue();

            layerName = (String) jo.get(StringConstants.NAME);
            layerDisplayName = (String) jo.get(StringConstants.DISPLAYNAME);

            btnOk.setDisabled(false);
        } else {
            btnOk.setDisabled(true);
        }
    }
}
