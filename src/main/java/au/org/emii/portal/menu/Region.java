package au.org.emii.portal.menu;


import au.org.emii.portal.value.BoundingBox;
import au.org.emii.portal.config.xmlbeans.RegionLayerGroup;

public class Region extends Facility {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BoundingBox boundingBox = new BoundingBox();

	public void copyFrom(RegionLayerGroup regionLayerGroup) {
		boundingBox.copyFrom(regionLayerGroup.getBoundingBox());		
	}
	
	public Object clone() throws CloneNotSupportedException {
		Region region = (Region) super.clone();
		return region;
	}

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}
}
