/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.results.PhylogeneticDiversityListResults;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
    private static final Logger LOGGER = Logger.getLogger(PhylogeneticDiversityComposer.class);

    private Object[] trees;
    private Listbox treesList;
    private List<String> header;

    @Override
    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Phylogenetic Diversity";
        this.totalSteps = 2;

        this.loadAreaLayers(true);
        this.updateWindowTitle();

        fillPDTreeList();

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                //apply preselected trees
                if (((Map.Entry) o).getKey() instanceof String
                        && "selectedTrees".equals(((Map.Entry) o).getKey())) {
                    String selectedTrees = (String) ((Map.Entry) o).getValue();
                    for (String tree : selectedTrees.split(",")) {
                        for (int i = 0; i < trees.length; i++) {
                            if (((Map<String, String>) trees[i]).containsValue(tree)) {
                                treesList.getItemAtIndex(i).setSelected(true);
                            }
                        }
                    }

                }

                //apply preselected areas
                if (((Map.Entry) o).getKey() instanceof String
                        && "selectedAreas".equals(((Map.Entry) o).getKey())) {
                    String selectedAreas = (String) ((Map.Entry) o).getValue();
                    for (String area : selectedAreas.split(",")) {
                        List checkboxes = getFellow("vboxArea").getChildren();
                        for (int i = 0; i < checkboxes.size(); i++) {
                            if (((Checkbox) checkboxes.get(i)).getLabel().equals(area)) {
                                ((Checkbox) checkboxes.get(i)).setChecked(true);
                            }
                        }
                    }

                }
            }
        }
    }

    private void fillPDTreeList() {
        JSONObject jo = null;
        String url = CommonData.getSettings().getProperty(CommonData.PHYLOLIST_URL) + "/phylo/getExpertTrees?noTreeText=true";
        JSONParser jp = new JSONParser();
        try {
            jo = (JSONObject) jp.parse(Util.readUrl(url));
        } catch (ParseException e) {
            LOGGER.error("failed to parse getExpertTrees");
        }

        JSONArray ja = (JSONArray) jo.get("expertTrees");

        trees = new Object[ja.size()];
        header = new ArrayList<String>();

        //restrict header to what is in the zul
        for (Component c : getFellow(StringConstants.TREES_HEADER).getChildren()) {
            header.add(c.getId().substring(3));
        }

        int row = 0;
        for (int i = 0; i < ja.size(); i++) {
            JSONObject j = (JSONObject) ja.get(i);

            Map<String, String> pdrow = new HashMap<String, String>();

            for (Object o : j.keySet()) {
                String key = (String) o;
                pdrow.put(key, j.get(key).toString());
            }

            trees[row] = pdrow;

            row++;
        }

        treesList.setModel(new

                        ListModelArray(trees, false)

        );
        treesList.setItemRenderer(
                new ListitemRenderer() {

                    public void render(Listitem li, Object data, int itemIdx) {
                        Map<String, String> map = (Map<String, String>) data;

                        for (int i = 0; i < header.size(); i++) {
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
                                img.addEventListener(StringConstants.ONCLICK, new EventListener() {

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
        Map<String, Object> hm = new HashMap<String, Object>();
        hm.put("selectedareas", sa);

        List<Object> st = new ArrayList<Object>();
        for (Object o : treesList.getSelectedItems()) {
            st.add(trees[((Listitem) o).getIndex()]);
        }

        hm.put("selectedtrees", st);

        PhylogeneticDiversityListResults window = (PhylogeneticDiversityListResults) Executions.createComponents("WEB-INF/zul/results/PhylogeneticDiversityResults.zul", getMapComposer(), hm);
        try {
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening PhylogeneticDiversityResults.zul", e);
        }

        detach();
        return true;
    }

    public List<SelectedArea> getSelectedAreas() {
        List<SelectedArea> selectedAreas = new ArrayList<SelectedArea>();

        Vbox vboxArea = (Vbox) getFellowIfAny("vboxArea");

        for (Component c : vboxArea.getChildren()) {
            if (((Checkbox) c).isChecked()) {
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
                            if (area.equals(ml.getWKT())) {
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

    @Override
    public void onClick$btnOk(Event event) {
        if (currentStep == 1 && getSelectedAreas().isEmpty()) {
            //must have 1 or more areas selected
            Messagebox.show("Select one or more areas.");
        } else if (currentStep == 2 && treesList.getSelectedItems().isEmpty()) {
            //must have 1 or more tree selected
            Messagebox.show("Select one or more Phylogenetic Trees.");
        } else {
            super.onClick$btnOk(event);
        }
    }
}
