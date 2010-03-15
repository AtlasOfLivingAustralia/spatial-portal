 //MyComposer.java
 import org.zkoss.zk.ui.event.Event;
 import org.zkoss.zk.ui.event.InputEvent;
 import org.zkoss.zk.ui.util.GenericComposer;
 import org.zkoss.zk.ui.Component;
 import org.zkoss.zul.Label;
 import org.zkoss.zul.ListModel;
 import org.zkoss.zul.SimpleListModel;
 import org.zkoss.zul.Combobox;
import org.zkoss.zul.Textbox;
import org.apache.http.impl.client.*;
import org.apache.http.impl.auth.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*; 
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.IOException; 
public class MyComposer extends GenericComposer {

//	private Combobox combo;
	private Textbox searchText;
	private Label resultLabel;

	public void doAfterCompose(Component win) throws Exception {
		super.doAfterCompose(win);

	//	combo = (Combobox) win.getFellow("combo");	
		searchText = (Textbox) win.getFellow("searchText");
		resultLabel = (Label) win.getFellow("resultLabel");
	}

/*	public void suggest(InputEvent evt) {
		 combo.getItems().clear();
	         if (evt.getValue().startsWith("A")) {
	             combo.appendItem("Ace");
	             combo.appendItem("Ajax");
	             combo.appendItem("Apple");
	         } else if (evt.getValue().startsWith("B")) {
	             combo.appendItem("Best");
	             combo.appendItem("Blog");
	         }
	}	*/
 
	public void onSearch(Event evt) {
		//TODO: remove hardcoded host, credentials
 		HttpHost targetHost = new HttpHost("localhost", 80, "http"); 

 		DefaultHttpClient httpclient = new DefaultHttpClient();;
		httpclient.getCredentialsProvider().setCredentials(
       		new AuthScope(targetHost.getHostName(), targetHost.getPort()), 
       		new UsernamePasswordCredentials("admin", "at1as0f0z"));

		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);

		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);    		
		
		HttpGet httpget = new HttpGet(
			     "/geoserver/rest/gazetteer-search/result.json?q=" + searchText.getValue());
		/*ResponseHandler<byte[]> handler = new ResponseHandler<byte[]>() {
	    	    public byte[] handleResponse(
	            HttpResponse response) throws ClientProtocolException, IOException {
	            HttpEntity entity = response.getEntity();
	            if (entity != null) {
            		return EntityUtils.toByteArray(entity);
		        } else {
            	return null;
        		}
    		}
		};*/

		try
		{
		    HttpResponse response = httpclient.execute(targetHost, httpget, localcontext);
		    HttpEntity entity = response.getEntity();
		    String responseText = "";
		    if (entity != null) {
            		responseText = new String(EntityUtils.toByteArray(entity));
		        } else {
		           responseText = "Fail";
		        }
		//evt.getTarget().appendChild(new Label(responseText)); 
		resultLabel.setValue(responseText);
		}
		catch(Exception e)
		{}
			
	/*	String[] _dict = { 
		"abacus", "accuracy", "acuity", "adage", "afar", "after", "apple",
		"bible", "bird", "bingle", "blog",
		"cabane", "cape", "cease", "cedar",
		"dacron", "defacto", "definable", "deluxe",
		"each", "eager", "effect", "efficacy",
		"far", "far from",
		"girl", "gigantean", "giant",
		"home", "honest", "huge",
		"information", "inner",
		"jump", "jungle", "jungle fever",
		"kaka", "kale", "kame",
		"lamella", "lane", "lemma",
		"master", "maxima", "music",
		"nerve", "new", "number",
		"omega", "opera",
		"pea", "peace", "peaceful",
		"rock", "RIA",
		"sound", "spread", "student", "super",
		"tea", "teacher",
		"unit", "universe",
		"vector", "victory",
		"wake", "wee", "weak", "web2.0",
		"xeme",
		"yea", "yellow",
		"zebra", "zk",
		
	};
	 
	ListModel dictModel= new SimpleListModel(_dict);
	combo.setModel(dictModel);*/

 	}

 }
