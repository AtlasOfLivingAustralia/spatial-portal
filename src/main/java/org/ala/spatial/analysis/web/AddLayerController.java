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
        
        rSearch.setChecked(true);
    }

    public void onClick$btnOk(Event event) {
        if(btnOk.isDisabled()) {
            return;
        }
        if(treeName != null) {
            getMapComposer().addWMSLayer(treeName,
                            treePath,
                            (float) 0.75, treeMetadata, null, treeSubType, null, null);

            getMapComposer().updateUserLogMapLayer("env - tree - add", /*joLayer.getString("uid")+*/"|"+treeName);
        } else if(searchName != null) {
            getMapComposer().addWMSLayer(searchName,
                            searchPath,
                            (float) 0.75, searchMetadata, null, searchSubType, null, null);

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

            metadata = CommonData.satServer + "/layers/" + jo.getString("uid");

            setLayer(jo.getString("displayname"), jo.getString("displaypath"), metadata, 
                    jo.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);
        }
    }

    public void setLayer(String name, String displaypath, String metadata, int subType) {
        if(rTree.isChecked()) {
            treeName = name;
            treePath = displaypath;
            treeMetadata = metadata;
            treeSubType = subType;
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

//        if(rSearch.isChecked()) {
//            lac.setFocus(true);
//        } else {
//            ((LayerListComposer)((HtmlMacroComponent)getFellow("layerList")).getFellow("layerswindow")).setFocus(true);
//        }
    }

}
