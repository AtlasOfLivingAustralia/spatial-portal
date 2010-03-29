package au.org.emii.portal.composer;

import au.org.emii.portal.databinding.ActiveLayerRenderer;
import au.org.emii.portal.databinding.BaseLayerListRenderer;
import au.org.emii.portal.request.DesktopState;
import au.org.emii.portal.databinding.EmptyActiveLayersRenderer;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.wms.ImageTester;
import au.org.emii.portal.menu.Link;
import au.org.emii.portal.motd.MOTD;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.databinding.MapLayerItemRenderer;
import au.org.emii.portal.databinding.MapLayerModel;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.menu.MenuItem;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.session.StringMedia;
import au.org.emii.portal.menu.TreeChildIdentifier;
import au.org.emii.portal.databinding.UserDefinedMapLayerItemRenderer;
import au.org.emii.portal.userdata.UserMap;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.service.LogoutService;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.session.PortalUser;
import au.org.emii.portal.userdata.DaoRegistry;
import au.org.emii.portal.userdata.UserDataDao;
import au.org.emii.portal.userdata.UserDataDaoImpl;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.web.SessionInitImpl;
import com.sun.tools.hat.internal.model.JavaObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.rest.GazetteerSearch;
import org.ala.rest.SearchResultItem;
import org.ala.spatial.gazetteer.GazetteerSearchController;
import org.ala.spatial.gazetteer.GazetteerSearchResult;
import org.ala.spatial.gazetteer.GazetteerSearchResultSummary;
import org.ala.spatial.search.TaxaScientificSearchResult;
import org.ala.spatial.search.TaxaScientificSearchSummary;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.SessionInit;
import org.zkoss.zkex.zul.West;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Radio;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Window;
import org.zkoss.zul.api.Textbox;

/**
 * ZK composer for the index.zul page
 *
 *
 * @author geoff
 *
 */
public class MapComposer extends GenericAutowireAutoforwardComposer {

    protected DesktopState desktopState = new DesktopState();
    private SettingsSupplementary settingsSupplementary = null;
    private static final String MENU_DEFAULT_WIDTH = "menu_default_width";
    private static final String MENU_MINIMISED_WIDTH = "menu_minimised_width";
    private static final long serialVersionUID = 1L;
    private RemoteMap remoteMap = null;

    /*
     * Autowired controls
     */
    private Window externalContentWindow;
    private Iframe rawMessageIframeHack;
    private Div rawMessageHackHolder;
    private Tabbox accordionMenu;
    private Tab layerNavigationTab;
    private Tab searchNavigationTab;
    private Tab linkNavigationTab;
    private Tab facilitiesTab;
    private Tab regionsTab;
    private Tab userTab;
    private Tab realtimeTab;
    private Combobox styleList;
    private Component layerNavigationTabContent;
    private Component searchNavigationTabContent;
    private Component linkNavigationTabContent;
    private Tabpanel facilitiesTabPanel;
    private Tabpanel regionsTabPanel;
    private Tabpanel userTabPanel;
    private Tabpanel realtimeTabPanel;
    private Slider opacitySlider;
    private Label opacityLabel;
    private Listbox activeLayersList;
    private Div layerControls;
    private Listbox baseLayerList;
    private Div styleControls;
    private Div transectControl;
    private HtmlMacroComponent animationControlsComposer;
    private Component searchPage;
    private Textbox safeToLoadMap;
    private Image layerSwitcherShow;
    private Image layerSwitcherHide;
    private Div layerSwitcherContainer;
    private Div activeLayersHolder;
    private West menus;
    private Div westContent;
    private Div westMinimised;
    private Textbox extUrl;
    private Textbox extSld;
    private Textbox extLabel;
    private Textbox extLayer;
    private Textbox extType;
    private Textbox extSubmit;
    private Textbox extCql;
    private Div addLayer;
    private Button addExtLayersButton;
    private Button saveMap;
    private Button mapSave;
    private Textbox createSavedMap;
    private Button loadSavedMapButton;
    private Div loadMapcont;
    private Div saveLoadMapContainer;
    private Label createSavedMapAdvice;
    private Div createMapcont;
    private Listbox savedMaps;
    private Label saveMapAdvice;
    private Label createSavedMapError;
    private Toolbarbutton loginButton;
    private Toolbarbutton logoutButton;
    /**
     * Logout service spring bean - autowired
     */
    private LogoutService logoutService = null;

    /*
     * User data object to allow for the saving of maps and searches
     */
    private UserDataDao userDataManager = null;
    private LanguagePack languagePack = null;
    private MOTD motd = null;
    private OpenLayersJavascript openLayersJavascript = null;
    private HttpConnection httpConnection = null;
    private ImageTester imageTester = null;
    private MapLayerItemRenderer mapLayerItemRenderer = null;
    private UserDefinedMapLayerItemRenderer userDefinedMapLayerItemRenderer = null;
    private ActiveLayerRenderer activeLayerRenderer = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    private Settings settings = null;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport = null;
    //additional controls for the ALA Species Search stuff
    private Textbox txtSearchSpecies;
    private Radio rdoCommonSearch;
    private Radio rdoScientificSearch;
    private Button gazSearch;
    private Textbox placeName;
    private Label resultGaz;
    private Listbox gazetteerResults;
    private GazetteerSearchController gazetteerSearchWindow;
    

    public UserDataDao getUserDataManager() {
        if (userDataManager == null) {
            userDataManager = DaoRegistry.getUserDataDao();
        }

        return userDataManager;

    }

    public void onCreate() {

        if (!settings.isDisablePortalUsers() && getPortalSession().isLoggedIn()) {
            userDataManager.fetchUser(getPortalSession().getPortalUser().getUsername());
            updateUserMapList();
        }

    }

    public void setUserDataManager(UserDataDaoImpl userDataManager) {
        this.userDataManager = userDataManager;
    }

    private void motd() {
        if (motd.isMotdEnabled()) {
            logger.debug("displayling MOTD");
            MOTDComposer composer = (MOTDComposer) Executions.createComponents("/WEB-INF/zul/MOTD.zul", this, null);
            composer.doOverlapped();
        }
    }

    public void onClick$removeAllLayers() {
        if (safeToPerformMapAction()) {
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            StringBuffer script = new StringBuffer();
            while (activeLayers.size() > 0) {
                MapLayer mapLayer = activeLayers.get(0);
                script.append(openLayersJavascript.removeMapLayer(mapLayer));

                // skip executing JS and reseting the layer controls - do
                // them at the end
                deactiveLayer(mapLayer, false, false);
            }
            updateLayerControls();
            openLayersJavascript.execute(
                    openLayersJavascript.iFrameReferences
                    + script.toString());
        }
    }

    public void onChange$safeToLoadMap() {
        mapLoaded(safeToLoadMap.getValue());
    }

    /**
     * Region tab click
     */
    public void onClick$regionsTab() {
        selectView(PortalSession.LAYER_REGION_TAB);
    }

    public void onClick$facilitiesTab() {
        selectView(PortalSession.LAYER_FACILITY_TAB);
    }

    public void onClick$realtimeTab() {
        selectView(PortalSession.LAYER_REALTIME_TAB);
    }

    public void onClick$userTab() {
        selectView(PortalSession.LAYER_USER_TAB);
    }

    public void onClick$addExtLayersButton() {
        addLayer.setVisible(true);
        addExtLayersButton.setVisible(false);

    }

    public void onClick$gazSearch() {
        String pName = placeName.getValue();
        //searchGazetteer(pName);

         Session session = (Session) Sessions.getCurrent();
         session.setAttribute("searchGazetteerTerm", pName);


         if (gazetteerSearchWindow == null) {
                    gazetteerSearchWindow= (GazetteerSearchController) Executions.createComponents(
                            "/WEB-INF/zul/GazetteerSearchResults.zul", null, null);
                } else {
                    gazetteerSearchWindow.detach();
                    gazetteerSearchWindow = (GazetteerSearchController) Executions.createComponents(
                            "/WEB-INF/zul/GazetteerSearchResults.zul", null, null);
                }

                gazetteerSearchWindow.setId(java.util.UUID.randomUUID().toString());
                gazetteerSearchWindow.setMaximizable(true);
                gazetteerSearchWindow.setPosition("center");
                gazetteerSearchWindow.doOverlapped();

    }

    

    public void closeAddLayerDiv() {
        addLayer.setVisible(false);
        addExtLayersButton.setVisible(true);
    }

    /**
     * Reorder the active layers list based on a d'n'd event
     * @param dragged
     * @param dropped
     */
    public void reorderList(Listitem dragged, Listitem dropped) {
        logger.debug(dragged.getLabel() + " dropped on " + dropped.getLabel());

        // get the position in the list
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        int droppedIndex = activeLayers.indexOf(dropped.getValue());

        ListModelList model = (ListModelList) activeLayersList.getModel();
        model.remove(dragged.getValue());
        model.add(droppedIndex, dragged.getValue());

        // tell openLayers to change zIndexs
        openLayersJavascript.updateMapLayerIndexesNow(activeLayers);

    }

    /**
     * Remove a maplayer from the active layers list and then
     * reinsert it at the same spot - should cause this part
     * of the list to be re-rendered.
     *
     * After re-rendering, reselect the corresponding item in
     * its listbox, as operations such as changing opacity
     * and animation require a layer is selected
     * @param mapLayer
     */
    public void refreshActiveLayer(MapLayer mapLayer) {
        ListModelList model = (ListModelList) activeLayersList.getModel();
        int index = model.indexOf(mapLayer);
        model.remove(index);
        model.add(index, mapLayer);

        activeLayersList.setSelectedIndex(index);

    }

    public void activateMenuItem(MenuItem item) {
        logger.debug("activate MenuItem: " + item.getId());
        if (item.isValueMapLayerInstance()) {
            activateLayer(item.getValueAsMapLayer(), true);
        } else if (item.isValueLinkInstance()) {
            activateLink(item.getValueAsLink());
        } else if (!item.isValueSet()) {
            logger.info(
                    "Can't activate menu item - null value instance");

        } else {
            logger.info(
                    "Can't activate menu item - unsupported value type: "
                    + String.valueOf(item.getValue()));
        }
    }

    /**
     * Radio buttons are generated dynamically so we can't autowire
     * them.
     * @param id
     * @return
     */
    private Radio getRadioButton(String id) {
        return (id == null) ? null : (Radio) getFellow("radio_" + id);
    }

    public void selectView(int view) {
        PortalSession ps = getPortalSession();
        ps.setLayerTab(view);

        // if we changed to the user defined view, we need to
        // load the menu too
        if (view == PortalSession.LAYER_USER_TAB) {
            showMenu(view, null);
        } else {
            // showMenu call does this for us when we display the user defined menu
            ps.setDisplayingUserDefinedMenuTree(false);
        }
    }

    public void activateLink(Link link) {
        logger.debug("activate Link " + link.getId());

        if (link.isExternal()) {
            // change browsers current location
            Clients.evalJavaScript(
                    "window.location.href ='" + link.getUri() + "';");
        } else {

            // iframe in another id-space so can't be auto wired
            Iframe iframe = getExternalContentIframe();
            iframe.setSrc(link.getUri());

            // use the link description as the popup caption
            ((Caption) externalContentWindow.getFellow("caption")).setLabel(
                    link.getDescription());
            externalContentWindow.setPosition("center");
            externalContentWindow.doOverlapped();

        }


    }

    public void activateBaseLayer(String id) {
        PortalSession portalSession = getPortalSession();
        MapLayer baseLayer = portalSessionUtilities.getBaseLayerById(portalSession, id);
        portalSession.setCurrentBaseLayer(baseLayer);
        activateBaseLayer(baseLayer);
    }

    public void activateBaseLayer(MapLayer baseLayer) {
        openLayersJavascript.activateMapLayerNow(baseLayer);
    }

    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript) {
        return activateLayer(mapLayer, doJavaScript, false);
    }

    /**
     * Activate a map layer on the map
     * @param layer MapLayer instance to activate
     * @param doJavaScript set false to defer execution of JavaScript
     * which actually adds the layer to the openlayers menu
     *
     * @return true if the layer was added successfully, otherwise false
     */
    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript, boolean skipTree) {
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        boolean layerAdded = false;

        if (!activeLayers.contains(mapLayer) && mapLayer.isDisplayable()) {
            // layer not already active - add it
            if (!skipTree) {
                ((MapLayerModel) getMenuTree().getModel()).changeSelection(mapLayer, true);
            }

            /* assume we want to display on the map straight away - set checkbox
             * to true
             */
            activeLayersList.setItemRenderer(activeLayerRenderer);

            /* switch to the ListModelList if we are currently using simplelistmodel
             * to display the 'no layers selected' message
             *
             * If the model is already an instance of ListModelList then
             * the model should already have the data it needs so just
             * fire the update event.
             */
            if (!(activeLayersList.getModel() instanceof ListModelList)) {
                logger.debug("changing model for Active Layers to ListModelList");
                /* this is the first item being added to the list so we make
                 * it a new ListModelList instance based on live data
                 */
                activeLayersList.setModel(new ListModelList(activeLayers, true));
            }


            /* use the MODEL facade to add the new layer (it's not smart enough
             * to detect the change otherwise.
             *
             * We always add to the top of the list so that newly actived
             * map layers display above existing ones
             */
            ((ListModelList) activeLayersList.getModel()).add(0, mapLayer);

            // update the map
            if (doJavaScript) {
                openLayersJavascript.activateMapLayerNow(mapLayer);
            }

            updateLayerControls();
            layerAdded = true;
        } else {
            logger.debug(
                    "not displaying map layer because its already listed or is marked non-displayable");
        }
        return layerAdded;

    }

    /**
     * Remove an item from the list of active layers and put it back in the
     * tree menu of available layers
     * @param itemToRemove
     */
    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive) {
        if (itemToRemove != null) {
            boolean deListedInActiveLayers = false;

            /* redraw the current menu tree if contains
             * the item we are removing, otherwise just
             * leave deListedInActiveLayers flag as it
             * is which marks the layer as not being
             * in active layers anymore so the tree
             * renderer will pick this up next time a
             * redraw of the menu is requested
             */
            Tree menuTree = getMenuTree();
            if (menuTree != null) {
                MapLayerModel treemodel = (MapLayerModel) menuTree.getModel();
                if (treemodel != null && treemodel.isHoldingMapLayer(itemToRemove)) {
                    treemodel.changeSelection(itemToRemove, false);
                    deListedInActiveLayers = true;
                }
            }

            // update the active layers list
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            if (activeLayers != null) {
                logger.debug("obtained activelayers arraylist from session, count: " + activeLayers.size());
                if (activeLayers.size() > 0) {
                    ListModelList listmodel = (ListModelList) activeLayersList.getModel();
                    if (listmodel != null) {
                        listmodel.remove(itemToRemove);

                        if (activeLayers.size() == 0) {
                            displayEmptyActiveLayers(activeLayersList);
                        }
                    }
                } else {
                    logger.debug("active layers list is empty, so not updating it");
                }
            }

            /* only the items in the active menu will have been marked
             * as being displayed (gets done by the changeSelection()
             * method), so we must set the listedInActiveLayers flag
             * ourself if we skipped this stage because the item is
             * in a menu that's not being displayed
             */
            if (!deListedInActiveLayers) {
                itemToRemove.setListedInActiveLayers(false);
            }

            if (updateMapAndLayerControls) {
                // tell openlayers to kill the layer
                openLayersJavascript.removeMapLayerNow(itemToRemove);
                updateLayerControls();
            }

            // descend children if we were called recursively
            if (recursive && itemToRemove.hasChildren()) {
                for (MapLayer child : itemToRemove.getChildren()) {
                    deactiveLayer(child, updateMapAndLayerControls, recursive);
                }
            }
        }
    }

    /**
     * Removes a MapLayer instance from the user defined menu in
     * portal session and then updates the relevant tree menu
     * @param itemToRemove
     */
    public void removeUserDefinedMenuItem(MenuItem itemToRemove) {
        if (itemToRemove != null && (itemToRemove.isValueMapLayerInstance())) {
            /* for the zk tree to pickup modifications, we have
             * to manipulate the underlying data through it's
             * model.  We can only do this if we are currently
             * displaying the user defined menu tree though,
             * otherwise, we have to remove the data directly
             * ourself.
             */
            PortalSession ps = getPortalSession();
            TreeChildIdentifier tci = portalSessionUtilities.removeUserDefinedMapLayer(ps, itemToRemove);
            Tree userDefinedMenu = getMenuTree("user");
            if (tci != null) {
                if (userDefinedMenu != null) {
                    MapLayerModel model = (MapLayerModel) userDefinedMenu.getModel();
                    if (model != null) {
                        // we remove the item at the position corresponding to the
                        // list entry in PortalSession
                        model.updateTreeItemRemoved(tci.getTreeMenuItem(), tci.getIndex());
                    } else {
                        logger.warn(
                                "null model detected for user defined tree menu - should never happen");
                    }
                } else {
                    logger.info("menutree for 'user' is null - can't update");
                }
            } else {
                logger.info("item not found in portalSession.userDefinedMenu; id=" + itemToRemove.getId());
            }
        } else {
            logger.info("itemToRemove is null or does not contain a MapLayer instance in it's value field");
        }
    }

    /**
     * A simple message dialogue
     * @param message Full text of message to show
     */
    public void showMessage(String message) {
        ErrorMessageComposer window = (ErrorMessageComposer) Executions.createComponents("WEB-INF/zul/ErrorMessage.zul", null, null);
        window.setMessage(message);
        window.doOverlapped();
    }

    /**
     * Show a message dialogue.  Initially a short message is
     * shown but the user can click 'show details' to get a more
     * informative message.
     *
     * A default message title is obtained from the config file
     * @param message
     * @param messageDetail
     */
    public void showMessage(String message, String messageDetail) {
        showMessage(languagePack.getLang("default_message_title"), message, messageDetail);
    }

    /**
     * Show a message dialogue.  Initially a short message is
     * shown but the user can click 'show details' to get a more
     * informative message.
     *
     * A title must be provided for the popup box
     * @param title
     * @param message
     * @param messageDetail
     */
    public void showMessage(String title, String message, String messageDetail) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageTitle", title);
        params.put("message", message);
        params.put("messageDetail", messageDetail);

        Window window = (Window) Executions.createComponents("WEB-INF/zul/ErrorMessageWithDetail.zul", null, params);
        window.doOverlapped();
    }

    /**
     * Show message for broken wms server
     * @param rm
     */
    public void errorMessageBrokenWMSServer(RemoteMap rm) {
        showMessage(
                languagePack.getLang("wms_server_added_error"),
                rm.getDiscoveryErrorMessageSimple(),
                rm.getDiscoveryErrorMessage(),
                "Link to WMS server",
                rm.getLastUriAttempted(),
                "Raw data from location",
                httpConnection.readRawData(rm.getLastUriAttempted()));
    }

    /**
     * Show message for broken WMS layer
     */
    public void errorMessageBrokenWMSLayer(ImageTester it) {
        showMessage(
                languagePack.getLang("wms_layer_added_error"),
                it.getErrorMessageSimple(),
                it.getErrorMessage(),
                "Link to test image",
                it.getLastUriAttempted(),
                "Raw image data",
                httpConnection.readRawData(it.getLastUriAttempted()));
    }

    /**
     * This is a fixed size (because of zk limitations) message
     * dialogue featuring:
     * 	o	title
     * 	o	brief description
     * 	o	detailed description (hidden by default)
     * 	o	link to raw data (hidden by default)
     * 	o	Iframe full of raw data (hidden by default)
     * There are lots of parameters for this method so if you want
     * to use it, it's easiest to write a wrapper and just call that
     * when you have a problem.
     *
     * This is not a general purpose error message - everything
     * is fixed sizes (has to be or the iframe doesn't display
     * properly.  If you want a general purpose error message, use
     * the showMessageNow() calls
     *
     * @param title TEXT of the title or null to use default from config file
     * @param message TEXT in config file of the error message
     * @param messageDetail TEXT to display if user clicks 'show detail'
     * @param rawMessageTitle TEXT to display before raw output or null to ignore
     * @param rawMessage TEXT of raw error message or null to ignore
     */
    public void showMessage(String title,
            String message,
            String messageDetail,
            String linkTitle,
            String linkHref,
            String rawMessageTitle,
            String rawMessage) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("messageTitle", title);
        params.put("message", message);
        params.put("messageDetail", messageDetail);
        params.put("linkTitle", linkTitle);
        params.put("linkHref", linkHref);
        params.put("rawMessageTitle", rawMessageTitle);

        // this will be the inside of the iframe...

        /* StringMedia is not allowed to be null (will give an error)
         * so if no data was available, give the user a message
         */
        if (rawMessage == null) {
            rawMessage = languagePack.getLang("null_raw_data");
        }
        StringMedia rawErrorMessageMedia = new StringMedia(rawMessage);

        /* have to store the raw error message in the user's session
         * temporarily to prevent getting a big zk severe error if
         * the iframe is requested after it is supposed to have been
         * dereferenced
         */
        getPortalSession().setRawErrorMessageMedia(rawErrorMessageMedia);
        rawMessageIframeHack.setContent(rawErrorMessageMedia);

        /* the raw text can't go in with the params or it gets
         * escaped by zk - you have to do weird things with
         * iframes and the media class instead
         */

        Window window;
        window = (Window) Executions.createComponents("WEB-INF/zul/ErrorMessageWithDetailAndRawData.zul", null, params);

        /* now we grab the hidden iframe in index.zul and move it into
         * our message box - again this is to prevent a massive zk error
         * if the iframe content is requested after it's supposed to have
         * gone
         */
        Component holder = window.getFellow("rawMessageHolder");
        rawMessageIframeHack.setParent(holder);
        window.doOverlapped();

        /* at this point the user has closed the message box - we
         * only have one more thing to do to stop the big zk error
         * and that is to grab the iframe back off the error message
         * window and put it back where we found it in the index.zul
         * page - not pretty or efficient but works a treat!
         */
        //rawMessageIframeHack.setParent(rawMessageHackHolder);
    }

    /**
     * Add a map layer to the user defined map layers group (My Layers)
     * @param layer
     *
     *
     */
    public void addUserDefinedLayerToMenu(MapLayer mapLayer, boolean activate) {
        if (safeToPerformMapAction()) {
            logger.debug("inside addUserDefinedLayerToMenu");

            PortalSession portalSession = getPortalSession();
            MenuItem menuItem = portalSessionUtilities.addUserDefinedMapLayer(portalSession, mapLayer);

            if (portalSession.isDisplayingUserDefinedMenuTree()) {
                logger.debug("user defined menu is being displayed - updating menus");
                Tree menu = getMenuTree("user");
                if (menu != null) {
                    MapLayerModel mapLayerModel = (MapLayerModel) menu.getModel();
                    if (mapLayerModel != null) {
                        logger.debug("asking tree to update");
                        if (!mapLayerModel.updateTreeItemAdded(menuItem)) {
                            /* we requested the tree be updated (repainted) but it failed -
                             * we still need to sync its tree to the model though so we
                             * touch its model property, otherwise the next step (state
                             * changes) will fail
                             */
                            menu.setModel(new MapLayerModel(portalSession.getMenuForUserDefined()));
                        }
                        if (activate) {
                            logger.debug("marking layer as displayed in active layers");
                            mapLayerModel.changeSelection(mapLayer, true);
                            activateLayer(mapLayer, true, false);
                        }
                    }
                } else {
                    logger.info(
                            "user defined menu is null - it shouldn't be.  No update done");
                }
            } else if (activate) {
                // activate the layer in openlayers and display in active layers without
                // updating the tree (because its not displayed)
                activateLayer(mapLayer, true, true);

                // we must tell any future tree menus that the map layer is already
                // displayed as we didn't use changeSelection()
                mapLayer.setListedInActiveLayers(true);
            }

            logger.debug("leaving addUserDefinedLayerToMenu");
            updateUserDefinedView();
        }
    }

    /**
     * Initial building of the tree menu and active layers list based on
     * the values obtained from the current session.
     *
     * JavaScript for loading default map layers and setting the default
     * zoombox is in SessionInit.java
     */
    public void load() {
        logger.debug("entering loadMapLayers");

        motd();

        PortalSession portalSession = getPortalSession();
        List<MapLayer> activeLayers = portalSession.getActiveLayers();

        // base layers drop down list
        baseLayerList.setModel(new ListModelList(portalSession.getBaseLayers()));
        baseLayerList.setItemRenderer(new BaseLayerListRenderer());
        baseLayerList.setSelectedIndex(
                portalSession.getIndexOfCurrentBaseLayer());


        // model and renderer for active layers list
        ListModelList activeLayerModel = new ListModelList(activeLayers, true);


        // tell the list about them...
        if (activeLayers.size() == 0) {
            displayEmptyActiveLayers(activeLayersList);
        } else {
            activeLayersList.setModel(activeLayerModel);
            activeLayersList.setItemRenderer(activeLayerRenderer);
        }

        // select the radiobutton for the current active region/facility
        // if we are not displaying the user defined menu
        if (!portalSession.isDisplayingUserDefinedMenuTree()) {
            if (portalSession.getSelectedMenuId() != null) {
                getRadioButton(portalSession.getSelectedMenuId()).setChecked(true);
            } else {
                logger.warn("No default facility or region set in portal session - no default menu will be selected");
            }
        }

        // select the correct accordian tab for above
        accordionMenu.setSelectedPanel(
                getTabpanel(portalSession.getCurrentLayerTab()));

        showCurrentMenu();

        updateUserDefinedView();


        activateNavigationTab(portalSession.getCurrentNavigationTab());
        maximise();

    }

    public void updateUserDefinedView() {
        PortalSession portalSession = getPortalSession();
        if (!settings.isDisableUserDefined()) {
            boolean display = portalSession.isUserDefinedViewDisplayable();

            getTabpanel(PortalSession.LAYER_USER_TAB).setVisible(display);
            getTab(PortalSession.LAYER_USER_TAB).setVisible(display);
        }
    }

    public void showCurrentMenu() {
        PortalSession portalSession = getPortalSession();
        String menuId = portalSession.getSelectedMenuId();
        int view = portalSession.getTabForCurrentMenu();

        showMenu(view, menuId);
    }

    private void onActivateTab(int view, boolean activate) {
        Tab tab = getTab(view);
        if (tab != null) {
            tab.setSelected(activate);
        } else {
            logger.warn("no tab found for view " + view);
        }
    }

    private Tab getTab(int view) {
        Tab tab;
        switch (view) {
            case PortalSession.LAYER_FACILITY_TAB:
                tab = facilitiesTab;
                break;
            case PortalSession.LAYER_REGION_TAB:
                tab = regionsTab;
                break;
            case PortalSession.LAYER_USER_TAB:
                tab = userTab;
                break;
            case PortalSession.LAYER_REALTIME_TAB:
                tab = realtimeTab;
                break;
            default:
                logger.warn(
                        "Tab panel ID requested for unknown view: "
                        + view + " will default to Facility");
                tab = facilitiesTab;
        }

        return tab;
    }

    private Tabpanel getTabpanel(int view) {
        Tabpanel tabpanel;
        switch (view) {
            case PortalSession.LAYER_FACILITY_TAB:
                tabpanel = facilitiesTabPanel;
                break;
            case PortalSession.LAYER_REGION_TAB:
                tabpanel = regionsTabPanel;
                break;
            case PortalSession.LAYER_USER_TAB:
                tabpanel = userTabPanel;
                break;
            case PortalSession.LAYER_REALTIME_TAB:
                tabpanel = realtimeTabPanel;
                break;
            default:
                logger.warn(
                        "Tab panel ID requested for unknown view: "
                        + view + " will default to Facility");
                tabpanel = facilitiesTabPanel;
        }

        return tabpanel;
    }

    /**
     * Display an empty list in the active layers box - usually this will
     * consist of a single list element with a value along the lines of
     * 'please select map layers'
     * @param activeLayersList
     */
    private void displayEmptyActiveLayers(Listbox activeLayersList) {
        logger.debug("will display empty active layers list");
        activeLayersList.setItemRenderer(new EmptyActiveLayersRenderer());
        activeLayersList.setModel(new SimpleListModel(new String[]{languagePack.getLang("active_layers_empty")}));
    }

    public void onChange$extSubmit() {
        logger.debug("onchange for id extSubmit");
        extSubmit.setValue("");
        String uri = extUrl.getValue();
        String sld = extSld.getValue();
        String cql = extCql.getValue();
        String name = extLabel.getValue();
        String layer = extLayer.getValue();
        String type = extType.getValue();
        String format = "image/png"; // todo: allow as a param

        PortalSession ps = getPortalSession();

        if (Validate.empty(uri) || Validate.empty(name) || Validate.empty(type)) {
            showMessage(languagePack.getLang("ext_layer_creation_failure2"));
            logger.error("A external layer was requested and failed with: uri=" + uri + " name=" + name + " type=" + type + " layer=" + layer);
        } else {
            if (portalSessionUtilities.getUserDefinedById(ps, uri) == null) {

                MapLayer mapLayer = null;

                if (!Validate.empty(layer)) {
                    // This is a WMS request
                    mapLayer = genericServiceAndBaseLayerSupport.createMapLayer(name, name, type, uri, layer, format, sld, cql);
                } else {
                    // KML request
                    mapLayer = genericServiceAndBaseLayerSupport.createMapLayer(name, name, type, uri);
                }

                if (mapLayer == null) {
                    logger.debug("The layer " + name + " couldnt be created");
                    showMessage(languagePack.getLang("ext_layer_creation_failure"));
                } else {

                    addUserDefinedLayerToMenu(mapLayer, true);
                }
            } else {
                showMessage(languagePack.getLang("wms_layer_already_exists"));
            }
        }

    }

    /**
     * Add a WMS layer identified by the given parameters to the menu system
     * and activate it
     * @param label Name of map layer
     * @param uri URI for the WMS service
     * @param layers layers to ask the WMS for
     * @param imageFormat MIME type of the image we will get back
     * @param opacity 0 for invisible, 1 for solid
     */
    public boolean addWMSLayer(String label, String uri, float opacity) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                MapLayer mapLayer = remoteMap.createAndTestWMSLayer(label, uri, opacity);
                if (mapLayer == null) {
                    // fail
                    errorMessageBrokenWMSLayer(imageTester);
                    logger.info("adding WMS layer failed ");
                } else {
                    // ok
                    addUserDefinedLayerToMenu(mapLayer, true);
                    addedOk = true;
                }
            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_already_exists"));
                logger.info(
                        "refusing to add a new layer with URI " + uri
                        + " because it already exists in the menu");
            }
        }
        return addedOk;
    }

    /**
     * Overridden to allow for the adding of a OGC Filter
     * Add a WMS layer identified by the given parameters to the menu system
     * and activate it
     * @param label Name of map layer
     * @param uri URI for the WMS service
     * @param layers layers to ask the WMS for
     * @param imageFormat MIME type of the image we will get back
     * @param opacity 0 for invisible, 1 for solid
     */
    public boolean addWMSLayer(String label, String uri, float opacity, String filter) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            //if (getPortalSession().getUserDefinedById(uri) == null) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                MapLayer mapLayer = remoteMap.createAndTestWMSLayer(label, uri, opacity);
                if (mapLayer == null) {
                    // fail
                    errorMessageBrokenWMSLayer(imageTester);
                    logger.info("adding WMS layer failed ");
                } else {
                    // ok
                    addUserDefinedLayerToMenu(mapLayer, true);
                    addedOk = true;
                }
            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_already_exists"));
                logger.info(
                        "refusing to add a new layer with URI " + uri
                        + " because it already exists in the menu");
            }
        }
        return addedOk;
    }

    public boolean addWMSGazetteerLayer(String label, String uri, float opacity, String filter) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            //if (getPortalSession().getUserDefinedById(uri) == null) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                MapLayer mapLayer = remoteMap.createAndTestWMSLayer(label, uri, opacity);
                mapLayer.setCql(filter);
                mapLayer.setQueryable(true);
                if (mapLayer == null) {
                    // fail
                    errorMessageBrokenWMSLayer(imageTester);
                    logger.info("adding WMS layer failed ");
                } else {
                    // ok
                    addUserDefinedLayerToMenu(mapLayer, true);
                    addedOk = true;
                }
            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_already_exists"));
                logger.info(
                        "refusing to add a new layer with URI " + uri
                        + " because it already exists in the menu");
            }
        }
        return addedOk;
    }

    /**
     * Select a tab and activate it - same as if a user clicked
     * it in the gui.  There is no corresponding deactivate
     * method as you can acheive the same effect by selecting
     * another tab
     * @param view
     */
    public void selectAndActivateTab(int view) {
        selectView(view);

        // simulate user clicking open on the tab...
        onActivateTab(view, true);
    }

    /**
     * Add a WMS server to the menu system
     * @param name Name for the root menu item or null to obtain from capabilities
     * document
     * @param uri URI to query for getCapabilities document - will be mangled
     * if autodiscovery was enabled by the version parameter (see below)
     * @param version one of ("auto", "1.3.0", "1.1.1", "1.1.0", "1.0.0") if
     * auto is used, the version will be automatically discovered and the url
     * will be mangled to add the required components
     * @param opacity default opacity to be used for all layers discovered
     * @param hostnameInDescription if set to true, the hostname will be appended
     * to the description for the root mapLayer
     * @return true if adding was successful, otherwise false
     */
    public boolean addWMSServer(String name, String uri, String version, float opacity, boolean hostnameInDescription) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            /*
             * First check that the URI is unique within the system (as an ID) - otherwise
             * there will be problems eg if the user adds a duplicate uri
             */
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {

                // make sure all layers are displayed for ud layers
                MapLayer mapLayer = remoteMap.autoDiscover(name, opacity, uri, version);

                // and add it to the map if we got a mapLayer back..
                if (mapLayer != null) {
                    // ok

                    // append the hostname to the description (top level only)
                    if (hostnameInDescription) {
                        try {
                            String hostnameDescription = languagePack.getCompoundLang(
                                    "user_defined_layer_description",
                                    new Object[]{new URL(uri).getHost()});
                            mapLayer.appendDescription(hostnameDescription, true);
                        } // should never happen since we used it to discover from earlier...
                        catch (MalformedURLException e) {
                        }
                    }

                    addUserDefinedLayerToMenu(mapLayer, false);
                    addedOk = true;
                } else {
                    // error
                    errorMessageBrokenWMSServer(remoteMap);
                    logger.info("no data/invalid data received from map server - refusing to add");
                }
            } else {
                // error
                showMessage(languagePack.getLang("wms_server_already_exists"));
                logger.info(
                        "refusing to add a new layer with URI " + uri
                        + " because it already exists in the menu");
            }
        }
        return addedOk;
    }

    /*
     * Public utility methods to interogate the state of the form controls
     */
    /**
     * Return the MapLayer instance associated with the item currently selected
     * in the active layers listbox or null if no item is currently selected.
     *
     * If nothing is selected and alertOnNoSelection is set true, show the user
     * a message and return null
     *
     * @return selected MapLayer instance or null if there is nothing selected
     */
    public MapLayer getActiveLayersSelection(boolean alertOnNoSelection) {
        MapLayer mapLayer = null;

        // only one item can be selected at a time
        Listitem selected = (Listitem) activeLayersList.getSelectedItem();
        if (selected != null) {
            mapLayer = (MapLayer) selected.getValue();
        } else if (alertOnNoSelection) {
            showMessage(languagePack.getLang("active_layer_not_selected"));

            // OK let's see who called us so we can debug this...
            Exception e = new RuntimeException();
            if (e.getStackTrace().length >= 2) {
                StackTraceElement s = e.getStackTrace()[1];
                logger.error(
                        "***FIXME*** no active layer selected - getActiveLayersSelection() invoked by "
                        + s.getClassName() + "::"
                        + s.getMethodName()
                        + " on line "
                        + s.getLineNumber());
            }
        }

        return mapLayer;
    }

    /**
     * Simple test to see whether we have active layers in the active layers
     * list or not
     * @return
     */
    public boolean haveActiveLayers() {
        boolean haveActiveLayers;
        if (getPortalSession().getActiveLayers().size() > 0) {
            haveActiveLayers = true;
        } else {
            haveActiveLayers = false;
        }
        return haveActiveLayers;
    }

    public Tab getNavigationTab(int tab) {
        Tab component = null;

        switch (tab) {
            case PortalSession.LAYER_TAB:
                component = layerNavigationTab;
                break;
            case PortalSession.SEARCH_TAB:
                component = searchNavigationTab;
                break;
            case PortalSession.LINK_TAB:
                component = linkNavigationTab;
                break;
            default:
                logger.warn("no navigation tab found for " + tab);
        }

        return component;
    }

    public Component getNavigationTabContent(int tab) {
        Component component = null;

        switch (tab) {
            case PortalSession.LAYER_TAB:
                component = layerNavigationTabContent;
                break;
            case PortalSession.SEARCH_TAB:
                component = searchNavigationTabContent;
                break;
            case PortalSession.LINK_TAB:
                component = linkNavigationTabContent;
                break;
            default:
                logger.warn("no navigation tab content found for " + tab);
        }

        return component;
    }

    public void onClick$layerNavigationTab() {
        activateNavigationTab(PortalSession.LAYER_TAB);
    }

    public void onClick$searchNavigationTab() {
        activateNavigationTab(PortalSession.SEARCH_TAB);
    }

    public void onClick$linkNavigationTab() {
        activateNavigationTab(PortalSession.LINK_TAB);
    }

    public void onSelect$baseLayerList() {
        changeBaseLayer((String) baseLayerList.getSelectedItem().getValue());
    }

    public void activateNavigationTab(int tab) {
        PortalSession portalSession = getPortalSession();

        Tab currentTab = getNavigationTab(portalSession.getCurrentNavigationTab()); //.setSelected(false);
        Component currentContent = getNavigationTabContent(portalSession.getCurrentNavigationTab()); //.setVisible(false);

        Tab newTab = getNavigationTab(tab); //.setSelected(true);
        Component newContent = getNavigationTabContent(tab); //.setVisible(true);
        portalSession.setCurrentNavigationTab(tab);

        // hide old tab
        if (currentTab != null) {
            currentTab.setSelected(false);
            currentContent.setVisible(false);
        }

        // show new stuff
        if (newTab == null) {
            logger.error("can't display tab content for tab ID=" + tab);
        } else {
            newTab.setSelected(true);
            newContent.setVisible(true);
        }

        // redraw current menu if change was to layers tab
        if (tab == PortalSession.LAYER_TAB) {
            // fix for #90
            logger.debug("forced redraw of current tree menu");
            Tree menu = getMenuTree();
            if (menu != null) {
                menu.setModel(menu.getModel());
            } else {
                logger.warn("detected null menu - should never happen");
            }

        }
    }

    public void onSelect$activeLayersList(ForwardEvent event) {
        updateLayerControls();
    }

    /**
     * Enable or disable layer controls depending on the current selection.
     *
     * At the moment this shows/hides the opacity controls
     */
    public void updateLayerControls() {
        MapLayer currentSelection = getActiveLayersSelection(false);
        /* only show /or attempt to update the controls when:
         * 	1	there are some active layers
         * 	2	a layer is selected (not safe without (1) because the selected item
         * 		might be the message saying there are no layers available
         *  3	the selected layer is being displayed in openlayers
         */
        if (haveActiveLayers()
                && (currentSelection != null)
                && currentSelection.isDisplayed()) {

            /* display the layer controls and set the slider and label to
             * the current layer opacity
             */
            int percentage = (int) (currentSelection.getOpacity() * 100);
            Slider slider = (Slider) layerControls.getFellow("opacitySlider");
            slider.setCurpos(percentage);
            opacityLabel.setValue(percentage + "%");

            /*
             * populate the list of styles
             */
            if (currentSelection.hasStyles()) {

                styleList.setModel(new ListModelList(currentSelection.getStyles()));
                logger.debug("select style: " + currentSelection.getSelectedSystemStyleName());
                styleList.setValue(currentSelection.getSelectedSystemStyleName());
                styleControls.setVisible(true);
            } else {
                styleControls.setVisible(false);
            }

            /*
             * switch the transect drawing div
             */
            if (currentSelection.isNcWmsType()) {
                transectControl.setVisible(true);
            } else {
                transectControl.setVisible(false);
            }

            // show animation controls if needed
            getAnimationControlsComposer().updateAnimationControls(currentSelection);

            layerControls.setVisible(true);
        } else {
            layerControls.setVisible(false);
        }
    }

    public void mapLoaded(String text) {
        boolean loaded = Boolean.parseBoolean(text);
        getPortalSession().setMapLoaded(loaded);
    }

    /**
     * Check if its safe to do things to the map - if it's not,
     * show a popup box
     * @return
     */
    public boolean safeToPerformMapAction() {
        boolean safe;
        if (openLayersJavascript.mapLoaded()) {
            safe = true;
        } else {
            showMessage(languagePack.getLang("map_load_error"));
            safe = false;
        }
        return safe;
    }

    /**
     * Change the opacity for the passed in mapLayer and update openlayers.
     *
     * If the opacity controls are being displayed and match the currently
     * selected layer in active layers,  update the label and slider as well
     * @param mapLayer
     * @param opacity
     */
    public void changeOpacity(MapLayer mapLayer, float opacity) {

        /* get reference to the slider if we can - that way we can
         * try to put the original position back if its not safe
         * to do things on the map yet
         */
        if (safeToPerformMapAction()) {
            logger.debug("opacity change: " + mapLayer.getId() + " " + opacity + "%");
            mapLayer.setOpacity(opacity);
            if (layerControls.isVisible() && (getActiveLayersSelection(true) == mapLayer)) {
                int percentage = (int) (opacity * 100);
                opacitySlider.setCurpos(percentage);
                opacityLabel.setValue(percentage + "%");
            }
            openLayersJavascript.setMapLayerOpacityNow(mapLayer, opacity);
        } else {
            // attempt to restore slider value
            if (opacitySlider != null) {
                opacitySlider.setCurpos((int) (mapLayer.getOpacity() * 100));
            }
        }
    }

    /**
     * Extract the value of custom attribute systemId from the passed
     * in ForwardEvent instance
     * @param event
     * @return value of custom attribute systemId
     */
    private String getSystemId(ForwardEvent event) {
        return (String) event.getOrigin().getTarget().getAttribute("systemId");
    }

    public void onSelectRegion(ForwardEvent event) {
        selectRegion(getSystemId(event), true);
    }

    public void selectRegion(String id, boolean doJavascript) {
        showMenu(PortalSession.LAYER_REGION_TAB, id);
        if (doJavascript) {
            openLayersJavascript.zoomToBoundingBoxNow(getPortalSession().getRegions().get(id).getBoundingBox());
        }
    }

    public void onSelectFacility(ForwardEvent event) {
        showMenu(PortalSession.LAYER_FACILITY_TAB, getSystemId(event));
    }

    public void onSelectUser(ForwardEvent event) {
        showMenu(PortalSession.LAYER_USER_TAB, null);
    }

    public void onSelectRealtime(ForwardEvent event) {
        showMenu(PortalSession.LAYER_REALTIME_TAB, getSystemId(event));
    }

    private void showMenu(MenuGroup menu, TreeitemRenderer renderer) {
        Tree menuTree = getMenuTree();
        if (menu != null) {
            MapLayerModel mapLayerModel = new MapLayerModel(menu);
            menuTree.setTreeitemRenderer(renderer);
            menuTree.setModel(mapLayerModel);
            logger.debug("menu set to: \n" + menu.dump(" "));
        }



    }

    /**
     * Show the tree menu for the given view
     * @param id
     * @param menu
     */
    private void showMenu(int view, String menuId) {
        PortalSession portalSession = getPortalSession();
        TreeitemRenderer renderer = null;
        MenuGroup menu = null;
        boolean okToChangeMenu = false;
        boolean displayUserDefined = false;

        // hide the old tree if one exists
        Tree oldMenu = getMenuTree();

        if (oldMenu != null) {
            getMenuTree().setModel(null);
        }

        // update portalSession to select new menu
        switch (view) {
            case PortalSession.LAYER_FACILITY_TAB:
            case PortalSession.LAYER_REALTIME_TAB:
            case PortalSession.LAYER_REGION_TAB:
                if (menuId == null) {
                    logger.warn("null menuId for view other than VIEW_USER - can't display menu");
                    okToChangeMenu = false;
                } else {
                    portalSession.setSelectedMenuId(menuId);
                    renderer = mapLayerItemRenderer;
                    okToChangeMenu = true;
                    displayUserDefined = false;
                }
                break;
            case PortalSession.LAYER_USER_TAB:
                // use a special renderer for user defined layers - adds a
                // remove icon
                renderer = userDefinedMapLayerItemRenderer;
                okToChangeMenu = true;
                displayUserDefined = true;
                break;
            default:
                logger.warn(
                        "Unkown view requested: " + portalSession.getCurrentLayerTab()
                        + " will do nothing");
        }

        if (okToChangeMenu) {
            portalSession.setDisplayingUserDefinedMenuTree(displayUserDefined);
            portalSession.setTabForCurrentMenu(view);
            menu = portalSessionUtilities.getMenu(portalSession, view, menuId);
            showMenu(menu, renderer);
        }
    }

    /**
     * Get the combobox for MEST search - is within another
     * id-space so can't be autowired.  Probably shouldn't be
     * done in this class at all.  Waiting for search stuff
     * to be autowired before rewrite...
     * @return
     */
    private Combobox getMESTSearchTermsCombobox() {
        return (Combobox) getFellow("leftMenuSearch").getFellow("leftSearch").getFellow("txtsearch");
    }

    private Tree getMenuTree(String id) {
        return (id == null) ? null : (Tree) getFellow("tree_" + id);
    }

    /**
     * Only one may be displayed at a time, so return the
     * one that is currently active
     * @return
     */
    private Tree getMenuTree() {
        PortalSession portalSession = getPortalSession();
        Tree menu;
        if (portalSession.isDisplayingUserDefinedMenuTree()) {
            menu = getMenuTree("user");
        } else {
            menu = getMenuTree(portalSession.getSelectedMenuId());
        }
        return menu;
    }

    /**
     * executed when the opacity slider is dragged
     */
    public void onScroll$opacitySlider() {
        float opacity = ((float) opacitySlider.getCurpos()) / 100;

        // change opacity for the current selected and displayed layer
        MapLayer selectedLayer = this.getActiveLayersSelection(true);
        if (selectedLayer != null && selectedLayer.isDisplayed()) {
            logger.debug("opacity change: " + selectedLayer.getId() + " "
                    + opacity + "%");
            this.changeOpacity(selectedLayer, opacity);
        }
    }

    /*
     * image clicked for transect drawing across NCWMS layers
     */
    public void onClick$turnonTransectDrawing() {
        MapLayer selectedLayer = this.getActiveLayersSelection(true);
        openLayersJavascript.initialiseTransectDrawing(selectedLayer);

    }

    public void changeBaseLayer(String id) {
        if (safeToPerformMapAction()) {
            logger.debug("user requests change of baselayer to " + id);
            this.activateBaseLayer(id);
        }
    }

    //-- AfterCompose --//
    @Override
    public void afterCompose() {
        super.afterCompose();

        // showtime!
        load();
    }

    public void onActivateLink(ForwardEvent event) {
        Component component = event.getOrigin().getTarget();
        Link link = (Link) component.getAttribute("link");
        logger.debug("activate link: " + link.getId());
        this.activateLink(link);
    }

    public void onClick$loginButton() {
        logger.debug("activate login: ");
        Window internalContentWindow = new Window();
        internalContentWindow = (Window) Executions.createComponents("WEB-INF/zul/login.zul", null, null);
        internalContentWindow.setPosition("center");
        internalContentWindow.doHighlighted();
    }

    public void onClick$logoutButton() {
        logger.debug("logout user: ");
        // do logout
        logoutService.logout(getPortalSession());
        logoutActions();
    }

    public void addLayerFromMest(String udLabelValue, String udUriValue,
            String udLayersValue, String udImageFormatValue,
            float udOpacityValue) {

        if (safeToPerformMapAction()) {
            this.addWMSLayer(udLabelValue, udUriValue, udOpacityValue);
        }
    }

    public void addServerToMap(String serverName, String uri, String version, float opacity) {
        if (safeToPerformMapAction()) {
            this.addWMSServer(serverName, uri, version, opacity, false);
        }
    }

    /**
     * Destroy session and reload page
     */
    public void onClick$reloadPortal() {
        // grab the portaluser instance so that logged in users stay logged in...
        PortalSession portalSession = getPortalSession();
        PortalUser portalUser = portalSession.getPortalUser();

        // create a new session from the master session
        SessionInit sessionInit = new SessionInitImpl();
        try {
            sessionInit.init(Sessions.getCurrent(), null);
        } catch (Exception ex) {
            logger.error("Error creating replacement session information after user clicked reload portal", ex);
        }

        // check PortalSession is not null - if it is, something is badly wrong
        // so redirect to error page
        portalSession = getPortalSession();
        if (portalSession == null) {
            logger.error("Null portal session created after clicking reset map, sending user to error page");
            try {
                Executions.getCurrent().forward("/WEB-INF/jsp/Error.jsp");
            } catch (IOException ex) {
                logger.error("error redirecting to error page", ex);
            }
        } else {
            // all good - put the portal user back in, then force a page reload
            portalSession.setPortalUser(portalUser);
            Executions.getCurrent().sendRedirect(null);
        }
    }

    /**
     * Change the style for the map layer selected in active layers
     */
    public void onChange$styleList() {

        // layer
        MapLayer mapLayer = this.getActiveLayersSelection(true);
        if (mapLayer != null) {

            // get the index of the selected style
            int style = styleList.getSelectedIndex();

            // tell the maplayer about it
            mapLayer.setSelectedStyleIndex(style);

            // update any displayed legends
            LegendComposer lc = desktopState.getVisibleLegendByMapLayer(mapLayer);
            if (lc != null) {
                lc.update();
            }

            openLayersJavascript.reloadMapLayerNow(mapLayer);
        } else {
            logger.info(
                    "user requested change style but no maplayer is "
                    + "selected in active layers");
        }
    }

    public void onClick$layerSwitcherShow() {
        logger.debug("show layer switcher");
        toggleLayerSwitcher(true);
    }

    public void onClick$layerSwitcherHide() {
        logger.debug("hide layer switcher");
        toggleLayerSwitcher(false);

    }

    public void onClick$hideLeftMenu() {
        getPortalSession().setMaximised(true);
        maximise();
    }

    public void onClick$showLeftMenu() {
        getPortalSession().setMaximised(false);
        maximise();
    }

    public void onClick$saveMap() {
        saveLoadMapContainer.setVisible(true);
        createMapcont.setVisible(true);
        saveMap.setVisible(false);
    }

    public void onClick$mapDel() {
        Listitem selectedMap = savedMaps.getSelectedItem();

        if (selectedMap == null) {
            logger.debug("you suck arsehole!!!");
        } else {
            String mapName = StringEscapeUtils.escapeSql((String) selectedMap.getValue());
            getUserDataManager().deleteMap(mapName);
            updateUserMapList();
        }


    }

    public void onClick$loadSavedMapButton() {
        PortalSession portalSession = getPortalSession();
        PortalSession newPortalSession = getMasterPortalSession();
        List<MapLayer> activeByDefault = newPortalSession.getActiveLayers();
        Listitem selectedMap = savedMaps.getSelectedItem();

        if (selectedMap == null) {
            logger.debug("you suck!!!");
        } else {
            // deactivate any active by default layers
            while (activeByDefault.size() > 0) {
                MapLayer mapLayer = activeByDefault.get(0);
                mapLayer.setListedInActiveLayers(false);
                mapLayer.setDisplayed(false);
                activeByDefault.remove(mapLayer);
            }

            String mapName = (String) selectedMap.getValue();
            logger.debug("load saved map " + mapName);
            UserMap uMap = getUserDataManager().fetchUserMapByName(mapName);
            portalSessionUtilities.updatePortalSession(newPortalSession, uMap);


            // put user back
            newPortalSession.setPortalUser(portalSession.getPortalUser());


            // replace session
            setPortalSession(newPortalSession);

            openLayersJavascript.zoomToBoundingBox(uMap.getbBox());

            // refresh page
            Executions.sendRedirect("/");
        }
    }

    public void onClick$mapSave() {

        if (createSavedMap.getText().length() < 1) {
            createSavedMapError.setValue("Please give your map a name");
            createSavedMapError.setVisible(true);
        } else {
            String mapNameEscaped = StringEscapeUtils.escapeSql(createSavedMap.getText());

            if (getUserDataManager().checkMapName(mapNameEscaped)) {
                getUserDataManager().saveMap(mapNameEscaped);
                mapSave.setVisible(true);
                createSavedMapError.setVisible(false);
                createMapcont.setVisible(false);
                saveMap.setVisible(true);
                updateUserMapList();
            } else {
                createSavedMapError.setValue("Map name already exists");
                createSavedMapError.setVisible(true);

            }
        }
    }

    public void updateUserMapList() {

        Boolean entries = false;

        //clear the list so we don't get multiples
        savedMaps.getItems().clear();



        for (UserMap um : getUserDataManager().getUserMaps()) {
            savedMaps.appendItem(um.getMapname(), um.getMapname());
            entries = true;


        }
        // dont show the list if its empty
        if (!entries) {
            loadMapcont.setVisible(false);
            createSavedMapAdvice.setVisible(true);
        } else {
            loadMapcont.setVisible(true);
            createSavedMapAdvice.setVisible(false);
            savedMaps.setSelectedIndex(0);



        }




    }

    public void logoutActions() {
        saveLoadMapContainer.setVisible(false);
        saveMapAdvice.setVisible(true);
        saveMap.setVisible(false);
        logoutButton.setVisible(false);
        loginButton.setVisible(true);
        getLeftmenuSearchComposer().logoutActions();




    }

    public void loginActions() {
        saveLoadMapContainer.setVisible(true);
        saveMapAdvice.setVisible(false);
        saveMap.setVisible(true);
        logoutButton.setVisible(true);
        loginButton.setVisible(false);

        //get the users list of maps and searches
        getUserDataManager().fetchUser(getPortalSession().getPortalUser().getUsername());
        updateUserMapList();
        getLeftmenuSearchComposer().loginActions();



    }

    /**
     * Maximise map display area - currently just hides the left menu
     * @param maximise
     */
    private void maximise() {
        boolean maximise = getPortalSession().isMaximised();




        if (maximise) {
            //westContent.setParent(leftMenuHolder);
            menus.setWidth(settingsSupplementary.getValue(MENU_MINIMISED_WIDTH));
            menus.setBorder("none");




        } else {
            //westContent.setParent(menus);
            menus.setWidth(settingsSupplementary.getValue(MENU_DEFAULT_WIDTH));
            menus.setBorder("normal");

            // must also hide the switcher...
            toggleLayerSwitcher(
                    false);




        }
        westContent.setVisible(!maximise);
        westMinimised.setVisible(maximise);
        menus.setSplittable(!maximise);
        layerSwitcherContainer.setVisible(maximise);






    }

    public void onSearchSpecies(ForwardEvent event) {

        //get the params from the controls

        String sSearchTerm = txtSearchSpecies.getText();
        String sSearchType = null;

        Session session = (Session) Sessions.getCurrent();

        if (rdoCommonSearch.isChecked()) {
            sSearchType = "common";
        } else {
            sSearchType = "scientific";
        }

        session.setAttribute("searchTerm", sSearchTerm);
        session.setAttribute("searchType", sSearchType);

        Window win = new Window();
        win.detach();



        win = (Window) Path.getComponent("/searchResults");

        if (win == null) {
            win = (Window) Executions.createComponents(
                    "/WEB-INF/zul/SpeciesNameSearchResults.zul", null, null);
        } else {
            win.detach();
            win = (Window) Executions.createComponents(
                    "/WEB-INF/zul/SpeciesNameSearchResults.zul", null, null);
        }


        win.setTitle("Search Results for " + sSearchTerm);
        win.setMaximizable(true);
        win.setClosable(true);
        win.setSizable(true);
        win.setPosition("center");
        win.doOverlapped();
    }

    private void toggleLayerSwitcher(boolean show) {
        layerSwitcherHide.setVisible(show);
        layerSwitcherShow.setVisible(!show);




        if (show) {
            layerSwitcherContainer.setWidth(settingsSupplementary.getValue(MENU_DEFAULT_WIDTH));
            layerSwitcherContainer.setClass("layerSwitcherContainer");
            activeLayersList.setParent(layerSwitcherContainer);
            activeLayersList.setHeight("200px");

            // force the width or we get a big blue box hiding the list
            // because the sizes zk uses will be based on the previous
            // width (90% of 20px == 2px)
            logger.debug("old width:" + activeLayersList.getWidth());
            activeLayersList.setWidth(settingsSupplementary.getValue(MENU_DEFAULT_WIDTH));




        } else {
            activeLayersList.setWidth(null);
            activeLayersList.setHeight(null);
            activeLayersList.setParent(activeLayersHolder);
            layerSwitcherContainer.setWidth(null);
            layerSwitcherContainer.setClass("layerSwitcherContainer_min");




        }


    }

    /**
     * Handy getter to handle typecasting the AnimationControlsComposer
     * macro component.
     *
     * You can't just set the type of the macro component to be it's
     * real type in it's field declaration, you have to use
     * HtmlMacroComponent (or Component) or it won't be autowired.
     * @return
     */
    public AnimationControlsComposer getAnimationControlsComposer() {
        return (AnimationControlsComposer) animationControlsComposer.getFellow("animationControls");




    }

    public LeftMenuSearchComposer getLeftmenuSearchComposer() {
        return (LeftMenuSearchComposer) getFellow("leftMenuSearch").getFellow("leftSearch");




    }

    /**
     * iframe in another id-space so can't be auto wired
     * @return
     */
    private Iframe getExternalContentIframe() {
        return (Iframe) externalContentWindow.getFellow("externalContentIframe");
    }

    public DesktopState getDesktopState() {
        return desktopState;
    }

    public void setDesktopState(DesktopState desktopState) {
        this.desktopState = desktopState;
    }

    public Iframe getRawMessageIframeHack() {
        return rawMessageIframeHack;
    }

    public void setRawMessageIframeHack(Iframe rawMessageIframeHack) {
        this.rawMessageIframeHack = rawMessageIframeHack;
    }

    public Div getRawMessageHackHolder() {
        return rawMessageHackHolder;
    }

    public void setRawMessageHackHolder(Div rawMessageHackHolder) {
        this.rawMessageHackHolder = rawMessageHackHolder;
    }

    /**
     * Toggle showing the the login button and hiding the logout button
     * @param show true to show login and hide logout, false to do the opposite
     */
    public void toggleLoginButton(boolean show) {
        loginButton.setVisible(show);
        logoutButton.setVisible(!show);
    }

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    public MOTD getMotd() {
        return motd;
    }

    public void setMotd(MOTD motd) {
        this.motd = motd;
    }

    public SettingsSupplementary getSettingsSupplementary() {
        return settingsSupplementary;
    }

    public void setSettingsSupplementary(SettingsSupplementary settingsSupplementary) {
        this.settingsSupplementary = settingsSupplementary;
    }

    public OpenLayersJavascript getOpenLayersJavascript() {
        return openLayersJavascript;
    }

    public void setOpenLayersJavascript(OpenLayersJavascript openLayersJavascript) {
        this.openLayersJavascript = openLayersJavascript;
    }

    public HttpConnection getHttpConnection() {
        return httpConnection;
    }

    public void setHttpConnection(HttpConnection httpConnection) {
        this.httpConnection = httpConnection;
    }

    public RemoteMap getRemoteMap() {
        return remoteMap;
    }

    public void setRemoteMap(RemoteMap remoteMap) {
        this.remoteMap = remoteMap;

    }

    public ImageTester getImageTester() {
        return imageTester;
    }

    public void setImageTester(ImageTester imageTester) {
        this.imageTester = imageTester;
    }

    public MapLayerItemRenderer getMapLayerItemRenderer() {
        return mapLayerItemRenderer;
    }

    public void setMapLayerItemRenderer(MapLayerItemRenderer mapLayerItemRenderer) {
        this.mapLayerItemRenderer = mapLayerItemRenderer;
    }

    public ActiveLayerRenderer getActiveLayerRenderer() {
        return activeLayerRenderer;
    }

    public void setActiveLayerRenderer(ActiveLayerRenderer activeLayerRenderer) {
        this.activeLayerRenderer = activeLayerRenderer;
    }

    public PortalSessionUtilities getPortalSessionUtilities() {
        return portalSessionUtilities;
    }

    public void setPortalSessionUtilities(PortalSessionUtilities portalSessionUtilities) {
        this.portalSessionUtilities = portalSessionUtilities;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
