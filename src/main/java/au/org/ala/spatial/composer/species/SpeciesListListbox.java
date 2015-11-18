package au.org.ala.spatial.composer.species;

import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.dto.SpeciesListDTO;
import au.org.ala.spatial.dto.SpeciesListItemDTO;
import au.org.ala.spatial.util.BiocacheQuery;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.SpeciesListUtil;
import au.org.ala.spatial.util.Util;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;
import org.zkoss.zul.event.ListDataEvent;
import org.zkoss.zul.ext.Sortable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A reusable species list listbox.  To be used in all sections that need to reference
 * species lists.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SpeciesListListbox extends Listbox {
    private static final Logger LOGGER = Logger.getLogger(SpeciesListListbox.class);

    //stores the selected lists
    private List<String> selectedLists = new ArrayList<String>();

    private List<SpeciesListDTO> currentLists;

    public SpeciesListListbox() {
        init();
    }

    /**
     * An event that is fired to inform interested components that the number of check boxes selected has changed.
     */
    private void postCheckBoxStatusChanged() {
        Events.sendEvent(new Event(StringConstants.ONSICHECKBOXCHANGED, this, selectedLists.size()));
    }

    public List<String> getSelectedLists() {
        return selectedLists;
    }

    /**
     * Returns a title that can be used to group the selected items.
     * TODO: Maybe we should have a dynamic name based on the selected lists.
     *
     * @return
     */
    public String getTitle() {
        return "Species List Items";
    }

    public void init() {
        setItemRenderer(new ListitemRenderer() {
            @Override
            public void render(Listitem li, Object data, int itemIdx) {
                if (data == null) {
                    return;
                }
                final SpeciesListDTO item = (SpeciesListDTO) data;
                li.setValue(item);
                // add a button to select the species list for the assemblage
                Listcell lc = new Listcell();
                Checkbox c = new Checkbox();
                c.setChecked(selectedLists.contains(item.getDataResourceUid()));
                c.addEventListener(StringConstants.ONCLICK, new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        Checkbox c = (Checkbox) event.getTarget();
                        if (c.isChecked()) {
                            selectedLists.add(item.getDataResourceUid());
                        } else {
                            selectedLists.remove(item.getDataResourceUid());
                        }
                        if (selectedLists.size() <= 1) {
                            //need to fire a refresh to the parent components
                            postCheckBoxStatusChanged();
                        }
                    }
                });

                c.setParent(lc);
                lc.setParent(li);
                Listcell name = new Listcell();
                name.setSclass("list-a");
                A a = new A(item.getListName());
                a.setHref(CommonData.getSpeciesListServer() + "/speciesListItem/list/" + item.getDataResourceUid());
                a.setTarget(StringConstants.BLANK);
                a.setParent(name);
                name.setParent(li);
                Listcell date = new Listcell(item.getDateCreated());
                date.setParent(li);
                String sowner = item.getFullName() != null ? item.getFullName() : item.getFirstName() + " " + item.getSurname();
                Listcell owner = new Listcell(sowner);
                owner.setParent(li);
                Listcell count = new Listcell(item.getItemCount().toString());
                count.setParent(li);

            }
        });

        //SpeciesListListModel model = new SpeciesListListModel();
        //this.setModel(model);

        String searchTerm = getParent() != null ? ((Textbox) getParent().getFellowIfAny("txtSearchTerm")) != null ? ((Textbox) getParent().getFellowIfAny("txtSearchTerm")).getValue() : null : null;
        MutableInt listCount = new MutableInt();
        currentLists = new ArrayList<SpeciesListDTO>(SpeciesListUtil.getPublicSpeciesLists(Util.getUserEmail(),
                0, 1000000, null, null, searchTerm, listCount));
        setModel(new SimpleListModel<Object>(currentLists));

    }

    @Override
    public void onInitRender() {
        //can't set the default sorting until this point.
        LOGGER.debug("ON INIT RENDER");
        //set the header sort stuff
        Listhead head = this.getListhead();
        Listheader namehead = (Listheader) head.getChildren().get(1);
        namehead.setSortAscending(new SpeciesListComparator(StringConstants.LISTNAME, true));
        namehead.setSortDescending(new SpeciesListComparator(StringConstants.LISTNAME, false));
        Listheader datehead = (Listheader) head.getChildren().get(2);
        datehead.setSortAscending(new SpeciesListComparator(StringConstants.DATE_CREATED, true));
        datehead.setSortDescending(new SpeciesListComparator(StringConstants.DATE_CREATED, false));
        Listheader ownerhead = (Listheader) head.getChildren().get(3);
        ownerhead.setSortAscending(new SpeciesListComparator(StringConstants.USERNAME, true));
        ownerhead.setSortDescending(new SpeciesListComparator(StringConstants.USERNAME, false));
        Listheader counthead = (Listheader) head.getChildren().get(4);
        counthead.setSortAscending(new SpeciesListComparator(StringConstants.COUNT, true));
        counthead.setSortDescending(new SpeciesListComparator(StringConstants.COUNT, false));

        super.onInitRender();
    }

    /**
     * Returns a query object that represents a query for all species on the selected lists.
     *
     * @param geospatialKosher
     * @return
     */
    public BiocacheQuery extractQueryFromSelectedLists(boolean[] geospatialKosher) {
        StringBuilder sb = new StringBuilder();
        List<String> names = new ArrayList<String>();
        for (String list : selectedLists) {
            //get the speciesListItems
            Collection<SpeciesListItemDTO> items = SpeciesListUtil.getListItems(list);

            for (SpeciesListItemDTO item : items) {
                if (item.getLsid() != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(item.getLsid());
                } else {
                    names.add(item.getName());
                }

            }
        }
        String[] unmatchedNames = !names.isEmpty() ? names.toArray(new String[names.size()]) : null;
        String lsids = sb.length() > 0 ? sb.toString() : null;
        return new BiocacheQuery(lsids, unmatchedNames, null, null, null, false, geospatialKosher);
    }

    public String getSelectedNames() {
        String name = "";
        for (String list : selectedLists) {
            for (SpeciesListDTO i : currentLists) {
                if (i.getDataResourceUid().equals(list)) {
                    if (name.length() > 0) name += "|";
                    name += i.getListName();
                }
            }
        }
        return name;
    }

    public void onClick$btnSearchSpeciesListListbox(Event event) {

        //((SpeciesListListModel) getModel()).setTxtSearchTerm((Textbox) getParent().getFellowIfAny("txtSearchTerm"));
        //((SpeciesListListModel) getModel()).refreshModel();
        init();
    }

    public void onClick$btnClearSearchSpeciesListListbox(Event event) {

        //((SpeciesListListModel) getModel()).setTxtSearchTerm(null);
        //((SpeciesListListModel) getModel()).refreshModel();
        init();
    }

    /**
     * The List Model to be used by the species list listbox. This supports the paging of lists via the use of
     * WS calls to the list tool.
     */
    public static class SpeciesListListModel extends AbstractListModel implements Sortable {
        private int pageSize = 10;
        private int currentOffset = 0;
        private List<SpeciesListDTO> currentLists;
        private Integer size = null;
        private String sort = null;
        private String order = null;
        private String user = Util.getUserEmail();
        private Textbox txtSearchTerm;

        public void refreshModel() {
            //remove the cached version of the current lists
            currentLists = null;
            size = null;
            fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
        }

        public void setTxtSearchTerm(Textbox txtSearchTerm) {
            this.txtSearchTerm = txtSearchTerm;
        }

        @Override
        public Object getElementAt(int index) {
            if (currentLists == null || index >= (currentOffset + pageSize) || index < currentOffset) {
                loadPageOfLists(index);
            }
            if (currentLists != null && currentLists.size() > index - currentOffset) {
                return currentLists.get(index - currentOffset);
            }

            return null;
        }

        /**
         * Loads the page of lists from
         */
        private void loadPageOfLists(int index) {
            //calculate the page that it would appear on
            int page = index / pageSize;

            currentOffset = page * pageSize;
            String searchTerm = null;
            if (txtSearchTerm != null && txtSearchTerm.getText().length() > 0) {
                searchTerm = txtSearchTerm.getText();
            }
            LOGGER.debug("Current offset: " + currentOffset + " index " + index + " " + sort + " " + order + " " + searchTerm);
            MutableInt listCount = new MutableInt();
            currentLists = new ArrayList<SpeciesListDTO>(SpeciesListUtil.getPublicSpeciesLists(user, currentOffset, pageSize, sort, order, searchTerm, listCount));
            size = listCount.intValue();
            LOGGER.debug("Finished getting items");

        }

        @Override
        public int getSize() {
            //The maximum number of items in the species list list
            if (size == null) {
                LOGGER.debug("Starting to get page size...");
                size = SpeciesListUtil.getNumberOfPublicSpeciesLists(user);
                LOGGER.debug("Finished getting page size");
            }
            return size;
        }

        @Override
        public void sort(Comparator cmpr, boolean ascending) {
            if (cmpr instanceof SpeciesListComparator) {
                SpeciesListComparator c = (SpeciesListComparator) cmpr;
                order = c.getOrder();
                sort = c.getColumn();
                //force the reload
                currentLists = null;
                size = null;
                fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
            }
        }

        @Override
        public String getSortDirection(Comparator cmprtr) {
            if (cmprtr instanceof SpeciesListComparator) {
                SpeciesListComparator c = (SpeciesListComparator) cmprtr;

                if ("asc".equals(c.getOrder())) {
                    return "ascending";
                } else {
                    return "descending";
                }
            }

            return "natural";
        }

    }

    /**
     * A comparator to be used by the specieslist listbox to support custom sorting of the columns.
     */
    private static class SpeciesListComparator implements Comparator {
        boolean ascending;
        String column;

        public SpeciesListComparator(String column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;

        }

        @Override
        public int compare(Object arg0, Object arg1) {
            // we are not actually performing the compare within this object because th sort will be perfomed by the species list ws
            int ret = 0;
            try {
                if (StringConstants.LISTNAME.equals(column)) {
                    ret = ((SpeciesListDTO) arg0).getListName().compareToIgnoreCase(((SpeciesListDTO) arg1).getListName());
                } else if (StringConstants.DATE_CREATED.equals(column)) {
                    ret = ((SpeciesListDTO) arg0).getDateCreated().compareToIgnoreCase(((SpeciesListDTO) arg1).getDateCreated());
                } else if (StringConstants.USERNAME.equals(column)) {
                    ret = ((SpeciesListDTO) arg0).getFullName().compareToIgnoreCase(((SpeciesListDTO) arg1).getFullName());
                } else if (StringConstants.COUNT.equals(column)) {
                    ret = ((SpeciesListDTO) arg0).getItemCount() - ((SpeciesListDTO) arg1).getItemCount();
                }
            } catch (Exception e) {
                LOGGER.error("error sorting species list; column=" + column, e);
            }
            return ascending ? ret : -1 * ret;
        }

        public String getColumn() {
            return column;
        }

        public String getOrder() {
            if (ascending) {
                return "asc";
            } else {
                return "desc";
            }
        }

    }


}
