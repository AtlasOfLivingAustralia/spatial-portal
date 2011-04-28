package au.org.emii.portal.menu;

import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import au.org.emii.portal.wms.WMSStyle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.plot.XYPlot;

/**
 * Representation of a map layer - used for both rendering map layers
 * and building the menu system.
 *
 * A tree structured menu is built by adding children with addChild.
 * Each node of the tree knows about it's parent and child objects so
 * it is possible to walk the tree from any given node.  The root node
 * is identifiable because it has a null parent.  Leaf nodes are
 * identified by either checking for the presence of children or by
 * calling the convenience method isLeaf()
 *
 * IMPORTANT - READ NOTES HERE BEFORE MODIFYING THIS CLASS!!
 * =========================================================
 * IF YOU ADD FIELDS TO THIS CLASS THAT AREN'T SIMPLE PRIMATIVE TYPES, MAKE SURE
 * YOU UPDATE THE clone() METHOD TO ENSURE YOUR CHANGES ARE PICKED UP WHEN THE
 * MAPLAYERS ARE COPIED FOR SESSION CREATION!
 *
 *
 * @author geoff
 *
 */
public class MapLayer extends AbstractIdentifierImpl implements TreeMenuValue, Cloneable, Serializable {

    private SettingsSupplementary settingsSupplementary = null;
    private static final long serialVersionUID = 1L;
    /**
     * Used as a value for selectedStyleIndex to
     * indicate that the default rendering style
     * should be used
     */
    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    public final static int STYLE_DEFAULT = 0;
    /**
     * Delimiter used when generating map layer ids
     */
    protected final static String DELIM = "::";
    /**
     * True if this item is currently being displayed in the active layers list
     */
    private boolean listedInActiveLayers = false;
    /**
     * Reference to containing parent if there is one or null if this is the
     * top layer
     */
    private MapLayer parent = null;
    /**
     * Child map layers - infinite depth is supported
     */
    private List<MapLayer> children = new ArrayList<MapLayer>();
    /**
     * The name of this layer as it is known on the mapserver.  This is usually
     * obtained from the 'Name' element of a 'Layer' instance in a WMS GetCapabilities
     * document - only really makes sense for WMS layers at the moment
     */
    private String layer = null;
    /**
     * It is possible to display this layer on the map
     */
    protected boolean displayable = false;
    /**
     * Can this layer handle WFS queries
     */
    protected boolean queryable = false;
    /**
     * URI to fetch WMS data from
     */
    protected String uri = null;

    /*
     * geoserver Common Query Language
     */
    protected String cql = null;
    /**
     * Opacity on map - range 0.0f (invisible) to 1.0f (opaque)
     */
    protected float opacity = 0.0f;
    /**
     * MIME image format - should be defined in the WMS get capabilties document,
     * only makes sense for WMS layers
     */
    protected String imageFormat = null;
    /**
     * The type of map layer we are - mostly used when generating JavaScript in
     * OpenLayersJavascript to specialcase the output
     */
    protected int type = LayerUtilitiesImpl.UNSUPPORTED;
    /**
     * True if this is a baselayer (prevents anything from showing up in the
     * active layers box and tree menu
     */
    protected boolean baseLayer = false;
    /**
     * True if this layer is currently being displayed on the map - attempting to
     * do this using clientside javascript will fail when items are removed from
     * the active layers list, etc
     */
    protected boolean displayed = false;
    /**
     * True if the uri for the default style has been set - set
     * internally when the setDefaultStyleLegendUri method is
     * called
     */
    protected boolean defaultStyleLegendUriSet = false;
    /*
     * Colour mode
     * -1 = user defined
     * 0 - 7 = scientific name to kingdom
     */
    String colourMode = "-1";
    /*
     * alaspatial ref number for species record highlight flag
     */
    String highlight = null;
    /*
     * data for 'dummy' layers
     */
    HashMap<String, Object> data = new HashMap<String, Object>();
    /*
     * display name
     */
    String displayName = null;
    /*
     * flag to determine if this is a polygon layer
     */
    private boolean polygonLayer = false;
    /*
     * layer sub type.  e.g. source
     */
    protected int subType = LayerUtilitiesImpl.UNSUPPORTED;

    public void setDefaultStyleLegendUriSet(boolean defaultStyleLegendUriSet) {
        this.defaultStyleLegendUriSet = defaultStyleLegendUriSet;
    }

    public boolean DefaultStyleLegendUriSet() {
        return this.defaultStyleLegendUriSet;
    }
    protected String geoJSON = null;

    public String getGeoJSON() {
        return geoJSON;
    }

    public void setGeoJSON(String geoJSON) {
        this.geoJSON = geoJSON;
    }
    protected String geometryWKT = null;

    public String getWKT() {
        if(isPolygonLayer() && getType() != LayerUtilities.WKT
                && geometryWKT == null) {
            //TODO: query for non-wkt layer geometry
            return null;
        } else {
            return geometryWKT;
        }
    }

    public void setWKT(String wkt) {
        this.geometryWKT = wkt;
    }
    /**
     * Supported rendering styles as advertised by the server
     * are stored in this list (WMS only)
     */
    protected List<WMSStyle> styles = new ArrayList<WMSStyle>();
    /**
     * Pointer to the element currently marked as selected in the
     * styles list - STYLE_DEFAULT means ignore all style information
     * and use server defaults until the user tells us otherwise
     */
    protected int selectedStyleIndex = STYLE_DEFAULT;
    /**
     * Animation parameters for netcdf files (only)
     */
    protected MapLayerMetadata mapLayerMetadata = null;
    /**
     * The user's selection based on an AnimationParameters instance.
     */
    protected AnimationSelection animationSelection = null;
    /**
     * True if this map layer is currently being displayed on the
     * map as an animation, otherwise false
     */
    protected boolean currentlyAnimated = false;
    /**
     * Layer Id added to enable Hibernate serialization
     * Uses the Postgres Serial data type on the database side
     */
    private long mapLayerId = 0;
    private long parentmaplayerid = 0;
    private long userMapId;
    private long maplayermetadataid;
    private boolean userDefinedLayer = false;
    private int geometryType;

    public int getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(int geometryType) {
        this.geometryType = geometryType;
    }
    private boolean dynamicStyle = false;
    //parameters for the dynamic styled layers
    private int redVal;
    private int blueVal;
    private int greenVal;
    private int sizeVal;
    private boolean sizeUncertain;
    private boolean clustered;

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
    private String envColour;
    private String envName;
    private String envSize;

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
    /** env params allow the user to dynamically style the
     * Layer
     */
    private String envParams = null;

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
     * Constructor
     *
     * Creates a blank default style
     */
    public MapLayer() {
        // create a default style
        WMSStyle style = new WMSStyle();

        style.setName("Default");
        style.setDescription("Default style");
        style.setTitle("Default");

        styles.add(style);
    }

    /**
     * Return the id of the maplayer
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
     * Get the stylename for use within the portal - should
     * never return an empty string (""), you should get the
     * name of the default style instead, for use populating
     * the style comboboxes
     * @return
     */
    public String getSelectedSystemStyleName() {
        return styles.get(selectedStyleIndex).getName();
    }

    /**
     * Return the name of the currently selected style or an
     * empty string if nothing has been selected yet.
     *
     * Empty string is returned as the default rendering style
     * can be requested from the wms server by asking for
     * style=""
     *
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
     * displaying, unless we have no style information (and thus no
     * legend available) in which case we return null;
     * @return
     */
    public String getCurrentLegendUri() {
        String uri;

        //check if its a dynamically generated style
        //if so go and go build the uri

        if (isDynamicStyle()) {
            uri = styles.get(selectedStyleIndex).getLegendUri();
        } else {
            if (hasStyle()) {
                // show the selected style or default style if one wasn't set
                uri = styles.get(selectedStyleIndex).getLegendUri();
            } else {
                /* if no styles are defined, there is likely no legend
                 * available, so dont attempt to retrieve one
                 */
                uri = null;
            }
        }

        logger.debug("8888888888888888888888888888888888888888888888");
        logger.debug(uri);
        return uri;
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

    public boolean isNcWmsType() {
        return (type == LayerUtilitiesImpl.NCWMS) ? true : false;
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

    public boolean isQueryable() {
        return queryable;
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public String getCql() {
        return cql;
    }

    /**
     * IDs for map layers are NOT unique until they are concatenated with the
     * layer name, because one discovery ID can generate several mapLayer
     * instances which then all share the same ID but have different layer
     * names
     * @return
     */
    public String getUniqueId() {
        return getId() + DELIM + layer;
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
        return children.size() > 0;
    }

    public void addChild(MapLayer c) {
        c.setParent(this);
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
     * True if at least 2 styles are available.  The first style in the styles
     * list is always called 'default' and gets set in the constructor
     * @return
     */
    public boolean hasStyles() {
        return (styles.size() > 1);
    }

    /**
     * True if one or more styles is available (including the default style)
     * otherwise false
     * @return
     */
    public boolean hasStyle() {
        return (styles.size() > 0);
    }

    public boolean isListedInActiveLayers() {
        return listedInActiveLayers;
    }

    public void setListedInActiveLayers(boolean listedInActiveLayers) {
        this.listedInActiveLayers = listedInActiveLayers;
    }

    public MapLayer getParent() {
        return parent;
    }

    public void setParent(MapLayer parent) {
        this.parent = parent;
    }

    public boolean isDisplayable() {
        return displayable;
    }

    public void setDisplayable(boolean displayable) {
        this.displayable = displayable;
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

    public boolean isDefaultStyleLegendUriSet() {
        return defaultStyleLegendUriSet;
    }

    /**
     * Recursive search for a MapLayer instance matching a given id.
     *
     * CAREFUL! MapLayer IDs are not plain strings, they are formed by
     * concatenating some variables - see method getId() for details
     *
     * @param uri
     * @return
     */
    public MapLayer findByLayer(String layer) {
        MapLayer match = null;

        if (getLayer() == null && layer == null) {
            match = this;
        } else if ((getLayer() != null) && (getLayer().equals(layer))) {
            match = this;
        } else {
            int i = 0;
            while ((match == null) && (i < getChildCount())) {
                match = getChild(i).findByLayer(layer);
                i++;
            }
        }
        return match;
    }

    public MapLayer findByUriAndLayer(String uri, String layer) {
        MapLayer match = null;

        if ((getUri() != null)
                && getUri().equals(uri)
                && (getLayer() != null)
                && getLayer().equals(layer)) {
            match = this;
        } else {
            int i = 0;
            while ((match == null) && (i < getChildCount())) {
                match = getChild(i).findByUriAndLayer(uri, layer);
                i++;
            }
        }
        return match;
    }

    public MapLayer findById(String id) {
        MapLayer match = null;

        if (getId().equals(id)) {
            match = this;
        } else {
            int i = 0;
            while ((match == null) && (i < getChildCount())) {
                match = getChild(i).findById(id);
                i++;
            }
        }
        return match;
    }

    /**
     * Provide a deep copy of the object - equivalent to clone() only not broken.  This is to allow
     * us to maintain a single list of layers obtained from the map servers and then provide a copy
     * of this list to clients rather than rerequesting it for each session/page access
     * @return
     */
    public Object clone() throws CloneNotSupportedException {
        MapLayer mapLayer = (MapLayer) super.clone();
        mapLayer.parent = null;
        mapLayer.children = new ArrayList<MapLayer>();
        mapLayer.styles = new ArrayList<WMSStyle>();

        if (mapLayerId > 0) {
            mapLayer.mapLayerId = mapLayerId;
        }

        // copy any styles
        if (styles != null) {
            for (WMSStyle style : styles) {
                mapLayer.addStyle((WMSStyle) style.clone());
            }
        }

        // copy any children.. - when we use add child, the parent is correctly set
        if (children != null) {
            for (MapLayer child : children) {
                MapLayer clonedChild = (MapLayer) child.clone();
                clonedChild.setParent(mapLayer);
                mapLayer.addChild(clonedChild);
            }
        }

        // copy animation parameters - since they are layer specific
        // we can COPY instead of CLONEing
        if (mapLayerMetadata != null) {
            mapLayer.mapLayerMetadata =
                    (MapLayerMetadata) mapLayerMetadata;
        }

        // copy data objects
        mapLayer.data = new HashMap<String, Object>();
        if(data != null) {
            for(Entry<String, Object> entry : mapLayer.data.entrySet()) {
                mapLayer.data.put(entry.getKey(), entry.getValue());
            }
        }
        
        return mapLayer;
    }

    /**
     * Return a string representation of this menu as ascii art
     */
    public String dump(String indent) {
        String value =
                "ML" + indent + getName() + "(" + getId() + ") (parent="
                + ((parent == null) ? "null" : parent.getId()) + ")\n";
        for (MapLayer child : children) {
            value += child.dump(indent + "    ");
        }
        return value;
    }

    public Object getChild(Object parent, int index) {
        return ((TreeMenuItem) parent).getChild(index);
    }

    public boolean isLeaf(Object parent) {
        return ((TreeMenuItem) parent).isLeaf();
    }

    public int getChildCount(Object parent) {
        return ((TreeMenuItem) parent).getChildCount();
    }

    public MapLayerMetadata getMapLayerMetadata() {
        return mapLayerMetadata;
    }

    public void setMapLayerMetadata(MapLayerMetadata mapLayerMetadata) {
        this.mapLayerMetadata = mapLayerMetadata;
    }

    public AnimationSelection getAnimationSelection() {
        return animationSelection;
    }

    public void setAnimationSelection(AnimationSelection animationSelection) {
        this.animationSelection = animationSelection;
    }

    public boolean isCurrentlyAnimated() {
        return currentlyAnimated;
    }

    public void setCurrentlyAnimated(boolean currentlyAnimated) {
        this.currentlyAnimated = currentlyAnimated;
    }

    /**
     * Get a list of all layerNames for this object
     * @return
     */
    public List<String[]> getAllLayerNames() {
        List<String[]> list = new ArrayList<String[]>();

        // add ourself...
        getAllLayerNames(list);

        return list;
    }

    private void getAllLayerNames(List<String[]> list) {

        // skip any null layer ids or just not displayable - eg for grouping layers
        if (layer != null && displayable) {
            list.add(new String[]{layer, getName()});
        }
        // then all children
        for (MapLayer child : children) {
            child.getAllLayerNames(list);
        }

    }

    public boolean isSupportsAnimation() {
        return (mapLayerMetadata == null)
                ? false : mapLayerMetadata.isSupportsAnimation();
    }

    /**
     * Append append to description
     * @param append
     * @param recursive
     */
    public void appendDescription(String append, boolean recursive) {
        // the leading space in the .properties file seems to go AWOL
        setDescription(getDescription() + " " + append);
        if (recursive) {
            for (MapLayer child : children) {
                child.appendDescription(append, recursive);
            }
        }
    }

    /**
     * Wrapper for getName() with javascript chars escaped
     * @return
     */
    public String getNameJS() {
        return StringEscapeUtils.escapeJavaScript(getName());
    }

    /**
     * Wrapper for getUniqueId() with javascript chars escaped
     * @return
     */
    public String getUniqueIdJS() {
        return StringEscapeUtils.escapeJavaScript(getUniqueId());
    }

    /**
     * Wrapper for getUri() with javascript chars escaped
     * @return
     */
    public String getUriJS() {
        return StringEscapeUtils.escapeJavaScript(getUri());
    }

    /**
     * Wrapper for getCql() with javascript chars escaped
     * @return
     */
    public String getCqlJS() {
        return StringEscapeUtils.escapeJavaScript(getCql());
    }

    /**
     * Wrapper for getLayer() with javascript chars escaped
     * @return
     */
    public String getLayerJS() {
        return StringEscapeUtils.escapeJavaScript(getLayer());
    }

    /**
     * Wrapper for getSeleectedStyleName() with javascript chars escaped
     * @return
     */
    public String getSelectedStyleNameJS() {
        return StringEscapeUtils.escapeJavaScript(getSelectedStyleName());
    }

    public void setChildren(List<MapLayer> children) {
        this.children = children;
    }

    public void setStyles(List<WMSStyle> styles) {
        this.styles = styles;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String pid) {
        this.highlight = pid;
    }

    public void setColourMode(String string) {
        colourMode = string;
    }

    public String getColourMode() {
        return colourMode;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public void setData(String key, Object value) {
        data.put(key, value);
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

    public void setPolygonLayer(boolean isPolygon) {
        polygonLayer = isPolygon;
    }

    public boolean isPolygonLayer() {
        return polygonLayer || subType == LayerUtilities.ENVIRONMENTAL_ENVELOPE;
    }

    public boolean isSpeciesLayer() {
        return (mapLayerMetadata != null && mapLayerMetadata.getSpeciesLsid() != null);
    }

    public boolean isGridLayer() {
        return subType == LayerUtilities.GRID;
    }

    public boolean isContextualLayer() {
        return subType == LayerUtilities.CONTEXTUAL;
    }

    public int getSubType() {
        return subType;
    }

    public void setSubType(int type) {
        subType = type;
    }
}
