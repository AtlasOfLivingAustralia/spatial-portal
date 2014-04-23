package au.org.ala.spatial.composer.species;

import au.org.ala.spatial.data.BiocacheQuery;
import au.org.ala.spatial.data.SpeciesListDTO;
import au.org.ala.spatial.data.SpeciesListItemDTO;
import au.org.ala.spatial.data.SpeciesListUtil;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
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
    private static Logger logger = Logger.getLogger(SpeciesListListbox.class);

    //stores the selected lists
    private java.util.List<String> selectedLists = new java.util.ArrayList<String>();

    public SpeciesListListbox() {
        init();
    }

    /**
     * An event that is fired to inform interested components that the number of check boxes selected has changed.
     */
    private void postCheckBoxStatusChanged() {
        Events.sendEvent(new Event("onSlCheckBoxChanged", this, selectedLists.size()));
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

    private void init() {
        setItemRenderer(new ListitemRenderer() {
            @Override
            public void render(Listitem li, Object data, int item_idx) {
                final SpeciesListDTO item = (SpeciesListDTO) data;
                li.setValue(item);
                // add a button to select the species list for the assemblage
                Listcell lc = new Listcell();
                Checkbox c = new Checkbox();
                c.setChecked(selectedLists.contains(item.getDataResourceUid()));
                c.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        Checkbox c = (Checkbox) event.getTarget();
                        if (c.isChecked())
                            selectedLists.add(item.getDataResourceUid());
                        else
                            selectedLists.remove(item.getDataResourceUid());
                        if (selectedLists.size() <= 1) {
                            //need to fire a refresh to the parent components
                            postCheckBoxStatusChanged();
                        }
                    }
                });

                c.setParent(lc);
                lc.setParent(li);
                Listcell name = new Listcell();
                A a = new A(item.getListName());
                a.setHref(CommonData.speciesListServer + "/speciesListItem/list/" + item.getDataResourceUid());
                a.setTarget("_blank");
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

        SpeciesListListModel model = new SpeciesListListModel();
        this.setModel(model);
    }

    @Override
    public void onInitRender() {
        //can't set the default sorting until this point.
        logger.debug("ON INIT RENDER");
        //set the header sort stuff
        Listhead head = this.getListhead();
        Listheader namehead = (Listheader) head.getChildren().get(1);
        namehead.setSortAscending(new SpeciesListComparator("listName", true));
        namehead.setSortDescending(new SpeciesListComparator("listName", false));
        Listheader datehead = (Listheader) head.getChildren().get(2);
        datehead.setSortAscending(new SpeciesListComparator("dateCreated", true));
        datehead.setSortDescending(new SpeciesListComparator("dateCreated", false));
        Listheader ownerhead = (Listheader) head.getChildren().get(3);
        ownerhead.setSortAscending(new SpeciesListComparator("username", true));
        ownerhead.setSortDescending(new SpeciesListComparator("username", false));
        Listheader counthead = (Listheader) head.getChildren().get(4);
        counthead.setSortAscending(new SpeciesListComparator("count", true));
        counthead.setSortDescending(new SpeciesListComparator("count", false));

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
        ArrayList<String> names = new ArrayList<String>();
        for (String list : selectedLists) {
            //get the speciesListItems
            Collection<SpeciesListItemDTO> items = SpeciesListUtil.getListItems(list);
            if (items != null) {
                for (SpeciesListItemDTO item : items) {
                    if (item.getLsid() != null) {
                        if (sb.length() > 0)
                            sb.append(",");
                        sb.append(item.getLsid());
                    } else {
                        names.add(item.getName());
                    }

                }
            }
        }
        String[] unmatchedNames = names.size() > 0 ? names.toArray(new String[names.size()]) : null;
        String lsids = sb.length() > 0 ? sb.toString() : null;
        return new BiocacheQuery(lsids, unmatchedNames, null, null, null, false, geospatialKosher);
    }

    public List<List<String>> getSelectedListItems() {
        ArrayList<String> guids = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();
        for (String list : selectedLists) {
            //get the speciesListItems
            Collection<SpeciesListItemDTO> items = SpeciesListUtil.getListItems(list);
            if (items != null) {
                for (SpeciesListItemDTO item : items) {
                    if (item.getLsid() != null) {
                        guids.add(item.getLsid());
                    } else {
                        names.add(item.getName());
                    }

                }
            }
        }
        List<List<String>> retList = new ArrayList<List<String>>(2);
        retList.add(guids);
        retList.add(names);
        return retList;
    }

    public static MapComposer getMapComposer() {
        return (MapComposer) Executions.getCurrent()
                .getDesktop()
                .getPage("MapZul")
                .getFellow("mapPortalPage");
    }

    /**
     * The List Model to be used by the species list listbox. This supports the paging of lists via the use of
     * WS calls to the list tool.
     */
    public class SpeciesListListModel extends AbstractListModel implements Sortable {
        int pageSize = 10;
        int currentOffset = 0;
        List<SpeciesListDTO> currentLists;
        Integer size = null;
        String sort = null;
        String order = null;
        String user = Util.getUserEmail();

        public void refreshModel() {
            //remove the cached version of the current lists
            currentLists = null;
            fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
        }

        @Override
        public Object getElementAt(int index) {
            //logger.debug("Index : " + index + " currentOffset: " + currentOffset );
            if (currentLists == null || index >= (currentOffset + pageSize) || index < currentOffset)
                loadPageOfLists(index);
            if (currentLists != null && currentLists.size() > index - currentOffset)
                return currentLists.get(index - currentOffset);

            return null;
        }

        /**
         * Loads the page of lists from
         */
        private void loadPageOfLists(int index) {
            //calculate the page that it would appear on
            int page = index / pageSize;

            currentOffset = page * pageSize;
            logger.debug("Current offset: " + currentOffset + " index " + index + " " + sort + " " + order);
            currentLists = new ArrayList<SpeciesListDTO>(SpeciesListUtil.getPublicSpeciesLists(user, currentOffset, pageSize, sort, order));
            logger.debug("Finished getting items");

        }

        @Override
        public int getSize() {
            //The maximum number of items in the species list list
            if (size == null) {
                logger.debug("Starting to get page size...");
                size = SpeciesListUtil.getNumberOfPublicSpeciesLists(user);
                logger.debug("Finished getting page size");
            }
            return size;
            //return SpeciesListUtil.getNumberOfPublicSpeciesLists(user);
        }

        @Override
        public void sort(Comparator cmpr, boolean ascending) {
            if (cmpr instanceof SpeciesListComparator) {
                SpeciesListComparator c = (SpeciesListComparator) cmpr;
                order = c.getOrder();
                sort = c.getColumn();
                //force the reload
                currentLists = null;
                fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
            }
        }

        @Override
        public String getSortDirection(Comparator cmprtr) {
            if (cmprtr instanceof SpeciesListComparator) {
                SpeciesListComparator c = (SpeciesListComparator) cmprtr;

                if (c.getOrder().equals("asc")) {
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
    private class SpeciesListComparator implements Comparator {
        boolean ascending;
        String column;

        public SpeciesListComparator(String column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;

        }

        @Override
        public int compare(Object arg0, Object arg1) {
            // we are not actually performing the compare within this object because th sort will be perfomed by the species list ws
            return 0;
        }

        public String getColumn() {
            return column;
        }

        public String getOrder() {
            if (ascending)
                return "asc";
            else
                return "desc";
        }

    }


}
