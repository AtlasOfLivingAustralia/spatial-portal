package au.org.emii.portal.wms;

import au.org.emii.portal.MapLayer;
import au.org.emii.portal.config.xmlbeans.Discovery;

public interface DiscoveryProcessor {
	
	/**
	 * Start the discovery process
	 * @param 	discovery Discovery instance describing where to discover data 
	 * 			from.  Usually manually specified in the configuration file and
	 * 			build automatically by XMLBEANS but can be created as required
	 * 			for parts of the system such as the MEST search and user defined
	 * 			WMS server import sections.
	 * @param 	descendAllChildren Force descending all child layers, even if a
	 * 			parent layer is displayable (has a name)
	 * @param 	queryableDisabled Do not set the queryable flag for any layers,
	 * 			detected from this server, even if the GetCapabilities document
	 * 			specifically allows it
	 * @param	quiet Log errors as debug messages rather than errors
	 * @return	MapLayer instance if the process was successful otherwise null
	 */
	public MapLayer discover(Discovery discovery, boolean descendAllChildren, boolean queryableDisabled, boolean quiet);
	
	/**
	 * Return true if we were able to get data from the host but were
	 * not able to parse a capabilities document from it.  Return false
	 * if no errors
	 * @return
	 */
	public boolean isParseError();
	
	/**
	 * Return true if we were got an error reading data from the host
	 * otherwise false
	 * @return
	 */
	public boolean isReadError();
	
	/**
	 * Return the last (if any) system generated error message
	 * @return
	 */
	public String getLastErrorMessage();
}
