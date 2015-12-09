package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.legend.Facet;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.input.PasteLayerListController;
import au.org.ala.spatial.composer.input.UploadLayerListController;
import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.composer.input.UploadToSpeciesListController;
import au.org.ala.spatial.composer.layer.EnvLayersCombobox;
import au.org.ala.spatial.composer.layer.EnvironmentalList;
import au.org.ala.spatial.composer.layer.SelectedLayersCombobox;
import au.org.ala.spatial.composer.sandbox.SandboxPasteController;
import au.org.ala.spatial.composer.species.SpeciesAutoCompleteComponent;
import au.org.ala.spatial.composer.species.SpeciesListListbox;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
 * @author ajay
 */
public class ToolComposer extends UtilityComposer {
    private static final Logger LOGGER = Logger.getLogger(ToolComposer.class);
    protected String selectedMethod = "";
    protected RemoteLogger remoteLogger;
    protected int currentStep = 1;
    protected int totalSteps = 5;
    protected String pid = "";
    protected Radiogroup rgArea;
    protected Radiogroup rgAreaHighlight;
    protected Radiogroup rgSpecies;
    protected Radiogroup rgSpeciesBk;
    protected Radio rSpeciesSearch;
    protected Radio rSpeciesSearchBk;
    protected Radio rAreaSelected;
    protected Button btnOk;
    protected Textbox tToolName;
    protected SpeciesAutoCompleteComponent searchSpeciesACComp;
    protected SpeciesAutoCompleteComponent bgSearchSpeciesACComp;
    protected EnvironmentalList lbListLayers;
    protected EnvLayersCombobox cbLayer1;
    protected EnvLayersCombobox cbLayer2;
    protected SelectedLayersCombobox selectedLayersCombobox;
    protected Doublebox dResolution;
    protected Checkbox chkEndemicSpecies;
    protected boolean isBackgroundProcess = true;
    protected boolean hasEstimated = false;
    private Map<String, Object> params;
    private Radio rMaxent, rAloc, rScatterplot, rGdm, rTabulation;
    private Radio rSpeciesAll;
    private Radio rSpeciesMapped;
    private Radio rSpeciesUploadLSID;
    private Radio rSpeciesUploadSpecies;
    private Radio rMultiple;
    private Radio rSpeciesNoneBk;
    private Radio rSpeciesAllBk;
    private Radio rSpeciesMappedBk;
    private Radio rSpeciesUploadLSIDBk;
    private Radio rSpeciesUploadSpeciesBk;
    private Radio rMultipleBk;
    private Radio rAreaWorld;
    private Radio rAreaCustom;
    private Radio rAreaWorldHighlight;
    private Radio rSpeciesLifeform;
    private Button btnCancel;
    private Button btnBack;
    private Button btnHelp;
    private Div divSpeciesSearch, divSpeciesSearchBk;
    private String winTop = "300px";
    private String winLeft = "500px";
    private boolean hasCustomArea = false;
    private MapLayer prevTopArea = null;
    private Fileupload fileUpload;
    private Div tlinfo;
    private Textbox tLayerList;
    private Div dLayerSummary;
    private EnvLayersCombobox cbLayer, cbLayerEnvironmentalOnly, cbLayerMix;
    private Button bLayerListDownload1;
    private Button bLayerListDownload2;
    private Label lLayersSelected, lendemicNote;
    private Button btnClearSelection;
    private Menupopup mpLayer2, mpLayer1;
    private Vbox vboxMultiple, vboxMultipleBk;
    private SpeciesAutoCompleteComponent mSearchSpeciesACComp, mSearchSpeciesACCompBk;
    private Textbox tMultiple, tMultipleBk;
    private Listbox lMultiple, lMultipleBk;
    private boolean includeAnalysisLayers;
    private boolean includeContextualLayers = false;
    private boolean singleLayerDomain = true;
    private boolean fullList;
    private boolean includeAnalysisLayersForUploadQuery = false;
    private boolean includeAnalysisLayersForAnyQuery = false;
    private boolean mpLayersIncludeAnalysisLayers = false;
    private Checkbox chkGeoKosherTrue;
    private Checkbox chkGeoKosherFalse;
    private Checkbox chkGeoKosherTrueBk, chkGeoKosherFalseBk;
    private Radio rAreaCurrent;
    private Caption cTitle;
    private Div atsummary;
    private Vbox vboxArea;
    private Checkbox cAreaCurrent;
    private Radio rAreaCurrentHighlight;
    private Radio rAreaNoneHighlight;
    private Radio rAreaNone;
    private boolean[] defaultGeospatialKosher = {true, true, false};
    private Label lEstimateMessage;
    private Div notLoggedIn;
    private Div isLoggedIn;
    //stuff for the dynamic species list inclusion
    private Vbox vboxImportSL, vboxImportSLBk;
    private SpeciesListListbox speciesListListbox, speciesListListboxBk;
    private Div divLifeform;
    private Combobox cbLifeform;

    @Override
    public void afterCompose() {
        super.afterCompose();

        winTop = this.getTop();
        winLeft = this.getLeft();

        setupDefaultParams();

        Map<String, Object> tmp = new HashMap<String, Object>();
        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                if (((Map.Entry) o).getKey() instanceof String) {
                    tmp.put((String) ((Map.Entry) o).getKey(), ((Map.Entry) o).getValue());
                }
            }
        }
        setParams(tmp);

        if (tmp.containsKey(StringConstants.GEOSPATIAL_KOSHER)) {
            setGeospatialKosherCheckboxes((boolean[]) tmp.get(StringConstants.GEOSPATIAL_KOSHER));
        }

        if (tmp.containsKey(StringConstants.CHOOSEENDEMIC)) {
            setChooseEndemic((Boolean) tmp.get(StringConstants.CHOOSEENDEMIC));
        } else if (chkEndemicSpecies != null) {
            updateEndemicCheckBox();
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

        if (searchSpeciesACComp != null) {
            mSearchSpeciesACComp.addEventListener("onValueSelected", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    toggles();
                }
            });
        }
        if (mSearchSpeciesACComp != null) {
            mSearchSpeciesACComp.getAutoComplete().setBiocacheOnly(true);
            mSearchSpeciesACComp.addEventListener("onValueSelected", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    mChooseSelected(null);
                }
            });
        }
        if (mSearchSpeciesACCompBk != null) {
            mSearchSpeciesACCompBk.getAutoComplete().setBiocacheOnly(true);
            mSearchSpeciesACCompBk.addEventListener("onValueSelected", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    mChooseSelectedBk(null);
                }
            });
        }
        // init includeLayers
        if (cbLayer != null) {
            cbLayer.setIncludeLayers("AllLayers");
            cbLayer.refresh("");
        }
        if (cbLayerEnvironmentalOnly != null) {
            cbLayerEnvironmentalOnly.setIncludeLayers(StringConstants.ENVIRONMENTAL_LAYERS);
            cbLayerEnvironmentalOnly.refresh("");
        }
        if (cbLayer1 != null) {
            cbLayer1.setIncludeLayers(StringConstants.ENVIRONMENTAL_LAYERS);
            cbLayer1.refresh("");
        }
        if (cbLayer2 != null) {
            cbLayer2.setIncludeLayers(StringConstants.ENVIRONMENTAL_LAYERS);
            cbLayer2.refresh("");
        }
        if (cbLayerMix != null) {
            cbLayerMix.setIncludeLayers("MixLayers");
            cbLayerMix.refresh("");
        }

        updateDefaultGeospatialKosherValues();

        //some variables won't wire because they are imported
        if (getFellowIfAny("splistbox") != null) {
            vboxImportSL = (Vbox) getFellow("splistbox").getFellow("vboxImportSL");
            speciesListListbox = (SpeciesListListbox) this.getFellow("splistbox").getFellow("speciesListListbox");
        }
        if (getFellowIfAny("splistboxbk") != null) {
            vboxImportSLBk = (Vbox) getFellow("splistboxbk").getFellow("vboxImportSL");
            speciesListListboxBk = (SpeciesListListbox) this.getFellow("splistboxbk").getFellow("speciesListListbox");
        }

        //add the species lists stuff
        if (rSpeciesUploadLSID != null && speciesListListbox != null) {
            speciesListListbox.addEventListener(StringConstants.ONSICHECKBOXCHANGED, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    btnOk.setDisabled((Integer) event.getData() == 0);
                }
            });
        }

        if (rSpeciesUploadLSIDBk != null && speciesListListbox != null) {
            speciesListListbox.addEventListener(StringConstants.ONSICHECKBOXCHANGED, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    btnOk.setDisabled((Integer) event.getData() == 0);
                }
            });
        }

        if (vboxImportSL != null) {
            vboxImportSL.getFellow("btnSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    onClick$btnSearchSpeciesListListbox(event);
                }
            });
            vboxImportSL.getFellow("btnClearSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    onClick$btnClearSearchSpeciesListListbox(event);
                }
            });
        }

        if (vboxImportSLBk != null) {
            vboxImportSLBk.getFellow("btnSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    onClick$btnSearchSpeciesListListbox(event);
                }
            });
            vboxImportSL.getFellow("btnClearSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    onClick$btnClearSearchSpeciesListListbox(event);
                }
            });
        }

        initLifeforms();
    }

    private void initLifeforms() {
        if (cbLifeform != null) {
            for (String lf : StringConstants.SPECIES_GROUPS) {
                Comboitem ci = new Comboitem(lf);
                ci.setParent(cbLifeform);
            }
            cbLifeform.setSelectedIndex(0);
        }
    }

    public void onClick$btnSearchSpeciesListListbox(Event event) {
        try {
            ((SpeciesListListbox) event.getTarget().getParent().getParent().getFellowIfAny("speciesListListbox")).onClick$btnSearchSpeciesListListbox(event);
        } catch (Exception e) {
            LOGGER.error("toolcomposer is missing speciesListListbox for refreshing", e);
        }
    }

    public void onClick$btnClearSearchSpeciesListListbox(Event event) {
        try {
            ((SpeciesListListbox) event.getTarget().getParent().getParent().getFellowIfAny("speciesListListbox")).onClick$btnClearSearchSpeciesListListbox(event);
        } catch (Exception e) {
            LOGGER.error("toolcomposer is missing speciesListListbox for refreshing", e);
        }
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
            int p1 = url.indexOf("&style=") + 7;
            int p2 = url.indexOf("_style", p1);
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
        mi.addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer1.setValue(mi.getValue() + " ");
                cbLayer1.refresh(mi.getValue());
                for (Object o : cbLayer1.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = ci.getValue();
                    if (jo.get(StringConstants.NAME).toString().equals(mi.getValue())) {
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
        mi.addEventListener(StringConstants.ONCLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Menuitem mi = (Menuitem) event.getTarget();
                cbLayer2.setValue(mi.getValue() + " ");
                cbLayer2.refresh(mi.getValue());
                for (Object o : cbLayer2.getItems()) {
                    Comboitem ci = (Comboitem) o;
                    JSONObject jo = ci.getValue();
                    if (jo.get(StringConstants.NAME).equals(mi.getValue())) {
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
        btnOk.setLabel(StringConstants.NEXT_GT);
    }

    public void updateWindowTitle() {
        if (cTitle != null) {
            cTitle.setLabel("Step " + currentStep + " of " + totalSteps + " - " + selectedMethod);
        }
    }

    public void updateName(String name) {
        if (tToolName != null) {
            tToolName.setValue(name);
        }
    }

    private void loadSummaryDetails() {
        if (atsummary != null) {
            String summary = "";
            summary += "<strong>Analytical tool</strong>: " + selectedMethod;
            summary += "<strong>Area</strong>: ";
            summary += "<strong>Species</strong>: ";
            summary += "<strong>Grids</strong>: ";
            summary += "<strong>Additional options</strong>: ";
            atsummary.setContext(summary);
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
            List<MapLayer> layers = getMapComposer().getSpeciesLayers();

            Radio selectedSpecies = null;
            String selectedSpeciesLayer = (String) params.get(StringConstants.SPECIES_LAYER_NAME);
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (biocacheOnly && lyr.getSpeciesQuery() != null && !(lyr.getSpeciesQuery() instanceof BiocacheQuery)) {
                    continue;
                }
                if (lyr.getSubType() != LayerUtilitiesImpl.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getName().replaceAll(" ", "") + "_" + i);
                rgSpecies.insertBefore(rSp, rSpeciesMapped);

                if (rSp.getValue().equals(selectedSpeciesLayer)) {
                    selectedSpecies = rSp;
                }
            }

            if (speciesLayersCount > 1) {
                rSpeciesMapped.setLabel("All " + speciesLayersCount + StringConstants.SPECIES_CURRENTLY_MAPPED);
            } else {
                rSpeciesMapped.setVisible(false);
            }

            if (selectedSpecies != null) {
                rgSpecies.setSelectedItem(selectedSpecies);
            } else if (StringConstants.NONE.equals(selectedSpeciesLayer)) {
                rgSpecies.setSelectedItem(rSpeciesAll);
            } else if (!layers.isEmpty()) {
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
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_LAYERS, e);
        }
    }

    public void loadSpeciesLayersBk() {
        try {
            List<MapLayer> layers = getMapComposer().getSpeciesLayers();
            int speciesLayersCount = 0;

            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                if (lyr.getSubType() != LayerUtilitiesImpl.SPECIES_UPLOAD) {
                    speciesLayersCount++;
                }

                Radio rSp = new Radio(lyr.getDisplayName());
                rSp.setValue(lyr.getName());
                rSp.setId(lyr.getName().replaceAll(" ", "") + "__bk" + i);
                rgSpeciesBk.insertBefore(rSp, rSpeciesMappedBk);
            }

            if (speciesLayersCount > 1) {
                rSpeciesMappedBk.setLabel("All " + speciesLayersCount + StringConstants.SPECIES_CURRENTLY_MAPPED);
            } else {
                rSpeciesMappedBk.setVisible(false);
            }

            updateGeospatialKosherCheckboxesBk();
        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_LAYERS, e);
        }
    }

    public void loadAreaLayers(boolean multiple) {
        if (!multiple) {
            loadAreaLayers(null);
        } else {
            loadAreaLayersCheckboxes(null);
        }
    }

    public void loadAreaLayers() {
        loadAreaLayers(null);
    }

    public void loadAreaLayers(String selectedAreaName) {
        try {
            // remove all radio buttons that don't have an id
            for (int i = rgArea.getItemCount() - 1; i >= 0; i--) {
                String id = rgArea.getItems().get(i).getId();
                if (id == null || id.length() == 0) {
                    rgArea.removeItemAt(i);
                } else {
                    rgArea.getItemAtIndex(i).setSelected(false);
                }
            }

            String selectedLayerName = null;
            if (params != null && params.containsKey(StringConstants.POLYGON_LAYER_NAME)) {
                selectedLayerName = (String) params.get(StringConstants.POLYGON_LAYER_NAME);
            }
            Radio rSelectedLayer = null;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getName());

                rAr.setParent(rgArea);
                if (rAreaCurrent != null) {
                    rgArea.insertBefore(rAr, rAreaCurrent);
                } else {
                    rgArea.appendChild(rAr);
                }

                if (lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                    rAreaSelected = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.isEmpty()) {
                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible() && rgArea.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        rgArea.setSelectedItem(rAreaSelected);
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rAreaSelected = rSelectedLayer;
                rgArea.setSelectedItem(rAreaSelected);
            } else if (StringConstants.NONE.equals(selectedLayerName)) {
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

            Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
        }
    }

    public void loadAreaLayersCheckboxes(String selectedAreaName) {
        try {
            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);

                //add only if missing
                boolean found = false;
                if (getFellow("vboxArea") != null) {
                    List checkboxes = getFellow("vboxArea").getChildren();

                    for (int j = 0; j < checkboxes.size(); j++) {
                        if (checkboxes.get(j) instanceof Checkbox &&
                                ((Checkbox) checkboxes.get(j)).getLabel().equals(lyr.getDisplayName())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    Checkbox rAr = new Checkbox(lyr.getDisplayName());
                    rAr.setValue(lyr.getName());

                    if (lyr.getDisplayName().equals(selectedAreaName)) {
                        rAr.setChecked(true);
                    }

                    rAr.setParent(vboxArea);
                    vboxArea.insertBefore(rAr, cAreaCurrent);
                }
            }
        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
        }
    }

    public void loadAreaHighlightLayers(String selectedAreaName) {
        try {
            // remove all radio buttons that don't have an id
            for (int i = rgAreaHighlight.getItemCount() - 1; i >= 0; i--) {
                String id = rgAreaHighlight.getItems().get(i).getId();
                if (id == null || id.length() == 0) {
                    rgAreaHighlight.removeItemAt(i);
                } else {
                    rgAreaHighlight.getItemAtIndex(i).setSelected(false);
                }
            }

            String selectedLayerName = (String) params.get(StringConstants.POLYGON_LAYER_NAME);
            Radio rSelectedLayer = null;

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setValue(lyr.getName());

                rAr.setParent(rgAreaHighlight);
                rgAreaHighlight.insertBefore(rAr, rAreaCurrentHighlight);

                if (lyr.getName().equals(selectedLayerName)) {
                    rSelectedLayer = rAr;
                }
            }

            if (selectedAreaName != null && !selectedAreaName.isEmpty()) {
                for (int i = 0; i < rgAreaHighlight.getItemCount(); i++) {
                    if (rgAreaHighlight.getItemAtIndex(i).isVisible()
                            && rgAreaHighlight.getItemAtIndex(i).getLabel().equals(selectedAreaName)) {
                        rgAreaHighlight.setSelectedItem(rgAreaHighlight.getItemAtIndex(i));
                        break;
                    }
                }
            } else if (rSelectedLayer != null) {
                rgAreaHighlight.setSelectedItem(rAreaSelected);
            } else if (StringConstants.NONE.equals(selectedLayerName)) {
                rgAreaHighlight.setSelectedItem(rAreaWorld);
            } else {
                for (int i = 0; i < rgAreaHighlight.getItemCount(); i++) {
                    if (rgAreaHighlight.getItemAtIndex(i).isVisible()) {
                        rgAreaHighlight.setSelectedItem(rgAreaHighlight.getItemAtIndex(i));
                        break;
                    }
                }
            }
            Clients.evalJavaScript("jq('#" + rgAreaHighlight.getSelectedItem().getUuid() + "-real').attr('checked', true);");

        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
        }
    }

    public void loadAreaLayersHighlight() {
        try {

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getName().replaceAll(" ", "") + "_" + i);
                rAr.setValue(lyr.getName());
                rAr.setParent(rgAreaHighlight);
                rgAreaHighlight.insertBefore(rAr, rAreaCurrentHighlight);
            }

            rAreaNoneHighlight.setSelected(true);
        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_ACTIVE_AREA_LAYERS, e);
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
            }

            String layers = (String) params.get(StringConstants.ENVIRONMENTALLAYERNAME);
            if (layers != null) {
                lbListLayers.selectLayers(layers.split(","));
            }

            lbListLayers.renderAll();
        } catch (Exception e) {
            LOGGER.error(StringConstants.UNABLE_TO_LOAD_LAYERS, e);
        }
    }

    public void onCheck$rgArea(Event event) {
        if (rgArea == null) {
            return;
        }
        hasCustomArea = false;
        rAreaSelected = rgArea.getSelectedItem();
        try {
            rAreaSelected = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
        } catch (Exception e) {
            LOGGER.error(StringConstants.FAILED_TO_SET_RADIO, e);
        }
        if (rAreaSelected == rAreaCustom) {
            hasCustomArea = false;
        }

        if (chkEndemicSpecies != null) {
            updateEndemicCheckBox();
        }
    }

    private void updateEndemicCheckBox() {
        if (rAreaSelected == null) {
            return;
        }

        // check to see if the area is within the required size...
        boolean showEndemic = false;
        String value = rAreaSelected.getValue();

        if (!StringConstants.AUSTRALIA.equals(value) && !StringConstants.WORLD.equals(value)
                && !"custom".equals(value)) {
            String areaName = rAreaSelected.getLabel();
            MapLayer ml = getMapComposer().getMapLayer(areaName);
            String sarea = "";
            if (StringConstants.CURRENT.equals(value)) {
                // check to see if the current extent is within the maximum
                // area
                SelectedArea sa = getSelectedArea();
                sarea = sa.getKm2Area();
            } else if (ml != null) {
                sarea = ml.getAreaSqKm();
                if (sarea == null) {
                    sarea = ml.calculateAndStoreArea();
                }
            }
            try {
                Float area = Float.parseFloat(sarea.replaceAll(",", ""));
                showEndemic = (area <= CommonData.getMaxEndemicArea());
            } catch (NumberFormatException e) {
                LOGGER.error("failed to parse endemic area from: " + sarea);
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
        hasCustomArea = false;
        if (StringConstants.RAREACUSTOMHIGHLIGHT.equals(rgAreaHighlight.getSelectedItem().getId())) {
            hasCustomArea = false;
        }
    }

    public void onCheck$rgSpecies(Event event) {
        if (rgSpecies == null) {
            return;
        }
        Radio selectedItem = rgSpecies.getSelectedItem();
        try {
            if (selectedItem == null && event != null) {
                selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
            }
        } catch (Exception e) {
            LOGGER.error(StringConstants.FAILED_TO_SET_RADIO, e);
        }
        try {
            if (vboxImportSL != null && event != null && selectedItem != rSpeciesUploadLSID) {
                vboxImportSL.setVisible(false);
            }
            if (vboxImportSLBk != null && event != null && vboxImportSLBk != null && selectedItem != rSpeciesUploadLSIDBk) {
                vboxImportSLBk.setVisible(false);
            }
            // Check to see if we are perform a normal or background upload
            if (selectedItem == rSpeciesSearch
                    && divSpeciesSearch != null) {
                divSpeciesSearch.setVisible(true);
                vboxMultiple.setVisible(false);
                if (divLifeform != null) divLifeform.setVisible(false);
                if (event != null) {
                    toggles();
                }
                return;
            }
            if (divSpeciesSearch != null) {
                divSpeciesSearch.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadSpecies) {
                btnOk.setVisible(true);
                if (vboxMultiple != null) vboxMultiple.setVisible(false);
                if (divLifeform != null) divLifeform.setVisible(false);
            } else if (selectedItem == rSpeciesUploadLSID) {
                btnOk.setDisabled(true);
                vboxImportSL.setVisible(true);
                if (vboxMultiple != null) vboxMultiple.setVisible(false);
                if (divLifeform != null) divLifeform.setVisible(false);
            } else if (rMultiple != null && rMultiple.isSelected()) {
                vboxMultiple.setVisible(true);
                if (divLifeform != null) divLifeform.setVisible(false);
            } else if (rSpeciesLifeform != null && rSpeciesLifeform.isSelected()) {
                if (vboxMultiple != null) vboxMultiple.setVisible(false);
                divLifeform.setVisible(true);
            } else {
                btnOk.setDisabled(false);
                if (vboxMultiple != null) vboxMultiple.setVisible(false);
                if (divLifeform != null) divLifeform.setVisible(false);
            }

            if (event != null) {
                toggles();
            }

            // set default geospatial kosher checkboxes unless a map layers has
            // been chosen
            MapLayer ml;
            Query q;
            if (rgSpecies.getSelectedItem() != null && rgSpecies.getSelectedItem().getValue() != null
                    && (ml = getMapComposer().getMapLayer(rgSpecies.getSelectedItem().getLabel())) != null
                    && (q = ml.getSpeciesQuery()) != null && q instanceof BiocacheQuery) {
                setGeospatialKosherCheckboxes(((BiocacheQuery) q).getGeospatialKosher());
            } else {
                setGeospatialKosherCheckboxes(defaultGeospatialKosher);
            }
            updateGeospatialKosherCheckboxes();
        } catch (Exception e) {
            LOGGER.error("error from selecting a species radio", e);
        }
    }

    public void onCheck$rgSpeciesBk(Event event) {
        if (rgSpeciesBk == null) {
            return;
        }
        Radio selectedItem = rgSpeciesBk.getSelectedItem();
        try {
            if (selectedItem == null && event != null) {
                selectedItem = (Radio) ((org.zkoss.zk.ui.event.ForwardEvent) event).getOrigin().getTarget();
            }
        } catch (Exception e) {
            LOGGER.error(StringConstants.FAILED_TO_SET_RADIO, e);
        }
        try {

            if (vboxImportSL != null && event != null && selectedItem != rSpeciesUploadLSID) {
                vboxImportSL.setVisible(false);
            }
            if (event != null && vboxImportSLBk != null && selectedItem != rSpeciesUploadLSIDBk) {
                vboxImportSLBk.setVisible(false);
            }

            if (selectedItem == rSpeciesSearchBk
                    && divSpeciesSearchBk != null) {
                divSpeciesSearchBk.setVisible(true);
                vboxMultipleBk.setVisible(false);
                if (event != null) {
                    toggles();
                }
                return;
            }

            if (divSpeciesSearchBk != null) {
                divSpeciesSearchBk.setVisible(false);
            }

            if (selectedItem == rSpeciesUploadLSIDBk) {
                btnOk.setDisabled(true);
                vboxImportSLBk.setVisible(true);
                vboxMultiple.setVisible(false);
            } else if (selectedItem == rSpeciesUploadSpeciesBk) {
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
            MapLayer ml;
            Query q;
            if (rgSpeciesBk.getSelectedItem() != null && rgSpeciesBk.getSelectedItem().getValue() != null
                    && (ml = getMapComposer().getMapLayer(rgSpeciesBk.getSelectedItem().getLabel())) != null
                    && (q = ml.getSpeciesQuery()) != null && q instanceof BiocacheQuery) {
                setGeospatialKosherCheckboxesBk(((BiocacheQuery) q).getGeospatialKosher());
            } else {
                setGeospatialKosherCheckboxesBk(defaultGeospatialKosher);
            }

            updateGeospatialKosherCheckboxesBk();
        } catch (Exception e) {
            LOGGER.error("error selecting background species radio", e);
        }
    }

    public void onValueSelected$searchSpeciesACComp(Event event) {
        toggles();
    }

    public void onClick$btnHelp(Event event) {
        String helpurl = "";

        if (StringConstants.PREDICTION.equals(selectedMethod)) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-prediction-tab/";
        } else if (StringConstants.SAMPLING.equals(selectedMethod)) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-sampling-tab/";
        } else if (StringConstants.CLASSIFICATION.equals(selectedMethod)) {
            helpurl = "http://www.ala.org.au/spatial-portal-help/analysis-classification-tab/";
        } else if ("Scatterplot".equals(selectedMethod)) {
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

        Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);
        Div previousDiv = currentStep > 1 ? (Div) getFellowIfAny(StringConstants.ATSTEP + (currentStep - 1)) : null;

        if (currentDiv.getZclass().contains(StringConstants.FIRST)) {
            if (btnBack != null) {
                btnBack.setDisabled(true);
            }
        } else {
            currentDiv.setVisible(false);
            previousDiv.setVisible(true);

            Html currentStepCompletedImg = (Html) getFellowIfAny(StringConstants.IMG_COMPLETED_STEP + (currentStep - 1));
            currentStepCompletedImg.setVisible(false);

            Label nextStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep));
            nextStepLabel.setStyle(StringConstants.FONT_WEIGHT_NORMAL);

            Label currentStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep - 1));
            currentStepLabel.setStyle(StringConstants.FONT_WEIGHT_BOLD);

            currentStep--;

            if (previousDiv != null && btnBack != null) {
                btnBack.setDisabled(previousDiv.getZclass().contains(StringConstants.FIRST));
            }
        }

        btnOk.setLabel(StringConstants.NEXT_GT);
        toggles();
        updateWindowTitle();
        displayTrafficLightInfo();

    }

    private void displayTrafficLightInfo() {
        if (tlinfo != null) {
            if (StringConstants.PREDICTION.equalsIgnoreCase(selectedMethod) && currentStep == 3) {
                tlinfo.setVisible(true);
            } else if (StringConstants.CLASSIFICATION.equalsIgnoreCase(selectedMethod) && currentStep == 2) {
                tlinfo.setVisible(true);
            } else if (StringConstants.GDM.equalsIgnoreCase(selectedMethod) && currentStep == 3) {
                tlinfo.setVisible(true);
            } else if (StringConstants.SAMPLING.equalsIgnoreCase(selectedMethod) && currentStep == 3) {
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
            if (type.compareTo(StringConstants.ASSEMBLAGE_BK) == 0) {
                setMultipleLsidsBk(lsid);
            }
        } catch (Exception e) {
            LOGGER.error("Exception when resetting analysis window", e);
        }
    }

    public void resetWindow(String selectedArea) {
        try {

            hasCustomArea = !(selectedArea == null || selectedArea.trim().isEmpty());

            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && !layers.isEmpty()) {
                    curTopArea = layers.get(0);
                } else {
                    curTopArea = null;
                }

                if (curTopArea != prevTopArea) {
                    if (isAreaHighlightTab()) {
                        loadAreaHighlightLayers(curTopArea.getDisplayName());
                    } else if (isAreaTab()) {
                        //multiple areas can be defined
                        if (getFellow("cAreaCurrent") != null) {
                            loadAreaLayersCheckboxes(curTopArea.getDisplayName());
                        } else {
                            loadAreaLayers(curTopArea.getDisplayName());
                        }
                    }

                    ok = true;
                }
            }
            this.setTop(winTop);
            this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                //not multiple areas can be defined
                if (getFellow("cAreaCurrent") == null) {
                    onClick$btnOk(null);
                } else if (rAreaCustom != null) {
                    rAreaCustom.setSelected(false);
                }
                hasCustomArea = false;
            }

            fixFocus();
        } catch (Exception ex) {
            LOGGER.error("Exception when resetting analysis window", ex);
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
                winProps.put(StringConstants.PARENT, this);
                winProps.put(StringConstants.PARENTNAME, "Tool");
                winProps.put(StringConstants.SELECTEDMETHOD, selectedMethod);

                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && !layers.isEmpty()) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/add/AddArea.zul", this, winProps);
                window.setAttribute("winProps", winProps, true);
                window.setParent(this);
                window.doModal();

                return;
            }

            Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);
            Div nextDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + (currentStep + 1));

            //check to see if one of the "create new species list" radio buttons is seleceted
            //ONLY perform these checks if the "lMultiple" or lMultipleBk" is on the current page
            if (Components.isAncestor(currentDiv, lMultiple) && rMultiple.isChecked()) {
                // display the dialog and change the radio button to "use existing"...
                if (Util.getUserEmail() != null && !"guest@ala.org.au".equals(Util.getUserEmail())) {
                    showExportSpeciesListDialog(lMultiple);
                    return;
                }
            } else if (Components.isAncestor(currentDiv, lMultipleBk) && rMultipleBk.isChecked()) {
                if (Util.getUserEmail() != null && !"guest@ala.org.au".equals(Util.getUserEmail())) {
                    showExportSpeciesListDialog(lMultipleBk);
                    return;
                }
            }

            if (!currentDiv.getZclass().contains(StringConstants.LAST)) {
                if (currentDiv.getZclass().contains(StringConstants.SPECIES) && (rSpeciesUploadSpecies != null && rSpeciesUploadSpecies.isSelected())) {
                    Boolean test = currentDiv.getZclass().contains(StringConstants.SPECIES) && rSpeciesUploadSpecies.isSelected();
                    LOGGER.debug("test=" + test);
                    onClick$btnUpload(event);

                } else {
                    currentDiv.setVisible(false);
                    nextDiv.setVisible(true);

                    Html previousStepCompletedImg = (Html) getFellowIfAny(StringConstants.IMG_COMPLETED_STEP + (currentStep));
                    previousStepCompletedImg.setVisible(true);

                    Label previousStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep));
                    previousStepLabel.setStyle(StringConstants.FONT_WEIGHT_NORMAL);

                    Label currentStepLabel = (Label) getFellowIfAny(StringConstants.LBLSTEP + (currentStep + 1));
                    currentStepLabel.setStyle(StringConstants.FONT_WEIGHT_BOLD);

                    // now include the extra options for step 4
                    if (nextDiv != null) {

                        if (nextDiv.getZclass().contains(StringConstants.LAST)) {
                            loadSummaryDetails();
                            onLastPanel();
                        }
                        btnOk.setLabel(StringConstants.NEXT_GT);
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
                        boolean test = includeAnalysisLayersForAnyQuery || (q instanceof UserDataQuery);

                        if (selectedLayersCombobox != null
                                && (selectedLayersCombobox.getIncludeAnalysisLayers()) != test) {
                            selectedLayersCombobox.init(getMapComposer().getLayerSelections(), getMapComposer(), test);
                        }
                        if (lbListLayers != null
                                && (lbListLayers.getIncludeAnalysisLayers()) != test) {
                            String[] selectedLayers = lbListLayers.getSelectedLayers();
                            lbListLayers.init(getMapComposer(), test, !includeContextualLayers, singleLayerDomain);
                            lbListLayers.updateDistances();

                            if (selectedLayers != null && selectedLayers.length > 0) {
                                lbListLayers.selectLayers(selectedLayers);
                            }

                            lbListLayers.renderAll();
                        }
                        if (cbLayer != null && (cbLayer.getIncludeAnalysisLayers()) != test) {
                            cbLayer.setIncludeAnalysisLayers(test);
                        }
                        if (cbLayerEnvironmentalOnly != null
                                && (cbLayerEnvironmentalOnly.getIncludeAnalysisLayers()) != test) {
                            cbLayerEnvironmentalOnly.setIncludeAnalysisLayers(test);
                        }
                        if (cbLayer1 != null
                                && (cbLayer1.getIncludeAnalysisLayers()) != test) {
                            cbLayer1.setIncludeAnalysisLayers(test);
                        }
                        if (cbLayer2 != null
                                && (cbLayer2.getIncludeAnalysisLayers()) != test) {
                            cbLayer2.setIncludeAnalysisLayers(test);
                        }
                        if (cbLayerMix != null
                                && (cbLayerMix.getIncludeAnalysisLayers()) != test) {
                            cbLayerMix.setIncludeAnalysisLayers(test);
                        }
                        if (mpLayer1 != null && mpLayer2 != null && mpLayersIncludeAnalysisLayers != test) {
                            // remove
                            while (!mpLayer1.getChildren().isEmpty()) {
                                mpLayer1.removeChild(mpLayer1.getFirstChild());
                            }
                            while (!mpLayer2.getChildren().isEmpty()) {
                                mpLayer2.removeChild(mpLayer2.getFirstChild());
                            }
                            // add
                            for (MapLayer ml : getMapComposer().getGridLayers()) {
                                addToMpLayers(ml, false);
                            }
                            mpLayersIncludeAnalysisLayers = test;
                            if (mpLayersIncludeAnalysisLayers) {
                                for (MapLayer ml : getMapComposer().getAnalysisLayers()) {
                                    if (ml.getSubType() != LayerUtilitiesImpl.ALOC) {
                                        addToMpLayers(ml, true);
                                    }
                                }
                            }
                        }
                    }
                }
                if (nextDiv != null && nextDiv.getZclass().contains(StringConstants.LAST)) {
                    updateLayerListText();
                }

                if (btnBack != null) {
                    btnBack.setDisabled(false);
                }
                updateWindowTitle();
            }

        } catch (Exception e) {
            LOGGER.error("error progressing to next screen of tool", e);
        }

        toggles();
        displayTrafficLightInfo();

        fixFocus();
    }

    void fixFocus() {
        //default is not focus fix
    }

    public void onLastPanel() {
        //default is no action on the last panel
    }

    public boolean onFinish() {
        this.detach();
        Messagebox.show("Running your analysis tool: " + selectedMethod);

        return true;
    }

    public long getEstimate() {
        return -1;
    }

    public void onClick$bLogin(Event event) {
        getMapComposer().activateLink("https://auth.ala.org.au/cas/login", "Login", false);

    }

    public void checkEstimate() {
        try {

            long estimate = getEstimate();
            double minutes = estimate / 60000.0;
            String estimateInMin;
            if (minutes < 0.5) {
                estimateInMin = "< 1 minute";
            } else {
                estimateInMin = ((int) Math.ceil(minutes)) + " minutes";
            }

            LOGGER.debug("Got estimate for process: " + estimate);
            LOGGER.debug("Estimate in minutes: " + estimateInMin);

            // String message = "Your process is going to take approximately " +
            lEstimateMessage.setValue(estimateInMin);

            hasEstimated = true;
            isBackgroundProcess = true;
            if (estimate > 300000 && !isUserLoggedIn()) {
                notLoggedIn.setVisible(true);
                isLoggedIn.setVisible(false);

                hasEstimated = false;

                return;
            }

            notLoggedIn.setVisible(false);
            isLoggedIn.setVisible(true);

        } catch (Exception e) {
            LOGGER.warn("Unable to get estimate for the process", e);
        }
    }

    public boolean isUserLoggedIn() {
        String authCookie = Util.getUserEmail();
        return !"guest@ala.org.au".equals(authCookie);
    }

    public SelectedArea getSelectedArea() {
        String area = rAreaSelected.getValue();
        SelectedArea sa = null;
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
                    if (area == null || area.equals(ml.getName())) {
                        sa = new SelectedArea(ml, null);
                        break;
                    }
                }

            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve selected area", e);
        }

        return sa;
    }

    public SelectedArea getSelectedAreaHighlight() {
        String area = rgAreaHighlight.getSelectedItem().getValue();

        SelectedArea sa = null;
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

        return sa;
    }

    public Query getSelectedSpecies() {
        return getSelectedSpecies(false, true);
    }

    public Query getSelectedSpecies(boolean mapspecies, boolean applycheckboxes) {
        Query q = null;

        if (rgSpecies == null) {
            return q;
        }
        Radio r = rgSpecies.getSelectedItem();
        if (r == null) {
            if (rgSpecies.getItemCount() == 0) {
                return q;
            }
            LOGGER.error("rgSpecies item is not selected. step=" + currentStep + " method=" + getTitle() + " rgSpeces item at [0]=" + (rgSpecies != null && rgSpecies.getItemCount() > 0 ? rgSpecies.getItemAtIndex(0).getValue() : "null or no species items in list"));
            r = rgSpecies.getItemAtIndex(0);
        }
        String species = r.getValue();
        String id = rgSpecies.getSelectedItem().getId();
        LOGGER.debug("getSelectedSpecies.species: " + species);

        MapLayer ml = getMapComposer().getMapLayer(species);
        if (ml != null) {
            q = ml.getSpeciesQuery();
            if (q instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) q;
                q = bq.newFacetGeospatialKosher(applycheckboxes ? getGeospatialKosher() : null, false);
            }
        } else {
            try {
                LOGGER.debug("getSelectedSpecies: " + species);
                LOGGER.debug("tool is: " + (tToolName == null ? StringConstants.NULL : tToolName.getValue()));
                if ("allspecies".equals(species)) {
                    q = new BiocacheQuery(null, null, null, null, false, applycheckboxes ? getGeospatialKosher() : null);
                } else if (ml == null && "allmapped".equals(species)) {

                    throw new UnsupportedOperationException("Not yet implemented");
                } else if (StringConstants.MULTIPLE.equals(species)) {
                    String lsids = getMultipleLsids();
                    LOGGER.debug("getSelectedSpecies.lsids: " + lsids);
                    if (lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids, null, null, null, false, applycheckboxes ? getGeospatialKosher() : null);
                        LOGGER.debug("getSelectedSpecies.query is now set");
                    }
                } else if ("uploadLsid".equals(species)) {
                    //get the query from species list list
                    SpeciesListListbox lb = id.endsWith("Bk") ? speciesListListboxBk : speciesListListbox;
                    q = lb.extractQueryFromSelectedLists(applycheckboxes ? getGeospatialKosher() : null);

                } else if ((StringConstants.SEARCH.equals(species) || StringConstants.UPLOAD_SPECIES.equals(species))
                        && searchSpeciesACComp.hasValidItemSelected()) {
                    if (!searchSpeciesACComp.hasValidAnnotatedItemSelected()) {
                        LOGGER.debug("error in getSelectedSpecies value=" + searchSpeciesACComp.getAutoComplete().getSelectedItem().getValue() + " text=" + searchSpeciesACComp.getAutoComplete().getText());
                    } else {
                        q = searchSpeciesACComp.getQuery((Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS), false, applycheckboxes ? getGeospatialKosher() : null);
                    }
                } else if ("lifeform".equalsIgnoreCase(species)) {
                    q = new BiocacheQuery(null, null, null,
                            Arrays.asList(new Facet[]{new Facet("species_group", cbLifeform.getValue(), true)})
                            , mapspecies, applycheckboxes ? getGeospatialKosher() : null);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to retrieve selected species", e);
            }
        }

        return q;
    }

    public Query getSelectedSpeciesBk() {
        return getSelectedSpeciesBk(false, true);
    }

    public Query getSelectedSpeciesBk(boolean mapspecies, boolean applycheckboxes) {
        Query q = null;

        if (rgSpeciesBk == null) {
            return q;
        }
        Radio r = rgSpeciesBk.getSelectedItem();
        if (r == null) {
            if (rgSpeciesBk.getItemCount() == 0) {
                return q;
            }
            LOGGER.error("rgSpeciesBk item is not selected. step=" + currentStep + " method=" + getTitle() + " rgSpeces item at [0]=" + (rgSpeciesBk != null && rgSpeciesBk.getItemCount() > 0 ? rgSpeciesBk.getItemAtIndex(0).getValue() : "null or no species items in list"));
            r = rgSpeciesBk.getItemAtIndex(0);
        }

        String species = r.getValue();
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
                if ("allspecies".equals(species)) {

                    q = new BiocacheQuery(null, null, null, null, false, applycheckboxes ? getGeospatialKosherBk() : null);
                } else if ("allmapped".equals(species)) {

                    throw new UnsupportedOperationException("Not yet implemented");
                } else if (StringConstants.MULTIPLE.equals(species)) {
                    String lsids = getMultipleLsidsBk();
                    if (lsids != null && lsids.length() > 0) {
                        q = new BiocacheQuery(lsids, null, null, null, false, applycheckboxes ? getGeospatialKosherBk() : null);
                    }
                } else if ("uploadLsid".equals(species)) {
                    //get the query from species list list
                    SpeciesListListbox lb = id.endsWith("Bk") ? speciesListListboxBk : speciesListListbox;
                    q = lb.extractQueryFromSelectedLists(applycheckboxes ? getGeospatialKosher() : null);

                } else if ((StringConstants.SEARCH.equals(species) || StringConstants.UPLOAD_SPECIES.equals(species))
                        && bgSearchSpeciesACComp.hasValidItemSelected()) {
                    q = bgSearchSpeciesACComp.getQuery((Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS), false, applycheckboxes ? getGeospatialKosherBk() : null);
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to retrieve selected species", e);
            }
        }

        return q;
    }

    public String getSelectedSpeciesName() {
        String species = rgSpecies.getSelectedItem().getValue();
        try {
            if ("allmapped".equals(species)) {

                species = "All mapped species";
            } else if (StringConstants.SEARCH.equals(species)) {
                if (searchSpeciesACComp.hasValidItemSelected()) {
                    species = searchSpeciesACComp.getAutoComplete().getText();
                }
            } else if ("uploadLsid".equals(species)) {
                //get the query from species list list
                String id = rgSpecies.getSelectedItem().getId();
                SpeciesListListbox lb = id.endsWith("Bk") ? speciesListListboxBk : speciesListListbox;
                species = lb.getSelectedNames();
            } else if ("lifeform".equalsIgnoreCase(species)) {
                species = cbLifeform.getText();
            } else {
                species = rgSpecies.getSelectedItem().getLabel();
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve selected species", e);
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
            LOGGER.warn("Unable to retrieve selected layers", e);
        }

        return layers;
    }

    void setLsid(String lsidName) {
        try {

            LOGGER.debug("lsidName: " + lsidName);

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
                        if (ci.getLabel().equalsIgnoreCase(searchSpeciesACComp.getAutoComplete().getValue())
                                && ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0).equals(lsid)) {
                            searchSpeciesACComp.getAutoComplete().setSelectedItem(ci);
                            break;
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
            LOGGER.warn("Error setting lsid:", e);
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
            String tmpSpecies = species;
            bgSearchSpeciesACComp.getAutoComplete().setValue(tmpSpecies);
            bgSearchSpeciesACComp.getAutoComplete().refresh(tmpSpecies);

            if (!bgSearchSpeciesACComp.hasValidItemSelected()) {
                List list = bgSearchSpeciesACComp.getAutoComplete().getItems();
                for (int i = 0; i < list.size(); i++) {
                    Comboitem ci = (Comboitem) list.get(i);
                    // compare name
                    if (ci.getLabel().equalsIgnoreCase(bgSearchSpeciesACComp.getAutoComplete().getValue())
                            && ci.getAnnotatedProperties() != null && ci.getAnnotatedProperties().get(0).equals(lsid)) {
                        bgSearchSpeciesACComp.getAutoComplete().setSelectedItem(ci);
                        break;
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

        btnOk.setDisabled(false);
        btnOk.setVisible(true);

        if (fileUpload != null) {
            fileUpload.setVisible(false);
        }
        Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);
        if (lbListLayers != null) {

            if (currentDiv.getZclass().contains("minlayers1")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 1);
            } else if (currentDiv.getZclass().contains("minlayers2")) {
                btnOk.setDisabled(lbListLayers.getSelectedCount() < 2);
            } else if (currentDiv.getZclass().contains(StringConstants.OPTIONAL)) {
                btnOk.setDisabled(false);
            }
            // test for max
            if (currentDiv.getZclass().contains(StringConstants.MAX_LAYERS)) {
                int start = currentDiv.getZclass().indexOf(StringConstants.MAX_LAYERS) + StringConstants.MAX_LAYERS.length();
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
                btnOk.setDisabled(cbLayer2.getSelectedItem() == null || cbLayer1.getSelectedItem() == null);
            }

            if (currentDiv.getZclass().contains(StringConstants.OPTIONAL)) {
                btnOk.setDisabled(false);
            }

            if (currentDiv.getZclass().contains(StringConstants.SPECIES)) {
                if (divSpeciesSearch.isVisible()) {
                    btnOk.setDisabled(!searchSpeciesACComp.hasValidAnnotatedItemSelected());
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

        if (lbListLayers != null
                && bLayerListDownload1 != null && bLayerListDownload2 != null) {
            bLayerListDownload1.setDisabled(lbListLayers.getSelectedCount() == 0);
            bLayerListDownload2.setDisabled(lbListLayers.getSelectedCount() == 0);
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
        return rAreaSelected.getLabel();
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
        return isAreaHighlightTab() && rgAreaHighlight != null && StringConstants.RAREACUSTOMHIGHLIGHT.equals(rgAreaHighlight.getSelectedItem().getId());
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
                        PasteLayerListController window = (PasteLayerListController) Executions.createComponents("WEB-INF/zul/input/PasteLayerList.zul", this, null);
                        window.setCallback(new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                selectLayerFromList((String) event.getData());
                                updateLayerSelectionCount();
                            }
                        });

                        try {
                            window.setParent(this);
                            window.doModal();
                        } catch (Exception e) {
                            LOGGER.error("error opening PasteLayerList.zul", e);
                        }
                    } else if (ci.getLabel().toLowerCase().contains("import")) {
                        UploadLayerListController window = (UploadLayerListController) Executions.createComponents("WEB-INF/zul/input/UploadLayerList.zul", this, null);
                        window.setCallback(new EventListener() {

                            @Override
                            public void onEvent(Event event) throws Exception {
                                selectLayerFromList((String) event.getData());
                                updateLayerSelectionCount();
                            }
                        });

                        try {
                            window.setParent(this);
                            window.doModal();
                        } catch (Exception e) {
                            LOGGER.error("error opening UploadLayerList.zul", e);
                        }
                    }
                    selectedLayersCombobox.setSelectedIndex(-1);
                }
            }
            selectLayerFromList(layersList);
        }
    }

    public void selectLayerFromList(String layersList) {
        String newLayersList = layersList;
        if (newLayersList == null) {
            return;
        }

        // support new line separated layer names
        newLayersList = newLayersList.replace("\n", ",");

        // check the whole layer string as well as the one at the end
        String[] layers = newLayersList.split(",");
        String[] list = new String[layers.length * 2];
        for (int i = 0; i < layers.length; i++) {
            int p1 = layers[i].lastIndexOf('(');
            int p2 = layers[i].lastIndexOf(')');
            if (p1 >= 0 && p2 >= 0 && p1 < p2) {
                list[i * 2] = CommonData.getLayerFacetName(layers[i].substring(p1 + 1, p2).trim());
            }
            list[i * 2 + 1] = CommonData.getLayerFacetName(layers[i]);
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
                Cookie c = new Cookie("analysis_layer_selections", URLEncoder.encode(sb.toString(), StringConstants.UTF_8));
                c.setMaxAge(Integer.MAX_VALUE);
                ((HttpServletResponse) Executions.getCurrent().getNativeResponse()).addCookie(c);
            } catch (Exception e) {
                LOGGER.error("error encoding cookies from UTF-8: " + sb.toString());
            }
        }
    }

    void updateLayerListText() {
        try {
            if (lbListLayers != null && lbListLayers.getSelectedCount() > 0 && tLayerList != null) {
                String lyrtext = getLayerListText();
                LOGGER.debug("Setting Layer text: \n" + lyrtext);
                tLayerList.setText(lyrtext);
                if (dLayerSummary != null) {
                    dLayerSummary.setVisible(tLayerList.getText().length() > 0);
                }
            }
        } catch (Exception e) {
            LOGGER.error("error selecting layers", e);
        }
    }

    String getLayerListText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                if (sb.length() > 0) {
                    sb.append(",");
                }

                String displayname = CommonData.getLayerDisplayName(s);
                if (displayname != null && displayname.length() > 0) {
                    sb.append(displayname).append(" (").append(s).append(")");
                } else {
                    sb.append(s);
                }
            } catch (Exception e) {
                LOGGER.error("error geting layer list as text", e);
            }
        }
        return sb.toString();
    }

    String getLayerListShortText() {
        StringBuilder sb = new StringBuilder();
        for (String s : lbListLayers.getSelectedLayers()) {
            try {
                String displayname = CommonData.getFacetLayerDisplayNameDefault(CommonData.getLayerFacetName(s));
                if (displayname != null && displayname.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(s);
                }
            } catch (Exception e) {
                LOGGER.error("error getting list of layer short names", e);
            }
        }
        return sb.toString();
    }

    public void onChange$cbLayer(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayer.getSelectedItem() != null) {
            JSONObject jo = cbLayer.getSelectedItem().getValue();
            String[] layer = jo.get(StringConstants.NAME).toString().split("/");
            lbListLayers.selectLayers(layer);
            cbLayer.setSelectedIndex(-1);
        }
        toggles();
    }

    public void onChange$cbLayerEnvironmentalOnly(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayerEnvironmentalOnly.getSelectedItem() != null) {
            JSONObject jo = cbLayerEnvironmentalOnly.getSelectedItem().getValue();
            String[] layer = jo.get(StringConstants.NAME).toString().split("/");
            lbListLayers.selectLayers(layer);
            cbLayerEnvironmentalOnly.setSelectedIndex(-1);
        }
        toggles();
    }

    public void onChange$cbLayerMix(Event event) {
        // seek to and select the same layer in the list
        if (lbListLayers != null && cbLayerMix.getSelectedItem() != null) {
            JSONObject jo = cbLayerMix.getSelectedItem().getValue();
            String[] layer = jo.get(StringConstants.NAME).toString().split("/");
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
        SimpleDateFormat sdf = new SimpleDateFormat(StringConstants.DATE_TIME_FILE);
        Filedownload.save(getLayerListShortText(), StringConstants.TEXT_PLAIN, "layer_selection_" + sdf.format(new Date()) + ".txt");
    }

    public void updateLayerSelectionCount() {
        if (lLayersSelected != null && lbListLayers != null) {
            if (lbListLayers.getSelectedCount() == 1) {
                lLayersSelected.setValue("1 layer selected");
            } else {
                // test for max
                String error = "";
                Div currentDiv = (Div) getFellowIfAny(StringConstants.ATSTEP + currentStep);
                if (currentDiv.getZclass().contains(StringConstants.MAX_LAYERS)) {
                    int start = currentDiv.getZclass().indexOf(StringConstants.MAX_LAYERS) + StringConstants.MAX_LAYERS.length();
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
                if (currentDiv.getZclass().contains(StringConstants.MIN_LAYERS)) {
                    int start = currentDiv.getZclass().indexOf(StringConstants.MIN_LAYERS) + StringConstants.MIN_LAYERS.length();
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
        UserDataDTO ud = null;

        if (event instanceof UploadEvent) {
            ue = (UploadEvent) event;
        } else if (event instanceof ForwardEvent) {
            ue = (UploadEvent) ((ForwardEvent) event).getOrigin();
        }
        Media m = ue.getMedia();
        if (ud == null) {
            ud = new UserDataDTO(m.getName());
        }
        if (ud.getName().trim().isEmpty()) {
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
        usc.setAddToMap(false);

        if (rgSpeciesBk != null) {
            if (rgSpeciesBk.getSelectedItem() == rSpeciesUploadLSIDBk) {
                usc.setUploadType(StringConstants.ASSEMBLAGE_BK);
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
            LOGGER.error("unable to upload file");
            return;
        } else {
            LOGGER.debug("fileUploaded()");
        }
        try {
            Media m = ue.getMedia();

            boolean loaded = false;
            try {
                loadLayerList(m.getReaderData());
                loaded = true;
                LOGGER.debug("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //failed to read user uploaded data, try another method
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(new String(m.getByteData())));
                    loaded = true;
                    LOGGER.debug("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //failed to read user uploaded data, try another method
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new InputStreamReader(m.getStreamData()));
                    loaded = true;
                    LOGGER.debug("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //failed to read user uploaded data, try another method
                }
            }
            if (!loaded) {
                try {
                    loadLayerList(new StringReader(m.getStringData()));

                    LOGGER.debug("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    // last one, report error
                    getMapComposer().showMessage("Unable to load your file.");
                    LOGGER.error("unable to load user layer list: ", e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
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

                    String sciname = searchResult.get(StringConstants.SCIENTIFIC_NAME);
                    String family = searchResult.get(StringConstants.FAMILY);
                    String kingdom = searchResult.get(StringConstants.KINGDOM);
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
                    LOGGER.error("error with species autocomplete", e);
                }
            }
        }
        toggles();
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

                    String sciname = searchResult.get(StringConstants.SCIENTIFIC_NAME);
                    String family = searchResult.get(StringConstants.FAMILY);
                    String kingdom = searchResult.get(StringConstants.KINGDOM);
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
                    LOGGER.error("error with species autocomplete for background species (scatterplot)", e);
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
        List<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            if (s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = (JSONArray) searchResult.get(StringConstants.VALUES);

                    String sciname = "", family = "", kingdom = "", lsid = null;
                    for (int j = 0; j < ja.size(); j++) {
                        if (StringConstants.SCIENTIFIC_NAME.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            sciname = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.FAMILY.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            family = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.KINGDOM.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            kingdom = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.TAXON_CONCEPT_ID.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            lsid = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                    }

                    // is 's' an LSID?
                    if ((lsid == null || lsid.length() == 0) && s.matches(".*[0-9].*")) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if (!sr.isEmpty() && sr.get(StringConstants.SCIENTIFIC_NAME) != null && sr.get(StringConstants.SCIENTIFIC_NAME).length() > 0) {
                            lsid = s;
                            sciname = sr.get(StringConstants.SCIENTIFIC_NAME);
                            family = sr.get(StringConstants.FAMILY);
                            kingdom = sr.get(StringConstants.KINGDOM);
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

        if (!notFound.isEmpty()) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    public void onClick$bMultipleBk(Event event) {
        String[] speciesNames = tMultipleBk.getText().replace("\n", ",").split(",");
        List<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            if (s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = (JSONArray) searchResult.get(StringConstants.VALUES);

                    String sciname = "", family = "", kingdom = "", lsid = null;
                    for (int j = 0; j < ja.size(); j++) {
                        if (StringConstants.SCIENTIFIC_NAME.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            sciname = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.FAMILY.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            family = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.KINGDOM.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            kingdom = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                        if (StringConstants.TAXON_CONCEPT_ID.equals(((JSONObject) ja.get(j)).get(StringConstants.NAME))) {
                            lsid = ((JSONObject) ja.get(j)).get(StringConstants.PROCESSED).toString();
                        }
                    }

                    // is 's' an LSID?
                    if (lsid == null || lsid.length() == 0) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if (!sr.isEmpty() && sr.get(StringConstants.SCIENTIFIC_NAME) != null && sr.get(StringConstants.SCIENTIFIC_NAME).length() > 0) {
                            lsid = s;
                            sciname = sr.get(StringConstants.SCIENTIFIC_NAME);
                            family = sr.get(StringConstants.FAMILY);
                            kingdom = sr.get(StringConstants.KINGDOM);
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

        if (!notFound.isEmpty()) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        toggles();
    }

    JSONObject processAdhoc(String scientificName) {
        String url = CommonData.getBiocacheServer() + "/process/adhoc";
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            StringRequestEntity sre = new StringRequestEntity("{ \"scientificName\": \"" + scientificName.replace("\"", "'") + "\" } ", StringConstants.APPLICATION_JSON, StringConstants.UTF_8);
            post.setRequestEntity(sre);
            int result = client.executeMethod(post);
            if (result == 200) {
                JSONParser jp = new JSONParser();
                return (JSONObject) jp.parse(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error("error processing species request: " + url + ", scientificName=" + scientificName, e);
        }
        return null;
    }

    private void addTolMultiple(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for (Listitem li : lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if ((lsidCell.getLabel().equals(lsid))
                    || (scinameCell.getLabel().replace("(not found)", "").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        // remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

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
            lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

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

        if (insertAtBeginning && !lMultiple.getChildren().isEmpty()) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }

    private void addTolMultipleBk(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for (Listitem li : lMultipleBk.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if ((lsidCell.getLabel().equals(lsid))
                    || (scinameCell.getLabel().replace("(not found)", "").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        // remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

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
            lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

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

        if (insertAtBeginning && !lMultipleBk.getChildren().isEmpty()) {
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
            LOGGER.warn("Error setting lsid:", e);
        }
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
            LOGGER.warn("Error setting lsid:", e);
        }
    }

    public void setIncludeAnalysisLayersForUploadQuery(boolean includeAnalysisLayersForUploadQuery) {
        this.includeAnalysisLayersForUploadQuery = includeAnalysisLayersForUploadQuery;
    }

    public void setIncludeAnalysisLayersForAnyQuery(boolean includeAnalysisLayersForAnyQuery) {
        this.includeAnalysisLayersForAnyQuery = includeAnalysisLayersForAnyQuery;
    }

    public void onClick$btnUpload(Event event) {
        try {
            LOGGER.debug("onClick$btnUpload(Event event)");
            if (StringUtils.isNotEmpty((String) CommonData.getSettings().getProperty("sandbox.url", null))
                    && CommonData.getSettings().getProperty("import.points.layers-service", "false").equals("false")) {
                SandboxPasteController spc = (SandboxPasteController) Executions.createComponents("WEB-INF/zul/sandbox/SandboxPaste.zul", getMapComposer(), null);
                spc.setAddToMap(true);
                spc.setParent(getMapComposer());
                spc.doModal();
            } else {
                UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/input/UploadSpecies.zul", this, null);

                if (rSpeciesUploadSpecies.isSelected()) {
                    usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
                } else if (rSpeciesUploadLSID.isSelected()) {
                    usc.setTbInstructions("3. Select file (text file, one LSID or name per line)");
                } else {
                    usc.setTbInstructions("3. Select file");
                }
                usc.setAddToMap(true);
                usc.setParent(this);

                usc.doModal();
            }
        } catch (Exception e) {
            LOGGER.error("file upload error", e);
        }
    }

    public void setChooseEndemic(boolean choose) {
        chkEndemicSpecies.setChecked(choose);
    }

    public boolean getIsEndemic() {
        return chkEndemicSpecies != null && chkEndemicSpecies.isChecked();
    }

    /**
     * TODO NC 2013-08-15: Remove the need for Constants.FALSE as the third item in the array.
     *
     * @return
     */
    public boolean[] getGeospatialKosher() {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return new boolean[]{true, true, false};
        } else {
            return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
        }
    }

    /**
     * TODO NC 2013-08-15: Remove the need for Constants.FALSE as the third item in the array.
     *
     * @return
     */
    public boolean[] getGeospatialKosherBk() {
        if (chkGeoKosherTrueBk == null || chkGeoKosherFalseBk == null) {
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher background species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return new boolean[]{true, true, false};
        } else {
            return new boolean[]{chkGeoKosherTrueBk.isChecked(), chkGeoKosherFalseBk.isChecked(), false};
        }
    }

    void updateGeospatialKosherCheckboxes() {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        // get selected species
        Query q = getSelectedSpecies(false, true);
        if (q != null) {
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
    }

    public void setGeospatialKosherCheckboxes(boolean[] geospatialKosher) {
        if (chkGeoKosherTrue == null || chkGeoKosherFalse == null) {
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
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
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher background species.  Tool: "
                    + ((tToolName == null) ? "'also missing tToolName textbox'" : tToolName.getValue()));
            return;
        }

        // get selected species
        Query q = getSelectedSpeciesBk(false, true);
        if (q != null) {
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
    }

    public void setGeospatialKosherCheckboxesBk(boolean[] geospatialKosher) {
        if (chkGeoKosherTrueBk == null || chkGeoKosherFalseBk == null) {
            LOGGER.warn("Error in ToolComposer.  Expect checkboxes for geospatial kosher species.  Tool: "
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
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherFalse.isChecked()) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherTrue.isChecked()) {
            chkGeoKosherTrue.setChecked(true);
        }
    }


    public void onCheck$chkGeoKosherTrueBk(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherFalseBk.isChecked()) {
            chkGeoKosherFalseBk.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalseBk(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherTrueBk.isChecked()) {
            chkGeoKosherTrueBk.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherNullBk(Event event) {
        Event evt = ((ForwardEvent) event).getOrigin();
        if (!((CheckEvent) evt).isChecked() && !chkGeoKosherTrueBk.isChecked()) {
            chkGeoKosherTrueBk.setChecked(true);
        }
    }

    public void onClick$bAssemblageExport(Event event) {

        SimpleDateFormat sdf = new SimpleDateFormat(StringConstants.DATE_TIME_FILE);
        Filedownload.save(getLsids(), StringConstants.TEXT_PLAIN, "species_assemblage_" + sdf.format(new Date()) + ".txt");
    }

    /**
     * Shows a "create species list" dialog for the supplied list box
     *
     * @param lb
     */
    private void showExportSpeciesListDialog(Listbox lb) {
        String values = getScientificName(lb);
        LOGGER.debug("Creating species list with " + values);
        if (values.length() > 0) {
            UploadToSpeciesListController dialog = (UploadToSpeciesListController) Executions.createComponents("WEB-INF/zul/input/UploadToSpeciesList.zul", this, null);
            dialog.setSpecies(values);

            dialog.setCallback(new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    updateSpeciesListMessage((String) event.getData());
                }
            });

            try {
                dialog.setParent(this);
                dialog.doModal();

            } catch (Exception e) {
                LOGGER.error("Unable to export assemblage", e);
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
        LOGGER.debug("Species list that was created : " + drUid);
        if (drUid != null) {
            boolean isBk = rMultipleBk != null && rMultipleBk.isChecked();
            SpeciesListListbox lb = isBk ? speciesListListboxBk : speciesListListbox;
            Radio r = isBk ? rSpeciesUploadLSIDBk : rSpeciesUploadLSID;

            ((SpeciesListListbox.SpeciesListListModel) lb.getModel()).refreshModel();

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

    public void onClick$bSpeciesListUpload(Event event) {
        UploadLayerListController window = (UploadLayerListController) Executions.createComponents("WEB-INF/zul/input/UploadSpeciesList.zul", this, null);
        window.setCallback(new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                importList((String) event.getData());

                //enable btnOk
                btnOk.setAutodisable("");
                btnOk.setDisabled(false);
            }
        });

        try {
            window.setParent(this);
            window.doModal();
        } catch (Exception e) {
            LOGGER.error("error opening UploadSpeciesList.zul", e);
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
