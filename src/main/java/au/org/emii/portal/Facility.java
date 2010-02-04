package au.org.emii.portal;


import au.org.emii.portal.config.xmlbeans.LayerGroup;
import org.apache.log4j.Logger;

public class Facility extends AbstractIdentifier {

	private static final long serialVersionUID = 1L;
	private MenuGroup menu = null;
	public MenuGroup getMenu() {
		return menu;
	}

	
	public void copyFrom(LayerGroup facility, PortalSession portalSession) {
		this.setId(facility.getId());
		this.setName(facility.getName());
		this.setDescription(facility.getDescription());

                if (facility.getMenu().getDisabled()) {
                    Logger logger = Logger.getLogger(getClass());
                    logger.info("skipping menu '" + facility.getId() + "' because it is disabled in config file");
                } else {
                    this.menu = new MenuGroup();
                    this.menu.copyFrom(facility.getMenu(), portalSession);
                }
	}
	
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
}
