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

import java.util.Arrays;

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

        this.selectedMethod = "Compare Areas";
        this.totalSteps = 2;

        this.updateWindowTitle();

        this.autoCompleteLayers.refresh("");
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
            Query out;

            //determine complement area
            JSONObject fld = CommonData.getLayer(layerName);
            JSONObject layer = (JSONObject) fld.get("layer");
            boolean isMarine = Util.isSameDomain(new String[]{"marine"}, Util.getDomain(layer));
            boolean isTerrestrial = Util.isSameDomain(new String[]{"terrestrial"}, Util.getDomain(layer));

            String terrestrialQuery = CommonData.getSettings().getProperty("in_out_report.terrestrial.query", "cl2013:*");
            String terrestrialName = CommonData.getSettings().getProperty("in_out_report.terrestrial.name", "Other - ASGS Australian States and Territories");
            Double terrestrialArea = Double.parseDouble(CommonData.getSettings().getProperty("in_out_report.terrestrial.area", "7719806.774"));

            String marineQuery = CommonData.getSettings().getProperty("in_out_report.marine.query", "cl21:*");
            String marineName = CommonData.getSettings().getProperty("in_out_report.marine.name", "Other - IMCRA 4");
            Double marineArea = Double.parseDouble(CommonData.getSettings().getProperty("in_out_report.marine.area", "8669607.781"));

            double outArea = 0;
            String outName;
            if (isMarine && isTerrestrial) {
                outName = marineName + " AND " + terrestrialName;
                outArea = terrestrialArea + marineArea;
                out = query.newFacets(Arrays.asList(new Facet[]{new Facet(field, "*", false),
                        Facet.parseFacet("(" + terrestrialQuery + " OR " + marineQuery + ")")}), false);
            } else if (isMarine) {
                outName = marineName;
                outArea = marineArea;
                out = query.newFacets(Arrays.asList(new Facet[]{new Facet(field, "*", false),
                        Facet.parseFacet(marineQuery)}), false);
            } else if (isTerrestrial) {
                outName = terrestrialName;
                outArea = terrestrialArea;
                out = query.newFacets(Arrays.asList(new Facet[]{new Facet(field, "*", false),
                        Facet.parseFacet(terrestrialQuery)}), false);
            } else {
                //world
                outName = "rest of the world";
                outArea = 510000000.0;
                out = query.newFacet(new Facet(field, "*", false), false);
            }

            results.append("Species,Area name,Sq km,Occurrences,Species\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append(layerDisplayName).append(",").
                    append(totalArea).append(",").
                    append(in.getOccurrenceCount()).append(",").
                    append(in.getSpeciesCount()).append("\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append("Not in: ").append(layerDisplayName).append(" (").append(outName).append("),").
                    append(outArea - totalArea).append(",").
                    append(out.getOccurrenceCount()).append(",").
                    append(out.getSpeciesCount()).append("\n");

            //show results
            String metadata = "<html><body>" +
                    "<div class='aooeoo'>" +
                    "<div>Report for: " + getSelectedSpeciesName() + "<br />In area (" + layerDisplayName + ")<br />Out area (" + outName + ") </div><br />" +
                    "<table >" +
                    "<tr><td>Area name</td><td>Sq km</td><td>Occurrences</td><td>Species</td></tr>" +
                    "<tr><td>" + layerDisplayName + "</td><td>" + String.format("%.2f", totalArea) + "</td><td>" + in.getOccurrenceCount() + "</td><td>" + in.getSpeciesCount() + "</td></tr>" +
                    "<tr><td>Not in: " + layerDisplayName + "</td><td>" + String.format("%.2f", (outArea - totalArea)) + "</td><td>" + out.getOccurrenceCount() + "</td><td>" + out.getSpeciesCount() + "</td></tr>" +
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

            layerName = (String) jo.get(StringConstants.ID);
            layerDisplayName = (String) jo.get(StringConstants.NAME);

            btnOk.setDisabled(false);
        } else {
            btnOk.setDisabled(true);
        }
    }
}
