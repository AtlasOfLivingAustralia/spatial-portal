package au.org.emii.portal.menu;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.ScatterplotDataDTO;
import au.org.ala.spatial.util.Query;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.wms.WMSStyle;
import net.sf.json.JSONArray;
import org.ala.layers.legend.Facet;
import org.ala.layers.legend.LegendObject;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a map layer - used for both rendering map layers and
 * building the menu system.
 * <p/>
 * A tree structured menu is built by adding children with addChild. Each node
 * of the tree knows about it's parent and child objects so it is possible to
 * walk the tree from any given node. The root node is identifiable because it
 * has a null parent. Leaf nodes are identified by either checking for the
 * presence of children or by calling the convenience method isLeaf()
 * <p/>
 * IMPORTANT - READ NOTES HERE BEFORE MODIFYING THIS CLASS!!
 * ========================================================= IF YOU ADD FIELDS
 * TO THIS CLASS THAT AREN'T SIMPLE PRIMITIVE TYPES, MAKE SURE YOU UPDATE THE
 * clone() METHOD TO ENSURE YOUR CHANGES ARE PICKED UP WHEN THE MAPLAYERS ARE
 * COPIED FOR SESSION CREATION!
 *
 * @author geoff
 */
public class MapLayer extends AbstractIdentifierImpl implements Cloneable, Serializable {

    /**
     * Used as a value for selectedStyleIndex to indicate that the default
     * rendering style should be used
     */
    public static final int STYLE_DEFAULT = 0;
    /**
     * Pointer to the element currently marked as selected in the styles list -
     * STYLE_DEFAULT means ignore all style information and use server defaults
     * until the user tells us otherwise
     */
    private int selectedStyleIndex = STYLE_DEFAULT;
    /**
     * Delimiter used when generating map layer ids
     */
    private static final String DELIM = "::";
    private static final long serialVersionUID = 1L;
    /**
     * URI to fetch WMS data from
     */
    private String uri = null;
    /*
     * geoserver Common Query Language
     */
    private String cql = null;
    /**
     * Opacity on map - range 0.0f (invisible) to 1.0f (opaque)
     */
    private float opacity = 0.0f;
    /**
     * MIME image format - should be defined in the WMS get capabilties
     * document, only makes sense for WMS layers
     */
    private String imageFormat = null;
    /**
     * The type of map layer we are - mostly used when generating JavaScript in
     * OpenLayersJavascript to specialcase the output
     */
    private int type = LayerUtilitiesImpl.UNSUPPORTED;
    /**
     * True if this is a baselayer (prevents anything from showing up in the
     * active layers box and tree menu
     */
    private boolean baseLayer = false;
    /**
     * True if this layer is currently being displayed on the map - attempting
     * to do this using clientside javascript will fail when items are removed
     * from the active layers list, etc
     */
    private boolean displayed = false;
    /**
     * True if the uri for the default style has been set - set internally when
     * the setDefaultStyleLegendUri method is called
     */
    private boolean defaultStyleLegendUriSet = false;
    /*
     * layer sub type. e.g. source
     */
    private int subType = LayerUtilitiesImpl.UNSUPPORTED;
    private String geoJSON = null;
    private String geometryWKT = null;
    /**
     * Supported rendering styles as advertised by the server are stored in this
     * list (WMS only)
     */
    private List<WMSStyle> styles = new ArrayList<WMSStyle>();
    /**
     * Animation parameters for netcdf files (only)
     */
    private MapLayerMetadata mapLayerMetadata = null;
    /**
     * True if this map layer is currently being displayed on the map as an
     * animation, otherwise false
     */
    private boolean currentlyAnimated = false;
    /*
     * Colour mode -1 = user defined 0 - 7 = scientific name to kingdom
     */
    private String colourMode = "-1";
    /*
     * alaspatial ref number for species record highlight flag
     */
    private String highlight = null;
    /*
     * display name
     */
    private String displayName = null;
    private ScatterplotDataDTO scatterplotDataDTO;
    /**
     * Child map layers - infinite depth is supported
     */
    private List<MapLayer> children = new ArrayList<MapLayer>();
    /**
     * The name of this layer as it is known on the mapserver. This is usually
     * obtained from the 'Name' element of a 'Layer' instance in a WMS
     * GetCapabilities document - only really makes sense for WMS layers at the
     * moment
     */
    private String layer = null;
    /*
     * flag to determine if this is a polygon layer
     */
    private boolean polygonLayer = false;
    /*
     * Flag indicating if this layer is removeable
     */
    private boolean removeable = true;
    private String areaSqKm;
    private Query speciesQuery;
    private List<Facet> facets;
    private String cache;
    private String envelope;
    private String spcode;
    private String poi;
    private String highlightState;
    private Integer animationStep;
    private Double animationInterval;
    private Integer lastYear;
    private Integer firstYear;
    private Integer classificationSelection;
    private Integer classificationGroupCount;
    private LegendObject legendObject;
    private String pid;
    /**
     * Layer Id added to enable Hibernate serialization Uses the Postgres Serial
     * data type on the database side
     */
    private long mapLayerId = 0;
    private long parentmaplayerid = 0;
    private long userMapId;
    private long maplayermetadataid;
    /**
     * The user's selection based on an AnimationParameters instance.
     */
    private boolean userDefinedLayer = false;
    private int geometryType;
    private boolean dynamicStyle = false;
    //parameters for the dynamic styled layers
    private int redVal;
    private int blueVal;
    private int greenVal;
    private int sizeVal;
    private boolean sizeUncertain;
    private boolean clustered;
    private String envColour;
    private String envName;
    private String envSize;
    /**
     * env params allow the user to dynamically style the Layer
     */
    private String envParams = null;
    private JSONArray classificationObjects;
    private String baseUri;

    /**
     * Constructor
     * <p/>
     * Creates a blank default style
     */
    public MapLayer() {
        // create a default style
        WMSStyle style = new WMSStyle();

        style.setName(StringConstants.DEFAULT);
        style.setDescription("Default style");
        style.setTitle(StringConstants.DEFAULT);

        styles.add(style);

        setMapLayerMetadata(new MapLayerMetadata());
    }

    public boolean defaultStyleLegendUriSet() {
        return this.defaultStyleLegendUriSet;
    }

    public String getGeoJSON() {
        return geoJSON;
    }

    public void setGeoJSON(String geoJSON) {
        this.geoJSON = geoJSON;
    }

    public String getWKT() {
        if (isPolygonLayer()
                && getType() != LayerUtilitiesImpl.WKT
                && geometryWKT == null
                && geoJSON == null) {
            //TODO: query for non-wkt layer geometry
            return null;
        } else {
            if (geometryWKT == null) {
                geometryWKT = Util.wktFromJSON(geoJSON);
            }
            return geometryWKT;
        }
    }

    public void setWKT(String wkt) {
        this.geometryWKT = wkt;
    }

    public int getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(int geometryType) {
        this.geometryType = geometryType;
    }

    public int getBlueVal() {
        return blueVal;
    }

    public void setBlueVal(int blueVal) {
        this.blueVal = blueVal;
    }

    public int getGreenVal() {
        return greenVal;
    }

    public void setGreenVal(int greenVal) {
        this.greenVal = greenVal;
    }

    public int getRedVal() {
        return redVal;
    }

    public void setRedVal(int redVal) {
        this.redVal = redVal;
    }

    public int getSizeVal() {
        return sizeVal;
    }

    public void setSizeVal(int sizeVal) {
        this.sizeVal = sizeVal;
    }

    public boolean getSizeUncertain() {
        return sizeUncertain;
    }

    public void setSizeUncertain(boolean sizeUncertain) {
        this.sizeUncertain = sizeUncertain;
    }

    public boolean isClustered() {
        return clustered;
    }

    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    public String getEnvColour() {
        return envColour;
    }

    public void setEnvColour(String envColour) {
        this.envColour = envColour;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvSize() {
        return envSize;
    }

    public void setEnvSize(String envSize) {
        this.envSize = envSize;
    }

    public boolean isDynamicStyle() {
        return dynamicStyle;
    }

    public void setDynamicStyle(boolean dynamicStyle) {
        this.dynamicStyle = dynamicStyle;
    }

    public String getEnvParams() {
        return envParams;
    }

    public void setEnvParams(String envParams) {
        this.envParams = envParams;
    }

    public boolean isUserDefinedLayer() {
        return userDefinedLayer;
    }

    public void setUserDefinedLayer(boolean userDefinedLayer) {
        this.userDefinedLayer = userDefinedLayer;
    }

    public long getMaplayermetadataid() {
        return maplayermetadataid;
    }

    public void setMaplayermetadataid(long maplayermetadataid) {
        this.maplayermetadataid = maplayermetadataid;
    }

    public long getParentmaplayerid() {
        return parentmaplayerid;
    }

    public void setParentmaplayerid(long parentmaplayerid) {
        this.parentmaplayerid = parentmaplayerid;
    }

    public long getUserMapId() {
        return userMapId;
    }

    public void setUserMapId(long userMapId) {
        this.userMapId = userMapId;
    }

    /**
     * Return the id of the maplayer
     *
     * @return
     */
    public long getMapLayerId() {
        return mapLayerId;
    }

    public void setMapLayerId(long mapLayerId) {
        this.mapLayerId = mapLayerId;
    }

    /**
     * Return the selected style or null if nothing has been selected
     *
     * @return
     */
    public WMSStyle getSelectedStyle() {
        WMSStyle style;
        if (selectedStyleIndex > STYLE_DEFAULT) {
            style = styles.get(selectedStyleIndex);
        } else {
            style = null;
        }
        return style;
    }

    /**
     * Get the stylename for use within the portal - should never return an
     * empty string (""), you should get the name of the default style instead,
     * for use populating the style comboboxes
     *
     * @return
     */
    public String getSelectedSystemStyleName() {
        return styles.get(selectedStyleIndex).getName();
    }

    /**
     * Return the name of the currently selected style or an empty string if
     * nothing has been selected yet.
     * <p/>
     * Empty string is returned as the default rendering style can be requested
     * from the wms server by asking for style=""
     *
     * @return
     */
    public String getSelectedStyleName() {
        String styleName;
        if (selectedStyleIndex > STYLE_DEFAULT) {
            return getSelectedSystemStyleName();
        } else {
            styleName = "";
        }
        return styleName;
    }

    /**
     * Return a URL to get the legend for the map layer we are currently
     * displaying, unless we have no style information (and thus no legend
     * available) in which case we return null;
     *
     * @return
     */
    public String getCurrentLegendUri() {
        String u;

        //check if its a dynamically generated style
        //if so go and go build the uri
        if (isDynamicStyle()) {
            u = styles.get(selectedStyleIndex).getLegendUri();
        } else {
            if (hasStyle()) {
                // show the selected style or default style if one wasn't set
                u = styles.get(selectedStyleIndex).getLegendUri();
            } else {
                /*
                 * if no styles are defined, there is likely no legend
                 * available, so dont attempt to retrieve one
                 */
                u = null;
            }
        }

        return u;
    }

    public int getSelectedStyleIndex() {
        return selectedStyleIndex;
    }

    public void setSelectedStyleIndex(int selectedStyleIndex) {
        this.selectedStyleIndex = selectedStyleIndex;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public String getUri() {
        if (cache != null) {
            return uri + "&CACHE=" + cache;
        }
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    /**
     * IDs for map layers are NOT unique until they are concatenated with the
     * layer name, because one discovery ID can generate several mapLayer
     * instances which then all share the same ID but have different layer names
     *
     * @return
     */
    public String getUniqueId() {
        return getId() + DELIM + layer + DELIM + getName();
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public List<MapLayer> getChildren() {
        return children;
    }

    public void setChildren(List<MapLayer> children) {
        this.children = children;
    }

    public MapLayer getChild(int i) {
        return children.get(i);
    }

    public boolean isLeaf() {
        return !hasChildren();
    }

    public int getChildCount() {
        return children.size();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void addChild(MapLayer c) {
        children.add(c);
    }

    public void addStyle(WMSStyle style) {
        styles.add(style);
    }

    public void addStyles(List<WMSStyle> styles) {
        this.styles.addAll(styles);
    }

    /**
     * Set a URI for the default map legend
     *
     * @param legendUri
     */
    public void setDefaultStyleLegendUri(String legendUri) {
        if (legendUri != null) {
            this.styles.get(STYLE_DEFAULT).setLegendUri(legendUri);
            defaultStyleLegendUriSet = true;
        }
    }

    public int getStyleCount() {
        return styles.size();
    }

    /**
     * True if at least 2 styles are available. The first style in the styles
     * list is always called 'default' and gets set in the constructor
     *
     * @return
     */
    public boolean hasStyles() {
        return styles.size() > 1;
    }

    /**
     * True if one or more styles is available (including the default style)
     * otherwise false
     *
     * @return
     */
    public boolean hasStyle() {
        return !styles.isEmpty();
    }

    public boolean isBaseLayer() {
        return baseLayer;
    }

    public void setBaseLayer(boolean baseLayer) {
        this.baseLayer = baseLayer;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    public List<WMSStyle> getStyles() {
        return styles;
    }

    public void setStyles(List<WMSStyle> styles) {
        this.styles = styles;
    }

    public boolean isDefaultStyleLegendUriSet() {
        return defaultStyleLegendUriSet;
    }

    public void setDefaultStyleLegendUriSet(boolean defaultStyleLegendUriSet) {
        this.defaultStyleLegendUriSet = defaultStyleLegendUriSet;
    }

    public boolean isRemoveable() {
        return removeable;
    }

    public void setRemoveable(boolean removeable) {
        this.removeable = removeable;
    }

    /**
     * Provide a deep copy of the object - equivalent to clone() only not
     * broken. This is to allow us to maintain a single list of layers obtained
     * from the map servers and then provide a copy of this list to clients
     * rather than rerequesting it for each session/page access
     *
     * @return
     */
    public Object clone() throws CloneNotSupportedException {
        MapLayer mapLayer = (MapLayer) super.clone();
        mapLayer.children = new ArrayList<MapLayer>();
        mapLayer.styles = new ArrayList<WMSStyle>();

        if (mapLayerId > 0) {
            mapLayer.mapLayerId = mapLayerId;
        }

        // copy any styles
        if (styles != null) {
            for (WMSStyle style : styles) {
                mapLayer.addStyle((WMSStyle) style.copy());
            }
        }

        // copy any children.. - when we use add child, the parent is correctly set
        if (children != null) {
            for (MapLayer child : children) {
                MapLayer clonedChild = (MapLayer) child.clone();
                mapLayer.addChild(clonedChild);
            }
        }

        // copy animation parameters - since they are layer specific
        // we can COPY instead of CLONEing
        if (mapLayerMetadata != null) {
            mapLayer.mapLayerMetadata
                    = mapLayerMetadata;
        }

        return mapLayer;
    }

    public MapLayerMetadata getMapLayerMetadata() {
        return mapLayerMetadata;
    }

    public void setMapLayerMetadata(MapLayerMetadata mapLayerMetadata) {
        this.mapLayerMetadata = mapLayerMetadata;
    }

    /**
     * Wrapper for getName() with javascript chars escaped
     *
     * @return
     */
    public String getNameJS() {
        return StringEscapeUtils.escapeJavaScript(getName());
    }

    /**
     * Wrapper for getUniqueId() with javascript chars escaped
     *
     * @return
     */
    public String getUniqueIdJS() {
        return StringEscapeUtils.escapeJavaScript(getUniqueId());
    }

    /**
     * Wrapper for getUri() with javascript chars escaped
     *
     * @return
     */
    public String getUriJS() {
        return StringEscapeUtils.escapeJavaScript(getUri());
    }

    /**
     * Wrapper for getCql() with javascript chars escaped
     *
     * @return
     */
    public String getCqlJS() {
        return StringEscapeUtils.escapeJavaScript(getCql());
    }

    /**
     * Wrapper for getLayer() with javascript chars escaped
     *
     * @return
     */
    public String getLayerJS() {
        return StringEscapeUtils.escapeJavaScript(getLayer());
    }

    /**
     * Wrapper for getSeleectedStyleName() with javascript chars escaped
     *
     * @return
     */
    public String getSelectedStyleNameJS() {
        return StringEscapeUtils.escapeJavaScript(getSelectedStyleName());
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String pid) {
        this.highlight = pid;
    }

    public String getColourMode() {
        return colourMode;
    }

    public void setColourMode(String string) {
        colourMode = string;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        displayName = name;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        setDisplayName(name);
    }

    public boolean isPolygonLayer() {
        return polygonLayer || subType == LayerUtilitiesImpl.ENVIRONMENTAL_ENVELOPE;
    }

    public void setPolygonLayer(boolean isPolygon) {
        polygonLayer = isPolygon;
    }

    public boolean isSpeciesLayer() {
        return speciesQuery != null;
    }

    public Query getSpeciesQuery() {
        return speciesQuery;
    }

    public void setSpeciesQuery(Query query) {
        this.speciesQuery = query;
    }

    public boolean isGridLayer() {
        return subType == LayerUtilitiesImpl.GRID;
    }

    public boolean isContextualLayer() {
        return subType == LayerUtilitiesImpl.CONTEXTUAL;
    }

    public int getSubType() {
        return subType;
    }

    public void setSubType(int type) {
        subType = type;
    }

    public String calculateAndStoreArea() {
        String area = new SelectedArea(this, null).getKm2Area();
        setAreaSqKm(area);
        return area;
    }

    public String getAreaSqKm() {
        return areaSqKm;
    }

    public void setAreaSqKm(String areaSqKm) {
        this.areaSqKm = areaSqKm;
    }

    public ScatterplotDataDTO getScatterplotDataDTO() {
        return scatterplotDataDTO;
    }

    public void setScatterplotDataDTO(ScatterplotDataDTO data) {
        scatterplotDataDTO = data;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    public String getHighlightState() {
        return highlightState;
    }

    public void setHighlightState(String state) {
        this.highlightState = state;
    }

    public String getPointsOfInterestWS() {
        return poi;
    }

    public void setPointsOfInterestWS(String poi) {
        this.poi = poi;
    }

    public LegendObject getLegendObject() {
        return legendObject;
    }

    public void setLegendObject(LegendObject lo) {
        this.legendObject = lo;
    }

    public Integer getClassificationGroupCount() {
        return classificationGroupCount;
    }

    public void setClassificationGroupCount(Integer classificationGroupCount) {
        this.classificationGroupCount = classificationGroupCount;
    }

    public Integer getClassificationSelection() {
        return classificationSelection;
    }

    public void setClassificationSelection(Integer groupSelection) {
        this.classificationSelection = groupSelection;
    }

    public Integer getFirstYear() {
        return firstYear;
    }

    public void setFirstYear(Integer firstYear) {
        this.firstYear = firstYear;
    }

    public Integer getLastYear() {
        return lastYear;
    }

    public void setLastYear(Integer lastYear) {
        this.lastYear = lastYear;
    }

    public Integer getAnimationStep() {
        return animationStep;
    }

    public void setAnimationStep(Integer step) {
        this.animationStep = step;
    }

    public Double getAnimationInterval() {
        return animationInterval;
    }

    public void setAnimationInterval(Double interval) {
        this.animationInterval = interval;
    }

    public String getSPCode() {
        return spcode;
    }

    public void setSPCode(String spcode) {
        this.spcode = spcode;
    }

    public String getEnvelope() {
        return envelope;
    }

    public void setEnvelope(String envelope) {
        this.envelope = envelope;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setClassificationObjects(JSONArray classificationObjects) {
        this.classificationObjects = classificationObjects;
    }

    public JSONArray getClassificationObjects() {
        return classificationObjects;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }
}
