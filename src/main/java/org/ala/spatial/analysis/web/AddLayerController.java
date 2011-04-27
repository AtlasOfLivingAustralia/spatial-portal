package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.LayerListComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONObject;
import org.ala.spatial.analysis.web.LayersAutoComplete;
import org.ala.spatial.analysis.web.SpeciesAutoComplete;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Radio;

/**
 *
 * @author ajay
 */
public class AddLayerController extends UtilityComposer {
    
    LayersAutoComplete lac;
    String treeName, treePath, treeMetadata;
    int treeSubType;
    String searchName, searchPath, searchMetadata;
    int searchSubType;

    Button btnOk;
    Div divSearch, divTree;
    Radio rSearch, rTree;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //((LayerListComposer)((HtmlMacroComponent)getFellow("layerList")).getFellow("layerswindow")).alc = this;
        rSearch.setChecked(true);
    }

    public void onClick$btnOk(Event event) {
        if(treeName != null) {
            getMapComposer().addWMSLayer(treeName,
                            treePath,
                            (float) 0.75, treeMetadata, treeSubType);

            getMapComposer().updateUserLogMapLayer("env - tree - add", /*joLayer.getString("uid")+*/"|"+treeName);
        } else if(searchName != null) {
            getMapComposer().addWMSLayer(searchName,
                            searchPath,
                            (float) 0.75, searchMetadata, searchSubType);

            getMapComposer().updateUserLogMapLayer("env - search - add", /*joLayer.getString("uid")+*/"|"+searchName);
        }

        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$lac(Event event) {
        searchName = null;
        btnOk.setDisabled(true);
        
        LayerListComposer llc = (LayerListComposer) getFellow("layerList").getFellow("layerswindow");

        if (lac.getItemCount() > 0 && lac.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) lac.getSelectedItem().getValue();
            String metadata = "";

            metadata = CommonData.satServer + "/alaspatial/layers/" + jo.getString("uid");

            setLayer(jo.getString("displayname"), jo.getString("displaypath"), metadata, 
                    jo.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);
        }
//        else {
//            JSONObject joLayer = JSONObject.fromObject(llc.tree.getSelectedItem().getTreerow().getAttribute("lyr"));
//            if (!joLayer.getString("type").contentEquals("class")) {
//
//                String metadata = CommonData.satServer + "/alaspatial/layers/" + joLayer.getString("uid");
//
//                setLayer(joLayer.getString("displayname"), joLayer.getString("displaypath"), metadata,
//                        joLayer.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);
//            } else {
//                String classAttribute = joLayer.getString("classname");
//                String classValue = joLayer.getString("displayname");
//                String layer = joLayer.getString("layername");
//                String displaypath = joLayer.getString("displaypath") + "&cql_filter=(" + classAttribute + "='" + classValue + "');include";
//                //Filtered requests don't work on
//                displaypath = displaypath.replace("gwc/service/", "");
//                // Messagebox.show(displaypath);
//                String metadata = CommonData.satServer + "/alaspatial/layers/" + joLayer.getString("uid");
//
//                setLayer(layer + " - " + classValue, displaypath, metadata,
//                        joLayer.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);
//            }
//
//            //close parent if it is 'addlayerwindow'
//            try {
//                getRoot().getFellow("addlayerwindow").detach();
//            } catch (Exception e) {}
//        }
    }

    public void setLayer(String name, String displaypath, String metadata, int subType) {
        if(rTree.isChecked()) {
            treeName = name;
            treePath = displaypath;
            treeMetadata = metadata;
            treeSubType = subType;

            //fill autocomplete text
            //lac.setText(name);

            //clear selection on tree
            //LayerListComposer llc = (LayerListComposer) getFellow("layerList").getFellow("layerswindow");
            //llc.tree.clearSelection();
        } else {
            searchName = name;
            searchPath = displaypath;
            searchMetadata = metadata;
            searchSubType = subType;
        }

        btnOk.setDisabled(false);
    }

    public void onCheck$rgAddLayer(Event event) {
        divSearch.setVisible(rSearch.isChecked());
        divTree.setVisible(rTree.isChecked());

        btnOk.setDisabled((rTree.isChecked() && treeName == null)
                || (rSearch.isChecked() && searchName == null));
    }

}
