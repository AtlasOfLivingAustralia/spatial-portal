package au.org.emii.portal.composer;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.composer.add.AddSpeciesController;
import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.composer.quicklinks.ContextualMenu;
import au.org.ala.spatial.composer.results.AreaReportController;
import au.org.ala.spatial.composer.results.DistributionsController;
import au.org.ala.spatial.composer.species.SpeciesAutoCompleteComponent;
import au.org.ala.spatial.data.*;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SelectedArea;
import au.org.ala.spatial.util.UserData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.legend.HasMapLayer;
import au.org.emii.portal.composer.legend.LayerLegendGeneralComposer;
import au.org.emii.portal.databinding.ActiveLayerRenderer;
import au.org.emii.portal.javascript.OpenLayersJavascript;
import au.org.emii.portal.lang.LanguagePack;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.net.HttpConnection;
import au.org.emii.portal.session.PortalSession;
import au.org.emii.portal.settings.Settings;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.*;
import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.web.SessionInitImpl;
import au.org.emii.portal.wms.RemoteMap;
import au.org.emii.portal.wms.WMSStyle;
import com.thoughtworks.xstream.persistence.FilePersistenceStrategy;
import com.thoughtworks.xstream.persistence.PersistenceStrategy;
import com.thoughtworks.xstream.persistence.XmlArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.intersect.SimpleShapeFile;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.LegendObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.SessionInit;
import org.zkoss.zul.Button;
import org.zkoss.zul.*;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * ZK composer for the index.zul page
 *
 * @author geoff
 */
public class MapComposer extends GenericAutowireAutoforwardComposer {

    private static Logger logger = Logger.getLogger(MapComposer.class);

    private SettingsSupplementary settingsSupplementary = null;
    private static final String MENU_DEFAULT_WIDTH = "menu_default_width";
    private static final String MENU_MINIMISED_WIDTH = "menu_minimised_width";
    public static final String POINTS_CLUSTER_THRESHOLD = "points_cluster_threshold";
    private static final long serialVersionUID = 1L;
    private RemoteMap remoteMap = null;
    public static final int DEFAULT_POINT_SIZE = 3;
    public static final float DEFAULT_POINT_OPACITY = 0.6f;
    public static final String DEFAULT_POINT_TYPE = "auto";

    /*
     * Autowired controls
     */
    private Iframe rawMessageIframeHack;
    private Div rawMessageHackHolder;
    Listbox activeLayersList;
    private Div layerControls;
    private West menus;
    private Div westContent;
    private Div westMinimised;
    /*
     * User data object to allow for the saving of maps and searches
     */
    private LanguagePack languagePack = null;
    private OpenLayersJavascript openLayersJavascript = null;
    private HttpConnection httpConnection = null;
    ActiveLayerRenderer activeLayerRenderer = null;
    private PortalSessionUtilities portalSessionUtilities = null;
    private Settings settings = null;

    HtmlMacroComponent contextualMenu;
    public String tbxPrintHack;
    int mapZoomLevel = 4;
    Hashtable activeLayerMapProperties;
    Label lblSelectedLayer;
    String useSpeciesWMSCache = "on";
    ArrayList<LayerSelection> selectedLayers = new ArrayList<LayerSelection>();

    /*
     * for capturing layer loaded events signaling listeners
     */
    String tbxLayerLoaded;
    HashMap<String, EventListener> layerLoadedChangeEvents = new HashMap<String, EventListener>();
    RemoteLogger remoteLogger;
    Textbox currentLayerExtent;

    public String featuresCSV;

    LayerLegendGeneralComposer llc2;
    public MapLayer llc2MapLayer;

    public void onClick$removeAllLayers() {
        if (safeToPerformMapAction()) {
            List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
            StringBuilder script = new StringBuilder();
            while (activeLayers.size() > 1) {
                MapLayer mapLayer = activeLayers.get(0);

                if (mapLayer.getType() != LayerUtilities.MAP) {
                    script.append(openLayersJavascript.removeMapLayer(mapLayer));

                    // skip executing JS and reseting the layer controls - do
                    // them at the end
                    deactiveLayer(mapLayer, false, false);
                }
            }
            updateLayerControls();
            refreshContextualMenu();
            openLayersJavascript.execute(
                    OpenLayersJavascript.iFrameReferences
                            + script.toString());
        }
    }

    public void safeToLoadMap(Event event) {
        mapLoaded("true");

        //listen for map extents changes
        EventListener el = new EventListener() {

            public void onEvent(Event event) throws Exception {
                onReloadLayers(null);
            }
        };
        getLeftmenuSearchComposer().addViewportEventListener("onReloadLayers", el);
    }

    public void zoomToExtent(MapLayer selectedLayer) {
        if (selectedLayer != null && selectedLayer.isDisplayed()) {
            logger.debug("zooming to extent " + selectedLayer.getId());
            if (selectedLayer.getType() == LayerUtilities.GEOJSON
                    || selectedLayer.getType() == LayerUtilities.WKT
                    || selectedLayer.getType() == LayerUtilities.KML) {
                openLayersJavascript.zoomGeoJsonExtentNow(selectedLayer);
            } else {
                openLayersJavascript.zoomLayerExtent(selectedLayer);
            }
        }
    }

    public void applyChange(MapLayer selectedLayer) {
        if (selectedLayer != null && selectedLayer.isDisplayed()) {
            /*
             * different path for each type layer 1. symbol 2. classification
             * legend 3. prediction legend 4. other (wms)
             */
            if (selectedLayer.isDynamicStyle()) {

                Color c = new Color(selectedLayer.getRedVal(), selectedLayer.getGreenVal(), selectedLayer.getBlueVal());
                String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
                String rgbColour = "rgb(" + String.valueOf(selectedLayer.getRedVal()) + "," + selectedLayer.getGreenVal()
                        + "," + selectedLayer.getBlueVal() + ")";
                selectedLayer.setEnvColour(rgbColour);

                if (selectedLayer.getType() == LayerUtilities.GEOJSON) {
                    openLayersJavascript.redrawFeatures(selectedLayer);
                } else if (selectedLayer.getType() == LayerUtilities.WKT) {
                    openLayersJavascript.redrawWKTFeatures(selectedLayer);
                } else {
                    String envString = "";
                    if (selectedLayer.getColourMode().equals("-1")) {
                        envString += "color:" + hexColour;
                    } else {
                        LegendObject lo = selectedLayer.getLegendObject();
                        if (lo != null && lo.getColourMode() != null) {
                            envString += "colormode:" + lo.getColourMode();
                        } else {
                            envString += "colormode:" + selectedLayer.getColourMode();
                        }
                    }
                    envString += ";name:circle;size:" + selectedLayer.getSizeVal();

                    //Opacity now handled only by openlayers
//                    envString += ";opacity:" + selectedLayer.getOpacity();
                    if (selectedLayer.getHighlight() != null && selectedLayer.getHighlight().length() > 0
                            && !selectedLayer.getColourMode().equals("grid")) {
                        //Highlight now handled in a second layer
                    } else if (selectedLayer.getSizeUncertain()) {
                        envString += ";uncertainty:1";
                    }
                    selectedLayer.setEnvParams(envString + ";opacity:1");

                    if (selectedLayer.hasChildren()) {
                        MapLayer highlightLayer = selectedLayer.getChild(0);
                        if (highlightLayer.getName().equals(selectedLayer.getName() + "_highlight")) {
                            //apply sel to envString
                            String highlightEnv = "color:000000;size:" + selectedLayer.getSizeVal() + ";opacity:0";
                            highlightLayer.setOpacity(1);
                            if (selectedLayer.getHighlight() != null && selectedLayer.getHighlight().length() > 0
                                    && !selectedLayer.getColourMode().equals("grid")) {
                                if (selectedLayer.getSpeciesQuery() instanceof UserDataQuery) {
                                    try {
                                        highlightLayer.setEnvParams(highlightEnv + ";sel:"
                                                + selectedLayer.getHighlight().replace(";", "%3B"));
                                    } catch (Exception e) {
                                        logger.error("error encoding highlight to UTF-8: " + selectedLayer.getHighlight(), e);
                                    }
                                } else {
                                    highlightLayer.setEnvParams(highlightEnv + ";sel:" + selectedLayer.getHighlight().replace(";", "%3B"));
                                }
                                highlightLayer.setHighlightState("show");
                            } else {
                                highlightLayer.setHighlightState("hide");
                                highlightLayer.setEnvParams(highlightEnv);
                            }
                        }
                    }

                    reloadMapLayerNowAndIndexes(selectedLayer);
                }
            } else if (selectedLayer.getSelectedStyle() != null) {
                /*
                 * 1. classification legend has uri with ".zul" content 2.
                 * prediction legend works here *
                 */
                logger.debug("******** is this ever reached? **********");
                selectedLayer.setOpacity(selectedLayer.getOpacity());
                String legendUri = selectedLayer.getSelectedStyle().getLegendUri();
                if (legendUri.contains(".zul")) {
                    addImageLayer(selectedLayer.getId(),
                            selectedLayer.getName(),
                            selectedLayer.getUri(),
                            selectedLayer.getOpacity(),
                            null, LayerUtilities.ALOC);  //bbox is null, not required for redraw
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

    public void reloadMapLayerNowAndIndexes(MapLayer selectedLayer) {
        if (safeToPerformMapAction()) {
            PortalSession portalSession = (PortalSession) Executions.getCurrent().getDesktop().getSession().getAttribute("portalSession");

            openLayersJavascript.execute(
                    OpenLayersJavascript.iFrameReferences
                            + openLayersJavascript.reloadMapLayer(selectedLayer)
                            + openLayersJavascript.updateMapLayerIndexes(
                            portalSession.getActiveLayers()));
        }
    }

    /**
     * Maps a species based on the selected item in the supplied species auto
     * complete component.
     *
     * @param sacc
     * @param sa
     * @param geospatialKosher
     */
    public void mapSpeciesFromAutocompleteComponent(SpeciesAutoCompleteComponent sacc, SelectedArea sa, boolean[] geospatialKosher) {
        if (!sacc.hasValidAnnotatedItemSelected()) {
            return;
        }
        String[] details = sacc.getSelectedTaxonDetails();
        if (details != null) {
            String taxon = details[0];
            String rank = details[1];
            Query query = sacc.getQuery(this, false, geospatialKosher);
            Query q = QueryUtil.queryFromSelectedArea(query, sa, false, geospatialKosher);
            String wkt = sa == null ? null : sa.getWkt();
            mapSpecies(q, taxon, rank, 0, LayerUtilities.SPECIES, wkt, -1, DEFAULT_POINT_SIZE, DEFAULT_POINT_OPACITY, Util.nextColour());
            logger.debug(">>>>> " + taxon + ", " + rank + " <<<<<");
        }
    }

    /**
     * Reorder the active layers list based on a d'n'd event
     *
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

        redrawLayersList();

        // hide legend controls
        //hideLayerControls(null);
        refreshContextualMenu();
    }

    /**
     * Remove a maplayer from the active layers list and then reinsert it at the
     * same spot - should cause this part of the list to be re-rendered.
     * <p/>
     * After re-rendering, reselect the corresponding item in its listbox, as
     * operations such as changing opacity and animation require a layer is
     * selected
     *
     * @param mapLayer
     */
    public void refreshActiveLayer(MapLayer mapLayer) {
        ListModelList model = (ListModelList) activeLayersList.getModel();
        int index = model.indexOf(mapLayer);
        if (index >= 0) {
            model.remove(index);
            model.add(index, mapLayer);

            activeLayersList.setSelectedIndex(index);
        }

        adjustActiveLayersList();
    }

    public void activateLink(String uri, String label, boolean isExternal) {
        activateLink(uri, label, isExternal, "");
    }

    public void activateLink(String uri, String label, boolean isExternal, String downloadPid) {
        closeExternalContentWindow();

        Window externalContentWindow = (Window) Executions.createComponents("WEB-INF/zul/ExternalContent.zul", layerControls, null);

        if (isExternal) {
            // change browsers current location
            Clients.evalJavaScript("window.location.href ='" + uri + "';");
        } else {

            Iframe iframe = (Iframe) externalContentWindow.getFellow("externalContentIframe");
            Html html = (Html) externalContentWindow.getFellow("externalContentHTML");

            if (uri.charAt(0) == '*') {
                //html content
                uri = uri.substring(1);

                //url
                iframe.setHeight("0px");
                iframe.setSrc("");

                String content = "";
                if (label.equalsIgnoreCase("download")) {
                    // Added fast download option -
                    // TODO refactor so this can be generated from same code that sets the downloadUrl (uri) in BiocacheQuery.java
                    String fastDownloadUrl = uri.replaceFirst("/occurrences/download", "/occurrences/index/download");

                    StringBuilder sbContent = new StringBuilder();
                    sbContent.append("<p id='termsOfUseDownload' style='padding:10px; margin-bottom: 0;'>");
                    sbContent.append("By downloading this content you are agreeing to use it in accordance ");
                    sbContent.append("with the Atlas of Living Australia <a href='http://www.ala.org.au/about/terms-of-use/#TOUusingcontent'>Terms of Use</a>");
                    sbContent.append(" and any Data Provider Terms associated with the data download. ");
                    sbContent.append("<br/><br/>");
                    sbContent.append("Please provide the following details before downloading (* required)");
                    sbContent.append("</p>");
                    sbContent.append("    <form id='downloadForm' onSubmit='downloadSubmitButtonClick(); return false;' style='padding:10px;'>");
                    sbContent.append("        <input type='hidden' name='url' id='downloadUrl' value='").append(uri).append("'/>");
                    sbContent.append("        <input type='hidden' name='url' id='fastDownloadUrl' value='").append(fastDownloadUrl).append("'/>");
                    //sbContent.append("        <input type='hidden' name='url' id='downloadChecklistUrl' value='http://biocache.ala.org.au/ws/occurrences/facets/download?q=text:macropus rufus'/>");
                    //sbContent.append("        <input type='hidden' name='url' id='downloadFieldGuideUrl' value='/occurrences/fieldguide/download?q=text:macropus rufus'/>");
                    sbContent.append("        <fieldset>");
                    sbContent.append("            <p><label for='email'>Email</label>");
                    sbContent.append("                <input type='text' name='email' id='email' value='' size='30'  />");
                    sbContent.append("            </p>");
                    sbContent.append("            <p><label for='filename'>File Name</label>");
                    sbContent.append("                <input type='text' name='filename' id='filename' value='data' size='30'  />");
                    sbContent.append("            </p>");

                    sbContent.append("            <p><label for='reasonTypeId' style='vertical-align: top'>Download Reason *</label>");
                    sbContent.append("            <select name='reasonTypeId' id='reasonTypeId'>");
                    sbContent.append("            <option value=''>-- select a reason --</option>");
                    JSONArray dlreasons = CommonData.getDownloadReasons();
                    for (int i = 0; i < dlreasons.size(); i++) {
                        JSONObject dlr = dlreasons.getJSONObject(i);
                        sbContent.append("            <option value='").append(dlr.getInt("id")).append("'>").append(dlr.getString("name")).append("</option>");
                    }
                    sbContent.append("            <select></p>");
                    sbContent.append("                    <input style='display:none' type='radio' name='downloadType' value='fast' class='tooltip' checked='checked' title='Faster download but fewer fields are included'>");

                    sbContent.append("            <p style='clear:both;'>&nbsp;</p>");
                    sbContent.append("            <p style='text-align:center;'><input type='submit' value='Download All Records' id='downloadSubmitButton'/></p>");

                    sbContent.append("        </fieldset>");
                    sbContent.append("    </form>");

                    content = sbContent.toString();
                } else {
                    content = uri;
                }

                //content
                html.setContent(content);
                html.setStyle("overflow: scroll;padding: 0 10px;");

                //for the 'reset window' button
                ((ExternalContentComposer) externalContentWindow).src = "";

                //update linked button
                externalContentWindow.getFellow("breakout").setVisible(false);

                externalContentWindow.setContentStyle("overflow:auto");
            } else {
                //url
                iframe.setHeight("100%");
                iframe.setSrc(uri);

                //content
                html.setContent("");

                //for the 'reset window' button
                ((ExternalContentComposer) externalContentWindow).src = uri;

                //update linked button
                ((Toolbarbutton) externalContentWindow.getFellow("breakout")).setHref(uri);
                externalContentWindow.getFellow("breakout").setVisible(true);

                externalContentWindow.setContentStyle("overflow:visible");
            }

            if (StringUtils.isNotBlank(downloadPid)) {
                String downloadUrl = CommonData.satServer + "/ws/download/" + downloadPid;
                if (downloadPid.startsWith("http")) {
                    downloadUrl = downloadPid;
                }
                ((Toolbarbutton) externalContentWindow.getFellow("download")).setHref(downloadUrl);
                externalContentWindow.getFellow("download").setVisible(true);
            } else {
                ((Toolbarbutton) externalContentWindow.getFellow("download")).setHref("");
                externalContentWindow.getFellow("download").setVisible(false);
            }

            // use the link description as the popup caption
            ((Caption) externalContentWindow.getFellow("caption")).setLabel(
                    label);
            externalContentWindow.setPosition("center");
            try {
                externalContentWindow.doModal();
            } catch (Exception e) {
                logger.error("error opening information popup", e);
            }
        }
    }

    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript) {
        return activateLayer(mapLayer, doJavaScript, false);
    }

    /**
     * Activate a map layer on the map
     *
     * @param doJavaScript set false to defer execution of JavaScript which
     *                     actually adds the layer to the openlayers menu
     * @return true if the layer was added successfully, otherwise false
     */
    public boolean activateLayer(MapLayer mapLayer, boolean doJavaScript, boolean skipTree) {
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        boolean layerAdded = false;

        /*
         * switch to the ListModelList if we are currently using simplelistmodel
         * to display the 'no layers selected' message
         *
         * If the model is already an instance of ListModelList then the model
         * should already have the data it needs so just fire the update event.
         */
        if (!(activeLayersList.getModel() instanceof ListModelList)) {
            logger.debug("changing model for Active Layers to ListModelList");
            /*
             * this is the first item being added to the list so we make it a
             * new ListModelList instance based on live data
             */
            activeLayersList.setModel(new ListModelList(activeLayers, true));

        }

        if (!activeLayers.contains(mapLayer)) {

            /*
             * assume we want to display on the map straight away - set checkbox
             * to true
             */
            activeLayersList.setItemRenderer(activeLayerRenderer);


            /*
             * use the MODEL facade to add the new layer (it's not smart enough
             * to detect the change otherwise.
             *
             * We always add to the top of the list so that newly actived map
             * layers display above existing ones
             */
            ((ListModelList) activeLayersList.getModel()).add(0, mapLayer);

            //select this map layer
            activeLayersList.setSelectedIndex(0);

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

        adjustActiveLayersList();

        refreshContextualMenu();

        return layerAdded;

    }

    /**
     * Remove an item from the list of active layers and put it back in the tree
     * menu of available layers
     *
     * @param itemToRemove
     */
    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive) {
        deactiveLayer(itemToRemove, updateMapAndLayerControls, recursive, false);
    }

    public void deactiveLayer(MapLayer itemToRemove, boolean updateMapAndLayerControls, boolean recursive, boolean updateOnly) {
        if (itemToRemove != null) {
            Query q = itemToRemove.getSpeciesQuery();
            if (q != null && q instanceof UserDataQuery) {
                String pid = q.getQ();

                Hashtable<String, UserData> htUserSpecies = (Hashtable) getMapComposer().getSession().getAttribute("userpoints");
                if (htUserSpecies != null) {
                    htUserSpecies.remove(pid);
                }
            }

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
                            lblSelectedLayer.setValue("No layers added");
                        }
                    }
                } else {
                    logger.debug("active layers list is empty, so not updating it");
                }
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

        refreshContextualMenu();
    }

    /**
     * Remove an item from the list of active layers and put it back in the tree
     * menu of available layers
     *
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
                    }
                } else {
                    logger.debug("active layers list is empty, so not updating it");
                }
            }
        }

        refreshContextualMenu();
    }

    /**
     * A simple message dialogue
     *
     * @param message Full text of message to show
     */
    public void showMessage(String message) {
        ErrorMessageComposer window = (ErrorMessageComposer) Executions.createComponents("WEB-INF/zul/ErrorMessage.zul", null, null);
        window.setMessage(message);
        window.doOverlapped();
    }

    /**
     * A simple message dialogue to display over AnalysisToolComposer
     *
     * @param message Full text of message to show
     */
    boolean mp = true;

    public void showMessage(String message, Component parent) {
        ErrorMessageComposer window = (ErrorMessageComposer) Executions.createComponents("WEB-INF/zul/ErrorMessage.zul", parent, null);
        window.setMessage(message);
        if (mp) {
            try {
                window.doModal();
            } catch (Exception e) {
                logger.error("error opening message window", e);
            }
        } else {
            window.doOverlapped();
        }
    }

    /**
     * Show a message dialogue. Initially a short message is shown but the user
     * can click 'show details' to get a more informative message.
     * <p/>
     * A default message title is obtained from the config file
     *
     * @param message
     * @param messageDetail
     */
    public void showMessage(String message, String messageDetail) {
        showMessage(languagePack.getLang("default_message_title"), message, messageDetail);
    }

    /**
     * Show a message dialogue. Initially a short message is shown but the user
     * can click 'show details' to get a more informative message.
     * <p/>
     * A title must be provided for the popup box
     *
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
     * This is a fixed size (because of zk limitations) message dialogue
     * featuring: o	title o	brief description o	detailed description (hidden by
     * default) o	link to raw data (hidden by default) o	Iframe full of raw data
     * (hidden by default) There are lots of parameters for this method so if
     * you want to use it, it's easiest to write a wrapper and just call that
     * when you have a problem.
     * <p/>
     * This is not a general purpose error message - everything is fixed sizes
     * (has to be or the iframe doesn't display properly. If you want a general
     * purpose error message, use the showMessageNow() calls
     *
     * @param title           TEXT of the title or null to use default from config file
     * @param message         TEXT in config file of the error message
     * @param messageDetail   TEXT to display if user clicks 'show detail'
     * @param rawMessageTitle TEXT to display before raw output or null to
     *                        ignore
     * @param rawMessage      TEXT of raw error message or null to ignore
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

        /*
         * StringMedia is not allowed to be null (will give an error) so if no
         * data was available, give the user a message
         */
        if (rawMessage == null) {
            rawMessage = languagePack.getLang("null_raw_data");
        }
        //StringMedia rawErrorMessageMedia = new StringMedia(rawMessage);

        /*
         * have to store the raw error message in the user's session temporarily
         * to prevent getting a big zk severe error if the iframe is requested
         * after it is supposed to have been dereferenced
         */
        //getPortalSession().setRawErrorMessageMedia(rawErrorMessageMedia);
        rawMessageIframeHack.setContent(null);

        /*
         * the raw text can't go in with the params or it gets escaped by zk -
         * you have to do weird things with iframes and the media class instead
         */
        Window window;
        window = (Window) Executions.createComponents("WEB-INF/zul/ErrorMessageWithDetailAndRawData.zul", null, params);

        /*
         * now we grab the hidden iframe in index.zul and move it into our
         * message box - again this is to prevent a massive zk error if the
         * iframe content is requested after it's supposed to have gone
         */
        Component holder = window.getFellow("rawMessageHolder");
        rawMessageIframeHack.setParent(holder);
        window.doOverlapped();

        /*
         * at this point the user has closed the message box - we only have one
         * more thing to do to stop the big zk error and that is to grab the
         * iframe back off the error message window and put it back where we
         * found it in the index.zul page - not pretty or efficient but works a
         * treat!
         */
        rawMessageIframeHack.setParent(rawMessageHackHolder);
    }

    /**
     * Add a map layer to the user defined map layers group (My Layers)
     *
     * @param mapLayer
     */
    public void addUserDefinedLayerToMenu(MapLayer mapLayer, boolean activate) {
        if (safeToPerformMapAction()) {
            // activate the layer in openlayers and display in active layers without
            // updating the tree (because its not displayed)
            activateLayer(mapLayer, true, true);

            logger.debug("leaving addUserDefinedLayerToMenu");
        }
    }

    /**
     * Initial building of the tree menu and active layers list based on the
     * values obtained from the current session.
     * <p/>
     * JavaScript for loading default map layers and setting the default zoombox
     * is in SessionInit.java
     */
    public void load() {
        logger.debug("entering loadMapLayers");

//        motd();
        PortalSession portalSession = getPortalSession();
        List<MapLayer> activeLayers = portalSession.getActiveLayers();

        // model and renderer for active layers list
        ListModelList activeLayerModel = new ListModelList(activeLayers, true);

        // tell the list about them...
        if (activeLayers.size() == 0) {
            MapLayer ml = remoteMap.createLocalLayer(LayerUtilities.MAP, "Map options");
            ml.setRemoveable(false);
            activeLayers.add(ml);
        }

        activeLayersList.setModel(activeLayerModel);
        activeLayersList.setItemRenderer(activeLayerRenderer);
        activeLayersList.setSelectedIndex(activeLayerModel.size() - 1);

        updateLayerControls();
        refreshContextualMenu();

    }

    /**
     * Adds a object as a layer to the map.
     *
     * @param pid
     */
    public MapLayer addObjectByPid(String pid) {

        JSONObject obj = JSONObject.fromObject(Util.readUrl(CommonData.layersServer + "/object/" + pid));
        //add feature to the map as a new layer
        String areaName = obj.getString("name"); //retrieve from service
        MapLayer mapLayer = getMapComposer().addWMSLayer(areaName, areaName, obj.getString("wmsurl"), 0.6f, null, null, LayerUtilities.WKT, null, null);
        if (mapLayer == null) {
            return null;
        }
        mapLayer.setName("PID:" + pid);
        mapLayer.setDisplayName(areaName);
        mapLayer.setPolygonLayer(true);

        //if the layer is a point create a radius
        if (mapLayer != null) {  //might be a duplicate layer making mapLayer == null
            String bbox = obj.getString("bbox");
            String fid = obj.getString("fid");
            String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.layersServer + "/fields"));

            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            try {
                double[][] bb = SimpleShapeFile.parseWKT(bbox).getBoundingBox();
                ArrayList<Double> dbb = new ArrayList<Double>();
                dbb.add(bb[0][0]);
                dbb.add(bb[0][1]);
                dbb.add(bb[1][0]);
                dbb.add(bb[1][1]);
                md.setBbox(dbb);
            } catch (Exception e) {
                logger.debug("failed to parse: " + bbox, e);
            }
            md.setMoreInfo(CommonData.layersServer + "/layers/view/more/" + spid);

            Facet facet = Util.getFacetForObject(obj, areaName);
            if (facet != null) {
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
                mapLayer.setWKT("ENVELOPE(" + fid + "," + pid + ")");
            } else {
                //not in biocache, so add as WKT
                mapLayer.setWKT(Util.readUrl(CommonData.layersServer + "/shape/wkt/" + pid));
            }
        }

        return mapLayer;
    }

    /**
     * Add a WMS layer identified by the given parameters to the menu system and
     * activate it
     *
     * @param name    Name of map layer
     * @param uri     URI for the WMS service
     * @param opacity 0 for invisible, 1 for solid
     */
    public MapLayer addWMSLayer(String name, String displayName, String uri, float opacity, String metadata, String legendUri, int subType, String cqlfilter, String envParams) {
        return addWMSLayer(name, displayName, uri, opacity, metadata, legendUri, subType, cqlfilter, envParams, null);
    }

    public MapLayer addWMSLayer(String name, String displayName, String uri, float opacity, String metadata, String legendUri, int subType, String cqlfilter, String envParams, Query q) {
        MapLayer mapLayer = null;
        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), uri) == null) {
                mapLayer = remoteMap.createAndTestWMSLayer(name, uri, opacity);
                mapLayer.setDisplayName(displayName);
                if (q != null) {
                    mapLayer.setSpeciesQuery(q);
                }
                if (mapLayer == null) {
                    // fail
                    //errorMessageBrokenWMSLayer(imageTester);
                    logger.debug("adding WMS layer failed ");
                } else {
                    //ok
                    mapLayer.setSubType(subType);
                    mapLayer.setCql(cqlfilter);
                    mapLayer.setEnvParams(envParams);
                    uri = CommonData.geoServer + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + mapLayer.getLayer();
                    mapLayer.setDefaultStyleLegendUri(uri);
                    if (metadata != null) {
                        if (metadata != null && metadata.startsWith("http")) {
                            mapLayer.getMapLayerMetadata().setMoreInfo(metadata + "\n" + displayName);
                        } else {
                            mapLayer.getMapLayerMetadata().setMoreInfo(metadata);
                        }
                    }
                    if (legendUri != null) {
                        WMSStyle style = new WMSStyle();
                        style.setName("Default");
                        style.setDescription("Default style");
                        style.setTitle("Default");
                        style.setLegendUri(legendUri);
                        mapLayer.addStyle(style);
                        mapLayer.setSelectedStyleIndex(1);
                        logger.debug("adding WMSStyle with legendUri: " + legendUri);
                        mapLayer.setDefaultStyleLegendUriSet(true);
                    }

                    addUserDefinedLayerToMenu(mapLayer, true);
                }
            } else {
                // fail
                showMessage(languagePack.getLang("wms_layer_already_exists"));
                logger.debug(
                        "refusing to add a new layer with URI " + uri
                                + " because it already exists in the menu");
            }
        }
        return mapLayer;
    }

    public MapLayer getMapLayer(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        logger.debug("session active layers: " + udl.size() + " looking for: " + label);
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            logger.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getName().equals(label)) {
                return ml;
            }
        }

        // now check if we can find it using the display name
        return getMapLayerDisplayName(label);
    }

    public int getMapLayerIdxInLegend(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        logger.debug("session active layers: " + udl.size() + " looking for: " + label);
        int pos = -1;
        while (iudl.hasNext()) {
            pos++;
            MapLayer ml = (MapLayer) iudl.next();
            logger.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getName().equals(label)) {
                return pos;
            }
        }

        // now check if we can find it using the display name
        return 0;
    }

    public MapLayer getMapLayerDisplayName(String label) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        logger.debug("session active layers: " + udl.size() + " looking for: " + label);
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            logger.debug("layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS());
            if (ml.getDisplayName().equals(label)) {
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
                logger.debug("unable to remove layer with label" + label);
            }
        }
    }

    public MapLayer addImageLayer(String id, String label, String uri, float opacity, List<Double> bbox, int subType) {
        // check if layer already present
        MapLayer imageLayer = getMapLayer(label);

        if (safeToPerformMapAction()) {
            if (imageLayer == null) {
                logger.debug("activating new layer");

                //start with a new MapLayer
                imageLayer = new MapLayer();
                //set its type
                imageLayer.setType(LayerUtilities.IMAGELAYER);
                //the name is what will appear in the active layer list
                imageLayer.setName(label);

                imageLayer.setSubType(subType);

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
                imageLayer.getMapLayerMetadata().setBbox(bbox);

                //call this to add it to the map and also put it in the active layer list
                activateLayer(imageLayer, true, true);

            } else {
                logger.debug("refreshing exisiting layer");
                imageLayer.setUri(uri); // + "&_lt=" + System.currentTimeMillis());
                imageLayer.setOpacity(opacity); // (float) 0.75

                openLayersJavascript.reloadMapLayerNow(imageLayer);
            }
        }

        return imageLayer;
    }

    /*
     * Public utility methods to interogate the state of the form controls
     */

    /**
     * Return the MapLayer instance associated with the item currently selected
     * in the active layers listbox or null if no item is currently selected.
     * <p/>
     * If nothing is selected and alertOnNoSelection is set true, show the user
     * a message and return null
     *
     * @return selected MapLayer instance or null if there is nothing selected
     */
    public MapLayer getActiveLayersSelection(boolean alertOnNoSelection) {
        MapLayer mapLayer = null;

        // only one item can be selected at a time
        Listitem selected = activeLayersList.getSelectedItem();

        if (selected != null) {
            mapLayer = selected.getValue();
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
        Listitem selected = activeLayersList.getSelectedItem();
        toggleLayer(selected, bCheck);
    }

    public void toggleLayer(Listitem selected, boolean bCheck) {

        if (selected != null) {
            for (Object cell : selected.getChildren()) {
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

    public void mapLoaded(String text) {
        boolean loaded = Boolean.parseBoolean(text);
        getPortalSession().setMapLoaded(loaded);

        if (loaded) {
            openLayersJavascript.setAdditionalScript("window.mapFrame.loadBaseMap();");

            logger.debug("map is now loaded. let's try mapping.");
            MapLayer ml = loadUrlParameters();

            if (ml == null) {
                openLayersJavascript.useAdditionalScript();
            }
        }
    }

    /**
     * Check if its safe to do things to the map - if it's not, show a popup box
     *
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


    //-- AfterCompose --//
    @Override
    public void afterCompose() {
        super.afterCompose();

        //window settings, for printing
        applyWindowParams();

        // showtime!
        load();

        //active layers list
    }

    /**
     * apply window parameters
     * <p/>
     * p = width in pixels,height in
     * pixels,longitude1,latitude1,longitude2,latitude2
     */
    void applyWindowParams() {
        String s = Executions.getCurrent().getParameter("p");

        //TODO: validate params
        if (s != null) {
            String[] pa = s.split(",");
            setWidth(pa[0] + "px");

            if (pa.length > 1) {
                setHeight(pa[1] + "px");
            }
        }
    }

    public String getCookieValue(String cookieName) {
        for (Cookie c : ((HttpServletRequest) Executions.getCurrent().getNativeRequest()).getCookies()) {
            if (c.getName().equals(cookieName)) {
                return c.getValue();
            }
        }

        return null;
    }

    private Pattern layerNameParamPattern = Pattern.compile("ly\\.[0-9]{1,}");

    public void mapMultiQueryLayers(boolean[] geospatialKosher, String baseBiocacheUrl, String baseWSBiocacheUrl, boolean supportDynamic) {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());
        for (String key : userParams.keySet()) {
            if (layerNameParamPattern.matcher(key).find()) {
                //we have a layer, retrieve the other bits
                String layerName = userParams.get(key);
                String query = userParams.get(key + ".q");
                String style = userParams.get(key + ".s");

                logger.debug(String.format("Add layer: '%s', query: '%s', style: '%s', key: '%s'", layerName, query, style, key));

                //format the query
                if (query != null && query.contains(",")) {
                    String[] queryComponents = query.split(",");
                    query = StringUtils.join(queryComponents, " OR ");
                }
                if (query != null && style != null && layerName != null) {

                    BiocacheQuery q = new BiocacheQuery(null, null, query, null, true, geospatialKosher, baseBiocacheUrl, baseWSBiocacheUrl, supportDynamic);
                    mapSpecies(q, layerName, "species", q.getOccurrenceCount(), LayerUtilities.SPECIES, null, 0, 4, 0.8f, Integer.decode(style));
                }
            }
        }
    }

    /**
     * Maps environmental and contextual layers from a "&layers" param. Uses the
     * short name of the layer. e.g. "aspect" or ""
     */
    public void mapLayerFromParams() {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());
        if (userParams != null) {
            String layersCSV = userParams.get("layers");
            if (StringUtils.trimToNull(layersCSV) == null) {
                return;
            }
            String[] layers = layersCSV.split(",");
            for (String s : layers) {
                JSONArray layerlist = CommonData.getLayerListJSONArray();
                for (int j = 0; j < layerlist.size(); j++) {
                    JSONObject jo = layerlist.getJSONObject(j);
                    String name = jo.getString("name");
                    if (name.equalsIgnoreCase(s)) {
                        String uid = jo.getString("id");
                        String type = jo.getString("type");
                        String treeName = StringUtils.capitalize(jo.getString("displayname"));
                        String treePath = jo.getString("displaypath");
                        String legendurl = CommonData.geoServer + "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=9&LAYER=" + s;
                        String metadata = CommonData.layersServer + "/layers/view/more/" + uid;
                        getMapComposer().addWMSLayer(s, treeName, treePath, (float) 0.75, metadata, legendurl, type.equalsIgnoreCase("environmental") ? LayerUtilities.GRID : LayerUtilities.CONTEXTUAL, null, null, null);
                    }
                }
            }
        }
    }

    /**
     * Maps environmental and contextual layers from a "&layers" param. Uses the
     * short name of the layer. e.g. "aspect" or ""
     */
    public void mapObjectFromParams() {
        Map<String, String> userParams = getQueryParameterMap(Executions.getCurrent().getDesktop().getQueryString());
        if (userParams != null) {
            String pidsAsString = userParams.get("pid");
            if (StringUtils.trimToNull(pidsAsString) == null) {
                return;
            }
            String[] pids = pidsAsString.trim().split(",");
            List<MapLayer> mapLayers = getMapComposer().getPortalSession().getActiveLayers();
            Map<String, MapLayer> names = new HashMap<String, MapLayer>();
            for (MapLayer ml : mapLayers) {
                names.put(ml.getName(), ml);
            }
            for (String pid : pids) {
                if (names.get("PID:" + pid) == null) {
                    MapLayer mapLayer = getMapComposer().addObjectByPid(pid);
                    if (pids.length == 1) {
                        //zoom to this region
                        getMapComposer().zoomToExtent(mapLayer);
                    }
                } else if (pids.length == 1) {
                    MapLayer mapLayer = names.get("PID:" + pid);
                    getMapComposer().zoomToExtent(mapLayer);
                }
            }
        }
    }

    /**
     * Parsing of "q" and "fq" params
     *
     * @return
     */
    private MapLayer loadUrlParameters() {
        String params = null;

        try {
            String analysis_layer_selections = getCookieValue("analysis_layer_selections");
            if (analysis_layer_selections != null) {
                String[] s = URLDecoder.decode(analysis_layer_selections, "UTF-8").split("\n");
                for (int i = 0; i < s.length; i++) {
                    String[] ls = s[i].split(" // ");
                    selectedLayers.add(new LayerSelection(ls[0], ls[1]));
                }
            }
        } catch (Exception e) {
        }

        try {
            params = Executions.getCurrent().getDesktop().getQueryString();
            logger.debug("User params: " + params);

            List<Entry<String, String>> userParams = Util.getQueryParameters(params);
            StringBuilder sb = new StringBuilder();
            String qc = null;
            String bs = null;
            String ws = null;
            String wkt = null;
            int size = 4;
            float opacity = 0.6f;
            int colour = 0xff0000;
            String pointtype = "auto";
            String bb = null;
            int count = 0;
            boolean allTermFound = false;
            Double lat = null;
            Double lon = null;
            Double radius = null;
            String colourBy = null;
            String savedsession = "";
            boolean[] geospatialKosher = null;
            boolean supportDynamic = false;
            if (userParams != null) {
                for (int i = 0; i < userParams.size(); i++) {
                    String key = userParams.get(i).getKey();
                    String value = userParams.get(i).getValue();

                    if (key.equals("wmscache")) {
                        useSpeciesWMSCache = value;
                    }

                    if (key.equals("species_lsid")) {
                        sb.append("lsid:").append(value);
                        count++;
                    } else if (key.equals("q") || key.equals("fq")) {
                        if (value.equals("*:*")) {
                            allTermFound = true;
                            continue;
                        }
                        if (sb.length() > 0) {
                            sb.append(" AND ");
                        }
                        //wrap value in " when appropriate
                        //Not key:[..
                        //Not key:*
                        //Not key:"...
                        //Not key:... AND key:....
                        //Not key:... OR key:...
                        //Not key:.."..
                        int p = value.indexOf(':');
                        if (p > 0 && p + 1 < value.length()
                                && value.charAt(p + 1) != '['
                                && value.charAt(p + 1) != '*'
                                && value.charAt(p + 1) != '"'
                                && !value.contains(" AND ")
                                && !value.contains(" OR ")
                                && !value.contains("\"")
                                && !value.startsWith("lsid")
                                && !value.startsWith("(")) {
                            value = value.substring(0, p + 1) + "\""
                                    + value.substring(p + 1) + "\"";
                        }

                        //flag geospatialKosher filters separately
                        boolean[] gk = null;
                        if ((gk = BiocacheQuery.parseGeospatialKosher(value)) != null) {
                            geospatialKosher = gk;
                        } else {
                            //TODO: remove this when biocache is working
                            if (value.contains("species_guid:")) {
                                value = value.replace("\"", "");
                            }

                            count++;
                            //can't put extra brackets around "not" operators this will prevent SOLR from returning results
                            if (value.startsWith("-")) {
                                sb.append(value);
                            } else {
                                sb.append("(").append(value).append(")");
                            }
                        }
                    } else if (key.equals("qc")) {
                        qc = "&qc=" + URLEncoder.encode(value, "UTF-8");
                    } else if (key.equals("bs")) {
                        bs = value;
                    } else if (key.equals("ws")) {
                        ws = value;
                    } else if (key.equals("wkt")) {
                        wkt = value;
                    } else if (key.equals("psize")) {
                        size = Integer.parseInt(value);
                    } else if (key.equals("popacity")) {
                        opacity = Float.parseFloat(value);
                    } else if (key.equals("pcolour")) {
                        colour = Integer.parseInt(value, 16);
                    } else if (key.equals("ptype")) {
                        pointtype = value;
                    } else if (key.equals("bbox")) {
                        bb = value;
                    } else if (key.equals("lat")) {
                        lat = Double.parseDouble(value);
                    } else if (key.equals("lon")) {
                        lon = Double.parseDouble(value);
                    } else if (key.equals("radius")) {
                        radius = Double.parseDouble(value);
                    } else if (key.equals("ss")) {
                        savedsession = value.trim();
                    } else if (key.equals("dynamic")) {
                        supportDynamic = Boolean.parseBoolean(value);
                    } else if (key.equals("cm")) {
                        colourBy = value.trim();
                    }
                }

                if (lat != null && lon != null && radius != null) {
                    wkt = Util.createCircleJs(lon, lat, radius * 1000); //m to km
                }

                if (count == 1) {
                    //remove brackets
                    String s = sb.toString();
                    sb = new StringBuilder();
                    sb.append(s.substring(1, s.length() - 1));
                } else if (count == 0 && allTermFound) {
                    sb.append("*:*");
                }

                if (StringUtils.isNotBlank(savedsession)) {
                    loadUserSession(savedsession);
                } else {
                    logger.debug("No saved session to load");
                }

                logger.debug("url query: " + sb.toString());
                if (sb.toString().length() > 0) {

                    BiocacheQuery q = new BiocacheQuery(null, wkt, sb.toString(), null, true, geospatialKosher, bs, ws, supportDynamic);

                    if (qc != null) {
                        q.setQc(qc);
                    }

                    if (getMapLayerDisplayName(q.getSolrName()) == null) {
                        if (bb == null) {
                            List<Double> bbox = q.getBBox();
                            String script = "map.zoomToExtent(new OpenLayers.Bounds("
                                    + bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + "," + bbox.get(3) + ")"
                                    + ".transform("
                                    + "  new OpenLayers.Projection('EPSG:4326'),"
                                    + "  map.getProjectionObject()), true);";
                            openLayersJavascript.setAdditionalScript(script);
                        }
                        //mappable attributes
                        int setGrid = -1;
                        if (pointtype.equals("grid")) {
                            setGrid = 1;
                        } else if (pointtype.equals("point")) {
                            setGrid = 0;
                        }
                        return mapSpecies(q, q.getSolrName(), "species", q.getOccurrenceCount(), LayerUtilities.SPECIES, null, setGrid, size, opacity, colour, colourBy, true);
                    }
                }

                mapMultiQueryLayers(geospatialKosher, bs, ws, supportDynamic);
            }
        } catch (Exception e) {
            logger.debug("Error loading url parameters: " + params, e);
        }

        //load any deep linked layers
        mapLayerFromParams();

        //load any deep linked objects
        mapObjectFromParams();

        return null;
    }

    public void onActivateLink(ForwardEvent event) {
        Component component = event.getOrigin().getTarget();
        Object oLink = component.getAttribute("link");
        if (oLink.getClass().getName().equals("java.lang.String")) {
            String uri = (String) oLink;
            String label = (String) component.getAttribute("label");
            this.activateLink(uri, label, false);
        }
    }

    public MapLayer addPointsOfInterestLayer(String wkt, String label, String displayName) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), label) == null) {
                mapLayer = remoteMap.createWKTLayer(wkt, label);
                mapLayer.setDisplayName(displayName);
                mapLayer.setPointsOfInterestWS(CommonData.layersServer + "/intersect/poi/wkt");
                if (mapLayer == null) {
                    // fail
                    showMessage("No mappable features available");
                    logger.debug("adding WKT layer failed ");
                } else {
                    mapLayer.setOpacity((float) 0.4);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);
                }
            } else {
                // fail
                showMessage("WKT layer already exists");
                logger.debug(
                        "refusing to add a new layer with name " + label
                                + " because it already exists in the menu");
            }
        }

        return mapLayer;
    }

    public MapLayer addWKTLayer(String wkt, String label, String displayName) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            if (portalSessionUtilities.getUserDefinedById(getPortalSession(), label) == null) {
                mapLayer = remoteMap.createWKTLayer(wkt, label);
                mapLayer.setDisplayName(displayName);
                if (mapLayer == null) {
                    // fail
                    showMessage("No mappable features available");
                    logger.debug("adding WKT layer failed ");
                } else {
                    mapLayer.setOpacity((float) 0.4);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);
                }
            } else {
                // fail
                showMessage("WKT layer already exists");
                logger.debug(
                        "refusing to add a new layer with name " + label
                                + " because it already exists in the menu");
            }
        }

        return mapLayer;
    }

    public MapLayer addGeoJSON(String labelValue, String uriValue) {
        return this.addGeoJSONLayer(labelValue, uriValue, "", false, false);
    }

    public MapLayer addGeoJSONLayer(String label, String uri, String params, boolean forceReload, boolean points_type) {
        MapLayer mapLayer = null;

        if (safeToPerformMapAction()) {
            MapLayer gjLayer = getMapLayer(label);
            if (forceReload) {
                if (gjLayer != null) {
                    logger.debug("removing existing layer: " + gjLayer.getName());
                    openLayersJavascript.setAdditionalScript(
                            openLayersJavascript.removeMapLayer(gjLayer));
                } //else {
                mapLayer = remoteMap.createGeoJSONLayer(label, uri, points_type, Util.nextColour());
                if (mapLayer == null) {
                    // fail
                    //hide error, might be clustering zoom in; showMessage("No mappable features available");
                    logger.debug("adding GEOJSON layer failed ");
                } else {
                    mapLayer.setOpacity((float) 0.6);
                    mapLayer.setDynamicStyle(true);

                    activateLayer(mapLayer, true, true);
                }
            } else {
                boolean okToAdd = false;
                MapLayer mlExisting = getMapLayer(label);
                if (mlExisting == null) {
                    okToAdd = true;
                } else if (!mlExisting.getUri().equals(uri)) {
                    // check if it's a different url
                    // if it is, then it is ok to add
                    // and assume the previous is removed.
                    okToAdd = true;
                } else {
                }

                if (okToAdd) {
                    mapLayer = remoteMap.createGeoJSONLayer(label, uri, points_type, activeLayerMapProperties, Util.nextColour());
                    if (mapLayer == null) {
                        // fail
                        //hide error, might be clustering zoom in; showMessage("No mappable features available");
                        logger.debug("adding GEOJSON layer failed ");
                    } else {
                        mapLayer.setOpacity((float) 0.6);
                        mapLayer.setDynamicStyle(true);

                        activateLayer(mapLayer, true, true);
                    }
                } else {
                    //need to cleanup any additional scripts outstanding
                    openLayersJavascript.useAdditionalScript();

                    // fail
                    logger.debug(
                            "refusing to add a new layer with URI " + uri
                                    + " because it already exists in the menu");
                }
            }
        }

        return mapLayer;
    }

    /**
     * Destroy session and reload page
     * <p/>
     * - added confirmation message, needs to be event driven since message box
     * always returning '1' with current zk settings.
     */
    public void onClick$reloadPortal() {
        // user confirmation for whole map reset
        try {
            Messagebox.show("Reset map to initial empty state, with no layers and default settings?", "Reset Map",
                    Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new EventListener() {

                @Override
                public void onEvent(Event event) throws Exception {
                    if ((Integer) event.getData() != Messagebox.YES) {
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
            logger.error("error reloading portal", e);
        }
    }

    /**
     * split from button for yes/no event processing
     * <p/>
     * see onClick$reloadPortal()
     */
    void reloadPortal() {
        // grab the portaluser instance so that logged in users stay logged in...
        PortalSession portalSession = getPortalSession();
        //PortalUser portalUser = portalSession.getPortalUser();

        // remove the old species filters
        getSession().removeAttribute("speciesfilters");

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
            Executions.getCurrent().sendRedirect(CommonData.webportalServer + "/");
        }
    }

    /**
     * Reload all species layers
     */
    public void onReloadLayers(Event event) {
        String tbxReloadLayers;

        if (event == null) {
            mapZoomLevel = getLeftmenuSearchComposer().getZoom();

            tbxReloadLayers = (new StringBuffer()).append("z=").append(String.valueOf(mapZoomLevel)).append("&amp;b=").append(getLeftmenuSearchComposer().getViewportBoundingBox().toString()).toString();
        } else {
            tbxReloadLayers = (String) event.getData();

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

        }
        logger.debug("tbxReloadLayers.getValue(): " + tbxReloadLayers);
        // iterate thru' active map layers
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        Vector<String> processedLayers = new Vector();
        String reloadScript = "";
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            if (processedLayers.contains(ml.getName())) {
                logger.debug(ml.getName() + " already processed.");
                continue;
            }
            logger.debug("checking reload layer: " + ml.getName() + " - " + ml.getId() + " - " + ml.getNameJS() + " -> type: " + ml.getType() + "," + ml.getGeometryType());

            processedLayers.add(ml.getName());
        }

        if (reloadScript.length() > 0) {
            openLayersJavascript.execute(
                    OpenLayersJavascript.iFrameReferences
                            + reloadScript);
        }
    }

    /**
     * generate pdf for from innerHTML of printHack txtbox
     * <p/>
     * dependant on whatever the ends up being on the map tab
     * <p/>
     * uses wkhtmltoimage (wkhtmltopdf not working?)
     */
    public void onClick$onPrint(Event event) {
        if (getFellowIfAny("printingwindow") != null) {
            getFellowIfAny("printingwindow").detach();
        }
        tbxPrintHack = (String) event.getData();
        PrintingComposer composer = (PrintingComposer) Executions.createComponents("/WEB-INF/zul/Printing.zul", this, null);
        try {
            composer.doModal();
        } catch (Exception e) {
            logger.error("Error opening map printing dialog", e);
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
                logger.error("error firing layer loaded listener", ex);
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


    /**
     * Maximise map display area - currently just hides the left
     * menumapNavigationTabContent
     */
    void maximise() {
        boolean maximise = getPortalSession().isMaximised();

        if (maximise) {
            menus.setWidth(settingsSupplementary.getValue(MENU_MINIMISED_WIDTH));
            menus.setBorder("none");
            menus.setStyle("overflow: hidden");
        } else {
            menus.setWidth(settingsSupplementary.getValue(MENU_DEFAULT_WIDTH));
            menus.setBorder("normal");
            menus.setStyle("overflow: auto");
        }
        westContent.setVisible(!maximise);
        westMinimised.setVisible(maximise);
        menus.setSplittable(!maximise);
    }

    public MapLayer mapPointsOfInterest(String wkt, String label, String displayName) {
        MapLayer ml = addPointsOfInterestLayer(wkt, label, displayName);
        return ml;
    }

    /**
     * gets a species map that doesn't have colourby set
     */
    public MapLayer mapSpecies(Query sq, String species, String rank, int count, int subType, String wkt, int setGrid, int size, float opacity, int colour) {
        return mapSpecies(sq, species, rank, count, subType, wkt, setGrid, size, opacity, colour, null, false);
    }

    public MapLayer mapSpecies(Query sq, String species, String rank, int count, int subType, String wkt, int setGrid, int size, float opacity, int colour, String colourBy, Boolean loadExpertLayer) {

        if (species == null) {
            species = sq.getName();
            rank = sq.getRank();
        }

        if (count < 0) {
            count = sq.getOccurrenceCount();
        }

        //use # of points cutoff;
        boolean grid;
        if (setGrid == 0) {
            grid = false;
        } else if (setGrid == 1) {
            grid = true;
        } else {
            grid = sq.getOccurrenceCount() > settingsSupplementary.getValueAsInt(POINTS_CLUSTER_THRESHOLD);
        }
        MapLayer ml = mapSpeciesFilter(sq, species, rank, count, subType, wkt, grid, size, opacity, colour);

        if (ml != null) {
            if (colourBy != null) {
                ml.setColourMode(colourBy);
            }
            ml.getMapLayerMetadata().setOccurrencesCount(count);  //for Active Area mapping

            String layerType = "Species - Search";
            if (species.startsWith("Occurrences in ")) {
                layerType = "Species - Occurrences";
            } else if (species.equals("Species assemblage")) {
                layerType = "Species - Assemblage";
            } else if (species.contains("My facet")) {
                layerType = "Species - Facet";
            }
            if (subType == LayerUtilities.SPECIES_UPLOAD) {
                layerType = "Import - Species";
            }
            if (subType == LayerUtilities.SPECIES && rank.equals("user")) {
                layerType = "Import - LSID";
            }
            if (sq instanceof BiocacheQuery) {
                BiocacheQuery bq = (BiocacheQuery) sq;
                String extra = bq.getWS() + "|" + bq.getBS() + "|" + bq.getFullQ(false);
                remoteLogger.logMapSpecies(ml.getDisplayName(), bq.getLsids(), wkt, layerType, extra);

            } else if (sq instanceof UserDataQuery) {
                remoteLogger.logMapSpecies(ml.getDisplayName(), "user-" + sq.getSpeciesCount() + " records", wkt, layerType, sq.getMetadataHtml());
            } else {
                remoteLogger.logMapSpecies(ml.getDisplayName(), species, wkt, layerType, sq.getMetadataHtml());
            }

            updateLayerControls();
            refreshContextualMenu();
            //changes need to be apply when the colour by is not null this allows the map to be updated to reflect the correct facet
            if (colourBy != null) {
                applyChange(ml);
            }
        }

        return ml;
    }

    void addLsidBoundingBoxToMetadata(MapLayerMetadata md, Query query) {
        //get bounding box for lsid
        List<Double> bb = query.getBBox();

        md.setBbox(bb);
    }

    public String getLayerBoundingBox(MapLayer ml) {
        String bbox = null;

        if (ml.getMapLayerMetadata().getBboxString() != null) {
            bbox = ml.getMapLayerMetadata().getBboxString();
        } else {
            Clients.evalJavaScript("jq('$currentLayerExtent')[0].innerHTML=map.getLayersByName('" + ml.getName() + "')[0].getExtent().transform(map.getProjectionObject(),new OpenLayers.Projection('EPSG:4326')).toString();");
            String bboxstr = currentLayerExtent.getValue();
            logger.debug("Got bboxstr: " + bboxstr);
            if (bboxstr != null) {
                String[] b = bboxstr.split(",");
                ArrayList<Double> bb = new ArrayList<Double>();
                bb.add(Double.parseDouble(b[0]));
                bb.add(Double.parseDouble(b[1]));
                bb.add(Double.parseDouble(b[2]));
                bb.add(Double.parseDouble(b[3]));

                ml.getMapLayerMetadata().setBbox(bb);

                bbox = bboxstr;
            }
        }

        return bbox;
    }

    private void loadDistributionMap(String lsids, String taxon, String wkt) {
        if (wkt != null && CommonData.WORLD_WKT.equals(wkt)) {
            wkt = null;
        }
        //test for a valid lsid match
        String[] wmsNames = CommonData.getSpeciesDistributionWMS(lsids);
        String[] metadata = CommonData.getSpeciesDistributionMetadata(lsids);
        String[] spcode = CommonData.getSpeciesDistributionSpcode(lsids);
        MapLayer ml = null;
        if (wmsNames != null && wmsNames.length > 0 && (wkt == null || wkt.equals(CommonData.WORLD_WKT))) {
            //add all
            for (int i = 0; i < wmsNames.length; i++) {
                if (getMapLayerWMS(wmsNames[i]) == null) {
                    String layerName = getNextAreaLayerName(taxon);
                    String html = DistributionsController.getMetadataHtmlFor(spcode[i], null, layerName);
                    ml = addWMSLayer(layerName, "Expert distribution: " + taxon, wmsNames[i], 0.35f, html, null, LayerUtilities.WKT, null, null);
                    ml.setSPCode(spcode[i]);
                    setupMapLayerAsDistributionArea(ml);
                }
            }
        } else if (wmsNames != null && wmsNames.length > 0 && wkt != null && !wkt.equals(CommonData.WORLD_WKT)) {
            String url = CommonData.layersServer + "/distributions";
            try {
                HttpClient client = new HttpClient();
                PostMethod post = new PostMethod(url); // testurl
                post.addParameter("wkt", wkt);
                post.addParameter("lsids", lsids);
                post.addRequestHeader("Accept", "application/json, text/javascript, */*");
                int result = client.executeMethod(post);
                if (result == 200) {
                    String txt = post.getResponseBodyAsString();
                    JSONArray ja = JSONArray.fromObject(txt);
                    ArrayList<String> found = new ArrayList();
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = ja.getJSONObject((i));
                        if (jo.containsKey("wmsurl")) {
                            found.add(jo.getString("wmsurl"));
                        }
                    }
                    for (int i = 0; i < wmsNames.length; i++) {
                        if (getMapLayerWMS(wmsNames[i]) == null) {
                            String layerName = getNextAreaLayerName(taxon + " area " + (i + 1));
                            String html = DistributionsController.getMetadataHtmlFor(spcode[i], null, layerName);
                            ml = addWMSLayer(layerName, "Expert distribution: " + taxon, found.get(i), 0.35f, html, null, LayerUtilities.WKT, null, null);
                            ml.setSPCode(spcode[i]);
                            setupMapLayerAsDistributionArea(ml);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("error posting distributions: " + url);
            }
        }

        openChecklistSpecies(lsids, wkt, true);
    }

    void openChecklistSpecies(String lsids, String wkt, boolean mapIfOnlyOne) {
        try {
            //species checklists
            String[] finallist = AreaReportController.getDistributionsOrChecklists("checklists", wkt, lsids, null);

            //open for optional mapping of areas
            if (finallist != null && finallist.length > 1) {
                if (mapIfOnlyOne && finallist.length == 2) {
                    try {
                        String[] row = null;
                        CSVReader csv = new CSVReader(new StringReader(finallist[1]));
                        row = csv.readNext();
                        csv.close();
                        if (getMapLayerWMS(CommonData.getSpeciesChecklistWMSFromSpcode(row[0])[1]) == null) {
                            //map it
                            String[] mapping = CommonData.getSpeciesChecklistWMSFromSpcode(row[0]);
                            String displayName = mapping[0] + " area";
                            if (row[11] != null && row[11].length() > 0) {//&& !row[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME)) {
                                displayName = row[11];
                            }

                            String layerName = getNextAreaLayerName(row[0] + " area");
                            String html = DistributionsController.getMetadataHtmlFor(row[0], row, layerName);

                            MapLayer ml = getMapComposer().addWMSLayer(layerName, displayName, mapping[1], 0.6f, html, null, LayerUtilities.WKT, null, null);
                            ml.setSPCode(row[0]);
                            MapComposer.setupMapLayerAsDistributionArea(ml);
                        }
                    } catch (Exception e) {
                        logger.error("error opening checklist species", e);
                    }
                } else {
                    try {
                        getFellowIfAny("distributionresults").detach();
                    } catch (Exception e) {

                    }
                    DistributionsController window = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

                    try {
                        window.doModal();
                        window.init(finallist, "Checklist species", String.valueOf(finallist.length - 1), null);
                    } catch (Exception e) {
                        logger.error("error opening checklist species dialog", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("error opening distribution area dialog", e);
        }
    }

    void openAreaChecklist(String geom_idx, String lsids, String wkt) {
        try {
            //checklist species
            String[] finallist = AreaReportController.getDistributionsOrChecklists("checklists", wkt, lsids, geom_idx);

            try {
                getFellowIfAny("distributionresults").detach();
            } catch (Exception e) {
                //make sure distributionresults is closed.  will fail if it is not open.
            }
            DistributionsController window = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

            try {
                window.doModal();
                window.init(finallist, "Checklist species", String.valueOf(finallist.length - 1), null);
            } catch (Exception e) {
                logger.error("error opening checklist species dialog", e);
            }
        } catch (Exception e) {
            logger.error("error opening distribution area dialog", e);
        }
    }

    void openDistributionSpecies(String lsids, String wkt) {
        try {
            //expert distributions
            String[] distributions = AreaReportController.getDistributionsOrChecklists("distributions", wkt, lsids, null);

            //open for optional mapping of areas
            if (distributions != null) {
                try {
                    getFellowIfAny("distributionresults").detach();
                } catch (Exception e) {
                }

                DistributionsController window = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

                try {
                    window.doModal();
                    window.init(distributions, "Expert distributions", String.valueOf(distributions.length - 1), null);
                } catch (Exception e) {
                    logger.error("error opening analysisdistributionresults.zul", e);
                }
            }

        } catch (Exception e) {
            logger.error("error opening distribution species", e);
        }
    }

    public static void setupMapLayerAsDistributionArea(MapLayer mapLayer) {
        try {
            //identify the spcode from the url     
            String spcode = mapLayer.getSPCode();
            String url = CommonData.layersServer + "/distribution/" + spcode;
            String jsontxt = Util.readUrl(url);
            if (jsontxt == null || jsontxt.length() == 0) {
                url = CommonData.layersServer + "/checklist/" + spcode;
                jsontxt = Util.readUrl(url);
            }
            if (jsontxt == null || jsontxt.length() == 0) {
                logger.debug("******** failed to find wkt for " + mapLayer.getUri() + " > " + spcode);
                return;
            }
            JSONObject jo = JSONObject.fromObject(jsontxt);
            if (!jo.containsKey("geometry")) {
                return;
            }
            mapLayer.setWKT(jo.getString("geometry"));
            mapLayer.setPolygonLayer(true);

            Facet facet = null;
            if (jo.containsKey("pid") && jo.containsKey("area_name")) {
                facet = Util.getFacetForObject(jo, jo.getString("area_name"));
            }
            if (facet != null) {
                ArrayList<Facet> facets = new ArrayList<Facet>();
                facets.add(facet);
                mapLayer.setFacets(facets);
            }
            MapLayerMetadata md = mapLayer.getMapLayerMetadata();

            try {
                double[][] bb = SimpleShapeFile.parseWKT(jo.getString("geometry")).getBoundingBox();
                ArrayList<Double> bbox = new ArrayList<Double>();
                bbox.add(bb[0][0]);
                bbox.add(bb[0][1]);
                bbox.add(bb[1][0]);
                bbox.add(bb[1][1]);
                md.setBbox(bbox);
            } catch (Exception e) {
                logger.error("failed to parse wkt in : " + url, e);
            }

            //add colour!
            mapLayer.setRedVal(255);
            mapLayer.setGreenVal(0);
            mapLayer.setBlueVal(0);

        } catch (Exception e) {
            logger.error("error setting up distributions map layer", e);
        }
    }

    MapLayer mapSpeciesFilter(Query q, String species, String rank, int count, int subType, String wkt, boolean grid, int size, float opacity, int colour) {
        String filter = q.getQ();

        if (q instanceof BiocacheQuery) {
            String lsids = ((BiocacheQuery) q).getLsids();
            List<String> extraLsids = ((BiocacheQuery) q).getLsidFromExtraParams();
            if (lsids != null && lsids.length() > 0) {
                loadDistributionMap(lsids, species, wkt);
            }
            for (String extraLsid : extraLsids) {
                logger.debug("loading layer for: " + extraLsid);
                loadDistributionMap(extraLsid, species, wkt);
            }
        }

        MapLayer ml = mapSpeciesWMSByFilter(getNextAreaLayerName(species), filter, subType, q, grid, size, opacity, colour);

        if (ml != null) {
            addToSession(ml.getName(), filter);
            MapLayerMetadata md = ml.getMapLayerMetadata();

            md.setOccurrencesCount(count);

            ml.setClustered(false);

            if (grid) {
                ml.setColourMode("grid");
            }

            addLsidBoundingBoxToMetadata(md, q);
        }

        return ml;
    }

    MapLayer mapSpeciesWMSByFilter(String label, String filter, int subType, Query query, boolean grid, int size, float opacity, int colour) {
        String uri;

        int r = (colour >> 16) & 0x000000ff;
        int g = (colour >> 8) & 0x000000ff;
        int b = (colour) & 0x000000ff;

        int uncertaintyCheck = 0; //0 == false default

        if (activeLayerMapProperties != null) {
            r = (Integer) activeLayerMapProperties.get("red");
            b = (Integer) activeLayerMapProperties.get("blue");
            g = (Integer) activeLayerMapProperties.get("green");
            size = (Integer) activeLayerMapProperties.get("size");
            opacity = (Float) activeLayerMapProperties.get("opacity");
            uncertaintyCheck = (Integer) activeLayerMapProperties.get("uncertainty");
        }

        if (subType == LayerUtilities.SCATTERPLOT) {
            //set defaults for scatterplot
            r = 0;
            g = 0;
            b = 255;
            opacity = 1;
            size = 4;
        }

        Color c = new Color(r, g, b);
        String hexColour = Integer.toHexString(c.getRGB() & 0x00ffffff);
        String envString = "";
        if (grid) {
            //colour mode is in 'filter' but need to move it to envString
            envString += "colormode:grid";
        } else {
            envString = "color:" + hexColour;
        }
        envString += ";name:circle;size:" + size + ";opacity:1";// + opacity;
        if (uncertaintyCheck > 0) {
            envString += ";uncertainty:1";
        }

        uri = query.getUrl();
        uri += "service=WMS&version=1.1.0&request=GetMap&styles=&format=image/png";
        uri += "&layers=ALA:occurrences";
        uri += "&transparent=true"; // "&env=" + envString +
        uri += (query.getQc() == null ? "" : query.getQc());
        uri += "&CACHE=" + useSpeciesWMSCache;
        uri += "&CQL_FILTER=";

        logger.debug("Mapping: " + label + " with " + uri + filter);

        try {
            if (safeToPerformMapAction()) {
                if (getMapLayer(label) == null) {
                    MapLayer ml = addWMSLayer(label, label, uri + filter, opacity, null, null, subType, "", envString, query);
                    if (ml != null) {
                        ml.setDynamicStyle(true);
                        ml.setEnvParams(envString);
                        ml.setGeometryType(GeoJSONUtilities.POINT); // for the sizechooser

                        ml.setBlueVal(b);
                        ml.setGreenVal(g);
                        ml.setRedVal(r);
                        ml.setSizeVal(3);
                        ml.setOpacity(opacity);

                        ml.setClustered(false);

                        MapLayerMetadata md = ml.getMapLayerMetadata();

                        ml.setSpeciesQuery(query);

                        updateLayerControls();

                        //create highlight layer
                        MapLayer mlHighlight = (MapLayer) ml.clone();
                        mlHighlight.setName(ml.getName() + "_highlight");
                        ml.addChild(mlHighlight);

                        return ml;
                    } else {
                        // fail
                        //hide error, might be clustering zoom in;  showMessage("No mappable features available");
                        logger.debug("adding WMS layer failed ");
                    }
                } else {
                    //need to cleanup any additional scripts outstanding
                    openLayersJavascript.useAdditionalScript();

                    // fail
                    //showMessage("GeoJSON layer already exists");
                    logger.debug(
                            "refusing to add a new layer with URI " + uri
                                    + " because it already exists in the menu");
                }
            }
        } catch (Exception ex) {
            logger.error("error mapSpeciesByNameRank:", ex);
        }
        return null;
    }

    public LeftMenuSearchComposer getLeftmenuSearchComposer() {
        return (LeftMenuSearchComposer) getFellow("leftMenuSearch").getFellow("leftSearch");
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
        logger.debug("tbxPrintHack:" + p);
        String[] ps = p.split(",");

        String server;
        server = settingsSupplementary.getValue("print_server_url");

        String jsessionid = getCookieValue("JSESSIONID");
        if (jsessionid == null) {
            jsessionid = "";
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
            SessionPrint pp = new SessionPrint(server, height, width, htmlpth, htmlurl, uid, jsessionid, zoom, header, grid, format, resolution);

            if (!preview) {
                pp.print();

                File f = new File(pp.getImageFilename());
                logger.debug("img (" + pp.getImageFilename() + ") exists: " + f.exists());

                if (format.equalsIgnoreCase("png")) {
                    Filedownload.save(new File(pp.getImageFilename()), "image/png");
                } else if (format.equalsIgnoreCase("pdf")) {
                    Filedownload.save(new File(pp.getImageFilename()), "application/pdf");
                } else {
                    Filedownload.save(new File(pp.getImageFilename()), "image/jpeg");
                }
            }

            remoteLogger.logMapAnalysis(header, "Export - Map", zoom, "", "", "", "header: " + header + "|" + "grid: " + grid + "|" + "format: " + format + "|" + "resolution: " + resolution + "|" + "preview: " + preview + "|" + "downloadfile: " + pp.getImageFilename(), "PRINTED");

            return pp;
        } catch (Exception e) {
            logger.error("error preparing printing", e);
        }

        showMessage("Error generating export");

        return null;
    }

    /*
     * for Events.echoEvent("openUrl",mapComposer,url as string)
     */
    public void openUrl(Event event) {
        String s = (String) event.getData();

        logger.debug("\n\n******\n\ns: " + s + "\n\n******\n\n");

        String url = "";
        String header = "";
        String download = "";
        String[] data = s.split("\n");
        if (!ArrayUtils.isEmpty(data)) {

            logger.debug("data.length: " + data.length);

            if (data.length == 1) {
                url = data[0];
            }
            if (data.length == 2) {
                url = data[0];
                header = data[1];

                // now the 'header' might be a 'header' or 'download link'
                if (header.startsWith("pid:")) {
                    download = header;
                    header = "";
                }
            }
            if (data.length == 3) {
                url = data[0];
                header = data[1];
                download = data[2];
            }

            if (download.length() > 0) {
                if (download.startsWith("pid")) {
                    download = download.substring(4);
                }
            }

            activateLink(url, header, false, download);
        }

    }

    /*
     * for Events.echoEvent("openHTML",mapComposer,url as string)
     *
     * first line of String input is header
     */
    public void openHTML(Event event) {
        String html = (String) event.getData();
        String[] lines = html.split("\n");
        String header = lines[0];
        String url = "*" + html.substring(header.length()).trim();
        activateLink(url, header, false);
    }

    public String getViewArea() {
        //default to: current view to be dynamically returned on usage
        BoundingBox bb = getMapComposer().getLeftmenuSearchComposer().getViewportBoundingBox();

        //NC 20130319: Change the order of the rectangle polygon so that it is going in the correct direction for SOLR 
        String wkt = "POLYGON(("
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMinLatitude() + ","
                + bb.getMaxLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMaxLatitude() + ","
                + bb.getMinLongitude() + " " + bb.getMinLatitude() + "))";

        return wkt;
    }

    public Session getSession() {
        return Sessions.getCurrent();
    }

    public int getMapZoom() {
        return mapZoomLevel;
    }

    public void addToSession(String species, String filter) {
        Map speciesfilters = (Map) getSession().getAttribute("speciesfilters");
        if (speciesfilters == null) {
            speciesfilters = new HashMap();
        }
        speciesfilters.put(species, filter);
        getSession().setAttribute("speciesfilters", speciesfilters);
    }

    public void removeFromSession(String species) {
        Map speciesfilters = (Map) Sessions.getCurrent().getAttribute("speciesfilters");
        if (speciesfilters != null) {
            speciesfilters.remove(species);
            getSession().setAttribute("speciesfilters", speciesfilters);
        }
    }

    public void loadScatterplot(ScatterplotData data, String lyrName) {
        MapLayer ml = mapSpecies(data.getQuery(), data.getSpeciesName(), "species", 0, LayerUtilities.SCATTERPLOT, null,
                0, DEFAULT_POINT_SIZE, DEFAULT_POINT_OPACITY, Util.nextColour());
        ml.setDisplayName(lyrName);
        ml.setSubType(LayerUtilities.SCATTERPLOT);
        ml.setScatterplotData(data);
        addUserDefinedLayerToMenu(ml, true);
        updateLayerControls();
        refreshContextualMenu();
    }

    /*
     * remove it + map it
     */
    public void removeLayerHighlight(ScatterplotData data, String rank) {
        List udl = getMapComposer().getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        MapLayer mapLayer = null;
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            MapLayerMetadata md = ml.getMapLayerMetadata();
            if (ml.isSpeciesLayer()
                    && ml.getSpeciesQuery().toString().equals(data.getQuery().toString())
                    && ml.getHighlight() != null) {

                ml.setHighlight(null);

                if (!ml.isClustered() && ml.isDisplayed()) {
                    applyChange(ml);
                }

                break;
            }
        }
    }

    public String getNextAreaLayerName(String layerPrefix) {
        if (getMapLayer(layerPrefix) == null && getMapLayerDisplayName(layerPrefix) == null) {
            return layerPrefix;
        }

        layerPrefix += " ";
        int i = 1;
        while (getMapLayer(layerPrefix + i) != null
                || getMapLayerDisplayName(layerPrefix + i) != null) {
            i++;
        }
        return layerPrefix + i;
    }

    public String getNextActiveAreaLayerName(String areaName) {
        if (areaName == null) {
            areaName = "Active area";
        } else if (areaName.trim().equals("")) {
            areaName = "Active area";
        }
        String layerPrefix = "Occurrences in " + areaName + " ";

        return layerPrefix;
    }

    public void onClick$btnAddSpecies(Event event) {
        openModal("WEB-INF/zul/add/AddSpecies.zul", null, "addspecieswindow");
    }

    public void onClick$btnAddArea(Event event) {
        openModal("WEB-INF/zul/add/AddArea.zul", null, "addareawindow");
    }

    public void onClick$btnAddLayer(Event event) {
        openModal("WEB-INF/zul/add/AddLayer.zul", null, "addlayerwindow");
    }

    public void onClick$btnAddFacet(Event event) {
        openModal("WEB-INF/zul/add/AddFacet.zul", null, "addfacetwindow");
    }

    public void onClick$btnAddWMSLayer(Event event) {
        openModal("WEB-INF/zul/add/AddWMSLayer.zul", null, "addLayerWindow");
    }

    public void toggleLayers(Event event) {
        Checkbox checkbox = (Checkbox) event.getTarget();
        boolean checked = checkbox.isChecked();
        Iterator it2 = activeLayersList.getItems().iterator();
        while (it2.hasNext()) {
            Listitem li = (Listitem) it2.next();
            MapLayer ml = li.getValue();
            if (ml.getType() != LayerUtilities.MAP) {
                toggleLayer(li, checked);
            }
        }
    }

    public void onClick$btnAddGDM(Event event) {
        openModal("WEB-INF/zul/tool/GDM.zul", null, "addtoolwindow");
    }

    public void onClick$btnAddMaxent(Event event) {
        openModal("WEB-INF/zul/tool/Maxent.zul", null, "addtoolwindow");
    }

    public void onClick$btnAddSampling(Event event) {
        openModal("WEB-INF/zul/tool/Sampling.zul", null, "addtoolwindow");
    }

    public void onClick$btnAddAloc(Event event) {
        openModal("WEB-INF/zul/tool/ALOC.zul", null, "addtoolwindow");
    }

    public void onClick$btnAddScatterplot(Event event) {
        openModal("WEB-INF/zul/tool/Scatterplot.zul", null, "addtoolwindow");
    }

    public void onClick$btnAddScatterplotList(Event event) {
        openModal("WEB-INF/zul/tool/ScatterplotList.zul", null, "addtoolwindow");
    }

    public void runTabulation(Event event) {
        openModal("WEB-INF/zul/tool/Tabulation.zul", null, "addtoolwindow");
    }

    public void onClick$btnAreaReport(Event event) {
        openModal("WEB-INF/zul/tool/AreaReport.zul", null, "addtoolwindow");
    }

    public void runNearestLocalityAction(Event event) {
        remoteLogger.logMapAnalysis("Nearest locality", "Tool - Nearest locality", "", "", "", "", "", "");
    }

    public void onClick$btnSpeciesList(Event event) {
        openModal("WEB-INF/zul/tool/SpeciesList.zul", null, "addtoolwindow");
    }

    public void onClick$btnSitesBySpecies(Event event) {
        openModal("WEB-INF/zul/tool/SitesBySpecies.zul", null, "addtoolwindow");
    }

    public Window openModal(String page, Hashtable<String, Object> params, String windowname) {
        //remove any existing window with the same name (bug somewhere?)
        if (windowname != null) {
            if (getFellowIfAny(windowname) != null) {
                getFellowIfAny(windowname).detach();
            }
        }

        Window window = (Window) Executions.createComponents(page, this, params);

        try {
            window.doModal();
        } catch (Exception e) {
            logger.error("error opening dialog: " + page, e);
        }
        return window;
    }

    void openOverlapped(String page) {
        Window window = (Window) Executions.createComponents(page, this, null);
        try {
            window.doOverlapped();
        } catch (Exception e) {
            logger.error("error opening overlapped dialog: " + page, e);
        }
    }

    public void updateLayerControls() {
        adjustActiveLayersList();

        //remove any scatterplot legend
        Component c = getFellowIfAny("scatterplotlayerlegend");
        if (c != null) {
            c.detach();
        }

        //remove children
        for (int i = layerControls.getChildren().size() - 1; i >= 0; i--) {
            try {
                layerControls.getChildren().get(i).detach();
            } catch (Exception e) {
            }
        }

        MapLayer selectedLayer = this.getActiveLayersSelection(false);
        if (selectedLayer == null) {
            if (activeLayersList.getItemCount() > 0) {
                activeLayersList.setSelectedIndex(0);
                selectedLayer = (MapLayer) activeLayersList.getModel().getElementAt(0);
            } else {
                return;
            }
        }

        lblSelectedLayer.setValue(selectedLayer.getDisplayName());

        String page = "";
        Window window = null;
        if (selectedLayer.getType() == LayerUtilities.SCATTERPLOT) {
            page = "WEB-INF/zul/legend/LayerLegendScatterplot.zul";
        } else if (selectedLayer.getType() == LayerUtilities.MAP) {
            page = "WEB-INF/zul/legend/MapOptions.zul";
        } else {
            llc2MapLayer = selectedLayer;
            llc2 = (LayerLegendGeneralComposer) Executions.createComponents("WEB-INF/zul/legend/LayerLegendGeneral.zul", layerControls, null);
            llc2.init(
                    llc2MapLayer,
                    llc2MapLayer.getSpeciesQuery(),
                    llc2MapLayer.getRedVal(),
                    llc2MapLayer.getGreenVal(),
                    llc2MapLayer.getBlueVal(),
                    llc2MapLayer.getSizeVal(),
                    (int) (llc2MapLayer.getOpacity() * 100),
                    llc2MapLayer.getColourMode(),
                    (llc2MapLayer.getColourMode().equals("grid")) ? 0 : ((llc2MapLayer.isClustered()) ? 1 : 2),
                    llc2MapLayer.getSizeUncertain(),
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            updateFromLegend();
                        }
                    });

            try {
                llc2.doEmbedded();
            } catch (Exception e) {
                logger.error("error setting up layer legend", e);
            }

            return;
        }

        window = (Window) Executions.createComponents(page, layerControls, null);
        try {
            ((HasMapLayer) window).setMapLayer(selectedLayer);
        } catch (Exception e) {
        }
        try {
            window.doEmbedded();
        } catch (Exception e) {
            logger.error("error setting layer legend", e);
        }
    }

    void updateFromLegend() {
        MapLayer ml = llc2MapLayer;

        //layer on map settings
        if (llc2.getRed() != ml.getRedVal()
                || llc2.getGreen() != ml.getGreenVal()
                || llc2.getBlue() != ml.getBlueVal()
                || llc2.getSize() != ml.getSizeVal()
                || llc2.getOpacity() != (int) (ml.getOpacity() * 100)
                || (ml.getColourMode() != null && !ml.getColourMode().equals(llc2.getColourMode()))
                || (ml.isClustered() && llc2.getPointType() != 1)
                || ml.getSizeUncertain() != llc2.getUncertainty()) {

            ml.setRedVal(llc2.getRed());
            ml.setGreenVal(llc2.getGreen());
            ml.setBlueVal(llc2.getBlue());
            ml.setSizeVal(llc2.getSize());
            ml.setOpacity(llc2.getOpacity() / 100.0f);
            ml.setColourMode(llc2.getColourMode());
            ml.setClustered(llc2.getPointType() == 0);
            ml.setSizeUncertain(llc2.getUncertainty());

            applyChange(ml);
        }

        //layer in menu settings
        if (!ml.getDisplayName().equals(llc2.getDisplayName())) {
            ml.setDisplayName(llc2.getDisplayName());

            //selection label
            lblSelectedLayer.setValue(llc2.getDisplayName());

            redrawLayersList();
        }
    }

    public void redrawLayersList() {
        int idx = activeLayersList.getSelectedIndex();
        List<MapLayer> activeLayers = getPortalSession().getActiveLayers();
        activeLayersList.setModel(new ListModelList(activeLayers, true));
        activeLayersList.setSelectedIndex(idx);
        llc2.txtLayerName.setValue(llc2MapLayer.getDisplayName());
        lblSelectedLayer.setValue(llc2MapLayer.getDisplayName());
        adjustActiveLayersList();
    }

    public void onSelect$activeLayersList(Event event) {
        updateLayerControls();

        refreshContextualMenu();
    }

    public List<MapLayer> getPolygonLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isPolygonLayer()) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getSpeciesLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).getSpeciesQuery() != null) {
                //&& allLayers.get(i).getSubType() != LayerUtilities.SCATTERPLOT) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getGridLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isGridLayer()
                    && allLayers.get(i).getSubType() != LayerUtilities.MAXENT
                    && allLayers.get(i).getSubType() != LayerUtilities.GDM
                    && allLayers.get(i).getSubType() != LayerUtilities.ALOC) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getAnalysisLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).getSubType() == LayerUtilities.MAXENT
                    || allLayers.get(i).getSubType() == LayerUtilities.GDM
                    || allLayers.get(i).getSubType() == LayerUtilities.ALOC
                    || allLayers.get(i).getSubType() == LayerUtilities.ODENSITY
                    || allLayers.get(i).getSubType() == LayerUtilities.SRICHNESS) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public List<MapLayer> getContextualLayers() {
        ArrayList<MapLayer> list = new ArrayList<MapLayer>();
        List<MapLayer> allLayers = getPortalSession().getActiveLayers();
        for (int i = 0; i < allLayers.size(); i++) {
            if (allLayers.get(i).isContextualLayer()
                    && allLayers.get(i).getSubType() != LayerUtilities.ALOC) {
                list.add(allLayers.get(i));
            }
        }

        return list;
    }

    public boolean isSelectedLayer(MapLayer ml) {
        MapLayer selectedLayer = getActiveLayersSelection(false);
        return selectedLayer == ml;
    }

    public void refreshContextualMenu() {
        ((ContextualMenu) contextualMenu.getFellow("contextualMenuWindow")).refresh();
        adjustActiveLayersList();
    }

    /**
     * Searches the occurrences at a given point and then maps the polygon
     * feature found at the location (for the current top contextual layer).
     *
     * @param event triggered by the usual javascript trickery
     */
    public void exportArea(Event event) {
        openModal("WEB-INF/zul/output/ExportLayer.zul", null, "addtoolwindow");
    }

    public void onBaseMap(Event event) {
        String newBaseMap = (String) event.getData();
        getPortalSession().setBaseLayer(newBaseMap);
    }

    public String getBaseMap() {
        return getPortalSession().getBaseLayer();
    }

    public void adjustActiveLayersList() {
        if (activeLayersList != null && activeLayersList.getItems() != null) {
            for (Listitem li : activeLayersList.getItems()) {
                if (li.getValue() != null
                        && ((MapLayer) li.getValue()).getType() == LayerUtilities.MAP) {

                    Listcell lc = (Listcell) li.getLastChild();

                    int checkedCount = 0;
                    for (Listitem i : activeLayersList.getItems()) {
                        if (i.getFirstChild().getFirstChild() != null
                                && ((Checkbox) i.getFirstChild().getFirstChild()).isChecked()) {
                            checkedCount++;
                        }
                    }

                    //update label
                    Button unsel = (Button) lc.getLastChild();
                    Button sel = (Button) unsel.getPreviousSibling();
                    Button remove = (Button) sel.getPreviousSibling();

                    //if all but map options layer is selected, title = 'unselect all'
                    if (activeLayersList.getItemCount() == 1) {
                        sel.setVisible(false);
                        unsel.setVisible(false);
                        remove.setVisible(false);
                    } else if (checkedCount == activeLayersList.getItemCount()) {
                        sel.setDisabled(true);
                        unsel.setDisabled(false);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    } else if (checkedCount == 1) {
                        sel.setDisabled(false);
                        unsel.setDisabled(true);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    } else {
                        sel.setDisabled(false);
                        unsel.setDisabled(false);
                        sel.setVisible(true);
                        unsel.setVisible(true);
                        remove.setVisible(true);
                    }
                }
            }
        }
    }

    public void setLayersVisible(boolean show) {
        PortalSession portalSession = (PortalSession) Executions.getCurrent()
                .getDesktop()
                .getSession()
                .getAttribute("portalSession");

        for (Listitem li : activeLayersList.getItems()) {
            if (li.getValue() == null
                    || !((MapLayer) li.getValue()).getName().equals("Map options")) {

                Checkbox cb = ((Checkbox) li.getFirstChild().getFirstChild());

                if (show && !cb.isChecked()) {
                    openLayersJavascript.execute(
                            OpenLayersJavascript.iFrameReferences
                                    + openLayersJavascript.activateMapLayer((MapLayer) li.getValue(), false, true)
                                    + openLayersJavascript.updateMapLayerIndexes(
                                    portalSession.getActiveLayers()
                            )
                    );
                } else if (!show && cb.isChecked()) {
                    openLayersJavascript.removeMapLayerNow((MapLayer) li.getValue());
                }

                cb.setChecked(show);
                cb.setTooltiptext(show ? "Hide" : "Show");


            }
        }

        refreshContextualMenu();
    }

    public void importAnalysis(Event event) {
        openModal("WEB-INF/zul/input/ImportAnalysis.zul", null, "importanalysis");
    }

    private Map<String, String> getQueryParameterMap(String params) {
        if (params == null || params.length() == 0) {
            return null;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        for (String s : params.split("&")) {
            String[] keyvalue = s.split("=");
            if (keyvalue.length >= 2) {
                String key = keyvalue[0];
                String value = keyvalue[1];
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (Exception e) {
                    logger.error("error decoding to UTF-8: " + value, e);
                }
                map.put(key, value);
            }
        }

        return map;
    }

    public ArrayList<LayerSelection> getLayerSelections() {
        return selectedLayers;
    }

    public void addLayerSelection(LayerSelection ls) {
        //check for duplication
        for (int i = 0; i < selectedLayers.size(); i++) {
            if (selectedLayers.get(i).equalsList(ls)) {
                selectedLayers.get(i).setLastUse(System.currentTimeMillis());
                return;
            }
        }

        //add
        selectedLayers.add(ls);

        //if more than 10, remove the recod with oldest use
        if (selectedLayers.size() > 20) {
            int oldestIdx = 0;
            for (int i = 1; i < selectedLayers.size(); i++) {
                if (selectedLayers.get(i).getLastUse()
                        < selectedLayers.get(oldestIdx).getLastUse()) {
                    oldestIdx = i;
                }
            }
            selectedLayers.remove(oldestIdx);
        }
    }

    public void onClick$openDistributions(Event event) {
        String lsids = (String) event.getData();
        if (lsids != null && lsids.length() > 0) {
            closeExternalContentWindow();
            openDistributionSpecies(lsids, null);
        }
    }

    public void onClick$openChecklists(Event event) {
        String lsids = (String) event.getData();
        if (lsids != null && lsids.length() > 0) {
            closeExternalContentWindow();
            openChecklistSpecies(lsids, null, false);
        }
    }

    public void onClick$openAreaChecklist(Event event) {
        String geom_idx = (String) event.getData();
        if (geom_idx != null && geom_idx.length() > 0) {
            closeExternalContentWindow();
            openAreaChecklist(geom_idx, null, null);
        }
    }

    void closeExternalContentWindow() {
        //close any prevously opened externalcontentwindow
        try {
            Component c = getFellowIfAny("externalContentWindow");
            if (c != null) {
                logger.debug("found externalContentWindow, closing");
                c.detach();
            }
        } catch (Exception e) {
            logger.error("error closing externalContentWindow window", e);
        }
    }

    public Object getMapLayerWMS(String wmsurl) {
        // check if layer already present
        List udl = getPortalSession().getActiveLayers();
        Iterator iudl = udl.iterator();
        while (iudl.hasNext()) {
            MapLayer ml = (MapLayer) iudl.next();
            if (ml.getUri() != null && ml.getUri().equals(wmsurl)) {
                return ml;
            }
        }

        return null;
    }

    /**
     * Species are imported through the species list tool.
     *
     * @param event
     */
    public void importSpecies(Event event) {

        String type = (String) event.getData();
        if (type != null && type.length() > 0) {
            if ("assemblage".equalsIgnoreCase(type)) {
                AddSpeciesController asc = (AddSpeciesController) openModal("WEB-INF/zul/add/AddSpecies.zul", null, "addspecieswindow");
                asc.enableImportAssemblage();
                //usc.setTbInstructions("3. Select the species list to import.");
            } else {
                UploadSpeciesController usc = (UploadSpeciesController) openModal("WEB-INF/zul/input/UploadSpecies.zul", null, "uploadspecieswindow");
                usc.setTbInstructions("3. Select file (comma separated ID (text), longitude (decimal degrees), latitude(decimal degrees))");
                usc.addToMap = true;
            }

        }
    }

    public void importAreas(Event event) {
        openModal("WEB-INF/zul/input/ImportAreas.zul", null, "addareawindow");
    }

    public void saveUserSession(Event event) {
        logger.debug("saving session");

        PrintWriter out = null;
        try {
            String jsessionid = getCookieValue("JSESSIONID");
            if (jsessionid == null) {
                jsessionid = "test";
            }

            String sfld = getSettingsSupplementary().getValue("analysis_output_dir") + "session/" + jsessionid;

            // if the session dir exists, then clear it,
            // if not let's create it.
            File sessfolder = new File(sfld + "/");
            if (!sessfolder.exists()) {
                sessfolder.mkdirs();
            } else {
                FileUtils.deleteDirectory(sessfolder);
                sessfolder.mkdirs();
            }

            StringBuilder sbSession = new StringBuilder();
            sbSession.append(String.valueOf(mapZoomLevel)).append(",").append(getLeftmenuSearchComposer().getViewportBoundingBox().toString());
            sbSession.append(System.getProperty("line.separator"));

            PersistenceStrategy strategy = new FilePersistenceStrategy(new File(sfld));
            List list = new XmlArrayList(strategy);

            String scatterplotNames = "";
            List udl = getPortalSession().getActiveLayers();
            Iterator iudl = udl.iterator();
            while (iudl.hasNext()) {
                MapLayer ml = (MapLayer) iudl.next();
                if (ml.getType() != LayerUtilities.MAP) {
                    if (ml.getSubType() == LayerUtilities.SCATTERPLOT) {
                        list.add(ml.getScatterplotData());
                        scatterplotNames += ((scatterplotNames.length() > 1) ? "___" + ml.getName() : ml.getName());
                    } else {
                        list.add(ml);
                    }
                }
            }
            sbSession.append("scatterplotNames=").append(scatterplotNames);
            sbSession.append(System.getProperty("line.separator"));

            out = new PrintWriter(new BufferedWriter(new FileWriter(sfld + "/details.txt")));
            out.write(sbSession.toString());
            out.close();

            String sessionurl = CommonData.webportalServer + "/?ss=" + jsessionid;
            String sessiondownload = CommonData.satServer + "/ws/download/session/" + jsessionid;
            //showMessage("Session saved. Please use the following link to share: \n <a href=''>"+sessionurl+"</a>" + sessionurl);

            //sbMessage.append("Your session is available at <br />");
            activateLink("*" + "<p>Your session has been saved and now available to share at <br />" + "<a href='" + sessionurl + "'>" + sessionurl + "</a>" + "<br />(Right-click on the link and to copy the link to clipboard)" + "</p>" + "<p>" + "Alternatively, click <a href='" + sessiondownload + "'>here</a> to download a direct link to this session" + "</p>", "Saved session", false, "");
        } catch (IOException ex) {
            logger.error("Unable to save session data: ", ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void loadUserSession(String sessionid) {
        Scanner scanner = null;
        try {

            String sfld = getSettingsSupplementary().getValue("analysis_output_dir") + "session/" + sessionid;

            File sessfolder = new File(sfld);
            if (!sessfolder.exists()) {
                showMessage("Session information does not exist. Please provide a valid session id");
                return;
            }

            scanner = new Scanner(new File(sfld + "/details.txt"));

            // first grab the zoom level and bounding box
            String[] mapdetails = scanner.nextLine().split(",");

            BoundingBox bb = new BoundingBox();
            bb.setMinLongitude(Float.parseFloat(mapdetails[1]));
            bb.setMinLatitude(Float.parseFloat(mapdetails[2]));
            bb.setMaxLongitude(Float.parseFloat(mapdetails[3]));
            bb.setMaxLatitude(Float.parseFloat(mapdetails[4]));
            //openLayersJavascript.zoomToBoundingBoxNow(bb);
            openLayersJavascript.setAdditionalScript(openLayersJavascript.zoomToBoundingBox(bb, true));

            String[] scatterplotNames = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("scatterplotNames")) {
                    scatterplotNames = line.substring(17).split("___");
                }
            }
            ArrayUtils.reverse(scatterplotNames);

            PersistenceStrategy strategy = new FilePersistenceStrategy(new File(sfld));
            List list = new XmlArrayList(strategy);

            ListIterator it = list.listIterator(list.size());
            int scatterplotIndex = 0;
            while (it.hasPrevious()) {
                Object o = it.previous();
                MapLayer ml = null;
                if (o instanceof MapLayer) {
                    ml = (MapLayer) o;
                    logger.debug("Loading " + ml.getName() + " -> " + ml.isDisplayed());
                    addUserDefinedLayerToMenu(ml, false);
                } else if (o instanceof ScatterplotData) {
                    ScatterplotData spdata = (ScatterplotData) o;
                    //double[] prevSelection = (double[])it.previous();
                    loadScatterplot(spdata, "My Scatterplot " + scatterplotIndex++); // scatterplotNames[scatterplotIndex++]

                }

                if (ml != null) {
                    addUserDefinedLayerToMenu(ml, true);
                }
            }

        } catch (Exception e) {
            logger.error("Unable to load session data", e);
            showMessage("Unable to load session data");
        } finally {
            scanner.close();
        }
    }

    public void updateAdhocGroup(Event event) {
        String[] params = ((String) event.getData()).split("\n");
        MapLayer ml = getMapLayer(params[0]);
        Query query;
        if (ml != null && (query = ml.getSpeciesQuery()) != null) {
            query.flagRecord(params[1], params[2].equalsIgnoreCase("true"));
        }
        updateLayerControls();
    }

    public Query downloadSecondQuery = null;
    public String[] downloadSecondLayers = null;

    public void downloadSecond(Event event) {
        SamplingDownloadUtil.downloadSecond(this, downloadSecondQuery, downloadSecondLayers);
    }

    public void openFacets(Event event) {
        llc2.cbColour.open();
    }

    public void onClick$downloadFeaturesCSV(Event event) {
        if (featuresCSV != null) {
            Filedownload.save(featuresCSV, "application/csv", "pointFeatures.csv");
        }
    }
}
