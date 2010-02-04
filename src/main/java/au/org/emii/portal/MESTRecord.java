package au.org.emii.portal;

import java.util.ArrayList;
import java.util.List;

public class MESTRecord {
	
	
	private List<DownloadLink> DownloadLinks = new ArrayList<DownloadLink>();
	private List<WMSLayer> WMSLayers = new ArrayList<WMSLayer>();
	private List<Website> Websites = new ArrayList<Website>();
	private List<WMSServer> WMSServers = new ArrayList<WMSServer>();
	public String Identifier = "";
	private String MESTUrl = "";


	public String getIdentifier() {
		return Identifier;
	}
	public void setIdentifier(String identifier) {
		Identifier = identifier;
	}
	public List<DownloadLink> getDownloadLinks() {
		return DownloadLinks;
	}
	public void setDownloadLinks(List<DownloadLink> downloadLinks) {
		DownloadLinks = downloadLinks;
	}
	public List<WMSLayer> getWMSLayers() {
		return WMSLayers;
	}
	public void setWMSLayers(List<WMSLayer> layers) {
		WMSLayers = layers;
	}
	public List<Website> getWebsites() {
		return Websites;
	}
	public void setWebsites(List<Website> websites) {
		Websites = websites;
	}
	public List<WMSServer> getWMSServers() {
		return WMSServers;
	}
	public void setWMSServers(List<WMSServer> servers) {
		WMSServers = servers;
	}	

	public String getMESTUrl() {
		return MESTUrl;
	}
	
	public void setMESTUrl(String url) {
		MESTUrl = url;
	}
	public void addDownloadLink(DownloadLink dl) {
		DownloadLinks.add(dl);				
	}
	public void addWMSLayer(WMSLayer wl) {
		WMSLayers.add(wl);				
	}
	public void addWebsite(Website wl) {
		Websites.add(wl);				
	}
	public void addWMSServer(WMSServer sl) {
		WMSServers.add(sl);				
	}



}
