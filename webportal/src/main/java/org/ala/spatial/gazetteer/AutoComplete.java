package org.ala.spatial.gazetteer;

import org.zkoss.zul.Combobox;
import org.zkoss.zk.ui.event.InputEvent;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.zkoss.zul.Comboitem;
import org.apache.http.impl.client.*;
import org.apache.http.HttpHost;
import org.apache.http.protocol.BasicHttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
        //TODO: remove hardcoded host,
        HttpHost targetHost = new HttpHost("ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com", 80, "http");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        BasicHttpContext localcontext = new BasicHttpContext();
        String searchString = val.trim().replaceAll("\\s+", "+");

        

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); 


        try {
            
            //Read in the xml response
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            String uri = targetHost.toString() + "/geoserver/rest/gazetteer/result.xml?q=" + searchString;
           //Messagebox.show(uri);
            Document resultDoc = builder.parse(uri);

            //Get a list of names from the xml
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//search/results/result/name/text()");

            Object result = expr.evaluate(resultDoc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
          
            Iterator it = getItems().iterator();
            
            
            for(int i=0;i<nodes.getLength();i++) {
                //Messagebox.show(nodes.item(i).getNodeValue());
                String itemString = (String) nodes.item(i).getNodeValue();
                if (it != null && it.hasNext()) {
                    ((Comboitem) it.next()).setLabel(itemString);
                } else {
                    it = null;
                    new Comboitem(itemString).setParent(this);
                }

            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {

        }

        
    }

}
