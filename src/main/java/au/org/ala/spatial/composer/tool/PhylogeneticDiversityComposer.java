/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.results.PhylogeneticDiversityListResults;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.menu.MapLayer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ajay
 */
public class PhylogeneticDiversityComposer extends ToolComposer {
    private static Logger logger = Logger.getLogger(PhylogeneticDiversityComposer.class);

    public Object[] trees; //HashMap<String, String>
    ArrayList<String> header;
    public Listbox treesList;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Phylogenetic Diversity";
        this.totalSteps = 2;

        this.loadAreaLayers(true);
        this.updateWindowTitle();

        fillPDTreeList();
    }

    private void fillPDTreeList() {
        JSONObject jo = null;
        String url = CommonData.settings.getProperty(CommonData.PHYLOLIST_URL) + "/phylo/getExpertTrees?noTreeText=true";
        //try n times
        int t = 0;
        int maxTry = 3;
        while (t < maxTry && jo == null) {
            t++;
            try {
                jo = JSONObject.fromObject(Util.readUrl(url));
            } catch (Exception e) {
                //so it fails, that's why trying again
            }
        }
        if (t == maxTry) {
            logger.error("failed to getExpertTrees from url: " + url);
        }

        JSONArray ja = jo.getJSONArray("expertTrees");

        trees = new Object[ja.size()];
        header = new ArrayList<String>();

        //restrict header to what is in the zul
        for(Component c : getFellow("treesHeader").getChildren()) {
            header.add((String)((Listheader)c).getId().substring(3));
        }

        int row = 0;
        for (int i = 0; i < ja.size(); i++) {
            JSONObject j = ja.getJSONObject(i);

            Map<String, String> pdrow = new HashMap<String, String>();

            for(Object o : j.keySet()) {
                String key = (String) o;
                //if (!header.contains(key)) {
                //    header.add(key);
                //}

                pdrow.put(key, j.getString(key));
            }

            trees[row] = pdrow;

            row++;
        }

        //write out header
        /*Component c = getFellow("treesHeader");
        for(int i = 0;i<header.size();i++) {
            Listheader lh = new Listheader();
            lh.setParent(c);
            lh.setLabel(header.get(i));
        }*/

        treesList.setModel(new

                        ListModelArray(trees, false)

        );
        treesList.setItemRenderer(
                new ListitemRenderer() {

                    public void render(Listitem li, Object data, int item_idx) {
                        Map<String, String> map = (Map<String, String>) data;

                        for (int i = 0; i < header.size(); i++) {
                            String value = map.get(header.get(i));
                            if (value == null) {
                                value = "";
                            }

                            if (header.get(i).equalsIgnoreCase("treeViewUrl")) {
                                Html img = new Html("<i class='icon-info-sign'></i>");
                                img.setAttribute("link", value);

                                Listcell lc = new Listcell();
                                lc.setParent(li);
                                img.setParent(lc);
                                img.addEventListener("onClick", new EventListener() {

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

        treesList.setMultiple(true);
    }

    @Override
    public boolean onFinish() {

        List<SelectedArea> sa = getSelectedAreas();
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedareas", sa);

        List<Object> st = new ArrayList<Object>();
        for(Object o : treesList.getSelectedItems()) {
            st.add(trees[((Listitem) o).getIndex()]);
        }

        hm.put("selectedtrees", st);

        PhylogeneticDiversityListResults window = (PhylogeneticDiversityListResults) Executions.createComponents("WEB-INF/zul/results/PhylogeneticDiversityResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening PhylogeneticDiversityResults.zul", e);
        }

        detach();
        return true;
    }

    @Override
    void fixFocus() {
        switch (currentStep) {
            case 1:
                //rgArea.setFocus(true);
                break;
            case 2:
                //exportFormat.setFocus(true);
                break;
        }
    }

    public List<SelectedArea> getSelectedAreas() {
        List<SelectedArea> selectedAreas = new ArrayList<SelectedArea>();

        Vbox vboxArea = (Vbox) getFellowIfAny("vboxArea");

        for(Component c : vboxArea.getChildren()){
            if (((Checkbox)c).isChecked()) {
                SelectedArea sa = null;
                String area = ((Checkbox)c).getValue();
                try {
                    if (area.equals("current")) {
                        sa = new SelectedArea(null, getMapComposer().getViewArea());
                    } else if (area.equals("australia")) {
                        sa = new SelectedArea(null, CommonData.AUSTRALIA_WKT);
                    } else if (area.equals("world")) {
                        sa = new SelectedArea(null, CommonData.WORLD_WKT);
                    } else {
                        List<MapLayer> layers = getMapComposer().getPolygonLayers();
                        for (MapLayer ml : layers) {
                            if (area.equals(ml.getWKT())) {
                                sa = new SelectedArea(ml, null);
                                break;
                            }
                        }

                    }
                } catch (Exception e) {
                    logger.warn("Unable to retrieve selected area", e);
                }
                if(sa != null) {
                    selectedAreas.add(sa);
                }
            }

        }
        return selectedAreas;
    }

    @Override
    public void onClick$btnOk(Event event) {
        if (currentStep == 1 && getSelectedAreas().size() == 0) {
            //must have 1 or more areas selected
            Messagebox.show("Select one or more areas.");
        } else if (currentStep == 2 && treesList.getSelectedItems().size() == 0) {
            //must have 1 or more tree selected
            Messagebox.show("Select one or more Phylogenetic Trees.");
        } else {
            super.onClick$btnOk(event);
        }
    }
}
