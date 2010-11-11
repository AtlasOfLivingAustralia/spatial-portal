package au.org.emii.portal.composer;

import au.org.emii.portal.menu.Region;
import au.org.emii.portal.mest.SearchQuery;
import au.org.emii.portal.userdata.UserSearch;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.userdata.UserDataDao;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.value.SearchCatalogue;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Window;

public class LeftMenuSearchComposer extends UtilityComposer {

    private static final long serialVersionUID = 2540820748110129339L;
    private Combobox txtsearch;
    private Checkbox chkBBOX;
    private Checkbox chkDate;
    private MESTSearchComposer searchWindow;
    private Datebox startdate;
    private Datebox enddate;
    private Doublebox north;
    private Doublebox south;
    private Doublebox east;
    private Doublebox west;
    private Div geoDiv;
    private Div dateDiv;

    private Div savedSearchCont;
    private Div loadSavedSearchcont;
    private Button searchLoad;
    private Listbox savedSearches;
    private Label saveSearchAdvice;
    private Label savedSearchesAdvice;
    private Label saveSearchAdviceLoggedin;
    
    private Label txtsearchError;
    private Label dateRangeError;
    private SearchQuery sq = new SearchQuery();
    private PortalSessionUtilities portalSessionUtilities = null;
    private Settings settings = null;

    //map of event listeners for viewport changes (west Doublebox onchange)
    HashMap<String, EventListener> viewportChangeEvents = new HashMap<String,EventListener>();

    public void onCreate() {
       if (!settings.isDisablePortalUsers() && getPortalSession().isLoggedIn()) {
            loadSavedSearches();
       }
       
    }

    public void doSearch() {

        boolean bDate = false;
        boolean chkSearch = true;

        if (txtsearch.getText().equals("")) {

            txtsearchError.setVisible(true);

        } else {

            txtsearchError.setVisible(false);
            dateRangeError.setVisible(false);

            sq.setSearchTerm(txtsearch.getText());
            sq.setUseBBOX(chkBBOX.isChecked());

            if (chkDate.isChecked()) {
                sq.setUseDate(true);
                if (startdate.getValue() != null) {
                    sq.setStartDate(startdate.getValue());
                    bDate = true;
                } else {
                    bDate = false;
                }

                if (enddate.getValue() != null) {
                    sq.setEndDate(enddate.getValue());
                    bDate = true;
                } else {
                    bDate = false;

                }


                if (bDate) {
                    try {
                    if (enddate.getValue().before(startdate.getValue())) {
                        chkSearch = false;
                        dateRangeError.setVisible(true);
                    }
                    } catch (NullPointerException nex) {
                        logger.debug("No start date to compare to");
                    }

                }
            }

            if (chkSearch) {

                if (sq.isUseBBOX()) {
                    sq.setTop(north.doubleValue());
                    sq.setBottom(south.doubleValue());
                    sq.setRight(east.doubleValue());
                    sq.setLeft(west.doubleValue());
                }

                Session session = (Session) Sessions.getCurrent();
                session.setAttribute("searchquery", sq);

                if (searchWindow == null) {
                    searchWindow = (MESTSearchComposer) Executions.createComponents(
                            "/WEB-INF/zul/search.zul", null, null);
                } else {
                    searchWindow.detach();
                    searchWindow = (MESTSearchComposer) Executions.createComponents(
                            "/WEB-INF/zul/search.zul", null, null);
                }

                searchWindow.setId(java.util.UUID.randomUUID().toString());
                searchWindow.setMaximizable(true);
                searchWindow.setPosition("center");
                searchWindow.doOverlapped();
            }
            
        }

    }

    public void onOK$textsearch(Event event) {
        doSearch();
    }

    public void onCheck$chkBBOX(Event event) {
        geoDiv.setVisible(chkBBOX.isChecked());
    }

    public void onCheck$chkDate(Event event) {
        dateDiv.setVisible(chkDate.isChecked());
    }

    public void onTextsearch(Event event) {
        doSearch();
    }

    public void onClick$searchLoad(Event event) {
        //go get the details of the search
        if (savedSearches.getItemCount() == 0) {
            logger.debug("no saved searches in listbox");
        } else {
            String searchName;
            if (savedSearches.getSelectedItem() == null) {
                // user hasn't selected anything yet so magically select first
                // item for them.
                searchName = (String) savedSearches.getItemAtIndex(0).getValue();
            } else {
                searchName = (String) savedSearches.getSelectedItem().getValue();
            }

            UserSearch us = getMapComposer().getUserDataManager().getUserSearchByName(searchName);

            txtsearch.setText(us.getKeyword());


            //try the bbox stuff

            try {

                Double n = us.getNorth();
                if (!n.equals(-999.0)) {
                    east.setValue(us.getEast());
                    north.setValue(us.getNorth());
                    south.setValue(us.getSouth());
                    west.setValue(us.getWest());
                    chkBBOX.setChecked(true);
                } else {
                    logger.debug("no BBOX");
                    chkBBOX.setChecked(false);
                }

            } catch (NullPointerException nex) {
                chkBBOX.setChecked(true);
                logger.debug("No bbox");
            }


            //try the date stuff


            try {


                if (us.getEndDate().equals(null) && us.getStartDate().equals(null)) {
                    startdate.setValue(null);
                    enddate.setValue(null);
                    chkDate.setChecked(false);
                } else {
                    startdate.setValue(us.getStartDate());
                    enddate.setValue(us.getEndDate());
                    chkDate.setChecked(true);
                }

            }catch (NullPointerException nex) {
                chkDate.setChecked(false);
                logger.debug("No date");
            }

            geoDiv.setVisible(chkBBOX.isChecked());
            dateDiv.setVisible(chkDate.isChecked());

            doSearch();
        }
    }

    public void onClick$btnSearch(Event event) {
        doSearch();
    }

    /**
     * Populate the search terms box
     */
    private void populateSearchTerms() {
        // populate search terms from current MEST search catalogue
        PortalSession portalSession = getPortalSession();
        SearchCatalogue searchCatalogue = null;//portalSessionUtilities.getSelectedSearchCatalogue(portalSession);
        if (searchCatalogue == null) {
            logger.error(String.format("requested search catalogue '%s' is not available - search is broken!",
                            portalSession.getSelectedSearchCatalogueId()));
        } else {
            List<String> searchTerms = searchCatalogue.getSearchKeywords();

            if (searchTerms == null) {
                logger.warn(
                        "no search keywords available to populate MEST search combobox (is MEST server at '"
                        + searchCatalogue.getUri() + "' down?)");
            } else {
                SimpleListModel dictModel = new SimpleListModel(searchTerms);
                txtsearch.setModel(dictModel);
            }
        }
    }

    public void loginActions() {
        toggleSavedSearchControls(true);
    }

    public void logoutActions() {
        toggleSavedSearchControls(false);
    }

     private void toggleSavedSearchControls(boolean show) {
       logger.debug("toggleSavedSearchcon: " + show);
       savedSearchCont.setVisible(show);
       saveSearchAdvice.setVisible(!show);
       saveSearchAdviceLoggedin.setVisible(show);

       // update the search results popup
       if (searchWindow !=  null) {
           if (show) {
                searchWindow.loginActions();
           } else {
                searchWindow.logoutActions();
           }
       }

       loadSavedSearches();

    }

     public void loadSavedSearches() {
        Boolean entries = false;
        savedSearches.getItems().clear();
        
        UserDataDao x  = getMapComposer().getUserDataManager();
        x.fetchUser(getPortalSession().getPortalUser().getUsername());

    	for (UserSearch u : x.getUserSearches()) {
            savedSearches.appendItem(u.getSearchName(), u.getSearchName());
            logger.debug( " saved searches - " + u.getSearchName());
            entries = true;
	   }
        // dont show the list if its empty
        if (!entries) {
              loadSavedSearchcont.setVisible(false);
               savedSearchesAdvice.setVisible(true);
               logger.debug(getMapComposer().getUserDataManager().getCurrentUser().getUserName() +    " saved searches empty ");
        }        
        else {               
               loadSavedSearchcont.setVisible(true);
               savedSearchesAdvice.setVisible(false);
        }        
     }

    @Override
    public void afterCompose() {
        super.afterCompose();

        populateSearchTerms();
    }

    public BoundingBox getViewportBoundingBox() {
        BoundingBox bbox = new BoundingBox();
        bbox.setMaxLatitude(north.getValue().floatValue());
        bbox.setMinLatitude(south.getValue().floatValue());
        bbox.setMinLongitude(west.getValue().floatValue());
        bbox.setMaxLongitude(east.getValue().floatValue());
        return bbox;
    }

    public void onClick$searchDel() {
        if (savedSearches.getItemCount() == 0) {
            logger.debug("list empty - nothing to do");
        } else {
            Listitem item = savedSearches.getSelectedItem();
            if (item == null) {
                // user never clicked on an item but then first item will
                // be visible - select it since this is what users expects
                item = savedSearches.getItemAtIndex(0);
            }
            getMapComposer().getUserDataManager().deleteSearch(
                    StringEscapeUtils.escapeSql((String)item.getValue()));
            loadSavedSearches();
        }
    }

    public void onChange$west(Event e) {
        //echo to give time for all doubleboxes to be updated
        Events.echoEvent("triggerViewportChange", this, null);
    }

    public void triggerViewportChange(Event e) throws Exception {
        //update bounding box for this session
        BoundingBox bb = new BoundingBox();
        bb.setMinLatitude(south.getValue().floatValue());
        bb.setMaxLatitude(north.getValue().floatValue());
        bb.setMinLongitude(west.getValue().floatValue());
        bb.setMaxLongitude(east.getValue().floatValue());
        getMapComposer().getPortalSession().setDefaultBoundingbox(bb);
        
        for (EventListener el : viewportChangeEvents.values()) {
            try {
                el.onEvent(null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void addViewportEventListener(String eventName, EventListener eventListener) {
        viewportChangeEvents.put(eventName, eventListener);
    }

    public void removeViewportEventListener(String eventName) {
        viewportChangeEvents.remove(eventName);
    }
}
