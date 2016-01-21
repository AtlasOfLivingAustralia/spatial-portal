package au.org.ala.spatial.composer.results;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.quicklinks.SamplingEvent;
import au.org.ala.spatial.composer.quicklinks.SpeciesListEvent;
import au.org.ala.spatial.dto.AreaReportItemDTO;
import au.org.ala.spatial.dto.AreaReportItemDTO.ListType;
import au.org.ala.spatial.logger.RemoteLogger;
import au.org.ala.spatial.util.*;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.SelectedArea;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.util.resource.Labels;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.*;

import javax.swing.event.ListDataEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static au.org.ala.spatial.dto.AreaReportItemDTO.ExtraInfoEnum;

/**
 * This class supports the Area Report in the spatial portal.
 * <p/>
 * DM 20/02/2013 - rewrite of this to allow for update of fields in the UI once
 * a query has finished. Previously these updates only happened when all queries
 * had returned.
 * <p/>
 * NC 17/10/2013 - rewrite to display everything nicely in a table. But update mechanism through
 * changing the model behind the view.
 *
 * @author adam
 * @author Dave Martin
 */
public class AreaReportController extends UtilityComposer {

    private static final String VIEW_RECORDS = "View Records";
    private static final Logger LOGGER = Logger.getLogger(AreaReportController.class);
    private String pid;
    private RemoteLogger remoteLogger;
    private String[] speciesDistributionText = null;
    private String[] speciesChecklistText = null;
    private String[] areaChecklistText = null;
    private boolean addedListener = false;

    private SelectedArea selectedArea = null;
    private String areaName = StringConstants.AREA_REPORT;
    private String areaDisplayName = StringConstants.AREA_REPORT;
    private String areaSqKm = null;
    private boolean includeEndemic;
    private Div divWorldNote;

    private JSONArray gazPoints = null;

    private JSONArray pointsOfInterest = null;

    private Map<String, Future<Map<String, String>>> futures = null;
    private long futuresStart = -1;
    private ExecutorService pool = null;
    private List<String> firedEvents = null;

    //Items for the list of configured facets
    private Grid facetsValues;
    private ChangableSimpleListModel areaReportListModel;
    private Map<String, AreaReportItemDTO> reportModelMap;
    private String journalmapHtml = null;

    public static void open(SelectedArea sa, String name, String displayName, String areaSqKm, double[] boundingBox, boolean includeEndemic) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("includeEndemic", includeEndemic);
        params.put("displayPointsOfInterest", CommonData.getDisplayPointsOfInterest());
        AreaReportController win = (AreaReportController) Executions.createComponents("/WEB-INF/zul/results/AreaReportResults.zul", null, params);
        try {
            win.doOverlapped();
            win.setPosition("center");
            win.setReportArea(sa, name, displayName, areaSqKm, (boundingBox == null) ? null : boundingBox.clone(), includeEndemic);
        } catch (Exception e) {
            LOGGER.error("error opening AreaReportResults.zul", e);
        }
    }

    public static JSONArray getGazPoints(String wkt) {
        try {
            int limit = Integer.MAX_VALUE;
            String url = CommonData.getLayersServer() + "/objects/inarea/" + CommonData.getSettings().get("area_report_gaz_field") + "?limit=" + limit;

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            LOGGER.debug(url);
            if (wkt != null) {
                post.addParameter(StringConstants.WKT, wkt);
            }
            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                return (JSONArray) jp.parse(txt);
            } else {
                LOGGER.debug(result + ", " + post.getResponseBodyAsString());
            }
        } catch (Exception e) {
            LOGGER.error("error getting number of gaz points in an area: " + wkt, e);
        }
        return null;
    }

    public static JSONArray getPointsOfInterest(String wkt) {
        try {
            String url = CommonData.getLayersServer() + "/intersect/poi/wkt";

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            if (wkt != null) {
                NameValuePair nvp = new NameValuePair(StringConstants.WKT, wkt);
                post.setRequestBody(new NameValuePair[]{nvp});
            }
            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                return (JSONArray) jp.parse(txt);
            }
        } catch (Exception e) {
            LOGGER.error("error getting points of interest in an area: " + wkt, e);
        }
        return null;
    }

    public static int getPointsOfInterestCount(String wkt) {
        try {
            String url = CommonData.getLayersServer() + "/intersect/poi/count/wkt";

            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);
            if (wkt != null) {
                NameValuePair nvp = new NameValuePair(StringConstants.WKT, wkt);
                post.setRequestBody(new NameValuePair[]{nvp});
            }
            post.addRequestHeader(StringConstants.ACCEPT, StringConstants.JSON_JAVASCRIPT_ALL);
            int result = client.executeMethod(post);
            if (result == 200) {
                String txt = post.getResponseBodyAsString();
                JSONParser jp = new JSONParser();
                return Integer.parseInt(((JSONObject) jp.parse(txt)).get("count").toString());
            }
        } catch (Exception e) {
            LOGGER.error("error getting points of interest in an area: " + wkt, e);
        }
        return -1;
    }

    public boolean shouldIncludeEndemic() {
        return includeEndemic;
    }

    public void setReportArea(SelectedArea sa, String name, String displayname, String areaSqKm, double[] boundingBox, boolean includeEndemic) {
        this.selectedArea = sa;

        this.areaName = name;
        this.areaDisplayName = displayname;
        this.areaSqKm = areaSqKm;
        this.includeEndemic = includeEndemic;
        ((Caption) getFellow(StringConstants.CTITLE)).setLabel(displayname);

        if (StringConstants.CURRENT_EXTENT.equals(name)) {
            addListener();
        }

        startQueries();
        String extras = "";
        extras += "areaSqKm: " + areaSqKm;
        if (boundingBox != null) {
            extras += ";boundingBox: " + boundingBox[0] + "," + boundingBox[1] + "," + boundingBox[2] + "," + boundingBox[3];
        }
        remoteLogger.logMapAnalysis(displayname, "Tool - Area Report", areaName + "__" + sa.getReducedWkt(), "", "", pid, extras, "0");

        // start checking of completed threads
        Events.echoEvent(StringConstants.CHECK_FUTURES, this, null);
    }

    @Override
    public void detach() {
        getMapComposer().getLeftmenuSearchComposer().removeViewportEventListener(StringConstants.FILTERING_RESULTS);
        super.detach();
    }

    void addListener() {
        if (!addedListener) {
            addedListener = true;
            // register for viewport changes
            EventListener el = new EventListener() {
                public void onEvent(Event event) throws Exception {
                    selectedArea = new SelectedArea(null, getMapComposer().getViewArea());
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener(StringConstants.FILTERING_RESULTS, el);
        }
    }

    /**
     * Set up the map for the model that is used to construct the area report table.
     *
     * @return an ordered map of hte maps that need to collect the sampled values
     */
    private Map<String, AreaReportItemDTO> setUpModelMap(boolean isWorldSelected) {
        Map<String, AreaReportItemDTO> values = new LinkedHashMap<String, AreaReportItemDTO>();
        String worldSuffix = isWorldSelected ? "*" : "";
        //area
        values.put(StringConstants.AREA, new AreaReportItemDTO("Area (sq km)"));
        //species
        values.put(StringConstants.SPECIES + worldSuffix, new AreaReportItemDTO("Number of species"));
        //spatially valid species
        values.put(StringConstants.SPATIAL_SPECIES, new AreaReportItemDTO("Number of species - spatially valid only"));
        if (includeEndemic) {
            //endemic
            values.put(StringConstants.ENDEMIC_SPECIES, new AreaReportItemDTO("Number of endemic species"));
            values.put(StringConstants.SPATIAL_ENDEMIC_SPECIES, new AreaReportItemDTO("Number of endemic species - spatially valid only"));
        }
        //occurrences
        values.put(StringConstants.OCCURRENCES + worldSuffix, new AreaReportItemDTO("Occurrences"));
        //spatially valid occurrences
        values.put(StringConstants.SPATIAL_OCCURRENCES, new AreaReportItemDTO("Occurrences - spatially valid only"));
        //expert distribution
        values.put(StringConstants.EXPERT_DISTRIBUTIONS, new AreaReportItemDTO("Expert distributions"));
        //checklist areas
        values.put(StringConstants.CHECKLIST_AREA, new AreaReportItemDTO(StringConstants.CHECKLIST_AREAS));
        //checklist species
        values.put(StringConstants.CHECKLIST_SPECIES, new AreaReportItemDTO("Checklist species"));
        //journalmap documents
        values.put(StringConstants.JOURNAL_MAP, new AreaReportItemDTO("Journalmap documents"));
        //gazetteer points
        values.put(StringConstants.GAZETTEER, new AreaReportItemDTO("Gazetteer points"));
        //points of interest
        if (CommonData.getDisplayPointsOfInterest()) {
            values.put("poi", new AreaReportItemDTO(StringConstants.POINTS_OF_INTEREST));
        }
        //configured facets
        for (int i = 0; i < CommonData.getAreaReportFacets().length; i++) {
            values.put(StringConstants.CONFIGURED_FACETS + i, new AreaReportItemDTO(""));
        }
        return values;
    }

    void startQueries() {

        final boolean worldAreaSelected = CommonData.WORLD_WKT.equals(selectedArea.getFacets() == null ? selectedArea.getWkt() : null);

        reportModelMap = setUpModelMap(worldAreaSelected);
        areaReportListModel = new ChangableSimpleListModel(new ArrayList(reportModelMap.values()));
        facetsValues.setModel(areaReportListModel);
        //Set the renderer that is responsible for the pretties and associating actions to the buttons
        facetsValues.setRowRenderer(new RowRenderer() {

            @Override
            public void render(Row row, Object data, int itemIdx) throws Exception {
                //data should be a map of facet result information
                if (data instanceof AreaReportItemDTO) {
                    final AreaReportItemDTO dto = (AreaReportItemDTO) data;
                    row.appendChild(new Label(dto.getTitle()));
                    row.appendChild(new Label(dto.getCount()));
                    //check for the buttons to display
                    Div listDiv = new Div();
                    Div mapDiv = new Div();
                    Div sampleDiv = new Div();
                    row.appendChild(listDiv);
                    row.appendChild(mapDiv);
                    row.appendChild(sampleDiv);
                    listDiv.setZclass("areaReportListCol");
                    mapDiv.setZclass("areaReportMapCol");
                    sampleDiv.setZclass("areaReportSampleCol");
                    Button b;
                    if (dto.getExtraInfo() != null) {
                        final boolean[] gk = dto.isGeospatialKosher() ? new boolean[]{true, false, false} : new boolean[]{true, true, false};
                        final boolean kosher = dto.isGeospatialKosher();
                        for (ExtraInfoEnum type : dto.getExtraInfo()) {
                            switch (type) {
                                case LIST:
                                    b = new Button("List");
                                    b.setZclass(StringConstants.BTN_BTN_MINI);
                                    b.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                        @Override
                                        public void onEvent(Event event)
                                                throws Exception {
                                            if (dto.getListType() == ListType.SPECIES) {
                                                new SpeciesListEvent(areaName, gk, dto.isEndemic(), dto.getExtraParams()).onEvent(event);
                                            } else if (dto.getListType() == ListType.DISTRIBUTION) {
                                                listDistributions(dto);
                                            } else if (dto.getListType() == ListType.AREA_CHECKLIST) {
                                                listAreaChecklists(dto);
                                            } else if (dto.getListType() == ListType.SPECIES_CHECKLIST) {
                                                listSpeciesChecklists(dto);
                                            } else if (dto.getListType() == ListType.JOURNAL_MAP) {
                                                listJournalmap();
                                            }

                                        }

                                    });
                                    listDiv.appendChild(b);
                                    break;
                                case SAMPLE:
                                    b = new Button("Sample");
                                    b.setZclass(StringConstants.BTN_BTN_MINI);
                                    final SamplingEvent sle = new SamplingEvent(null, areaName, null, gk);
                                    b.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                        @Override
                                        public void onEvent(Event event)
                                                throws Exception {
                                            sle.onEvent(new ForwardEvent("", getMapComposer(), null));

                                        }

                                    });
                                    sampleDiv.appendChild(b);
                                    break;
                                case MAP_ALL:
                                    //set up the map button
                                    b = new Button("Map all");
                                    b.setZclass(StringConstants.BTN_BTN_MINI);
                                    b.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                        @Override
                                        public void onEvent(Event event)
                                                throws Exception {
                                            if (dto.getTitle().contains("Gazetteer")) {
                                                mapGazetteer();
                                            } else if (StringConstants.POINTS_OF_INTEREST.equals(dto.getTitle())) {
                                                onMapPointsOfInterest(event);
                                            } else if (kosher) {
                                                onMapSpeciesKosher(new Event("", null, dto.getExtraParams()));
                                            } else {
                                                onMapSpecies(new Event("", null, dto.getExtraParams()));
                                            }
                                        }
                                    });
                                    mapDiv.appendChild(b);
                                    break;
                                default:
                                    LOGGER.error("invalid type for AreaReportController: " + type);
                            }
                        }
                    }
                    if (dto.getUrlDetails() != null) {
                        Vlayout vlayout = new Vlayout();
                        for (Map.Entry<String, String> entry : dto.getUrlDetails().entrySet()) {
                            String urlTitle = entry.getKey();
                            org.zkoss.zul.A viewRecords = new org.zkoss.zul.A(urlTitle);
                            String url = entry.getValue();

                            if (url.startsWith("http")) {
                                viewRecords.setHref(url);
                                viewRecords.setTarget(StringConstants.BLANK);
                            } else {
                                final String helpUrl = CommonData.getSettings().get("help_url") + "/spatial-portal-help/" + url;
                                viewRecords.addEventListener(StringConstants.ONCLICK, new EventListener() {

                                    @Override
                                    public void onEvent(Event event)
                                            throws Exception {
                                        Events.echoEvent(StringConstants.OPEN_URL, getMapComposer(), helpUrl);

                                    }

                                });
                            }
                            vlayout.appendChild(viewRecords);
                        }
                        row.appendChild(vlayout);
                    } else {
                        row.appendChild(new Label(""));
                    }
                }
            }

        });

        divWorldNote.setVisible(worldAreaSelected);

        Callable occurrenceCount = new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() {
                return occurrenceCount(worldAreaSelected, reportModelMap.get(StringConstants.OCCURRENCES));
            }
        };

        Callable occurrenceCountKosher = new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() {
                return occurrenceCountKosher(worldAreaSelected, reportModelMap.get(StringConstants.SPATIAL_OCCURRENCES));
            }
        };

        Callable speciesCount = new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                return speciesCount(worldAreaSelected, reportModelMap.get(StringConstants.SPECIES));
            }
        };

        Callable speciesCountKosher = new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                return speciesCountKosher(worldAreaSelected, reportModelMap.get(StringConstants.SPATIAL_SPECIES));
            }
        };

        Callable endemismCount = new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                return endemismCount(worldAreaSelected, reportModelMap.get(StringConstants.ENDEMIC_SPECIES));
            }
        };

        Callable endemismCountKosher = new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                return endemismCountKosher(worldAreaSelected, reportModelMap.get(StringConstants.SPATIAL_ENDEMIC_SPECIES));
            }
        };

        Callable speciesDistributions = new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                return intersectWithSpeciesDistributions(reportModelMap.get(StringConstants.EXPERT_DISTRIBUTIONS));
            }
        };

        Callable calculatedArea = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return calculateArea(reportModelMap.get(StringConstants.AREA));
            }
        };

        Callable speciesChecklists = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return intersectWithSpeciesChecklists(reportModelMap.get(StringConstants.CHECKLIST_AREA), reportModelMap.get(StringConstants.CHECKLIST_SPECIES));
            }
        };

        Callable gazPointsC = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return countGazPoints(reportModelMap.get(StringConstants.GAZETTEER));
            }
        };

        Callable journalmap = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return journalmap(reportModelMap.get(StringConstants.JOURNAL_MAP));
            }
        };

        Callable pointsOfInterestC = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return countPointsOfInterest(reportModelMap.get("poi"));
            }
        };

        Callable[] areaFacets = new Callable[CommonData.getAreaReportFacets().length];
        for (int i = 0; i < CommonData.getAreaReportFacets().length; i++) {
            final String facet = CommonData.getAreaReportFacets()[i];
            final String s = String.valueOf(i);
            areaFacets[i] = new Callable<Map<String, Object>>() {
                @Override
                public Map<String, Object> call() {
                    return facetCounts(reportModelMap.get(StringConstants.CONFIGURED_FACETS + s), facet);
                }
            };
        }

        try {
            this.pool = Executors.newFixedThreadPool(50);
            this.futures = new HashMap<String, Future<Map<String, String>>>();
            this.firedEvents = new ArrayList<String>();
            // add all futures
            futures.put("CalculatedArea", pool.submit(calculatedArea));
            futures.put("OccurrenceCount", pool.submit(occurrenceCount));
            futures.put("OccurrenceCountKosher", pool.submit(occurrenceCountKosher));
            for (int i = 0; i < areaFacets.length; i++) {
                futures.put("AreaFacetCounts" + i, pool.submit(areaFacets[i]));
            }
            futures.put("SpeciesCount", pool.submit(speciesCount));
            futures.put("SpeciesCountKosher", pool.submit(speciesCountKosher));
            futures.put("GazPoints", pool.submit(gazPointsC));
            futures.put("SpeciesChecklists", pool.submit(speciesChecklists));
            futures.put("SpeciesDistributions", pool.submit(speciesDistributions));
            futures.put("Journalmap", pool.submit(journalmap));

            if (CommonData.getDisplayPointsOfInterest()) {
                futures.put("PointsOfInterest", pool.submit(pointsOfInterestC));
            }

            if (includeEndemic) {
                futures.put("EndemicCount", pool.submit(endemismCount));
                futures.put("EndemicCountKosher", pool.submit(endemismCountKosher));
            }

            futuresStart = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.error("error setting counts for futures", e);
        }
    }

    public void checkFutures() {
        try {
            LOGGER.debug("Check futures.....");
            int cancelled = 0;

            for (Map.Entry<String, Future<Map<String, String>>> futureEntry : futures.entrySet()) {
                String eventToFire = "render" + futureEntry.getKey();

                if (futureEntry.getValue().isDone() && !firedEvents.contains(eventToFire)) {
                    firedEvents.add(eventToFire);
                    //inform the list model to update
                    areaReportListModel.setModelChanged();
                }

                // kill anything taking longer than 5 mins
                if (!futureEntry.getValue().isDone() && (System.currentTimeMillis() - futuresStart) > 300000) {
                    futureEntry.getValue().cancel(true);
                    //now set everything not completed to "Request timed out"
                    for (AreaReportItemDTO model : (List<AreaReportItemDTO>) areaReportListModel.getInnerList()) {
                        if (model.isLoading()) {
                            model.setCount("Request timed out");
                        }
                    }
                    cancelled++;
                    areaReportListModel.setModelChanged();
                }
            }
            LOGGER.debug("Fired events: " + firedEvents.size() + ", Futures: " + futures.size() + ", Cancelled: " + cancelled);

            boolean allComplete = firedEvents.size() + cancelled == futures.size();

            if (!allComplete) {
                Thread.sleep(1000);
                Events.echoEvent(StringConstants.CHECK_FUTURES, this, null);
            } else {
                LOGGER.debug("All futures completed.");
                this.pool.shutdown();
                futures = null;
            }

        } catch (InterruptedException e) {
            LOGGER.error("", e);
        }
    }

    Map<String, Integer> endemismCount(boolean worldSelected, AreaReportItemDTO model) {
        Map<String, Integer> countsData = new HashMap<String, Integer>();
        Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, new boolean[]{true, true, false});
        int endemicCount = sq.getEndemicSpeciesCount();
        countsData.put("endemicSpeciesCount", endemicCount);
        model.setCount(String.format("%,d", endemicCount));
        model.setGeospatialKosher(false);
        if (endemicCount > 0) {
            model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST, ExtraInfoEnum.MAP_ALL});
            model.setListType(ListType.SPECIES);
            model.setEndemic(true);
        }
        model.setEndemic(true);
        return countsData;
    }

    Map<String, Integer> endemismCountKosher(boolean worldSelected, AreaReportItemDTO model) {
        Map<String, Integer> countsData = new HashMap<String, Integer>();
        Query sq2 = QueryUtil.queryFromSelectedArea(null, selectedArea, false, new boolean[]{true, false, false});
        // based the endemic count on the geospatially kosher - endemic is
        // everything if the world is selected
        int count = sq2.getEndemicSpeciesCount();
        countsData.put("endemicSpeciesCountKosher", count);
        model.setCount(String.format("%,d", count));
        if (count > 0) {
            model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST, ExtraInfoEnum.MAP_ALL});
            model.setListType(ListType.SPECIES);
            model.setEndemic(true);
            model.setGeospatialKosher(true);
        }
        return countsData;
    }

    Map<String, Integer> speciesCount(boolean worldSelected, AreaReportItemDTO model) {

        Map<String, Integer> countsData = new HashMap<String, Integer>();
        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, null);
            int resultsCount = sq.getSpeciesCount();

            if (resultsCount == 0) {
                countsData.put(StringConstants.SPECIES_COUNT, 0);
                model.setCount("0");

                return countsData;
            }
            countsData.put(StringConstants.SPECIES_COUNT, resultsCount);
            model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});
            model.setListType(ListType.SPECIES);

            model.setGeospatialKosher(false);
            model.setCount(String.format("%,d", resultsCount));
        } catch (Exception ex) {
            LOGGER.error("error getting speciescount", ex);
        }
        return countsData;
    }

    Map<String, Object> facetCounts(AreaReportItemDTO pmodel, String facet) {
        org.zkoss.util.Locales.setThreadLocal(null);
        Map<String, AreaReportItemDTO> countsData = new LinkedHashMap<String, AreaReportItemDTO>();
        LOGGER.debug("Starting to get facet counts for : " + facet);

        AreaReportItemDTO dto = pmodel;
        int colonIdx = facet.indexOf(':');
        String query = colonIdx > 0 ? facet : facet + ":*";

        Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, query, false, null);
        int count = sq.getSpeciesCount();
        if (count == -1) count = 0;
        String label = Labels.getLabel("facet." + facet, facet);
        //title
        dto.setTitle(label);
        //count
        dto.setCount(String.format("%,d", count));
        //add the appropriate urls
        //check to see if it is a species list
        if (facet.equals(CommonData.speciesListThreatened)) {
            dto.setTitle("Threatened Species");
        } else if (facet.equals(CommonData.speciesListInvasive)) {
            dto.setTitle("Invasive Species");
        } else if (facet.startsWith("species_list") && colonIdx > 0) {
            //extract everything to the right of the colon and construct the url
            String dataResourceUid = facet.substring(colonIdx + 1);
            String title = SpeciesListUtil.getSpeciesListMap().get(dataResourceUid);
            if (title != null) {
                dto.setTitle(title);
            }
            dto.addUrlDetails("Full List", CommonData.getSpeciesListServer() + "/speciesListItem/list/" + dataResourceUid);
        } else if (facet.startsWith("species_group")) {
            dto.setTitle(facet.substring(colonIdx + 1));
        }

        //url
        if (count > 0) {
            dto.addUrlDetails(VIEW_RECORDS, CommonData.getBiocacheWebServer() + "/occurrences/search?q=" + sq.getQ() +
                    (StringUtils.isNotEmpty(sq.getQc()) ? sq.getQc() : ""));

            dto.setExtraParams(query);
            dto.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST, ExtraInfoEnum.MAP_ALL});
            dto.setListType(ListType.SPECIES);
        }
        countsData.put(facet, dto);

        LOGGER.debug("Facet Counts ::: " + countsData);

        //this data needs to be added to the model on the correct thread...
        return (Map) countsData;
    }

    Map<String, Integer> speciesCountKosher(boolean worldSelected, AreaReportItemDTO model) {

        Map<String, Integer> countsData = new HashMap<String, Integer>();

        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, new boolean[]{true, false, false});
            int resultsCountKosher = sq.getSpeciesCount();

            if (resultsCountKosher == 0) {
                countsData.put(StringConstants.SPECIES_COUNT_KOSHER, 0);
                model.setCount("0");
                return countsData;
            }
            countsData.put(StringConstants.SPECIES_COUNT_KOSHER, resultsCountKosher);
            model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});

            model.setListType(ListType.SPECIES);
            model.setGeospatialKosher(true);
            model.setCount(String.format("%,d", resultsCountKosher));
        } catch (Exception ex) {
            LOGGER.error("error getting speciesCountKosher", ex);
        }
        return countsData;
    }

    Map<String, Object> occurrenceCount(boolean worldSelected, AreaReportItemDTO model) {

        Map<String, Object> countsData = new HashMap<String, Object>();
        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, null);
            int resultsCountOccurrences = sq.getOccurrenceCount();
            if (resultsCountOccurrences == 0) {
                countsData.put(StringConstants.OCCURRENCES_COUNT, "0");
                model.setCount("0");

                return countsData;
            } else {
                model.addUrlDetails(VIEW_RECORDS, CommonData.getBiocacheWebServer() + "/occurrences/search?q=" + sq.getQ() +
                        (StringUtils.isNotEmpty(sq.getQc()) ? sq.getQc() : ""));
            }

            countsData.put(StringConstants.OCCURRENCES_COUNT, resultsCountOccurrences);
            model.setCount(String.format("%,d", resultsCountOccurrences));
            //add the info about the buttons to include

            if (resultsCountOccurrences > 0 && resultsCountOccurrences <= Integer.parseInt(CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP))) {
                model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.MAP_ALL, ExtraInfoEnum.SAMPLE});
            }
            model.setGeospatialKosher(false);
        } catch (Exception ex) {
            LOGGER.error("error getting occurrences count", ex);
        }
        return countsData;
    }

    Map<String, Object> occurrenceCountKosher(boolean worldSelected, AreaReportItemDTO model) {

        Map<String, Object> countsData = new HashMap<String, Object>();
        try {
            Query sq = QueryUtil.queryFromSelectedArea(null, selectedArea, false, new boolean[]{true, false, false});
            int resultsCountOccurrencesKosher = sq.getOccurrenceCount();

            if (resultsCountOccurrencesKosher == 0) {
                countsData.put(StringConstants.OCCURRENCES_COUNT_KOSHER, 0);
                model.setCount("0");

                return countsData;
            } else {
                countsData.put("viewRecordsKosherUrl", CommonData.getBiocacheWebServer() + "/occurrences/search?q=" + sq.getQ() +
                        (StringUtils.isNotEmpty(sq.getQc()) ? sq.getQc() : ""));
                model.addUrlDetails(VIEW_RECORDS, CommonData.getBiocacheWebServer() + "/occurrences/search?q=" + sq.getQ() +
                        (StringUtils.isNotEmpty(sq.getQc()) ? sq.getQc() : ""));
            }
            model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.MAP_ALL, ExtraInfoEnum.SAMPLE});
            model.setGeospatialKosher(true);
            if (resultsCountOccurrencesKosher > 0 && resultsCountOccurrencesKosher <= Integer.parseInt(CommonData.getSettings().getProperty(StringConstants.MAX_RECORD_COUNT_MAP))) {
                countsData.put(StringConstants.OCCURRENCES_COUNT_KOSHER, resultsCountOccurrencesKosher);
            }
            model.setCount(String.format("%,d", resultsCountOccurrencesKosher));
        } catch (Exception ex) {
            LOGGER.error("error getting occurrences count", ex);
        }
        return countsData;
    }

    public void onMapSpecies(Event event) {
        try {
            SelectedArea sa;
            if (!StringConstants.CURRENT_EXTENT.equalsIgnoreCase(areaName)) {
                sa = selectedArea;
            } else {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            }

            Query baseQuery = null;
            if (event != null && event.getData() != null) {
                baseQuery = new BiocacheQuery(null, null, (String) event.getData(), null, false, null);
            }
            Query query = QueryUtil.queryFromSelectedArea(baseQuery, sa, true, null);

            String activeAreaLayerName = getMapComposer().getNextActiveAreaLayerName(areaDisplayName);
            getMapComposer().mapSpecies(query, activeAreaLayerName, StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                    Util.nextColour(), false);

        } catch (Exception e) {
            LOGGER.error("error mapping species in area", e);
        }
    }

    public void onMapSpeciesKosher(Event event) {
        try {
            SelectedArea sa;
            if (!StringConstants.CURRENT_EXTENT.equalsIgnoreCase(areaName)) {
                sa = selectedArea;
            } else {
                sa = new SelectedArea(null, getMapComposer().getViewArea());
            }

            Query baseQuery = null;
            if (event != null && event.getData() != null) {
                baseQuery = new BiocacheQuery(null, null, (String) event.getData(), null, false, null);
            }
            Query query = QueryUtil.queryFromSelectedArea(baseQuery, sa, true, new boolean[]{true, false, false});

            String activeAreaLayerName = getMapComposer().getNextActiveAreaLayerName(areaDisplayName + " geospatial kosher");
            getMapComposer().mapSpecies(query, activeAreaLayerName, StringConstants.SPECIES, -1, LayerUtilitiesImpl.SPECIES, null, -1, MapComposer.DEFAULT_POINT_SIZE, MapComposer.DEFAULT_POINT_OPACITY,
                    Util.nextColour(), false);

        } catch (Exception e) {
            LOGGER.error("error mapping kosher species in area", e);
        }
    }

    public void onMapPointsOfInterest(Event event) {
        try {
            String activeAreaLayerName = "Points of interest in " + areaDisplayName;

            StringBuilder sb = new StringBuilder();
            sb.append("MULTIPOINT(");

            if (pointsOfInterest == null) {
                String wkt = selectedArea.getReducedWkt();
                if (wkt.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                    // use boundingbox
                    List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                    double long1 = bbox.get(0);
                    double lat1 = bbox.get(1);
                    double long2 = bbox.get(2);
                    double lat2 = bbox.get(3);
                    wkt = StringConstants.POLYGON + "((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
                }
                pointsOfInterest = getPointsOfInterest(wkt);
            }
            for (int i = 0; i < pointsOfInterest.size(); i++) {
                JSONObject jsonObjPoi = (JSONObject) pointsOfInterest.get(i);
                double latitude = Double.parseDouble(jsonObjPoi.get(StringConstants.LATITUDE).toString());
                double longitude = Double.parseDouble(jsonObjPoi.get(StringConstants.LONGITUDE).toString());

                sb.append(longitude);
                sb.append(" ");
                sb.append(latitude);

                if (i < pointsOfInterest.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(")");

            getMapComposer().mapPointsOfInterest(sb.toString(), activeAreaLayerName, activeAreaLayerName);

        } catch (Exception e) {
            LOGGER.error("error mapping points of interest", e);
        }
    }

    public Map<String, Integer> intersectWithSpeciesDistributions(AreaReportItemDTO model) {
        Map<String, Integer> speciesDistributions = new HashMap<String, Integer>();
        try {
            String wkt = selectedArea.getReducedWkt();
            if (wkt.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                // use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = StringConstants.POLYGON + "((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }
            String[] lines = Util.getDistributionsOrChecklists(StringConstants.DISTRIBUTIONS, wkt, null, null);

            if (lines.length <= 1) {
                speciesDistributions.put(StringConstants.INTERSECT_WITH_SPECIES_DISTRIBUTIONS, 0);
                model.setCount("0");
                speciesDistributionText = null;
            } else {
                speciesDistributions.put(StringConstants.INTERSECT_WITH_SPECIES_DISTRIBUTIONS, lines.length - 1);
                model.setCount(Integer.toString(lines.length - 1));
                model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});
                model.setListType(ListType.DISTRIBUTION);
                speciesDistributionText = lines;
            }
        } catch (Exception e) {
            LOGGER.error("error getting intersected distribution areas", e);
        }
        return speciesDistributions;
    }

    public Map<String, String> intersectWithSpeciesChecklists(AreaReportItemDTO areaModel, AreaReportItemDTO spModel) {
        Map<String, String> checklistsCounts = new HashMap<String, String>();
        try {
            String wkt = selectedArea.getReducedWkt();
            if (wkt.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                // use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = StringConstants.POLYGON + "((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            String[] lines = Util.getDistributionsOrChecklists(StringConstants.CHECKLISTS, wkt, null, null);

            if (lines.length <= 1) {
                spModel.setCount("0");
                checklistsCounts.put(StringConstants.INTERSECT_WITH_SPECIES_CHECKLISTS, "0");
                areaModel.setCount("0");
                checklistsCounts.put(StringConstants.INTERSECT_WITH_AREA_CHECKLISTS, "0");
                speciesChecklistText = null;
            } else {
                checklistsCounts.put(StringConstants.INTERSECT_WITH_SPECIES_CHECKLISTS, String.format("%,d", lines.length - 1));
                spModel.setCount(String.format("%,d", lines.length - 1));
                spModel.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});
                spModel.setListType(ListType.SPECIES_CHECKLIST);
                areaChecklistText = Util.getAreaChecklists(lines);
                checklistsCounts.put(StringConstants.INTERSECT_WITH_AREA_CHECKLISTS, String.format("%,d", areaChecklistText.length - 1));
                areaModel.setCount(String.format("%,d", areaChecklistText.length - 1));
                areaModel.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});
                areaModel.setListType(ListType.AREA_CHECKLIST);
                speciesChecklistText = lines;
            }
        } catch (Exception e) {
            LOGGER.error("error getting intersected species checklists", e);
        }

        return checklistsCounts;
    }

    public Map<String, String> countGazPoints(AreaReportItemDTO model) {
        Map<String, String> gazPointsCounts = new HashMap<String, String>();
        try {
            String wkt = selectedArea.getReducedWkt();
            if (wkt.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                // use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = StringConstants.POLYGON + "((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            JSONArray ja = getGazPoints(wkt);

            if (ja == null || ja.isEmpty()) {
                gazPointsCounts.put(StringConstants.COUNT_GAZ_POINTS, "0");
                model.setCount("0");
                speciesChecklistText = null;
            } else {
                gazPointsCounts.put(StringConstants.COUNT_GAZ_POINTS, String.format("%,d", ja.size()));
                model.setCount(String.format("%,d", ja.size()));
                model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.MAP_ALL});
                gazPoints = ja;
            }
        } catch (Exception e) {
            LOGGER.error("error with area gaz point count", e);
        }

        return gazPointsCounts;
    }

    public Map<String, String> countPointsOfInterest(AreaReportItemDTO model) {
        Map<String, String> poiCounts = new HashMap<String, String>();
        try {
            String wkt = selectedArea.getReducedWkt();
            if (wkt.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                // use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                double long1 = bbox.get(0);
                double lat1 = bbox.get(1);
                double long2 = bbox.get(2);
                double lat2 = bbox.get(3);
                wkt = StringConstants.POLYGON + "((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," + long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            int count = getPointsOfInterestCount(wkt);

            if (count <= 0) {
                poiCounts.put(StringConstants.COUNT_POINTS_OF_INTEREST, "0");
                model.setCount("0");
                speciesChecklistText = null;
            } else {
                poiCounts.put(StringConstants.COUNT_POINTS_OF_INTEREST, String.format("%,d", count));
                model.setCount(String.format("%,d", count));
                model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.MAP_ALL});
            }
        } catch (Exception e) {
            LOGGER.error("area report error counting points of interest", e);
        }

        return poiCounts;
    }

    Map<String, String> journalmap(AreaReportItemDTO model) {

        Map<String, String> countData = new HashMap<String, String>();

        try {
            String area = selectedArea.getWkt();
            double lat1 = 0;
            double lat2 = 0;
            double long1 = 0;
            double long2 = 0;
            if (area.contains(StringConstants.ENVELOPE) && selectedArea.getMapLayer() != null) {
                // use boundingbox
                List<Double> bbox = selectedArea.getMapLayer().getMapLayerMetadata().getBbox();
                long1 = bbox.get(0);
                lat1 = bbox.get(1);
                long2 = bbox.get(2);
                lat2 = bbox.get(3);

                area = "POLYGON((" + long1 + " " + lat1 + "," + long1 + " " + lat2 + "," +
                        long2 + " " + lat2 + "," + long2 + " " + lat1 + "," + long1 + " " + lat1 + "))";
            }

            List<JSONObject> list = CommonData.filterJournalMapArticles(area);

            String journalmapUrl = CommonData.getSettings().getProperty("journalmap.url", null);

            StringBuilder sb = new StringBuilder();
            sb.append("<ol>");
            for (int i = 0; i < list.size(); i++) {
                JSONObject o = list.get(i);
                sb.append("<li>");
                sb.append("<a href=\"").append(journalmapUrl + "articles/" + o.get("id").toString()).append("\" ");
                sb.append("target=\"_blank\">");
                sb.append(o.get("title"));
                sb.append("</li>");
            }
            sb.append("</ol>");

            if (!list.isEmpty()) {
                journalmapHtml = sb.toString();
            }

            countData.put(StringConstants.JOURNAL_MAP, String.valueOf(list.size()));
            model.setCount(Integer.toString(list.size()));
            if (!list.isEmpty()) {
                model.setExtraInfo(new ExtraInfoEnum[]{ExtraInfoEnum.LIST});
                model.setListType(ListType.JOURNAL_MAP);
                model.addUrlDetails("JournalMap", "https://www.journalmap.org/");
            }

        } catch (Exception e) {
            LOGGER.error("unable to get area info from journalmap", e);
            countData.put(StringConstants.JOURNAL_MAP, "unavailable");
            model.setCount("Unavailable");
        }
        return countData;
    }

    public void listDistributions(AreaReportItemDTO model) {
        int c = 0;
        try {
            c = Integer.parseInt(model.getCount().replace(",", ""));
        } catch (Exception e) {
            LOGGER.error("failed to list distributions", e);
        }
        if (c > 0 && speciesDistributionText != null) {
            if (getMapComposer().hasFellow(StringConstants.DISTRIBUTION_RESULTS)) {
                getMapComposer().getFellowIfAny(StringConstants.DISTRIBUTION_RESULTS).detach();
            }
            DistributionsController dc = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

            try {
                dc.setParent(this);
                dc.doModal();
                dc.init(speciesDistributionText, "Expert Distributions", model.getCount());
            } catch (Exception e) {
                LOGGER.error("error opening expert distributions window", e);
            }
        }
    }

    public void listSpeciesChecklists(AreaReportItemDTO model) {
        int c = 0;
        try {
            c = Integer.parseInt(model.getCount().replace(",", ""));
        } catch (Exception e) {
            LOGGER.error("failed to get checklists", e);
        }
        if (c > 0 && speciesChecklistText != null) {
            DistributionsController dc = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

            try {
                dc.setParent(this);
                dc.doModal();
                dc.init(speciesChecklistText, "Species Checklists", model.getCount());
            } catch (Exception e) {
                LOGGER.error("error opening species checklists window", e);
            }
        }
    }

    public void listAreaChecklists(AreaReportItemDTO model) {
        int c = 0;
        //get the areaChecklist model

        try {
            c = Integer.parseInt(model.getCount().replace(",", ""));
        } catch (Exception e) {
            LOGGER.error("failed to get checklists", e);
        }
        if (c > 0 && areaChecklistText != null) {
            DistributionsController dc = (DistributionsController) Executions.createComponents("WEB-INF/zul/results/AnalysisDistributionResults.zul", this, null);

            try {
                dc.setParent(this);
                dc.doModal();
                dc.init(areaChecklistText, StringConstants.CHECKLIST_AREAS, model.getCount());

            } catch (Exception e) {
                LOGGER.error("error opening area checklists window", e);
            }
        }
    }

    public void onClick$downloadexpert(Event event) {
        onClick$sdDownload(event);
    }

    public void onClick$sdDownload(Event event) {
        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : speciesDistributionText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), StringConstants.TEXT_PLAIN, "Species_distributions_" + sdate + "_" + spid + ".csv");
    }

    public void onClick$downloadchecklist(Event event) {
        onClick$clDownload(event);
    }

    public void onClick$clDownload(Event event) {
        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : speciesChecklistText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), StringConstants.TEXT_PLAIN, "Species_checklists_" + sdate + "_" + spid + ".csv");
    }

    public void onClick$aclDownload(Event event) {
        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        for (String s : areaChecklistText) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s);
        }
        Filedownload.save(sb.toString(), StringConstants.TEXT_PLAIN, "Area_checklists_" + sdate + "_" + spid + ".csv");
    }

    protected Map<String, String> calculateArea(AreaReportItemDTO model) {

        Map<String, String> areaCalc = new HashMap<String, String>();

        if (areaSqKm != null) {
            areaCalc.put(StringConstants.AREA, areaSqKm);
            model.setCount(areaSqKm);
            speciesDistributionText = null;

            model.addUrlDetails("Info", "note-area-sq-km");
            return areaCalc;
        }

        try {
            double totalarea = Util.calculateArea(selectedArea.getWkt());
            DecimalFormat df = new DecimalFormat("###,###.##");

            areaCalc.put(StringConstants.AREA, df.format(totalarea / 1000 / 1000));
            model.setCount(df.format(totalarea / 1000 / 1000));

            model.addUrlDetails("Info", "note-area-sq-km");

        } catch (Exception e) {
            LOGGER.error("Error in calculateArea", e);
            areaCalc.put(StringConstants.AREA, "");
            model.setCount("ERROR...");
        }
        return areaCalc;
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void listJournalmap() {
        if (journalmapHtml != null) {
            Event ev = new Event(StringConstants.ONCLICK, this, "Journalmap Documents\n" + journalmapHtml);
            getMapComposer().openHTML(ev);
        }
    }

    public void mapGazetteer() {
        try {
            if (gazPoints == null || gazPoints.isEmpty()) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            Set<String> columns = new TreeSet<String>();

            // get columns
            for (int i = 0; i < gazPoints.size(); i++) {
                columns.addAll(((JSONObject) gazPoints.get(i)).keySet());
            }

            // write columns, first two are longitude,latitude
            sb.append("longitude,latitude");
            for (String s : columns) {
                if (!StringConstants.LONGITUDE.equals(s) && !StringConstants.LATITUDE.equals(s)) {
                    sb.append(",").append(s);
                }
            }

            for (int i = 0; i < gazPoints.size(); i++) {
                sb.append("\n");

                if (((JSONObject) gazPoints.get(i)).containsKey(StringConstants.GEOMETRY)) {
                    String geometry = ((JSONObject) gazPoints.get(i)).get(StringConstants.GEOMETRY).toString();
                    geometry = geometry.replace("POINT(", "").replace(")", "").replace(" ", ",");
                    sb.append(geometry);
                } else {
                    sb.append(",");
                }

                for (String s : columns) {
                    if (!StringConstants.LONGITUDE.equals(s) && !StringConstants.LATITUDE.equals(s)) {
                        String ss = ((JSONObject) gazPoints.get(i)).containsKey(s) ? ((JSONObject) gazPoints.get(i)).get(s).toString() : "";
                        sb.append(",\"").append(ss.replace("\"", "\"\"").replace("\\", "\\\\")).append("\"");
                    }
                }
            }

            getMapComposer().setFeaturesCSV(sb.toString());
            getMapComposer().getOpenLayersJavascript().execute("mapFrame.mapPoints('" + StringEscapeUtils.escapeJavaScript(gazPoints.toString()) + "');");
        } catch (Exception e) {
            LOGGER.error("error mapping gaz points from area report", e);
        }
    }

    /**
     * A simple list model object that allows external objects to request change events to be fired.
     *
     * @author Natasha Carter (natasha.carter@csiro.au)
     */
    public static class ChangableSimpleListModel extends ListModelList {
        public ChangableSimpleListModel(List data) {
            super(data);
        }

        public void setModelChanged() {

            fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
        }
    }

    public void onClick$btnDownload(Event event) {
        String spid = pid;
        if (spid == null || StringConstants.NONE.equals(spid)) {
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat(StringConstants.DATE);
        String sdate = date.format(new Date());

        StringBuilder sb = new StringBuilder();
        //area name
        sb.append("Area: " + areaDisplayName);
        for (Map.Entry<String, AreaReportItemDTO> i : reportModelMap.entrySet()) {
            sb.append("\n\"").append(i.getValue().getTitle()).append("\",\"").append(i.getValue().getCount()).append("\"");
        }


        Filedownload.save(sb.toString(), StringConstants.TEXT_PLAIN, "Area_report_" + sdate + "_" + spid + ".csv");
    }
}