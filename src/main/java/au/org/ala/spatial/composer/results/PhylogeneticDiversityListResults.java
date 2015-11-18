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
import au.org.emii.portal.util.AreaReportPDF;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.*;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
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

    private String csv;
    private List<String[]> csvCells;

    private ExecutorService pool;
    private LinkedBlockingQueue<SelectedArea> queue = new LinkedBlockingQueue<SelectedArea>();
    private Future<AreaReportPDF> future;
    private Map progress;
    private Label progressLabel;
    private Div divProgress;
    private Div divResults;
    private Progressmeter jobprogress;
    private Query selectedQuery;

    @Override
    public void afterCompose() {
        super.afterCompose();
        selectedAreas = (List<SelectedArea>) Executions.getCurrent().getArg().get("selectedareas");
        selectedTrees = (List<Object>) Executions.getCurrent().getArg().get("selectedtrees");
        selectedQuery = (Query) Executions.getCurrent().getArg().get("query");

        populateListWithProgressBar();
    }

    private void fillPDTreeList() {

        List list = csvCells.subList(1, csvCells.size());

        popupListboxResults.setModel(new SimpleListModel(list));

        //header:
        //Area Name,Area (sq km),PD,Proportional PD (PD / Tree PD),Species,Proportional Species (Species / Tree Species),Tree Name,Tree ID,DOI,Study Name,Notes,Tree PD
        popupListboxResults.setItemRenderer(
                new ListitemRenderer() {

                    public void render(Listitem li, Object data, int itemIdx) {
                        String[] row = (String[]) data;

                        //map everything
                        for (int i = 0; i < row.length; i++) {
                            //study id is a link
                            if (i == 7) {
                                Listcell lc = new Listcell(row[i]);
                                lc.setParent(li);
                                lc.setSclass("underline");

                                final String studyId = row[i];
                                lc.addEventListener(StringConstants.ONCLICK, new org.zkoss.zk.ui.event.EventListener() {

                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        for (int k = 0; k < selectedTrees.size(); k++) {
                                            if (((Map<String, String>) selectedTrees.get(k)).get(StringConstants.STUDY_ID).equals(studyId)) {
                                                String url = ((Map<String, String>) selectedTrees.get(k)).get("treeViewUrl");
                                                getMapComposer().activateLink(url, "Metadata", false);
                                            }
                                        }


                                    }
                                });
                            } else {
                                Listcell lc = new Listcell(row[i]);
                                lc.setParent(li);
                            }
                        }
                    }
                }

        );
    }
    
    public void populateListWithProgressBar() {

        areaPds = new ArrayList<Map<String, String>>();
        areaSpeciesMatches = new ArrayList<Map<String, JSONArray>>();
        
        for (SelectedArea sa : selectedAreas) {
            queue.add(sa);
        }
        
        progress = new ConcurrentHashMap();
        progress.put("label", "Starting");
        progress.put("percent", 0.0);

        Callable backgroundProcess = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                //TODO: setup for multiple threads
                try {
                    while (!queue.isEmpty()) {
                        SelectedArea sa = queue.take();

                        progress.put("label", "Processing area: " +
                                (selectedAreas.size() - queue.size()) + " of " + selectedAreas.size());
                        
                        evalArea(sa);

                        progress.put("percent",
                                String.valueOf((selectedAreas.size() - queue.size())  / (double) selectedAreas.size() ));
                    }
                } catch (InterruptedException e) {
                    
                }
                
                return true;
            }
        };

        pool = Executors.newFixedThreadPool(1);
        future = pool.submit(backgroundProcess);

        getMapComposer().getOpenLayersJavascript().execute("setTimeout('checkProgress()', 2000);");

        divProgress.setVisible(true);
        divResults.setVisible(false);
        
    }

    public void checkProgress(Event event) {
        if (future.isDone()) {
            progressLabel.setValue("Finished.");
            jobprogress.setValue(100);

            makeCSV();

            fillPDTreeList();

            divProgress.setVisible(false);
            divResults.setVisible(true);

        } else {
            progressLabel.setValue("Running> " + (String) progress.get("label"));
            jobprogress.setValue((int) Math.min(100, (int) (Double.parseDouble(String.valueOf(progress.get("percent"))) * 100)));

            getMapComposer().getOpenLayersJavascript().execute("setTimeout('checkProgress()', 2000);");
        }
    }

    public void populateList() {

        areaPds = new ArrayList<Map<String, String>>();
        areaSpeciesMatches = new ArrayList<Map<String, JSONArray>>();

        try {
            for (int i = 0; i < selectedAreas.size(); i++) {
                SelectedArea sa = selectedAreas.get(i);

                evalArea(sa);
            }

            makeCSV();

            fillPDTreeList();

        } catch (Exception e) {
            LOGGER.error("error reading Phylogenetic Diversity list data", e);

            getMapComposer().showMessage("Unknown error.");
            this.detach();
        }
    }
    
    private void evalArea(SelectedArea sa) {
        try {
            Query sq = QueryUtil.queryFromSelectedArea(selectedQuery, sa, null, false, null);
            
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
        } catch (Exception e) {
            LOGGER.error("failed processing a pd for a selected area.", e);
        }
    }

    private void makeCSV() {
        StringBuilder sb = new StringBuilder();

        //header
        sb.append("Area Name,Area (sq km),PD,Proportional PD (PD / Tree PD),");
        sb.append("Species,Proportional Species (Species / Tree Species),Tree Name,Tree ID,DOI,Study Name,Notes,Tree PD");

        //rows
        for (int j = 0; j < selectedTrees.size(); j++) {
            Map<String, String> map = (Map<String, String>) selectedTrees.get(j);

            for (int i = 0; i < selectedAreas.size(); i++) {
                sb.append("\n");

                //numbers
                double pd = 0;
                double maxpd = 0;
                int speciesFound = 0;
                int studySpecieCount = 1;
                try {
                    //area pd
                    pd = Double.parseDouble(areaPds.get(i).get(map.get(StringConstants.STUDY_ID)));
                    //tree pd
                    maxpd = Double.parseDouble(map.get("maxPd"));
                    //species found in tree
                    speciesFound = areaSpeciesMatches.get(i).get(map.get(StringConstants.STUDY_ID)).size();//Integer.parseInt();
                    //tree species count
                    studySpecieCount = Integer.parseInt(map.get("numberOfLeaves"));
                } catch (Exception e) {
                }


                //'current extent' does not have a map layer
                if (selectedAreas.get(i).getMapLayer() == null) {
                    sb.append(toCSVString("Current extent")).append(",");
                    sb.append(selectedAreas.get(i).getKm2Area()).append(",");
                } else {
                    sb.append(toCSVString(selectedAreas.get(i).getMapLayer().getDisplayName())).append(",");
                    sb.append(toCSVString(selectedAreas.get(i).getKm2Area())).append(",");
                }

                String s = areaPds.get(i).get(map.get(StringConstants.STUDY_ID));
                if (s == null) {
                    s = "";
                }
                sb.append(s).append(",");
                sb.append(String.format("%.4f,", pd / maxpd));

                sb.append("" + speciesFound).append(",");
                sb.append(String.format("%.4f,", speciesFound / (double) studySpecieCount));

                //tree name
                sb.append(toCSVString(map.get("authors"))).append(",");
                //tree id
                sb.append(toCSVString(map.get("studyId"))).append(",");
                //doi
                sb.append(toCSVString(map.get("doi"))).append(",");
                //study name
                sb.append(toCSVString(map.get("studyName"))).append(",");
                //notes
                sb.append(toCSVString(map.get("notes"))).append(",");
                //tree pd
                sb.append(toCSVString(map.get("maxPd")));
            }
        }

        csv = sb.toString();
        try {
            csvCells = new CSVReader(new StringReader(csv)).readAll();
        } catch (Exception e) {
            LOGGER.error("failed to parse phylogenetic diversity list", e);
        }
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
     * Area Name, Area sq km, Area PD, Area number of species, Tree (use 'author' value), Study Name, Notes, Study Id
     */
    public void onClick$btnDownload() {

        Map<String, String> files = new HashMap<String, String>();

        String csvName = "Phylogenetic_Diversity.csv";
        files.put(csvName, csv);

        //build one file for each tree
        for (int j = 0; j < selectedTrees.size(); j++) {
            Map<String, String> map = (Map<String, String>) selectedTrees.get(j);
            String treeId = map.get(StringConstants.STUDY_ID);

            StringBuilder sb = new StringBuilder();

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

    @Override
    public void detach() {
        super.detach();

        //remove all future tasks
        queue.clear();
    }
}
