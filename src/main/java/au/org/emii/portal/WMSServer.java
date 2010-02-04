package au.org.emii.portal;

public class WMSServer {
	
	private String URL = "";
	private String name = "";
	private String description = "";
	private String version = "";
	
	public String getURL() {
		return URL;
	}
	public void setURL(String url) {
		URL = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	

}
