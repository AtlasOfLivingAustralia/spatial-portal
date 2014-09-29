package au.org.ala.spatial.composer.results;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.QueryUtil;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.log4j.Logger;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.*;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author adam
 */
public class SpeciesListResults extends UtilityComposer {
    private static final Logger LOGGER = Logger.getLogger(SpeciesListResults.class);
    private String pid;
    private String[] results;
    private Button download;
    private Listbox popupListboxResults;
    private Label resultsLabel;
    private RemoteLogger remoteLogger;
    private SelectedArea selectedArea;
    private boolean[] geospatialKosher;
    private boolean chooseEndemic;
    //Support for extra filters to be applied - allows facets to be listed
    private String extraParams;
    private boolean addedListener = false;

    @Override
    public void afterCompose() {
        super.afterCompose();
        selectedArea = (SelectedArea) Executions.getCurrent().getArg().get("selectedarea");
        geospatialKosher = (boolean[]) Executions.getCurrent().getArg().get("geospatialKosher");
        chooseEndemic = (Boolean) Executions.getCurrent().getArg().get(StringConstants.CHOOSEENDEMIC);
        extraParams = (String) Executions.getCurrent().getArg().get(StringConstants.EXTRAPARAMS);
        populateList();
    }

    public void populateList() {
        if (selectedArea == null) {
            selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
        }

        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, extraParams, false, geospatialKosher);

            if (sq.getSpeciesCount() <= 0) {
                getMapComposer().showMessage("No species records in the active area.");
                results = null;
                popupListboxResults.setVisible(false);
                resultsLabel.setVisible(false);
                this.detach();
                return;
            }

            //remove header
            String speciesList = chooseEndemic ? sq.endemicSpeciesList() : sq.speciesList();
            results = speciesList.substring(speciesList.indexOf('\n') + 1).split("\n");

            java.util.Arrays.sort(results);

            // results should already be sorted
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                resultsLabel.setValue("preview of first 200 of " + results.length + " species found");
            } else {
                resultsLabel.setValue("preview of all " + results.length + " species found");
            }

            popupListboxResults.setModel(new ListModelArray(tmp, false));
            popupListboxResults.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data, int itemIdx) {
                            String s = (String) data;
                            CSVReader reader = new CSVReader(new StringReader(s));

                            String[] ss;
                            try {
                                ss = reader.readNext();
                            } catch (Exception e) {
                                ss = new String[0];
                            }

                            if (ss == null || ss.length == 0) {
                                return;
                            }

                            Listcell lc = new Listcell(ss[0]);
                            lc.setParent(li);

                            int col = 1;
                            while (col < ss.length) {
                                lc = new Listcell(ss[col]);
                                lc.setParent(li);
                                col++;
                            }

                            try {
                                reader.close();
                            } catch (IOException e) {
                                LOGGER.error("error closing after reading species list", e);
                            }
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("error reading species list data", e);
        }
    }

    public void onClick$btnDownload() {
        StringBuilder sb = new StringBuilder();
        sb.append("LSID,Scientific Name,Taxon Concept,Taxon Rank,Kingdom,Phylum,Class,Order,Family,Genus,Vernacular Name,Number of records\r\n");
        for (String s : results) {
            sb.append(s);
            sb.append("\r\n");
        }

        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), StringConstants.TEXT_PLAIN, "Species_list_" + sdate + "_" + spid + ".csv");

        if (selectedArea == null) {
            selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
        }

        remoteLogger.logMapAnalysis("Species List", "Export - Species List", selectedArea.getWkt(), "", "", spid, "Species_list_" + sdate + "_" + spid + ".csv", "");

        detach();
    }
}
