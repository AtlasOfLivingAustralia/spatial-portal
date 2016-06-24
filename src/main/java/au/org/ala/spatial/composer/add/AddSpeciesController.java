package au.org.ala.spatial.composer.add;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.input.UploadLayerListController;
import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.composer.input.UploadToSpeciesListController;
import au.org.ala.spatial.composer.sandbox.SandboxPasteController;
import au.org.ala.spatial.composer.species.SpeciesAutoCompleteComponent;
import au.org.ala.spatial.composer.species.SpeciesListListbox;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.UtilityComposer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zul.*;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adam
 */
public class AddSpeciesController extends UtilityComposer {
    private static final Logger LOGGER = Logger.getLogger(AddSpeciesController.class);
    private List<String> selectedLists;
    private SpeciesAutoCompleteComponent searchSpeciesACComponent;
    private Button btnOk, bMultiple, bAssemblageExport;
    private Radio rSearch;
    private Radio rMultiple;
    private Radio rUploadCoordinates;
    private Radio rUploadLSIDs;
    private Radio rAllSpecies;
    private Radiogroup rgAddSpecies;
    private Vbox vboxSearch;
    private Checkbox chkArea;
    private Vbox vboxMultiple;
    //the box layout for the import species list
    private Vbox vboxImportSL;
    private SpeciesAutoCompleteComponent mSearchSpeciesACComponent;
    private Textbox tMultiple;
    private Listbox lMultiple;
    private Event event;
    private Checkbox chkGeoKosherTrue, chkGeoKosherFalse;
    private Checkbox chkExpertDistributions;
    private Query query;
    private String rank;
    private String taxon;
    private String multipleSpeciesUploadName = null;
    private boolean prevAreaState = true;
    private boolean loadedAssemblage = false;
    private SpeciesListListbox speciesListListbox;
    private A aMessage;
    private Label lblMessage;

    private boolean listBoxEcho = false;

    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
        chkArea.setChecked(true);
        mSearchSpeciesACComponent.getAutoComplete().setBiocacheOnly(true);

        vboxImportSL = (Vbox) this.getFellow("splistbox").getFellow("vboxImportSL");
        vboxImportSL.getFellow("btnSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onClick$btnSearchSpeciesListListbox(event);

                //make listbox count update correctly by repeating once
                if (listBoxEcho == false) {
                    listBoxEcho = true;
                    Events.echoEvent("onClick", vboxImportSL.getFellow("btnSearchSpeciesListListbox"), null);
                } else {
                    listBoxEcho = false;
                }
            }
        });
        vboxImportSL.getFellow("btnClearSearchSpeciesListListbox").addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onClick$btnClearSearchSpeciesListListbox(event);

                //make listbox count update correctly by repeating once
                if (listBoxEcho == false) {
                    listBoxEcho = true;
                    Events.echoEvent("onClick", vboxImportSL.getFellow("btnClearSearchSpeciesListListbox"), null);
                } else {
                    listBoxEcho = false;
                }
            }
        });
        speciesListListbox = (SpeciesListListbox) this.getFellow("splistbox").getFellow("speciesListListbox");
        //check to see if a user is logged in
        String user = Util.getUserEmail();
        if (user != null && user.length() > 0) {
            rMultiple.setDisabled(false);
        } else {
            rMultiple.setDisabled(true);
            rMultiple.setLabel(rMultiple.getLabel() + " " + CommonData.lang("msg_login_required"));
        }
        speciesListListbox.addEventListener(StringConstants.ONSICHECKBOXCHANGED, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                refreshBtnOkDisabled();
            }
        });

        Map m = Executions.getCurrent().getArg();
        if (m != null) {
            for (Object o : m.entrySet()) {
                if (((Map.Entry) o).getKey() instanceof String
                        && "enableImportAssemblage".equals(((Map.Entry) o).getKey())) {
                    enableImportAssemblage();
                }
            }
        }

    }

    public void onClick$btnSearchSpeciesListListbox(Event event) {
        try {
            ((SpeciesListListbox) event.getTarget().getParent().getParent().getFellowIfAny("speciesListListbox")).onClick$btnSearchSpeciesListListbox(event);
        } catch (Exception e) {
            LOGGER.error("addspeciescontroller is missing speciesListListbox for refreshing", e);
        }
    }

    public void onClick$btnClearSearchSpeciesListListbox(Event event) {
        try {
            ((SpeciesListListbox) event.getTarget().getParent().getParent().getFellowIfAny("speciesListListbox")).onClick$btnClearSearchSpeciesListListbox(event);
        } catch (Exception e) {
            LOGGER.error("addspeciescontroller is missing speciesListListbox for refreshing", e);
        }
    }

    public void onValueSelected$searchSpeciesACComponent(Event event) {
        onChange$searchSpeciesAuto(event);
    }

    public void onClick$btnOk(Event event) {
        Map params = new HashMap();
        params.put(StringConstants.POLYGON_LAYERS, getMapComposer().getPolygonLayers());

        if (btnOk.isDisabled()) {
            return;
        }
        loadedAssemblage = false;
        if (rAllSpecies.isSelected()) {
            AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), params);
            window.setGeospatialKosher(getGeospatialKosher());
            window.setExpertDistributions(chkExpertDistributions.isChecked());
            window.setAllSpecies(true);
            window.loadAreaLayers();
            try {
                window.setParent(getMapComposer());
                window.doModal();
            } catch (Exception e) {
                LOGGER.error("error opening AddSpeciesInArea.zul", e);
            }
        } else if (chkArea.isChecked()) {
            getFromAutocomplete();

            if (rSearch.isSelected()) {
                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), params);
                window.setSpeciesParams(query, rank, taxon);
                window.setExpertDistributions(chkExpertDistributions.isChecked());
                window.loadAreaLayers();
                try {
                    window.setParent(getMapComposer());
                    window.doModal();
                } catch (Exception e) {
                    LOGGER.error("error opening AddSpeciesInArea.zul", e);
                }
            } else if (rMultiple.isSelected()) {
                //when next is pressed we want to export the list to the species list
                if (Util.getUserEmail() != null && !"guest@ala.org.au".equals(Util.getUserEmail())) {
                    showExportSpeciesListDialog();
                    loadedAssemblage = true;
                } else {
                    AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), params);
                    window.setExpertDistributions(chkExpertDistributions.isChecked());
                    //extract all lsids
                    StringBuilder sb = new StringBuilder();
                    for (Listitem li : lMultiple.getItems()) {
                        Listcell lsidCell = (Listcell) li.getLastChild();
                        if (!lsidCell.getLabel().contains("not found")) {
                            if (sb.length() > 0) {
                                sb.append(",");
                            }
                            sb.append(lsidCell.getLabel());
                        }
                    }
                    query = new BiocacheQuery(sb.toString(), null, null, null, null, false, getGeospatialKosher());
                    window.setSpeciesParams(query, rank, taxon);
                    window.setMultipleSpeciesUploadName(CommonData.lang("uploaded_species_list_layer_name"));
                    window.loadAreaLayers();
                    try {
                        window.setParent(getMapComposer());
                        window.doModal();
                    } catch (Exception e) {
                        LOGGER.error("error opening AddSpeciesInArea.zul", e);
                    }
                }

            } else if (rUploadLSIDs.isSelected()) {
                //we need to populate the "create assemblage" with the values from the species list

                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/add/AddSpeciesInArea.zul", getMapComposer(), params);
                //extract all the items for selected species lists
                query = speciesListListbox.extractQueryFromSelectedLists(getGeospatialKosher());
                window.setSpeciesParams(query, rank, taxon);
                window.setMultipleSpeciesUploadName(CommonData.lang("uploaded_species_list_layer_name"));
                window.loadAreaLayers();
                try {
                    window.setParent(getMapComposer());
                    window.doModal();
                } catch (Exception e) {
                    LOGGER.error("error opening AddSpeciesInArea.zul", e);
                }
            } else {
                onClick$btnUpload(event);
            }
        } else {
            if (rSearch.isSelected()) {
                getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComponent, null, getGeospatialKosher(), chkExpertDistributions.isChecked());
            } else if (rUploadLSIDs.isSelected()) {
                //we need to populate the "create assemblage" with the values from the species list
                loadedAssemblage = true;
            } else {
                onClick$btnUpload(event);
            }
        }
        if (!loadedAssemblage) {
            this.detach();
        }
    }

    public void enableImportAssemblage() {
        rUploadLSIDs.setSelected(true);
        onCheck$rgAddSpecies(null);
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$searchSpeciesAuto(Event event) {
        btnOk.setDisabled(true);

        refreshBtnOkDisabled();
    }

    public void onValueSelected$mSearchSpeciesACComponent(Event event) {
        //add to lMultiple
        Comboitem ci = mSearchSpeciesACComponent.getAutoComplete().getSelectedItem();
        if (ci != null && ci.getAnnotatedProperties() != null
                && ci.getAnnotatedProperties().get(0) != null) {
            String annotatedValue = ci.getAnnotatedProperties().get(0);
            if (mSearchSpeciesACComponent.shouldUseRawName()) {
                addTolMultiple(null, annotatedValue, null, null, true);
            } else {
                try {
                    String lsid = annotatedValue;
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

                        mSearchSpeciesACComponent.getAutoComplete().setText("");
                    }
                } catch (Exception e) {
                    LOGGER.error("error with species search autocomplete", e);
                }
            }
        }
        refreshBtnOkDisabled();
    }

    public void onClick$btnUpload(Event event) {
        try {
            if (StringUtils.isNotEmpty((String) CommonData.getSettings().getProperty("sandbox.url", null))
                    && CommonData.getSettings().getProperty("import.points.layers-service", "false").equals("false")) {
                SandboxPasteController spc = (SandboxPasteController) Executions.createComponents("WEB-INF/zul/sandbox/SandboxPaste.zul", getMapComposer(), null);
                spc.setAddToMap(true);
                spc.setParent(getMapComposer());
                spc.doModal();
            } else {
                UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/input/UploadSpecies.zul", getMapComposer(), null);

                if (rUploadCoordinates.isSelected()) {
                    usc.setTbInstructions(CommonData.lang("instruction_upload_species_csv"));
                } else if (rUploadLSIDs.isSelected()) {
                    usc.setTbInstructions(CommonData.lang("instruction_upload_species_lsids"));
                } else {
                    usc.setTbInstructions(CommonData.lang("instruction_upload_species_other"));
                }
                usc.setAddToMap(true);
                usc.setDefineArea(chkArea.isChecked());
                usc.setParent(getMapComposer());
                usc.doModal();
            }
        } catch (Exception e) {
            LOGGER.error("error displaying uploadspecies.zul", e);
        }
    }


    public void onCheck$rgAddSpecies(Event event) {
        if (rSearch.isSelected()) {
            vboxSearch.setVisible(true);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(false);
        } else if (rMultiple.isSelected()) {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(true);
            vboxImportSL.setVisible(false);
            aMessage.setVisible(false);
            lblMessage.setValue("");
        } else if (rUploadLSIDs.isSelected()) {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(true);
        } else {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(false);
        }

        refreshBtnOkDisabled();

        setPosition("center,center");
    }

    private void refreshBtnOkDisabled() {
        if (rSearch.isSelected()) {
            btnOk.setDisabled(!searchSpeciesACComponent.hasValidItemSelected());
        } else if (rMultiple.isSelected()) {
            btnOk.setDisabled(getMultipleLsids().length() == 0 && getNamesWithoutLsids().length == 0);
        } else if (rUploadLSIDs.isSelected()) {
            btnOk.setDisabled(speciesListListbox.getSelectedLists() == null || speciesListListbox.getSelectedLists().isEmpty());
        } else {
            btnOk.setDisabled(false);
        }
    }

    void getFromAutocomplete() {
        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (!searchSpeciesACComponent.hasValidAnnotatedItemSelected()) {
            return;
        }

        taxon = searchSpeciesACComponent.getAutoComplete().getValue();
        rank = "";

        String spVal = searchSpeciesACComponent.getAutoComplete().getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":"));

        } else {
            rank = StringUtils.substringBefore(spVal, ",").toLowerCase();
            LOGGER.debug("mapping rank and species: " + rank + " - " + taxon);
        }
        if (StringConstants.SCIENTIFICNAME.equalsIgnoreCase(rank) || StringConstants.SCIENTIFIC.equalsIgnoreCase(rank)) {
            rank = StringConstants.TAXON;
        }
        query = searchSpeciesACComponent.getQuery((Map) getMapComposer().getSession().getAttribute(StringConstants.USERPOINTS), true, getGeospatialKosher());
    }

    public void onClick$bMultiple(Event event) {
        importList(tMultiple.getText());
        refreshBtnOkDisabled();
    }

    /**
     * imports a list of species names into the create assemblages table.
     * <p/>
     * It will look up the scinetific name, common name in the BIE before displaying a record count.
     * <p/>
     * When the name can not be located in the BIE a raw_name search on occurrence records is performed.
     *
     * @param list
     */
    void importList(String list) {
        String[] speciesNames = list.replace("\n", ",").replace("\t", ",").split(",");
        List<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            //see if we can find the lsid from the BIE
            String lsid = BiocacheQuery.getGuid(s);
            String sciname = "", family = "", kingdom = "";
            String classLsid = lsid != null ? lsid : s;
            Map<String, String> sr = BiocacheQuery.getClassification(classLsid);
            if (!sr.isEmpty()
                    && ("true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie")) ||
                            (sr.get(StringConstants.SCIENTIFIC_NAME) != null
                                && sr.get(StringConstants.SCIENTIFIC_NAME).length() > 0))) {

                if (lsid == null) {
                    lsid = s;
                }

                if ("true".equalsIgnoreCase(CommonData.getSettings().getProperty("new.bie"))) {
                    sciname = sr.containsKey(StringConstants.SUB_SPECIES) ? sr.get(StringConstants.SUB_SPECIES) : null;
                    if (sciname == null && sr.containsKey(StringConstants.SPECIES)) {
                        sciname = sr.get(StringConstants.SPECIES);
                    }
                    if (sciname == null && sr.containsKey(StringConstants.GENUS)) {
                        sciname = sr.get(StringConstants.GENUS);
                    }
                    if (sciname == null && sr.containsKey(StringConstants.FAMILY)) {
                        sciname = sr.get(StringConstants.FAMILY);
                    }
                } else {
                    sciname = sr.get(StringConstants.SCIENTIFIC_NAME);
                }

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
            if (lsid != null && lsid.length() > 0) {
                addTolMultiple(lsid, sciname, family, kingdom, false);
            } else {
                addTolMultiple(null, s, "", "", false);
            }

            if (lsid == null || lsid.length() == 0) {
                notFound.add(s);
                notFoundSb.append(s).append("\n");
            }
        }
        if (!notFound.isEmpty()) {
            getMapComposer().showMessage(CommonData.lang("names_not_found_continuing") + ":\n" + notFoundSb.toString(), this);
        }
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

        //remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                refreshBtnOkDisabled();
                if (multipleSpeciesUploadName == null) {
                    multipleSpeciesUploadName = "";
                }
                if (loadedAssemblage && !multipleSpeciesUploadName.contains("subset")) {
                    multipleSpeciesUploadName += " - subset";
                }
            }
        });
        lc.setParent(li);

        //sci name
        if (lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        //family
        if (lsid == null && !sciname.matches(".*[0-9].*")) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener(StringConstants.ONCLICK, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)", "").trim();
                    mSearchSpeciesACComponent.getAutoComplete().refresh(sciname);
                    mSearchSpeciesACComponent.getAutoComplete().open();
                    mSearchSpeciesACComponent.getAutoComplete().setText(sciname + " ");
                    li.detach();
                }
            });
        } else {
            lc = new Listcell(family);
        }
        lc.setParent(li);

        //kingdom
        lc = new Listcell(kingdom);
        lc.setParent(li);

        //count
        if (lsid != null) {
            int count = new BiocacheQuery(lsid, null, null, null, false, getGeospatialKosher()).getOccurrenceCount();
            if (count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            int count = new BiocacheQuery(null, null, "raw_name:\"" + sciname + "\"", null, false, getGeospatialKosher()).getOccurrenceCount();
            if (count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        }
        lc.setParent(li);

        //lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if (insertAtBeginning && !lMultiple.getChildren().isEmpty()) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }

    /**
     * Retrieves a CSV version scientific names that have been added to the
     * list.
     *
     * @return
     */
    private String getScientificName() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lMultiple.getItems()) {
            Listcell sciNameCell = (Listcell) li.getChildren().get(1);
            String name = sciNameCell.getLabel();
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(name.replaceAll("\\(not found\\)", "").trim());
        }
        return sb.toString();
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

    /**
     * Constructs the query for names without matches.
     *
     * @return
     */
    private String[] getNamesWithoutLsids() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : lMultiple.getItems()) {
            Listcell sciNameCell = (Listcell) li.getChildren().get(1);
            String name = sciNameCell.getLabel();
            if (name.contains("(not found)")) {
                if (sb.length() > 0) {
                    sb.append("|");
                }
                sb.append(name.replaceAll("\\(not found\\)", "").trim());
            }
        }
        if (sb.length() > 0) {
            return sb.toString().split("\\|");
        } else {
            return new String[0];
        }
    }

    public void setMultipleSpecies(String splist, String layername) {
        tMultiple.setText(splist);
        rSearch.setSelected(false);
        rMultiple.setSelected(true);
        vboxSearch.setVisible(false);
        vboxMultiple.setVisible(true);
        vboxImportSL.setVisible(false);
        bMultiple.focus();
        this.onClick$bMultiple(event);
        multipleSpeciesUploadName = layername;
    }

    /**
     * TODO NC 2013-08-15: Remove the need for Constants.FALSE as the third item in the array.
     *
     * @return
     */
    public boolean[] getGeospatialKosher() {
        return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
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


    private void showExportSpeciesListDialog() {
        String values = getScientificName();
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

    public void updateSpeciesListMessage(String drUid) {
        //if a data resource exists check report it
        LOGGER.debug("Species list that was created : " + drUid);
        if (drUid != null) {

            speciesListListbox.init();
            rUploadLSIDs.setSelected(true);
            onCheck$rgAddSpecies(null);
        }
    }

    /**
     * Exporting an assemblage will create a new species list for the species that are contained in the create assemblage table.
     *
     * @param event
     */
    public void onClick$bAssemblageExport(Event event) {
        showExportSpeciesListDialog();
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
