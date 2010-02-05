/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
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
	private String searchURL;

	public void buildResultBox() {

		// remove any that already exist
		box.getItems().clear();

		// ok lets populate the listbox with the results
		for (Dataset d : datasets) {
			Listitem li = new Listitem();
			Listcell lc = new Listcell();
			Vbox vb = new Vbox();
			Hbox hb = new Hbox();
			Label title = new Label();

			title.setSclass("h2");
			title.setValue(d.getTitle());

			Toolbarbutton tbFull = new Toolbarbutton();
			tbFull.setLabel("Full record");
			tbFull.setSclass("z-label");
			tbFull.setHref(searchURL + d.getMestrecord());
			tbFull.setTarget("_blank");
			Separator s = new Separator();

			Toolbarbutton tbMore = new Toolbarbutton();
			tbMore.setSclass("z-label");
			tbMore.setLabel("More info..");

			MoreInfoEvent mie = new MoreInfoEvent();
			tbMore.addEventListener("onClick", mie);

			Label la = new Label(d.getAbstract());
			la.setPre(true);
			la.setMultiline(true);

			Div di = new Div();
			di.setId(d.getIdentifier());
			di.setVisible(false);

			hb.appendChild(title);
			hb.appendChild(tbFull);
			hb.appendChild(s);
			hb.appendChild(tbMore);
			vb.appendChild(hb);
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

		boolean bCheck = mc
				.addWMSServer(serverName, s, version, opacity, false);

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
		mfts.GetResultFromTo(start, end);
		datasets = mfts.getDatasets();

		buildResultBox();
	}

	public void afterCompose() {
		super.afterCompose();

		PortalSession portalSession = getPortalSession();

		sq = (SearchQuery) sess.getAttribute("searchquery");
		searchURL = portalSession.getSelectedSearchCatalogue().getProtocol()
				+ "://" + portalSession.getSelectedSearchCatalogue().getUri();
		searchURL += "/geonetwork/srv/en";

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
				numberOfPages.setValue(String.valueOf(iPages));
			}
			searcher.setTitle("MEST Search - " + s + " results");

			buildResultBox();

		}

	}

	private class MoreInfoEvent implements EventListener {

		public void onEvent(Event event) throws UiException {
			try {
				
				Toolbarbutton tb = (Toolbarbutton) event.getTarget();
				Button mapButton = new Button();
				Button wmsButton = new Button();
				Listitem li = (Listitem) tb.getParent().getParent().getParent()
						.getParent();
				box.setSelectedIndex(li.getIndex());
				tb.setDisabled(true);

				// ok now we have the selection index we can query the mest for
				// the full record
				// parse what we need out of it and add the links & buttons
				Dataset d = datasets.get(li.getIndex());
				Div dv = (Div) Path.getComponent("/searcher/" + d.identifier);
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
}
