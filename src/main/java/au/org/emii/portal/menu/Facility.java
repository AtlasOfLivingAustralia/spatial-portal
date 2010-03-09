package au.org.emii.portal.menu;

import au.org.emii.portal.value.AbstractIdentifierImpl;
import au.org.emii.portal.menu.MenuGroup;
import au.org.emii.portal.config.xmlbeans.LayerGroup;
import org.apache.log4j.Logger;

public class Facility extends AbstractIdentifierImpl {

    private static final long serialVersionUID = 1L;
    private MenuGroup menu = null;

    public MenuGroup getMenu() {
        return menu;
    }

    /**
     * FIXME - move this !
     * @return
     */
    public String dump() {
        String dump = super.dump();
        if (menu == null) {
            dump += "MENU NOT SET";
        } else {
            dump += "\nMENU:\n" + menu.dump("");
        }
        return dump;


    }

    public Object clone() throws CloneNotSupportedException {
        Facility facility = (Facility) super.clone();
        if (menu != null) {
            facility.menu = (MenuGroup) menu.clone();
        }
        return facility;
    }

    public void setMenu(MenuGroup menu) {
        this.menu = menu;
    }

    

}
