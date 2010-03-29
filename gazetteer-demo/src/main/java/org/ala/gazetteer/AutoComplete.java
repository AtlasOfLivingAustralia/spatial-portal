 import org.zkoss.zul.Combobox;
 import org.zkoss.zk.ui.event.InputEvent;
import java.util.Arrays;
import java.util.Iterator;
import org.zkoss.zul.Comboitem;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
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

public class AutoComplete extends Combobox {
      public AutoComplete() {
        refresh(""); //init the child comboitems
      }
      public AutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
      }
 
      public void setValue(String value) {
        super.setValue(value);
        refresh(value); //refresh the child comboitems
      }
      /** Listens what an user is entering.
       */
      public void onChanging(InputEvent evt) {
        refresh(evt.getValue());
      }
 
      /** Refresh comboitem based on the specified value.
       */
      private void refresh(String val) {
	//update the dictionary
		//TODO: remove hardcoded host, credentials
 		HttpHost targetHost = new HttpHost("localhost", 8080, "http"); 

 		DefaultHttpClient httpclient = new DefaultHttpClient();

		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		//localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);    		
		String searchString = val.trim().replaceAll("\\s+","+");	
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
		//resultLabel.setValue(responseText);
		 		  JSONObject responseJson = JSONObject.fromObject(responseText);		  
		  JSONObject search = responseJson.getJSONObject( "org.ala.rest.GazetteerSearch" );
		  JSONArray results = search.getJSONObject("results").getJSONArray("org.ala.rest.SearchResultItem");
		  _dict = new String[results.size()];

		  for (int i = 0; i<results.size();i++){ 
			_dict[i] = (String)results.getJSONObject(i).get("name");
			}
		}
		catch(Exception e)
		{
			
		}

	//        

	int j = Arrays.binarySearch(_dict, val);
        if (j < 0) j = -j-1;
 
        Iterator it = getItems().iterator();
        for (int cnt = 10; --cnt >= 0 && j < _dict.length && _dict[j].startsWith(val); ++j) {
          if (it != null && it.hasNext()) {
            ((Comboitem)it.next()).setLabel(_dict[j]);
          } else {
            it = null;
            new Comboitem(_dict[j]).setParent(this);
          }
        }
 
        while (it != null && it.hasNext()) {
          it.next();
          it.remove();
        }
      }
 
      private static String[] _dict = { ""};
    }
