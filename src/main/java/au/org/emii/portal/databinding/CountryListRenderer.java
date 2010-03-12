/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.databinding;

import au.org.emii.portal.util.IsoCountries;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;

/**
 * Renderer for the country list combo box (iso country codes -> iso country names)
 * @author geoff
 */
public class CountryListRenderer implements ComboitemRenderer {

        @Override
        public void render(Comboitem comboItem, Object o) throws Exception {
                String[] country = (String[]) o;
                comboItem.setValue(country[IsoCountries.CODE]);
                comboItem.setLabel(country[IsoCountries.NAME]);
        }

}
