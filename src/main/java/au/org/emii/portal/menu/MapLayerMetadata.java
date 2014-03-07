package au.org.emii.portal.menu;

import java.io.Serializable;
import java.util.List;

public class MapLayerMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    public final static int EARLIEST_CONCATENATED = 0;
    public final static int EARLIEST_ISO = 1;
    public final static int LATEST_CONCATENATED = 2;
    public final static int LATEST_ISO = 3;
    private String units = null;
    private List<Double> bbox = null;

    private String moreInfo;

    private long id;
    private int occurrencesCount;

    public List<Double> getBbox() {
        return bbox;
    }

    public String getBboxString() {
        if (bbox == null) {
            return null;
        }
        if (bbox.isEmpty()) {
            return null;
        }
        return bbox.get(0) + ","
                + bbox.get(1) + ","
                + bbox.get(2) + ","
                + bbox.get(3);
    }

    public void setBbox(List<Double> bbox) {
        bbox.set(0, Math.max(-180.0, bbox.get(0)));
        bbox.set(1, Math.max(-85.0, bbox.get(1)));
        bbox.set(2, Math.min(180.0, bbox.get(2)));
        bbox.set(3, Math.min(85.0, bbox.get(3)));
        this.bbox = bbox;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    public int getOccurrencesCount() {
        return occurrencesCount;
    }

    public void setOccurrencesCount(int count) {
        occurrencesCount = count;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
