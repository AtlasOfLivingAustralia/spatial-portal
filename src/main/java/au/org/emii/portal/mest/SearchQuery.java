package au.org.emii.portal.mest;

import java.util.Date;

public class SearchQuery {

    private Double top;
    private Double left;
    private Double right;
    private Double bottom;
    private String searchTerm;
    private Date startDate;
    private Date endDate;
    private boolean useBBOX = false;
    private boolean useDate = false;

    public boolean isUseDate() {
		return useDate;
	}

	public void setUseDate(boolean useDate) {
		this.useDate = useDate;
	}

	public boolean isUseBBOX() {
        return useBBOX;
    }

    public void setUseBBOX(boolean useBBOX) {
        this.useBBOX = useBBOX;
    }

    public Double getTop() {
        return top;
    }

    public void setTop(Double top) {
        this.top = top;
    }

    public Double getLeft() {
        return left;
    }

    public void setLeft(Double left) {
        this.left = left;
    }

    public Double getRight() {
        return right;
    }

    public void setRight(Double right) {
        this.right = right;
    }

    public Double getBottom() {
        return bottom;
    }

    public void setBottom(Double bottom) {
        this.bottom = bottom;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
