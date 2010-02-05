package au.org.ala.portal;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.bcel.generic.LSTORE;
import org.apache.commons.io.IOUtils;
import net.opengis.wms.LayerDocument.Layer;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;

import au.org.emii.portal.HttpConnection;
import au.org.emii.portal.MapComposer;
import au.org.emii.portal.MapLayerMetadata;
import au.org.emii.portal.UtilityComposer;

/**
 * @author brendon
 *
 */
public class SpeciesNameSearchController extends UtilityComposer {

	/**
	 * run the species searches
	 */
	private static final long serialVersionUID = -7475055275833653831L;
	private static final String sCommon = "common";
	private static final String sScientific = "scientific";
	// TODO these need to be sourced from the config file
	private static final String sURL = "http://data.ala.org.au";
	private static final String commonSearch = "http://data.ala.org.au/search/commonNames/";
	private static final String scientificSearch = "http://data.ala.org.au/search/scientificNames/";
	private String mapWMS = "http://data.ala.org.au/mapping/simple/?id=";

	
	private Session sess = (Session) Sessions.getCurrent();
	private Label lblRecordCount;
	private Listbox lbResultCommon;
	private Listbox lbResultScientific;
	private Div pagingDiv;
	private int iPageNo = 1;
	private Intbox pageNo;
	private Label numberOfPages;
	private Window searchResults;
	private Button addToMap;
	private int TotalRecords;
	private String sSearchTerm;
	private String sSearchType;

	private URLConnection connection = null;

	public void afterCompose() {
		super.afterCompose();
		sSearchTerm = (String) sess.getAttribute("searchTerm");
		sSearchType = (String) sess.getAttribute("searchType");
		runSearch(sSearchTerm, sSearchType, getURI());
	}
	
	private String getURI(){
		String uri;
		if (sSearchType.equals(sCommon)) {
			uri = commonSearch;
		} else {
			uri = scientificSearch;
		}

		// add the search term to the uri and append the required json to it
		uri = uri + forURL(sSearchTerm) + "/json";
		
		return uri;
	}
	

	public void runSearch(String sSearchTerm, String sSearchType, String uri) {
	
		String json = null;
		json = setupConnection(uri);

		try {
			if (json != null) {
				if (sSearchType.equals(sCommon)) {
					TaxaCommonSearchSummary tcs = searchCommon(json);
					renderCommonResults(tcs);
				} else {
					TaxaScientificSearchSummary tss = searchScientific(json);
					renderScientificResults(tss);
				}
			} else {
				showNoResult();
			}
		} catch (JSONException e) {
			logger.error(e.getMessage() + " trying to parse JSON from " + json);
		}

	}

	public void showNoResult() {
		lblRecordCount.setValue("Sorry no matching results found");
		lbResultScientific.setVisible(false);
		lbResultCommon.setVisible(false);
	}

	public TaxaScientificSearchSummary searchScientific(String json) {

		TaxaScientificSearchSummary tss = new TaxaScientificSearchSummary();
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setRootClass(TaxaScientificSearchSummary.class);
		jsonConfig.setJavaPropertyFilter(new PropertyFilter() {
			@Override
			public boolean apply(Object source, String name, Object value) {
				if ("result".equals(name)) {
					return true;
				}
				return false;
			}
		});

		JSONObject jo = JSONObject.fromObject(json);

		tss = (TaxaScientificSearchSummary) JSONSerializer.toJava(jo,
				jsonConfig);

		if (tss.getRecordsReturned() > 1) {
			JSONArray joResult = jo.getJSONArray("result");
			JsonConfig jsonConfigResult = new JsonConfig();
			jsonConfigResult.setRootClass(TaxaScientificSearchResult.class);

			for (int i = 0; i < joResult.size(); i++) {
				TaxaScientificSearchResult tr = (TaxaScientificSearchResult) JSONSerializer
						.toJava(joResult.getJSONObject(i), jsonConfigResult);
				tss.addResult(tr);
			}
		}

		return tss;

	}

	public TaxaCommonSearchSummary searchCommon(String json) {

		TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
		jsonConfig.setJavaPropertyFilter(new PropertyFilter() {
			@Override
			public boolean apply(Object source, String name, Object value) {
				if ("result".equals(name)) {
					return true;
				}
				return false;
			}
		});

		JSONObject jo = JSONObject.fromObject(json);

		tss = (TaxaCommonSearchSummary) JSONSerializer.toJava(jo, jsonConfig);

		if (tss.getRecordsReturned() > 1) {

			JSONArray joResult = jo.getJSONArray("result");

			JsonConfig jsonConfigResult = new JsonConfig();
			jsonConfigResult.setRootClass(TaxaCommonSearchResult.class);

			for (int i = 0; i < joResult.size(); i++) {
				TaxaCommonSearchResult tr = (TaxaCommonSearchResult) JSONSerializer
						.toJava(joResult.getJSONObject(i), jsonConfigResult);
				tss.addResult(tr);
			}
		}

		return tss;

	}

	public String setupConnection(String uri) {
		InputStream in = null;
		String json = null;
		try {
			connection = HttpConnection.configureURLConnection(uri);
			in = connection.getInputStream();
			json = IOUtils.toString(in);

		} catch (IOException e) {
			logger.error("IO error fetching search URI: " + uri, e);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return json;
	}

	public void renderScientificResults(TaxaScientificSearchSummary tss) {
		lbResultCommon.setVisible(false);
		lbResultScientific.setVisible(true);
		lbResultScientific.getItems().clear();
		TotalRecords = tss.getTotalRecords();
		lblRecordCount.setValue(String.valueOf(TotalRecords) + " results for " + sSearchTerm);
		setPagingBox();

		if (TotalRecords > 1) {

			for (TaxaScientificSearchResult tr : tss.getResultList()) {
				Listitem li = new Listitem();
				Listcell lc = new Listcell();
				
				//extract the entityId from the url
				String sId = tr.getScientificNameUrl().substring(9);

				// Add hyperlink to ala scientificName info page
				Toolbarbutton tbFull = new Toolbarbutton();
				tbFull.setLabel(tr.getScientificName());
				tbFull.setSclass("z-label");
				tbFull.setHref(sURL + tr.getScientificNameUrl());
				tbFull.setTarget("_blank");

				lc.appendChild(tbFull);
				li.appendChild(lc);

				// Add scientific name
				lc = new Listcell();
				Label lb = new Label();
				lb.setValue(tr.getAuthor());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add taxa rank
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getRank());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add Family
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getFamily());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add Kingdom
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getKingdom());
				lc.appendChild(lb);
				li.appendChild(lc);
				
				//Add to  map button
				lc = new Listcell();
				Button btn = new Button();
				btn.setLabel("Add to map");
				btn.addEventListener("onClick", new myOnClickEventListener());
				btn.setId(sId);
				Label ln = new Label();
				ln.setValue(tr.getScientificName());
				ln.setVisible(false);
				ln.setId("ln" + sId);
				lc.appendChild(btn);
				lc.appendChild(ln);
				li.appendChild(lc);

				// add the row to the listbox
				lbResultScientific.appendChild(li);

			}
		} else {
			showNoResult();
		}

	}
	
	public void setPagingBox(){
		int iPages = 1;
		
		if (TotalRecords == 10) {
			iPages = 1;
		} else {
			iPages = (TotalRecords / 10) + 1;
		}
		
		if (iPages == 1) {
			pagingDiv.setVisible(false);
		} else {
			pageNo.setValue(iPageNo);
			numberOfPages.setValue(String.valueOf(iPages));
		}
	}

	public void renderCommonResults(TaxaCommonSearchSummary tss) {

		lbResultCommon.setVisible(true);
		lbResultScientific.setVisible(false);
		lbResultCommon.getItems().clear();
		TotalRecords = tss.getTotalRecords();
		lblRecordCount.setValue(String.valueOf(TotalRecords) + " results for " + sSearchTerm);
		
		setPagingBox();

		if (TotalRecords > 1) {

			for (TaxaCommonSearchResult tr : tss.getResultList()) {
				Listitem li = new Listitem();
				Listcell lc = new Listcell();
				
				//extract the entityId from the url
				String sId = tr.getCommonNameUrl().substring(9);
				int id = sId.indexOf("/");
				sId = sId.substring(0, id);
				logger.debug(sId);
				

				// Add hyperlink to ala commonName info page
				Toolbarbutton tbFull = new Toolbarbutton();
				tbFull.setLabel(tr.getCommonName());
				tbFull.setSclass("z-label");
				tbFull.setHref(sURL + tr.getCommonNameUrl());
				tbFull.setTarget("_blank");

				lc.appendChild(tbFull);
				li.appendChild(lc);

				// Add scientific name
				lc = new Listcell();
				Label lb = new Label();
				lb.setValue(tr.getScientificName());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add taxa rank
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getRank());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add Family
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getFamily());
				lc.appendChild(lb);
				li.appendChild(lc);

				// Add Kingdom
				lc = new Listcell();
				lb = new Label();
				lb.setValue(tr.getKingdom());
				lc.appendChild(lb);
				li.appendChild(lc);
				
				try {
				
				//Add to  map button
				lc = new Listcell();
				Button btn = new Button();
				btn.setLabel("Add to map");
				btn.addEventListener("onClick", new myOnClickEventListener());
				btn.setId(sId);
				Label ln = new Label();
				ln.setValue(tr.getScientificName());
				ln.setVisible(false);
				ln.setId("ln" + sId);
				lc.appendChild(btn);
				lc.appendChild(ln);
				li.appendChild(lc);
				

				// add the row to the listbox
				lbResultCommon.appendChild(li);
				} catch (UiException ex) {
					//tried to add one that already exists
				}
			}
		} else {
			showNoResult();
		}

	}

	public void onOK$pageNo(Event event) {
		String sURL = "?startIndex=";

		int i = pageNo.getValue();
		// checks for stupidity
		int x = (TotalRecords / 10) + 1;
		if (i < 1) {
			i = 1;
			pageNo.setValue(i);
		}

		if (i > x) {
			i = x;
			pageNo.setValue(i);
		}
		iPageNo = i;
		int start = (i * 10) - 9;
		
		//get the corresponding set of 10
		sURL = getURI() + sURL + String.valueOf(start);
		runSearch(sSearchTerm, sSearchType, sURL);
	}
	
	public void AddToMap() {
		
	}
	
	
	
	/**
	 * Gets the main pages controller so we can add a
	 * layer to the map
	 * @return MapComposer = map controller class
	 */
	private MapComposer getThisMapComposer() {
		
		MapComposer mapComposer = null;
		Page page = searchResults.getPage();
		mapComposer = (MapComposer) page.getFellow("mapPortalPage");

		return mapComposer;
	}
	
	
	/**
	 * string checking code, for spaces and special characters
	 * @param aURLFragment
	 * @return String
	 */
	
	public static String forURL(String aURLFragment){
	     String result = null;
	     try {
	       result = URLEncoder.encode(aURLFragment, "UTF-8");
	     }
	     catch (UnsupportedEncodingException ex){
	       throw new RuntimeException("UTF-8 not supported", ex);
	     }
	     return result;
	   }
	
	
	
	public final class  myOnClickEventListener implements EventListener {
		@Override
		public void onEvent(Event event) throws Exception {
			String label = null;
			String uri = null;
			String filter = null;
			String entity = null;
			
			uri = "http://maps.ala.org.au/wms?LAYERS=ala%3AtabDensityLayer&srs=EPSG:4326&VERSION=1.0.0&TRANSPARENT=true&FORMAT=image%2Fpng";
			
			//get the entity value from the button id
			entity = event.getTarget().getId();
			//get the scientific name from the hidden label
			Label ln = (Label) event.getTarget().getFellow("ln" + entity);
			label = ln.getValue();
			
			//get the current MapComposer instance
			MapComposer mc = getThisMapComposer();

			//contruct the filter
			filter = "<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+mapWMS+entity+"&type=1&unit=1]]></Literal></PropertyIsEqualTo></Filter>";
			
			mc.addWMSLayer(label, uri, 1, filter);

		}
	}

}
