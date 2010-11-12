/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.composer;

import au.org.emii.portal.mest.Dataset;
import au.org.emii.portal.mest.MESTFreeTextSearch;
import au.org.emii.portal.mest.MESTGetRecord;
import au.org.emii.portal.mest.MESTRecord;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.mest.SearchQuery;
import au.org.emii.portal.mest.WMSLayer;
import au.org.emii.portal.mest.WMSServer;
import au.org.emii.portal.mest.Website;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.value.SearchCatalogue;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

/**
 * 
 * @author brendonward
 */
public class MESTSearchComposer extends UtilityComposer {

    private static final long serialVersionUID = 1L;
    private WMSLayer wms = new WMSLayer();
    private WMSServer serverWMS = new WMSServer();
    private Div listBox;
    private Window searcher;
    private Listbox box;
    private Div pagingDiv;
    private Intbox pageNo;
    private Label numberOfPages;
    private List<Dataset> datasets = new ArrayList<Dataset>();
    private SearchQuery sq = new SearchQuery();
    private Session sess = (Session) Sessions.getCurrent();
    private MESTFreeTextSearch mfts = new MESTFreeTextSearch();
    private Button btnFirst;
    private Button btnPrevious;
    private Button btnNext;
    private Button btnLast;
    private String searchURL;
    private Div saveSearchPrompt;
    private Div loginPrompter;
    private Textbox createSavedSearch;
    private Button saveSearch;
    private Label createSavedSearchError;
    private Label savedSearchOk;
    private Div pagingDivcontainer;
    private PortalSessionUtilities portalSessionUtilities = null;

    public void onCreate() {        
        if (getPortalSession().isLoggedIn()) {
            loginActions();
        }
    }

    private void toggleLoginPrompt(boolean show) {
        loginPrompter.setVisible(show);
        saveSearchPrompt.setVisible(! show);
    }

    public void loginActions() {
        toggleLoginPrompt(false);
    }

    public void logoutActions() {
        toggleLoginPrompt(true);
    }

    public void searchSavedActions() {
        saveSearchPrompt.setVisible(false);
        savedSearchOk.setVisible(true);
    }

    public void buildResultBox() {

        // remove any that already exist
        box.getItems().clear();

        // ok lets populate the listbox with the results
        for (Dataset d : datasets) {

            Div div = new Div();
            Div divRight = new Div();
            divRight.setSclass("right searchResControls");
            Listitem li = new Listitem();
            Listcell lc = new Listcell();
            Vbox vb = new Vbox();
            //Hbox hb = new Hbox();
            Label title = new Label();

            title.setValue(d.getTitle());
            title.setWidth("250");
            title.setSclass("searchResTitle h3");

            Toolbarbutton tbFull = new Toolbarbutton();
            tbFull.setLabel("Full record");
            tbFull.setSclass("right z-button-cm external");
            tbFull.setHref(searchURL + d.getMestrecord());
            tbFull.setTarget("_blank");
            Separator s = new Separator();
            s.setOrient("vertical");
            s.setSclass("right");

            Toolbarbutton tbMore = new Toolbarbutton();
            tbMore.setSclass("right z-button-cm");
            tbMore.setLabel("More info..");

            MoreInfoEvent mie = new MoreInfoEvent();
            tbMore.addEventListener("onClick", mie);

            Label la = new Label(d.getAbstract());
            la.setPre(true);
            la.setMultiline(true);

            Div di = new Div();
            di.setId(d.getIdentifier());
            di.setVisible(false);


            divRight.appendChild(tbMore);
            divRight.appendChild(s);
            divRight.appendChild(tbFull);
            div.appendChild(title);
            div.appendChild(divRight);
            vb.appendChild(div);
            vb.appendChild(la);
            vb.appendChild(di);

            lc.appendChild(vb);
            li.appendChild(lc);

            box.appendChild(li);
        }

    }

    public void addToMap() {
        float opacity = (float) 0.5;
        String label;
        String serverURL = wms.getServerURL();
        String s;

        s = serverURL.trim();

        if (wms.getTitle() != null) {
            label = wms.getTitle();
        } else {
            label = wms.getName();
        }

        MapComposer mc = getThisMapComposer();

        boolean bCheck = mc.addWMSLayer(label, s, opacity);

        if (bCheck) {
            mc.showMessage("Server successfully added");
        }

    }

    public void addServerToMap() {
        float opacity = (float) 0.5;

        String serverURL = serverWMS.getURL();
        String serverName = serverWMS.getName();
        String version = "1.1.1";
        String s;

        s = serverURL.trim();
        version = serverWMS.getVersion();
        MapComposer mc = getThisMapComposer();

        boolean bCheck = mc.addWMSServer(serverName, s, version, opacity, false);

        if (bCheck) {
            mc.showMessage("Server successfully added");
        }

    }

    public void onOK$pageNo(Event event) {

        int i = pageNo.getValue();
        // checks for stupidity
        int x = (mfts.getResultCount() / 10) + 1;
        if (i < 1) {
            i = 1;
            pageNo.setValue(i);
        }

        if (i > x) {
            i = x;
            pageNo.setValue(i);
        }
        int start = (i * 10) - 9;
        int end = (i * 10);


        setResults(i, start, end);

    }

    public void onClick$btnFirst(Event event) {
        btnPrevious.setDisabled(true);
        setResults(1, 1, 10);
    }

    public void onClick$btnLast(Event event) {
        btnPrevious.setDisabled(false);
        int i = (mfts.getResultCount() / 10) + 1;
        int start = (i * 10) - 9;
        int end = (i * 10);
        setResults(i, start, end);
    }

    public void onClick$btnPrevious(Event event) {
        int i = pageNo.getValue();
        i = i - 1;
        int start = (i * 10) - 9;
        int end = (i * 10);
        setResults(i, start, end);
    }

    public void onClick$btnNext(Event event) {
        int i = pageNo.getValue();
        i = i + 1;
        int start = (i * 10) - 9;
        int end = (i * 10);
        setResults(i, start, end);
    }

    private void setResults(int iPageNo, int iStart, int iEnd) {
        int x = (mfts.getResultCount() / 10) + 1;

        pageNo.setValue(iPageNo);
        //check which buttons to disable

        if (iPageNo == 1) {
            btnPrevious.setDisabled(true);
        } else {
            btnPrevious.setDisabled(false);
        }

        if (iPageNo == x) {
            btnNext.setDisabled(true);
        } else {
            btnNext.setDisabled(false);
        }

        mfts.GetResultFromTo(iStart, iEnd);
        datasets = mfts.getDatasets();
        buildResultBox();

    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        PortalSession portalSession = getPortalSession();
        SearchCatalogue searchCatalogue = portalSessionUtilities.getSelectedSearchCatalogue(portalSession);
        if (searchCatalogue == null) {
            logger.error(String.format("requested search catalogue '%s' is not available - search is broken!",
                    portalSession.getSelectedSearchCatalogueId()));
            getThisMapComposer().showMessage("Search is not available at the moment, please close your browser and try again later");
        } else {
            sq = (SearchQuery) sess.getAttribute("searchquery");
            searchURL = searchCatalogue.getUri() + "/geonetwork/srv/en";

            mfts.setSearchURL(searchURL);
            mfts.setSearchParameter(sq.getSearchTerm());

            if (sq.isUseDate()) {
                mfts.setUseDate(true);

                if (sq.getStartDate() != null) {
                    mfts.setStartDate(sq.getStartDate());
                }

                if (sq.getEndDate() != null) {
                    mfts.setEndDate(sq.getEndDate());
                }
            }

            // does it use the bbox stuff
            if (sq.isUseBBOX()) {
                mfts.setUseBBOX(true);
                mfts.setEast(sq.getRight());
                mfts.setWest(sq.getLeft());
                mfts.setNorth(sq.getTop());
                mfts.setSouth(sq.getBottom());
            }

            mfts.RunSearch();
            mfts.ParseResult();

            if (mfts.getResultCount() == 0) {
                getThisMapComposer().showMessage("Your search returned no results");
                searcher.detach();

            } else {
                String s = String.valueOf(mfts.getResultCount());

                pageNo.setValue(1);
                datasets = mfts.getDatasets();
                int iPages = (mfts.getResultCount() / 10) + 1;

                if (iPages == 1) {
                    pagingDiv.setVisible(false);
                } else {
                    pageNo.setValue(1);
                    btnFirst.setLabel("<<");
                    btnLast.setLabel(">>");
                    btnPrevious.setLabel("<");
                    btnPrevious.setDisabled(true);
                    btnNext.setLabel(">");

                    numberOfPages.setValue(String.valueOf(iPages));
                }
                searcher.setTitle("MEST Search - " + s + " results for '" + sq.getSearchTerm() + "'");

                buildResultBox();

            }
        }
    }

    private class MoreInfoEvent implements EventListener {

        public void onEvent(Event event) throws UiException {
            try {

                Toolbarbutton tb = (Toolbarbutton) event.getTarget();
                Button mapButton = new Button();
                Button wmsButton = new Button();
                Listitem li = (Listitem) tb.getParent().getParent().getParent().getParent().getParent();

                box.setSelectedIndex(li.getIndex());
                tb.setDisabled(true);

                // ok now we have the selection index we can query the mest for
                // the full record
                // parse what we need out of it and add the links & buttons
                Dataset d = datasets.get(li.getIndex());

                Div dv = (Div) tb.getParent().getParent().getFellow(d.getIdentifier());
                MESTGetRecord mgr = new MESTGetRecord();

                mgr.setSearchURL(searchURL);
                mgr.setSearchParameter(d.identifier);
                mgr.RunSearch();
                mgr.ParseResult();
                MESTRecord mr = mgr.getMestRecord();

                Vbox vb = new Vbox();

                // check if its mappable

                if (mr.getWMSLayers().size() > 0) {
                    for (WMSLayer wL : mr.getWMSLayers()) {
                        wms = wL;
                        AddLayerEvent al = new AddLayerEvent();
                        mapButton.addEventListener("onClick", al);
                        mapButton.setVisible(true);
                        mapButton.setSclass("z-label");
                        mapButton.setLabel("Add to map");
                        vb.appendChild(mapButton);
                    }
                }

                if (mr.getWMSServers().size() > 0) {
                    for (WMSServer wL : mr.getWMSServers()) {
                        serverWMS = wL;
                        AddServerEvent ase = new AddServerEvent();
                        wmsButton.addEventListener("onClick", ase);
                        wmsButton.setLabel("Add server to map");
                        wmsButton.setSclass("z-label");
                        wmsButton.setVisible(true);
                        vb.appendChild(wmsButton);
                    }
                }
                // get all the links into the page

                for (Website w : mr.getWebsites()) {
                    Toolbarbutton tw = new Toolbarbutton();
                    tw.setLabel(w.getDescription());
                    tw.setHref(w.getURL());
                    tw.setTarget("_blank");
                    vb.appendChild(tw);
                }

                dv.appendChild(vb);
                dv.setVisible(true);

            } catch (RuntimeException ex) {
                java.util.logging.Logger.getLogger(
                        MESTSearchComposer.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
    }

    private MapComposer getThisMapComposer() {
        MapComposer mapComposer = null;
        Page page = searcher.getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    private class AddServerEvent implements EventListener {

        public void onEvent(Event event) throws UiException {
            try {
                addServerToMap();
            } catch (Exception ex) {
                logger.debug(ex.getMessage());
            }
        }
    }

    private class AddLayerEvent implements EventListener {

        public void onEvent(Event event) throws UiException {
            try {
                addToMap();
            } catch (Exception ex) {
                logger.debug(ex.getMessage());
            }
        }
    }

    public void onClick$saveSearch(Event event) {
        
        //check the validity of the name
        if (createSavedSearch.getText().length() < 1) {
            createSavedSearchError.setValue("Please give your map a name");
            saveSearchErrorActions(true);
        }
        else {

            if (checkString(createSavedSearch.getText())) {

                if (getMapComposer().getUserDataManager().checkSearchName(createSavedSearch.getText())) {

                    // success!
                    getMapComposer().getUserDataManager().saveSearch(createSavedSearch.getText(), sq);
                    getMapComposer().getLeftmenuSearchComposer().loadSavedSearches();
                    saveSearchErrorActions(false);
                    searchSavedActions();
                }
                else {
                    createSavedSearchError.setValue("Search name already exists");
                     saveSearchErrorActions(true);
                }
            }
            else {
                createSavedSearchError.setValue("Only alpha numeric characters can be used");
                saveSearchErrorActions(true);
            }
        }
    }

    public void saveSearchErrorActions(Boolean bool) {

        int expandby = 35;
        int pagingDivcontainerHeight = 25;
        int searcherHeight = 620;

        if (bool) {
             createSavedSearchError.setVisible(true);
             pagingDivcontainer.setHeight((pagingDivcontainerHeight + expandby) + "px");
             searcher.setHeight((searcherHeight + expandby) + "px");
        }
        else {
             createSavedSearchError.setVisible(false);
             pagingDivcontainer.setHeight(pagingDivcontainerHeight  + "px");
             searcher.setHeight(searcherHeight + "px");
        }

    }

    public boolean checkString(String aText) {
        boolean bCheck = true;
        StringBuilder result = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(
                aText);


        char character = iterator.current();


        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                bCheck = false;


                break;


            } else if (character == '>') {
                bCheck = false;


                break;


            } else if (character == '&') {
                bCheck = false;


                break;


            } else if (character == '\"') {
                bCheck = false;


                break;


            } else if (character == '\t') {
                bCheck = false;


                break;


            } else if (character == '!') {
                bCheck = false;


                break;


            } else if (character == '#') {
                bCheck = false;


                break;


            } else if (character == '$') {
                bCheck = false;


                break;


            } else if (character == '%') {
                bCheck = false;


                break;


            } else if (character == '\'') {
                bCheck = false;


                break;


            } else if (character == '(') {
                bCheck = false;


                break;


            } else if (character == ')') {
                bCheck = false;


                break;


            } else if (character == '*') {
                bCheck = false;


                break;


            } else if (character == '+') {
                bCheck = false;


                break;


            } else if (character == ',') {
                bCheck = false;


                break;


            } else if (character == '-') {
                bCheck = false;


                break;


            } else if (character == '.') {
                bCheck = false;


                break;


            } else if (character == '/') {
                bCheck = false;


                break;


            } else if (character == ':') {
                bCheck = false;


                break;


            } else if (character == ';') {
                bCheck = false;


                break;


            } else if (character == '=') {
                bCheck = false;


                break;


            } else if (character == '?') {
                bCheck = false;


                break;


            } else if (character == '@') {
                bCheck = false;


                break;


            } else if (character == '[') {
                bCheck = false;


                break;


            } else if (character == '\\') {
                bCheck = false;


                break;


            } else if (character == ']') {
                bCheck = false;


                break;


            } else if (character == '^') {
                bCheck = false;


                break;


            } else if (character == '_') {
                bCheck = false;


                break;


            } else if (character == '`') {
                bCheck = false;


                break;


            } else if (character == '{') {
                bCheck = false;


                break;


            } else if (character == '|') {
                bCheck = false;


                break;


            } else if (character == '}') {
                bCheck = false;


                break;


            } else if (character == '~') {
                bCheck = false;


                break;


            } else if (character == ' ') {
                bCheck = false;


                break;


            } else {
                // the char is not a special one
                // add it to the result as is
                result.append(character);


            }
            character = iterator.next();


        }
        return bCheck;

    }
}