package au.org.ala.spatial.composer.results;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author adam
 */
public class PhylogeneticDiversityListResults extends UtilityComposer {
    private static final Logger LOGGER = Logger.getLogger(PhylogeneticDiversityListResults.class);
    private String pid;
    private Button download;
    private Listbox popupListboxResults;
    private Label resultsLabel;
    private RemoteLogger remoteLogger;
    private List<SelectedArea> selectedAreas;
    private List<Object> selectedTrees;
    private List<String> header;
    private List<Map<String, String>> areaPds;
    private List<Map<String, JSONArray>> areaSpeciesMatches;

    @Override
    public void afterCompose() {
        super.afterCompose();
        selectedAreas = (List<SelectedArea>) Executions.getCurrent().getArg().get("selectedareas");
        selectedTrees = (List<Object>) Executions.getCurrent().getArg().get("selectedtrees");

        populateList();
    }

    private void fillPDTreeList() {

        Object[] trees = new Object[selectedTrees.size()];
        header = new ArrayList<String>();

        //restrict header to what is in the zul
        for (int i = 0; i < selectedAreas.size(); i++) {
            if (selectedAreas.get(i).getMapLayer() != null) {
                header.add(selectedAreas.get(i).getMapLayer().getDisplayName());
            } else {
                header.add("Selected Area");
            }
        }
        Component firstChild = getFellow(StringConstants.TREES_HEADER).getFirstChild();
        for (Component c : getFellow(StringConstants.TREES_HEADER).getChildren()) {
            header.add(c.getId().substring(3));
        }
        for (int i = 0; i < selectedAreas.size(); i++) {
            String s = "Selected Area";
            if (selectedAreas.get(i).getMapLayer() != null) {
                s = selectedAreas.get(i).getMapLayer().getDisplayName();
            }

            Listheader lh = new Listheader(s);
            lh.setHflex("min");
            getFellow(StringConstants.TREES_HEADER).insertBefore(lh, firstChild);
        }

        int row = 0;
        for (int i = 0; i < selectedTrees.size(); i++) {
            Map<String, String> j = (HashMap<String, String>) selectedTrees.get(i);

            Map<String, String> pdrow = new HashMap<String, String>();

            for (Object o : j.keySet()) {
                String key = (String) o;

                pdrow.put(key, j.get(key));
            }

            trees[row] = pdrow;

            row++;
        }

        popupListboxResults.setModel(new

                        ListModelArray(trees, false)

        );
        popupListboxResults.setItemRenderer(
                new ListitemRenderer() {

                    public void render(Listitem li, Object data, int itemIdx) {
                        Map<String, String> map = (Map<String, String>) data;

                        for (int i = 0; i < selectedAreas.size(); i++) {
                            String value = areaPds.get(i).get(map.get(StringConstants.STUDY_ID));
                            if (value == null) {
                                value = "";
                            }
                            Listcell lc = new Listcell(value);
                            lc.setParent(li);
                        }
                        for (int i = selectedAreas.size(); i < header.size(); i++) {
                            String value = map.get(header.get(i));
                            if (value == null) {
                                value = "";
                            }

                            if ("treeViewUrl".equalsIgnoreCase(header.get(i))) {
                                Html img = new Html("<i class='icon-info-sign'></i>");
                                img.setAttribute("link", value);

                                Listcell lc = new Listcell();
                                lc.setParent(li);
                                img.setParent(lc);
                                img.addEventListener(StringConstants.ONCLICK, new org.zkoss.zk.ui.event.EventListener() {

                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        //re-toggle the checked flag
                                        Listitem li = (Listitem) event.getTarget().getParent().getParent();
                                        li.getListbox().toggleItemSelection(li);

                                        String metadata = (String) event.getTarget().getAttribute("link");
                                        getMapComposer().activateLink(metadata, "Metadata", false);

                                    }
                                });


                            } else {
                                Listcell lc = new Listcell(value);
                                lc.setParent(li);
                            }
                        }
                    }
                }

        );
    }

    public void populateList() {

        areaPds = new ArrayList<Map<String, String>>();
        areaSpeciesMatches = new ArrayList<Map<String, JSONArray>>();

        try {
            for (int i = 0; i < selectedAreas.size(); i++) {
                SelectedArea sa = selectedAreas.get(i);

                Query sq = QueryUtil.queryFromSelectedArea(null, sa, null, false, null);
                CSVReader r = new CSVReader(new StringReader(sq.speciesList()));

                JSONArray ja = new JSONArray();
                for (String[] s : r.readAll()) {
                    ja.add(s[1]);
                }

                //call pd with specieslist=ja.toString()
                String url = CommonData.getSettings().getProperty(CommonData.PHYLOLIST_URL) + "/phylo/getPD";
                NameValuePair[] params = new NameValuePair[2];
                params[0] = new NameValuePair("noTreeText", StringConstants.TRUE);
                params[1] = new NameValuePair("speciesList", ja.toString());
                JSONParser jp = new JSONParser();
                JSONArray pds = (JSONArray) jp.parse(Util.readUrlPost(url, params));

                Map<String, String> pdrow = new HashMap<String, String>();
                Map<String, JSONArray> speciesRow = new HashMap<String, JSONArray>();

                for (int j = 0; j < pds.size(); j++) {
                    String tree = "" + ((JSONObject) pds.get(j)).get(StringConstants.STUDY_ID);
                    pdrow.put(tree, ((JSONObject) pds.get(j)).get("pd").toString());
                    speciesRow.put(tree, (JSONArray) ((JSONObject) pds.get(j)).get("taxaRecognised"));

                    //maxPD retrieval
                    String maxPd = ((JSONObject) pds.get(j)).get("maxPd").toString();
                    for (int k = 0; k < selectedTrees.size(); k++) {
                        if (((Map<String, String>) selectedTrees.get(k)).get(StringConstants.STUDY_ID).equals(tree)) {
                            ((Map<String, String>) selectedTrees.get(k)).put("maxPd", maxPd);
                        }
                    }
                }

                areaPds.add(pdrow);
                areaSpeciesMatches.add(speciesRow);
            }

        } catch (Exception e) {
            LOGGER.error("error reading Phylogenetic Diversity list data", e);
        }

        fillPDTreeList();
    }

    private String toCSVString(String string) {
        if (string == null) {
            return "";
        } else {
            return "\"" + string.replace("\"", "\"\"").replace("\\", "\\\\") + "\"";
        }
    }

    /**
     * download format: CSV
     * <p/>
     * areas: 1 or more
     * trees: 1 or more
     * <p/>
     * columns
     * Area Name, Area sq km, Area PD, Area number of species, Tree (use 'author' value), Study Name, Study Id
     */
    public void onClick$btnDownload() {
        StringBuilder sb = new StringBuilder();

        //header
        sb.append("Area Name,Area (sq km),PD,Number of species,Focal Clade,Max PD,Tree Name,Study Name,Number of leaves,DOI,Study Id");

        //rows
        for (int j = 0; j < selectedTrees.size(); j++) {
            Map<String, String> map = (Map<String, String>) selectedTrees.get(j);

            for (int i = 0; i < selectedAreas.size(); i++) {
                sb.append("\n");

                //'current extent' does not have a map layer
                if (selectedAreas.get(i).getMapLayer() == null) {
                    sb.append(toCSVString("Current extent")).append(",");
                    sb.append("" + Util.calculateArea(selectedAreas.get(i).getWkt()) / 1000000.0).append(",");
                } else {
                    sb.append(toCSVString(selectedAreas.get(i).getMapLayer().getDisplayName())).append(",");
                    sb.append(selectedAreas.get(i).getKm2Area()).append(",");
                }

                String s = areaPds.get(i).get(map.get(StringConstants.STUDY_ID));
                if (s == null) {
                    s = "";
                }
                sb.append(s).append(",");

                Query sq = QueryUtil.queryFromSelectedArea(null, selectedAreas.get(i), false, null);
                int resultsCount = sq.getSpeciesCount();
                sb.append("" + resultsCount).append(",");

                sb.append(toCSVString(map.get("focalClade"))).append(",");
                sb.append(toCSVString(map.get("maxPd"))).append(",");
                sb.append(toCSVString(map.get("authors"))).append(",");
                sb.append(toCSVString(map.get("studyName"))).append(",");
                sb.append(toCSVString(map.get("numberOfLeaves"))).append(",");
                sb.append(toCSVString(map.get("doi"))).append(",");
                sb.append(toCSVString(map.get("studyId")));

            }
        }

        Map<String, String> files = new HashMap<String, String>();

        String csvName = "Phylogenetic_Diversity.csv";
        files.put(csvName, sb.toString());

        //build one file for each tree
        for (int j = 0; j < selectedTrees.size(); j++) {
            Map<String, String> map = (Map<String, String>) selectedTrees.get(j);
            String treeId = map.get(StringConstants.STUDY_ID);

            sb = new StringBuilder();

            //header
            sb.append("taxaRecognised (studyId ").append(treeId).append(")");
            for (int i = 0; i < selectedAreas.size(); i++) {
                sb.append(",");
                if (selectedAreas.get(i).getMapLayer() != null) {
                    sb.append(toCSVString(selectedAreas.get(i).getMapLayer().getDisplayName()));
                } else {
                    sb.append(toCSVString("Current extent"));
                }
            }

            //build map for the rows
            Set<String> species = new HashSet<String>();
            for (int i = 0; i < selectedAreas.size(); i++) {
                if (areaSpeciesMatches.get(i).get(treeId) != null) {
                    Collections.sort(areaSpeciesMatches.get(i).get(treeId));

                    species.addAll(areaSpeciesMatches.get(i).get(treeId));
                }
            }

            //write out rows
            String[] a = new String[species.size()];
            species.toArray(a);
            for (int i = 0; i < species.size(); i++) {
                sb.append("\n");

                String s = a[i];
                sb.append("\"").append(s.replace("\"", "\"\"").replace("\\", "\\\\")).append("\"");

                for (int k = 0; k < selectedAreas.size(); k++) {
                    String v = "0";
                    if (areaSpeciesMatches.get(k).get(treeId) != null) {
                        int pos = Collections.binarySearch(areaSpeciesMatches.get(k).get(treeId), s);
                        if (pos >= 0 && pos < areaSpeciesMatches.get(k).get(treeId).size()
                                && areaSpeciesMatches.get(k).get(treeId).get(pos).equals(s)) {
                            v = "1";
                        }
                    }
                    sb.append(",").append(v);
                }
            }

            files.put("taxaRecognised_studyId_" + treeId + ".csv", sb.toString());
        }

        //make zip
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ZipOutputStream zos = new ZipOutputStream(bos);

            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                zos.write(entry.getValue().getBytes());
                zos.closeEntry();
            }

            zos.close();

        } catch (Exception e) {
            LOGGER.error("error making zip file", e);
        }


        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        Filedownload.save(bos.toByteArray(), "application/zip", "Phylogenetic_Diversity_" + sdate + "_" + spid + ".zip");

        remoteLogger.logMapAnalysis("Phylogenetic Diversity", "Export - Phylogenetic Diversity", "", "", "", spid, "Phylogenetic_Diversity" + sdate + "_" + spid + ".zip", "");

        detach();
    }
}
