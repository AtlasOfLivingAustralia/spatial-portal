/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.layer.ContextualLayersAutoComplete;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Vbox;

import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author ajay
 */
public class InOutComposer extends ToolComposer {
    private static final Logger LOGGER = Logger.getLogger(InOutComposer.class);
    private int generationCount = 1;

    private Checkbox cAreasFromLayer;
    private Div divContextualLayer;
    private ContextualLayersAutoComplete autoCompleteLayers;
    private String autoCompleteLayerSelection;
    private List<SelectedArea> autoCompleteLayerAreas;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Compare Areas";
        this.totalSteps = 2;

        this.updateWindowTitle();

        this.loadAreaLayers(true);

        this.autoCompleteLayers.refresh("");
    }

    @Override
    public void onLastPanel() {
        super.onLastPanel();
    }

    @Override
    public boolean onFinish() {
        Query query = getSelectedSpecies();
        query = query.newFacet(new Facet("occurrence_status_s", "absent", false), false);
        if (query == null) {
            return false;
        }

        List<SelectedArea> sa = getSelectedAreas();

        //check the number of selected areas
        int countSelected = sa.size();
        if (autoCompleteLayerSelection != null && cAreasFromLayer.isChecked() && autoCompleteLayerAreas == null) {
            countSelected += 2;
        }
        if (countSelected != 2) {
            getMapComposer().showMessage(countSelected + " areas are selected. Select only 2 areas.");
            return false;
        }

        try {
            StringBuilder results = new StringBuilder();

            double inArea = 0;
            Query in;
            Query out;
            double outArea = 0;
            String outName;
            String inName;

            Map<String, String []> onlyIn = new HashMap<String, String[]>();
            Map<String, String []> onlyOut = new HashMap<String, String[]>();
            Map<String, String []> both = new HashMap<String, String[]>();
            String [] speciesListHeader = null;

            //get area of the layer
            if (autoCompleteLayerSelection != null && cAreasFromLayer.isChecked() && autoCompleteLayerAreas == null) {
                // in/out for a selected layer and the marine/terrestrial complement
                String fieldId = CommonData.getLayerFacetNameDefault(autoCompleteLayerSelection);
                JSONParser jp = new JSONParser();
                JSONObject jo = (JSONObject) jp.parse(Util.readUrl(CommonData.getLayersServer() + "/field/" + fieldId));
                inName = "In area (" + jo.get("name") + ")";
                JSONArray objects = (JSONArray) jo.get("objects");

                for (int i = 0; i < objects.size(); i++) {
                    if (((JSONObject) objects.get(i)).containsKey("area_km")) {
                        inArea += (Double) ((JSONObject) objects.get(i)).get("area_km");
                    }
                }

                in = query.newFacet(new Facet(fieldId, "*", true), false);

                //determine complement area
                JSONObject fld = CommonData.getLayer(fieldId);
                JSONObject layer = (JSONObject) fld.get("layer");
                boolean isMarine = Util.isSameDomain(new String[]{"marine"}, Util.getDomain(layer));
                boolean isTerrestrial = Util.isSameDomain(new String[]{"terrestrial"}, Util.getDomain(layer));

                String terrestrialQuery = CommonData.getSettings().getProperty("in_out_report.terrestrial.query", "cl2013:*");
                String terrestrialName = CommonData.getSettings().getProperty("in_out_report.terrestrial.name", "Other - ASGS Australian States and Territories");
                Double terrestrialArea = Double.parseDouble(CommonData.getSettings().getProperty("in_out_report.terrestrial.area", "7719806.774"));

                String marineQuery = CommonData.getSettings().getProperty("in_out_report.marine.query", "cl21:*");
                String marineName = CommonData.getSettings().getProperty("in_out_report.marine.name", "Other - IMCRA 4");
                Double marineArea = Double.parseDouble(CommonData.getSettings().getProperty("in_out_report.marine.area", "8669607.781"));

                if (isMarine && isTerrestrial) {
                    outName = marineName + " AND " + terrestrialName;
                    outArea = terrestrialArea + marineArea;
                    out = query.newFacets(Arrays.asList(new Facet[]{new Facet(fieldId, "*", false),
                            Facet.parseFacet("(" + terrestrialQuery + " OR " + marineQuery + ")")}), false);
                } else if (isMarine) {
                    outName = marineName;
                    outArea = marineArea;
                    out = query.newFacets(Arrays.asList(new Facet[]{new Facet(fieldId, "*", false),
                            Facet.parseFacet(marineQuery)}), false);
                } else if (isTerrestrial) {
                    outName = terrestrialName;
                    outArea = terrestrialArea;
                    out = query.newFacets(Arrays.asList(new Facet[]{new Facet(fieldId, "*", false),
                            Facet.parseFacet(terrestrialQuery)}), false);
                } else {
                    //world
                    outName = "rest of the world";
                    outArea = 510000000.0;
                    out = query.newFacet(new Facet(fieldId, "*", false), false);
                }

                outArea -= inArea;
            } else {
                inName = sa.get(0).getMapLayer().getDisplayName();
                outName = sa.get(1).getMapLayer().getDisplayName();
                in = QueryUtil.queryFromSelectedArea(query, sa.get(0), false, null);
                out = QueryUtil.queryFromSelectedArea(query, sa.get(1), false, null);
                try {
                    inArea = Double.parseDouble(sa.get(0).getKm2Area().replace(",",""));
                    outArea = Double.parseDouble(sa.get(1).getKm2Area().replace(",",""));
                } catch (Exception e) {}

                //build species lists for comparison
                List<String[]> inSpeciesList = new CSVReader(new StringReader(in.speciesList())).readAll();
                List<String[]> outSpeciesList = new CSVReader(new StringReader(out.speciesList())).readAll();

                //used to exclude header
                int row = 0;

                for (String [] line : inSpeciesList) {
                    if (row == 0) {
                        speciesListHeader = line;
                        row++;
                    } else if (line.length > 0) {
                        onlyIn.put(line[0], line);
                    }
                }

                row = 0;
                for (String [] line : outSpeciesList) {
                    if (row == 0) {
                        //header will be missing if no species in inArea
                        speciesListHeader = line;
                        row++;
                    } else if (line.length > 0) {
                        if (!onlyIn.containsKey(line[0])) {
                            onlyOut.put(line[0], line);
                        } else {
                            onlyIn.remove(line[0]);
                            both.put(line[0], line);
                        }
                    }
                }
            }

            results.append("Species,Area name,Sq km,Occurrences,Species\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append(inName).append(",").
                    append(inArea).append(",").
                    append(in.getOccurrenceCount()).append(",").
                    append(in.getSpeciesCount()).append("\n");
            results.append(getSelectedSpeciesName()).append(",");
            results.append(outName).append(" (").append(outName).append("),").
                    append(outArea).append(",").
                    append(out.getOccurrenceCount()).append(",").
                    append(out.getSpeciesCount()).append("\n");

            if (onlyIn.size() + onlyOut.size() + both.size() > 0) {
                results.append("\n");
                results.append("\nSpecies found only in ").append(inName).append(",").append(String.format("%d", onlyIn.size()));
                results.append("\nSpecies found only in ").append(outName).append(",").append(String.format("%d", onlyOut.size()));
                results.append("\nSpecies found in both areas,").append(String.format("%d", both.size()));

                results.append("\n\n").append(StringUtils.join(speciesListHeader, ",", 0, speciesListHeader.length - 3)).append(",").append(inName).append(",").append(outName);
                for (String[] line : both.values()) {
                    results.append("\n");
                    for (int i=0;i<line.length-3;i++) results.append("\"").append(line[i].replace("\"","\"\",")).append("\",");
                    results.append("found,found");
                }
                for (String[] line : onlyIn.values()) {
                    results.append("\n");
                    for (int i=0;i<line.length-3;i++) results.append("\"").append(line[i].replace("\"","\"\",")).append("\",");
                    results.append("found,not found");
                }
                for (String[] line : onlyOut.values()) {
                    results.append("\n");
                    for (int i=0;i<line.length-3;i++) results.append("\"").append(line[i].replace("\"","\"\",")).append("\",");
                    results.append("not found,found");
                }
            }

            //show results
            String metadata = "<html><body>" +
                    "<div class='aooeoo'>" +
                    "<div>Report for: " + getSelectedSpeciesName() + "<br />" + inName + "<br />" + outName + "</div><br />" +
                    "<table >" +
                    "<tr><td>Area name</td><td>Sq km</td><td>Occurrences</td><td>Species</td></tr>" +
                    "<tr><td>" + inName + "</td><td>" + String.format("%.2f", inArea) + "</td><td>" + in.getOccurrenceCount() + "</td><td>" + in.getSpeciesCount() + "</td></tr>" +
                    "<tr><td>" + outName + "</td><td>" + String.format("%.2f", outArea) + "</td><td>" + out.getOccurrenceCount() + "</td><td>" + out.getSpeciesCount() + "</td></tr>";

            if (onlyIn.size() + onlyOut.size() + both.size() > 0) {
                metadata +=
                        "<tr><td>&nbsp;</td></tr>" +
                                "<tr><td>Species found only in " + inName + "</td><td>" + String.format("%d", onlyIn.size()) + "</td></tr>" +
                                "<tr><td>Species found only in " + outName + "</td><td>" + String.format("%d", onlyOut.size()) + "</td></tr>" +
                                "<tr><td>Species found in both areas</td><td>" + String.format("%d", both.size()) + "</td></tr>";

            }

            metadata += "</table></div>";

            Event ev = new Event(StringConstants.ONCLICK, null, "Compare Areas Report\n" + metadata);
            getMapComposer().openHTML(ev);

            //download metadata as text
            Filedownload.save(results.toString(), "text/plain", "Compare Areas Report.csv");

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

    public List<SelectedArea> getSelectedAreas() {
        List<SelectedArea> selectedAreas = new ArrayList<SelectedArea>();

        Vbox vboxArea = (Vbox) getFellowIfAny("vboxArea");

        for (Component c : vboxArea.getChildren()) {
            if ((c instanceof Checkbox) && ((Checkbox) c).isChecked()) {
                SelectedArea sa = null;
                String area = ((Checkbox) c).getValue();
                try {
                    if (StringConstants.CURRENT.equals(area)) {
                        sa = new SelectedArea(null, getMapComposer().getViewArea());
                    } else if (StringConstants.AUSTRALIA.equals(area)) {
                        sa = new SelectedArea(null, CommonData.getSettings().getProperty(CommonData.AUSTRALIA_WKT));
                    } else if (StringConstants.WORLD.equals(area)) {
                        sa = new SelectedArea(null, CommonData.WORLD_WKT);
                    } else {
                        List<MapLayer> layers = getMapComposer().getPolygonLayers();
                        for (MapLayer ml : layers) {
                            if (area.equals(ml.getName())) {
                                sa = new SelectedArea(ml, null);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Unable to retrieve selected area", e);
                }
                if (sa != null) {
                    selectedAreas.add(sa);
                }
            }

        }
        return selectedAreas;
    }

    public void onCheck$cAreasFromLayer(Event event) {
        divContextualLayer.setVisible(cAreasFromLayer.isChecked());
    }

    public void onChange$autoCompleteLayers(Event event) {

        autoCompleteLayerSelection = null;
        autoCompleteLayerAreas = null;

        if (autoCompleteLayers.getItemCount() > 0 && autoCompleteLayers.getSelectedItem() != null) {
            JSONObject jo = autoCompleteLayers.getSelectedItem().getValue();

            autoCompleteLayerSelection = (String) jo.get(StringConstants.ID);

        }
    }
}
