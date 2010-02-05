package au.org.emii.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;


public class MapLayerMetadata implements Serializable {

	private static final long serialVersionUID = 1L;
	public final static int EARLIEST_CONCATENATED = 0;
	public final static int EARLIEST_ISO = 1;
	public final static int LATEST_CONCATENATED = 2;
	public final static int LATEST_ISO = 3;
	
	private String units = null;
	private List<Double> bbox;
	private List<Double> scaleRange;
	private List<String> supportedStyles;
	private List<String> datesWithData = new ArrayList<String>();
	private String nearestTimeIso;
	private String copyright;
	private List<String> palettes;
	private String defaultPalette;
	private boolean logScaling;
	private String moreInfo;

	// necessary?
	private int timeSteps;
	
	private String zAxisUnits = null;
	private boolean zAxisPositive;
	private List<Double> zAxisValues = new ArrayList<Double>();

	public void addDateWithData(String date) {
		datesWithData.add(date);
	}
	
	
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}
	public int getTimeSteps() {
		return timeSteps;
	}
	public void setTimeSteps(int timeSteps) {
		this.timeSteps = timeSteps;
	}
	public String getZAxisUnits() {
		return zAxisUnits;
	}
	
	
	public void setZAxisUnits(String axisUnits) {
		zAxisUnits = axisUnits;
	}
	public boolean isZAxisPositive() {
		return zAxisPositive;
	}
	public void setZAxisPositive(boolean axisPositive) {
		zAxisPositive = axisPositive;
	}




	public String getNearestTimeIso() {
		return nearestTimeIso;
	}
	public void setNearestTimeIso(String nearestTimeIso) {
		this.nearestTimeIso = nearestTimeIso;
	}
	public String getCopyright() {
		return copyright;
	}
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public String getDefaultPalette() {
		return defaultPalette;
	}
	public void setDefaultPalette(String defaultPalette) {
		this.defaultPalette = defaultPalette;
	}
	public boolean isLogScaling() {
		return logScaling;
	}
	public void setLogScaling(boolean logScaling) {
		this.logScaling = logScaling;
	}
	public List<Double> getBbox() {
		return bbox;
	}
	
	public String getBboxString() {
		return 
			bbox.get(0) + "," +
			bbox.get(1) + "," + 
			bbox.get(2) + "," + 
			bbox.get(3);
	}
	
	public void setBbox(List<Double> bbox) {
		this.bbox = bbox;
	}
	public List<Double> getScaleRange() {
		return scaleRange;
	}
	public void setScaleRange(List<Double> scaleRange) {
		this.scaleRange = scaleRange;
	}

	public List<String> getPalettes() {
		return palettes;
	}
	public void setPalettes(List<String> palettes) {
		this.palettes = palettes;
	}
	public List<String> getSupportedStyles() {
		return supportedStyles;
	}
	public void setSupportedStyles(List<String> supportedStyles) {
		this.supportedStyles = supportedStyles;
	}
	public String getMoreInfo() {
		return moreInfo;
	}
	public void setMoreInfo(String moreInfo) {
		this.moreInfo = moreInfo;
	}
	


	public List<Double> getZAxisValues() {
		return zAxisValues;
	}

	public void setZAxisValues(List<Double> axisValues) {
		zAxisValues = axisValues;
	}
	
	@SuppressWarnings("unchecked")
	public void setZAxisValues(Collection collection) {
		zAxisValues.addAll(collection);
	}


	public List<String> getDatesWithData() {
		return datesWithData;
	}

	public void sortDatesWithData() {
		Collections.sort(datesWithData);
	}


	/**
	 * A layer supports animation if it has more than
	 * one date listed in it's datesWithData list  
	 * @return
	 */
	public boolean isSupportsAnimation() {
		return (datesWithData.size() > 1);

	}

        /**
         * wrapper for getUnits() to escape any javascript chars
         * @return
         */
	public String getUnitsJS() {
		return StringEscapeUtils.escapeJavaScript(getUnits());
	}
}

