package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.composer.input.UploadToSpeciesListController;
import au.org.ala.spatial.composer.layer.EnvLayersCombobox;
import au.org.ala.spatial.composer.layer.EnvironmentalList;
import au.org.ala.spatial.composer.layer.SelectedLayersCombobox;
import au.org.ala.spatial.composer.species.SpeciesAutoCompleteComponent;
import au.org.ala.spatial.composer.species.SpeciesListListbox;
import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.Query;
import au.org.ala.spatial.data.UserDataQuery;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerSelection;
import au.org.emii.portal.util.LayerUtilities;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TODO: NC 20130712 - This class really needs to be reviewed when we get a chance.
 *
 * @author ajay
 */
public class ToolComposer extends UtilityComposer {
    private static Logger logger = Logger.getLogger(ToolComposer.class);
    public RemoteLogger remoteLogger;
    int currentStep = 1, totalSteps = 5;
    Map<String, Object> params;
    protected String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea, rgAreaHighlight, rgSpecies, rgSpeciesBk;
    Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    Radio rSpeciesAll, rSpeciesMapped, rSpeciesSearch, rSpeciesUploadLSID, rSpeciesUploadSpecies, rMultiple;
    Radio rSpeciesNoneBk, rSpeciesAllBk, rSpeciesMappedBk, rSpeciesSearchBk, rSpeciesUploadLSIDBk, rSpeciesUploadSpeciesBk, rMultipleBk;
    Radio rAreaWorld, rAreaCustom, rAreaWorldHighlight, rAreaSelected;
    Button btnCancel, btnOk, btnBack, btnHelp;
    Textbox tToolName;
    SpeciesAutoCompleteComponent searchSpeciesACComp, bgSearchSpeciesACComp;
    //SpeciesAutoComplete searchSpeciesAuto, bgSearchSpeciesAuto;
    EnvironmentalList lbListLayers;
    Div divSpeciesSearch, divSpeciesSearchBk;
    EnvLayersCombobox cbLayer1;
    EnvLayersCombobox cbLayer2;
    String winTop = "300px";
    String winLeft = "500px";
    // boolean setCustomArea = false;
    boolean hasCustomArea = false;
    MapLayer prevTopArea = null;
    Fileupload fileUpload;
    SelectedLayersCombobox selectedLayersCombobox;
    Div tlinfo;
    Textbox tLayerList;
    Div dLayerSummary;
    EnvLayersCombobox cbLayer, cbLayerEnvironmentalOnly, cbLayerMix;
    Button bLayerListDownload1;
    Button bLayerListDownload2;
    Label lLayersSelected, lendemicNote;
    Button btnClearSelection;
    Menupopup mpLayer2, mpLayer1;
    Doublebox dResolution;
    Vbox vboxMultiple, vboxMultipleBk;
    SpeciesAutoCompleteComponent mSearchSpeciesACComp, mSearchSpeciesACCompBk;
    //SpeciesAutoComplete mSearchSpeciesAuto, mSearchSpeciesAutoBk;
    Textbox tMultiple, tMultipleBk;
    Listbox lMultiple, lMultipleBk;
    boolean includeAnalysisLayers = true;
    boolean includeContextualLayers = false;
    boolean singleLayerDomain = true;
    boolean fullList = false;
    boolean includeAnalysisLayersForUploadQuery = false;
    boolean includeAnalysisLayersForAnyQuery = false;
    boolean mpLayersIncludeAnalysisLayers = false;
    Checkbox chkGeoKosherTrue, chkGeoKosherFalse, chkEndemicSpecies;
    Checkbox chkGeoKosherTrueBk, chkGeoKosherFalseBk;
    boolean[] defaultGeospatialKosher = {true, true, false};

    private Label lEstimateMessage;
    private Button bLogin;
    private Div notLoggedIn;
    private Div isLoggedIn;
    boolean isBackgroundProcess = true;
    boolean hasEstimated = false;

    //stuff for the dynamic species list inclusion
    private Vbox vboxImportSL, vboxImportSLBk; //the box layout for the import species list
    private SpeciesListListbox speciesListListbox, speciesListListboxBk;

    @Override
    public void afterCompose() {
        super.afterCompose();

        winTop = this.getTop();
        winLeft = this.getLeft();

        setupDefaultParams();

        HashMap<String, Object> tmp = new HashMap<String, Object>();
        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object k : m.keySet()) {
                if (k instanceof String) {
                    tmp.put((String) k, m.get(k));
                }
            }
        }
        setParams(tmp);

        //add the species lists stuff
        if (rSpeciesUploadLSID != null && this.hasFellow("splistbox")) {
            vboxImportSL = (Vbox) this.getFellow("splistbox").getFellow("vboxImportSL");
            speciesListListbox = (SpeciesListListbox) this.getFellow("splistbox").getFellow("speciesListListbox");
            speciesListListbox.addEventListener("onSlCheckBoxChanged", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    btnOk.setDisabled((Integer) event.getData() == 0);
                }
            });
        }

        if (rSpeciesUploadLSIDBk != null && this.hasFellow("splistboxbk")) {
            vboxImportSLBk = (Vbox) this.getFellow("splistboxbk").getFellow("vboxImportSL");
            speciesListListboxBk = (SpeciesListListbox) this.getFellow("splistboxbk").getFellow("speciesListListbox");
            speciesListListbox.addEventListener("onSlCheckBoxChanged", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    btnOk.setDisabled((Integer) event.getData() == 0);
                }
            });
        }

        updateWindowTitle();

        fixFocus();

        if (lbListLayers != null) {
            lbListLayers.clearSelection();
            lbListLayers.updateDistances();
        }

        // init mpLayer1 and mpLayer2
        if (mpLayer1 != null && mpLayer2 != null) {
            for (MapLayer ml : getMapComposer().getGridLayers()) {
                addToMpLayers(ml, false);
            }
        }

        if (mSearchSpeciesACComp != null) {
            mSearchSpeciesACComp.getAutoComplete().setBiocacheOnly(true);
        }
        if (mSearchSpeciesACCompBk != null) {
            mSearchSpeciesACCompBk.getAutoComplete().setBiocacheOnly(true);
        }

        // init includeLayers
        if (cbLayer != null) {
            cbLayer.setIncludeLayers("AllLayers");
            cbLayer.refresh("");
        }
        if (cbLayerEnvironmentalOnly != null) {
            cbLayerEnvironmentalOnly.setIncludeLayers("EnvironmentalLayers");
            cbLayerEnvironmentalOnly.refresh("");
        }
        if (cbLayer1 != null) {
            cbLayer1.setIncludeLayers("EnvironmentalLayers");
            cbLayer1.refresh("");
        }
        if (cbLayer2 != null) {
            cbLayer2.setIncludeLayers("EnvironmentalLayers");
            cbLayer2.refresh("");
        }
        if (cbLayerMix != null) {
            cbLayerMix.setIncludeLayers("MixLayers");
            cbLayerMix.refresh("");
        }

        updateDefaultGeospatialKosherValues();
    }

    void updateDefaultGeospatialKosherValues() {
        if (chkGeoKosherTrue != null) {
            defaultGeospatialKosher[0] = chkGeoKosherTrue.isChecked();
        }
        if (chkGeoKosherFalse != null) {
            defaultGeospatialKosher[1] = chkGeoKosherFalse.isChecked();
        }

    }

    void addToMpLayers(MapLayer ml, boolean analysis) {
        // get layer name
        String name = null;
        String url = ml.getUri();
        if (analysis) {
            name = ml.getName();
        } else {
            int p1 = url.indexOf("ALA:") + 4;
            int p2 = url.indexOf("&", p1);
            if (p1 > 4) {
                if (p2 < 0) {
                    p2 = url.length();
                }
                name = url.substring(p1, p2);
            }
        }

        // cbLayer1
        Menuitem mi = new Menuitem(ml.getDisplayName());
        mi.setValue(name);
        mi.addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer1.setValue(mi.getValue() + " ");
                cbLayer1.refresh(mi.getValue());
                for (Object o : cbLayer1.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = ci.getValue();
                    if (jo.getString("name").equals(mi.getValue())) {
                        cbLayer1.setSelectedItem(ci);
                        cbLayer1.setText(ci.getLabel());
                        toggles();
                        return;
                    }
                }
            }
        });
        mi.setParent(mpLayer1);

        // cbLayer2
        mi = new Menuitem(ml.getDisplayName());
        mi.setValue(name);
        mi.addEventListener("onClick", new EventListener() {

            public void onEvent(Event event) throws Exception {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer2.setValue(mi.getValue() + " ");
                cbLayer2.refresh(mi.getValue());
                for (Object o : cbLayer2.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = ci.getValue();
                    if (jo.getString("name").equals(mi.getValue())) {
                        cbLayer2.setSelectedItem(ci);
                        cbLayer2.setText(ci.getLabel());
                        toggles();
                        return;
                    }
                }
            }
        });
        mi.setParent(mpLayer2);
    }

    private void setupDefaultParams() {
        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        btnOk.setLabel("Next >");
    }

    public void updateWindowTitle() {
        if (this.getFellowIfAny("cTitle") != null) {
            ((Caption) this.getFellowIfAny("cTitle")).setLabel("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
        }
    }

    public void updateName(String name) {
        if (tToolName != null) {
            tToolName.setValue(name);
        }
    }

    private void loadSummaryDetails() {
        try {
            Div atsummary = (Div) getFellowIfAny("atsummary");
            if (atsummary != null) {
                String summary = "";
                summary += "<strong>Analytical tool</strong>: " + selectedMethod;
                summary += "<strong>Area</strong>: ";
                summary += "<strong>Species</strong>: ";
                summary += "<strong>Grids</strong>: ";
                summary += "<strong>Additional options</strong>: ";
                atsummary.setContext(summary);
            }
        } catch (Exception e) {
        }
    }

    public void setParams(Map<String, Object> params) {
        if (this.params == null) {
            this.params = new HashMap<String, Object>();
        }
        if (params != null) {
            this.params.putAll(params);
        }
    }

    public void loadSpeciesLayers() {
        loadSpeciesLayers(false);
    }

    public void loadSpeciesLayers(boolean biocacheOnly) {
        try {

            Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpecies");
            Radio rSpeciesMapped = (Radio) getFellowIfAny("rSpeciesMapped");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

            Radio selectedSpecies = null;
            String selectedSpeciesLayer = (String) params.get("speciesLayerName");
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (biocacheOnly && lyr.getSpeciesQuery() != null && !(lyr.getSpeciesQuery() instanceof BiocacheQuery)) {
                    continue;
                }
                if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getName().replaceAll(" ", ""));
                rgSpecies.insertBefore(rSp, rSpeciesMapped);

                if (selectedSpeciesLayer != null && rSp.getValue().equals(selectedSpeciesLayer)) {
                    selectedSpecies = rSp;
                }
            }

            if (speciesLayersCount > 1) {
                rSpeciesMapped.setLabel("All " + speciesLayersCount + " species currently mapped (excludes coordinate uploads)");
            } else {
                rSpeciesMapped.setVisible(false);
            }

            if (selectedSpecies != null) {
                rgSpecies.setSelectedItem(selectedSpecies);
            } else if (selectedSpeciesLayer != null && selectedSpeciesLayer.equals("none")) {
                rgSpecies.setSelectedItem(rSpeciesAll);
            } else if (layers.size() > 0) {
                rgSpecies.setSelectedItem(rgSpecies.getItemAtIndex(1));
            } else {
                for (int i = 0; i < rgSpecies.getItemCount(); i++) {
                    if (rgSpecies.getItemAtIndex(i).isVisible() && rgSpecies.getItemAtIndex(i) != rSpeciesAll) {
                        rgSpecies.setSelectedItem(rgSpecies.getItemAtIndex(i));
                        break;
                    }
                }
            }

            updateGeospatialKosherCheckboxes();
        } catch (Exception e) {
            logger.error("Unable to load species layers:", e);
            //e.printStackTrace(System.out);
        }
    }

    public void loadSpeciesLayersBk() {
        try {
            Radiogroup rgSpecies = (Radiogroup) getFellowIfAny("rgSpeciesBk");

            List<MapLayer> layers = getMapComposer().getSpeciesLayers();
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getName().replaceAll(" ", "") + "__bk");
                rgSpecies.insertBefore(rSp, rSpeciesMappedBk);
            }

            if (speciesLayersCount > 1) {
                rSpeciesMappedBk.setLabel("All " + speciesLayersCount + " species currently mapped (excludes coordinate uploads)");
            } else {
                rSpeciesMappedBk.setVisible(false);
            }

            updateGeospatialKosherCheckboxesBk();
        } catch (Exception e) {
            logger.error("Unable to load species layers:", e);
        }
    }

    public void loadAreaLayers() {
        loadAreaLayers(null);
    }

    public void loadAreaLayers(String selectedAreaName) {
        try {
            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgArea");
            // remove all radio buttons that don't have an id
            for (int i = rgArea.getItemCount() - 1; i >= 0; i--) {
                String id = rgArea.getItems().get(i).getId();
                if (id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getWKT());

                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        logger.debug("2.resetting indexToSelect = " + i);
                        rgArea.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rAreaSelected = rSelectedLayer;
                rgArea.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArea.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
                rgArea.setSelectedItem(rAreaSelected);
            } else {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            }

            if (chkEndemicSpecies != null) {
                updateEndemicCheckBox();
            }

            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void loadAreaHighlightLayers(String selectedAreaName) {
        try {
            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            // remove all radio buttons that don't have an id
            for (int i = rgArea.getItemCount() - 1; i >= 0; i--) {
                String id = rgArea.getItems().get(i).getId();
                if (id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            Radio rAreaCurrentHighlight = (Radio) getFellowIfAny("rAreaCurrentHighlight");

            String selectedLayerName = (String) params.get("polygonLayerName");
            Radio rSelectedLayer = null;

            int count_not_envelopes = 0;
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getWKT());

                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrentHighlight);

                if (selectedLayerName != null && lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    // rAreaSelected = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.equals("")) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        logger.debug("2.resetting indexToSelect = " + i);
                        rgArea.setSelectedItem(rgArea.getItemAtIndex(i));
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rgArea.setSelectedItem(rAreaSelected);
            } else if (selectedLayerName != null && selectedLayerName.equals("none")) {
                rgArea.setSelectedItem(rAreaWorld);
            } else {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        // rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rgArea.getItemAtIndex(i));
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rgArea.getSelectedItem().getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void loadAreaLayersHighlight() {
        try {

            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgAreaHighlight");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrentHighlight");
            Radio rAreaNone = (Radio) getFellowIfAny("rAreaNoneHighlight");

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            rAreaNone.setSelected(true);
        } catch (Exception e) {
            logger.error("Unable to load active area layers:", e);
        }
    }

    public void loadGridLayers(boolean includeAnalysisLayers, boolean includeContextualLayers, boolean singleLayerDomain) {
        this.includeAnalysisLayers = includeAnalysisLayers;
        this.includeContextualLayers = includeContextualLayers;
        this.singleLayerDomain = singleLayerDomain;
        this.fullList = true;

        if (selectedLayersCombobox != null) {
            selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer(), false);
        }
        try {

            if (fullList) {
                lbListLayers.init(getMapComposer(), includeAnalysisLayers, !includeContextualLayers, singleLayerDomain);
                lbListLayers.updateDistances();
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (int i = 0; i < layers.size(); i++) {
                    MapLayer lyr = layers.get(i);
                    // logger.debug(lyr.getDisplayName());
                }
            }

            String layers = (String) params.get("environmentalLayerName");
            if (layers != null) {
                lbListLayers.selectLayers(layers.split(","));
            }

            lbListLayers.renderAll();
        } catch (Exception e) {
            logger.error("Unable to load species layers:", e);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        // setCustomArea = false;
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        if (rAreaSelected == rAreaCustom) {
            // setCustomArea = true;
            hasCustomArea = false;
        }


        // case for enabling the endemic checkbox
        // logger.debug("RADIO SELECTED = " + rAreaSelected.getValue());

        if (chkEndemicSpecies != null) {
            updateEndemicCheckBox();
        }
    }

    private void updateEndemicCheckBox() {
        // check to see if the area is within the required size...
        boolean showEndemic = false;
        String value = rAreaSelected.getValue();
        if (rAreaSelected != null) {
            if (value.equals("australia") || value.equals("world") || value.equals("custom")) {
                // logger.debug("Large areas");

            } else {

                String areaName = rAreaSelected.getLabel();
                MapLayer ml = getMapComposer().getMapLayer(areaName);
                String sarea = "";
                if (value.equals("current")) {
                    // check to see if the current extent is within the maximum
                    // area
                    SelectedArea sa = getSelectedArea();
                    sarea = sa.getKm2Area();
                } else if (ml != null) {
                    sarea = ml.getAreaSqKm();
                    if (sarea == null)
                        sarea = ml.calculateAndStoreArea();
                }
                try {
                    Float area = Float.parseFloat(sarea.replaceAll(",", ""));
                    showEndemic = (area <= CommonData.maxEndemicArea);
                } catch (NumberFormatException e) {

                }

            }
        }
        chkEndemicSpecies.setDisabled(!showEndemic);
        chkEndemicSpecies.setChecked(false);

        if (lendemicNote != null) {
            if (showEndemic) {
                lendemicNote.setValue("Please note this may take several minutes depending on the area selected.");
            } else {
                lendemicNote.setValue("The selected area is too large to be considered for endemic species.");
            }
        }
    }

    public void onCheck$rgAreaHighlight(Event event) {
        if (rgAreaHighlight == null) {
            return;
        }
        // setCustomArea = false;
        hasCustomArea = false;
        if (rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight")) {
            // setCustomArea = true;
            hasCustomArea = false;
        }
    }

    public void onCheck$rgSpecies(Event event) {
        if (rgSpecies == null) {
            return;
        }
        Radio selectedItem = rgSpecies.getSelectedItem();
        try {
            selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        try {
            if (event != null && selectedItem != rSpeciesUploadLSID) {
                vboxImportSL.setVisible(false);
            }
            if (event != null && vboxImportSLBk != null && selectedItem != rSpeciesUploadLSIDBk) {
                vboxImportSLBk.setVisible(false);
            }
            // Check to see if we are perform a normal or background upload
            if (rgSpecies != null && selectedItem == rSpeciesSearch) {
                if (divSpeciesSearch != null) {
                    divSpeciesSearch.setVisible(true);
                    vboxMultiple.setVisible(false);
                    if (event != null) {
                        toggles();
                    }
                    return;
                }
            }
            if (divSpeciesSearch != null) {
                divSpeciesSearch.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadSpecies) {
                // btnOk.setVisible(false);
                // fileUpload.setVisible(true);                
                btnOk.setVisible(true);
                vboxMultiple.setVisible(false);
            } else if (selectedItem == rSpeciesUploadLSID) {
                btnOk.setDisabled(true);
                vboxImportSL.setVisible(true);
                vboxMultiple.setVisible(false);
            } else if (rMultiple != null && rMultiple.isSelected()) {

                vboxMultiple.setVisible(true);
            } else {
                btnOk.setDisabled(false);
                vboxMultiple.setVisible(false);
            }

            if (event != null) {
                toggles();
            }

            // set default geospatial kosher checkboxes unless a map layers has
            // been chosen
            MapLayer ml = null;
            Query q = null;
            if (rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue() != null
                    && (ml = getMapComposer().getMapLayer(rgSpecies.getSelectedItem().getLabel())) != null
                    && (q = ml.getSpeciesQuery()) != null && q instanceof BiocacheQuery) {
                setGeospatialKosherCheckboxes(((BiocacheQuery) q).getGeospatialKosher());
            } else {
                setGeospatialKosherCheckboxes(defaultGeospatialKosher);
            }
            updateGeospatialKosherCheckboxes();
        } catch (Exception e) {
        }
    }

    public void onCheck$rgSpeciesBk(Event event) {
        if (rgSpeciesBk == null) {
            return;
        }
        Radio selectedItem = rgSpeciesBk.getSelectedItem();
        try {
            selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
        }
        try {

            if (event != null && selectedItem != rSpeciesUploadLSID) {
                vboxImportSL.setVisible(false);
            }
            if (event != null && vboxImportSLBk != null && selectedItem != rSpeciesUploadLSIDBk) {
                vboxImportSLBk.setVisible(false);
            }

            if (rgSpeciesBk != null && selectedItem == rSpeciesSearchBk) {
                if (divSpeciesSearchBk != null) {
                    divSpeciesSearchBk.setVisible(true);
                    vboxMultipleBk.setVisible(false);
                    if (event != null) {
                        toggles();
                    }
                    return;
                }
            }

            if (divSpeciesSearchBk != null) {
                divSpeciesSearchBk.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadLSIDBk) {
                btnOk.setDisabled(true);
                vboxImportSLBk.setVisible(true);
                vboxMultiple.setVisible(false);
            } else if (selectedItem == rSpeciesUploadSpeciesBk) {
                // btnOk.setVisible(false);
                // fileUpload.setVisible(true);
                btnOk.setVisible(true);
            }

            if (rMultipleBk != null && rMultipleBk.isSelected()) {
                vboxMultipleBk.setVisible(true);
            } else {
                vboxMultipleBk.setVisible(false);
            }

            if (event != null) {
                toggles();
            }

            // set default geospatial kosher checkboxes unless a map layers has
            // been chosen
            MapLayer ml = null;
            Query q = null;
            if (rgSpeciesBk.getSelectedItem() != null && rgSpeciesBk.getSelectedItem().getValue() != null
                    && (ml = getMapComposer().getMapLayer(rgSpeciesBk.getSelectedItem().getLabel())) != null
                    && (q = ml.getSpeciesQuery()) != null && q instanceof BiocacheQuery) {
                setGeospatialKosherCheckboxesBk(((BiocacheQuery) q).getGeospatialKosher());
            } else {
                setGeospatialKosherCheckboxesBk(defaultGeospatialKosher);
            }
            updateGeospatialKosherCheckboxes();
            updateGeospatialKosherCheckboxesBk();
        } catch (Exception e) {
        }
    }

    public void onValueSelected$searchSpeciesACComp(Event event) {
        toggles();
    }
//DELETE    
//    public void onSelect$searchSpeciesAuto(Event event) {
//        toggles();
//    }

    public void onClick$btnHelp(Event event) {
        String helpurl = "";

        if (selectedMethod.equals("Prediction")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-prediction-tab/";
        } else if (selectedMethod.equals("Sampling")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-sampling-tab/";
        } else if (selectedMethod.equals("Classification")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-classification-tab/";
        } else if (selectedMethod.equals("Scatterplot")) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/scatterplot-tab/";
        }

        if (StringUtils.isNotBlank(helpurl)) {
            getMapComposer().activateLink(helpurl, "Help", false, "");
        }
    }

    public void onClick$btnCancel(Event event) {
        currentStep = 1;
        if (lbListLayers != null) {
            lbListLayers.clearSelection();
            toggles();
        }
        this.detach();
    }

    public void onClick$btnBack(Event event) {

        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
        Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep - 1))) : null;

        if (currentDiv.getZclass().contains("first")) {
            if (btnBack != null) btnBack.setDisabled(true);
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);

            Image currentStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep - 1));
            currentStepCompletedImg.setVisible(false);

            Label nextStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
            nextStepLabel.setStyle("font-weight:normal");

            Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep - 1));
            currentStepLabel.setStyle("font-weight:bold");

            currentStep--;

            if (previousDiv != null) {
                if (btnBack != null) btnBack.setDisabled(((previousDiv.getZclass().contains("first"))));
            }
        }

        btnOk.setLabel("Next >");
        toggles();
        updateWindowTitle();
        displayTrafficLightInfo();

    }

    private void displayTrafficLightInfo() {
        if (tlinfo != null) {
            if (selectedMethod.equalsIgnoreCase("Prediction") && currentStep == 3) {
                tlinfo.setVisible(true);
            } else if (selectedMethod.equalsIgnoreCase("Classification") && currentStep == 2) {
                tlinfo.setVisible(true);
            } else if (selectedMethod.equalsIgnoreCase("GDM") && currentStep == 3) {
                tlinfo.setVisible(true);
            } else if (selectedMethod.equalsIgnoreCase("Sampling") && currentStep == 3) {
                tlinfo.setVisible(true);
            } else {
                tlinfo.setVisible(false);
            }
        }
    }

    public void resetWindowFromSpeciesUpload(String lsid, String type) {
        try {
            if (type.compareTo("cancel") == 0) {
                fixFocus();
                return;
            }
            if (type.compareTo("normal") == 0) {
                setLsid(lsid);
            }
            if (type.compareTo("bk") == 0) {
                setLsidBk(lsid);
            }
            if (type.compareTo("assemblage") == 0) {
                setMultipleLsids(lsid);
            }
            if (type.compareTo("assemblagebk") == 0) {
                setMultipleLsidsBk(lsid);
            }
        } catch (Exception e) {
            logger.error("Exception when resetting analysis window", e);
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            if (selectedArea == null || selectedArea.trim().equals("")) {
                hasCustomArea = false;
            } else {
                hasCustomArea = true;
            }

            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea = null;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    curTopArea = layers.get(0);
                } else {
                    curTopArea = null;
                }

                if (curTopArea != prevTopArea) {
                    if (isAreaHighlightTab()) {
                        loadAreaHighlightLayers(curTopArea.getDisplayName());
                    } else if (isAreaTab()) {
                        loadAreaLayers(curTopArea.getDisplayName());
                    }

                    ok = true;
                }
            }
            this.setTop(winTop);
            this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                onClick$btnOk(null);
                hasCustomArea = false;
            }

            fixFocus();
        } catch (Exception ex) {
            logger.error("Exception when resetting analysis window", ex);
        }
    }

    public void onClick$btnOk(Event event) {
        if (btnOk.isDisabled()) {
            return;
        }
        boolean successful = false;
        try {

            if (!hasCustomArea && (isAreaCustom() || isAreaHighlightCustom())) {
                this.doOverlapped();
                this.setTop("-9999px");
                this.setLeft("-9999px");

                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "Tool");
                winProps.put("selectedMethod", selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
            Div nextDiv = (Div) getFellowIfAny("atstep" + (currentStep + 1));
            Div previousDiv = (currentStep > 1) ? ((Div) getFellowIfAny("atstep" + (currentStep + 1))) : null;

            //check to see if one of the "create new species list" radio buttons is seleceted
            //ONLY perform these checks if the "lMultiple" or lMultipleBk" is on the current page
            if (Components.isAncestor(currentDiv, lMultiple) && rMultiple.isChecked()) {
                // display the dialog and change the radio button to "use existing"...
                showExportSpeciesListDialog(lMultiple);
                return;
            } else if (Components.isAncestor(currentDiv, lMultipleBk) && rMultipleBk.isChecked()) {
                showExportSpeciesListDialog(lMultipleBk);
                return;
            }

            if (!currentDiv.getZclass().contains("last")) {
                if (currentDiv.getZclass().contains("species") && (rSpeciesUploadSpecies != null && rSpeciesUploadSpecies.isSelected())) {
                    Boolean test = currentDiv.getZclass().contains("species") && (rSpeciesUploadSpecies.isSelected());
                    logger.debug("test=" + test);
                    onClick$btnUpload(event);

                } else {
                    currentDiv.setVisible(false);
                    nextDiv.setVisible(true);

                    Image previousStepCompletedImg = (Image) getFellowIfAny("imgCompletedStep" + (currentStep));
                    previousStepCompletedImg.setVisible(true);

                    Label previousStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep));
                    previousStepLabel.setStyle("font-weight:normal");

                    Label currentStepLabel = (Label) getFellowIfAny("lblStep" + (currentStep + 1));
                    currentStepLabel.setStyle("font-weight:bold");

                    // now include the extra options for step 4
                    if (nextDiv != null) {

                        if (nextDiv.getZclass().contains("last")) {
                            loadSummaryDetails();
                            onLastPanel();
                        }
                        btnOk.setLabel("Next >");
                    }

                    currentStep++;

                    successful = true;
                }
            } else {
                saveLayerSelection();

                successful = onFinish();

                if (successful) {
                    currentStep = 1;
                }
            }

            if (successful) {
                if (nextDiv != null && rgSpecies != null && (includeAnalysisLayersForUploadQuery || includeAnalysisLayersForAnyQuery)) {
                    Query q = getSelectedSpecies();
                    if (q != null) {
                        boolean test = (includeAnalysisLayersForAnyQuery || (q instanceof UserDataQuery));

                        if (selectedLayersCombobox != null) {
                            if ((selectedLayersCombobox.getIncludeAnalysisLayers()) != test) {
                                selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer(), test);
                            }
                        }
                        if (lbListLayers != null) {
                            if ((lbListLayers.getIncludeAnalysisLayers()) != test) {
                                String[] selectedLayers = lbListLayers.getSelectedLayers();
                                lbListLayers.init(getMapComposer(), test, !includeContextualLayers, singleLayerDomain);
                                lbListLayers.updateDistances();

                                if (selectedLayers != null && selectedLayers.length > 0) {
                                    lbListLayers.selectLayers(selectedLayers);
                                }

                                lbListLayers.renderAll();
                            }
                        }
                        if (cbLayer != null) {
                            if ((cbLayer.getIncludeAnalysisLayers()) != test) {
                                cbLayer.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayerEnvironmentalOnly != null) {
                            if ((cbLayerEnvironmentalOnly.getIncludeAnalysisLayers()) != test) {
                                cbLayerEnvironmentalOnly.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayer1 != null) {
                            if ((cbLayer1.getIncludeAnalysisLayers()) != test) {
                                cbLayer1.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayer2 != null) {
                            if ((cbLayer2.getIncludeAnalysisLayers()) != test) {
                                cbLayer2.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (cbLayerMix != null) {
                            if ((cbLayerMix.getIncludeAnalysisLayers()) != test) {
                                cbLayerMix.setIncludeAnalysisLayers(test);
                            }
                        }
                        if (mpLayer1 != null && mpLayer2 != null && mpLayersIncludeAnalysisLayers != test) {
                            // remove
                            while (mpLayer1.getChildren().size() > 0) {
                                mpLayer1.removeChild(mpLayer1.getFirstChild());
                            }
                            while (mpLayer2.getChildren().size() > 0) {
                                mpLayer2.removeChild(mpLayer2.getFirstChild());
                            }
                            // add
                            for (MapLayer ml : getMapComposer().getGridLayers()) {
                                addToMpLayers(ml, false);
                            }
                            mpLayersIncludeAnalysisLayers = test;
                            if (mpLayersIncludeAnalysisLayers) {
                                for (MapLayer ml : getMapComposer().getAnalysisLayers()) {
                                    if (ml.getSubType() != LayerUtilities.ALOC) {
                                        addToMpLayers(ml, true);
                                    }
                                }
                            }
                        }
                    }
                }
                if (nextDiv != null && nextDiv.getZclass().contains("last")) {
                    updateLayerListText();
                }

                if (btnBack != null) {
                    btnBack.setDisabled(false);
                }
                updateWindowTitle();
            }

        } catch (Exception e) {
            logger.error("error progressing to next screen of tool", e);
        }

        toggles();
        displayTrafficLightInfo();

        fixFocus();
    }

    void fixFocus() {
        // set element of focus
    }

    public void onLastPanel() {
    }

    public boolean onFinish() {
        try {
            this.detach();
            Messagebox.show("Running your analysis tool: " + selectedMethod);

        } catch (Exception e) {
            logger.error("errro running analysis tool: " + selectedMethod, e);
        }

        return true;
    }

    public void loadMap(Event event) {
    }

    public long getEstimate() {
        return -1;
    }

    public void onClick$bLogin(Event event) {
        getMapComposer().activateLink("https://auth.ala.org.au/cas/login", "Login", false);

        // Window extWin = (Window) getFellowIfAny("externalContentWindow");
        // extWin.addEventListener("onClose", new EventListener() {
        // @Override
        // public void onEvent(Event event) throws Exception {
        // checkEstimate(null);
        // }
        // });
    }

    public void checkEstimate() {
        try {

            long estimate = getEstimate();
            double minutes = estimate / 60000.0;
            String estimateInMin = "";
            if (minutes < 0.5) {
                estimateInMin = "< 1 minute";
            } else {
                estimateInMin = String.valueOf((int) Math.ceil(minutes)) + " minutes";
            }

            logger.debug("Got estimate for process: " + estimate);
            logger.debug("Estimate in minutes: " + estimateInMin);

            // String message = "Your process is going to take approximately " +
            // estimateInMin + " minutes. ";
            lEstimateMessage.setValue(estimateInMin);

            hasEstimated = true;
            isBackgroundProcess = true;
            if (estimate > 300000) {
                if (!isUserLoggedIn()) {

                    notLoggedIn.setVisible(true);
                    isLoggedIn.setVisible(false);
                    // btnOk.setDisabled(true);
                    hasEstimated = false;

                    return;
                }
                // isBackgroundProcess = true;
            } else {
                // isBackgroundProcess = false;
            }

            notLoggedIn.setVisible(false);
            isLoggedIn.setVisible(true);
            // btnOk.setDisabled(false);

        } catch (Exception e) {
            logger.warn("Unable to get estimate for the process", e);
        }
    }

    public boolean isUserLoggedIn() {
        String authCookie = Util.getUserEmail();
        return (authCookie != null);
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();
        SelectedArea sa = null;
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

        return sa;
    }

    public SelectedArea getSelectedAreaHighlight() {
        String area = rgAreaHighlight.getSelectedItem().getValue();

        SelectedArea sa = null;
        try {
            if (area.equals("none")) {
            } else if (area.equals("current")) {
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

        return sa;
    }

    public Query getSelectedSpecies() {
        return getSelectedSpecies(false, true);
    }

    public Query getSelectedSpecies(boolean mapspecies, boolean applycheckboxes) {
        Query q = null;

        String species = rgSpecies.getSelectedItem().getValue();
        String id = rgSpecies.getSelectedItem().getId();
        logger.debug("getSelectedSpecies.species: " + species);

        MapLayer ml = getMapComposer().getMapLayer(species);
        if (ml != null) {
            q = ml.getSpeciesQuery();
            if (q instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) q;
                q = bq.newFacetGeospatialKosher(applycheckboxes ? getGeospatialKosher() : null, false);
            }
        } else {
            try {
                logger.debug("getSelectedSpecies: " + species);
                logger.debug("tool is: " + (tToolName == null ? "null" : tToolName.getValue()));
                if (species.equals("allspecies")) {
                    species = "none";
                    q = new BiocacheQuery(null, null, null, null, false, applycheckboxes ? getGeospatialKosher() : null);
                } else if (ml == null && species.equals("allmapped")) {

                    // species = "";
                    // List<MapLayer> layers =
                    // getMapComposer().getSpeciesLayers();
                    //
                    // BiocacheQuery sq = new BiocacheQuery();
                    // for (int i = 0; i < layers.size(); i++) {
                    // MapLayer lyr = layers.get(i);
                    // if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    // sq.addLsid(lyr.getMapLayerMetadata().getSpeciesLsid());
                    // }
                    // }
                    //
                    // species = sq.getShortQuery();
                    throw new UnsupportedOperationException("Not yet implemented");
                } else if (species.equals("multiple")) {
                    String lsids = getMultipleLsids();
                    logger.debug("getSelectedSpecies.lsids: " + lsids);
                    if (lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids, null, null, null, false, applycheckboxes ? getGeospatialKosher() : null);
                        logger.debug("getSelectedSpecies.query is now set");
                    }
                } else if (species.equals("uploadLsid")) {
                    //get the query from species list list
                    SpeciesListListbox lb = id.endsWith("Bk") ? speciesListListboxBk : speciesListListbox;
                    q = lb.extractQueryFromSelectedLists(applycheckboxes ? getGeospatialKosher() : null);

                } else if (species.equals("search") || species.equals("uploadSpecies")) {
                    if (searchSpeciesACComp.hasValidItemSelected()) {
                        //if (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0) {
                        if (!searchSpeciesACComp.hasValidAnnotatedItemSelected()) {
                            logger.debug("error in getSelectedSpecies value=" + searchSpeciesACComp.getAutoComplete().getSelectedItem().getValue() + " text=" + searchSpeciesACComp.getAutoComplete().getText());
                        } else {
                            species = searchSpeciesACComp.getAutoComplete().getSelectedItem().getAnnotatedProperties().get(0);
                            q = searchSpeciesACComp.getQuery(getMapComposer(), false, applycheckboxes ? getGeospatialKosher() : null);//QueryUtil.get(species, getMapComposer(), false, applycheckboxes ? getGeospatialKosher() : null);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to retrieve selected species", e);
            }
        }

        return q;
    }

    public Query getSelectedSpeciesBk() {
        return getSelectedSpeciesBk(false, true);
    }

    public Query getSelectedSpeciesBk(boolean mapspecies, boolean applycheckboxes) {
        Query q = null;

        String species = rgSpeciesBk.getSelectedItem().getValue();
        String id = rgSpeciesBk.getSelectedItem().getId();

        MapLayer ml = getMapComposer().getMapLayer(species);
        if (ml != null) {
            q = ml.getSpeciesQuery();
            if (q instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) q;
                q = bq.newFacetGeospatialKosher(applycheckboxes ? getGeospatialKosherBk() : null, false);
            }
        } else {
            try {
                if (species.equals("none")) {
                    species = null;
                } else if (species.equals("allspecies")) {
                    species = "none";
                    q = new BiocacheQuery(null, null, null, null, false, applycheckboxes ? getGeospatialKosherBk() : null);
                } else if (species.equals("allmapped")) {
                    // species = "";
                    // List<MapLayer> layers =
                    // getMapComposer().getSpeciesLayers();
                    //
                    // BiocacheQuery sq = new BiocacheQuery();
                    // for (int i = 0; i < layers.size(); i++) {
                    // MapLayer lyr = layers.get(i);
                    // if (lyr.getSubType() != LayerUtilities.SPECIES_UPLOAD) {
                    // sq.addLsid(lyr.getMapLayerMetadata().getSpeciesLsid());
                    // }
                    // }
                    //
                    // species = sq.getShortQuery();
                    throw new UnsupportedOperationException("Not yet implemented");
                } else if (species.equals("multiple")) {
                    String lsids = getMultipleLsidsBk();
                    if (lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids, null, null, null, false, applycheckboxes ? getGeospatialKosherBk() : null);
                    }
                } else if (species.equals("uploadLsid")) {
                    //get the query from species list list
                    SpeciesListListbox lb = id.endsWith("Bk") ? speciesListListboxBk : speciesListListbox;
                    q = lb.extractQueryFromSelectedLists(applycheckboxes ? getGeospatialKosher() : null);

                } else if (species.equals("search") || species.equals("uploadSpecies")) {

                    if (bgSearchSpeciesACComp == null) {
                        bgSearchSpeciesACComp = (SpeciesAutoCompleteComponent) getFellowIfAny("bgSearchSpeciesACComp");
                    }
                    if (bgSearchSpeciesACComp.hasValidItemSelected()) {
                        //species = (String) (bgSearchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0));
                        q = bgSearchSpeciesACComp.getQuery(getMapComposer(), false, applycheckboxes ? getGeospatialKosherBk() : null);//QueryUtil.get(species, getMapComposer(), false, applycheckboxes ? getGeospatialKosherBk() : null);
                    }
                }
            } catch (Exception e) {
                logger.warn("Unable to retrieve selected species", e);
            }
        }

        return q;
    }

    public String getSelectedSpeciesName() {
        String species = rgSpecies.getSelectedItem().getValue();
        try {
            if (species.equals("allspecies")) {
            } else if (species.equals("allmapped")) {
                // species = "";
                // List<MapLayer> layers = getMapComposer().getSpeciesLayers();
                //
                // for (int i = 0; i < layers.size(); i++) {
                // MapLayer lyr = layers.get(i);
                // Radio rSp = new Radio(lyr.getDisplayName());
                // species += lyr.getMapLayerMetadata().getSpeciesLsid() + ",";
                // }
                // species = species.substring(0, species.length() - 1);

                species = "All mapped species";
            } else if (species.equals("search")) {
                if (searchSpeciesACComp.hasValidItemSelected()) {
                    species = searchSpeciesACComp.getAutoComplete().getText();
                }
            } else {
                species = rgSpecies.getSelectedItem().getLabel();
            }
        } catch (Exception e) {
            logger.warn("Unable to retrieve selected species", e);
        }

        return species;
    }

    public String getSelectedLayers() {
        String layers = "";

        try {
            if (lbListLayers.getSelectedLayers().length > 0) {
                String[] sellayers = lbListLayers.getSelectedLayers();
                for (String l : sellayers) {
                    layers += l + ":";
                }
                layers = layers.substring(0, layers.length() - 1);
            }
        } catch (Exception e) {
            logger.warn("Unable to retrieve selected layers", e);
        }

        return layers;
    }

    void setMultipleLsids(String lsids) {
        try {

            /* set species from layer selector */
            if (lsids != null) {
                tMultiple.setText(lsids);
                rgSpecies.setSelectedItem(rMultiple);
                Clients.evalJavaScript("jq('#" + rMultiple.getUuid() + "-real').attr('checked', true);");
                this.onClick$bMultiple(null);
            }
        } catch (Exception e) {
            logger.warn("Error setting lsid:", e);
        }
    }

    void setMultipleLsidsBk(String lsids) {
        try {

            /* set species from layer selector */
            if (lsids != null) {
                tMultipleBk.setText(lsids);
                rgSpeciesBk.setSelectedItem(rMultipleBk);
                Clients.evalJavaScript("jq('#" + rMultipleBk.getUuid() + "-real').attr('checked', true);");
                this.onClick$bMultipleBk(null);
            }
        } catch (Exception e) {
            logger.warn("Error setting lsid:", e);
        }
    }

    void setLsid(String lsidName) {
        try {

            logger.debug("lsidName: " + lsidName);

            String[] s = lsidName.split("\t");
            String species = s[1];
            String lsid = s[0];

            /* set species from layer selector */
            if (species != null) {
                String tmpSpecies = species;
                searchSpeciesACComp.getAutoComplete().setValue(tmpSpecies);
                searchSpeciesACComp.getAutoComplete().refresh(tmpSpecies);

                if (!searchSpeciesACComp.hasValidItemSelected()) {
                    List list = searchSpeciesACComp.getAutoComplete().getItems();
                    for (int i = 0; i < list.size(); i++) {
                        Comboitem ci = (Comboitem) list.get(i);
                        // compare name
                        if (ci.getLabel().equalsIgnoreCase(searchSpeciesACComp.getAutoComplete().getValue())) {
                            // compare lsid
                            if (ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0).equals(lsid)) {
                                searchSpeciesACComp.getAutoComplete().setSelectedItem(ci);
                                break;
                            }
                        }
                    }
                }
                btnOk.setDisabled(!searchSpeciesACComp.hasValidItemSelected());

                rgSpecies.setSelectedItem(rSpeciesSearch);
                Clients.evalJavaScript("jq('#" + rSpeciesSearch.getUuid() + "-real').attr('checked', true);");
                toggles();
                onClick$btnOk(null);
            }

        } catch (Exception e) {
            logger.warn("Error setting lsid:", e);
        }
    }

    void setLsidBk(String lsidName) {
        if (lsidName == null) {
            return;
        }
        String[] s = lsidName.split("\t");
        String species = s[1];
        String lsid = s[0];

        /* set species from layer selector */
        if (species != null) {
            if (bgSearchSpeciesACComp == null) {
                bgSearchSpeciesACComp = (SpeciesAutoCompleteComponent) getFellowIfAny("bgSearchSpeciesACComp");
            }

            String tmpSpecies = species;
            bgSearchSpeciesACComp.getAutoComplete().setValue(tmpSpecies);
            bgSearchSpeciesACComp.getAutoComplete().refresh(tmpSpecies);

            if (!bgSearchSpeciesACComp.hasValidItemSelected()) {
                List list = bgSearchSpeciesACComp.getAutoComplete().getItems();
                for (int i = 0; i < list.size(); i++) {
                    Comboitem ci = (Comboitem) list.get(i);
                    // compare name
                    if (ci.getLabel().equalsIgnoreCase(bgSearchSpeciesACComp.getAutoComplete().getValue())) {
                        // compare lsid
                        if (ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0).equals(lsid)) {
                            bgSearchSpeciesACComp.getAutoComplete().setSelectedItem(ci);
                            break;
                        }
                    }
                }
            }
            btnOk.setDisabled(!bgSearchSpeciesACComp.hasValidItemSelected());
            rgSpeciesBk.setSelectedItem(rSpeciesSearchBk);
            Clients.evalJavaScript("jq('#" + rSpeciesSearchBk.getUuid() + "-real').attr('checked', true);");

            onClick$btnOk(null);
        }
    }

    public void onSelect$lbListLayers(Event event) {
        toggles();
    }

    void toggles() {
        // btnOk.setDisabled(true);
        btnOk.setDisabled(false);
        btnOk.setVisible(true);

        if (fileUpload != null) {
            fileUpload.setVisible(false);
        }
        Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
        if (lbListLayers != null) {

            if (currentDiv.getZclass().contains("minlayers1")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 1);
            } else if (currentDiv.getZclass().contains("minlayers2")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 2);
            } else if (currentDiv.getZclass().contains("optional")) {
                btnOk.setDisabled(false);
            }
            // test for max
            if (currentDiv.getZclass().contains("maxlayers")) {
                int start = currentDiv.getZclass().indexOf("maxlayers") + "maxlayers".length();
                int end = Math.max(currentDiv.getZclass().indexOf(" ", start), currentDiv.getZclass().length());
                int max = Integer.parseInt(currentDiv.getZclass().substring(start, end));
                if (!btnOk.isDisabled()) {
                    btnOk.setDisabled(lbListLayers.getSelectedCount() > max);
                }
            }
            updateLayerSelectionCount();
        }
        //NC 2013-07-15: Only want to perform the rgSpeices onCheck action if it is on the current page.
        if (rgSpecies != null && Components.isAncestor(currentDiv, rgSpecies)) {
            onCheck$rgSpecies(null);
        }
        //NC 2013-07-15: Only want to perform the rgSpeicesBk onCheck action if it is on the current page.
        if (rgSpeciesBk != null && Components.isAncestor(currentDiv, rgSpeciesBk)) {
            onCheck$rgSpeciesBk(null);
        }

        if (currentDiv != null) {
            if (currentDiv.getZclass().contains("layers2auto")) {
                cbLayer2 = (EnvLayersCombobox) getFellowIfAny("cbLayer2");
                cbLayer1 = (EnvLayersCombobox) getFellowIfAny("cbLayer1");
                btnOk.setDisabled(cbLayer2.getSelectedItem() == null || cbLayer1.getSelectedItem() == null);
            }

            if (currentDiv.getZclass().contains("optional")) {
                btnOk.setDisabled(false);
            }

            if (currentDiv.getZclass().contains("species")) {
                // if (divSpeciesSearch != null && divSpeciesSearch.isVisible()){
                if (divSpeciesSearch.isVisible()) {
                    btnOk.setDisabled(!searchSpeciesACComp.hasValidAnnotatedItemSelected());
                    //                btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() != null
                    //                        && (searchSpeciesAuto.getSelectedItem().getValue() == null || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null || searchSpeciesAuto.getSelectedItem()
                    //                                .getAnnotatedProperties().size() == 0));
                } else if (vboxMultiple.isVisible()) {
                    btnOk.setDisabled(getMultipleLsids().length() == 0);
                }
                if (chkGeoKosherTrue != null) {
                    updateGeospatialKosherCheckboxes();
                }
                if (chkGeoKosherTrueBk != null) {
                    updateGeospatialKosherCheckboxesBk();
                }
            }
        }

        if (lbListLayers != null) {
            if (bLayerListDownload1 != null && bLayerListDownload2 != null) {
                bLayerListDownload1.setDisabled(lbListLayers.getSelectedCount() == 0);
                bLayerListDownload2.setDisabled(lbListLayers.getSelectedCount() == 0);
            }
        }
    }

    public void onChange$cbLayer2(Event event) {
        toggles();
    }

    public void onChange$cbLayer1(Event event) {
        toggles();
    }

    public String getSelectedAreaName() {
        String area = rAreaSelected.getLabel();
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (MapLayer ml : layers) {
            if (area.equals(ml.getDisplayName())) {
                area = ml.getName();
                break;
            }
        }

        return area;
    }

    public String getSelectedAreaDisplayName() {
        String areaName = rAreaSelected.getLabel();

        return areaName;
    }

    public void onClick$btnClearSelection(Event event) {
        lbListLayers.clearSelection();
        lbListLayers.updateDistances();
        toggles();
        btnOk.setDisabled(true);
    }

    private boolean isAreaHighlightTab() {
        return rgAreaHighlight != null && rgAreaHighlight.getParent().isVisible();
    }

    boolean isAreaTab() {
        return rgArea != null && rgArea.getParent().isVisible();
    }

    boolean isAreaCustom() {
        return isAreaTab() && rAreaCustom != null && rAreaCustom.isSelected();
    }

    boolean isAreaHighlightCustom() {
        return isAreaHighlightTab() && rgAreaHighlight != null && rgAreaHighlight.getSelectedItem().getId().equals("rAreaCustomHighlight");
    }

    public void onSelect$selectedLayersCombobox(Event event) {
        Comboitem ci = selectedLayersCombobox.getSelectedItem();
        if (ci != null && lbListLayers != null) {
            String layersList = null;
            if (ci.getValue() != null && ci.getValue() instanceof LayerSelection) {
                layersList = ((LayerSelection) ci.getValue()).getLayers();
            } else {
                if (ci.getValue() == null) {
                    if (ci.getLabel().toLowerCase().contains("paste")) {
                        Window window = (Window) Executions.createComponents("WEB-INF/zul/input/PasteLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            logger.error("error opening PasteLayerList.zul", e);
                        }
                    } else if (ci.getLabel().toLowerCase().contains("import")) {
                        Window window = (Window) Executions.createComponents("WEB-INF/zul/input/UploadLayerList.zul", this, null);

                        try {
                            window.doModal();
                        } catch (Exception e) {
                            logger.error("error opening UploadLayerList.zul", e);
                        }
                    }
                    selectedLayersCombobox.setSelectedIndex(-1);
                }
            }
            selectLayerFromList(layersList);
        }
    }

    public void selectLayerFromList(String layersList) {
        if (layersList == null) {
            return;
        }

        // support new line separated layer names
        layersList = layersList.replace("\n", ",");

        // check the whole layer string as well as the one at the end
        String[] layers = layersList.split(",");
        String[] list = new String[layers.length * 2];
        for (int i = 0; i < layers.length; i++) {
            int p1 = layers[i].lastIndexOf('(');
            int p2 = layers[i].lastIndexOf(')');
            if (p1 >= 0 && p2 >= 0 && p1 < p2) {
                list[i * 2] = layers[i].substring(p1 + 1, p2).trim();
            }
            list[i * 2 + 1] = layers[i];
        }
        lbListLayers.selectLayers(list);
        toggles();
    }

    public void saveLayerSelection() {
        // save layer selection
        if (lbListLayers != null && lbListLayers.getSelectedCount() > 0) {
            String list = getLayerListShortText();
            LayerSelection ls = new LayerSelection(selectedMethod, tToolName.getText(), System.currentTimeMillis(), list);
            getMapComposer().addLayerSelection(ls);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < getMapComposer().getLayerSelections().size(); i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                sb.append(getMapComposer().getLayerSelections().get(i).toString());
                sb.append(" // ");
                sb.append(getMapComposer().getLayerSelections().get(i).getLayers());
            }

            try {
                Cookie c = new Cookie("analysis_layer_selections", URLEncoder.encode(sb.toString(), "UTF-8"));
                c.setMaxAge(Integer.MAX_VALUE);
                ((HttpServletResponse) Executions.getCurrent().getNativeResponse()).addCookie(c);
            } catch (Exception e) {
                logger.error("error encoding cookies from UTF-8: " + sb.toString());
            }
        }
    }

    void updateLayerListText() {
        try {
            if (lbListLayers != null && lbListLayers.getSelectedCount() > 0 && tLayerList != null) {
                String lyrtext = getLayerListText();
                logger.debug("Setting Layer text: \n" + lyrtext);
                tLayerList.setText(lyrtext);
                if (dLayerSummary != null) {
                    dLayerSummary.setVisible(tLayerList.getText().length() > 0);
                }
            }
        } catch (Exception e) {
            logger.error("error selecting layers", e);
        }
    }

    String getLayerListText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                if (sb.length() > 0) {
                    sb.append(",");
                }

                // String displayname =
                // CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                String displayname = CommonData.getLayerDisplayName(s);
                if (displayname != null && displayname.length() > 0) {
                    sb.append(displayname).append(" (").append(s).append(")");
                } else {
                    sb.append(s);
                }
            } catch (Exception e) {
                logger.error("error geting layer list as text", e);
            }
        }
        return sb.toString();
    }

    String getLayerListShortText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayName(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(s);
                }
            } catch (Exception e) {
                logger.error("error getting list of layer short names", e);
            }
        }
        return sb.toString();
    }

    public void onChange$cbLayer(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayer.getSelectedItem() != null) {
            JSONObject jo = cbLayer.getSelectedItem().getValue();
            String[] layer = jo.getString("name").split("/");
            lbListLayers.selectLayers(layer);
            cbLayer.setSelectedIndex(-1);
        }
        toggles();
    }

    public void onChange$cbLayerEnvironmentalOnly(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayerEnvironmentalOnly.getSelectedItem() != null) {
            JSONObject jo = cbLayerEnvironmentalOnly.getSelectedItem().getValue();
            String[] layer = jo.getString("name").split("/");
            lbListLayers.selectLayers(layer);
            cbLayerEnvironmentalOnly.setSelectedIndex(-1);
        }
        toggles();
    }

    public void onChange$cbLayerMix(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayerMix.getSelectedItem() != null) {
            JSONObject jo = cbLayerMix.getSelectedItem().getValue();
            String[] layer = jo.getString("name").split("/");
            lbListLayers.selectLayers(layer);
            cbLayerMix.setSelectedIndex(-1);
        }
        toggles();
    }

    public void onClick$bLayerListDownload1(Event event) {
        downloadLayerList();
    }

    public void onClick$bLayerListDownload2(Event event) {
        downloadLayerList();
    }

    void downloadLayerList() {
        SimpleDateFormat sdf = new SimpleDateFormat("ddmmyyyy_hhmm");
        Filedownload.save(getLayerListShortText(), "text/plain", "layer_selection_" + sdf.format(new Date()) + ".txt");
    }

    public void updateLayerSelectionCount() {
        if (lLayersSelected != null && lbListLayers != null) {
            if (lbListLayers.getSelectedCount() == 1) {
                lLayersSelected.setValue("1 layer selected");
            } else {
                // test for max
                String error = "";
                Div currentDiv = (Div) getFellowIfAny("atstep" + currentStep);
                if (currentDiv.getZclass().contains("maxlayers")) {
                    int start = currentDiv.getZclass().indexOf("maxlayers") + "maxlayers".length();
                    int end = currentDiv.getZclass().indexOf(" ", start);
                    if (end < 0) {
                        end = currentDiv.getZclass().length();
                    }

                    int max = Integer.parseInt(currentDiv.getZclass().substring(start, end));

                    if (lbListLayers.getSelectedCount() > max) {
                        lLayersSelected.setSclass("lblRed");
                        error = ", INVALID: select no more than " + max + " layers.";
                    } else {
                        lLayersSelected.setSclass("");
                    }
                }
                if (currentDiv.getZclass().contains("minlayers")) {
                    int start = currentDiv.getZclass().indexOf("minlayers") + "minlayers".length();
                    int end = currentDiv.getZclass().indexOf(" ", start);
                    if (end < 0) {
                        end = currentDiv.getZclass().length();
                    }

                    int min = Integer.parseInt(currentDiv.getZclass().substring(start, end));
                    if (lbListLayers.getSelectedCount() < min) {
                        lLayersSelected.setSclass("lblRed");
                        error = ", INVALID: select at least " + min + " layers.";
                    }
                }
                if (error.length() == 0) {
                    lLayersSelected.setSclass("");
                }

                lLayersSelected.setValue(lbListLayers.getSelectedCount() + " layers selected" + error);
            }
        }
    }

    public void onUpload$uploadLayerList(Event event) {
        doFileUpload(event);
    }

    public void onUpload$fileUpload(Event event) {
        UploadEvent ue = null;
        UserData ud = null;

        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        Media m = ue.getMedia();
        if (ud == null) {
            ud = new UserData(m.getName());
        }
        if (ud.getName().trim().equals("")) {
            ud.setName(m.getName());
        }
        ud.setFilename(m.getName());

        if (ud.getName() == null || ud.getName().length() == 0) {
            ud.setName(m.getName());
        }
        if (ud.getDescription() == null || ud.getDescription().length() == 0) {
            ud.setDescription(m.getName());
        }

        ud.setUploadedTimeInMs(System.currentTimeMillis());

        UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/input/UploadSpecies.zul", this, null);
        usc.addToMap = false;

        // is it bk or normal lsid
        // if (rgSpeciesBk != null
        // && (rgSpeciesBk.getSelectedItem() == rSpeciesUploadLSIDBk
        // || rgSpeciesBk.getSelectedItem() == rSpeciesUploadSpeciesBk)) {
        // usc.setUploadType("bk");
        // }
        if (rgSpeciesBk != null) {
            if (rgSpeciesBk.getSelectedItem() == rSpeciesUploadLSIDBk) {
                usc.setUploadType("assemblagebk");
            } else if (rgSpeciesBk.getSelectedItem() == rSpeciesUploadSpeciesBk) {
                usc.setUploadType("bk");
            }
        }

        usc.setVisible(false);
        usc.doOverlapped();
        usc.doFileUpload(ud, event);
        usc.detach();
    }

    public void doFileUpload(Event event) {
        UploadEvent ue = null;
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            logger.error("unable to upload file");
            return;
        } else {
            logger.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                logger.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    // last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    logger.error("unable to load user layer list: ", e);
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    private void loadLayerList(Reader r) throws Exception {
        CSVReader reader = new CSVReader(r);
        // one line, read it
        StringBuilder sb = new StringBuilder();
        for (String s : reader.readNext()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }
        reader.close();
        selectLayerFromList(sb.toString());
        updateLayerSelectionCount();
    }

    public void onValueSelected$mSearchSpeciesACComp(Event event) {
        Events.echoEvent("mChooseSelected", this, null);
    }

    public void mChooseSelected(Event event) {
        Comboitem ci = mSearchSpeciesACComp.getAutoComplete().getSelectedItem();
        if (ci != null && ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0) != null) {
            String annotatedValue = ci.getAnnotatedProperties().get(0);
            if (mSearchSpeciesACComp.shouldUseRawName()) {
                addTolMultiple(null, annotatedValue, null, null, true);
            } else {
                String lsid = annotatedValue;

                try {
                    Map<String, String> searchResult = BiocacheQuery.getClassification(lsid);

                    String sciname = searchResult.get("scientificName");
                    String family = searchResult.get("family");
                    String kingdom = searchResult.get("kingdom");
                    if (sciname == null) {
                        sciname = "";
                    }
                    if (family == null) {
                        family = "";
                    }
                    if (kingdom == null) {
                        kingdom = "";
                    }

                    if (sciname != null && sciname.length() > 0) {
                        addTolMultiple(lsid, sciname, family, kingdom, true);

                        mSearchSpeciesACComp.getAutoComplete().setText("");
                    }
                } catch (Exception e) {
                    logger.error("error with species autocomplete", e);
                }
            }
        }
        toggles();
    }

    public void onValueSelected$mSearchSpeciesACCompBk(Event event) {
        Events.echoEvent("mChooseSelectedBk", this, null);
    }

    public void mChooseSelectedBk(Event event) {
        Comboitem ci = mSearchSpeciesACCompBk.getAutoComplete().getSelectedItem();
        if (ci != null && ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0) != null) {

            String annotatedValue = ci.getAnnotatedProperties().get(0);
            if (mSearchSpeciesACCompBk.shouldUseRawName()) {
                addTolMultipleBk(null, annotatedValue, null, null, true);
            } else {

                String lsid = annotatedValue;

                try {
                    Map<String, String> searchResult = BiocacheQuery.getClassification(lsid);

                    String sciname = searchResult.get("scientificName");
                    String family = searchResult.get("family");
                    String kingdom = searchResult.get("kingdom");
                    if (sciname == null) {
                        sciname = "";
                    }
                    if (family == null) {
                        family = "";
                    }
                    if (kingdom == null) {
                        kingdom = "";
                    }

                    if (sciname != null && sciname.length() > 0) {
                        addTolMultipleBk(lsid, sciname, family, kingdom, true);

                        mSearchSpeciesACCompBk.getAutoComplete().setText("");
                    }
                } catch (Exception e) {
                    logger.error("error with species autocomplete for background species (scatterplot)", e);
                }
            }
        }
        toggles();
    }

    public void onClick$bMultiple(Event event) {
        importList(tMultiple.getText());
    }

    void importList(String list) {
        String[] speciesNames = list.replace("\n", ",").replace("\t", ",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            if (s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = searchResult.getJSONArray("values");

                    String sciname = "", family = "", kingdom = "", lsid = null;
                    for (int j = 0; j < ja.size(); j++) {
                        if (ja.getJSONObject(j).getString("name").equals("scientificName")) {
                            sciname = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("family")) {
                            family = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("kingdom")) {
                            kingdom = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("taxonConceptID")) {
                            lsid = ja.getJSONObject(j).getString("processed");
                        }
                    }

                    // is 's' an LSID?
                    if ((lsid == null || lsid.length() == 0) && s.matches(".*[0-9].*")) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if (sr.size() > 0 && sr.get("scientificName") != null && sr.get("scientificName").length() > 0) {
                            lsid = s;
                            sciname = sr.get("scientificName");
                            family = sr.get("family");
                            kingdom = sr.get("kingdom");
                            if (sciname == null) {
                                sciname = "";
                            }
                            if (family == null) {
                                family = "";
                            }
                            if (kingdom == null) {
                                kingdom = "";
                            }
                        }
                    }

                    if (lsid != null && lsid.length() > 0) {
                        addTolMultiple(lsid, sciname, family, kingdom, false);
                    } else {
                        addTolMultiple(null, s, "", "", false);
                    }

                    if (lsid == null || lsid.length() == 0) {
                        notFound.add(s);
                        notFoundSb.append(s).append("\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s).append("\n");
                }
            }
        }

        if (notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    public void onClick$bMultipleBk(Event event) {
        String[] speciesNames = tMultipleBk.getText().replace("\n", ",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            if (s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = searchResult.getJSONArray("values");

                    String sciname = "", family = "", kingdom = "", lsid = null;
                    for (int j = 0; j < ja.size(); j++) {
                        if (ja.getJSONObject(j).getString("name").equals("scientificName")) {
                            sciname = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("family")) {
                            family = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("kingdom")) {
                            kingdom = ja.getJSONObject(j).getString("processed");
                        }
                        if (ja.getJSONObject(j).getString("name").equals("taxonConceptID")) {
                            lsid = ja.getJSONObject(j).getString("processed");
                        }
                    }

                    // is 's' an LSID?
                    if (lsid == null || lsid.length() == 0) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if (sr.size() > 0 && sr.get("scientificName") != null && sr.get("scientificName").length() > 0) {
                            lsid = s;
                            sciname = sr.get("scientificName");
                            family = sr.get("family");
                            kingdom = sr.get("kingdom");
                            if (sciname == null) {
                                sciname = "";
                            }
                            if (family == null) {
                                family = "";
                            }
                            if (kingdom == null) {
                                kingdom = "";
                            }
                        }
                    }

                    if (lsid != null && lsid.length() > 0) {
                        addTolMultipleBk(lsid, sciname, family, kingdom, false);
                    } else {
                        addTolMultipleBk(null, s, "", "", false);
                    }

                    if (lsid == null || lsid.length() == 0) {
                        notFound.add(s);
                        notFoundSb.append(s).append("\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s).append("\n");
                }
            }
        }

        if (notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    JSONObject processAdhoc(String scientificName) {
        String url = CommonData.biocacheServer + "/process/adhoc";
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            StringRequestEntity sre = new StringRequestEntity("{ \"scientificName\": \"" + scientificName.replace("\"", "'") + "\" } ", "application/json", "UTF-8");
            post.setRequestEntity(sre);
            int result = client.executeMethod(post);
            if (result == 200) {
                return JSONObject.fromObject(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            logger.error("error processing species request: " + url + ", scientificName=" + scientificName, e);
        }
        return null;
    }

    private void addTolMultiple(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for (Listitem li : lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if ((lsid != null && lsidCell.getLabel().equals(lsid)) || (sciname != null && scinameCell.getLabel().replace("(not found)", "").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        // remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                toggles();
            }
        });
        lc.setParent(li);

        // sci name
        if (lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        // family
        if (lsid == null) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)", "").trim();
                    mSearchSpeciesACComp.getAutoComplete().refresh(sciname);
                    mSearchSpeciesACComp.getAutoComplete().open();
                    mSearchSpeciesACComp.getAutoComplete().setText(sciname + " ");
                    li.detach();
                    toggles();
                }
            });
        } else {
            lc = new Listcell(family);
        }
        lc.setParent(li);

        // kingdom
        lc = new Listcell(kingdom);
        lc.setParent(li);

        // count
        if (lsid != null) {
            int count = new BiocacheQuery(lsid, null, null, null, false, getGeospatialKosher()).getOccurrenceCount();
            if (count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        // lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if (insertAtBeginning && lMultiple.getChildren().size() > 0) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }

    private void addTolMultipleBk(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for (Listitem li : lMultipleBk.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if ((lsid != null && lsidCell.getLabel().equals(lsid)) || (sciname != null && scinameCell.getLabel().replace("(not found)", "").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        // remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                toggles();
            }
        });
        lc.setParent(li);

        // sci name
        if (lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        // family
        if (lsid == null) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)", "").trim();
                    mSearchSpeciesACCompBk.getAutoComplete().refresh(sciname);
                    mSearchSpeciesACCompBk.getAutoComplete().open();
                    mSearchSpeciesACCompBk.getAutoComplete().setText(sciname + " ");
                    li.detach();
                    toggles();
                }
            });
        } else {
            lc = new Listcell(family);
        }
        lc.setParent(li);

        // kingdom
        lc = new Listcell(kingdom);
        lc.setParent(li);

        // count
        if (lsid != null) {
            int count = new BiocacheQuery(lsid, null, null, null, false, getGeospatialKosherBk()).getOccurrenceCount();
            if (count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        // lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if (insertAtBeginning && lMultipleBk.getChildren().size() > 0) {
            lMultipleBk.insertBefore(li, lMultipleBk.getFirstChild());
        } else {
            li.setParent(lMultipleBk);
        }
    }

    private String getMultipleLsids() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lMultiple.getItems()) {
            Listcell lc = (Listcell) li.getLastChild();
            if (lc.getLabel() != null && lc.getLabel().length() > 0) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lc.getLabel());
            }
        }
        return sb.toString();
    }

    private String getMultipleLsidsBk() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lMultipleBk.getItems()) {
            Listcell lc = (Listcell) li.getLastChild();
            if (lc.getLabel() != null && lc.getLabel().length() > 0) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lc.getLabel());
            }
        }
        return sb.toString();
    }

    public void setIncludeAnalysisLayersForUploadQuery(boolean includeAnalysisLayersForUploadQuery) {
        this.includeAnalysisLayersForUploadQuery = includeAnalysisLayersForUploadQuery;
    }

    public void setIncludeAnalysisLayersForAnyQuery(boolean includeAnalysisLayersForAnyQuery) {
        this.includeAnalysisLayersForAnyQuery = includeAnalysisLayersForAnyQuery;
    }

    public void onClick$btnUpload(Event event) {
        try {
            logger.debug("onClick$btnUpload(Event event)");
            UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/input/UploadSpecies.zul", this, null);

            if (rSpeciesUploadSpecies.isSelected()) {
                usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
            } else if (rSpeciesUploadLSID.isSelected()) {
                usc.setTbInstructions("3. Select file (text file, one LSID or name per line)");
            } else {
                usc.setTbInstructions("3. Select file");
            }
            usc.addToMap = true;
            // usc.setDefineArea(chkArea.isChecked());
            usc.doModal();
            return;
        } catch (Exception e) {
            logger.error("file upload error", e);
        }
    }

    public void setChooseEndemic(boolean choose) {
        chkEndemicSpecies.setChecked(choose);
    }

    public boolean getIsEndemic() {
        return chkEndemicSpecies != null && chkEndemicSpecies.isChecked();
    }

    /**
     * TODO NC 2013-08-15: Remove the need for "false" as the third item in the array.
     *
     * @return
     */
    public boolean[] getGeospatialKosher() {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return new boolean[]{true, true, false};
        } else {
            return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
        }
    }

    /**
     * TODO NC 2013-08-15: Remove the need for "false" as the third item in the array.
     *
     * @return
     */
    public boolean[] getGeospatialKosherBk() {
        if (chkGeoKosherTrueBk == null || chkGeoKosherFalseBk == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher background species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return new boolean[]{true, true, false};
        } else {
            return new boolean[]{chkGeoKosherTrueBk.isChecked(), chkGeoKosherFalseBk.isChecked(), false};
        }
    }

    void updateGeospatialKosherCheckboxes() {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        // get selected species
        Query q = getSelectedSpecies(false, true);
        boolean[] gk;
        if (q instanceof BiocacheQuery && (gk = ((BiocacheQuery) q).getGeospatialKosher()) != null) {
            chkGeoKosherTrue.setDisabled(false);
            chkGeoKosherFalse.setDisabled(false);

            if (chkGeoKosherTrue.isVisible()) {
                chkGeoKosherTrue.setChecked(gk[0]);
            }
            if (chkGeoKosherFalse.isVisible()) {
                chkGeoKosherFalse.setChecked(gk[1]);
            }

        } else {
            chkGeoKosherTrue.setDisabled(true);
            chkGeoKosherFalse.setDisabled(true);

        }
    }

    public void setGeospatialKosherCheckboxes(boolean[] geospatialKosher) {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        if (geospatialKosher != null) {
            chkGeoKosherTrue.setChecked(geospatialKosher[0]);
            chkGeoKosherFalse.setChecked(geospatialKosher[1]);

        } else {
            chkGeoKosherTrue.setChecked(true);
            chkGeoKosherFalse.setChecked(true);

        }
    }

    void updateGeospatialKosherCheckboxesBk() {
        if (chkGeoKosherTrueBk == null || chkGeoKosherFalseBk == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher background species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        // get selected species
        Query q = getSelectedSpeciesBk(false, true);
        boolean[] gk;
        if (q instanceof BiocacheQuery && (gk = ((BiocacheQuery) q).getGeospatialKosher()) != null) {
            chkGeoKosherTrueBk.setDisabled(false);
            chkGeoKosherFalseBk.setDisabled(false);

            if (chkGeoKosherTrueBk.isVisible()) {
                chkGeoKosherTrueBk.setChecked(gk[0]);
            }
            if (chkGeoKosherFalseBk.isVisible()) {
                chkGeoKosherFalseBk.setChecked(gk[1]);
            }

        } else {
            chkGeoKosherTrueBk.setDisabled(true);
            chkGeoKosherFalseBk.setDisabled(true);
        }
    }

    public void setGeospatialKosherCheckboxesBk(boolean[] geospatialKosher) {
        if (chkGeoKosherTrueBk == null || chkGeoKosherFalseBk == null) {
            logger.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        if (geospatialKosher != null) {
            chkGeoKosherTrueBk.setChecked(geospatialKosher[0]);
            chkGeoKosherFalseBk.setChecked(geospatialKosher[1]);
        } else {
            chkGeoKosherTrueBk.setChecked(true);
            chkGeoKosherFalseBk.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherTrue(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherFalse.isChecked()) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherTrue.isChecked()) {
            chkGeoKosherTrue.setChecked(true);
        }
    }


    public void onCheck$chkGeoKosherTrueBk(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherFalseBk.isChecked()) {
            chkGeoKosherFalseBk.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalseBk(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherTrueBk.isChecked()) {
            chkGeoKosherTrueBk.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherNullBk(Event event) {
        event = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) event).isChecked() && !chkGeoKosherTrueBk.isChecked()) {
            chkGeoKosherTrueBk.setChecked(true);
        }
    }

    public void onClick$bAssemblageExport(Event event) {

        SimpleDateFormat sdf = new SimpleDateFormat("ddmmyyyy_hhmm");
        Filedownload.save(getLsids(), "text/plain", "species_assemblage_" + sdf.format(new Date()) + ".txt");
    }

    /**
     * Shows a "create species list" dialog for the supplied list box
     *
     * @param lb
     */
    private void showExportSpeciesListDialog(Listbox lb) {
        String values = getScientificName(lb);//tMultiple.getValue().replace("\n", ",").replace("\t",",");
        logger.debug("Creating species list with " + values);
        if (values.length() > 0) {
            UploadToSpeciesListController dialog = (UploadToSpeciesListController) Executions.createComponents("WEB-INF/zul/input/UploadToSpeciesList.zul", this, null);
            dialog.setSpecies(values);
            try {
                dialog.doModal();

            } catch (Exception e) {
                logger.error("Unable to export assemblage", e);
            }
        }

    }

    /**
     * Retrieves a CSV version scientific names that have been added to the
     * list.
     *
     * @param lb The listbox to retrieve the items from
     * @return
     */
    private String getScientificName(Listbox lb) {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lb.getItems()) {
            Listcell sciNameCell = (Listcell) li.getChildren().get(1);
            String name = sciNameCell.getLabel();
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(name.replaceAll("\\(not found\\)", "").trim());
        }
        return sb.toString();
    }

    public void updateSpeciesListMessage(String drUid) {
        //if a data resource exists check report it
        logger.debug("Species list that was created : " + drUid);
        if (drUid != null) {
            boolean isBk = rMultipleBk != null && rMultipleBk.isChecked();
            SpeciesListListbox lb = isBk ? speciesListListboxBk : speciesListListbox;
            Radio r = isBk ? rSpeciesUploadLSIDBk : rSpeciesUploadLSID;

            ((SpeciesListListbox.SpeciesListListModel) lb.getModel()).refreshModel();
            //rgAddSpecies.setS
            r.setSelected(true);
            if (isBk) {
                onCheck$rgSpeciesBk(null);
            } else {
                onCheck$rgSpecies(null);
            }
        }
    }

    String getLsids() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(lsidCell.getLabel());
        }
        return sb.toString();
    }

    public void onUpload$bSpeciesListUpload(Event event) {
        UploadEvent ue = null;
        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        if (ue == null) {
            logger.warn("unable to upload file");
            return;
        } else {
            logger.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            // forget content types, do 'try'
            boolean loaded = false;
            try {
                importList(readerToString(m.getReaderData()));
                loaded = true;
                logger.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                // e.printStackTrace();
            }
            if (!loaded) {
                try {
                    importList(new String(m.getByteData()));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    importList(readerToString(new InputStreamReader(m.getStreamData())));
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    importList(m.getStringData());
                    loaded = true;
                    logger.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    // last one, report error
                    getMapComposer().showMessage("Unable to load your file. Please try again.");
                    logger.error("unable to load user points: ", e);
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    String readerToString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1000];
        int size;
        while ((size = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, size);
        }
        return sb.toString();
    }
}
