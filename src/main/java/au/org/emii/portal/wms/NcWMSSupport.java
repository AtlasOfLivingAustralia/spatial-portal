package au.org.emii.portal.wms;

import au.org.emii.portal.menu.AnimationSelection;
import au.org.emii.portal.net.HttpConnectionImpl;
import au.org.emii.portal.util.LayerUtilitiesImpl;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.util.Validate;
import au.org.emii.portal.wms.WMSSupport_1_3_0;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import net.opengis.wms.LayerDocument.Layer;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;

public class NcWMSSupport extends WMSSupport_1_3_0 {
	
	/**
	 * Set the MapLayer instance's type property
	 * @param mapLayer
	 * @param layer
	 */
	protected void setType(MapLayer mapLayer, Layer layer) {
		mapLayer.setType(LayerUtilitiesImpl.NCWMS);
	}
	
	@Override
	protected void layerSettings(MapLayer mapLayer, Layer layer) {
		setType(mapLayer, layer);
		
		// nasty hack -  ncwms layers are assumed queryable
		// because the server doensn't do anything with the 
		// queryable flag
		mapLayer.setQueryable(true);
		
		// ncwms obtains layer names from within the netcdf file and always
		// returns the name 'none' when there is no name available.  There
		// is usually something in the abstract we can use instead.
		if ((mapLayer.getName().equals("none")) &&
			(layer.getAbstract() != null)) {
			mapLayer.setName(layer.getAbstract());
		}
		
		mapLayerMetadata(mapLayer, layer);
		mapLayer.setMapLayerMetadata(
				getMapLayerMetadata(mapLayer)
		);
	}
	
	protected void mapLayerMetadata(MapLayer mapLayer, Layer layer) {
		mapLayer.setMapLayerMetadata(
				getMapLayerMetadata(mapLayer)
		);
		
	}
	
	@SuppressWarnings("unchecked")
	protected void jsonDatesWithData(JSONObject jo, MapLayerMetadata animationParameters) {
		JSONObject datesWithData = jo.getJSONObject("datesWithData");
		if (! datesWithData.isNullObject()) {
			Set<String> years = datesWithData.keySet();

			for (String year : years) {
				JSONObject yearWithData = datesWithData.getJSONObject(year);
				Set<String> months = yearWithData.keySet();
				for (String month : months) {
					/* months are 0 indexed, so we need to parse
					 * an integer, increment by one and then re-format
					 * to restore the leading 0
					 */
					DecimalFormat formatter = new DecimalFormat("00");

					String monthString = 
						formatter.format(Integer.parseInt(month) + 1);
					JSONArray monthWithData = yearWithData.getJSONArray(month);
					for (Object day: monthWithData) {
						String dayString = formatter.format(Integer.parseInt(day.toString(), 10));
						animationParameters.addDateWithData(
								year + "-" +
								monthString + "-" + 
								dayString
						);
					}
				}
			}
		}			

	}
	// 
	 
	// TIMESTEPS URI
	//http://obsidian:8080/ncWMS/wms?item=timesteps&layerName=67%2Fu&day=2006-09-19T00%3A00%3A00Z&request=GetMetadata

	// this is  timestrings we can use in the uri to control animation
	// based on timestepss
	//http://obsidian:8080/ncWMS/wms?item=animationTimesteps&layerName=67%2FTemperature_layer_between_two_pressure_difference_from_ground&start=2002-12-02T22%3A00%3A00.000Z&end=2002-12-03T01%3A00%3A00.000Z&request=GetMetadata
	/** 
	 * Support for parsing JSON animation parameters from NCWMS JSON responses 
	 * 
	 * Example JSON response string: 
	 * {
	 * 	"units":"m/sec",
	 * 	"bbox":[146.80064392089844,-43.80047607421875,163.8016815185547,-10.000572204589844],
	 * 	"scaleRange":[-0.99646884,1.2169001],
	 * 	"supportedStyles":["BOXFILL"],
	 * 	"zaxis":{
	 * 		"units":"meters",
	 * 		"positive":false,
	 * 		"values":[-5]
	 * 	},
	 * 	"datesWithData":{
	 * 		"2006":{
	 * 			"8":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19]
	 * 		}
	 * 	},
	 * 	"nearestTimeIso":"2006-09-01T12:00:00.000Z",
	 * 	"moreInfo":"",
	 * 	"copyright":"",
	 * 	"palettes":["redblue","alg","ncview","greyscale","alg2","occam","rainbow","sst_36","ferret","occam_pastel-30"],
	 * 	"defaultPalette":"rainbow",
	 * 	"logScaling":false
	 * }
	 */
	public MapLayerMetadata  getMapLayerMetadata(MapLayer mapLayer) {

		String uri = layerUtilities.getMetadataUri(
				layerUtilities.getFQUri(mapLayer.getUri()),
				mapLayer.getLayer()
		);
		logger.debug("ncwms animation parameter uri: " + uri);
		MapLayerMetadata animationParameters = null;


		// sample JSON string - this was used for initial testing: //"{'label':'eMII','children':[{'label':'ocean temp AUS','children':[{'id':'ocean_EastAUS/temp','label':'none'}]},{'label':'ncar','children':[{'id':'ncar/ua','label':'eastward_wind'},{'id':'ncar/pr','label':'precipitation_flux'},{'id':'ncar/tas','label':'air_temperature'},{'id':'ncar/area','label':'Surface area'},{'id':'ncar/msk_rgn','label':'Mask region'}]},{'label':'tos2002','children':[{'id':'tos2002/tos','label':'sea_surface_temperature'}]}]}"  ;
		String json = null;
		URLConnection connection = null;
		InputStream in = null;
		try {
			connection = httpConnection.configureURLConnection(uri);
			in = connection.getInputStream();
			json = IOUtils.toString(in);


			if (json != null) {
				logger.debug("process JSON: " + json);
				JsonConfig jsonConfig = new JsonConfig();  
				jsonConfig.setRootClass(MapLayerMetadata.class);
				jsonConfig.setJavaPropertyFilter( new PropertyFilter(){
                                        @Override
					public boolean apply( Object source, String name, Object value ) {    
						if( "datesWithData".equals( name ) || "zaxis".equals( name ) ){    
							return true;    
						}    
						return false;    
					}    
				}); 
				JSONObject jo = JSONObject.fromObject( json);  	

				animationParameters = (MapLayerMetadata) JSONSerializer.toJava(jo, jsonConfig);

				// dates with data - we want to be able to ignore these
				// for THREDDS
				jsonDatesWithData(jo, animationParameters);
				
				// z-axis - can be null if there's no data
				JSONObject zAxis = jo.getJSONObject("zaxis");
				if (! zAxis.isNullObject()) {
					animationParameters.setZAxisPositive(
							zAxis.getBoolean("positive")
					);
					animationParameters.setZAxisUnits(
							zAxis.getString("units")
					);
					animationParameters.setZAxisValues(
							JSONArray.toCollection(zAxis.getJSONArray("values"))
					);
				}

				animationParameters.sortDatesWithData();

			}
			else {
                                parseError = true;
                                lastErrorMessage = "empty NCWMS JSON response from '" + uri + "'";
			}
		} 
		catch (MalformedURLException e) {
                        broken = true;
                        lastErrorMessage = "malformed uri: '" + uri + "'";
		} 
		catch (IOException e) {
			lastErrorMessage = "IO error fetching ncwms URI: '" + uri + "'.  Root cause: " + e.getMessage();
                        readError = true;
		}
		catch (JSONException e) {
                        parseError = true;
                        lastErrorMessage = "Error parsing JSON response '"+ json + "'from '" + uri + "'";
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		logger.debug("leaving layerDetails()");
		
		return animationParameters;
	}

	/**
	 * When asking ncwms for dateStrings, you have to use the correct time
	 * parameter or you get an error eg, requesting:
	 * 	
	 * 	start=2006-04-03T00:00:00.000Z --> error: {"exception":{"className":"uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException","message":"The value &#034;2006-09-01T00:00:00.000Z&#034; is not valid for the TIME dimension"}}
	 *  start=2006-04-03T12:00:00.000Z --> ok
	 *  
	 * The time portion of the date string to use has to be obtained from
	 * ncwms for each day you wish to serve
	 * 
	 * @param mapLayer
	 */
	protected String correctTimeInDate(MapLayer mapLayer, Date date, boolean minimize) {
		String dateString = null;

		// example uri - 		http://obsidian:8080/ncWMS/wms?item=timesteps&layerName=67/v&request=GetMetadata&day=2006-09-01T00:00:00.000Z
		String uri = layerUtilities.getTimestepsUri(
				layerUtilities.getFQUri(mapLayer.getUri()),
				mapLayer.getLayer(),
				date
		);

		String json = null;
		URLConnection connection = null;
		InputStream in = null;
		logger.debug("Making JSON request for: " + uri);
		try {
			connection = httpConnection.configureURLConnection(uri);
			in = connection.getInputStream();
			json= IOUtils.toString(in);
			logger.debug("got JSON: " + json);
			JSONObject jo = JSONObject.fromObject( json);
			JSONArray timesteps = jo.getJSONArray("timesteps");
			/* we get an array of suitable timesteps (time component) 
			 * back - I'm assuming these will be ordered from earliest
			 * to latest but haven't seen data with more than one 
			 * element in this array before in the wild.
			 */
			String timepart;		
			
			/* ncwms will return an empty array if for some reason there
			 * is no data available, eg you requested data outside the 
			 * allowed date range.  Normally this should never happen 
			 * because the web interface should protect against this.  If
			 * it doesn't, at least capture the error and stop the user
			 * getting a stack trace.  Partial fix for ticket #72
			 */
			if (timesteps !=  null && timesteps.size() > 0) {
				if (minimize) {
					// minimal date range - pick earliest data sample
					timepart = timesteps.getString(0);
				}
				else {
					// pick latest data sample
					timepart = timesteps.getString(timesteps.size() - 1);
				}
	
	
				// now replace the time portion of the date with 
				// the one we just obtained
				DateFormat df = Validate.getShortIsoDateFormatter();
	
				dateString = df.format(date) + 'T' + timepart;
			}
			else {
				logger.error(
					"user requested animation for dates outside the " +
					"dataset date range (timesteps null or empty) " + 
					"uri: " + uri
				);
			}
		} 
		catch (JSONException e) {
			logger.error(e.getMessage() + "text was: " + json);
		}
		catch (IOException e) {
			logger.error(e.getMessage() + " reading " + uri);
			
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		return dateString;
	}
	

	
	/**
	 * ncWMS references a combination of date range and frame rate
	 * by a so-called 'date string'.  Clients then make a request 
	 * with the desired date string to show the animation.  The 
	 * date string determines the frame rate.
	 * 
	 * To obtain the list of possible date strings, we need to
	 * ask ncWMS what strings are available for a given date range.
	 * 
	 * The result comes back as JSON which we store in the 
	 * AnimationSelection instance for the mapLayer;
	 * 
	 * Sample conversation:
	 * http://obsidian:8080/ncWMS/wms?item=animationTimesteps&layerName=67%2Fv&start=2006-09-01T12%3A00%3A00.000Z&end=2006-09-19T12%3A00%3A00.000Z&request=GetMetadata
	 * {
	 * 	"timeStrings":[
	 * 		{
	 * 			"title":"Full (18 frames)",
	 * 			"timeString":"2006-09-01T12:00:00.000Z/2006-09-19T12:00:00.000Z"
	 * 		},
	 * 		{
	 * 			"title":"Daily (18 frames)",
	 * 			"timeString":"2006-09-01T12:00:00.000Z,2006-09-02T12:00:00.000Z,2006-09-03T12:00:00.000Z,2006-09-05T12:00:00.000Z,2006-09-06T12:00:00.000Z,2006-09-07T12:00:00.000Z,2006-09-08T12:00:00.000Z,2006-09-09T12:00:00.000Z,2006-09-10T12:00:00.000Z,2006-09-11T12:00:00.000Z,2006-09-12T12:00:00.000Z,2006-09-13T12:00:00.000Z,2006-09-14T12:00:00.000Z,2006-09-15T12:00:00.000Z,2006-09-16T12:00:00.000Z,2006-09-17T12:00:00.000Z,2006-09-18T12:00:00.000Z,2006-09-19T12:00:00.000Z"
	 * 		},
	 * 		{
	 * 			"title":"Weekly (3 frames)",
	 * 			"timeString":"2006-09-01T12:00:00.000Z,2006-09-08T12:00:00.000Z,2006-09-15T12:00:00.000Z"
	 * 		}
	 * 	]
	 * }
	 * @param mapLayer
	 */
	public boolean animationDateStrings(MapLayer mapLayer) {
		boolean success = false;
		AnimationSelection as = mapLayer.getAnimationSelection();
		if (as != null) {
			if (as.validDateSelection()) {
				String startDateString = correctTimeInDate(mapLayer, as.getStartDate(), true);
				String endDateString = correctTimeInDate(mapLayer, as.getEndDate(), false);
				if ((startDateString != null) && (endDateString != null)) {
					// store the adjusted date strings in AnimationSelection - 
					// we need them to do the timeseries plot
					as.setAdjustedStartDate(startDateString);
					as.setAdjustedEndDate(endDateString);
					
					// fetch the timestrings
					success = getTimeStrings(mapLayer);
				}
				else {
					logger.debug("Animation timing information not received from ncwms");
				}
			}
			else {
				logger.debug(
						"AnimationSelection contains invalid date range"
				);	
			}
		}
		else {
			logger.debug(
					"AnimationSelection is null"
			);	
		}
		logger.debug("leaving animationDateStrings()");
		return success;
	}
	
	/**
	 * Ask ncwms what timestrings we can use to request animations with.
	 * Timestrings are an ncwms internal notation to represent the set of
	 * start date, end date and framerate.
	 * 
	 * Store the result inside the passed in MapLayer instance
	 * @param mapLayer
	 * @return
	 */
	protected boolean getTimeStrings(MapLayer mapLayer) {
		boolean success = false;
		String uri = layerUtilities.getNcWMSTimeStringsUri(
				layerUtilities.getFQUri(mapLayer.getUri()),
				mapLayer.getLayer(),
				mapLayer.getAnimationSelection().getAdjustedStartDate(),
				mapLayer.getAnimationSelection().getAdjustedEndDate()
		);

		String json = null;
		URLConnection connection = null;
		InputStream in = null;
		logger.debug("Making JSON request for: " + uri);

		try {
			connection = httpConnection.configureURLConnection(uri);
			in = connection.getInputStream();
			json= IOUtils.toString(in);
			logger.debug("got JSON: " + json);
			JSONObject jo = JSONObject.fromObject( json);
			JSONArray ja = jo.getJSONArray("timeStrings");
			HashMap<String,String> timeStrings = new HashMap<String, String>();
			for (Object object : ja) {
				timeStrings.put(
						((JSONObject) object).getString("title"),
						((JSONObject) object).getString("timeString")
				);
			}
			TreeMap<String, String> sortedMap = new TreeMap<String, String>(timeStrings);
			mapLayer.getAnimationSelection().setTimeStrings(sortedMap);
			success = true;
		} 
		catch (IOException e) {
			logger.error(e.getMessage() + " reading " + uri);
		}
		catch (JSONException e) {
			logger.error(
					e.getMessage() + " trying to parse JSON from " + json + " at " + uri);
		}
		finally {
			IOUtils.closeQuietly(in);
		}
		
		return success;
	}

}
