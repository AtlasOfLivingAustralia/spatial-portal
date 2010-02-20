package au.org.emii.portal.wms;
import au.org.emii.portal.MapLayer;
import au.org.emii.portal.config.xmlbeans.Service;

public interface ServiceProcessor {
	public MapLayer service(Service service);
}
