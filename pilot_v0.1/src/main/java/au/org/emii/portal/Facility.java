package au.org.emii.portal;


import au.org.emii.portal.config.LayerGroup;

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
		
		this.menu = new MenuGroup();
		this.menu.copyFrom(facility.getMenu(), portalSession);
	}
	
	public String dump() {
		String dump = super.dump();
		dump += "\nMENU:\n" + menu.dump("");
		return dump;

		
	}
	
	public Object clone() throws CloneNotSupportedException {
		Facility facility = (Facility) super.clone();
		facility.menu = (MenuGroup) menu.clone();
		return facility;
	}
}
