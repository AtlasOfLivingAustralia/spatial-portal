package au.org.emii.portal;


import au.org.emii.portal.config.xmlbeans.LayerGroup;
import au.org.emii.portal.config.xmlbeans.RegionLayerGroup;

public class Region extends Facility {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BoundingBox boundingBox = new BoundingBox();

	public void copyFrom(LayerGroup region, PortalSession portalSession) {
		super.copyFrom(region, portalSession);
		RegionLayerGroup regionLayerGroup = (RegionLayerGroup) region;
		boundingBox.copyFrom(regionLayerGroup.getBoundingBox());		
	}
	
	public Object clone() throws CloneNotSupportedException {
		Region region = (Region) super.clone();
		// do all the simple member variables get copied here?
		return region;
	}

	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}
}
