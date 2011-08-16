package au.org.emii.portal.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;

public class MapLayerMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    public final static int EARLIEST_CONCATENATED = 0;
    public final static int EARLIEST_ISO = 1;
    public final static int LATEST_CONCATENATED = 2;
    public final static int LATEST_ISO = 3;
    private String units = null;
    private List<Double> bbox = null;
    private List<Double> scaleRange;
    private List<String> supportedStyles;
    private List<String> datesWithData = new ArrayList<String>();
    /**
     *
     */
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
    private long id;
    private long maplayerid;
    private boolean isSpeciesLayer = false;
    private String speciesLsid;
    private String speciesRank;
    private String speciesDisplayName;
    private String speciesDisplayLsid;
    private double[] layerExtent;
    private int partsCount;
    private int occurrencesCount;

    public boolean isIsSpeciesLayer() {
        return isSpeciesLayer;
    }

    public void setIsSpeciesLayer(boolean isSpeciesLayer) {
        this.isSpeciesLayer = isSpeciesLayer;
    }

    public String getSpeciesLsid() {
        return speciesLsid;
    }

    public void setSpeciesLsid(String speciesLsid) {
        this.speciesLsid = speciesLsid;
    }

    public String getSpeciesDisplayName() {
        return speciesDisplayName;
    }

    public void setSpeciesDisplayName(String speciesDisplayName) {
        this.speciesDisplayName = speciesDisplayName;
    }

    public String getSpeciesDisplayLsid() {
        return speciesDisplayLsid;
    }

    public void setSpeciesDisplayLsid(String speciesDisplayLsid) {
        this.speciesDisplayLsid = speciesDisplayLsid;
    }

    public String getSpeciesRank() {
        return speciesRank;
    }

    public void setSpeciesRank(String speciesRank) {
        this.speciesRank = speciesRank;
    }

    public double[] getLayerExtent() {
        return layerExtent;
    }

    public void setLayerExtent(double[] layerExtent) {
        this.layerExtent = layerExtent;
    }

    /**
     * Set the layer extent after expanding the polygon by the
     * given percent in all directions.
     *
     * min longitude -= factor*Width
     * min latitude -= factor*Height
     * max longitude += factor*Width
     * max latitude += factor*Height
     *
     * @param polygon WKT for a rectangular polygon.
     * @param expandFactor
     */
    public void setLayerExtent(String polygon, double expandFactor) {
        layerExtent = polygonToExtents(polygon);
        double fw = expandFactor * (layerExtent[2] - layerExtent[0]);
        double fh = expandFactor * (layerExtent[3] - layerExtent[1]);
        layerExtent[0] -= fw;
        layerExtent[1] -= fh;
        layerExtent[2] += fw;
        layerExtent[3] += fh;
    }

    /**
     * WKT bounding box as POLYGON.
     * 
     * @return
     */
    public String getLayerExtentString() {
        if (layerExtent != null) {
            return "POLYGON((" + layerExtent[0] + " " + layerExtent[1] + ","
                    + layerExtent[2] + " " + layerExtent[1] + ","
                    + layerExtent[2] + " " + layerExtent[3] + ","
                    + layerExtent[0] + " " + layerExtent[3] + ","
                    + layerExtent[0] + " " + layerExtent[1] + "))";
        }
        return null;
    }

    public long getMaplayerid() {
        return maplayerid;
    }

    public void setMaplayerid(long maplayerid) {
        this.maplayerid = maplayerid;
    }

    public String getzAxisUnits() {
        return zAxisUnits;
    }

    public void setzAxisUnits(String zAxisUnits) {
        this.zAxisUnits = zAxisUnits;
    }

    public boolean iszAxisPositive() {
        return zAxisPositive;
    }

    public void setzAxisPositive(boolean zAxisPositive) {
        this.zAxisPositive = zAxisPositive;
    }

    public List<Double> getzAxisValues() {
        return zAxisValues;
    }

    public void setzAxisValues(List<Double> zAxisValues) {
        this.zAxisValues = zAxisValues;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDatesWithData(List<String> datesWithData) {
        this.datesWithData = datesWithData;
    }

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
        if (bbox == null) {
            return null;
        }
        if (bbox.size() == 0) {
            return null;
        }
        return bbox.get(0) + ","
                + bbox.get(1) + ","
                + bbox.get(2) + ","
                + bbox.get(3);
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

    public int getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(int pc) {
        partsCount = pc;
    }

    public boolean isOutside(String viewArea) {
        double[] vArea = polygonToExtents(viewArea);
        if(vArea == null) {
            return false;
        }
        return !(vArea[0] >= layerExtent[0] && vArea[2] <= layerExtent[2]
                && vArea[1] >= layerExtent[1] && vArea[3] <= layerExtent[3]);
    }

    double[] polygonToExtents(String polygon) {
        try {
            double[] extents = new double[4];
            String s = polygon.replace("POLYGON((", "").replace("))", "").replace(",", " ").replace("MULTI(", "").replace(")", "").replace("(", "");
            String[] sa = s.split(" ");
            double long1 = Double.parseDouble(sa[0]);
            double lat1 = Double.parseDouble(sa[1]);
            double long2 = Double.parseDouble(sa[0]);
            double lat2 = Double.parseDouble(sa[1]);
            for (int i = 0; i < sa.length; i += 2) {
                double lng = Double.parseDouble(sa[i]);
                double lat = Double.parseDouble(sa[i + 1]);
                if (lng < long1) {
                    long1 = lng;
                }
                if (lng > long2) {
                    long2 = lng;
                }
                if (lat < lat1) {
                    lat1 = lat;
                }
                if (lat > lat2) {
                    lat2 = lat;
                }
            }
            extents[0] = Math.min(long1, long2);
            extents[2] = Math.max(long1, long2);
            extents[1] = Math.min(lat1, lat2);
            extents[3] = Math.max(lat1, lat2);
            return extents;
        } catch (Exception e) {
            System.out.println("polygonToExtents: " + polygon);
            e.printStackTrace();
        }
        return null;
    }

    public int getOccurrencesCount() {
        return occurrencesCount;
    }

    public void setOccurrencesCount(int count) {
        occurrencesCount = count;
    }
}
