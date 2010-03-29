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

 
	public void onSearch(Event evt) {
		//TODO: remove hardcoded host, credentials
 		HttpHost targetHost = new HttpHost("localhost", 8080, "http"); 

 		DefaultHttpClient httpclient = new DefaultHttpClient();

		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		//localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);    		
		String searchString = searchText.getValue().trim().replaceAll("\\s+","+");	
		HttpGet httpget = new HttpGet(
			     "/geoserver/rest/gazetteer-search/result.json?q=" + searchString);
	

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
			


 	}

 }
