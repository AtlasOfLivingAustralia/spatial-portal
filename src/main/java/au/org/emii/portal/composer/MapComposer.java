package au.org.emii.portal.composer;

import au.org.emii.portal.databinding.ActiveLayerRenderer;
import au.org.emii.portal.request.DesktopState;
import au.org.emii.portal.databinding.EmptyActiveLayersRenderer;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.wms.ImageTester;
import au.org.emii.portal.menu.Link;
import au.org.emii.portal.motd.MOTD;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.databinding.MapLayerItemRenderer;
import au.org.emii.portal.menu.MenuItem;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.session.StringMedia;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.session.PortalUser;
import au.org.emii.portal.userdata.DaoRegistry;
import au.org.emii.portal.userdata.UserDataDao;
import au.org.emii.portal.userdata.UserDataDaoImpl;
import au.org.emii.portal.util.GeoJSONUtilities;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.util.PortalSessionUtilities;
import au.org.emii.portal.util.SessionPrint;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.web.SessionInitImpl;
import au.org.emii.portal.wms.WMSStyle;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONObject;
import org.ala.spatial.gazetteer.AutoComplete;
import org.ala.spatial.analysis.web.SpeciesAutoComplete;
import org.ala.spatial.analysis.web.AnalysisController;
import org.ala.spatial.analysis.web.LayersAutoComplete;
import org.ala.spatial.analysis.web.SelectionController;
import org.ala.spatial.analysis.web.SpeciesPointsProgress;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.LayersUtil;
import org.ala.spatial.util.LegendMaker;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.SessionInit;
import org.zkoss.zul.West;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Toolbarbutton;
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
    private static final String SPECIES_METADATA_URL = "species_metadata_url";
    private static final long serialVersionUID = 1L;
    private RemoteMap remoteMap = null;
    public String geoServer;

    /*
     * Autowired controls
     */
    private Window externalContentWindow;
    private Iframe rawMessageIframeHack;
    private Div rawMessageHackHolder;
    private Tabbox mainTab;    
    private Tab linkNavigationTab;
    private Tab startNavigationTab;
    private Tab mapNavigationTab;
    private Component linkNavigationTabContent;
    private Component mapNavigationTabContent;
    private Component startNavigationTabContent;
    private Slider opacitySlider;
    private Label opacityLabel;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Slider sizeSlider;
    private Checkbox chkUncertaintySize;
    private Checkbox chkPointsCluster;
    private Label redLabel;
    private Label greenLabel;
    private Label blueLabel;
    private Label sizeLabel;
    private Listbox activeLayersList;
    private Div layerControls; 
    private West menus;
    private Div westContent;
    private Div westMinimised;
    
    private LayersAutoComplete lac;
    private Textbox userparams;
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
    private ActiveLayerRenderer activeLayerRenderer = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    private Settings settings = null;
    //additional controls for the ALA Species Search stuff
    
    private AutoComplete gazetteerAuto;
    private SpeciesAutoComplete searchSpeciesAuto;
    private Div colourChooser;
    private Div sizeChooser;
    private Image legendImg;
    private Image legendImgUri;
    private Div legendHtml;
    private Label legendLabel;
    private HtmlMacroComponent leftMenuAnalysis;
    public String tbxPrintHack;
    int mapZoomLevel = 4;
    private Hashtable activeLayerMapProperties;

    /*
     * for capturing layer loaded events signaling listeners
     */
    String tbxLayerLoaded;
    HashMap<String, EventListener> layerLoadedChangeEvents = new HashMap<String, EventListener>();

    public UserDataDao getUserDataManager() {
        if (userDataManager == null) {
            userDataManager = DaoRegistry.getUserDataDao();
        }

        return userDataManager;

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
    
    public void safeToLoadMap(Event event) {
        mapLoaded("true");
    }

    public void onSelect$mainTab() {
        Session session = (Session) Sessions.getCurrent();
        if (mainTab.getSelectedTab().getId().equalsIgnoreCase("linkNavigationTab")) {
            session.setAttribute("default_menus_width", menus.getWidth());
            menus.setWidth("30%"); // 800px
        } else {
            menus.setWidth((String) session.getAttribute("default_menus_width"));
        }
    }

    public void zoomToExtent(MapLayer selectedLayer) {

        if (selectedLayer != null && selectedLayer.isDisplayed()) {
            logger.debug("zooming to extent " + selectedLayer.getId());
            if (selectedLayer.getType() == LayerUtilities.GEOJSON
                    || selectedLayer.getType() == LayerUtilities.WKT) {
                openLayersJavascript.zoomGeoJsonExtentNow(selectedLayer);
            } else {
                openLayersJavascript.zoomLayerExtent(selectedLayer);
            }
        }
    }

    public void onScroll$opacitySlider(Event e) {
        float opacity = ((float) opacitySlider.getCurpos()) / 100;
        int percentage = (int) (opacity * 100);
        opacitySlider.setCurpos(percentage);
        opacityLabel.setValue(percentage + "%");

        onClick$applyChange();
    }

    public void onClick$applyChange() {
        MapLayer selectedLayer = this.getActiveLayersSelection(true);
        if (selectedLayer != null && selectedLayer.isDisplayed()) {
            float opacity = ((float) opacitySlider.getCurpos()) / 100;
            selectedLayer.setOpacity(opacity);

            /* different path for each type layer
             * 1. symbol
             * 2. classification legend
             * 3. prediction legend
             * 4. other (wms)
             */
            if (selectedLayer.isDynamicStyle()) {
                selectedLayer.setRedVal(redSlider.getCurpos());
                selectedLayer.setGreenVal(greenSlider.getCurpos());
                selectedLayer.setBlueVal(blueSlider.getCurpos());
                selectedLayer.setSizeVal(sizeSlider.getCurpos());
                selectedLayer.setSizeUncertain(chkUncertaintySize.isChecked());

                Color c = new Color(redSlider.getCurpos(), greenSlider.getCurpos(), blueSlider.getCurpos());
                String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
                String rgbColour = "rgb(" + String.valueOf(redSlider.getCurpos()) + "," + greenSlider.getCurpos() + "," + blueSlider.getCurpos() + ")";
                selectedLayer.setEnvColour(rgbColour);

                if (selectedLayer.getType() == LayerUtilities.GEOJSON) {
                    //if this is a cluster, update geojson for new cluster radius and density
                    if (selectedLayer.getGeoJSON() != null && selectedLayer.getGeoJSON().length() > 0) {
                        try {
                            String satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
                            String lsid = selectedLayer.getMapLayerMetadata().getSpeciesLsid();
                            lsid = StringUtils.replace(lsid, ".", "__");
                            lsid = URLEncoder.encode(lsid, "UTF-8");
                            String area = getViewArea();
                            StringBuffer sbProcessUrl = new StringBuffer();
                            MapLayerMetadata md = new MapLayerMetadata();
                            md.setLayerExtent(area, 0.2);
                            sbProcessUrl.append(satServer).append("/alaspatial/");
                            sbProcessUrl.append("species");
                            sbProcessUrl.append("/cluster/").append(lsid);
                            sbProcessUrl.append("/area/").append(URLEncoder.encode(md.getLayerExtentString(), "UTF-8"));
                            sbProcessUrl.append("/id/").append(System.currentTimeMillis());
                            sbProcessUrl.append("/now");
                            sbProcessUrl.append("?z=").append(String.valueOf(mapZoomLevel));
                            sbProcessUrl.append("&m=").append(String.valueOf(selectedLayer.getSizeVal() * 2));
                            HttpClient client = new HttpClient();
                            GetMethod post = new GetMethod(sbProcessUrl.toString());
                            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
                            int result = client.executeMethod(post);
                            String slist = post.getResponseBodyAsString();
                            selectedLayer.setGeoJSON(slist);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    openLayersJavascript.redrawFeatures(selectedLayer);
                } else if (selectedLayer.getType() == LayerUtilities.WKT) {
                    openLayersJavascript.redrawWKTFeatures(selectedLayer);
                } else {
                    System.out.println("nothing:" + selectedLayer.getType());
                    //selectedLayer.setEnvParams("color:" + rgbColour + ";name:circle;size:8");
                    String envString = "color:" + hexColour + ";name:circle;size:" + sizeSlider.getCurpos();
                    envString += ";opacity:" + opacity;
                    selectedLayer.setEnvParams(envString);
                    reloadMapLayerNowAndIndexes(selectedLayer);
                }
            } else if (selectedLayer.getSelectedStyle() != null) {
                /* 1. classification legend has uri with ".zul" content
                 * 2. prediction legend works here                 *
                 */
                selectedLayer.setOpacity(opacity);
                String legendUri = selectedLayer.getSelectedStyle().getLegendUri();
                if (legendUri.indexOf(".zul") >= 0) {
                    addImageLayer(selectedLayer.getId(),
                            selectedLayer.getName(),
                            selectedLayer.getUri(),
                            opacity,
                            null);  //bbox is null, not required for redraw
                } else {
                    //redraw
                    reloadMapLayerNowAndIndexes(selectedLayer);
                }
            } else {// if (selectedLayer.getCurrentLegendUri() != null) {
                //redraw wms layer if opacity changed
                reloadMapLayerNowAndIndexes(selectedLayer);

            }
        }
    }

    void reloadMapLayerNowAndIndexes(MapLayer selectedLayer) {
        if (safeToPerformMapAction()) {
            PortalSession portalSession = (PortalSession) Executions.getCurrent().getDesktop().getSession().getAttribute("portalSession");

            openLayersJavascript.execute(
                    openLayersJavascript.iFrameReferences
                    + openLayersJavascript.reloadMapLayer(selectedLayer)
                    + openLayersJavascript.updateMapLayerIndexes(
                    portalSession.getActiveLayers()));
        }
    }

    public String getSelectionArea() {
        HtmlMacroComponent sf = (HtmlMacroComponent) ((AnalysisController) leftMenuAnalysis.getFellow("analysiswindow")).getSelectionHtmlMacroComponent();
        return ((SelectionController) sf.getFellow("selectionwindow")).getGeom();
    }

    public String getSelectionAreaPolygon() {
        String area = getSelectionArea();
        if (area.startsWith("LAYER(")) {
            String layername = area.substring(6, area.lastIndexOf(','));
            return getLayerGeoJsonAsWkt(layername);
        } else if (area.startsWith("ENVELOPE(")) {
            return getViewArea();
        }
        return area;
    }

    public void onClick$closeLayerControls() {
        layerControls.setVisible(false);
        activeLayersList.clearSelection();
    }

    /**
     * Adds the currently selected gazetteer feature to the map
     */
    //public void onClick$gazSearch() {
    public void onChange$gazetteerAuto() {

        Comboitem ci = gazetteerAuto.getSelectedItem();

        //when no item selected find an exact match from listed items
        if (ci == null) {
            String txt = gazetteerAuto.getText();
            for (Object o : gazetteerAuto.getItems()) {
                Comboitem c = (Comboitem) o;
                if (c.getLabel().equalsIgnoreCase(txt)) {
                    gazetteerAuto.setSelectedItem(c);
                    ci = c;
                    break;
                }
            }
        }

        //exit if no match found
        if (ci == null) {
            return;
        }

        String link = (String) ci.getValue();
        String label = ci.getLabel();
        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
            logger.debug(geoServer + link);
        } else {
            return;
        }

        //FIXME: this is a hack, gaz should probably return links to metadata
        String uid = "897";
        if (link.contains("aus1")) {
            uid = "22";
        }
        if (link.contains("aus2")) {
            uid = "23";
        }
        if (link.contains("imcra")) {
            uid = "21";
        }
        if (link.contains("ibra")) {
            uid = "20";
        }
        if (link.contains("all_gaz")) {
            uid = "897";
        }

        //add feature to the map as a new layer
        String metadata = settingsSupplementary.getValue(CommonData.SAT_URL) + "/alaspatial/layers/" + uid;
        MapLayer mapLayer = addGeoJSON(label, geoServer + link);
        mapLayer.setMapLayerMetadata(new MapLayerMetadata());
        mapLayer.getMapLayerMetadata().setMoreInfo(metadata + "\n" + label);

        updateUserLogMapLayer("gaz", label + "|" + geoServer + link);
    }

    public void onChange$searchSpeciesAuto() {

        // check if the species name is not valid
        // this might happen as we are automatically mapping
        // species without the user pressing a button
        if (searchSpeciesAuto.getSelectedItem() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties() == null
                || searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().size() == 0) {
            return;
        }

        //btnSearchSpecies.setVisible(true);
        String taxon = searchSpeciesAuto.getValue();
        String rank = "";

        String spVal = searchSpeciesAuto.getSelectedItem().getDescription();
        if (spVal.trim().contains(": ")) {  
            taxon = spVal.trim().substring(spVal.trim().indexOf(":") + 1, spVal.trim().indexOf("-")).trim() + " (" + taxon + ")";
            rank = spVal.trim().substring(0, spVal.trim().indexOf(":")); //"species";
            
        } else {
            rank = StringUtils.substringBefore(spVal, " ").toLowerCase();
            System.out.println("mapping rank and species: " + rank + " - " + taxon);
        }
        if (rank.equalsIgnoreCase("scientific name") || rank.equalsIgnoreCase("scientific")) {
            rank = "taxon";
        }
        mapSpeciesByLsid((String) (searchSpeciesAuto.getSelectedItem().getAnnotatedProperties().get(0)), taxon, rank);


        System.out.println(">>>>> " + taxon + ", " + rank + " <<<<<");

    }

    public void onChange$lac() {

        if (lac.getItemCount() > 0 && lac.getSelectedItem() != null) {
            JSONObject jo = (JSONObject) lac.getSelectedItem().getValue();
            String metadata = "";
            /*
            if (jo.getString("metadatapath") != null && !jo.getString("metadatapath").trim().equals("")) {
            metadata = jo.getString("metadatapath");
            } else {
            metadata += "Name: " + jo.getString("displayname") + "\n";
            metadata += "Source: " + jo.getString("source") + "\n";
            metadata += "Classification1: " + jo.getString("classification1") + "\n";
            }
             */
            metadata = settingsSupplementary.getValue(CommonData.SAT_URL) + "/alaspatial/layers/" + jo.getString("uid");
            addWMSLayer(jo.getString("displayname"), jo.getString("displaypath"), (float) 0.75, metadata);
            lac.setValue("");

            updateUserLogMapLayer("env - search - add", jo.getString("uid") + "|" + jo.getString("displayname"));
        }
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

        // hide legend controls
        hideLayerControls(null);
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

    
    public void activateLink(String uri, String label, boolean isExternal) {
        if (isExternal) {
            // change browsers current location
            Clients.evalJavaScript(
                    "window.location.href ='" + uri + "';");
        } else {

            // iframe in another id-space so can't be auto wired
            Iframe iframe = getExternalContentIframe();
            iframe.setSrc(uri);

            //for the 'reset window' button
            ((ExternalContentComposer) externalContentWindow).src = uri;

            //update linked button
            ((Toolbarbutton) externalContentWindow.getFellow("breakout")).setHref(uri);

            // use the link description as the popup caption
            ((Caption) externalContentWindow.getFellow("caption")).setLabel(
                    label);
            externalContentWindow.setPosition("center");
            externalContentWindow.doOverlapped();
        }
    }

    public void activateLink(Link link) {
        logger.debug("activate Link " + link.getId());

        activateLink(link.getUri(), link.getDescription(), link.isExternal());

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

        if (!activeLayers.contains(mapLayer) && mapLayer.isDisplayable()) {            

            /* assume we want to display on the map straight away - set checkbox
             * to true
             */
            activeLayersList.setItemRenderer(activeLayerRenderer);


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
        deactiveLayer(itemToRemove, updateMapAndLayerControls, recursive, false);
    }

    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive, boolean updateOnly) {
        if (itemToRemove != null) {
            boolean deListedInActiveLayers = false;

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
                if (!updateOnly) {
                    // tell openlayers to kill the layer
                    openLayersJavascript.removeMapLayerNow(itemToRemove);

                }
                updateLayerControls();
                removeFromSession(itemToRemove.getName());
            }

            // descend children if we were called recursively
            if (recursive && itemToRemove.hasChildren()) {
                for (MapLayer child : itemToRemove.getChildren()) {
                    deactiveLayer(child, updateMapAndLayerControls, recursive);
                }
            }
        }

        // hide layer controls
        hideLayerControls(null);
    }

    /**
     * Remove an item from the list of active layers and put it back in the
     * tree menu of available layers
     * @param itemToRemove
     */
    public void removeFromList(MapLayer itemToRemove) {
        if (itemToRemove != null) {
            boolean deListedInActiveLayers = false;

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

        }

        // hide layer controls
        hideLayerControls(null);
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
            PortalSession portalSession = getPortalSession();
            MenuItem menuItem = portalSessionUtilities.addUserDefinedMapLayer(portalSession, mapLayer);

            //if (portalSession.isDisplayingUserDefinedMenuTree()) {
            //    logger.debug("user defined menu is being displayed - updating menus");
                
            //} else if (activate) {
                // activate the layer in openlayers and display in active layers without
                // updating the tree (because its not displayed)
                activateLayer(mapLayer, true, true);

                // we must tell any future tree menus that the map layer is already
                // displayed as we didn't use changeSelection()
                mapLayer.setListedInActiveLayers(true);
                //mapLayer.setQueryable(_visible);
            //}

            logger.debug("leaving addUserDefinedLayerToMenu");
            //updateUserDefinedView();
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

        // model and renderer for active layers list
        ListModelList activeLayerModel = new ListModelList(activeLayers, true);


        // tell the list about them...
        if (activeLayers.size() == 0) {
            displayEmptyActiveLayers(activeLayersList);
        } else {
            activeLayersList.setModel(activeLayerModel);
            activeLayersList.setItemRenderer(activeLayerRenderer);
        }       

        //showCurrentMenu();

        activateNavigationTab(portalSession.getCurrentNavigationTab());
        maximise();

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
        return addWMSLayer(label, uri, opacity, "");
    }

    /**
     * Add a WMS layer identified by the given parameters to the menu system
     * and activate it
     * @param label Name of map layer
     * @param uri URI for the WMS service
     * @param layers layers to ask the WMS for
     * @param imageFormat MIME type of the image we will get back
     * @param opacity 0 for invisible, 1 for solid
     * @param metadata either a url or text 
     */
    public boolean addWMSLayer(String label, String uri, float opacity, String metadata) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                MapLayer mapLayer = remoteMap.createAndTestWMSLayer(label, uri, opacity);
                String geoserver = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
                uri = geoserver + "/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=" + mapLayer.getLayer();
                mapLayer.setDefaultStyleLegendUri(uri);
                if (mapLayer == null) {
                    // fail
                    errorMessageBrokenWMSLayer(imageTester);
                    logger.info("adding WMS layer failed ");
                } else {
                    // ok
                    if (mapLayer.getMapLayerMetadata() == null) {
                        mapLayer.setMapLayerMetadata(new MapLayerMetadata());
                    }
                    mapLayer.getMapLayerMetadata().setMoreInfo(metadata + "\n" + label);
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
     * Add a WMS layer identified by the given parameters to the menu system
     * and activate it
     * @param label Name of map layer
     * @param uri URI for the WMS service
     * @param opacity 0 for invisible, 1 for solid
     * @param filter filter
     * @param legend URI for map layer legend
     */
    public boolean addWMSLayer(String label, String uri, float opacity, String filter, String legend) {
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
                    WMSStyle style = new WMSStyle();
                    style.setName("Default");
                    style.setDescription("Default style");
                    style.setTitle("Default");
                    style.setLegendUri(legend);
                    mapLayer.addStyle(style);
                    mapLayer.setSelectedStyleIndex(1);
                    logger.info("adding WMSStyle with legendUri: " + legend);

                    mapLayer.setDefaultStyleLegendUriSet(true);

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
     * Overridden to allow for the adding servers from known Servers ie can be queried
     * Add a WMS layer identified by the given parameters to the menu system
     * and activate it
     * @param label Name of map layer
     * @param uri URI for the WMS service
     * @param layers layers to ask the WMS for
     * @param imageFormat MIME type of the image we will get back
     * @param opacity 0 for invisible, 1 for solid
     */
    public boolean addKnownWMSLayer(String label, String uri, float opacity, String filter) {
        return addKnownWMSLayer(label, uri, opacity, filter, "");
    }

    public boolean addKnownWMSLayer(String label, String uri, float opacity, String filter, String envParams) {
        boolean addedOk = false;
        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                MapLayer mapLayer = remoteMap.createAndTestWMSLayer(label, uri, opacity, true);
                mapLayer.setCql(filter);
                mapLayer.setEnvParams(envParams);
                mapLayer.setBaseLayer(false);
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

    public MapLayer getMapLayer(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        System.out.println("session active layers: " + udl.size() + " looking for: " + label);
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            System.out.println("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getName().equals(label)) {
                return ml;
            }
        }

        return null;

    }

    public void removeLayer(String label) {
        if (safeToPerformMapAction()) {
            MapLayer mapLayer = getMapLayer(label);
            if (mapLayer != null) {
                deactiveLayer(mapLayer, true, false);

            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_remove_error"));
                logger.info("unable to remove layer with label" + label);
            }

        }

    }

    public boolean addImageLayer(String id, String label, String uri, float opacity, List<Double> bbox) {
        boolean addedOk = false;

        if (safeToPerformMapAction()) {

            // check if layer already present
            MapLayer imageLayer = getMapLayer(label);

            if (imageLayer == null) {
                System.out.println("activating new layer");

                //start with a new MapLayer
                imageLayer = new MapLayer();
                //set its type
                imageLayer.setType(LayerUtilities.IMAGELAYER);
                //the name is what will appear in the active layer list
                imageLayer.setName(label);

                //where do i find the image at the moment it is only enabled for png
                imageLayer.setId(label);

                //the combination of the next two is used by openlayers to create a unique name
                //its a bit dull using the ural and layer name but its a hangover from thinking
                //everything in the world is a WMS layer
                imageLayer.setLayer("wms.png");
                imageLayer.setUri(uri);

                //set the layers opacity 0 - 1
                imageLayer.setOpacity(opacity); // (float) 0.75

                //need the bbox so the map knows where to put it
                //bbox info is stored in here
                MapLayerMetadata md = new MapLayerMetadata();
                md.setBbox(bbox);
                //remember to add the MapLayerMetadata to the MapLayer
                imageLayer.setMapLayerMetadata(md);

                //needs to be true or the map won't bother rendering it
                imageLayer.setDisplayable(true);

                //call this to add it to the map and also put it in the active layer list
                activateLayer(imageLayer, true, true);
                //addUserDefinedLayerToMenu(imageLayer, true);

                addedOk = true;


            } else {
                System.out.println("refreshing exisiting layer");
                imageLayer.setUri(uri); // + "&_lt=" + System.currentTimeMillis());
                imageLayer.setOpacity(opacity); // (float) 0.75

                // layer already exists, so lets just update that.
                //refreshActiveLayer(imageLayer);
                //Clients.evalJavaScript(
                //        "map.getLayersByName('" + label + "')[0].setUrl('" + uri + "');");
                openLayersJavascript.reloadMapLayerNow(imageLayer);

                addedOk = true;
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

    public void toggleActiveLayer(boolean bCheck) {
        Listitem selected = (Listitem) activeLayersList.getSelectedItem();

        if (selected != null) {
            for (Object cell : ((Listitem) selected).getChildren()) {
                logger.debug("cell :" + cell);
                // CHILDREN COUNT is ALWAYS 1
                if (cell instanceof Listcell) {
                    Listcell listcell = (Listcell) cell;

                    logger.debug("cell :" + listcell.getLabel());
                    for (Object innercell : listcell.getChildren()) {
                        // NEVER GET HERE
                        if (innercell instanceof Checkbox) {
                            logger.debug("InnerCell = Checkbox");
                            ((Checkbox) innercell).setChecked(bCheck);
                            Events.sendEvent(((Checkbox) innercell), new Event("onCheck", ((Checkbox) innercell)));

                        }
                    }
                }
            }
        }

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
                component = startNavigationTab;
                break;
            case PortalSession.LINK_TAB:
                component = linkNavigationTab;
                break;
            case PortalSession.MAP_TAB:
                component = mapNavigationTab;
                break;
            case PortalSession.START_TAB:
                component = startNavigationTab;
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
                component = startNavigationTabContent;
                break;
            case PortalSession.LINK_TAB:
                component = linkNavigationTabContent;
                break;
            case PortalSession.MAP_TAB:
                component = mapNavigationTabContent;
                break;
            case PortalSession.START_TAB:
                component = startNavigationTabContent;
                break;
            default:
                logger.warn("no navigation tab content found for " + tab);
        }

        return component;
    }

   
    public void onClick$mapNavigationTab() {
        activateNavigationTab(PortalSession.MAP_TAB);
    }

    public void onClick$startNavigationTab() {
        activateNavigationTab(PortalSession.START_TAB);
    }

    /*public void onClick$searchNavigationTab() {
    activateNavigationTab(PortalSession.SEARCH_TAB);
    }*/
    public void onClick$linkNavigationTab() {
        ((AnalysisController) leftMenuAnalysis.getFellow("analysiswindow")).callPullFromActiveLayers();
        activateNavigationTab(PortalSession.LINK_TAB);
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
    }

    public void onSelect$activeLayersList(ForwardEvent event) {
        // updateLayerControls();

        // hide layer controls
        hideLayerControls(null);
    }

    public void setupLayerControls(MapLayer m) {

        MapLayer currentSelection = m;

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

            if (currentSelection.isDynamicStyle()) {
                LegendMaker lm = new LegendMaker();
                int red = currentSelection.getRedVal();
                int blue = currentSelection.getBlueVal();
                int green = currentSelection.getGreenVal();
                int size = currentSelection.getSizeVal();
                boolean sizeUncertain = currentSelection.getSizeUncertain();
                System.out.println("r:" + red + " g:" + green + " b:" + blue);
                Color c = new Color(red, green, blue);

                redSlider.setCurpos(red);
                greenSlider.setCurpos(green);
                blueSlider.setCurpos(blue);
                sizeSlider.setCurpos(size); //size scale
                chkUncertaintySize.setChecked(sizeUncertain);
                //chkPointsCluster.setChecked(currentSelection.isClustered());

                blueLabel.setValue(String.valueOf(blue));
                redLabel.setValue(String.valueOf(red));
                greenLabel.setValue(String.valueOf(green));
                sizeLabel.setValue(String.valueOf(size));

                if (currentSelection.getGeometryType() != GeoJSONUtilities.POINT) {
                    legendImg.setContent(lm.singleRectImage(c, 50, 50, 45, 45));
                    sizeChooser.setVisible(false);
                    //uncertainty.setVisible(false);
                } else {
                    legendImg.setContent(lm.singleCircleImage(c, 50, 50, 20.0));
                    sizeChooser.setVisible(true);
                    if (m.getGeoJSON() != null && m.getGeoJSON().length() > 0) {
                        //uncertainty.setVisible(false);
                    } else {
                        //uncertainty.setVisible(true);
                    }
                }
                legendImg.setVisible(true);
                legendLabel.setVisible(true);
                legendImgUri.setVisible(false);
                legendHtml.setVisible(false);
                colourChooser.setVisible(true);
            } else if (currentSelection.getSelectedStyle() != null) {
                /* 1. classification legend has uri with ".zul" content
                 * 2. prediction legend works here
                 * TODO: do this nicely when implementing editable prediction layers
                 */
                String legendUri = currentSelection.getSelectedStyle().getLegendUri();
                if (legendUri != null && legendUri.indexOf(".zul") >= 0) {
                    //remove all
                    while (legendHtml.getChildren().size() > 0) {
                        legendHtml.removeChild(legendHtml.getFirstChild());
                    }

                    //put any parameters into map
                    Map map = null;
                    if (legendUri.indexOf("?") > 0) {
                        String[] parameters = legendUri.substring(legendUri.indexOf("?") + 1,
                                legendUri.length()).split("&");
                        if (parameters.length > 0) {
                            map = new HashMap();
                        }
                        for (String p : parameters) {
                            String[] parameter = p.split("=");
                            if (parameter.length == 2) {
                                map.put(parameter[0], parameter[1]);
                            }
                        }
                        legendUri = legendUri.substring(0, legendUri.indexOf("?"));
                    }

                    //open .zul with parameters
                    Executions.createComponents(
                            legendUri, legendHtml, map);

                    legendHtml.setVisible(true);
                    legendImgUri.setVisible(false);
                    legendLabel.setVisible(true);
                } else {
                    legendImgUri.setSrc(legendUri);
                    legendImgUri.setVisible(true);
                    legendHtml.setVisible(false);
                    legendLabel.setVisible(false);
                }
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
            } else if (currentSelection.getCurrentLegendUri() != null) {
                // works for normal wms layers
                legendImgUri.setSrc(currentSelection.getCurrentLegendUri());
                legendImgUri.setVisible(true);
                legendHtml.setVisible(false);
                legendLabel.setVisible(false);
                legendImg.setVisible(false);
                colourChooser.setVisible(false);
                sizeChooser.setVisible(false);
            } else {
                hideLayerControls(null);
            }
            layerControls.setVisible(true);
            layerControls.setAttribute("activeLayerName", currentSelection.getName());
        } else {
            hideLayerControls(null);
        }

        // TODO: fix the points/cluster toggle for active area
        if (m != null) {
            if (m.getName().equalsIgnoreCase("Species in Active area")) {
                chkPointsCluster.setVisible(false);
            } else {
                chkPointsCluster.setVisible(true);
            }
        }
    }

    public void toggleLayerControls() {
        MapLayer activeLayer = getActiveLayersSelection(false);
        String attrLayerName = (String) layerControls.getAttribute("activeLayerName");
        if (isLayerControlVisible()) {
            if (activeLayer.getName().equals(attrLayerName)) {
                hideLayerControls(activeLayer);
            } else {
                MapLayer previousLayer = getMapLayer(attrLayerName);
                hideLayerControls(previousLayer);
                setupLayerControls(activeLayer);
            }
        } else {
            setupLayerControls(activeLayer);
        }
    }

    /**
     * hides layer controls.
     *
     * @param layer layer as MapLayer whose controls need to be hidden
     * if visible. null to hide without testing against active layer.
     */
    public void hideLayerControls(MapLayer layer) {
        if (layer == null
                || layer == getActiveLayersSelection(false)) {
            layerControls.setVisible(false);
            legendImg.setVisible(false);
            legendImgUri.setVisible(false);
            legendLabel.setVisible(false);
            sizeChooser.setVisible(false);
            colourChooser.setVisible(false);
            legendHtml.setVisible(false);
        }
    }

    /**
     * Enable or disable layer controls depending on the current selection.
     *
     * At the moment this shows/hides the opacity controls
     */
    public void updateLayerControls() {

        MapLayer currentSelection = getActiveLayersSelection(false);
        setupLayerControls(currentSelection);
    }

    public void mapLoaded(String text) {
        boolean loaded = Boolean.parseBoolean(text);
        getPortalSession().setMapLoaded(loaded);

        if (loaded) {
            //openLayersJavascript.execute("window.mapFrame.loadBaseMap();");
            openLayersJavascript.setAdditionalScript("window.mapFrame.loadBaseMap();");
            System.out.println("---------------------------------------------");
            System.out.println("---------------------------------------------");
            System.out.println("map is now loaded. let's try mapping.");
            MapLayer ml = loadUrlParameters();
            System.out.println("---------------------------------------------");
            System.out.println("---------------------------------------------");
//            if (ml != null) {
//                openLayersJavascript.setAdditionalScript(openLayersJavascript.zoomGeoJsonExtent(ml));
//            } else {
//                openLayersJavascript.setAdditionalScript("");
//            }
            openLayersJavascript.setAdditionalScript("");

        }
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

   public void updateLegendImage() {
        LegendMaker lm = new LegendMaker();
        int red = redSlider.getCurpos();
        int blue = blueSlider.getCurpos();
        int green = greenSlider.getCurpos();
        Color c = new Color(red, green, blue);

        MapLayer selectedLayer = this.getActiveLayersSelection(true);
        if (selectedLayer.getGeometryType() != GeoJSONUtilities.POINT) {
            legendImg.setContent(lm.singleRectImage(c, 50, 50, 45, 45));
            sizeChooser.setVisible(false);
            //uncertainty.setVisible(false);
        } else {
            legendImg.setContent(lm.singleCircleImage(c, 50, 50, 20.0));
            sizeChooser.setVisible(true);
            if (selectedLayer.getGeoJSON() != null && selectedLayer.getGeoJSON().length() > 0) {
                //uncertainty.setVisible(false); //hide uncertianty for clusters
            } else {
                //uncertainty.setVisible(true);
            }

        }
    }

    public void onScroll$sizeSlider() {
        int size = sizeSlider.getCurpos();
        sizeLabel.setValue(String.valueOf(size));
        updateLegendImage();
        onClick$applyChange();
    }

    public void onCheck$chkUncertaintySize() {
        updateLegendImage();
        onClick$applyChange();
    }

    public void onCheck$chkPointsCluster() {
        MapLayer selectedLayer = this.getActiveLayersSelection(true);
        MapLayerMetadata md = selectedLayer.getMapLayerMetadata();
        if (md != null) {
            String species = md.getSpeciesDisplayName();
            String rank = md.getSpeciesRank();
            String lsid = md.getSpeciesLsid();

            int red = selectedLayer.getRedVal();
            int green = selectedLayer.getGreenVal();
            int blue = selectedLayer.getBlueVal();
            int size = selectedLayer.getSizeVal();
            float opacity = selectedLayer.getOpacity();
            String envParams = selectedLayer.getEnvParams();
            String envName = selectedLayer.getEnvName();
            String envColour = selectedLayer.getEnvColour();
            String envSize = selectedLayer.getEnvSize();

            if (activeLayerMapProperties == null) {
                activeLayerMapProperties = new Hashtable();
            }
            if (envColour == null) {
                envColour = "rgb(" + String.valueOf(red) + "," + String.valueOf(green) + "," + String.valueOf(blue) + ")";
            }
            if (envParams == null) {
                Color c = new Color(red, green, blue);
                String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
                envParams = "color:" + hexColour + ";name:circle;size:" + size + ";opacity:" + opacity + "";
            }
            activeLayerMapProperties.put("red", red);
            activeLayerMapProperties.put("blue", blue);
            activeLayerMapProperties.put("green", green);
            activeLayerMapProperties.put("size", size);
            activeLayerMapProperties.put("opacity", opacity);
            activeLayerMapProperties.put("envColour", envColour);
            activeLayerMapProperties.put("envParams", envParams);

            //removeLayer(species);
            openLayersJavascript.setAdditionalScript(openLayersJavascript.iFrameReferences
                    + openLayersJavascript.removeMapLayer(selectedLayer));

            MapLayer convLayer = null;
            if (selectedLayer.isClustered()) {
                System.out.println("calling lsidfilter with:");
                System.out.println("lsid: " + lsid);
                System.out.println("species: " + species);
                System.out.println("rank: " + rank);
                convLayer = mapSpeciesByLsidFilter(lsid, species, rank);
            } else {
                System.out.println("calling lsidcluster with:");
                System.out.println("lsid: " + lsid);
                System.out.println("species: " + species);
                convLayer = mapSpeciesByLsidCluster(lsid, species, rank);
            }

            deactiveLayer(selectedLayer, true, false, true);

            // reopen the layer controls
            refreshActiveLayer(convLayer);
            setupLayerControls(convLayer);

            // now remove the colour settings
            activeLayerMapProperties = null;

        }
    }

    public void onScroll$blueSlider() {
        int blue = blueSlider.getCurpos();
        blueLabel.setValue(String.valueOf(blue));
        updateLegendImage();
        onClick$applyChange();
    }

    public void onScroll$redSlider() {
        int red = redSlider.getCurpos();
        redLabel.setValue(String.valueOf(red));
        updateLegendImage();
        onClick$applyChange();

    }

    public void onScroll$greenSlider() {
        int green = greenSlider.getCurpos();
        greenLabel.setValue(String.valueOf(green));
        updateLegendImage();
        onClick$applyChange();
    }  

    //-- AfterCompose --//
    @Override
    public void afterCompose() {
        super.afterCompose();

        //window settings, for printing
        applyWindowParams();

        // showtime!
        load();

        //loadLayerTree();
    }

    /**
     * apply window parameters
     *
     * p = width in pixels,height in pixels,longitude1,latitude1,longitude2,latitude2
     */
    void applyWindowParams() {
        String s = (String) Executions.getCurrent().getParameter("p");

        //TODO: validate params
        if (s != null) {
            String[] pa = s.split(",");
            setWidth(pa[0] + "px");

            if (pa.length > 1) {
                setHeight(pa[1] + "px");
            }
        }
    }

    private Map getUserParameters(String userParams) {
        if (StringUtils.isBlank(userParams) || userParams.equals("{}")) {
            return null;
        }

        Map<String, String> uparams = new HashMap();

        // check if the string is a proper params
        if (userParams.startsWith("{") && userParams.endsWith("}")) {
            userParams = userParams.substring(1, userParams.lastIndexOf("}"));
        }

        String[] auparams = userParams.split(",");
        System.out.println("Got " + auparams.length + " parameters");
        for (int ip = 0; ip < auparams.length; ip++) {
            String[] uparam = auparams[ip].split("=");
            if (uparam.length > 1) {
                uparams.put(uparam[0].trim(), uparam[1].trim());
            }
        }

        return uparams;
    }

    public void echoMapSpeciesByLSID(Event event) {
        String lsid = (String) event.getData();
        try {
            mapSpeciesByLsid(lsid, lsid);
        } catch (Exception e) {
            //try again
            Events.echoEvent("echoMapSpeciesByLSID", this, lsid);
        }
    }

    private MapLayer loadUrlParameters() {
        MapLayer ml = null;
        try {

            boolean showLayerTab = false;

            System.out.println("User params: " + userparams.getValue());

            Map<String, String> userParams = getUserParameters(userparams.getValue());
            if (userParams != null) {

                if (userParams.containsKey("species_lsid")) {
                    //TODO: get species name as layer name
                    Events.echoEvent("echoMapSpeciesByLSID", this, userParams.get("species_lsid"));
                    //ml = mapSpeciesByLsid(userParams.get("species_lsid"), userParams.get("species_lsid"));
                    showLayerTab = true;
                } else if (userParams.containsKey("layer")) {
                    // TODO: eventually add env/ctx layer loading code here
                } else {
                    Iterator<String> itParams = userParams.keySet().iterator();
                    String label = "";
                    String filter = "";
                    while (itParams.hasNext()) {
                        String key = itParams.next();
                        label += key;
                        filter += key += " eq '" + userParams.get(key) + "'";
                        if (itParams.hasNext()) {
                            label += " - ";
                            filter += " AND ";
                        }
                    }
                    System.out.println("filter: " + filter);
                    try {
                        ml = mapSpeciesByFilter(filter, filter);
                    } catch (Exception e) {
                    }
                    showLayerTab = true;
                }
            }

            if (showLayerTab) {
                activateNavigationTab(PortalSession.MAP_TAB);
            }

        } catch (Exception e) {
            System.out.println("Opps error loading url parameters");
            e.printStackTrace(System.out);
        }

        return ml;
    }

    public void onActivateLink(ForwardEvent event) {
        Component component = event.getOrigin().getTarget();
        Object oLink = component.getAttribute("link");
        if (oLink.getClass().getName().equals("java.lang.String")) {
            String uri = (String) oLink;
            String label = (String) component.getAttribute("label");
            this.activateLink(uri, label, false);
        } else {
            Link link = (Link) oLink;
            logger.debug("activate link: " + link.getId());
            this.activateLink(link);
        }
    }

    public MapLayer addGeoJSON(String labelValue, String uriValue) {
        if (safeToPerformMapAction()) {

            return this.addGeoJSONLayer(labelValue, uriValue, false);

        } else {
            return null;
        }

    }

    public MapLayer addWKTLayer(String wkt, String label) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), label) == null) {
                mapLayer = remoteMap.createWKTLayer(wkt, label);
                if (mapLayer == null) {
                    // fail
                    showMessage("No mappable features available");
                    logger.info("adding WKT layer failed ");
                } else {
                    mapLayer.setDisplayable(true);
                    mapLayer.setOpacity((float) 0.4);
                    mapLayer.setQueryable(true);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);

                    // we must tell any future tree menus that the map layer is already
                    // displayed as we didn't use changeSelection()
                    mapLayer.setListedInActiveLayers(true);
                }
            } else {
                // fail
                showMessage("WKT layer already exists");
                logger.info(
                        "refusing to add a new layer with name " + label
                        + " because it already exists in the menu");
            }
        }

        return mapLayer;
    }

    public MapLayer addGeoJSONLayer(String label, String uri, boolean points_type) {
        return addGeoJSONLayer(label, uri, "", false, points_type);
    }

    public MapLayer addGeoJSONLayer(String label, String uri, String params, boolean forceReload, boolean points_type) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            MapLayer gjLayer = getMapLayer(label);
            if (label.equalsIgnoreCase("Species in Active area") || forceReload) {
                if (gjLayer != null) {
                    System.out.println("removing existing layer: " + gjLayer.getName());
                    openLayersJavascript.setAdditionalScript(
                            openLayersJavascript.removeMapLayer(gjLayer));
                } //else {
                mapLayer = remoteMap.createGeoJSONLayer(label, uri, points_type);
                if (mapLayer == null) {
                    // fail
                    //hide error, might be clustering zoom in; showMessage("No mappable features available");
                    logger.info("adding GEOJSON layer failed ");
                } else {
                    mapLayer.setDisplayable(true);
                    mapLayer.setOpacity((float) 0.6);
                    mapLayer.setQueryable(true);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);

                    // we must tell any future tree menus that the map layer is already
                    // displayed as we didn't use changeSelection()
                    mapLayer.setListedInActiveLayers(true);
                }
                //}
            } else {
                //if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                boolean okToAdd = false;
                MapLayer mlExisting = getMapLayer(label);
                if (mlExisting == null) {
                    okToAdd = true;
                } else if (!mlExisting.getUri().equals(uri)) {
                    // check if it's a different url
                    // if it is, then it is ok to add
                    // and assume the previous is removed.
                    //System.out.println("mlExisting.getUri(): " + mlExisting.getUri());
                    //System.out.println("mlExisting.newUri:   " + uri);
                    okToAdd = true;
                } else {
                }

                if (okToAdd) {
                    mapLayer = remoteMap.createGeoJSONLayer(label, uri, points_type, activeLayerMapProperties);
                    if (mapLayer == null) {
                        // fail
                        //hide error, might be clustering zoom in; showMessage("No mappable features available");
                        logger.info("adding GEOJSON layer failed ");
                    } else {
                        mapLayer.setDisplayable(true);
                        mapLayer.setOpacity((float) 0.6);
                        mapLayer.setQueryable(true);
                        mapLayer.setDynamicStyle(true);

                        activateLayer(mapLayer, true, true);

                        // we must tell any future tree menus that the map layer is already
                        // displayed as we didn't use changeSelection()
                        mapLayer.setListedInActiveLayers(true);
                    }
                } else {
                    //need to cleanup any additional scripts outstanding
                    openLayersJavascript.useAdditionalScript();

                    // fail
                    //showMessage("GeoJSON layer already exists");
                    logger.info(
                            "refusing to add a new layer with URI " + uri
                            + " because it already exists in the menu");
                }
            }
        }

        return mapLayer;
    }

    public void addGeoJSONLayerProgressBar(String label, String uri, String params, boolean forceReload, int partsCount, String lsid) {
        //start the progress bar...
        SpeciesPointsProgress window = (SpeciesPointsProgress) Executions.createComponents("WEB-INF/zul/SpeciesPointsProgress.zul", this, null);
        window.start(label, uri, params, forceReload, partsCount, lsid);
        try {
            window.doModal();
        } catch (Exception e) {
        }
    }

    public String getJson(String uri) {
        return remoteMap.getJson(uri);
    }

    public MapLayer addGeoJSONLayerProgressBarReady(String label, String uri, String params, boolean forceReload, String json, String lsid, int partsCount) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            MapLayer gjLayer = getMapLayer(label);
            if (label.equalsIgnoreCase("Species in Active area") || forceReload) {
                if (gjLayer != null) {
                    System.out.println("removing existing layer: " + gjLayer.getName());
                    openLayersJavascript.setAdditionalScript(
                            openLayersJavascript.removeMapLayer(gjLayer));
                } //else {
                mapLayer = remoteMap.createGeoJSONLayerWithGeoJSON(label, uri, json);
                if (mapLayer == null) {
                    // fail
                    //hide error, might be clustering zoom in;  showMessage("No mappable features available");
                    logger.info("adding GEOJSON layer failed ");
                } else {
                    mapLayer.setDisplayable(true);
                    mapLayer.setOpacity((float) 0.4);
                    mapLayer.setQueryable(true);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);

                    // we must tell any future tree menus that the map layer is already
                    // displayed as we didn't use changeSelection()
                    mapLayer.setListedInActiveLayers(true);
                }
                //}
            } else {
                //if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                if (getMapLayer(label) == null) {
                    mapLayer = remoteMap.createGeoJSONLayerWithGeoJSON(label, uri, json);
                    if (mapLayer == null) {
                        // fail
                        //hide error, might be clustering zoom in;  showMessage("No mappable features available");
                        logger.info("adding GEOJSON layer failed ");
                    } else {
                        mapLayer.setDisplayable(true);
                        mapLayer.setOpacity((float) 0.4);
                        mapLayer.setQueryable(true);
                        mapLayer.setDynamicStyle(true);

                        activateLayer(mapLayer, true, true);

                        // we must tell any future tree menus that the map layer is already
                        // displayed as we didn't use changeSelection()
                        mapLayer.setListedInActiveLayers(true);
                    }
                } else {
                    //need to cleanup any additional scripts outstanding
                    openLayersJavascript.useAdditionalScript();

                    // fail
                    //showMessage("GeoJSON layer already exists");
                    logger.info(
                            "refusing to add a new layer with URI " + uri
                            + " because it already exists in the menu");
                }
            }
        }

        if (lsid != null) {
            MapLayer ml = getMapLayer(label);
            String infoUrl = getSettingsSupplementary().getValue(SPECIES_METADATA_URL).replace("_lsid_", lsid);
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setMoreInfo(infoUrl + "\n" + label);
            md.setSpeciesLsid(lsid);
        }
        if (partsCount > 0) {
            MapLayer ml = getMapLayer(label);
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setPartsCount(partsCount);
        }

        return mapLayer;
    }

    public MapLayer appendGeoJSONLayerProgressBarReady(String label, String uri, int part, String params, boolean forceReload, String json, String lsid) {
        MapLayer mapLayer = null;

        Clients.evalJavaScript(
                "window.mapFrame.appendJsonUrlToMap('" + uri + "_" + part + "','" + uri + "_0','" + label + "');");

        return mapLayer;
    }

    /**
     * Destroy session and reload page
     *
     * - added confirmation message, needs to be event driven since
     * message box always returning '1' with current zk settings.
     */
    public void onClick$reloadPortal() {
        // user confirmation for whole map reset
        try {
            Messagebox.show("Reset map to initial empty state, with no layers and default settings?", "Reset Map",
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    if (((Integer) event.getData()).intValue() != Messagebox.YES) {
                        //no selected
                        return;
                    } else {
                        //reset map
                        reloadPortal();
                        return;
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * split from button for yes/no event processing
     *
     * see onClick$reloadPortal()
     */
    void reloadPortal() {
        // grab the portaluser instance so that logged in users stay logged in...
        PortalSession portalSession = getPortalSession();
        PortalUser portalUser = portalSession.getPortalUser();

        // create a new session from the master session
        SessionInit sessionInit = new SessionInitImpl();
        try {
            sessionInit.init(Sessions.getCurrent(), null);
        } catch (Exception ex) {
            logger.error("Error creating replacement session information after user clicked reload portal", ex);

        } // check PortalSession is not null - if it is, something is badly wrong
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

    public void onClick$hideLeftMenu() {
        getPortalSession().setMaximised(true);
        maximise();
    }

    public void onClick$showLeftMenu() {
        getPortalSession().setMaximised(false);
        maximise();
    }

    /**
     * Reload all species layers
     */
    public void onReloadLayers(Event event) {
        String tbxReloadLayers = (String) event.getData();
        
        //update this.mapZoomLevel
        boolean mapZoomChanged = true;
        try {
            int mapZoomLevelOld = mapZoomLevel;
            String s = tbxReloadLayers;
            int s1 = s.indexOf("z=");
            int s2 = s.indexOf("&");
            if (s1 >= 0) {
                if (s2 >= 0) {
                    mapZoomLevel = Integer.parseInt(s.substring(s1 + 2, s2));
                } else {
                    mapZoomLevel = Integer.parseInt(s.substring(s1 + 2));
                }
            }
            mapZoomChanged = (mapZoomLevelOld != mapZoomLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String satServer = settingsSupplementary.getValue(CommonData.SAT_URL);
        System.out.println("tbxReloadLayers.getValue(): " + tbxReloadLayers);
        // iterate thru' active map layers
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        Vector<String> processedLayers = new Vector();
        String reloadScript = "";
        while (iudl.hasNext()) {
            //try {
            MapLayer ml = (MapLayer) iudl.next();
            if (processedLayers.contains(ml.getName())) {
                System.out.println(ml.getName() + " already processed.");
                continue;
            }
            System.out.println("checking reload layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS() + " -> type: " + ml.getType() + "," + ml.getGeometryType());
            if (ml.getType() == LayerUtilitiesImpl.GEOJSON
                    && ml.getGeometryType() == GeoJSONUtilities.POINT
                    && ml.getMapLayerMetadata() != null) {
                if (ml.getGeoJSON() != null && ml.getGeoJSON().length() > 0) {
                    //addGeoJSONLayer(ml.getName(), ml.getUri() + "?" + tbxReloadLayers.getValue(), "", true);
                    String baseuri = ml.getUri();
                    if (baseuri.indexOf("?z=") > -1) {
                        baseuri = baseuri.substring(0, baseuri.indexOf("?z="));
                    }
                    //ml.setUri(ml.getUri() + "?" + tbxReloadLayers.getValue());

                    String reqUri = "";

                    StringBuffer sbProcessUrl = new StringBuffer();
                    try {
                        if (ml.getMapLayerMetadata() != null && ml.getMapLayerMetadata().getSpeciesLsid() != null) {
                            //cluster
                            if (mapZoomChanged || ml.getMapLayerMetadata().isOutside(getViewArea())) {
                                ml.getMapLayerMetadata().setLayerExtent(getViewArea(), 0.2);

                                String lsid = ml.getMapLayerMetadata().getSpeciesLsid();
                                lsid = StringUtils.replace(lsid, ".", "__");
                                lsid = URLEncoder.encode(lsid, "UTF-8");
                                sbProcessUrl.append("/species");
                                sbProcessUrl.append("/cluster/").append(lsid);
                                sbProcessUrl.append("/area/").append(URLEncoder.encode(ml.getMapLayerMetadata().getLayerExtentString(), "UTF-8"));
                                sbProcessUrl.append("/id/").append(System.currentTimeMillis());
                                sbProcessUrl.append("/now");
                                reqUri = satServer + "/alaspatial/"
                                        + sbProcessUrl.toString()
                                        + "?" + tbxReloadLayers + "&m="
                                        + (ml.getSizeVal() * 2);
                            }
                        } else {
                            //points
                            if (mapZoomChanged || ml.getMapLayerMetadata().isOutside(getViewArea())) {
                                ml.getMapLayerMetadata().setLayerExtent(getViewArea(), 0.2);

                                reqUri = ml.getUri() + "?" + tbxReloadLayers
                                        + "&a=" + URLEncoder.encode(ml.getMapLayerMetadata().getLayerExtentString(), "UTF-8")
                                        + "&m=" + (ml.getSizeVal() * 2);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Unabled to set sp.cluster url");
                        e.printStackTrace(System.out);
                    }

                    if (reqUri.length() > 0) {
                        try {
                            HttpClient client = new HttpClient();
                            GetMethod post = new GetMethod(reqUri);
                            post.addRequestHeader("Accept", "application/json, text/javascript, */*");

                            int result = client.executeMethod(post);
                            String slist = post.getResponseBodyAsString();
                            ml.setGeoJSON(slist);
                        } catch (Exception e) {
                            System.out.println("error loading new geojson:");
                            e.printStackTrace(System.out);
                        }
                        if (ml.isDisplayed()) {
                            reloadScript += openLayersJavascript.reloadMapLayer(ml);
                        }
                    }
                }
            }
            processedLayers.add(ml.getName());
        }

        if (reloadScript.length() > 0) {
            openLayersJavascript.execute(
                    openLayersJavascript.iFrameReferences
                    + reloadScript);
        }
    }

    
    /**
     * generate pdf for from innerHTML of printHack txtbox
     *
     * dependant on whatever the ends up being on the map tab
     *
     * uses wkhtmltoimage (wkhtmltopdf not working?)
     *
     *
     */
    public void onPrint(Event event) {
        tbxPrintHack = (String) event.getData();
        PrintingComposer composer = (PrintingComposer) Executions.createComponents("/WEB-INF/zul/Printing.zul", this, null);
        try {
            composer.doModal();
        } catch (InterruptedException ex) {
            Logger.getLogger(MapComposer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SuspendNotAllowedException ex) {
            Logger.getLogger(MapComposer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * watch for layer loaded signals
     */
    public void onLayerLoaded(Event event) {
        tbxLayerLoaded = (String) event.getData();
        for (EventListener el : layerLoadedChangeEvents.values()) {
            try {
                el.onEvent(null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getLayerLoaded() {
        return tbxLayerLoaded;
    }

    public void addLayerLoadedEventListener(String eventName, EventListener eventListener) {
        layerLoadedChangeEvents.put(eventName, eventListener);
    }

    public void removeLayerLoadedEventListener(String eventName) {
        layerLoadedChangeEvents.remove(eventName);
    }    

    public void setWestWidth(String width) {
        menus.setWidth(width);
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
            //toggleLayerSwitcher(false);
        }
        westContent.setVisible(!maximise);
        westMinimised.setVisible(maximise);
        menus.setSplittable(!maximise);        
    }

    public MapLayer mapSpeciesByLsid(String lsid, String species) {
        return mapSpeciesByLsid(lsid, species, "species");
    }

    public MapLayer mapSpeciesByLsid(String lsid, String species, String rank) {
        if (species == null || (lsid != null && species.equalsIgnoreCase(lsid))) {
            //species = LayersUtil.getScientificNameRank(lsid);
            String speciesrank = LayersUtil.getScientificNameRank(lsid);
            species = speciesrank.split(",")[0];
            rank = speciesrank.split(",")[1];
        }

        //use # of points cutoff; //        if(chkPointsCluster.isChecked()){
        MapLayer ml = null;
        if (countOfLsid(lsid) > 20000 || (Executions.getCurrent().isExplorer() && countOfLsid(lsid) > 200)) {
            ml = mapSpeciesByLsidCluster(lsid, species, rank);
        } else {
            //return mapSpeciesByLsidPoints(lsid,species);
            ml = mapSpeciesByLsidFilter(lsid, species, rank);
        }

        MapLayerMetadata md = ml.getMapLayerMetadata();
        if (md == null) {
            md = new MapLayerMetadata();
            ml.setMapLayerMetadata(md);
        }
        md.setSpeciesLsid(lsid);
        md.setSpeciesDisplayName(species);
        md.setSpeciesRank(rank);

        updateUserLogMapSpecies(lsid);

        return ml;
    }

    int countOfLsid(String lsid) {
        int count = 0;
        //get bounding box for lsid
        try {
            //cluster, must have an lsid
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(settingsSupplementary.getValue(CommonData.SAT_URL));
            sbProcessUrl.append("/alaspatial/species/lsid/").append(lsid);
            sbProcessUrl.append("/count");
            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(sbProcessUrl.toString());
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            count = Integer.parseInt(slist);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    MapLayer mapSpeciesByLsidCluster(String lsid, String species) {
        return mapSpeciesByLsidCluster(lsid, species, "species");
    }

    MapLayer mapSpeciesByLsidCluster(String lsid, String species, String rank) {
        try {
            String satServer = settingsSupplementary.getValue(CommonData.SAT_URL);

            String speciesLsid = lsid;
            lsid = StringUtils.replace(lsid, ".", "__");
            lsid = URLEncoder.encode(lsid, "UTF-8");

            String area = getViewArea();

            StringBuffer sbProcessUrl = new StringBuffer();

            MapLayerMetadata md = new MapLayerMetadata();
            md.setLayerExtent(area, 0.2);

            //for plotting species by clustering
            sbProcessUrl.append("species");
            sbProcessUrl.append("/cluster/").append(lsid);
            sbProcessUrl.append("/area/").append(URLEncoder.encode(md.getLayerExtentString(), "UTF-8"));
            sbProcessUrl.append("/id/").append(System.currentTimeMillis());
            sbProcessUrl.append("/now");
            sbProcessUrl.append("?z=").append(String.valueOf(mapZoomLevel));
            sbProcessUrl.append("&m=").append(String.valueOf(8));
            MapLayer ml = addGeoJSONLayer(species, satServer + "/alaspatial/" + sbProcessUrl.toString(), true);

            if (ml != null) {
                String infoUrl = getSettingsSupplementary().getValue(SPECIES_METADATA_URL).replace("_lsid_", lsid);
                md = ml.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    ml.setMapLayerMetadata(md);
                }
                md.setMoreInfo(infoUrl + "\n" + species);
                md.setSpeciesLsid(speciesLsid);
                md.setSpeciesDisplayName(species);
                md.setSpeciesRank(rank);
                md.setLayerExtent(area, 0.2);

                addLsidBoundingBoxToMetadata(md, lsid);

                ml.setClustered(true);

                chkPointsCluster.setChecked(false);
                chkPointsCluster.setLabel(" Display species as points");
            }

            return ml;

        } catch (Exception ex) {
            //logger.debug(ex.getMessage());
            System.out.println("Opps error in mapsSpeciesByLsid");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    void addLsidBoundingBoxToMetadata(MapLayerMetadata md, String lsid) {
        //get bounding box for lsid
        try {
            //cluster, must have an lsid
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(settingsSupplementary.getValue(CommonData.SAT_URL));
            sbProcessUrl.append("/alaspatial/species/cluster/lsid/").append(lsid);
            sbProcessUrl.append("/bb");
            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(sbProcessUrl.toString());
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            List<Double> bb = new ArrayList<Double>();
            String[] sa = slist.split(",");
            for (int i = 0; i < sa.length; i++) {
                bb.add(Double.parseDouble(sa[i]));
            }
            md.setBbox(bb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MapLayer mapSpeciesByLsidPoints(String lsid, String species) {
        try {
            String satServer = settingsSupplementary.getValue(CommonData.SAT_URL);

            lsid = StringUtils.replace(lsid, ".", "__");
            lsid = URLEncoder.encode(lsid, "UTF-8");

            String area = getViewArea();

            StringBuffer sbProcessUrl = new StringBuffer();

            //for plotting species by points
            sbProcessUrl.append("species");
            sbProcessUrl.append("/lsid/").append(lsid);
            sbProcessUrl.append("/geojson");
            HttpClient client = new HttpClient();
            GetMethod post = new GetMethod(satServer + "/alaspatial/" + sbProcessUrl.toString());
            post.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(post);
            String slist = post.getResponseBodyAsString();
            String[] results = slist.split("\n");

            addGeoJSONLayerProgressBar(species, satServer + "/alaspatial/" + results[0], "", false, Integer.parseInt(results[1]), lsid);//set progress bar with maximum
        } catch (Exception ex) {
            //logger.debug(ex.getMessage());
            System.out.println("Opps error in mapsSpeciesByLsid");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public MapLayer mapSpeciesByLsidFilter(String lsid, String species, String rank) {
        String filter = rank + "conceptid='" + lsid + "'";
        //filter = rank+"='"+species+"'";
        MapLayer ml = mapSpeciesWMSByFilter(species, filter);

        if (ml != null) {
            addToSession(species, filter);

            String infoUrl = getSettingsSupplementary().getValue(SPECIES_METADATA_URL).replace("_lsid_", lsid);
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (md == null) {
                md = new MapLayerMetadata();
                ml.setMapLayerMetadata(md);
            }
            md.setMoreInfo(infoUrl + "\n" + species);
            md.setSpeciesLsid(lsid);
            md.setSpeciesDisplayName(species);
            md.setSpeciesRank(rank);

            ml.setClustered(false);
            chkPointsCluster.setLabel(" Display species as clusters");
            chkPointsCluster.setChecked(false);

            lsid = StringUtils.replace(lsid, ".", "__");
            try {
                lsid = URLEncoder.encode(lsid, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            addLsidBoundingBoxToMetadata(md, lsid);
        }

        return ml;
    }

    public MapLayer mapSpeciesWMSByFilter(String label, String filter) {
        String uri;
        String layerName = "ALA:occurrences";
        String sld = "species_point";

        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
        } else {
            return null;
        }

        int hash = Math.abs(label.hashCode());
        int r = (hash >> 16) % 255;
        int g = (hash >> 8) % 255;
        int b = (hash) % 255;

        int size = 8;
        float opacity = (float) 0.8;

        if (activeLayerMapProperties != null) {
            r = ((Integer) activeLayerMapProperties.get("red")).intValue();
            b = ((Integer) activeLayerMapProperties.get("blue")).intValue();
            g = ((Integer) activeLayerMapProperties.get("green")).intValue();
            size = ((Integer) activeLayerMapProperties.get("size")).intValue();
            opacity = ((Float) activeLayerMapProperties.get("opacity")).floatValue();
        }

        Color c = new Color(r, g, b);
        String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
        String envString = "color:" + hexColour + ";name:circle;size:" + size + ";opacity:" + opacity + "";

        //uri = geoServer + "/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=ALA:occurrences&outputFormat=json&CQL_FILTER=";
        //geoServer = "http://localhost:8080";
        uri = geoServer + "/geoserver/wms?";
        uri += "service=WMS&version=1.1.0&request=GetMap&styles=&format=image/png";
        uri += "&layers=ALA:occurrences";
        uri += "&transparent=true"; // "&env=" + envString +
        uri += "&CQL_FILTER=";

        System.out.println("Mapping: " + label + " with " + uri + filter);

        try {
            if (safeToPerformMapAction()) {
                boolean addedOk = addKnownWMSLayer(label, uri + filter, (float) 0.8, "", envString);
                if (addedOk) {
                    MapLayer ml = getMapLayer(label);
                    ml.setDynamicStyle(true);
                    ml.setEnvParams(envString);
                    ml.setGeometryType(GeoJSONUtilities.POINT); // for the sizechooser

                    ml.setBlueVal(b);
                    ml.setGreenVal(g);
                    ml.setRedVal(r);
                    ml.setSizeVal(8);
                    ml.setOpacity(opacity);

                    return ml;
                }
            }
        } catch (Exception ex) {
            //logger.debug(ex.getMessage());
            System.out.println("error mapSpeciesByNameRank:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public MapLayer mapSpeciesByName(String speciesName) {
        return mapSpeciesByName(speciesName, null);
    }

    public MapLayer mapSpeciesByName(String speciesName, String commonName) {
        return mapSpeciesByNameRank(speciesName, "scientificname", commonName);
    }
    
    public MapLayer mapSpeciesByNameRank(String speciesName, String speciesRank, String commonName) {
        String filter;
        String uri;
        String layerName = "ALA:occurrences";
        String sld = "species_point";

        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
        } else {
            return null;
        }
        uri = geoServer + "/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=ALA:occurrences&outputFormat=json&CQL_FILTER=";

        //contruct the filter in cql
        //have to check the Genus name is in Capitals
        if (speciesRank.equals("scientificname")) {
            speciesRank = "species";
        }
        filter = speciesRank + " eq '" + StringUtils.capitalize(speciesName.trim()) + "'";

        String label = speciesName;

        if (StringUtils.isNotBlank(commonName)) {
            label += " (" + commonName + ")";
        }

        return mapSpeciesByFilter(label, filter);
    }

    public MapLayer mapSpeciesByFilter(String label, String filter) {
        String uri;
        String layerName = "ALA:occurrences";
        String sld = "species_point";

        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(CommonData.GEOSERVER_URL);
        } else {
            return null;
        }
        uri = geoServer + "/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=ALA:occurrences&outputFormat=json&CQL_FILTER=";

        System.out.println("Mapping: " + label + " with " + uri);

        try {
            if (safeToPerformMapAction()) {
                return addGeoJSON(label, uri + URLEncoder.encode(filter, "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            //logger.debug(ex.getMessage());
            System.out.println("error mapSpeciesByNameRank:");
            ex.printStackTrace(System.out);
        }
        return null;
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

    public boolean isLayerControlVisible() {
        return layerControls.isVisible();
    }

    SessionPrint print(String header, double grid, String format, int resolution, boolean preview) {
        //tbxPrintHack is 'screen width, screen height, map extents'
        String p = tbxPrintHack;
        System.out.println("tbxPrintHack:" + p);
        String[] ps = p.split(",");

        String server;
        server = settingsSupplementary.getValue("print_server_url");


        //session id/cookie JSESSIONID=
        String jsessionid = "";
        try {
            //get cookie
            for (Cookie c : ((HttpServletRequest) Executions.getCurrent().getNativeRequest()).getCookies()) {
                if (c.getName().equalsIgnoreCase("JSESSIONID")) {
                    System.out.println("printcookie:" + c.getValue());
                    jsessionid = c.getValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //width
        String width = "1024"; //default
        if (ps.length > 1) {
            width = ps[0];
        }

        //height
        String height = "800"; //default
        if (ps.length > 2) {
            height = ps[1];
        }

        //zoom (minlong, minlat, maxlong, maxlat)
        String zoom = "112,-44,154,-9"; //default
        if (ps.length > 5) {
            zoom = ps[2] + "," + ps[3] + "," + ps[4] + "," + ps[5];
        }

        //lhs panel width
        //append to zoom for now > String lhsWidth = "0"; //default
        if (ps.length > 6) {
            zoom += "," + ps[6]; //lhsWidth = ps[6];
        } else {
            zoom += ",350"; //default
        }
        //base map type
        if (ps.length > 7) {
            zoom += "," + ps[7];
        } else {
            zoom += ",normal";
        }


        //unique id
        String uid = String.valueOf(System.currentTimeMillis());

        String pth = this.settingsSupplementary.getValue("print_output_path");

        String htmlpth = pth;
        String htmlurl = settingsSupplementary.getValue("print_output_url");

        try {
            SessionPrint pp = new SessionPrint(server, height, width, htmlpth, htmlurl, uid, jsessionid, zoom, header, grid, format, resolution, this);

            if (!preview) {
                pp.print();

                File f = new File(pp.getImageFilename());
                System.out.println("img (" + pp.getImageFilename() + ") exists: " + f.exists());

                if (format.equalsIgnoreCase("png")) {
                    Filedownload.save(new File(pp.getImageFilename()), "image/png");
                } else if (format.equalsIgnoreCase("pdf")) {
                    Filedownload.save(new File(pp.getImageFilename()), "application/pdf");
                } else {
                    Filedownload.save(new File(pp.getImageFilename()), "image/jpeg");
                }
            }


            StringBuffer sbParams = new StringBuffer();
            sbParams.append("header: " + header);
            sbParams.append("grid: " + grid);
            sbParams.append("format: " + format);
            sbParams.append("resolution: " + resolution);
            sbParams.append("preview: " + preview);
            Map attrs = new HashMap();
            attrs.put("actionby", "user");
            attrs.put("actiontype", "print");
            attrs.put("lsid", "");
            attrs.put("useremail", "spatialuser");
            attrs.put("processid", "");
            attrs.put("sessionid", jsessionid);
            attrs.put("layers", "");
            attrs.put("method", "print");
            attrs.put("params", sbParams.toString());
            attrs.put("downloadfile", pp.getImageFilename());
            updateUserLog(attrs, "print");

            return pp;
        } catch (Exception e) {
            e.printStackTrace();
        }

        showMessage("Error generating export");

        return null;
    }

    /*
     * for Events.echoEvent("openUrl",mapComposer,url as string)
     */
    public void openUrl(Event event) {
        //browsers don't like popups so these two don't work well
        /*Clients.evalJavaScript("alert('hello'); window.open('"
        + (String)event.getData()
        + "', '_blank');");*/
        //Executions.getCurrent().sendRedirect((String)event.getData(), "_blank");

        /*
        Window w = new Window("Metadata", "normal", false);
        w.setWidth("80%");
        w.setClosable(true);
        
        Iframe iframe = new Iframe();
        iframe.setWidth("95%");
        iframe.setHeight("90%");
        iframe.setSrc((String) event.getData());
        iframe.setParent(w);
        
        w.setParent(getMapComposer().getFellow("mapIframe").getParent());
        w.setPosition("top,center");
        try {
        w.doModal();
        } catch (Exception e) {
        e.printStackTrace();
        }*/

        String s = (String) event.getData();
        int separator = s.lastIndexOf("\n");
        String url = (separator > 0) ? s.substring(0, separator).trim() : s;
        String header = (separator > 0) ? s.substring(separator).trim() : "";
        activateLink(url, header, false);
    }

    public String getViewArea() {
        //default to: current view to be dynamically returned on usage
        BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();

        String wkt = "POLYGON(("
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + "))";

        return wkt;
    }

    public Session getSession() {
        return Sessions.getCurrent();
    }

    public boolean useClustering() {
        return chkPointsCluster.isChecked();
    }

    public int getMapZoom() {
        return mapZoomLevel;
    }

    private void addToSession(String species, String filter) {
        Map speciesfilters = (Map) getSession().getAttribute("speciesfilters");
        if (speciesfilters == null) {
            speciesfilters = new HashMap();
        }
        speciesfilters.put(species, filter);
        getSession().setAttribute("speciesfilters", speciesfilters);
    }

    private void removeFromSession(String species) {
        Map speciesfilters = (Map) Sessions.getCurrent().getAttribute("speciesfilters");
        if (speciesfilters != null) {
            speciesfilters.remove(species);
            getSession().setAttribute("speciesfilters", speciesfilters);
        }
    }

    public void updateUserLogMapSpecies(String lsid) {
        Map attrs = new HashMap();
        attrs.put("actionby", "user");
        attrs.put("actiontype", "map");
        attrs.put("lsid", lsid);
        attrs.put("useremail", "spatialuser");
        attrs.put("processid", "");
        attrs.put("sessionid", "");
        attrs.put("layers", "");
        attrs.put("method", "");
        attrs.put("params", "");
        attrs.put("downloadfile", "");
        updateUserLog(attrs, "species");
    }

    public void updateUserLogMapLayer(String layers) {
        updateUserLogMapLayer("", layers);
    }

    public void updateUserLogMapLayer(String type, String layers) {
        Map attrs = new HashMap();
        attrs.put("actionby", "user");
        attrs.put("actiontype", "map");
        attrs.put("lsid", "");
        attrs.put("useremail", "spatialuser");
        attrs.put("processid", "");
        attrs.put("sessionid", "");
        attrs.put("layers", layers);
        attrs.put("method", "");
        attrs.put("params", "");
        attrs.put("downloadfile", "");
        updateUserLog(attrs, "layer: " + type);
    }

    public void updateUserLogAnalysis(String method, String params) {
        updateUserLogAnalysis(method, params, "");
    }

    public void updateUserLogAnalysis(String method, String params, String layers) {
        updateUserLogAnalysis(method, params, layers, "");
    }

    public void updateUserLogAnalysis(String method, String params, String layers, String msg) {
        updateUserLogAnalysis(method, params, layers, "", "", msg);
    }

    public void updateUserLogAnalysis(String method, String params, String layers, String download, String pid, String msg) {
        Map attrs = new HashMap();
        attrs.put("actionby", "user");
        attrs.put("actiontype", "analysis");
        attrs.put("lsid", "");
        attrs.put("useremail", "spatialuser");
        attrs.put("processid", pid);
        attrs.put("sessionid", "");
        attrs.put("layers", layers);
        attrs.put("method", method);
        attrs.put("params", params);
        attrs.put("downloadfile", download);
        updateUserLog(attrs, "analysis: " + msg);
    }

    public void updateUserLog(Map attrs) {
        updateUserLog(attrs, "");
    }

    public void updateUserLog(Map attrs, String msg) {
        Iterator it = attrs.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            MDC.put(key, attrs.get(key));
        }
        //MDC.put("userip", Executions.getCurrent().getRemoteAddr());
        String userip = Executions.getCurrent().getHeader("x-forwarded-for");
        if (StringUtils.isBlank(userip)) {
            userip = ""; 
        }
        MDC.put("userip", userip);

        logger.info(msg);
        MDC.clear();
    }

    /**
     * get Active Area as WKT string, from a layer name
     *
     * @param layer name of layer as String
     * @param register_shape true to register the shape with alaspatial shape register
     * @return
     */
    String getLayerGeoJsonAsWkt(String layer) {
        try {
            //class_name is same as layer name
            return wktFromJSON(getMapLayer(layer).getGeoJSON());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getViewArea();
    }

    private String wktFromJSON(String json) {
        try {
            JSONObject obj = JSONObject.fromObject(json);

            String coords = obj.getJSONArray("geometries").getJSONObject(0).getString("coordinates");

            if (obj.getJSONArray("geometries").getJSONObject(0).getString("type").equalsIgnoreCase("multipolygon")) {
                String wkt = coords.replace("]]],[[[", "))*((").replace("]],[[", "))*((").replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "MULTIPOLYGON(((").replace("]]]]", ")))");
                return wkt;
            } else {
                String wkt = coords.replace("],[", "*").replace(",", " ").replace("*", ",").replace("[[[[", "POLYGON((").replace("]]]]", "))");
                return wkt;
            }
        } catch (Exception e) {
            return "none";
        }
    }

    public void selectColour(Object obj) {
        Div div = (Div) obj;
        String style = div.getStyle();
        String background_color = "background-color";
        int a = style.indexOf(background_color);
        if (a >= 0) {
            String colour = style.substring(a + background_color.length() + 2, a + background_color.length() + 8);
            int r = Integer.parseInt(colour.substring(0, 2), 16);
            int g = Integer.parseInt(colour.substring(2, 4), 16);
            int b = Integer.parseInt(colour.substring(4, 6), 16);

            redSlider.setCurpos(r);
            greenSlider.setCurpos(g);
            blueSlider.setCurpos(b);
            redLabel.setValue(String.valueOf(r));
            greenLabel.setValue(String.valueOf(g));
            blueLabel.setValue(String.valueOf(b));

            updateLegendImage();
            onClick$applyChange();
        }
    }
    
    public void onUser(Event event){
        showMessage("onUser:" + (String)event.getData());        
    }
    
    public void onUser2(Event event){
        Clients.evalJavaScript("alert('from server:" + (String)event.getData() + "')");
    }
    
    public void onUser(){
        int a = 4;
    }
}
