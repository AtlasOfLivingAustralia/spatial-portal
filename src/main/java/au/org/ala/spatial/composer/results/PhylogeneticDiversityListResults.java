package au.org.ala.spatial.composer.results;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.QueryUtil;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import net.sf.json.JSONArray;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;
import net.sf.json.JSONObject;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.*;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author adam
 */
public class PhylogeneticDiversityListResults extends UtilityComposer {
    private static Logger logger = Logger.getLogger(PhylogeneticDiversityListResults.class);

    RemoteLogger remoteLogger;
    public String pid;
    public Object[] results; //HashMap<String, String>
    public Button download;
    public Listbox popup_listbox_results;
    public Label results_label;
    List<SelectedArea> selectedAreas;
    List<Object> selectedTrees; //HashMap<String, String>
    ArrayList<String> header;

    @Override
    public void afterCompose() {
        super.afterCompose();
        selectedAreas = (List<SelectedArea>) Executions.getCurrent().getArg().get("selectedareas");
        selectedTrees = (List<Object>) Executions.getCurrent().getArg().get("selectedtrees");

        populateList();
    }

    public void populateList() {

        results = new Object[selectedAreas.size()];

        header = new ArrayList<String>();
        header.add("Area Name");

        try {
            int row = 0;
            for(SelectedArea sa : selectedAreas) {
                results[row] = new HashMap<String, String>();
                Query sq = QueryUtil.queryFromSelectedArea(null, sa, null, false, null);
                CSVReader r = new CSVReader(new StringReader(sq.speciesList()));

                JSONArray ja = new JSONArray();
                for (String[] s : r.readAll()) {
                    ja.add(s[1]);
                }

                //call pd with specieslist=ja.toString()
                String url = CommonData.settings.getProperty(CommonData.PHYLOLIST_URL) + "/phylo/getPD";
                NameValuePair[] params = new NameValuePair[2];
                params[0] = new NameValuePair("noTreeText","true");
                params[1] = new NameValuePair("speciesList", ja.toString());
                JSONArray pds = null;

                //try n times
                int t = 0;
                int maxTry = 3;
                while (t < maxTry && pds == null) {
                    t++;
                    try {
                        pds = JSONArray.fromObject(Util.readUrlPost(url, params));
                    } catch (Exception e) {
                        //so it fails, that's why trying again
                    }
                }
                if (t == maxTry) {
                    logger.error("failed to get PD for url: " + url + " and species list: " + ja.toString());
                }

                Map<String, String> pdrow = new HashMap<String, String>();
                if (sa.getMapLayer() != null) {
                    pdrow.put("Area Name", sa.getMapLayer().getDisplayName());
                } else {
                    pdrow.put("Area Name", "Current Extent");
                }

                for (int i = 0; i < pds.size(); i++) {
                    String tree = (String)((JSONObject) pds.get(i)).get("treeId");

                    boolean found = false;
                    //is this tree a selected tree?
                    for(Object o : selectedTrees) {
                        Map<String, String> map = (Map<String, String>) o;
                        if (map.get("treeId").equals(tree)) {
                            found = true;
                        }
                    }

                    if(found) {
                        if (!header.contains(tree)) {
                            header.add(tree);
                        }
                        pdrow.put(tree, ((JSONObject) pds.get(i)).getString("pd"));
                    }
                }

                results[row] = pdrow;

                row++;
            }

            //write out header
            Component c = getFellow("popup_listbox_header");
            for(int i=0;i<header.size();i++) {
                Listheader lh = new Listheader();
                lh.setParent(c);
                lh.setLabel(header.get(i));
                lh.setHflex("min");
            }

            //java.util.Arrays.sort(results);

            popup_listbox_results.setModel(new ListModelArray(results, false));
            popup_listbox_results.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data, int item_idx) {
                            Map<String, String> map = (Map<String, String>) data;

                            for(int i=0;i<header.size();i++) {
                                String value = map.get(header.get(i));
                                if(value == null) {
                                    value = "";
                                }
                                Listcell lc = new Listcell(value);
                                lc.setParent(li);
                            }
                        }
                    });

        } catch (Exception e) {
            logger.error("error reading Phylogenetic Diversity list data", e);
        }
    }

    public void onClick$btnDownload() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < header.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(header.get(i).replace("\"","\"\"")).append("\"");
        }

        for(int j=0;j<results.length;j++) {
            Map<String, String> map = (Map<String, String>) results[j];

            sb.append("\n");

            for (int i = 0; i < header.size(); i++) {
                String value = map.get(header.get(i));
                if (value == null) {
                    value = "";
                }

                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(value.replace("\"", "\"\"")).append("\"");
            }
        }
        String spid = pid;
        if (spid == null || spid.equals("none")) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), "text/plain", "Phylogenetic_Diversity_" + sdate + "_" + spid + ".csv");

        remoteLogger.logMapAnalysis("Phylogenetic Diversity", "Export - Phylogenetic Diversity", "", "", "", spid, "Phylogenetic_Diversity" + sdate + "_" + spid + ".csv", "");

        detach();
    }
}
