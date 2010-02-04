package au.org.emii.portal;
import au.org.emii.portal.config.xmlbeans.Service;

public interface ServiceProcessor {
	public MapLayer service(Service service);
}
