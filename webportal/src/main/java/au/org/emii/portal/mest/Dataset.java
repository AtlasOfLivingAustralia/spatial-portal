package au.org.emii.portal.mest;

import java.util.ArrayList;
import java.util.List;

public class Dataset {
	
	public String identifier;
	public String title;
	public String type;	
	public String mestAbstract;
	public String mestrecord;
	public String showdetails;	
	public List<String> subject = new ArrayList<String>();
	public List<DatasetFormat> formats = new ArrayList<DatasetFormat>();
	
	
	
	public List<String> getSubject() {
		return subject;
	}
	public void setSubject(List<String> subject) {
		this.subject = subject;
	}	
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAbstract() {
		return mestAbstract;
	}
	public void setAbstract(String abstract1) {
		mestAbstract = abstract1;
	}
	public List<DatasetFormat> getFormats() {
		return formats;
	}
	public void setFormats(List<DatasetFormat> formats) {
		this.formats = formats;
	}
	
	public void addFormat(DatasetFormat n) {
		formats.add(n);
	}
	
	
	public void addSubject(String n) {
		subject.add(n);
	}
	
	public String getMestrecord() {
		return mestrecord;
	}
	public void setMestrecord(String mestrecord) {
		this.mestrecord = mestrecord;
	}
	
	public String getShowdetails() {
		return showdetails;
	}
	public void setShowdetails(String showdetails) {
		this.showdetails = showdetails;
	}


 	

}
