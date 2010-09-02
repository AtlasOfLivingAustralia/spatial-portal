package org.ala.rest;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.xml.XML;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Search resource used for querying the gazetteer.
 * @author Angus
 */
public class SearchResource extends AbstractResource {//ReflectiveResource {

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {

        List<DataFormat> formats = new ArrayList();
        formats.add(new StringFormat(MediaType.APPLICATION_XML));

        return formats;
    }

    /***
     * Handles the get request for a gazetteer search. Responds with a list of search results
     */
    @Override
    public void handleGet() {
        XStream xstream = new XStream();
        DataFormat format = getFormatGet();

        System.out.println(getRequest().getAttributes().toString());

        
        String query = getRequest().getAttributes().get("q").toString();
        if (query.contains("q")) {
        String nameTerm = query.split("=")[1].replace(",type","");
        //Support & as separator as well
        nameTerm = nameTerm.replace("&type","");
        Search searchObj;
        if (query.contains("type")) {
            String typeTerm =  query.split("=")[2].replace(",","");
            searchObj = new Search(nameTerm.replace("+", "* AND ") + "*", typeTerm);
            xstream.processAnnotations(Search.class);
            //System.out.println(xstream.toXML(searchObj));
            String xmlString = xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        } else {
            searchObj = new Search(nameTerm.replace("+", "* AND ") + "*");
            xstream.processAnnotations(Search.class);
            //System.out.println(xstream.toXML(searchObj));
            String xmlString =  xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        }
        }
        else if (query.contains("point")) {
            System.out.println("Point search ...");
            String point = query.split("&")[0];
            String layerName = query.split("&layer=")[1];
            String x = point.split("=")[1].split(",")[0];
            String y = point.split("=")[1].split(",")[1];


            PointSearch searchObj = new PointSearch(x,y,layerName);
            xstream.processAnnotations(PointSearch.class);
            //System.out.println(xstream.toXML(searchObj));
            String xmlString =  xstream.toXML(searchObj);
            getResponse().setEntity(format.toRepresentation(xmlString));
        }
    }
}
