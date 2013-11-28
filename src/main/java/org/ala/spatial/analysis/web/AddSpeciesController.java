package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.data.SpeciesListDTO;
import org.ala.spatial.data.SpeciesListItemDTO;
import org.ala.spatial.data.SpeciesListUtil;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.A;
import org.zkoss.zul.AbstractListModel;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelExt;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.event.ListDataEvent;
import org.zkoss.zul.event.ListDataListener;

/**
 *
 * @author Adam
 */
public class AddSpeciesController extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    SpeciesAutoCompleteComponent searchSpeciesACComponent;
    //SpeciesAutoComplete searchSpeciesAuto;
    Button btnOk, bMultiple,bAssemblageExport;
    Radio rSearch;
    Radio rMultiple;
    Radio rUploadCoordinates;
    Radio rUploadLSIDs;
    Radio rAllSpecies;
    Radiogroup rgAddSpecies;
    Vbox vboxSearch;
    Checkbox chkArea;
    //Checkbox chkUseRawName;
    Vbox vboxMultiple;
    Vbox vboxImportSL; //the box layout for the import species list
    //SpeciesAutoComplete mSearchSpeciesAuto;
    SpeciesAutoCompleteComponent mSearchSpeciesACComponent;
    //Combobox cbSpeciesLists;
    Textbox tMultiple;
    Listbox lMultiple;
    Event event;
    Checkbox chkGeoKosherTrue, chkGeoKosherFalse;
    Query query;
    String rank;
    String taxon;
    String multipleSpeciesUploadName = null;
    boolean prevAreaState = true;
    boolean loadedAssemblage =false;
    SpeciesListListbox speciesListListbox;
    String user;
    java.util.List<String> selectedLists;
    A aMessage;
    Label lblMessage;

    
    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
        chkArea.setChecked(true);
        mSearchSpeciesACComponent.getAutoComplete().setBiocacheOnly(true);        
        
        vboxImportSL = (Vbox)this.getFellow("splistbox").getFellow("vboxImportSL");
        speciesListListbox = (SpeciesListListbox)this.getFellow("splistbox").getFellow("speciesListListbox");
        //check to see if a user is logged in
        user = getMapComposer().getCookieValue("ALA-Auth");
        if(user != null && user.length()>0){
            rMultiple.setDisabled(false);
        }
        else{
            rMultiple.setDisabled(true);
            rMultiple.setLabel(rMultiple.getLabel() +  " (log in required)");
        }
        speciesListListbox.addEventListener("onSlCheckBoxChanged", new EventListener(){
          @Override
          public void onEvent(Event event) throws Exception {
              refreshBtnOkDisabled();
          }
        });
        
    }
    
    public void onValueSelected$searchSpeciesACComponent(Event event){
        onChange$searchSpeciesAuto(event);
    }

    public void onClick$btnOk(Event event) {
        
        if (btnOk.isDisabled()) {
            return;
        }
        loadedAssemblage = false;
        if (rAllSpecies.isSelected()) {
            AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
            window.setGeospatialKosher(getGeospatialKosher());
            window.setAllSpecies(true);
            window.loadAreaLayers();
            try {
                window.doModal();
            } catch (InterruptedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SuspendNotAllowedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (chkArea.isChecked()) {
            getFromAutocomplete();

            if (rSearch.isSelected()) {
                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
                window.setSpeciesParams(query, rank, taxon);
                window.loadAreaLayers();
                try {
                    window.doModal();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SuspendNotAllowedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (rMultiple.isSelected()) {
                //when next is pressed we want to export the list to the species list
                showExportSpeciesListDialog();
                loadedAssemblage = true;

            } else if (rUploadLSIDs.isSelected()){
                //we need to populate the "create assemblage" with the values from the species list
                
                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
                //extract all the items for selected species lists
                query = speciesListListbox.extractQueryFromSelectedLists(getGeospatialKosher());
                window.setSpeciesParams(query, rank, taxon);
                window.setMultipleSpeciesUploadName("Species List Items");
                window.loadAreaLayers();
                try {
                    window.doModal();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SuspendNotAllowedException ex) {
                    Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                onClick$btnUpload(event);
            }
        } else {
            if (rSearch.isSelected()) {
                getMapComposer().mapSpeciesFromAutocompleteComponent(searchSpeciesACComponent, null, getGeospatialKosher());
            } else if (rUploadLSIDs.isSelected()){
                  //we need to populate the "create assemblage" with the values from the species list
                //refreshAssemblage();
                loadedAssemblage=true;
            } else {
                onClick$btnUpload(event);
            }
        }
        if (!loadedAssemblage)
            this.detach();       
            
    }
    
    public void enableImportAssemblage(){
        rUploadLSIDs.setSelected(true);
        onCheck$rgAddSpecies(null);
    }
    
//    private void refreshAssemblage(SpeciesListDTO list){
//        if(rUploadLSIDs.isSelected()){
//            //Comboitem ci = cbSpeciesLists.getSelectedItem();
//            //if(ci != null){
//             //   SpeciesListDTO list = (SpeciesListDTO)ci.getValue();
//                Collection<SpeciesListItemDTO> items = SpeciesListUtil.getListItems(list.getDataResourceUid());
//                //System.out.println(items);
//                if(items != null){
//                    StringBuilder sb = new StringBuilder();
//                    boolean first=true;
//                    for(SpeciesListItemDTO item: items){
//                        if(!first)
//                            sb.append(",");
//                        else
//                            first = false;
//                        sb.append(item.getName());
//                    }
//                    setMultipleSpecies(sb.toString(), list.getListName() + " (" + list.getDataResourceUid() + ")");
//                }
//                refreshBtnOkDisabled();
//            }
//        //}
//    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$searchSpeciesAuto(Event event) {
        btnOk.setDisabled(true);

        refreshBtnOkDisabled();
    }

    public void onValueSelected$mSearchSpeciesACComponent(Event event){
        //add to lMultiple
        Comboitem ci = mSearchSpeciesACComponent.getAutoComplete().getSelectedItem();
        if (ci != null && ci.getAnnotatedProperties() != null
                && ((String) ci.getAnnotatedProperties().get(0)) != null) {
            String annotatedValue = ((String) ci.getAnnotatedProperties().get(0));
            if(mSearchSpeciesACComponent.shouldUseRawName()){
                addTolMultiple(null, annotatedValue, null, null, true);
            } else { 
                try {
                    String lsid = annotatedValue;
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
    
                        mSearchSpeciesACComponent.getAutoComplete().setText("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        refreshBtnOkDisabled();
    }

    public void onClick$btnUpload(Event event) {
        try {
            UploadSpeciesController usc = (UploadSpeciesController) Executions.createComponents("WEB-INF/zul/UploadSpecies.zul", getMapComposer(), null);

            if (rUploadCoordinates.isSelected()) {
                usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
            } else if (rUploadLSIDs.isSelected()) {
                usc.setTbInstructions("3. Select file (text file, one LSID or name per line)");
            } else {
                usc.setTbInstructions("3. Select file");
            }
            usc.addToMap = true;
            usc.setDefineArea(chkArea.isChecked());
            usc.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    void setLsid(String lsidName) {
//        String[] s = lsidName.split("\t");
//        String species = s[1];
//        String lsid = s[0];
//
//        /* set species from layer selector */
//        if (species != null) {
//            String tmpSpecies = species;
//            searchSpeciesAuto.setValue(tmpSpecies);
//            searchSpeciesAuto.refresh(tmpSpecies);
//
//            if (searchSpeciesAuto.getSelectedItem() == null) {
//                List list = searchSpeciesAuto.getItems();
//                for (int i = 0; i < list.size(); i++) {
//                    Comboitem ci = (Comboitem) list.get(i);
//                    //compare name
//                    if (ci.getLabel().equalsIgnoreCase(searchSpeciesAuto.getValue())) {
//                        //compare lsid
//                        if (ci.getAnnotatedProperties() != null
//                                && ((String) ci.getAnnotatedProperties().get(0)).equals(lsid)) {
//                            searchSpeciesAuto.setSelectedItem(ci);
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//
//        refreshBtnOkDisabled();
//    }

    public void onCheck$rgAddSpecies(Event event) {
        if (rSearch.isSelected()) {
            vboxSearch.setVisible(true);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(false);
//           searchSpeciesAuto.setFocus(true);
        } else if (rMultiple.isSelected()) {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(true);
            vboxImportSL.setVisible(false);
            aMessage.setVisible(false);
            lblMessage.setValue("");
            String authCookie = getMapComposer().getCookieValue("ALA-Auth");
            //bAssemblageExport.setDisabled(loadedAssemblage || authCookie == null);
        } else if(rUploadLSIDs.isSelected()){
            //refreshSpeciesLists();
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(true);
        } else {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(false);
            vboxImportSL.setVisible(false);
        }

        refreshBtnOkDisabled();
    }
    
    private void refreshBtnOkDisabled() {
        if (rSearch.isSelected()) {
            btnOk.setDisabled(!searchSpeciesACComponent.hasValidItemSelected());
        } else if (rMultiple.isSelected()) {            
            btnOk.setDisabled(getMultipleLsids().length() == 0 && getNamesWithoutLsids() == null);
        } else if(rUploadLSIDs.isSelected()) {
            btnOk.setDisabled(speciesListListbox.getSelectedLists() == null || speciesListListbox.getSelectedLists().size()==0);
            //btnOk.setDisabled(true);
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

        //btnSearchSpecies.setVisible(true);
        taxon = searchSpeciesACComponent.getAutoComplete().getValue();
        rank = "";

        String spVal = searchSpeciesACComponent.getAutoComplete().getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

        } else {
            rank = StringUtils.substringBefore(spVal, ",").toLowerCase();
            logger.debug("mapping rank and species: " + rank + " - " + taxon);
        }
        if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
            rank = "taxon";
        }
        query = searchSpeciesACComponent.getQuery(getMapComposer(),true, getGeospatialKosher());
    }

    public void onClick$bMultiple(Event event) {
        importList(tMultiple.getText());
        refreshBtnOkDisabled();
    }
    
    /**
     * imports a list of species names into the create assemblages table.
     * 
     * It will look up the scinetific name, common name in the BIE before displaying a record count.
     * 
     * When the name can not be located in the BIE a raw_name search on occurrence records is performed.
     * 
     * @param list
     */
    void importList(String list){
        String[] speciesNames = list.replace("\n", ",").replace("\t",",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for (int i = 0; i < speciesNames.length; i++) {
            String s = speciesNames[i].trim();
            //see if we can find the lsid from the BIE
            String lsid = BiocacheQuery.getGuid(s);
            String sciname = "", family = "", kingdom = "";
            String classLsid = lsid!= null?lsid:s;
            Map<String,String> sr = BiocacheQuery.getClassification(classLsid);
            if (sr.size() > 0
                && sr.get("scientificName") != null
                && sr.get("scientificName").length() > 0) {
                
                if(lsid == null)
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
          if (lsid != null && lsid.length() > 0) {
              addTolMultiple(lsid, sciname, family, kingdom, false);
          } else {
              addTolMultiple(null, s, "", "", false);
          }

          if (lsid == null || lsid.length() == 0) {
              notFound.add(s);
              notFoundSb.append(s + "\n");
          }
        }
        if (notFound.size() > 0) {
            getMapComposer().showMessage("The following list of scientific names could not be located. A free text search on the raw name will be performed:\n" + notFoundSb.toString(), this);
        }
    }
    
    void importListold(String list) {
        String[] speciesNames = list.replace("\n", ",").replace("\t",",").split(",");
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

                    //is 's' an LSID?
                    if ((lsid == null || lsid.length() == 0) && s.matches(".*[0-9].*")) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if (sr.size() > 0
                                && sr.get("scientificName") != null
                                && sr.get("scientificName").length() > 0) {
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
                        notFoundSb.append(s + "\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s + "\n");
                }
            }
        }

        if (notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        refreshBtnOkDisabled();
    }

    JSONObject processAdhoc(String scientificName) {
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.biocacheServer + "/process/adhoc");
            StringRequestEntity sre = new StringRequestEntity("{ \"scientificName\": \"" + scientificName.replace("\"", "'") + "\" } ", "application/json", "UTF-8");
            post.setRequestEntity(sre);
            int result = client.executeMethod(post);
            if (result == 200) {
                return JSONObject.fromObject(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addTolMultiple(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if ((lsid != null && lsidCell.getLabel().equals(lsid))
                    || (sciname != null && scinameCell.getLabel().replace("(not found)", "").trim().equals(sciname))) {
                return;
            }
        }

        Listitem li = new Listitem();

        //remove button
        Listcell lc = new Listcell("x");
        lc.setSclass("xRemove");
        lc.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event event) throws Exception {
                Listitem li = (Listitem) event.getTarget().getParent();
                li.detach();
                refreshBtnOkDisabled();
                if(loadedAssemblage && !multipleSpeciesUploadName.contains("subset"))
                    multipleSpeciesUploadName += " - subset";
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
            lc.addEventListener("onClick", new EventListener() {

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
            int count = new BiocacheQuery(null,null,"raw_name:\""+sciname+"\"", null,false, getGeospatialKosher()).getOccurrenceCount();
            if(count >0)
                lc = new Listcell(String.valueOf(count));
            else
                lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        //lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if (insertAtBeginning && lMultiple.getChildren().size() > 0) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }
    /**
     * Retrieves a CSV version scientific names that have been added to the 
     * list.
     * @return
     */
    private String getScientificName(){
        StringBuilder sb = new StringBuilder();
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
            Listcell sciNameCell = (Listcell)li.getChildren().get(1);
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
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
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
     * @return
     */
    private String[] getNamesWithoutLsids(){
        StringBuilder sb = new StringBuilder();
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
            //Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell sciNameCell = (Listcell)li.getChildren().get(1);
            String name = sciNameCell.getLabel();
            if(name.contains("(not found)")){
              if(sb.length()>0)
                  sb.append("|");
                sb.append(name.replaceAll("\\(not found\\)", "").trim());
//               if(sb.length()>0)
//                   sb.append(" AND ");
//               sb.append("raw_name:\"").append(name.replaceAll("\\(not found\\)", "").trim()).append("\"");
            }
        }
        if(sb.length()>0)
            return sb.toString().split("\\|");
        else
            return null;
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
     * TODO NC 2013-08-15: Remove the need for "false" as the third item in the array.
     * @return
     */
    public boolean[] getGeospatialKosher() {
        return new boolean[]{chkGeoKosherTrue.isChecked(), chkGeoKosherFalse.isChecked(), false};
    }

    public void onCheck$chkGeoKosherTrue(Event event) {
        event = ((ForwardEvent)event).getOrigin();
        if (!((CheckEvent)event).isChecked() && !chkGeoKosherFalse.isChecked()) {
            chkGeoKosherFalse.setChecked(true);
        }
    }

    public void onCheck$chkGeoKosherFalse(Event event) {
        event = ((ForwardEvent)event).getOrigin();
        if (!((CheckEvent)event).isChecked() && !chkGeoKosherTrue.isChecked()) {
            chkGeoKosherTrue.setChecked(true);
        }
    }

   
    private void showExportSpeciesListDialog(){
      String values = getScientificName();//tMultiple.getValue().replace("\n", ",").replace("\t",",");
      logger.debug("Creating species list with " + values);
      if(values.length()>0){
          UploadToSpeciesListController dialog = (UploadToSpeciesListController)Executions.createComponents("WEB-INF/zul/UploadToSpeciesList.zul", this, null);
          dialog.setSpecies(values);
          try{
              dialog.doModal();
              
          }
          catch(Exception e){
              logger.error("Unable to export assemblage",e);
          }
      }
      
    }
    public void updateSpeciesListMessage(String drUid){
      //if a data resource exists check report it        
        logger.debug("Species list that was created : " + drUid);
        if(drUid != null){
            
            ((SpeciesListListbox.SpeciesListListModel)speciesListListbox.getModel()).refreshModel();
            //rgAddSpecies.setS
            rUploadLSIDs.setSelected(true);
            onCheck$rgAddSpecies(null);
        }        
    }
    /**
     * Exporting an assemblage will create a new species list for the species that are contained in the create assemblage table.
     * @param event
     */
    public void onClick$bAssemblageExport(Event event) {
        showExportSpeciesListDialog();
        //dialog
      //AddSpeciesController window = (AddSpeciesController) Executions.createComponents("WEB-INF/zul/AddSpecies.zul", getMapComposer(), null);
        //loading the assemblage list to a new species list
        //TODO create a dialog that asks for a title, url etc...
        //SimpleDateFormat sdf = new SimpleDateFormat("ddmmyyyy_hhmm");
        //Filedownload.save(getLsids(), "text/plain", "species_assemblage_" + sdf.format(new Date()) + ".txt");
    }

    String getLsids() {
        StringBuilder sb = new StringBuilder();
        for (Listitem li : (List<Listitem>) lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            if(sb.length() > 0) sb.append("\n");
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
            
            //forget content types, do 'try'
            boolean loaded = false;
            try {
                importList(readerToString(m.getReaderData()));
                loaded = true;
                logger.info("read type " + m.getContentType() + " with getReaderData");
            } catch (Exception e) {
                //e.printStackTrace();
            }
            if (!loaded) {
                try {                    
                    importList(new String(m.getByteData()));
                    loaded = true;                    
                    logger.info("read type " + m.getContentType() + " with getByteData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    importList(readerToString(new InputStreamReader(m.getStreamData())));
                    loaded = true;
                    logger.info("read type " + m.getContentType() + " with getStreamData");
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
            if (!loaded) {
                try {
                    importList(m.getStringData());
                    loaded = true;
                    logger.info("read type " + m.getContentType() + " with getStringData");
                } catch (Exception e) {
                    //last one, report error
                    getMapComposer().showMessage("Unable to load your file. Please try again.");
                    //System.out.println("unable to load user points: ");
                    //e.printStackTrace();
                    logger.error("unable to load user points", e);
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    String readerToString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char [] buffer = new char[1000];
        int size;
        while((size = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, size);
        }
        return sb.toString();
    }
    
    private BiocacheQuery extractQueryFromSelectedLists(){
      StringBuilder sb = new StringBuilder();
      ArrayList<String>names = new ArrayList<String>();
      for(String list : selectedLists){
          //get the speciesListItems
          Collection<SpeciesListItemDTO> items=SpeciesListUtil.getListItems(list);
          if(items != null){
              for(SpeciesListItemDTO item:items){
                  if(item.getLsid() != null){
                      if(sb.length()>0)
                          sb.append(",");
                      sb.append(item.getLsid());
                  }
                  else{
                      names.add(item.getName());
                  }
                      
              }
          }
      }
      String[] unmatchedNames = names.size()>0 ? names.toArray(new String[]{}):null;
      String lsids = sb.length()>0 ? sb.toString() :null;
      return new BiocacheQuery(lsids,unmatchedNames,null,null, null, false, getGeospatialKosher());
    }
}
