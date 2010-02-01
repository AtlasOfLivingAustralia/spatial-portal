package au.org.emii.portal.composer;

import au.org.emii.portal.SearchQuery;
import au.org.emii.portal.composer.UtilityComposer;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Window;

public class LeftMenuSearchComposer extends UtilityComposer {

	private static final long serialVersionUID = 2540820748110129339L;
	private Combobox txtsearch;
	private Checkbox chkBBOX;
	private Checkbox chkDate;
	private Window searchpopup;
	private Datebox startdate;
	private Datebox enddate;
	private Doublebox north;
	private Doublebox south;
	private Doublebox east;
	private Doublebox west;
	private Div geoDiv;
	private Div dateDiv;

	public void doSearch() {

		boolean bDate = false;
		boolean chkSearch = true;

			if (txtsearch.getText().equals("")) {

				getMapComposer().showMessage("You must enter a search term");

			} else {
				SearchQuery sq = new SearchQuery();
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
							if (enddate.getValue().before(startdate.getValue())) {
								chkSearch = false;
								getMapComposer().showMessage(
									"Please select a valid date range"
								);
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
					
					if (searchpopup == null) {
                                                logger.debug("new window");
                                                searchpopup = new Window();
                                                searchpopup = (Window) Executions.createComponents(
								"/WEB-INF/zul/search.zul", null, null);
					} 
                                        else {

                                                logger.debug("existing window");
						searchpopup.detach();
						searchpopup = (Window) Executions.createComponents(
								"/WEB-INF/zul/search.zul", null, null);
					}

                                        searchpopup.setId(java.util.UUID.randomUUID().toString());
					searchpopup.setMaximizable(true);
					searchpopup.setPosition("center");
					searchpopup.doOverlapped();
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

	public void onClick$btnSearch(Event event) {
		doSearch();
	}

}
