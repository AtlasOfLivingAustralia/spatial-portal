/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.emii.portal.databinding;

import au.org.emii.portal.config.IsoCountriesImpl;
import java.util.LinkedList;
import java.util.List;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListSubModel;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.event.ListDataListener;

/**
 *
 * @author geoff
 */
public class CountryListModel implements ListSubModel, ListModel {

        private List<String[]> countries;

        public CountryListModel(List<String[]> countries) {
                this.countries = countries;
        }

        public List<String[]> getCountries() {
                return countries;
        }

        public void setCountries(List<String[]> countries) {
                this.countries = countries;
        }

        /**
         * Shamelessly ripped off from http://www.zkoss.org/smalltalks/comboboxEnhancement/
         * @param o
         * @param nRows
         * @return
         */
        @Override
        public ListModel getSubModel(Object o, int nRows) {
                String idx = (o == null) ? "" : (String) o;
                if (nRows < 0) {
                        nRows = 10;
                }
                LinkedList data = new LinkedList();
                for (int i = 0; i < countries.size(); i++) {
                        // put the prefix and value to upper case to remove case sensitivity
                        if (idx.equals("") || countries.get(i)[IsoCountriesImpl.NAME].toUpperCase().startsWith(idx.toUpperCase())) {
                                data.add(countries.get(i));
                                if (--nRows <= 0) {
                                        break; //done
                                }
                        }
                }
                return new SimpleListModel(data);

        }

        @Override
        public Object getElementAt(int i) {
                return countries.get(i);
        }

        @Override
        public int getSize() {
                return countries.size();
        }

        @Override
        public void addListDataListener(ListDataListener ll) {

        }

        @Override
        public void removeListDataListener(ListDataListener ll) {

        }
}
