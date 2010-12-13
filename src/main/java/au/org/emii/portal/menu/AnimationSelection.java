package au.org.emii.portal.menu;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Date;

public class AnimationSelection implements Serializable {

	private static final long serialVersionUID = 1L;
	private Date startDate = null;
	private Date endDate = null;
	private double z = 0;

	/**
	 * startData adjusted for the earliest TIME with data on the given
	 * date (obtained by from ncwms)
	 */
	private String adjustedStartDate = null;

	/**
	 * startData adjusted for the lastest TIME with data on the given
	 * date (obtained by from ncwms)
	 */
	private String adjustedEndDate = null;
	
	private AbstractMap<String, String> timeStrings = null;
	private String selectedTimeStringKey = null;
	private long id;
        private long maplayerid;

        public long getMaplayerid() {
            return maplayerid;
        }

        public void setMaplayerid(long mapLayerId) {
            this.maplayerid = mapLayerId;
         }

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
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
	public double getZ() {
		return z;
	}
	public void setZ(double z) {
		this.z = z;
	}


	public AbstractMap<String, String> getTimeStrings() {
		return timeStrings;
	}
	public void setTimeStrings(AbstractMap<String, String> timeStrings) {
		this.timeStrings = timeStrings;
	}
	public String getSelectedTimeStringKey() {
		return selectedTimeStringKey;
	}
	public void setSelectedTimeStringKey(String selectedTimeStringKey) {
		this.selectedTimeStringKey = selectedTimeStringKey;
	}
	
	public String getSelectedTimeString() {
		return timeStrings.get(selectedTimeStringKey);
	}
	
	public boolean validDateSelection() {
		boolean valid;
		if (	(startDate != null) &&
				(endDate != null) &&
				(startDate.before(endDate)) ) {
			valid = true;
		}
		else {
			valid = false;
		}
		return valid;
	}
	public String getAdjustedStartDate() {
		return adjustedStartDate;
	}
	public void setAdjustedStartDate(String adjustedStartDate) {
		this.adjustedStartDate = adjustedStartDate;
	}
	public String getAdjustedEndDate() {
		return adjustedEndDate;
	}
	public void setAdjustedEndDate(String adjustedEndDate) {
		this.adjustedEndDate = adjustedEndDate;
	}


}
