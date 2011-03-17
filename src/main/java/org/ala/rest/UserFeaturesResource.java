package org.ala.rest;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import org.geoserver.rest.MapResource;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;


/**
 * Handles user uploaded features (for intersection etc.)
 * @author angus
 */
public class UserFeaturesResource extends MapResource {

	public Map test = new HashMap();

	@Override
	public boolean allowPost() {
		return true;
	}


	@Override
	public void postMap(Map map) { //throws Exception {
		JSONArray coords = JSONArray.fromObject(map.get("coordinates"));
		List<Point> points = new ArrayList();
		GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);
		for (Object obj : coords)  {
			JSONArray joCoordinate = JSONArray.fromObject(obj);
			Coordinate coordinate = new Coordinate((Double)joCoordinate.get(1),(Double)joCoordinate.get(0));
			//Coordinate coordinate = new Coordinate(Double.parseDouble((String)joCoordinate.get(0)),Double.parseDouble((String)joCoordinate.get(1)));

			Point point = gf.createPoint(coordinate);
			points.add(point);
		}
		
//		double[][] points = new double[coords.size()][2];
//
//		for (int i =0;i <coords.size(); i++)  {
//			JSONArray joCoordinate = JSONArray.fromObject(coords.get(i));
//			points[i][0] = (Double)joCoordinate.get(1);
//			points[i][1] = (Double)joCoordinate.get(0);
//		}

		GazetteerConfig gc = new GazetteerConfig();
		System.out.println("Using layers: " +  gc.getDefaultLayerNames().toString());
		Intersector is = new Intersector(gc.getDefaultLayerNames());

		List<String[]> allResults = is.intersect(points);
		System.out.println("Successes = " + allResults.size());
		try {
			FileWriter fw = new FileWriter("out.csv");
			for(String[] resultSet : allResults) {
				for (String result : resultSet) {
					fw.append(result).append('\n');
				}

			}
		fw.close();
		}
		catch(IOException e) {
		}
			 // Set the response's status and entity
			 getResponse().setStatus(Status.SUCCESS_CREATED);
			 Representation rep = new StringRepresentation("<root><result>success</result></root>",
			       MediaType.APPLICATION_XML);
			 // Indicates where is located the new resource.
			 rep.setIdentifier(getRequest().getResourceRef().getIdentifier()
			       + "/blah");

			 getResponse().setEntity(rep);


	}

	@Override
	public Map getMap() throws Exception {
		test.put("result", "slyrup");
		return test;
	}
}
