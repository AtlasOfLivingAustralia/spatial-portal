package au.org.emii.portal;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import net.opengis.wms.DimensionDocument.Dimension;
import net.opengis.wms.LayerDocument.Layer;
import net.opengis.wms.StyleDocument.Style;
import net.sf.json.JSONObject;


public class ThreddsSupport extends NcWMSSupport {
	public final static int MAXIMUM_ALLOWED_FRAMES = Config.getValueAsInt("thredds_maximum_allowed_frames"); 
	
	/**
	 * Set the MapLayer instance's type property
	 * @param mapLayer
	 * @param layer
	 */
	protected void setType(MapLayer mapLayer, Layer layer) {
		mapLayer.setType(LayerUtilities.THREDDS);
	}
	
	/**
	 * Count the number of animation frames the user has selected
	 * @param mapLayer
	 * @return the number of frames selected
	 */
	public int countSelectedFrames(MapLayer mapLayer) {
		MapLayerMetadata metadata = mapLayer.getMapLayerMetadata();
		AnimationSelection animationSelection = mapLayer.getAnimationSelection();
		int frames = 0;
		if ((metadata != null) && (animationSelection != null)) {
			List<String> datesWithData = metadata.getDatesWithData();

			int start = datesWithData.indexOf(animationSelection.getAdjustedStartDate());
			int end = datesWithData.indexOf(animationSelection.getAdjustedEndDate());
			frames = end - start + 1;
			logger.debug("user selected " + frames + " frames");
		}
		else {
			/* else user called us incorrectly - prevent doing the animation
			 * by returning false and log it
			 */
			logger.warn("countSelectedFrames called with null MapLayerMetadata or AnimationSelection (should never happen)");
		}
		return frames;
	}
	
	/**
	 * Check whether the number of frames selected exceeds the maximum
	 * allowed (for performance reasons)
	 * @return true if too many frames are requested, otherwise false
	 */
	public boolean maxFramesExceeded(MapLayer mapLayer) {
		return (countSelectedFrames(mapLayer) > MAXIMUM_ALLOWED_FRAMES);		
	}
	
	/**
	 * Thredds doesn't have support for picking timestrings - so there
	 * is only one framerate.
	 * 
	 * Generate a 'default' date string by concatenating start and end
	 * dates around a '/' character
	 * @param mapLayer
	 * @return
	 */
	@Override
	protected boolean getTimeStrings(MapLayer mapLayer) {
		logger.debug("inside ThreddsSupport.getTimeStrings()");
		boolean success = false;
		String animationKey = "DEFAULT_FRAMERATE";
		HashMap<String,String> timeStrings = new HashMap<String, String>();
		AnimationSelection as = mapLayer.getAnimationSelection();
		if (as != null) {
			String adjustedStart = as.getAdjustedStartDate();
			String adjustedEnd = as.getAdjustedEndDate();
			if ((adjustedStart != null) && (adjustedEnd != null)) {
			
				/* concatenate the date strings around '/' and store as the only
				 * available timeString in the AnimationSelection instance 
				 */ 
				timeStrings.put(
						animationKey,
						adjustedStart +
						"/" +
						adjustedEnd
				);
				as.setTimeStrings(timeStrings);
				as.setSelectedTimeStringKey(animationKey);
				logger.debug("available animation timestrings: " + timeStrings.get(animationKey));
				success = true;
			}
			else {
				logger.debug("error correcting THREDDS animation dates");
			}
		}
		else {
			logger.debug("AnimationSelection is null - skipping");
		}
		return success;
	}
 
	protected void layerSettings(MapLayer mapLayer, Layer layer) {
		super.layerSettings(mapLayer, layer);
		MapLayerMetadata metadata = mapLayer.getMapLayerMetadata();
		if (metadata == null) {
			logger.debug("MapLayerMetadata was null - created it");
			metadata = new MapLayerMetadata();
			mapLayer.setMapLayerMetadata(metadata);
		}
		convertDimensions(layer.getDimensionList(), metadata);
		
		/* FIXME! Temporary hack:
		 * Disable querying of THREDDS layers because any GetFeatureInfo
		 * call results in an error 500 page
		 */
		mapLayer.setQueryable(false);
	}
	
	/**
	 * Obtain string values from list of dimensions and store in 
	 * MapLayerMetadata instance
	 * @param timeDimension
	 */
	protected void convertDimensions(List<Dimension> timeDimension, MapLayerMetadata metadata) {
		if (timeDimension != null) {
			/* the dimensions we get back from thredds comes back as just
			 * one string at the moment, so iterate over it (in case this 
			 * changes later), split each entry on ',' and trim whitespace
			 */
			for (Dimension dimension : timeDimension) {
				String[] split = dimension.getStringValue().split(",");
				for (String string : split) {
					metadata.addDateWithData(string.trim());
				}
			}
		}
		logger.debug("got " + metadata.getDatesWithData().size() + " dates with animation from dimension list");
	}
	
	/**
	 * ncWMS needs us to ask the server for the time part of each desired date
	 * but with thredds we already have a full date+time stored in the 
	 * datesWithData list, so all we have to do is find the one corresponding
	 * to the Date instance we have been given.
	 * 
	 * If we are asked to minimize a date, we find the earliest corresponding 
	 * date, if we are asked to not minimize a date, we find the latest 
	 * corresponding date.
	 * 
	 * If nothing matches (should never happen) we return null
	 *    
	 */
	protected String correctTimeInDate(MapLayer mapLayer, Date date, boolean minimize) {
		String correctedDate = null;
		List<String> datesWithData = mapLayer.getMapLayerMetadata().getDatesWithData();
		String targetDate = Validate.getShortIsoDateFormatter().format(date);
		
		List<String> matches = new ArrayList<String>();

		/* now find any strings that match the targetDate and add
		 * them to the matches list
		 */
		for (String dateWithData : datesWithData) {
			if (dateWithData.contains(targetDate)) {
				matches.add(dateWithData);
			}
		}
		
		if (matches.size() > 0) {
			/* now we've got a list of possible candiate dates (in reality
			 * there will probably only be one date here most of the time.
			 * 
			 * To honour the minimise parameter, all we will do is sort the 
			 * list using regular string rules and we will then have a list
			 * ordered earliest to latest, top to bottom, so all we do then 
			 * is pick and return the right date according to minimise
			 * 
			 * However, the list we get in the dimensions array has already
			 * been sorted when we read it so we don't need to do this again
			 */
			if (minimize) {
				// smallest date
				correctedDate = matches.get(0);
			}
			else {
				// largest date
				correctedDate = matches.get(matches.size() - 1);
			}
		}
		// else: no matches -  correctedDate will be left as null
		
		return correctedDate;
	}
	
	/**
	 * Do nothing (we already have the dates from GetCapabilities)
	 */
	protected void jsonDatesWithData(JSONObject jo, MapLayerMetadata animationParameters) {
		logger.debug("skipped processing JSON dates with data (THREDDS layer)");
	}
	
	/**
	 * FIXME
	 * Temporary hack - thredds needs both LAYER and LAYERS to be set in 
	 * the legend url
	 */
	protected void styleSettings(WMSStyle style, Style serverStyle) {
		String legendUri = style.getLegendUri();
		String layerName;
		String rawLegendUri;
		
		// YUK! Have to urldecode the uri so the LayerUtilities class
		// can use it - then have to make sure to urlencode anything
		// we need to add!
		
		try {
			rawLegendUri = URLDecoder.decode(legendUri,"utf-8");
		} 
		catch (UnsupportedEncodingException e) {
			// FUBAR jdk - will have to hope  for the best...
			logger.error("missing urldecoder! " + e.getMessage());
			rawLegendUri = legendUri;
		}
	
		layerName = LayerUtilities.getLayer(rawLegendUri);
		
		if (! Validate.empty(layerName)) {
			String styleFragment =
				LayerUtilities.queryConjunction(rawLegendUri) +
				"LAYERS=" + layerName;
			try {
				styleFragment = URLEncoder.encode(styleFragment, "utf-8");
			} 
			catch (UnsupportedEncodingException e) {
				// FUBAR
				logger.error("missing urlencoder! " + e.getMessage());
			}
			
			style.setLegendUri(legendUri + styleFragment);
		}
	}
}
