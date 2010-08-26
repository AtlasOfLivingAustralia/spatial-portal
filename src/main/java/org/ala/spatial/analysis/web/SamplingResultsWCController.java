package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

//import org.ala.spatial.analysis.tabulation.SpeciesListIndex;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchResult;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkmax.zul.Filedownload;
import org.zkoss.zul.Button;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listgroup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleGroupsModel;

/**
 *
 * @author ajay
 */
public class SamplingResultsWCController extends UtilityComposer {

    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
    private Radio rdoCommonSearch;
    private SpeciesAutoComplete sac;
    private Listbox lbenvlayers;
    private Button btnMapSpecies;
    private Button btnPreview;
    private Popup results;
    private Popup p;
    private Html h;
    public Rows results_rows;
    private Button btnDownload;
    public Label samplingresultslabel;
    private List layers;
    private Map layerdata;
    private String selectedLayer;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;
    private MapComposer mc;
    private String geoServer = "http://spatial-dev.ala.org.au";  // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;
    private String user_polygon = "";
    private String[] groupLabels = null;
    Button pullFromActiveLayers;
    Checkbox useArea;
    LayersUtil layersUtil;

    public SamplingWCController parent = null;

    public void onClick$btnDownload(Event event) {
        parent.download();
    }    
}
