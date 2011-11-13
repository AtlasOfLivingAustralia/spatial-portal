package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.data.BiocacheQuery;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.CommonData;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 *
 * @author Adam
 */
public class AddSpeciesController extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    SpeciesAutoComplete searchSpeciesAuto;
    Button btnOk;
    Radio rSearch;
    Radio rMultiple;
    Radio rUploadCoordinates;
    Radio rUploadLSIDs;
    Radio rAllSpecies;
    Radiogroup rgAddSpecies;
    Vbox vboxSearch;
    Checkbox chkArea;
    Vbox vboxMultiple;
    SpeciesAutoComplete mSearchSpeciesAuto;
    Textbox tMultiple;
    Listbox lMultiple;
    
    Query query;
    String rank;
    String taxon;

    boolean prevAreaState = true;

    @Override
    public void afterCompose() {
        super.afterCompose();
        rSearch.setSelected(true);
        chkArea.setChecked(true);
        mSearchSpeciesAuto.setBiocacheOnly(true);
    }

    public void onClick$btnOk(Event event) {
        if(btnOk.isDisabled()) {
            return;
        }
        if(rAllSpecies.isSelected()) {
            AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
            window.setAllSpecies(true);
            window.loadAreaLayers();
            try {
                window.doModal();
            } catch (InterruptedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SuspendNotAllowedException ex) {
                Logger.getLogger(AddSpeciesController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if(chkArea.isChecked()) {
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
            } else if(rMultiple.isSelected()){
                AddSpeciesInArea window = (AddSpeciesInArea) Executions.createComponents("WEB-INF/zul/AddSpeciesInArea.zul", getMapComposer(), null);
                String lsids = getMultipleLsids();
                query = new BiocacheQuery(lsids, null, null, null, false);
                window.setSpeciesParams(query, rank, taxon);
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
                getMapComposer().mapSpeciesFromAutocomplete(searchSpeciesAuto, null);
            } else {
                onClick$btnUpload(event);
            }
        }
        this.detach();
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void onChange$searchSpeciesAuto(Event event) {
        btnOk.setDisabled(true);

        refreshBtnOkDisabled();
    }

    public void onChange$mSearchSpeciesAuto(Event event) {
        //add to lMultiple
        Comboitem ci = mSearchSpeciesAuto.getSelectedItem();
        if(ci != null && ci.getAnnotatedProperties() != null
                && ((String) ci.getAnnotatedProperties().get(0)) != null) {
            String lsid = ((String) ci.getAnnotatedProperties().get(0));

            try {
                Map<String, String> searchResult = BiocacheQuery.getClassification(lsid);

                String sciname= searchResult.get("scientificName");
                String family = searchResult.get("family");
                String kingdom = searchResult.get("kingdom");
                if(sciname == null) sciname = "";
                if(family == null) family = "";
                if(kingdom == null) kingdom = "";

                if(sciname != null && sciname.length() > 0) {
                    addTolMultiple(lsid, sciname, family, kingdom, true);

                    mSearchSpeciesAuto.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                usc.setTbInstructions("3. Select file (text file, one LSID per line)");
            } else {
                usc.setTbInstructions("3. Select file");
            }
            usc.setDefineArea(chkArea.isChecked());
            usc.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setLsid(String lsidName) {
        String[] s = lsidName.split("\t");
        String species = s[1];
        String lsid = s[0];

        /* set species from layer selector */
        if (species != null) {
            String tmpSpecies = species;
            searchSpeciesAuto.setValue(tmpSpecies);
            searchSpeciesAuto.refresh(tmpSpecies);

            if (searchSpeciesAuto.getSelectedItem() == null) {
                List list = searchSpeciesAuto.getItems();
                for (int i = 0; i < list.size(); i++) {
                    Comboitem ci = (Comboitem) list.get(i);
                    //compare name
                    if (ci.getLabel().equalsIgnoreCase(searchSpeciesAuto.getValue())) {
                        //compare lsid
                        if (ci.getAnnotatedProperties() != null
                                && ((String) ci.getAnnotatedProperties().get(0)).equals(lsid)) {
                            searchSpeciesAuto.setSelectedItem(ci);
                            break;
                        }
                    }
                }
            }            
        }
        
        refreshBtnOkDisabled();
    }

    public void onCheck$rgAddSpecies(Event event) {
        if(rSearch.isSelected()) {
           vboxSearch.setVisible(true);
           vboxMultiple.setVisible(false);
//           searchSpeciesAuto.setFocus(true);
        } else if(rMultiple.isSelected()) {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(true);
        } else {
            vboxSearch.setVisible(false);
            vboxMultiple.setVisible(false);
        }

        refreshBtnOkDisabled();
    }

    private void refreshBtnOkDisabled() {
        if (rSearch.isSelected()) {
            btnOk.setDisabled(searchSpeciesAuto.getSelectedItem() == null || searchSpeciesAuto.getSelectedItem().getValue() == null);
        } else if(rMultiple.isSelected()) {
            btnOk.setDisabled(getMultipleLsids().length() == 0);
        } else {
            btnOk.setDisabled(false);
        }
    }

    void getFromAutocomplete() {
        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (searchSpeciesAuto.getSelectedItem() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0) {
            return;
        }

        //btnSearchSpecies.setVisible(true);
        taxon = searchSpeciesAuto.getValue();
        rank = "";

        String spVal = searchSpeciesAuto.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";

        } else {
            rank = StringUtils.substringBefore(spVal, ",").toLowerCase();
            System.out.println("mapping rank and species: " + rank + " - " + taxon);
        }
        if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
            rank = "taxon";
        }

        query = QueryUtil.get((String)searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0), getMapComposer(), true);
    }

    public void onClick$bMultiple(Event event) {
        String [] speciesNames = tMultiple.getText().replace("\n", ",").split(",");
        ArrayList<String> notFound = new ArrayList<String>();
        StringBuilder notFoundSb = new StringBuilder();
        for(int i=0;i<speciesNames.length;i++) {
            String s = speciesNames[i].trim();
            if(s.length() > 0) {
                JSONObject searchResult = processAdhoc(s);
                try {
                    JSONArray ja = searchResult.getJSONArray("values");

                    String sciname= "", family="", kingdom="", lsid = null;
                    for(int j=0;j<ja.size();j++) {
                        if(ja.getJSONObject(j).getString("name").equals("scientificName")) {
                            sciname = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("family")) {
                            family = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("kingdom")) {
                            kingdom = ja.getJSONObject(j).getString("processed");
                        }
                        if(ja.getJSONObject(j).getString("name").equals("taxonConceptID")) {
                            lsid = ja.getJSONObject(j).getString("processed");
                        }
                    }

                    //is 's' an LSID?
                    if(lsid == null || lsid.length() == 0) {
                        Map<String, String> sr = BiocacheQuery.getClassification(s);
                        if(sr.size() > 0 
                                && sr.get("scientificName") != null
                                && sr.get("scientificName").length() > 0) {
                            lsid = s;
                            sciname= sr.get("scientificName");
                            family = sr.get("family");
                            kingdom = sr.get("kingdom");
                            if(sciname == null) sciname = "";
                            if(family == null) family = "";
                            if(kingdom == null) kingdom = "";
                        }
                    }

                    if(lsid != null && lsid.length() > 0) {
                        addTolMultiple(lsid,sciname,family,kingdom, false);
                    } else {
                        addTolMultiple(null,s,"","", false);
                    }

                    if(lsid == null || lsid.length() == 0) {
                        notFound.add(s);
                        notFoundSb.append(s + "\n");
                    }
                } catch (Exception e) {
                    notFound.add(s);
                    notFoundSb.append(s + "\n");
                }
            }
        }

        if(notFound.size() > 0) {
            getMapComposer().showMessage("Cannot identify these scientific names:\n" + notFoundSb.toString(), this);
        }

        refreshBtnOkDisabled();
    }

    JSONObject processAdhoc(String scientificName) {
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(CommonData.biocacheServer + "/process/adhoc");
            StringRequestEntity sre = new StringRequestEntity("{ \"scientificName\": \"" + scientificName.replace("\"","'") + "\" } ", "application/json", "UTF-8");
            post.setRequestEntity(sre);
            int result = client.executeMethod(post);
            if(result == 200) {
                return JSONObject.fromObject(post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addTolMultiple(String lsid, String sciname, String family, String kingdom, boolean insertAtBeginning) {
        for(Listitem li : (List<Listitem>)lMultiple.getItems()) {
            Listcell lsidCell = (Listcell) li.getLastChild();
            Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
            if((lsid != null && lsidCell.getLabel().equals(lsid))
                    || (sciname != null && scinameCell.getLabel().replace("(not found)","").trim().equals(sciname))) {
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
            }
        });
        lc.setParent(li);

        //sci name
        if(lsid == null) {
            lc = new Listcell(sciname + " (not found)");
            lc.setSclass("notFoundSciname");
        } else {
            lc = new Listcell(sciname);
        }
        lc.setParent(li);

        //family
        if(lsid == null) {
            lc = new Listcell("click to search");
            lc.setSclass("notFoundFamily");
            lc.addEventListener("onClick", new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    Listitem li = (Listitem) event.getTarget().getParent();
                    Listcell scinameCell = (Listcell) li.getFirstChild().getNextSibling();
                    String sciname = scinameCell.getLabel().replace("(not found)","").trim();
                    mSearchSpeciesAuto.refresh(sciname);
                    mSearchSpeciesAuto.open();
                    mSearchSpeciesAuto.setText(sciname + " ");
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
        if(lsid != null) {
            int count = new BiocacheQuery(lsid,null,null,null,false).getOccurrenceCount();
            if(count > 0) {
                lc = new Listcell(String.valueOf(count));
            } else {
                lc = new Listcell(kingdom);
            }
        } else {
            lc = new Listcell(kingdom);
        }
        lc.setParent(li);

        //lsid
        lc = new Listcell(lsid);
        lc.setParent(li);

        if(insertAtBeginning && lMultiple.getChildren().size() > 0) {
            lMultiple.insertBefore(li, lMultiple.getFirstChild());
        } else {
            li.setParent(lMultiple);
        }
    }

    private String getMultipleLsids() {
        StringBuilder sb = new StringBuilder();
        for(Listitem li : (List<Listitem>)lMultiple.getItems()) {
            Listcell lc = (Listcell) li.getLastChild();
            if(lc.getLabel() != null && lc.getLabel().length() > 0) {
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(lc.getLabel());
            }
        }
        return sb.toString();
    }
}
