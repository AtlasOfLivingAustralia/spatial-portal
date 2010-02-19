package au.org.emii.portal;

import java.util.ArrayList;
import java.util.List;

public class PortalException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private List<String> faults = new ArrayList<String>();
	
	public PortalException(String message) {
		super(message);
	}
	
	public PortalException(List<String> faults) {
		this.faults = faults;
	}
	
	public PortalException(String message, List<String> faults) {
		super(message);
		this.faults = faults;
	}
	
	public void addFault(String fault) {
		faults.add(fault);
	}

	public List<String> getFaults() {
		return faults;
	}

	public void setFaults(List<String> faults) {
		this.faults = faults;
	}

        @Override
	public String getMessage() {
		StringBuffer message = new StringBuffer((super.getMessage() == null) ? "" : super.getMessage() + "; ");
		if ((faults != null) && (faults.size() > 0)) {
			message.append("Faults detected: ");
			String delim = "";
			for (String fault : faults) {
				message.append(delim + fault);
				delim = ", ";
			}
		}
		else {
			message.append("No addional fault information available");
		}
		return message.toString();
	}

}
